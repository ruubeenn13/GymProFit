package es.pmdm.gymprofit.ui.fragments;

import android.content.Context;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.activities.MainActivity;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// BaseFragment — clase base de las pestañas principales (Inicio, Entrenar,
// Nutrición, Progreso) y de las secciones de Progreso.
// Centraliza el acceso a preferencias, el guard de usuario registrado, el helper
// findViewById sobre la vista del fragment y el cambio de pestaña.
//
// Desde GP-105 la barra ya no flota sobre el contenido: va debajo del pager, así
// que las pestañas no reservan hueco para ella (lo hacía reservarHuecoBarraFlotante).
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

    /** La MainActivity anfitriona, o null si el fragment vive en otra pantalla. */
    @Nullable
    protected MainActivity main() {
        return getActivity() instanceof MainActivity ? (MainActivity) getActivity() : null;
    }

    // Cambia a otra pestaña principal.
    protected void irATab(int index) {
        MainActivity m = main();
        if (m != null) m.irATab(index);
    }
}
