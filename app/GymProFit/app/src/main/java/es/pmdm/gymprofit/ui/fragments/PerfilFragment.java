package es.pmdm.gymprofit.ui.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.MedicionApi;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.ui.activities.AcercaDeActivity;
import es.pmdm.gymprofit.ui.activities.AdminActivity;
import es.pmdm.gymprofit.ui.activities.EditarPerfilActivity;
import es.pmdm.gymprofit.ui.activities.LogrosActivity;
import es.pmdm.gymprofit.ui.activities.MedicionesActivity;
import es.pmdm.gymprofit.ui.activities.SesionesActivity;
import es.pmdm.gymprofit.utils.UiFeedback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

// ============================================================
// PerfilFragment — pestaña de perfil del usuario.
// Muestra los datos personales y nutricionales, la última medición corporal y
// la foto de perfil (con opción de cambiarla desde galería o cámara). Da acceso
// a sesiones, mediciones, logros, ajustes de admin y "Acerca de".
// ============================================================
public class PerfilFragment extends BaseFragment {
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
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);
    private final MedicionApi medicionApi = ApiClient.service(MedicionApi.class);

    // Registra los launchers (editar perfil, mediciones, galería, cámara y
    // permiso de cámara) antes de que el fragment alcance STARTED.
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        editarPerfilLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) configurarDatosUsuario();
                });

        medicionesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        int uid = prefsManager.getUsuarioId();
                        if (uid != -1) cargarUltimaMedicion(uid);
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) subirFoto(uri); });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                ok -> { if (ok && cameraUri != null) subirFoto(cameraUri); });

        cameraPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) lanzarCamara();
                    else Toast.makeText(requireContext(), R.string.perfil_permiso_camara, Toast.LENGTH_SHORT).show();
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMenuButton();
        inicializarVistas();
        configurarDatosUsuario();
        configurarBotones();
    }

    // Enlaza las vistas y configura el click sobre el avatar para cambiar la foto.
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
        ((View) ivAvatar.getParent()).setOnClickListener(v -> {
            if (!verificarAccesoRegistrado()) return;
            mostrarDialogoFoto();
        });
    }

    // Carga y muestra los datos del usuario actual (medición, foto y datos).
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

        usuarioApi.getPorId(usuarioId).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (u == null || !isAdded()) return;
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
            }

            @Override
            public void onFail(int code, String message) {
                Log.e("GymProFit", "getUsuarioPorId error status=" + code + " msg=" + message);
                if (!isAdded()) return;
                String username = prefsManager.getUsername();
                if (username != null && !username.isEmpty()) {
                    tvNombreUsuario.setText(username);
                    tvInfoNombre.setText(username);
                }
                UiFeedback.toastError(requireActivity(), code, message);
            }
        });
    }

    // Diálogo para elegir el origen de la nueva foto (galería o cámara).
    private void mostrarDialogoFoto() {
        String[] opciones = {
            getString(R.string.perfil_foto_galeria),
            getString(R.string.perfil_foto_camara)
        };
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.perfil_cambiar_foto)
                .setItems(opciones, (d, which) -> {
                    if (which == 0) galleryLauncher.launch("image/*");
                    else pedirPermisoCamara();
                })
                .show();
    }

    // Pide el permiso de cámara si falta, o lanza la cámara si ya está concedido.
    private void pedirPermisoCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            lanzarCamara();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // Crea un archivo temporal y lanza la cámara para capturar la foto en él.
    private void lanzarCamara() {
        File foto = new File(requireContext().getCacheDir(), "perfil_temp.jpg");
        cameraUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", foto);
        cameraLauncher.launch(cameraUri);
    }

    // Sube la foto seleccionada/capturada a la API y actualiza el avatar.
    private void subirFoto(Uri uri) {
        int uid = prefsManager.getUsuarioId();
        if (uid == -1) return;
        final FragmentActivity act = requireActivity();

        Toast.makeText(act, R.string.perfil_foto_subiendo, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try (InputStream is = act.getContentResolver().openInputStream(uri)) {
                if (is == null) {
                    act.runOnUiThread(() ->
                            Toast.makeText(act, R.string.perfil_foto_error, Toast.LENGTH_SHORT).show());
                    return;
                }
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int n;
                while ((n = is.read(chunk)) != -1) buffer.write(chunk, 0, n);
                RequestBody fileBody = RequestBody.create(buffer.toByteArray(), MediaType.parse("image/jpeg"));
                MultipartBody.Part part = MultipartBody.Part.createFormData("foto", "foto.jpg", fileBody);

                usuarioApi.subirFoto(uid, part).enqueue(new ApiCallback<Void>() {
                    @Override
                    public void onOk(Void body) {
                        Toast.makeText(act, R.string.perfil_foto_ok, Toast.LENGTH_SHORT).show();
                        if (!isAdded()) return;
                        ivAvatar.setImageURI(null);
                        ivAvatar.setImageURI(uri);
                    }

                    @Override
                    public void onFail(int code, String message) {
                        Toast.makeText(act, R.string.perfil_foto_error, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                act.runOnUiThread(() ->
                        Toast.makeText(act, R.string.perfil_foto_error, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Descarga y muestra la foto de perfil; silencioso si no hay o falla.
    private void cargarFotoPerfil(int userId) {
        usuarioApi.descargarFoto(userId).enqueue(new ApiCallback<ResponseBody>() {
            @Override
            public void onOk(ResponseBody body) {
                if (body == null || !isAdded()) return;
                Bitmap bmp = BitmapFactory.decodeStream(body.byteStream());
                if (bmp != null) ivAvatar.setImageBitmap(bmp);
            }

            @Override
            public void onFail(int code, String message) { }
        });
    }

    // Muestra el peso/altura de la medición más reciente en el resumen.
    private void cargarUltimaMedicion(int usuarioId) {
        medicionApi.getOrdenadas(usuarioId).enqueue(new ApiCallback<List<MedicionCorporal>>() {
            @Override
            public void onOk(List<MedicionCorporal> lista) {
                if (lista == null || lista.isEmpty() || !isAdded()) return;
                MedicionCorporal ultima = lista.get(0);
                boolean tienePeso = ultima.getPeso() > 0;
                boolean tieneAltura = ultima.getAltura() > 0;
                if (tienePeso || tieneAltura) {
                    llMedicionesResumen.setVisibility(View.VISIBLE);
                    tvPesoMedicion.setText(tienePeso
                            ? getString(R.string.perfil_kg, String.format(java.util.Locale.getDefault(), "%.1f", ultima.getPeso())) : "");
                    tvAlturaMedicion.setText(tieneAltura
                            ? getString(R.string.perfil_cm, (int) ultima.getAltura()) : "");
                }
            }

            @Override
            public void onFail(int code, String message) {}
        });
    }

    // Accesos del perfil: editar, sesiones, mediciones, logros, acerca de, admin.
    private void configurarBotones() {
        findViewById(R.id.btnEditarPerfil).setOnClickListener(v -> {
            if (!verificarAccesoRegistrado()) return;
            editarPerfilLauncher.launch(new Intent(requireContext(), EditarPerfilActivity.class));
        });

        findViewById(R.id.itemSesiones).setOnClickListener(v -> {
            if (!verificarAccesoRegistrado()) return;
            startActivity(new Intent(requireContext(), SesionesActivity.class));
        });

        findViewById(R.id.itemMediciones).setOnClickListener(v -> {
            if (!verificarAccesoRegistrado()) return;
            medicionesLauncher.launch(new Intent(requireContext(), MedicionesActivity.class));
        });

        findViewById(R.id.itemLogros).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LogrosActivity.class)));

        findViewById(R.id.btnAcercaDe).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AcercaDeActivity.class)));

        View itemAdmin = findViewById(R.id.itemAdmin);
        if (prefsManager.isAdmin()) {
            itemAdmin.setVisibility(View.VISIBLE);
            itemAdmin.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), AdminActivity.class)));
        } else {
            itemAdmin.setVisibility(View.GONE);
        }
    }

    // Devuelve el valor si es válido (no nulo/vacío/"null"), o el fallback.
    private String val(String s, String fallback) {
        return (s != null && !s.isEmpty() && !"null".equals(s)) ? s : fallback;
    }

    // Traduce el nivel de experiencia (API) a su texto localizado.
    private String mapearNivel(String nivel) {
        if (nivel == null) return getString(R.string.perfil_sin_datos);
        switch (nivel) {
            case "PRINCIPIANTE": return getString(R.string.nivel_principiante);
            case "INTERMEDIO":   return getString(R.string.nivel_intermedio);
            case "AVANZADO":     return getString(R.string.nivel_avanzado);
            default:             return nivel;
        }
    }

    // Traduce el enum de objetivo (API) a su texto localizado.
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
