package es.pmdm.gymprofit.utils;

// ============================================================
// CuandoTocaRutina — «cuándo toca» cada rutina del programa en Entrenar (GP-216, tablero 14).
// Los programas son cíclicos y la API manda las rutinas en el orden en que tocan desde hoy: la
// primera es la de hoy, la segunda la siguiente y, a partir de ahí, «en N entrenos». No se dice
// «mañana» ni un día de la semana: no se sabe qué días entrenará la persona. Sin dependencias
// de Android.
// ============================================================
public final class CuandoTocaRutina {

    /** Qué se dice. */
    public enum Tipo { HOY, SIGUIENTE, EN_N }

    public final Tipo tipo;
    /** Cuántos entrenos faltan; solo en EN_N. */
    public final int n;

    private CuandoTocaRutina(Tipo tipo, int n) {
        this.tipo = tipo;
        this.n = n;
    }

    /** @param indice posición de la rutina en la lista que manda la API (0 = la de hoy). */
    public static CuandoTocaRutina de(int indice) {
        if (indice <= 0) return new CuandoTocaRutina(Tipo.HOY, 0);
        if (indice == 1) return new CuandoTocaRutina(Tipo.SIGUIENTE, 1);
        return new CuandoTocaRutina(Tipo.EN_N, indice);
    }
}
