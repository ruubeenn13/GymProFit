package es.pmdm.gymprofit.envivo;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.envivo.UltimaVez;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.network.SesionApi;

// ============================================================
// RedSesionEnVivo — las tres llamadas de la sesión en vivo sobre Retrofit. Solo
// traduce: la respuesta pasa tal cual al repositorio, que es quien decide.
// ============================================================
public class RedSesionEnVivo implements SesionEnCursoRepositorio.Red {

    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);

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
