package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.admin.EstadisticasGlobales;
import es.pmdm.gymprofit.network.AdminApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.UiFeedback;

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

    // Interfaz Retrofit tipada del panel de administración (etapa 2)
    private final AdminApi api = ApiClient.service(AdminApi.class);

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

    // Solicita a la API las estadísticas globales y actualiza los TextViews (callback ya en hilo UI)
    private void cargarEstadisticas() {
        // Muestra el overlay de carga mientras se piden las estadísticas
        LoadingDialog.show(this);
        api.getEstadisticas().enqueue(new ApiCallback<EstadisticasGlobales>() {
            @Override
            public void onOk(EstadisticasGlobales e) {
                // Oculta el overlay al terminar la carga con éxito
                LoadingDialog.hide(AdminActivity.this);
                if (e == null) return;
                tvTotalUsuarios.setText(String.valueOf(e.getTotalUsuarios()));
                tvUsuariosActivos.setText(String.valueOf(e.getUsuariosActivos()));
                tvTotalSesiones.setText(String.valueOf(e.getTotalSesiones()));
                tvSesionesHoy.setText(String.valueOf(e.getSesionesHoy()));
                tvRutinasPredefinidas.setText(String.valueOf(e.getRutinasPredefinidas()));
                tvEjerciciosActivos.setText(String.valueOf(e.getEjerciciosActivos()));
            }
            @Override
            public void onFail(int code, String message) {
                // Oculta el overlay y muestra el error mapeado al fallar la carga
                LoadingDialog.hide(AdminActivity.this);
                UiFeedback.toastError(AdminActivity.this, code, message);
            }
        });
    }
}
