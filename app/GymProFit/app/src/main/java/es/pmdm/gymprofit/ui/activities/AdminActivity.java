package es.pmdm.gymprofit.ui.activities;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.AdminUsuarioAdapter;
import es.pmdm.gymprofit.utils.PreferencesManager;

public class AdminActivity extends AppCompatActivity {

    private TextView tvTotalUsuarios, tvUsuariosActivos, tvTotalSesiones, tvSesionesHoy;
    private RecyclerView rvUsuarios;
    private PreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
        setContentView(R.layout.activity_admin);

        tvTotalUsuarios   = findViewById(R.id.tvTotalUsuarios);
        tvUsuariosActivos = findViewById(R.id.tvUsuariosActivos);
        tvTotalSesiones   = findViewById(R.id.tvTotalSesiones);
        tvSesionesHoy     = findViewById(R.id.tvSesionesHoy);
        rvUsuarios        = findViewById(R.id.rvUsuarios);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        rvUsuarios.setLayoutManager(new LinearLayoutManager(this));

        cargarEstadisticas();
        cargarUsuarios();
    }

    private void cargarEstadisticas() {
        API.getAdminEstadisticas(new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    JSONObject obj = new JSONObject(response);
                    runOnUiThread(() -> {
                        tvTotalUsuarios.setText(String.valueOf(obj.optLong("totalUsuarios", 0)));
                        tvUsuariosActivos.setText(String.valueOf(obj.optLong("usuariosActivos", 0)));
                        tvTotalSesiones.setText(String.valueOf(obj.optLong("totalSesiones", 0)));
                        tvSesionesHoy.setText(String.valueOf(obj.optLong("sesionesHoy", 0)));
                    });
                } catch (JSONException ignored) {}
            }
            @Override
            public void onError(String message, int statusCode) {}
        });
    }

    private void cargarUsuarios() {
        API.getAdminUsuarios(new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    List<Usuario> lista = UtilJSONParser.parseUsuarioList(response);
                    runOnUiThread(() -> rvUsuarios.setAdapter(new AdminUsuarioAdapter(lista)));
                } catch (JSONException ignored) {
                    runOnUiThread(() -> rvUsuarios.setAdapter(new AdminUsuarioAdapter(new ArrayList<>())));
                }
            }
            @Override
            public void onError(String message, int statusCode) {
                runOnUiThread(() -> rvUsuarios.setAdapter(new AdminUsuarioAdapter(new ArrayList<>())));
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
