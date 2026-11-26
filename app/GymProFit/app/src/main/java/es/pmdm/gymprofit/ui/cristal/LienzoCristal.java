package es.pmdm.gymprofit.ui.cristal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// LienzoCristal — lo que hay detrás del cristal vivo (GP-285, DEC-070).
//
// Un FrameLayout que, además de dibujar a sus hijos, se los guarda en un RenderNode. El
// cristal vivo (CristalVivo), que va encima y fuera de él, vuelve a dibujar ese nodo
// desenfocado en su hueco. Guardarlo no copia píxeles: el nodo apunta a las listas de dibujo
// de los hijos, que Android ya mantiene, así que cuando el contenido se desplaza no hay que
// volver a grabar nada ni invalidar el cristal: lo recompone el hilo de dibujo.
//
// Solo graba donde hace falta: Android 12 o más, con algún cristal apuntado y un nivel que
// desenfoque. En el nivel 3 (y en Android 11 o menos) es un FrameLayout sin más.
//
// Un CristalVivo no puede ir dentro de su lienzo: se dibujaría a sí mismo.
// ============================================================
public class LienzoCristal extends FrameLayout {

    private final List<CristalVivo> cristales = new ArrayList<>();
    @NonNull private NivelCristal nivel;
    // El nodo con lo grabado; solo existe en Android 12 o más.
    @Nullable private final Captura captura;

    public LienzoCristal(@NonNull Context context) {
        this(context, null);
    }

    public LienzoCristal(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        nivel = NivelCristal.actual(context);
        captura = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? new Captura() : null;
    }

    /** El nivel del cristal vivo que se apoya en este lienzo. */
    @NonNull
    public NivelCristal nivel() {
        return nivel;
    }

    /**
     * Vuelve a mirar el nivel (al volver a la pantalla: el alto contraste puede haber cambiado,
     * o el rendimiento haber bajado el techo) y avisa a los cristales si es otro.
     */
    public void revisarNivel() {
        NivelCristal ahora = NivelCristal.actual(getContext());
        if (ahora == nivel) return;
        nivel = ahora;
        for (CristalVivo c : cristales) c.alCambiarNivel(nivel);
        invalidate();
    }

    void apuntar(@NonNull CristalVivo cristal) {
        if (!cristales.contains(cristal)) cristales.add(cristal);
        invalidate();
    }

    void borrar(@NonNull CristalVivo cristal) {
        cristales.remove(cristal);
    }

    /** Lo grabado, o null si este lienzo no graba (Android 11 o menos). */
    @Nullable
    Captura captura() {
        return captura;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && captura != null && nivel != NivelCristal.OPACO
                && !cristales.isEmpty() && canvas.isHardwareAccelerated()) {
            captura.grabar(this, canvas);
        } else {
            super.dispatchDraw(canvas);
        }
    }

    // Para Captura: dibujar a los hijos en el lienzo de grabación.
    void dibujarHijos(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    /** El contenido del lienzo en un RenderNode. Clase aparte: RenderNode no existe antes de Android 10. */
    @RequiresApi(Build.VERSION_CODES.S)
    static final class Captura {
        final RenderNode nodo = new RenderNode("LienzoCristal");

        void grabar(@NonNull LienzoCristal lienzo, @NonNull Canvas destino) {
            nodo.setPosition(0, 0, lienzo.getWidth(), lienzo.getHeight());
            RecordingCanvas grabacion = nodo.beginRecording();
            try {
                lienzo.dibujarHijos(grabacion);
            } finally {
                nodo.endRecording();
            }
            destino.drawRenderNode(nodo);
        }

        boolean lista() {
            return nodo.hasDisplayList();
        }
    }
}
