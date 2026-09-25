package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

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
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// RecordsActivity — récords personales del usuario (GP-088).
//
// El récord vigente de cada ejercicio, por zona del cuerpo, calculado por la API a
// partir de las series de las sesiones: no hay nada que apuntar a mano. La primera
// vez que se hace un ejercicio no es un récord sino el punto de partida, así que un
// usuario recién llegado ve el estado vacío hasta que supera alguna marca.
// ============================================================
public class RecordsActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private RecyclerView rvRecords;
    private View layoutVacio, layoutError;
    private TextView tvError;

    private final RecordApi recordApi = ApiClient.service(RecordApi.class);
    private final EjercicioApi ejercicioApi = ApiClient.service(EjercicioApi.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new PreferencesManager(this).applyTheme();
        setContentView(R.layout.activity_records);

        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        rvRecords   = findViewById(R.id.rvRecords);
        layoutVacio = findViewById(R.id.layoutVacio);
        layoutError = findViewById(R.id.layoutError);
        tvError     = findViewById(R.id.tvError);
        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnReintentar).setOnClickListener(v -> cargar());

        cargar();
    }

    // Pide los récords y enseña la lista, el vacío o el error.
    private void cargar() {
        LoadingDialog.show(this);
        layoutError.setVisibility(View.GONE);
        recordApi.getRecords(null).enqueue(new ApiCallback<Records>() {
            @Override
            public void onOk(Records r) {
                if (isFinishing()) return;
                LoadingDialog.hide(RecordsActivity.this);
                if (r == null || r.getRecords().isEmpty()) {
                    mostrar(layoutVacio);
                    return;
                }
                rvRecords.setAdapter(new RecordAdapter(r.getRecords(), RecordsActivity.this::abrirEjercicio));
                mostrar(rvRecords);
            }
            @Override
            public void onFail(int code, String message) {
                if (isFinishing()) return;
                LoadingDialog.hide(RecordsActivity.this);
                // El fallo se queda en la pantalla, con reintentar: un aviso que se va
                // dejaría una pantalla vacía que parece «no tienes récords».
                tvError.setText(UiFeedback.mensaje(RecordsActivity.this, code));
                mostrar(layoutError);
            }
        });
    }

    private void mostrar(View visible) {
        rvRecords.setVisibility(visible == rvRecords ? View.VISIBLE : View.GONE);
        layoutVacio.setVisibility(visible == layoutVacio ? View.VISIBLE : View.GONE);
        layoutError.setVisibility(visible == layoutError ? View.VISIBLE : View.GONE);
    }

    // La ficha espera el ejercicio entero en extras: se pide al pulsar.
    private void abrirEjercicio(Record record) {
        ejercicioApi.getPorId(record.getEjercicioId()).enqueue(new ApiCallback<Ejercicio>() {
            @Override
            public void onOk(Ejercicio ejercicio) {
                if (isFinishing() || ejercicio == null) return;
                EjercicioNavHelper.abrir(RecordsActivity.this, ejercicio);
            }
            @Override
            public void onFail(int code, String message) {
                if (isFinishing()) return;
                UiFeedback.toastError(RecordsActivity.this, code, message);
            }
        });
    }
}
