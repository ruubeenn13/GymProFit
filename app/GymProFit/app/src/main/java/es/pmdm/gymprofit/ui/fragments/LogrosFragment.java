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

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.logro.LogroProgreso;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.LogroApi;
import es.pmdm.gymprofit.ui.adapters.LogroAdapter;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// LogrosFragment — sección Logros de Progreso (GP-105; antes LogrosActivity).
//
// El catálogo con el estado de cada logro: conseguido y cuándo, o cuánto falta
// (GP-079), en una sola llamada a GET /logros/progreso.
// ============================================================
public class LogrosFragment extends BaseFragment {

    private RecyclerView rvLogros;
    private TextView tvVacio;

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
        tvVacio = findViewById(R.id.tvVacio);
        rvLogros.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    @Override
    public void onResume() {
        super.onResume();
        api.getProgreso().enqueue(new ApiCallback<List<LogroProgreso>>() {
            @Override
            public void onOk(List<LogroProgreso> lista) {
                if (isAdded()) mostrar(lista != null ? lista : new ArrayList<>());
            }
            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                // Sin la respuesta no hay nada fiable que pintar: ni el catálogo ni el
                // estado. Se dice en la sección y en el aviso, no una lista a medias.
                UiFeedback.toastError(requireActivity(), code, message);
                tvVacio.setText(UiFeedback.mensaje(requireContext(), code, message));
                mostrar(new ArrayList<>());
            }
        });
    }

    // Conseguidos primero, conservando el orden del catálogo dentro de cada grupo.
    private void mostrar(List<LogroProgreso> logros) {
        if (logros.isEmpty()) {
            rvLogros.setVisibility(View.GONE);
            tvVacio.setVisibility(View.VISIBLE);
            return;
        }
        List<LogroProgreso> ordenados = new ArrayList<>();
        for (LogroProgreso l : logros) if (l.isConseguido()) ordenados.add(l);
        for (LogroProgreso l : logros) if (!l.isConseguido()) ordenados.add(l);

        tvVacio.setVisibility(View.GONE);
        rvLogros.setVisibility(View.VISIBLE);
        rvLogros.setAdapter(new LogroAdapter(ordenados));
    }
}
