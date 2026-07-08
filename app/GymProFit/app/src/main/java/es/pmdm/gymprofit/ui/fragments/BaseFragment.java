package es.pmdm.gymprofit.ui.fragments;

import android.content.Context;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
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
