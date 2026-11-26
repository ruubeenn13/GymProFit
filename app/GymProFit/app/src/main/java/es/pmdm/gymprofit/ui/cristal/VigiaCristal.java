package es.pmdm.gymprofit.ui.cristal;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.FrameMetrics;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// VigiaCristal — mide los fotogramas de una pantalla con cristal vivo y, si el móvil no lo
// aguanta, lo baja de nivel (GP-285, DEC-070).
//
// Android cuenta lo que tarda cada fotograma de la ventana (FrameMetrics); aquí se le pasa a
// RendimientoCristal, que es quien decide. Cuando pide bajar, se guarda el nivel siguiente como
// techo de este móvil (PreferencesManager) y el lienzo se lo dice a sus cristales: de la
// refracción al desenfoque, y del desenfoque a la superficie opaca. No vuelve a subir.
//
// Se mide lo que tarda el DIBUJO del fotograma —mandar las órdenes a la GPU y que la GPU las
// ejecute—, que es donde cae el desenfoque, y no su duración total. En el total entran las
// esperas por un búfer libre y los retrasos del hilo principal, que no son del cristal: medido
// en el emulador de API 36, con la superficie opaca (sin desenfoque) ya había ventanas con 87
// de 120 fotogramas por encima de 33 ms de total, y el dibujo solo pasaba de ahí en 0 a 7, lo
// mismo en los tres niveles (documentacion/estado/2026-11-26-gp285-liquid-glass-lote1.md).
//
// Solo escucha mientras la pantalla está delante (empezar en onResume, parar en onPause) y
// mientras hay algo que bajar: en el nivel 3 no hace nada. Las medidas llegan a un hilo propio,
// no al principal: contar no puede costarle fotogramas a lo que se cuenta.
// ============================================================
public final class VigiaCristal {

    private final Activity actividad;
    private final LienzoCristal lienzo;
    private final RendimientoCristal rendimiento = new RendimientoCristal();
    private final Handler principal = new Handler(Looper.getMainLooper());
    // El oyente de FrameMetrics y el hilo al que llegan sus medidas; solo existen mientras se
    // escucha (Android 12 o más).
    private Object oyente;
    private HandlerThread hilo;

    public VigiaCristal(@NonNull Activity actividad, @NonNull LienzoCristal lienzo) {
        this.actividad = actividad;
        this.lienzo = lienzo;
    }

    /** Empieza a medir, si el nivel de ahora desenfoca (lo que ya implica Android 12 o más). */
    public void empezar() {
        if (oyente != null || lienzo.nivel() == NivelCristal.OPACO) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        hilo = new HandlerThread("VigiaCristal");
        hilo.start();
        oyente = Api31.escuchar(this, new Handler(hilo.getLooper()));
    }

    /** Deja de medir. */
    public void parar() {
        if (oyente == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Api31.dejar(this);
        oyente = null;
        hilo.quitSafely();
        hilo = null;
    }

    // Lo que tardó en dibujarse un fotograma, en nanosegundos. En el hilo del vigía.
    private void fotograma(long dibujoNs) {
        if (rendimiento.fotograma(dibujoNs)) principal.post(this::bajar);
    }

    // En el hilo principal: un nivel menos, guardado para siempre en este móvil.
    private void bajar() {
        if (oyente == null) return;
        NivelCristal menos = lienzo.nivel().unoMenos();
        new PreferencesManager(actividad).saveCristalTecho(menos.numero);
        lienzo.revisarNivel();
        if (lienzo.nivel() == NivelCristal.OPACO) parar();
    }

    /** Clase aparte: la duración de la GPU es de API 31. */
    @RequiresApi(Build.VERSION_CODES.S)
    private static final class Api31 {
        static Object escuchar(VigiaCristal vigia, Handler enSuHilo) {
            Window.OnFrameMetricsAvailableListener o = (ventana, metricas, perdidas) -> {
                // El primer fotograma de una ventana carga con inflarlo todo: no dice nada del cristal.
                if (metricas.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1) return;
                vigia.fotograma(metricas.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION)
                        + metricas.getMetric(FrameMetrics.GPU_DURATION));
            };
            vigia.actividad.getWindow().addOnFrameMetricsAvailableListener(o, enSuHilo);
            return o;
        }

        static void dejar(VigiaCristal vigia) {
            vigia.actividad.getWindow().removeOnFrameMetricsAvailableListener(
                    (Window.OnFrameMetricsAvailableListener) vigia.oyente);
        }
    }
}
