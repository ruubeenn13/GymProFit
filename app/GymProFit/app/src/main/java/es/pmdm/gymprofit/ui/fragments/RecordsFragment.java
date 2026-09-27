package es.pmdm.gymprofit.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.EjercicioApi;
import es.pmdm.gymprofit.network.RecordApi;
import es.pmdm.gymprofit.ui.adapters.RecordAdapter;
import es.pmdm.gymprofit.utils.EjercicioNavHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// RecordsFragment — sección Récords de Progreso (GP-105; antes RecordsActivity).
//
// El récord vigente de cada ejercicio, por zona del cuerpo, calculado por la API a
// partir de las series de las sesiones (GP-088). La primera vez que se hace un
// ejercicio no es un récord sino el punto de partida. Estados: cargando, vacío,
// error con reintentar (en el contenido, no en un aviso que se va) y lista.
// ============================================================
public class RecordsFragment extends BaseFragment {

    private RecyclerView rvRecords;
    private View layoutVacio, layoutError, cargando;
    private TextView tvError;

    private final RecordApi recordApi = ApiClient.service(RecordApi.class);
    private final EjercicioApi ejercicioApi = ApiClient.service(EjercicioApi.class);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvRecords   = findViewById(R.id.rvRecords);
        layoutVacio = findViewById(R.id.layoutVacio);
        layoutError = findViewById(R.id.layoutError);
        tvError     = findViewById(R.id.tvError);
        cargando    = findViewById(R.id.cargandoRecords);
        rvRecords.setLayoutManager(new LinearLayoutManager(requireContext()));
        findViewById(R.id.btnReintentar).setOnClickListener(v -> cargar());
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    // Pide los récords y enseña la lista, el vacío o el error.
    private void cargar() {
        // Los récords salen de las sesiones: un invitado no tiene.
        if (prefsManager.isGuest() || prefsManager.getUsuarioId() == -1) {
            ((TextView) findViewById(R.id.tvVacioTitulo)).setText(R.string.progreso_solo_registrados);
            mostrar(layoutVacio);
            return;
        }
        if (rvRecords.getAdapter() == null) mostrar(cargando);
        recordApi.getRecords(null).enqueue(new ApiCallback<Records>() {
            @Override
            public void onOk(Records r) {
                if (!isAdded()) return;
                if (r == null || r.getRecords().isEmpty()) {
                    mostrar(layoutVacio);
                    return;
                }
                rvRecords.setAdapter(new RecordAdapter(r.getRecords(), RecordsFragment.this::abrirEjercicio));
                mostrar(rvRecords);
            }
            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                tvError.setText(UiFeedback.mensaje(requireContext(), code, message));
                mostrar(layoutError);
            }
        });
    }

    private void mostrar(View visible) {
        rvRecords.setVisibility(visible == rvRecords ? View.VISIBLE : View.GONE);
        layoutVacio.setVisibility(visible == layoutVacio ? View.VISIBLE : View.GONE);
        layoutError.setVisibility(visible == layoutError ? View.VISIBLE : View.GONE);
        cargando.setVisibility(visible == cargando ? View.VISIBLE : View.GONE);
    }

    // La ficha espera el ejercicio entero en extras: se pide al pulsar.
    private void abrirEjercicio(Record record) {
        ejercicioApi.getPorId(record.getEjercicioId()).enqueue(new ApiCallback<Ejercicio>() {
            @Override
            public void onOk(Ejercicio ejercicio) {
                if (!isAdded() || ejercicio == null) return;
                EjercicioNavHelper.abrir(requireContext(), ejercicio);
            }
            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                UiFeedback.toastError(requireActivity(), code, message);
            }
        });
    }
}
