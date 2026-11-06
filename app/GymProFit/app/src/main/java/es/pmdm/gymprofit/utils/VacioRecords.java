package es.pmdm.gymprofit.utils;

// ============================================================
// VacioRecords — qué dice Récords cuando no hay ninguno (GP-229).
//
// La primera vez que se hace un ejercicio es el punto de partida, no un récord. Sin entrenos se
// dice cómo salen los récords; con entrenos y sin récords, que el punto de partida ya está. Para
// saberlo hace falta la lista de sesiones: mientras no llega se espera, y si falla se dice el
// texto que vale en los dos casos.
// ============================================================
public final class VacioRecords {

    /** Lo que se enseña en el sitio de la lista vacía. */
    public enum Que { CARGANDO, SIN_ENTRENOS, CON_ENTRENOS, SIN_SABER }

    private VacioRecords() { }

    /**
     * @param sesionesLlegadas si la petición de sesiones ya ha respondido (bien o mal).
     * @param sesionesFallo    si esa respuesta fue un fallo.
     * @param sesiones         cuántas sesiones tiene la cuenta (si llegaron bien).
     * @return qué vacío enseñar.
     */
    public static Que de(boolean sesionesLlegadas, boolean sesionesFallo, int sesiones) {
        if (!sesionesLlegadas) return Que.CARGANDO;
        if (sesionesFallo) return Que.SIN_SABER;
        return sesiones > 0 ? Que.CON_ENTRENOS : Que.SIN_ENTRENOS;
    }

    /** El texto de cada vacío (CARGANDO no tiene: se enseña la carga). */
    public static int texto(Que que) {
        switch (que) {
            case SIN_ENTRENOS: return es.pmdm.gymprofit.R.string.records_vacio_sin_entrenos;
            case CON_ENTRENOS: return es.pmdm.gymprofit.R.string.records_vacio_con_entrenos;
            default:           return es.pmdm.gymprofit.R.string.records_vacio_texto;
        }
    }
}
