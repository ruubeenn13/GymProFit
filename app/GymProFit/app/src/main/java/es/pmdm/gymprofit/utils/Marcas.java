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
     * La marca entera: «82,5 kg × 5» si es de peso, «12 reps» si no.
     */
    public static String texto(Context ctx, Record r) {
        return texto(ctx, r.esDePeso(), r.getPeso(), r.getRepeticiones());
    }

    /**
     * La marca que se superó, con su rótulo: «Antes: 80 kg × 5».
     *
     * @return el texto, o {@code null} si es una primera marca y no hay anterior.
     */
    @Nullable
    public static String anterior(Context ctx, Record r) {
        if (!r.tieneAnterior()) return null;
        double peso = r.getPesoAnterior() != null ? r.getPesoAnterior() : 0;
        int reps = r.getRepeticionesAnterior() != null ? r.getRepeticionesAnterior() : 0;
        return ctx.getString(R.string.record_antes, texto(ctx, r.esDePeso(), peso, reps));
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
