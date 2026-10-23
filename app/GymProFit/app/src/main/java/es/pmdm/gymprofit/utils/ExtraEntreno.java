package es.pmdm.gymprofit.utils;

// ============================================================
// ExtraEntreno — la línea de color de cada entreno en «Tus últimos entrenos» (GP-216,
// tablero 13): «2 récords» en dorado, «+4 % que la anterior» en verde y «Sin récords» o una
// bajada en neutro. Bajar no se pinta de rojo (DEC-055): una sesión más floja no es un fallo.
// Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class ExtraEntreno {

    /** Qué dice la línea. */
    public enum Tipo { RECORDS, SUBE, BAJA, SIN_RECORDS }

    public final Tipo tipo;
    /** Récords, o el porcentaje sin signo. */
    public final int n;

    private ExtraEntreno(Tipo tipo, int n) {
        this.tipo = tipo;
        this.n = n;
    }

    /**
     * @param records        récords batidos en esa sesión.
     * @param kilos          los kilos de la sesión.
     * @param kilosAnterior  los de la sesión anterior de la misma rutina; 0 o menos si no la hay.
     */
    public static ExtraEntreno de(int records, double kilos, double kilosAnterior) {
        if (records > 0) return new ExtraEntreno(Tipo.RECORDS, records);
        if (kilos <= 0 || kilosAnterior <= 0) return new ExtraEntreno(Tipo.SIN_RECORDS, 0);
        double cambio = (kilos - kilosAnterior) / kilosAnterior * 100.0;
        int pct = (int) Math.round(Math.abs(cambio));
        // Una diferencia que redondea a 0 % es «igual» (DEC-055).
        if (pct == 0) return new ExtraEntreno(Tipo.SIN_RECORDS, 0);
        return new ExtraEntreno(cambio > 0 ? Tipo.SUBE : Tipo.BAJA, pct);
    }
}
