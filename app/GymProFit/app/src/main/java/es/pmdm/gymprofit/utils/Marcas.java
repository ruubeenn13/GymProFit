package es.pmdm.gymprofit.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.record.Record;

// ============================================================
// Marcas — cómo se escribe una marca en pantalla (GP-088).
//
// Un solo sitio para «82,5 kg × 5», «80 kg» o «12 reps», usado por la tarjeta de
// Inicio, la pantalla de Récords y el resumen de la sesión. Los formatos están en
// strings.xml, en ES y EN.
// ============================================================
public final class Marcas {

    private Marcas() { }

    /**
     * Kilos sin decimales cuando no aportan: «80 kg» y no «80,0 kg».
     *
     * @param kilos peso en kg
     */
    public static String kilos(Context ctx, double kilos) {
        return kilos == Math.floor(kilos)
                ? ctx.getString(R.string.unidad_kg_entero, (long) kilos)
                : ctx.getString(R.string.unidad_kg_decimal, kilos);
    }

    /**
     * La marca entera: «82,5 kg × 5» si es de peso, «0:45» si es por tiempo, «12 reps» si no.
     */
    public static String texto(Context ctx, Record r) {
        if (r.esDeTiempo()) return tiempo(r.getSegundos());
        return texto(ctx, r.esDePeso(), r.getPeso(), r.getRepeticiones());
    }

    /**
     * Segundos en minutos y segundos: «0:45», «1:05», «12:00» (GP-125). Igual en los dos
     * idiomas, como se lee un cronómetro.
     */
    public static String tiempo(int segundos) {
        int s = Math.max(0, segundos);
        return String.format(java.util.Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    /**
     * La marca que se superó, con su rótulo: «Antes: 80 kg × 5».
     *
     * @return el texto, o {@code null} si es una primera marca y no hay anterior.
     */
    @Nullable
    public static String anterior(Context ctx, Record r) {
        if (!r.tieneAnterior()) return null;
        if (r.esDeTiempo()) return ctx.getString(R.string.record_antes, tiempo(r.getSegundosAnterior()));
        double peso = r.getPesoAnterior() != null ? r.getPesoAnterior() : 0;
        int reps = r.getRepeticionesAnterior() != null ? r.getRepeticionesAnterior() : 0;
        return ctx.getString(R.string.record_antes, texto(ctx, r.esDePeso(), peso, reps));
    }

    /**
     * La marca que se superó, sin rótulo: «80 kg × 5» (GP-105, «Antes: … · hace N días»).
     *
     * @return el texto, o {@code null} si es una primera marca y no hay anterior.
     */
    @Nullable
    public static String anteriorSolo(Context ctx, Record r) {
        if (!r.tieneAnterior()) return null;
        if (r.esDeTiempo()) return tiempo(r.getSegundosAnterior());
        double peso = r.getPesoAnterior() != null ? r.getPesoAnterior() : 0;
        int reps = r.getRepeticionesAnterior() != null ? r.getRepeticionesAnterior() : 0;
        return texto(ctx, r.esDePeso(), peso, reps);
    }

    /**
     * Pone el cronómetro delante de una marca por tiempo, del color del texto y a su
     * tamaño; en las demás, lo quita (la vista se recicla).
     */
    public static void iconoTiempo(android.widget.TextView tv, Record r) {
        if (!r.esDeTiempo()) {
            tv.setCompoundDrawablesRelative(null, null, null, null);
            return;
        }
        android.graphics.drawable.Drawable d = androidx.core.content.ContextCompat.getDrawable(
                tv.getContext(), R.drawable.ic_ms_timer);
        if (d == null) return;
        d = d.mutate();
        int px = Math.round(tv.getTextSize());
        d.setBounds(0, 0, px, px);
        d.setTint(tv.getCurrentTextColor());
        tv.setCompoundDrawablePadding(Math.round(4 * tv.getResources().getDisplayMetrics().density));
        tv.setCompoundDrawablesRelative(d, null, null, null);
    }

    private static String texto(Context ctx, boolean dePeso, double peso, int reps) {
        if (!dePeso) {
            return ctx.getResources().getQuantityString(R.plurals.record_repeticiones, reps, reps);
        }
        return reps > 0
                ? ctx.getString(R.string.record_peso_por_reps, kilos(ctx, peso), reps)
                : kilos(ctx, peso);
    }
}
