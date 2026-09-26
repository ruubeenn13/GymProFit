package es.pmdm.gymprofit.utils;

// ============================================================
// NavTabs — índices de las 5 pestañas principales de la app.
// La navegación principal es MainActivity + ViewPager2 + Fragments: la barra
// flotante (FloatingNavBar) queda FIJA fuera del pager y su burbuja viaja
// sincronizada con el scroll del pager. Aquí solo viven los índices de pestaña
// y la clave del extra para arrancar MainActivity en una pestaña concreta.
// ============================================================
public final class NavTabs {

    public static final int HOME = 0, RUTINAS = 1, EJERCICIOS = 2, NUTRICION = 3, PERFIL = 4;

    // Pestaña inicial al abrir MainActivity (int con uno de los índices de arriba).
    public static final String EXTRA_TAB = "nav_tab";

    /**
     * Si atrás, desde esta pestaña, vuelve a Inicio en vez de salir de la app (GP-097).
     * En Android la pantalla de inicio es la última antes de salir: desde cualquier otra
     * pestaña atrás lleva a Inicio, y solo desde Inicio se sale.
     */
    public static boolean atrasVuelveAInicio(int tab) {
        return tab != HOME;
    }

    private NavTabs() {}
}
