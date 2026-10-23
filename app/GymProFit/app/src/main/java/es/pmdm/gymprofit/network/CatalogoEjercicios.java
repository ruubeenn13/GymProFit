package es.pmdm.gymprofit.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.ejercicio.Ejercicio;

// ============================================================
// CatalogoEjercicios — el catálogo de ejercicios activos, pedido una vez y guardado en memoria
// mientras viva el proceso (GP-216: Entrenar lo usa para los músculos de cada rutina y el
// detalle de rutina para las fotos). Son unos 870 ejercicios en una sola respuesta; pedirla en
// cada pantalla era el gasto más grande de abrir una rutina.
// Si dos pantallas lo piden a la vez, la segunda espera a la primera y no repite la llamada.
// ============================================================
public final class CatalogoEjercicios {

    /** Quien lo pide: el catálogo por id, o null si no ha podido cargarse. */
    public interface AlTenerlo {
        void listo(@Nullable Map<Integer, Ejercicio> porId);
    }

    private static Map<Integer, Ejercicio> cache;
    private static final List<AlTenerlo> esperando = new ArrayList<>();
    private static boolean pidiendo;

    private CatalogoEjercicios() { }

    /** Lo que haya ya en memoria, o null. */
    @Nullable
    public static synchronized Map<Integer, Ejercicio> enMemoria() {
        return cache;
    }

    /** Lo entrega ya si está en memoria; si no, lo pide a la API y avisa a todos los que esperan. */
    public static void cargar(@NonNull AlTenerlo alTenerlo) {
        synchronized (CatalogoEjercicios.class) {
            if (cache != null) {
                alTenerlo.listo(cache);
                return;
            }
            esperando.add(alTenerlo);
            if (pidiendo) return;
            pidiendo = true;
        }
        ApiClient.service(EjercicioApi.class).getActivos().enqueue(new ApiCallback<List<Ejercicio>>() {
            @Override public void onOk(List<Ejercicio> lista) {
                Map<Integer, Ejercicio> m = new HashMap<>();
                if (lista != null) for (Ejercicio e : lista) m.put(e.getId(), e);
                terminar(m);
            }
            @Override public void onFail(int code, String message) {
                // No se guarda: la próxima pantalla lo vuelve a intentar. Quien espera se entera con null.
                terminar(null);
            }
        });
    }

    private static void terminar(@Nullable Map<Integer, Ejercicio> m) {
        List<AlTenerlo> avisar;
        synchronized (CatalogoEjercicios.class) {
            if (m != null) cache = m;
            pidiendo = false;
            avisar = new ArrayList<>(esperando);
            esperando.clear();
        }
        for (AlTenerlo a : avisar) a.listo(m);
    }
}
