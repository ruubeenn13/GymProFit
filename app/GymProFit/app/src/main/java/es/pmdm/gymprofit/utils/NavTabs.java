package es.pmdm.gymprofit.utils;

import android.app.Activity;
import android.content.Intent;

import es.pmdm.gymprofit.ui.activities.EjerciciosActivity;
import es.pmdm.gymprofit.ui.activities.HomeActivity;
import es.pmdm.gymprofit.ui.activities.NutricionActivity;
import es.pmdm.gymprofit.ui.activities.PerfilActivity;
import es.pmdm.gymprofit.ui.activities.RutinasActivity;

// ============================================================
// NavTabs — navegación entre los 5 destinos principales (barra flotante).
// Centraliza el índice→Activity de la FloatingNavBar para que cada pantalla
// principal solo declare su propio índice y delegue el salto aquí. Mantiene
// la arquitectura solo-Activities (sin ViewPager) con transición instantánea.
// ============================================================
public final class NavTabs {

    public static final int HOME = 0, RUTINAS = 1, EJERCICIOS = 2, NUTRICION = 3, PERFIL = 4;

    // Índice del destino DESDE el que se navega, para que la barra flotante de
    // la pantalla destino haga "viajar" su burbuja desde ahí (efecto visual).
    public static final String EXTRA_FROM = "nav_from_tab";

    private NavTabs() {}

    // Salta al destino `index` desde `from` (que está en el destino `actual`).
    // No hace nada si se selecciona el destino en el que ya se está.
    public static void ir(Activity from, int index, int actual) {
        if (index == actual) return;

        Class<? extends Activity> destino;
        switch (index) {
            case HOME:       destino = HomeActivity.class; break;
            case RUTINAS:    destino = RutinasActivity.class; break;
            case EJERCICIOS: destino = EjerciciosActivity.class; break;
            case NUTRICION:  destino = NutricionActivity.class; break;
            default:         destino = PerfilActivity.class; break;
        }

        Intent intent = new Intent(from, destino);
        intent.putExtra(EXTRA_FROM, actual); // la barra destino hará viajar la burbuja desde aquí
        from.startActivity(intent);
        AnimUtils.transicionTab(from); // fundido entre pestañas
        // Home es la raíz: no se cierra al salir de ella para conservar el back;
        // el resto de destinos sí se cierran (navegación lateral entre iguales).
        if (actual != HOME) from.finish();
    }
}
