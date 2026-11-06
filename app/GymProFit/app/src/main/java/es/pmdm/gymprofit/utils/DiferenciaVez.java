package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

// ============================================================
// DiferenciaVez — el chip de «Tus últimas veces» (ficha del ejercicio): cuántos kilos más o menos
// que la sesión anterior (GP-225).
//
// Con la misma marca sale «—», neutro: antes no salía nada y parecía que faltaba el dato. Por
// tiempo o sin sesión anterior, no hay chip. Se redondea al kilo, como lo que se escribe.
// ============================================================
public final class DiferenciaVez {

    /** Qué chip lleva la sesión. */
    public enum Que { NADA, IGUAL, MAS, MENOS }

    private DiferenciaVez() { }

    /**
     * @param kg        kilos de diferencia con la anterior, o null si no hay anterior.
     * @param porTiempo si el ejercicio se mide en segundos.
     */
    public static Que de(@Nullable Double kg, boolean porTiempo) {
        if (porTiempo || kg == null) return Que.NADA;
        long r = Math.round(kg);
        if (r == 0) return Que.IGUAL;
        return r > 0 ? Que.MAS : Que.MENOS;
    }
}
