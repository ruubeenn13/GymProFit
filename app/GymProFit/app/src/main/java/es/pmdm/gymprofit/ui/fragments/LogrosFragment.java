package es.pmdm.gymprofit.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.logro.LogroProgreso;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.LogroApi;
import es.pmdm.gymprofit.ui.adapters.LogroAdapter;
import es.pmdm.gymprofit.utils.LogrosVisibles;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// LogrosFragment — sección Logros de Progreso (GP-105; antes LogrosActivity).
//
// El catálogo con el estado de cada logro: conseguido y cuándo, o cuánto falta
// (GP-079), en una sola llamada a GET /logros/progreso. Los que dependen de
// objetivos personales no salen mientras no haya pantalla para crearlos (GP-114).
// Carga, vacío y error con «Reintentar» en VistaEstado (GP-064).
// ============================================================
public class LogrosFragment extends BaseFragment {

    private RecyclerView rvLogros;
    private VistaEstado estado;

    private final LogroApi api = ApiClient.service(LogroApi.class);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_logros, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvLogros = findViewById(R.id.rvLogros);
        estado = new VistaEstado(findViewById(R.id.estadoLogros));
        rvLogros.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    private void cargar() {
        // Al volver, lo que había se queda hasta que llega lo nuevo: sin parpadeo.
        if (rvLogros.getAdapter() == null) estado.cargando();
        api.getProgreso().enqueue(new ApiCallback<List<LogroProgreso>>() {
            @Override
            public void onOk(List<LogroProgreso> lista) {
                if (isAdded()) mostrar(LogrosVisibles.filtrar(lista));
            }
            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                // Sin la respuesta no hay nada fiable que pintar: ni el catálogo ni el
                // estado. Se dice en la sección, con «Reintentar», no una lista a medias.
                rvLogros.setVisibility(View.GONE);
                estado.error(UiFeedback.mensaje(requireContext(), code, message), LogrosFragment.this::cargar);
            }
        });
    }

    // Conseguidos primero, conservando el orden del catálogo dentro de cada grupo.
    private void mostrar(List<LogroProgreso> logros) {
        if (logros.isEmpty()) {
            rvLogros.setVisibility(View.GONE);
            estado.vacio(R.drawable.ic_ms_workspace_premium, R.string.logros_sin_logros);
            return;
        }
        List<LogroProgreso> ordenados = new ArrayList<>();
        for (LogroProgreso l : logros) if (l.isConseguido()) ordenados.add(l);
        for (LogroProgreso l : logros) if (!l.isConseguido()) ordenados.add(l);

        estado.oculto();
        rvLogros.setVisibility(View.VISIBLE);
        rvLogros.setAdapter(new LogroAdapter(ordenados));
    }
}
