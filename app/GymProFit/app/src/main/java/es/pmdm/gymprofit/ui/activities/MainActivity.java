package es.pmdm.gymprofit.ui.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.fragments.EjerciciosFragment;
import es.pmdm.gymprofit.ui.fragments.HomeFragment;
import es.pmdm.gymprofit.ui.fragments.NutricionFragment;
import es.pmdm.gymprofit.ui.fragments.PerfilFragment;
import es.pmdm.gymprofit.ui.fragments.RutinasFragment;
import es.pmdm.gymprofit.ui.widget.FloatingNavBar;
import es.pmdm.gymprofit.utils.NavTabs;

// ============================================================
// MainActivity — anfitriona de la navegación principal por pestañas.
// Aloja un ViewPager2 con los 5 Fragments (Home/Rutinas/Ejercicios/Nutrición/
// Perfil) y una FloatingNavBar FIJA. Sincroniza la burbuja de la barra con el
// scroll del pager para que "viaje" de forma continua al cambiar de pestaña,
// tanto al pulsar/arrastrar la barra como al deslizar el contenido.
// ============================================================
public class MainActivity extends BaseActivity {

    // Clave para recordar la pestaña activa al recrearse la Activity (cambio de tema/idioma).
    private static final String KEY_TAB = "gpf_tab_activa";

    private ViewPager2 pager;
    private FloatingNavBar nav;
    // true mientras el usuario ARRASTRA el contenido (swipe manual). Solo entonces la
    // burbuja debe seguir el scroll del pager; en los cambios programáticos (pulsar una
    // pestaña, irATab) la burbuja viaja con su propia animación para no depender del
    // salto interno de ViewPager2 en distancias >3 (que la haría empezar en el medio).
    private boolean swiping = false;

    // Infla el contenedor, monta el pager con los 5 fragments, engancha la barra
    // flotante al scroll del pager y abre la pestaña indicada en el intent.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = findViewById(R.id.pager);
        nav   = findViewById(R.id.floatingNav);

        pager.setAdapter(new TabsAdapter(this));
        // Mantiene vivas las 5 pestañas (sin re-inflar al cambiar); cada una
        // recarga sus datos en onResume al volverse visible.
        pager.setOffscreenPageLimit(4);

        // Transición shared-axis entre pestañas: al deslizar/cambiar, las páginas se
        // atenúan un poco mientras se desplazan (fade + slide), no un corte seco.
        pager.setPageTransformer((page, pos) ->
                page.setAlpha(1f - Math.min(1f, Math.abs(pos) * 0.5f)));

        // Barra → pager: al pulsar un destino la página cambia CON animación. El viaje
        // de la píldora lo conduce la propia FloatingNavBar (setActiveFrom en el tap).
        nav.setOnTabSelectedListener(this::animarPager);

        // Pager → barra: SOLO en el swipe manual la burbuja sigue el scroll (viaje
        // continuo) y se asienta en la pestaña destino cuando el desplazamiento reposa.
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float offset, int offsetPx) {
                if (swiping) nav.followScroll(position + offset);
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    swiping = true;
                } else if (state == ViewPager2.SCROLL_STATE_IDLE && swiping) {
                    swiping = false;
                    nav.setActiveIndex(pager.getCurrentItem());
                }
            }
        });

        // Pestaña inicial: si venimos de un recreate (p.ej. cambio de tema) se restaura
        // la que estaba activa; si no, la del intent (por defecto Home). Sin animación.
        int tabPorDefecto = getIntent().getIntExtra(NavTabs.EXTRA_TAB, NavTabs.HOME);
        int tab = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_TAB, tabPorDefecto) : tabPorDefecto;
        pager.setCurrentItem(tab, false);
        nav.setActiveIndex(tab);

        pedirPermisoNotificaciones();
    }

    // Recuerda la pestaña activa para restaurarla tras un recreate (cambio de tema/idioma).
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pager != null) outState.putInt(KEY_TAB, pager.getCurrentItem());
    }

    // Cambia de pestaña programáticamente (Home, notificaciones): página ANIMADA +
    // viaje suave de la píldora desde la pestaña actual hasta el destino.
    public void irATab(int index) {
        if (pager == null) return;
        int from = pager.getCurrentItem();
        animarPager(index);
        nav.setActiveFrom(from, index);
    }

    // Anima el cambio de página del pager. En saltos largos (>1 pestaña) se salta al
    // instante hasta el vecino del destino y solo se anima el ÚLTIMO tramo: así hay
    // deslizamiento visible sin recorrer (ni parpadear) todas las páginas intermedias.
    private void animarPager(int index) {
        if (pager == null || index == pager.getCurrentItem()) return;
        int cur = pager.getCurrentItem();
        if (Math.abs(index - cur) > 1) {
            pager.setCurrentItem(index > cur ? index - 1 : index + 1, false);
            pager.post(() -> pager.setCurrentItem(index, true));
        } else {
            pager.setCurrentItem(index, true);
        }
    }

    // Solicita el permiso de notificaciones (Android 13+) una sola vez al entrar.
    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
        }
    }

    // Adapter del pager: instancia el fragment correspondiente a cada índice.
    private static final class TabsAdapter extends FragmentStateAdapter {
        TabsAdapter(FragmentActivity fa) { super(fa); }

        @Override public int getItemCount() { return 5; }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case NavTabs.RUTINAS:    return new RutinasFragment();
                case NavTabs.EJERCICIOS: return new EjerciciosFragment();
                case NavTabs.NUTRICION:  return new NutricionFragment();
                case NavTabs.PERFIL:     return new PerfilFragment();
                default:                 return new HomeFragment();
            }
        }
    }
}
