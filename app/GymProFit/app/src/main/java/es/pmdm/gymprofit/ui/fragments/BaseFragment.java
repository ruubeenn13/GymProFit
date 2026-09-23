package es.pmdm.gymprofit.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.activities.BaseActivity;
import es.pmdm.gymprofit.ui.widget.FloatingNavBar;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// BaseFragment — clase base de las 5 pestañas principales (Home/Rutinas/
// Ejercicios/Nutrición/Perfil) hospedadas en MainActivity + ViewPager2.
// Centraliza el acceso a preferencias, el guard de usuario registrado, el
// helper findViewById sobre la vista del fragment (para reutilizar el código
// que venía de las Activities sin cambiar cada llamada) y la delegación del
// botón de menú de opciones a la BaseActivity anfitriona.
// ============================================================
public abstract class BaseFragment extends Fragment {

    protected PreferencesManager prefsManager;

    // Prepara el gestor de preferencias en cuanto el fragment se asocia al host.
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        prefsManager = new PreferencesManager(context);
    }

    // La barra flotante se dibuja ENCIMA del contenido: sin reservar hueco, lo que
    // cae en sus últimos 86 dp no se puede leer ni pulsar. Se añade ese hueco al
    // padding inferior del scroller principal de la pestaña, y la medida sale de la
    // propia barra (FloatingNavBar.espacioReservado), no de un número escrito aquí:
    // así las cinco pestañas se enteran a la vez si la barra cambia de alto.
    //
    // clipToPadding="false" acompaña al padding, no lo sustituye: deja que el
    // contenido siga viéndose difuminado bajo el cristal mientras se desplaza, pero
    // el sitio lo reserva el padding.
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reservarHuecoBarraFlotante(view);
    }

    // Aplica la reserva al primer scroller vertical del árbol. Se marca la vista con
    // un tag para no sumar el hueco dos veces si la vista se reutiliza.
    private void reservarHuecoBarraFlotante(@NonNull View raiz) {
        View scroller = buscarScrollerVertical(raiz);
        if (scroller == null) return;
        if (Boolean.TRUE.equals(scroller.getTag(R.id.tag_hueco_barra_reservado))) return;

        int reserva = FloatingNavBar.espacioReservado(requireContext());
        scroller.setPadding(scroller.getPaddingLeft(), scroller.getPaddingTop(),
                scroller.getPaddingRight(), scroller.getPaddingBottom() + reserva);
        if (scroller instanceof ViewGroup) ((ViewGroup) scroller).setClipToPadding(false);
        scroller.setTag(R.id.tag_hueco_barra_reservado, Boolean.TRUE);
    }

    // Primer contenedor con scroll VERTICAL del árbol (ScrollView/NestedScrollView/
    // RecyclerView). Se excluyen los horizontales (chips) porque no son subclases de
    // ScrollView y el RecyclerView principal de las listas siempre es vertical.
    private View buscarScrollerVertical(View v) {
        if (v instanceof android.widget.ScrollView
                || v instanceof androidx.core.widget.NestedScrollView
                || v instanceof androidx.recyclerview.widget.RecyclerView) {
            return v;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View found = buscarScrollerVertical(g.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    // findViewById sobre la vista del fragment: permite reutilizar tal cual el
    // código de referencia de vistas que venía de las Activities.
    protected <T extends View> T findViewById(@IdRes int id) {
        View v = getView();
        return v != null ? v.findViewById(id) : null;
    }

    // Muestra el toast de "solo usuarios registrados" si el usuario es invitado.
    protected boolean verificarAccesoRegistrado() {
        if (prefsManager.isGuest()) {
            UIHelper.mostrarToastError(requireActivity(), getString(R.string.error_solo_usuarios_registrados));
            return false;
        }
        return true;
    }

    // Vincula el botón de menú de opciones (si existe en el layout) delegando en
    // la BaseActivity anfitriona (tema/idioma/contacto/cerrar sesión).
    protected void setupMenuButton() {
        View btn = findViewById(R.id.btnMenuOpciones);
        if (btn != null) {
            btn.setOnClickListener(v -> ((BaseActivity) requireActivity()).mostrarMenuOpciones(v));
        }
    }

    // Cambia a otra pestaña principal del pager (viaje de burbuja incluido).
    protected void irATab(int index) {
        androidx.fragment.app.FragmentActivity act = getActivity();
        if (act instanceof es.pmdm.gymprofit.ui.activities.MainActivity) {
            ((es.pmdm.gymprofit.ui.activities.MainActivity) act).irATab(index);
        }
    }
}
