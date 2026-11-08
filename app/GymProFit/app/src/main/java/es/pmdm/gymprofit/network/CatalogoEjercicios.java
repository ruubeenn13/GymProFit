package es.pmdm.gymprofit.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.utils.CachePorIdioma;

// ============================================================
// CatalogoEjercicios — el catálogo de ejercicios activos, pedido una vez y guardado en memoria
// mientras viva el proceso (GP-216: Entrenar lo usa para los músculos de cada rutina y el
// detalle de rutina para las fotos). Son unos 870 ejercicios en una sola respuesta; pedirla en
// cada pantalla era el gasto más grande de abrir una rutina.
// Si dos pantallas lo piden a la vez, la segunda espera a la primera y no repite la llamada.
// Se guarda con el idioma en que se pidió (GP-251): cambiar de idioma no reinicia el proceso, y el
// detalle de una rutina sacaba los nombres del catálogo pedido en español con la app en inglés.
// ============================================================
public final class CatalogoEjercicios {

    /** Quien lo pide: el catálogo por id, o null si no ha podido cargarse. */
    public interface AlTenerlo {
        void listo(@Nullable Map<Integer, Ejercicio> porId);
    }

    private static final CachePorIdioma<Map<Integer, Ejercicio>> cache = new CachePorIdioma<>();
    private static final List<AlTenerlo> esperando = new ArrayList<>();
    // El idioma de la petición en vuelo, o null si no hay ninguna.
    @Nullable private static String pidiendo;

    private CatalogoEjercicios() { }

    /** Lo que haya ya en memoria en el idioma de ahora, o null. */
    @Nullable
    public static synchronized Map<Integer, Ejercicio> enMemoria() {
        return cache.get(ApiClient.idiomaDePeticiones());
    }

    /** Lo entrega ya si está en memoria; si no, lo pide a la API y avisa a todos los que esperan. */
    public static void cargar(@NonNull AlTenerlo alTenerlo) {
        String idioma = ApiClient.idiomaDePeticiones();
        synchronized (CatalogoEjercicios.class) {
            Map<Integer, Ejercicio> guardado = cache.get(idioma);
            if (guardado != null) {
                alTenerlo.listo(guardado);
                return;
            }
            esperando.add(alTenerlo);
            // Una en vuelo en otro idioma entrega lo suyo a quien ya esperaba; esta pide la suya.
            if (idioma.equals(pidiendo)) return;
            pidiendo = idioma;
        }
        ApiClient.service(EjercicioApi.class).getActivos().enqueue(new ApiCallback<List<Ejercicio>>() {
            @Override public void onOk(List<Ejercicio> lista) {
                Map<Integer, Ejercicio> m = new HashMap<>();
                if (lista != null) for (Ejercicio e : lista) m.put(e.getId(), e);
                terminar(idioma, m);
            }
            @Override public void onFail(int code, String message) {
                // No se guarda: la próxima pantalla lo vuelve a intentar. Quien espera se entera con null.
                terminar(idioma, null);
            }
        });
    }

    private static void terminar(@NonNull String idioma, @Nullable Map<Integer, Ejercicio> m) {
        List<AlTenerlo> avisar;
        synchronized (CatalogoEjercicios.class) {
            if (m != null) cache.put(idioma, m);
            if (idioma.equals(pidiendo)) pidiendo = null;
            avisar = new ArrayList<>(esperando);
            esperando.clear();
        }
        for (AlTenerlo a : avisar) a.listo(m);
    }
}
