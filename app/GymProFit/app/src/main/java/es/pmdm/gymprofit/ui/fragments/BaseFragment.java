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

    // La barra flotante se solapa con el contenido: reserva espacio inferior en el
    // scroller principal para que el último elemento no quede oculto tras el cristal.
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View scroller = buscarScrollerVertical(view);
        if (scroller != null) {
            int reserva = Math.round(96 * getResources().getDisplayMetrics().density);
            scroller.setPadding(scroller.getPaddingLeft(), scroller.getPaddingTop(),
                    scroller.getPaddingRight(), scroller.getPaddingBottom() + reserva);
            if (scroller instanceof ViewGroup) ((ViewGroup) scroller).setClipToPadding(false);
        }
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
