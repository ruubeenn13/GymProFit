package es.pmdm.gymprofit.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.utils.VacioRecords;
import java.util.List;
import es.pmdm.gymprofit.ui.adapters.RecordAdapter;
import es.pmdm.gymprofit.utils.EjercicioNavHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// RecordsFragment — sección Récords de Progreso (GP-105; antes RecordsActivity).
//
// El récord vigente de cada ejercicio, por zona del cuerpo, calculado por la API a
// partir de las series de las sesiones (GP-088). La primera vez que se hace un
// ejercicio no es un récord sino el punto de partida. Estados: cargando, vacío,
// error con reintentar (en el contenido, no en un aviso que se va) y lista, en VistaEstado.
// ============================================================
public class RecordsFragment extends BaseFragment {

    private RecyclerView rvRecords;
    private VistaEstado estado;

    private final RecordApi recordApi = ApiClient.service(RecordApi.class);
    private final EjercicioApi ejercicioApi = ApiClient.service(EjercicioApi.class);
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);

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
        estado      = new VistaEstado(findViewById(R.id.estadoRecords));
        rvRecords.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    // Pide los récords y enseña la lista, el vacío o el error.
    private void cargar() {
        // Sin id de cuenta no hay récords que pedir: el vacío de siempre.
        if (prefsManager.getUsuarioId() == -1) {
            vacio(VacioRecords.Que.SIN_SABER);
            return;
        }
        if (rvRecords.getAdapter() == null) cargando();
        recordApi.getRecords(null).enqueue(new ApiCallback<Records>() {
            @Override
            public void onOk(Records r) {
                if (!isAdded()) return;
                if (r == null || r.getRecords().isEmpty()) {
                    pintarVacio();
                    return;
                }
                rvRecords.setAdapter(new RecordAdapter(r.getRecords(), RecordsFragment.this::abrirEjercicio));
                rvRecords.setVisibility(View.VISIBLE);
                estado.oculto();
            }
            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                rvRecords.setVisibility(View.GONE);
                estado.error(VistaEstado.mensaje(requireContext(), R.string.progreso_records_error, code, message),
                        RecordsFragment.this::cargar);
            }
        });
    }

    // Sin récords: lo que se dice depende de si ya hay entrenos (GP-229). La lista de sesiones
    // solo se pide entonces.
    private void pintarVacio() {
        if (rvRecords.getAdapter() == null) cargando();
        sesionApi.getDeUsuario(prefsManager.getUsuarioId()).enqueue(new ApiCallback<List<SesionEntrenamiento>>() {
            @Override
            public void onOk(List<SesionEntrenamiento> sesiones) {
                if (!isAdded()) return;
                vacio(VacioRecords.de(true, false, sesiones == null ? 0 : sesiones.size()));
            }
            @Override
            public void onFail(int code, String message) {
                // Sin saber si hay entrenos, el texto que vale en los dos casos: los récords ya
                // llegaron (vacíos), así que no es un fallo que enseñar.
                if (!isAdded()) return;
                vacio(VacioRecords.Que.SIN_SABER);
            }
        });
    }

    private void vacio(VacioRecords.Que que) {
        rvRecords.setVisibility(View.GONE);
        estado.vacio(R.drawable.ic_ms_trophy, R.string.records_vacio_titulo, VacioRecords.texto(que), 0, null);
    }

    private void cargando() {
        rvRecords.setVisibility(View.GONE);
        estado.cargando();
    }

    // La ficha espera el ejercicio entero en extras: se pide al pulsar.
    private void abrirEjercicio(Record record, View tarjeta) {
        ejercicioApi.getPorId(record.getEjercicioId()).enqueue(new ApiCallback<Ejercicio>() {
            @Override
            public void onOk(Ejercicio ejercicio) {
                if (!isAdded() || ejercicio == null) return;
                // El récord crece hasta la progresión de su ejercicio, si su tarjeta sigue en pantalla.
                if (tarjeta.isAttachedToWindow()) EjercicioNavHelper.abrir(requireActivity(), tarjeta, ejercicio);
                else EjercicioNavHelper.abrir(requireContext(), ejercicio);
            }
            @Override
            public void onFail(int code, String message) {
                // GP-064: acción. Se pide al tocar, para abrir la ficha del ejercicio: lo que había
                // sigue en pantalla, y el toast dice por qué no se abre.
                if (!isAdded()) return;
                UiFeedback.toastError(requireActivity(), code, message);
            }
        });
    }
}
