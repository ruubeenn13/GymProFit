package es.pmdm.gymprofit.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.activities.SeccionProgresoActivity;
import es.pmdm.gymprofit.ui.progreso.PantallaProgreso;
import es.pmdm.gymprofit.utils.AvatarUtils;
import es.pmdm.gymprofit.utils.KilosMovidos;

// ============================================================
// ProgresoFragment — pestaña Progreso en una sola pantalla (GP-216, lote 4, tablero 16).
//
// Cabecera con el avatar (abre Perfil y ajustes, como las otras pestañas) y, de arriba abajo, el
// resumen, «Kilos movidos», «Récords recientes», «Logros», «Tu cuerpo» e «Historial». Ya no hay
// pestañas ni el perfil de dentro: cada «Ver todo» abre la lista completa (RecordsFragment,
// LogrosFragment, MedidasFragment o HistorialFragment) en SeccionProgresoActivity.
//
// Las peticiones van todas a la vez y la lista de sesiones se pide una sola vez (la leen el
// resumen, «Todo» de los kilos y el historial); cada sección pinta lo suyo en cuanto llega, con su
// carga, su vacío y su error con «Reintentar» (PantallaProgreso). Se recarga al volver.
// ============================================================
public class ProgresoFragment extends BaseFragment {

    /** Las secciones con lista completa, para SeccionProgresoActivity y las notificaciones. */
    public static final int RECORDS = 0, LOGROS = 1, MEDIDAS = 2, HISTORIAL = 3;

    private static final String KEY_PERIODO = "gpf_periodo_kilos";

    @Nullable private PantallaProgreso pantalla;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progreso, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int periodo = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_PERIODO, KilosMovidos.PERIODO_INICIAL) : KilosMovidos.PERIODO_INICIAL;
        pantalla = new PantallaProgreso(this, view, prefsManager, periodo, this::abrirSeccion);
    }

    @Override
    public void onResume() {
        super.onResume();
        AvatarUtils.cabecera(requireContext(), findViewById(R.id.avatarCabecera), prefsManager);
        if (pantalla != null) pantalla.cargar();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pantalla != null) outState.putInt(KEY_PERIODO, pantalla.periodo());
    }

    @Override
    public void onDestroyView() {
        pantalla = null;
        super.onDestroyView();
    }

    // «Ver todo», una fila de logro o la tarjeta del cuerpo: la lista completa en su pantalla.
    private void abrirSeccion(int seccion) {
        startActivity(SeccionProgresoActivity.intent(requireContext(), seccion, false));
    }
}
