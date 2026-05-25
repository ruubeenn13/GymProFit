package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;

import java.util.List;

import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import es.pmdm.gymprofit.network.UtilJSONParser;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.UIHelper;

public class PerfilActivity extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private ActivityResultLauncher<Intent> editarPerfilLauncher;
    private ActivityResultLauncher<Intent> medicionesLauncher;
    private TextView tvNombreUsuario, tvEmailUsuario;
    private TextView tvInfoNombre, tvInfoEmail;
    private TextView tvInfoNivel, tvInfoPeso, tvInfoAltura, tvInfoEdad, tvInfoObjetivo;
    private TextView tvPesoMedicion, tvAlturaMedicion;
    private LinearLayout llMedicionesResumen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        editarPerfilLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        configurarDatosUsuario();
                    }
                }
        );

        medicionesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        int uid = prefsManager.getUsuarioId();
                        if (uid != -1) cargarUltimaMedicion(uid);
                    }
                }
        );

        setContentView(R.layout.activity_perfil);

        setupMenuButton();
        inicializarVistas();
        configurarDatosUsuario();
        configurarBotones();
        configurarNavegacion();
    }

    private void inicializarVistas() {
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        tvEmailUsuario = findViewById(R.id.tvEmailUsuario);
        tvInfoNombre = findViewById(R.id.tvInfoNombre);
        tvInfoEmail = findViewById(R.id.tvInfoEmail);
        tvInfoNivel = findViewById(R.id.tvInfoNivel);
        tvInfoPeso = findViewById(R.id.tvInfoPeso);
        tvInfoAltura = findViewById(R.id.tvInfoAltura);
        tvInfoEdad = findViewById(R.id.tvInfoEdad);
        tvInfoObjetivo = findViewById(R.id.tvInfoObjetivo);
        tvPesoMedicion = findViewById(R.id.tvPesoMedicion);
        tvAlturaMedicion = findViewById(R.id.tvAlturaMedicion);
        llMedicionesResumen = findViewById(R.id.llMedicionesResumen);
    }

    private void configurarDatosUsuario() {
        String sinDatos = getString(R.string.perfil_sin_datos);
        tvInfoNivel.setText(sinDatos);
        tvInfoPeso.setText(sinDatos);
        tvInfoAltura.setText(sinDatos);
        tvInfoEdad.setText(sinDatos);
        tvInfoObjetivo.setText(sinDatos);

        int usuarioId = prefsManager.getUsuarioId();
        if (usuarioId == -1) return;

        cargarUltimaMedicion(usuarioId);

        API.getUsuarioPorId(usuarioId, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    Usuario u = UtilJSONParser.parseUsuario(response);
                    if (u == null) return;

                    runOnUiThread(() -> {
                        tvNombreUsuario.setText(u.getUsername());
                        tvEmailUsuario.setText(val(u.getEmail(), sinDatos));
                        tvInfoNombre.setText(u.getUsername());
                        tvInfoEmail.setText(val(u.getEmail(), sinDatos));
                        tvInfoNivel.setText(val(u.getNivelExperiencia(), null) != null
                                ? mapearNivel(u.getNivelExperiencia()) : sinDatos);
                        tvInfoPeso.setText(val(u.getPeso(), null) != null
                                ? getString(R.string.perfil_kg, u.getPeso()) : sinDatos);
                        tvInfoAltura.setText(u.getAltura() > 0
                                ? getString(R.string.perfil_cm, (int) u.getAltura()) : sinDatos);
                        tvInfoEdad.setText(u.getEdad() > 0
                                ? getString(R.string.perfil_anos, u.getEdad()) : sinDatos);
                        tvInfoObjetivo.setText(val(u.getObjetivo(), sinDatos) != null
                                ? mapearObjetivo(u.getObjetivo()) : sinDatos);
                    });
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String message, int statusCode) {
                Log.e("GymProFit", "getUsuarioPorId error status=" + statusCode + " msg=" + message);
                runOnUiThread(() -> {
                    String username = prefsManager.getUsername();
                    if (username != null && !username.isEmpty()) {
                        tvNombreUsuario.setText(username);
                        tvInfoNombre.setText(username);
                    }
                });
            }
        });
    }

    private void cargarUltimaMedicion(int usuarioId) {
        API.getMedicionesDeUsuario(usuarioId, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    List<MedicionCorporal> lista = UtilJSONParser.parseMedicionList(response);
                    if (lista == null || lista.isEmpty()) return;
                    MedicionCorporal ultima = lista.get(0);
                    runOnUiThread(() -> {
                        boolean tienePeso = ultima.getPeso() > 0;
                        boolean tieneAltura = ultima.getAltura() > 0;
                        if (tienePeso || tieneAltura) {
                            llMedicionesResumen.setVisibility(View.VISIBLE);
                            tvPesoMedicion.setText(tienePeso
                                    ? getString(R.string.perfil_kg, String.format(java.util.Locale.getDefault(), "%.1f", ultima.getPeso())) : "");
                            tvAlturaMedicion.setText(tieneAltura
                                    ? getString(R.string.perfil_cm, (int) ultima.getAltura()) : "");
                        }
                    });
                } catch (JSONException ignored) {}
            }

            @Override
            public void onError(String message, int statusCode) {}
        });
    }

    private void configurarBotones() {
        findViewById(R.id.btnEditarPerfil).setOnClickListener(v ->
                editarPerfilLauncher.launch(new Intent(this, EditarPerfilActivity.class))
        );

        findViewById(R.id.itemSesiones).setOnClickListener(v ->
                startActivity(new Intent(this, SesionesActivity.class)));

        findViewById(R.id.itemMediciones).setOnClickListener(v ->
                medicionesLauncher.launch(new Intent(this, MedicionesActivity.class)));

        findViewById(R.id.itemLogros).setOnClickListener(v ->
                startActivity(new Intent(this, LogrosActivity.class)));

        View itemAdmin = findViewById(R.id.itemAdmin);
        if (prefsManager.isAdmin()) {
            itemAdmin.setVisibility(View.VISIBLE);
            itemAdmin.setOnClickListener(v ->
                    startActivity(new Intent(this, AdminActivity.class)));
        } else {
            itemAdmin.setVisibility(View.GONE);
        }

    }

    private void configurarNavegacion() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_perfil);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_rutinas) {
                startActivity(new Intent(this, RutinasActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_ejercicios) {
                startActivity(new Intent(this, EjerciciosActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_nutricion) {
                startActivity(new Intent(this, NutricionActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_perfil) {
                return true;
            }
            return false;
        });
    }

    private String val(String s, String fallback) {
        return (s != null && !s.isEmpty() && !"null".equals(s)) ? s : fallback;
    }

    private String mapearNivel(String nivel) {
        if (nivel == null) return getString(R.string.perfil_sin_datos);
        switch (nivel) {
            case "PRINCIPIANTE": return getString(R.string.nivel_principiante);
            case "INTERMEDIO":   return getString(R.string.nivel_intermedio);
            case "AVANZADO":     return getString(R.string.nivel_avanzado);
            default:             return nivel;
        }
    }

    private String mapearObjetivo(String objetivo) {
        if (objetivo == null) return getString(R.string.perfil_sin_datos);
        switch (objetivo) {
            case "PERDER_PESO":             return getString(R.string.objetivo_perder_peso);
            case "GANAR_MASA_MUSCULAR":     return getString(R.string.objetivo_ganar_musculo);
            case "MANTENER_PESO":           return getString(R.string.objetivo_mantener);
            case "MEJORAR_RESISTENCIA":     return getString(R.string.objetivo_resistencia);
            case "MEJORAR_FUERZA":          return getString(R.string.objetivo_fuerza);
            case "REDUCIR_GRASA_CORPORAL":  return getString(R.string.objetivo_reducir_grasa);
            case "MEJORAR_FLEXIBILIDAD":    return getString(R.string.objetivo_flexibilidad);
            case "MEJORAR_VELOCIDAD":       return getString(R.string.objetivo_velocidad);
            case "AUMENTAR_CALORIAS":       return getString(R.string.objetivo_aumentar_calorias);
            case "MEJORAR_MOVILIDAD":       return getString(R.string.objetivo_movilidad);
            default:                        return objetivo;
        }
    }

}