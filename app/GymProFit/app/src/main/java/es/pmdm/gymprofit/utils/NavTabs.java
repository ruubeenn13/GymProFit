package es.pmdm.gymprofit.utils;

// ============================================================
// NavTabs — las cuatro pestañas de la barra (GP-105) y los extras con los que se
// abre MainActivity en una de ellas.
//
// Inicio · Entrenar · «+» · Nutrición · Progreso. El «+» no es una pestaña: abre las
// acciones rápidas encima de la que esté activa.
// ============================================================
public final class NavTabs {

    public static final int INICIO = 0, ENTRENAR = 1, NUTRICION = 2, PROGRESO = 3;

    /** Número de pestañas. */
    public static final int TOTAL = 4;

    // Pestaña inicial al abrir MainActivity (int con uno de los índices de arriba).
    public static final String EXTRA_TAB = "nav_tab";

    // Sección de Progreso a enseñar (ProgresoFragment.RECORDS…HISTORIAL).
    public static final String EXTRA_SECCION_PROGRESO = "nav_seccion_progreso";

    // Abrir Progreso › Medidas con el diálogo del peso ya abierto (atajo del «+»).
    public static final String EXTRA_ANOTAR_PESO = "nav_anotar_peso";

    /**
     * Si atrás, desde esta pestaña, vuelve a Inicio en vez de salir de la app (GP-097).
     * En Android la pantalla de inicio es la última antes de salir: desde cualquier otra
     * pestaña atrás lleva a Inicio, y solo desde Inicio se sale.
     */
    public static boolean atrasVuelveAInicio(int tab) {
        return tab != INICIO;
    }

    private NavTabs() {}
}
