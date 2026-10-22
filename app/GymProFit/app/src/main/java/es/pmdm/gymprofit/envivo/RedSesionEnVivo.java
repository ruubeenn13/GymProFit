package es.pmdm.gymprofit.envivo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.envivo.UltimaVez;
import es.pmdm.gymprofit.model.record.PuntoProgresion;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RecordApi;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.network.SesionApi;

// ============================================================
// RedSesionEnVivo — las llamadas de la sesión en vivo sobre Retrofit. Traduce y poco más:
// la respuesta pasa al repositorio, que es quien decide (la mejor marca de cada ejercicio
// se arma aquí con /records y, si falta, con la progresión).
// ============================================================
public class RedSesionEnVivo implements SesionEnCursoRepositorio.Red {

    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final RecordApi recordApi = ApiClient.service(RecordApi.class);

    @Override
    public void ejerciciosDeRutina(int rutinaId, Respuesta<List<RutinaEjercicio>> r) {
        rutinaApi.getEjerciciosDeRutina(rutinaId).enqueue(reenviar(r));
    }

    @Override
    public void ultimaVez(List<Integer> ejercicioIds, Respuesta<List<UltimaVez>> r) {
        StringBuilder ids = new StringBuilder();
        for (Integer id : ejercicioIds) {
            if (ids.length() > 0) ids.append(',');
            ids.append(id);
        }
        sesionApi.ultimaVez(ids.toString()).enqueue(reenviar(r));
    }

    // La mejor marca de cada ejercicio sin tocar la API (GP-210): los récords vigentes de
    // GET /records y, para quien aún no ha batido ninguno pero ya entrenó (su única marca es
    // el punto de partida), la mejor de su progresión. Un ejercicio sin nada no sale.
    @Override
    public void mejoresMarcas(List<Integer> ejercicioIds, Respuesta<Map<Integer, SesionEnCurso.Marca>> r) {
        recordApi.getRecords(null).enqueue(new ApiCallback<Records>() {
            @Override public void onOk(Records body) {
                Map<Integer, SesionEnCurso.Marca> marcas = new HashMap<>();
                if (body != null) {
                    for (Record rec : body.getRecords()) {
                        if (ejercicioIds.contains(rec.getEjercicioId())) {
                            marcas.put(rec.getEjercicioId(), deRecord(rec));
                        }
                    }
                }
                List<Integer> faltan = new ArrayList<>();
                for (Integer id : ejercicioIds) if (!marcas.containsKey(id)) faltan.add(id);
                progresiones(faltan, 0, marcas, r);
            }

            @Override public void onFail(int code, String message) { r.fallo(code, message); }
        });
    }

    // Una a una, para no abrir un puñado de peticiones a la vez: son pocas y raras.
    private void progresiones(List<Integer> ids, int i, Map<Integer, SesionEnCurso.Marca> marcas,
                              Respuesta<Map<Integer, SesionEnCurso.Marca>> r) {
        if (i >= ids.size()) {
            r.ok(marcas);
            return;
        }
        int id = ids.get(i);
        recordApi.getProgresion(id).enqueue(new ApiCallback<List<PuntoProgresion>>() {
            @Override public void onOk(List<PuntoProgresion> puntos) {
                SesionEnCurso.Marca mejor = mejorDe(puntos);
                if (mejor != null) marcas.put(id, mejor);
                progresiones(ids, i + 1, marcas, r);
            }

            @Override public void onFail(int code, String message) {
                // Sin la progresión de uno no se sabe su marca, y el resto no se pierde:
                // mejor una respuesta incompleta que ninguna; ese ejercicio no celebrará.
                r.fallo(code, message);
            }
        });
    }

    private static SesionEnCurso.Marca deRecord(Record rec) {
        return new SesionEnCurso.Marca(rec.getTipo(),
                rec.esDePeso() ? kilos(rec.getPeso()) : null, rec.getRepeticiones(), rec.getSegundos());
    }

    private static String kilos(double peso) {
        return java.math.BigDecimal.valueOf(peso).stripTrailingZeros().toPlainString();
    }

    // La mejor marca de una progresión: como en el récord vigente de la API, manda la de peso
    // y, dentro de un tipo, la mejor según la regla de GP-088.
    @androidx.annotation.Nullable
    static SesionEnCurso.Marca mejorDe(@androidx.annotation.Nullable List<PuntoProgresion> puntos) {
        if (puntos == null) return null;
        SesionEnCurso.Marca mejor = null;
        for (PuntoProgresion p : puntos) {
            SesionEnCurso.Marca m = new SesionEnCurso.Marca(p.getTipo(),
                    p.esDePeso() ? kilos(p.getPeso()) : null, p.getRepeticiones(), p.getSegundos());
            if (mejor == null) {
                mejor = m;
            } else if (m.tipo != null && m.tipo.equals(mejor.tipo)) {
                if (LogicaRecordVivo.supera(m, mejor)) mejor = m;
            } else if (Record.TIPO_PESO.equals(m.tipo)) {
                mejor = m;
            }
        }
        return mejor;
    }

    @Override
    public void guardarCompleta(Map<String, Object> cuerpo, Respuesta<SesionEntrenamiento> r) {
        sesionApi.guardarCompleta(cuerpo).enqueue(reenviar(r));
    }

    private static <T> ApiCallback<T> reenviar(Respuesta<T> r) {
        return new ApiCallback<T>() {
            @Override public void onOk(T body) { r.ok(body); }
            @Override public void onFail(int code, String message) { r.fallo(code, message); }
        };
    }
}
