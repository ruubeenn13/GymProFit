package es.pmdm.gymprofit.utils;

// ============================================================
// ParaRecuperar — la regla de «Para recuperar» del resumen del entreno (GP-210 paso 1,
// decisión 12 del lienzo de nutrición), en lógica pura para poder probarla.
//
// Al acabar de entrenar, si a la proteína del día le faltan 20 g o más, el resumen enseña
// lo que falta y tres ideas para apuntar. Los 20 g se cuentan como los enseña la pantalla
// (redondeados). Los objetivos del día no cambian nunca: lo que se apunta desde aquí solo
// suma a lo comido, y la barra se acerca al objetivo de siempre.
// ============================================================
public final class ParaRecuperar {

    /** Gramos de proteína que tienen que faltar para que la sección salga. */
    public static final int FALTA_MINIMA = 20;

    /** Gramos de proteína que tiene que aportar una idea para proponerla: una lechuga no recupera nada. */
    public static final int PROTEINA_MINIMA_IDEA = 10;

    private ParaRecuperar() { }

    /** ¿Aporta una idea la proteína suficiente para proponerla en «Para recuperar»? */
    public static boolean sirveDeIdea(double proteinaGramos) {
        return proteinaGramos >= PROTEINA_MINIMA_IDEA;
    }

    /**
     * ¿Sale la sección?
     *
     * @param objetivoProteinas objetivo del día en gramos; ≤ 0 si no hay.
     * @param proteinas         proteína ya apuntada hoy, en gramos.
     * @param sinObjetivo       «Prefiero no decirlo» (GP-103): no hay objetivo que completar.
     */
    public static boolean sale(int objetivoProteinas, double proteinas, boolean sinObjetivo) {
        if (sinObjetivo || objetivoProteinas <= 0) return false;
        return falta(objetivoProteinas, proteinas) >= FALTA_MINIMA;
    }

    /** Gramos que faltan, redondeados como los enseña la pantalla; nunca negativo. */
    public static int falta(int objetivoProteinas, double proteinas) {
        return (int) Math.max(0, Math.round(objetivoProteinas - proteinas));
    }

    /** ¿Se llegó al objetivo de proteína del día? */
    public static boolean conseguida(int objetivoProteinas, double proteinas) {
        return objetivoProteinas > 0 && proteinas >= objetivoProteinas;
    }
}
