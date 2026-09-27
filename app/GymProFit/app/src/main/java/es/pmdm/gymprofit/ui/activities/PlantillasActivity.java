package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.ui.adapters.RutinaAdapter;
import es.pmdm.gymprofit.ui.widget.MenuRutina;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// PlantillasActivity — todas las plantillas de GymProFit (GP-105).
//
// Es el «Ver todas» del carrusel de Entrenar: la lista entera de rutinas predefinidas
// con el filtro por nivel que antes tenía la pestaña Rutinas (GP-072). Tocar abre el
// detalle; un ADMIN conserva la pulsación larga para editar o activar/desactivar.
// ============================================================
public class PlantillasActivity extends AppCompatActivity {

    private static final String NIVEL_TODOS = "Todos";

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private RutinaAdapter adapter;
    private RecyclerView rv;
    private TextView tvEmpty;
    private String nivelActual = NIVEL_TODOS;
    private boolean falloCarga = false;

    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);
    private ActivityResultLauncher<Intent> recargar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();
        setContentView(R.layout.activity_plantillas);

        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        rv = findViewById(R.id.rvPlantillas);
        tvEmpty = findViewById(R.id.tvEmpty);
        recargar = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                r -> { if (r.getResultCode() == RESULT_OK) cargar(); });

        adapter = new RutinaAdapter(new ArrayList<>(), r -> MenuRutina.abrirDetalle(this, recargar, r));
        adapter.setUserContext(prefs.isAdmin(), prefs.getUsuarioId());
        if (prefs.isAdmin()) {
            adapter.setOnLongClickListener((r, ancla) -> MenuRutina.mostrar(this, ancla, r, recargar, this::cargar));
        }
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        ((ChipGroup) findViewById(R.id.chipGroupNivel)).setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chipPrincipiante)     nivelActual = "Principiante";
            else if (id == R.id.chipIntermedio)  nivelActual = "Intermedio";
            else if (id == R.id.chipAvanzado)    nivelActual = "Avanzado";
            else                                 nivelActual = NIVEL_TODOS;
            adapter.filtrarPorNivel(nivelActual);
            actualizarVacio();
        });

        cargar();
    }

    private void cargar() {
        LoadingDialog.show(this);
        rutinaApi.getPredefinidas().enqueue(new ApiCallback<List<Rutina>>() {
            @Override
            public void onOk(List<Rutina> lista) {
                LoadingDialog.hide(PlantillasActivity.this);
                if (isFinishing()) return;
                falloCarga = false;
                adapter.setRutinas(lista != null ? lista : new ArrayList<>());
                adapter.filtrarPorNivel(nivelActual);
                actualizarVacio();
            }
            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(PlantillasActivity.this);
                if (isFinishing()) return;
                falloCarga = true;
                UiFeedback.toastError(PlantillasActivity.this, code, message);
                adapter.setRutinas(new ArrayList<>());
                actualizarVacio();
            }
        });
    }

    // Vacío por el filtro, por no haber plantillas o por un fallo: tres mensajes.
    private void actualizarVacio() {
        boolean vacio = adapter.getItemCount() == 0;
        tvEmpty.setText(falloCarga ? R.string.plantillas_error
                : NIVEL_TODOS.equals(nivelActual) ? R.string.plantillas_vacio : R.string.rutinas_vacio_filtro);
        tvEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rv.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }
}
