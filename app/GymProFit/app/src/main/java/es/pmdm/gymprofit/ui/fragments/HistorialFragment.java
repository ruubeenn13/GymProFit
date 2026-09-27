package es.pmdm.gymprofit.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.activities.ResumenSesionActivity;
import es.pmdm.gymprofit.ui.adapters.SesionAdapter;
import es.pmdm.gymprofit.utils.NombresRutina;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// HistorialFragment — sección Historial de Progreso (GP-105; antes SesionesActivity).
//
// Las sesiones registradas con el nombre de su rutina. El nombre sale de las rutinas
// propias Y de las plantillas: antes solo conocía las propias y una sesión hecha con
// una plantilla decía «Sin rutina asociada» (GP-113). Se borra con confirmación y se
// abre el resumen de cada una. Registrar una sesión está en el «+» y en Entrenar.
// ============================================================
public class HistorialFragment extends BaseFragment {

    private RecyclerView rvSesiones;
    private TextView tvVacio;

    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);

    private Map<Integer, String> rutinaNombres = new HashMap<>();
    @Nullable private List<Rutina> propias, plantillas;
    @Nullable private List<SesionEntrenamiento> sesiones;
    private int pendientes;
    private boolean fallo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvSesiones = findViewById(R.id.rvSesiones);
        tvVacio = findViewById(R.id.tvVacio);
        rvSesiones.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    // Las tres listas a la vez; se pinta cuando han llegado todas.
    private void cargar() {
        int uid = prefsManager.getUsuarioId();
        if (uid == -1 || prefsManager.isGuest()) {
            tvVacio.setText(R.string.progreso_solo_registrados);
            mostrar(new ArrayList<>());
            return;
        }
        pendientes = 3;
        fallo = false;
        propias = null;
        plantillas = null;
        sesiones = null;

        rutinaApi.getDeUsuarioActivas(uid).enqueue(new ApiCallback<List<Rutina>>() {
            @Override public void onOk(List<Rutina> l) { propias = l; listo(); }
            // Sin los nombres, las sesiones se ven igual, solo sin rótulo de rutina.
            @Override public void onFail(int code, String m) { listo(); }
        });
        rutinaApi.getPredefinidas().enqueue(new ApiCallback<List<Rutina>>() {
            @Override public void onOk(List<Rutina> l) { plantillas = l; listo(); }
            // Igual que arriba: el nombre es un rótulo, no el dato.
            @Override public void onFail(int code, String m) { listo(); }
        });
        sesionApi.getDeUsuario(uid).enqueue(new ApiCallback<List<SesionEntrenamiento>>() {
            @Override public void onOk(List<SesionEntrenamiento> l) {
                sesiones = l != null ? l : new ArrayList<>();
                listo();
            }
            @Override public void onFail(int code, String message) {
                if (!isAdded()) return;
                fallo = true;
                UiFeedback.toastError(requireActivity(), code, message);
                tvVacio.setText(UiFeedback.mensaje(requireContext(), code, message));
                mostrar(new ArrayList<>());
            }
        });
    }

    private void listo() {
        if (!isAdded() || fallo || --pendientes > 0 || sesiones == null) return;
        rutinaNombres = NombresRutina.de(propias, plantillas);
        tvVacio.setText(R.string.sesiones_sin_sesiones);
        mostrar(sesiones);
    }

    private void mostrar(List<SesionEntrenamiento> lista) {
        if (lista.isEmpty()) {
            rvSesiones.setVisibility(View.GONE);
            tvVacio.setVisibility(View.VISIBLE);
            return;
        }
        tvVacio.setVisibility(View.GONE);
        rvSesiones.setVisibility(View.VISIBLE);
        rvSesiones.setAdapter(new SesionAdapter(
                requireContext(),
                lista,
                rutinaNombres,
                sesion -> UIHelper.mostrarDialogoConIcono(requireActivity(),
                        getString(R.string.sesiones_eliminar),
                        getString(R.string.sesiones_confirmar_eliminar),
                        R.drawable.ic_ms_delete,
                        () -> eliminar(sesion)),
                this::abrirResumen));
    }

    private void abrirResumen(SesionEntrenamiento sesion) {
        Intent intent = new Intent(requireContext(), ResumenSesionActivity.class);
        intent.putExtra("sesionId", sesion.getId());
        // El resumen vuelve a pedir la sesión, pero tarda: sin este aviso un
        // entrenamiento libre se pintaría un instante como «sin rutina asociada» (GP-057).
        intent.putExtra(ResumenSesionActivity.EXTRA_ENTRENAMIENTO_LIBRE, sesion.esEntrenamientoLibre());
        String nombre = sesion.esEntrenamientoLibre() ? null : rutinaNombres.get(sesion.getRutinaId());
        intent.putExtra("rutinaNombre", nombre != null ? nombre : "");
        startActivity(intent);
    }

    private void eliminar(SesionEntrenamiento sesion) {
        sesionApi.eliminar(sesion.getId()).enqueue(new ApiCallback<Void>() {
            @Override public void onOk(Void body) { if (isAdded()) cargar(); }
            @Override public void onFail(int code, String message) {
                if (isAdded()) UIHelper.mostrarToastError(requireActivity(), getString(R.string.error_conexion));
            }
        });
    }
}
