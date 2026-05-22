package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.MedicionAdapter;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

public class MedicionesActivity extends AppCompatActivity {

    private RecyclerView rvMediciones;
    private View tvVacio;
    private MedicionAdapter adapter;
    private PreferencesManager prefsManager;

    private final List<MedicionCorporal> mediciones = new ArrayList<>();

    private ActivityResultLauncher<Intent> registrarLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
        setContentView(R.layout.activity_mediciones);

        rvMediciones = findViewById(R.id.rvMediciones);
        tvVacio      = findViewById(R.id.tvVacio);

        registrarLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) cargarMediciones();
                });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        FloatingActionButton fab = findViewById(R.id.fabRegistrar);
        fab.setOnClickListener(v ->
                registrarLauncher.launch(new Intent(this, RegistrarMedicionActivity.class)));

        rvMediciones.setLayoutManager(new LinearLayoutManager(this));
        cargarMediciones();
    }

    private void cargarMediciones() {
        int usuarioId = prefsManager.getUsuarioId();
        if (usuarioId == -1) return;

        API.getMedicionesDeUsuario(usuarioId, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    List<MedicionCorporal> lista = UtilJSONParser.parseMedicionList(response);
                    runOnUiThread(() -> mostrar(lista));
                } catch (JSONException e) {
                    runOnUiThread(() -> mostrar(new ArrayList<>()));
                }
            }
            @Override
            public void onError(String message, int statusCode) {
                // 404 = sin mediciones
                runOnUiThread(() -> mostrar(new ArrayList<>()));
            }
        });
    }

    private void mostrar(List<MedicionCorporal> lista) {
        mediciones.clear();
        mediciones.addAll(lista);

        if (mediciones.isEmpty()) {
            rvMediciones.setVisibility(View.GONE);
            tvVacio.setVisibility(View.VISIBLE);
            return;
        }
        tvVacio.setVisibility(View.GONE);
        rvMediciones.setVisibility(View.VISIBLE);

        adapter = new MedicionAdapter(this, mediciones, medicion ->
                UIHelper.mostrarDialogoConIcono(this,
                        getString(R.string.mediciones_eliminar),
                        getString(R.string.mediciones_confirmar_eliminar),
                        R.drawable.ic_delete,
                        () -> eliminarMedicion(medicion)));
        rvMediciones.setAdapter(adapter);
    }

    private void eliminarMedicion(MedicionCorporal medicion) {
        API.eliminarMedicion(medicion.getId(), new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                runOnUiThread(() -> cargarMediciones());
            }
            @Override
            public void onError(String message, int statusCode) {
                runOnUiThread(() -> UIHelper.mostrarToastError(
                        MedicionesActivity.this, getString(R.string.error_conexion)));
            }
        });
    }

    private void aplicarIdioma() {
        String lang = prefsManager.getLanguage();
        if (!lang.isEmpty()) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Resources res = getResources();
            Configuration cfg = res.getConfiguration();
            cfg.setLocale(locale);
            res.updateConfiguration(cfg, res.getDisplayMetrics());
        }
    }
}
