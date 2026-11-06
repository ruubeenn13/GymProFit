package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

// ============================================================
// ComparacionSesion — la sesión frente a la anterior de la misma rutina (GP-232).
//
// En la tarjeta: «3 % más que la anterior» en verde, «2 % menos que la anterior» en neutro
// (bajar no se pinta de rojo, DEC-055), «Igual que la anterior» si redondea a 0 % y «Primera
// vez con esta rutina» la primera vez. Un entrenamiento libre no se compara, ni una sesión sin
// kilos (sin ejercicios o sin pesos) ni una cuya anterior no tenía kilos: no hay porcentaje
// que valga. Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class ComparacionSesion {

    /** Qué dice la línea. */
    public enum Tipo { PRIMERA, SUBE, BAJA, IGUAL }

    public final Tipo tipo;
    /** El porcentaje sin signo; 0 en PRIMERA e IGUAL. */
    public final int porcentaje;

    private ComparacionSesion(Tipo tipo, int porcentaje) {
        this.tipo = tipo;
        this.porcentaje = porcentaje;
    }

    /**
     * @param libre         true si la sesión no tiene rutina.
     * @param kilos         los de la sesión.
     * @param kilosAnterior los de la anterior de la misma rutina; null si es la primera vez.
     * @return la comparación, o null si no se compara.
     */
    @Nullable
    public static ComparacionSesion de(boolean libre, double kilos, @Nullable Double kilosAnterior) {
        if (libre || kilos <= 0) return null;
        if (kilosAnterior == null) return new ComparacionSesion(Tipo.PRIMERA, 0);
        if (kilosAnterior <= 0) return null;
        double cambio = (kilos - kilosAnterior) / kilosAnterior * 100.0;
        int pct = (int) Math.round(Math.abs(cambio));
        if (pct == 0) return new ComparacionSesion(Tipo.IGUAL, 0);
        return new ComparacionSesion(cambio > 0 ? Tipo.SUBE : Tipo.BAJA, pct);
    }
}
