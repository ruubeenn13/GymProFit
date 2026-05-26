package es.pmdm.gymprofit.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import es.pmdm.gymprofit.BuildConfig;
import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;

public class PerfilActivity extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private ActivityResultLauncher<Intent> editarPerfilLauncher;
    private ActivityResultLauncher<Intent> medicionesLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermLauncher;
    private Uri cameraUri;
    private TextView tvNombreUsuario, tvEmailUsuario;
    private TextView tvInfoNombre, tvInfoEmail;
    private TextView tvInfoNivel, tvInfoPeso, tvInfoAltura, tvInfoEdad, tvInfoObjetivo;
    private TextView tvPesoMedicion, tvAlturaMedicion;
    private LinearLayout llMedicionesResumen;
    private ImageView ivAvatar;

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

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) subirFoto(uri); }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                ok -> { if (ok && cameraUri != null) subirFoto(cameraUri); }
        );

        cameraPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) lanzarCamara();
                    else Toast.makeText(this, R.string.perfil_permiso_camara, Toast.LENGTH_SHORT).show();
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
        ivAvatar = findViewById(R.id.ivAvatar);
        ivAvatar.getParent().requestChildFocus(ivAvatar, ivAvatar);
        ((View) ivAvatar.getParent()).setOnClickListener(v -> mostrarDialogoFoto());
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
        cargarFotoPerfil(usuarioId);

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

    private void mostrarDialogoFoto() {
        String[] opciones = {
            getString(R.string.perfil_foto_galeria),
            getString(R.string.perfil_foto_camara)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.perfil_cambiar_foto)
                .setItems(opciones, (d, which) -> {
                    if (which == 0) galleryLauncher.launch("image/*");
                    else pedirPermisoCamara();
                })
                .show();
    }

    private void pedirPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            lanzarCamara();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void lanzarCamara() {
        File foto = new File(getCacheDir(), "perfil_temp.jpg");
        cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", foto);
        cameraLauncher.launch(cameraUri);
    }

    private void subirFoto(Uri uri) {
        int uid = prefsManager.getUsuarioId();
        if (uid == -1) return;

        Toast.makeText(this, R.string.perfil_foto_subiendo, Toast.LENGTH_SHORT).show();

        API.uploadFotoPerfil(this, uid, uri, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                runOnUiThread(() -> {
                    Toast.makeText(PerfilActivity.this, R.string.perfil_foto_ok, Toast.LENGTH_SHORT).show();
                    ivAvatar.setImageURI(null);
                    ivAvatar.setImageURI(uri);
                });
            }

            @Override
            public void onError(String message, int statusCode) {
                runOnUiThread(() ->
                    Toast.makeText(PerfilActivity.this, R.string.perfil_foto_error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void cargarFotoPerfil(int userId) {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... v) {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(BuildConfig.BASE_URL + "usuarios/" + userId + "/foto");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    String tok = UtilREST.getToken();
                    if (tok != null) conn.setRequestProperty("Authorization", "Bearer " + tok);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    if (conn.getResponseCode() == 200) {
                        InputStream is = conn.getInputStream();
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        is.close();
                        return bmp;
                    }
                } catch (Exception ignored) {
                } finally {
                    if (conn != null) conn.disconnect();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Bitmap bmp) {
                if (bmp != null) ivAvatar.setImageBitmap(bmp);
            }
        }.execute();
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
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_rutinas) {
                startActivity(new Intent(this, RutinasActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_ejercicios) {
                startActivity(new Intent(this, EjerciciosActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_nutricion) {
                startActivity(new Intent(this, NutricionActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
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