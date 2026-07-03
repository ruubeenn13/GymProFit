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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.SesionAdapter;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// SesionesActivity — historial de sesiones de entrenamiento del usuario.
// Lista las sesiones registradas mostrando el nombre de la rutina asociada,
// permite registrar nuevas sesiones, eliminarlas y navegar al resumen de cada una.
// ============================================================
public class SesionesActivity extends AppCompatActivity {

    private RecyclerView rvSesiones;
    private View tvVacio;
    private SesionAdapter adapter;
    private PreferencesManager prefsManager;
    // Interfaz Retrofit tipada del dominio sesiones (etapa 2)
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);

    private final List<SesionEntrenamiento> sesiones = new ArrayList<>();
    private final Map<Integer, String> rutinaNombres = new HashMap<>();

    private ActivityResultLauncher<Intent> registrarLauncher;

    // Inicializa vistas, tema/idioma, el launcher de registro de sesión y carga los datos.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
        setContentView(R.layout.activity_sesiones);

        rvSesiones = findViewById(R.id.rvSesiones);
        tvVacio    = findViewById(R.id.tvVacio);

        registrarLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) cargarDatos();
                });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        FloatingActionButton fab = findViewById(R.id.fabRegistrar);
        fab.setOnClickListener(v ->
                registrarLauncher.launch(new Intent(this, RegistrarSesionActivity.class)));

        rvSesiones.setLayoutManager(new LinearLayoutManager(this));
        cargarDatos();
    }

    // Punto de entrada de la carga: primero resuelve el mapa id→nombre de rutina
    // y después las sesiones del usuario.
    private void cargarDatos() {
        int usuarioId = prefsManager.getUsuarioId();
        if (usuarioId == -1) return;

        // Carga rutinas del usuario para mapear id→nombre
        API.getRutinasDeUsuario(usuarioId, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    List<Rutina> rutinas = UtilJSONParser.parseRutinaList(response);
                    for (Rutina r : rutinas) rutinaNombres.put(r.getId(), r.getNombre());
                } catch (JSONException ignored) {}
                cargarSesiones(usuarioId);
            }
            @Override
            public void onError(String message, int statusCode) { cargarSesiones(usuarioId); }
        });
    }

    // Obtiene las sesiones de entrenamiento del usuario (ya deserializadas por Gson) y las muestra.
    private void cargarSesiones(int usuarioId) {
        sesionApi.getDeUsuario(usuarioId).enqueue(new ApiCallback<List<SesionEntrenamiento>>() {
            @Override
            public void onOk(List<SesionEntrenamiento> lista) {
                mostrar(lista != null ? lista : new ArrayList<>());
            }
            @Override
            public void onFail(int code, String message) {
                mostrar(new ArrayList<>());
            }
        });
    }

    // Actualiza la lista interna y configura el adapter con los callbacks de
    // eliminar sesión (con confirmación) y abrir su resumen.
    private void mostrar(List<SesionEntrenamiento> lista) {
        sesiones.clear();
        sesiones.addAll(lista);

        if (sesiones.isEmpty()) {
            rvSesiones.setVisibility(View.GONE);
            tvVacio.setVisibility(View.VISIBLE);
            return;
        }
        tvVacio.setVisibility(View.GONE);
        rvSesiones.setVisibility(View.VISIBLE);

        adapter = new SesionAdapter(
                this,
                sesiones,
                rutinaNombres,
                sesion -> UIHelper.mostrarDialogoConIcono(this,
                        getString(R.string.sesiones_eliminar),
                        getString(R.string.sesiones_confirmar_eliminar),
                        R.drawable.ic_delete,
                        () -> eliminarSesion(sesion)),
                sesion -> {
                    Intent intent = new Intent(this, ResumenSesionActivity.class);
                    intent.putExtra("sesionId", sesion.getId());
                    String nombre = rutinaNombres.get(sesion.getRutinaId());
                    intent.putExtra("rutinaNombre", nombre != null ? nombre : "");
                    startActivity(intent);
                });
        rvSesiones.setAdapter(adapter);
    }

    // Elimina una sesión de entrenamiento y recarga el historial.
    private void eliminarSesion(SesionEntrenamiento sesion) {
        sesionApi.eliminar(sesion.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void body) {
                cargarDatos();
            }
            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(
                        SesionesActivity.this, getString(R.string.error_conexion));
            }
        });
    }

    // Aplica el idioma guardado en preferencias a la configuración de recursos.
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
