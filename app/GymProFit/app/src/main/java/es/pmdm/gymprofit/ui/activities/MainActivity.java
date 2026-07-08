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

        // Barra → pager: al pulsar un destino se cambia de pantalla al INSTANTE (sin
        // animación de VP2 → sin salto interno ni flash de la pantalla intermedia). El
        // viaje de la burbuja lo conduce la propia FloatingNavBar (setActiveFrom).
        nav.setOnTabSelectedListener(index -> pager.setCurrentItem(index, false));

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

        // Pestaña inicial (por defecto Home). Sin animación en el arranque.
        int tab = getIntent().getIntExtra(NavTabs.EXTRA_TAB, NavTabs.HOME);
        pager.setCurrentItem(tab, false);
        nav.setActiveIndex(tab);

        pedirPermisoNotificaciones();
    }

    // Cambia de pestaña programáticamente (Home, notificaciones): swap instantáneo de
    // pantalla + viaje suave de la burbuja desde la pestaña actual hasta el destino.
    public void irATab(int index) {
        if (pager == null) return;
        int from = pager.getCurrentItem();
        pager.setCurrentItem(index, false);
        nav.setActiveFrom(from, index);
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
