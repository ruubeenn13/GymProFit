package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilREST;

// ============================================================
// AdminActivity — panel principal de administración (rol ADMIN)
// Muestra estadísticas globales de la app (usuarios, sesiones, rutinas,
// ejercicios) y sirve como menú de navegación hacia las pantallas de
// gestión de usuarios, rutinas, ejercicios y alimentos.
// ============================================================
public class AdminActivity extends BaseActivity {

    // TextViews donde se pintan las estadísticas globales devueltas por la API
    private TextView tvTotalUsuarios, tvUsuariosActivos, tvTotalSesiones,
            tvSesionesHoy, tvRutinasPredefinidas, tvEjerciciosActivos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        tvTotalUsuarios       = findViewById(R.id.tvTotalUsuarios);
        tvUsuariosActivos     = findViewById(R.id.tvUsuariosActivos);
        tvTotalSesiones       = findViewById(R.id.tvTotalSesiones);
        tvSesionesHoy         = findViewById(R.id.tvSesionesHoy);
        tvRutinasPredefinidas = findViewById(R.id.tvRutinasPredefinidas);
        tvEjerciciosActivos   = findViewById(R.id.tvEjerciciosActivos);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        setupMenuButton();

        findViewById(R.id.cardGestionarUsuarios).setOnClickListener(v ->
                startActivity(new Intent(this, AdminUsuariosActivity.class)));

        findViewById(R.id.cardGestionarRutinas).setOnClickListener(v ->
                startActivity(new Intent(this, AdminRutinasActivity.class)));

        findViewById(R.id.cardGestionarEjercicios).setOnClickListener(v ->
                startActivity(new Intent(this, AdminEjerciciosActivity.class)));

        findViewById(R.id.cardGestionarAlimentos).setOnClickListener(v ->
                startActivity(new Intent(this, AdminAlimentosActivity.class)));

        cargarEstadisticas();
    }

    // Solicita a la API las estadísticas globales y actualiza los TextViews en el hilo UI
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
                        tvRutinasPredefinidas.setText(String.valueOf(obj.optLong("rutinasPredefinidas", 0)));
                        tvEjerciciosActivos.setText(String.valueOf(obj.optLong("ejerciciosActivos", 0)));
                    });
                } catch (JSONException ignored) {}
            }

            @Override
            public void onError(String message, int statusCode) {}
        });
    }
}
