package es.pmdm.gymprofit.utils;

// ============================================================
// VistaKilos — qué enseña «Kilos movidos» de Progreso (GP-216, lote 4) según lo que ha llegado de
// las dos peticiones que lo deciden: la lista de sesiones y los kilos por semana.
//
// Las dos salen a la vez y llegan en cualquier orden. La cuenta sin ningún entreno terminado ve
// su vacío, nunca ocho barras a cero: las barras de una cuenta así son todas cero, así que hasta
// saber si tiene entrenos no se pinta ninguna, y cuando lo sabe, el vacío gana a lo que hayan
// traído los kilos (8311d31: los kilos llegaban después y se pintaban encima del vacío).
// «Todo» necesita además la primera sesión para saber cuántas semanas pedir.
// Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class VistaKilos {

    /** En qué punto está una petición. */
    public enum Llegada { PENDIENTE, OK, FALLO }

    /** Lo que enseña la sección. */
    public enum Que {
        /** Cargando (si ya había barras, se quedan hasta que llegue lo nuevo). */
        CARGANDO,
        /** «Todo» sin la lista de sesiones: el fallo de la lista, con su «Reintentar». */
        ERROR_SESIONES,
        /** La cuenta no tiene ningún entreno terminado: el vacío, sin selector. */
        VACIO_CUENTA_NUEVA,
        /** Falló la petición de los kilos. */
        ERROR_KILOS,
        /** La API no mandó ninguna semana. */
        VACIO,
        /** Las barras y la cifra de esta semana. */
        BARRAS
    }

    private VistaKilos() { }

    /**
     * @param todo         el período elegido es «Todo».
     * @param sesiones     cómo va la lista de sesiones.
     * @param sinEntrenos  la lista llegó y no tiene ninguna sesión terminada (solo cuenta con OK).
     * @param kilos        cómo va la petición de kilos del período elegido.
     * @param sinSemanas   los kilos llegaron sin ninguna semana (solo cuenta con OK).
     */
    public static Que decidir(boolean todo, Llegada sesiones, boolean sinEntrenos,
                              Llegada kilos, boolean sinSemanas) {
        if (sesiones == Llegada.OK && sinEntrenos) return Que.VACIO_CUENTA_NUEVA;
        // Sin la lista no se sabe si es una cuenta nueva (ni, con «Todo», desde cuándo pedir).
        if (sesiones == Llegada.PENDIENTE) return Que.CARGANDO;
        if (todo && sesiones == Llegada.FALLO) return Que.ERROR_SESIONES;
        if (kilos == Llegada.PENDIENTE) return Que.CARGANDO;
        if (kilos == Llegada.FALLO) return Que.ERROR_KILOS;
        return sinSemanas ? Que.VACIO : Que.BARRAS;
    }

    /** Si ya se pueden pedir los kilos: con «Todo» hace falta la lista, y a una cuenta nueva no se le piden. */
    public static boolean pedirKilos(boolean todo, Llegada sesiones, boolean sinEntrenos) {
        if (sesiones == Llegada.OK && sinEntrenos) return false;
        return !todo || sesiones == Llegada.OK;
    }
}
