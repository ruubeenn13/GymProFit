package es.pmdm.gymprofit.ui.widget;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.function.Supplier;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.utils.AvatarUtils;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

// ============================================================
// FotoPerfil — cambiar la foto de perfil desde la galería o la cámara (GP-105).
//
// Estaba dentro de la pestaña Perfil; ahora la usan el avatar de Progreso y «Foto de
// perfil» de Ajustes, así que vive aquí una vez. Los launchers se registran al crear
// la pantalla (antes de STARTED), por eso se construye en onCreate.
// ============================================================
public final class FotoPerfil {

    private final Supplier<Activity> actividad;
    private final int usuarioId;
    private final Runnable alSubir;
    private final ActivityResultLauncher<String> galeria;
    private final ActivityResultLauncher<Uri> camara;
    private final ActivityResultLauncher<String> permisoCamara;
    private Uri uriCamara;

    /**
     * @param caller    la Activity o el Fragment que la aloja.
     * @param actividad de dónde sacar la Activity cuando haga falta.
     * @param usuarioId dueño de la foto.
     * @param alSubir   se llama con la foto ya subida y guardada en AvatarUtils.
     */
    public FotoPerfil(ActivityResultCaller caller, Supplier<Activity> actividad, int usuarioId, Runnable alSubir) {
        this.actividad = actividad;
        this.usuarioId = usuarioId;
        this.alSubir = alSubir;
        galeria = caller.registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) subir(uri); });
        camara = caller.registerForActivityResult(new ActivityResultContracts.TakePicture(),
                ok -> { if (ok && uriCamara != null) subir(uriCamara); });
        permisoCamara = caller.registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                concedido -> {
                    if (concedido) lanzarCamara();
                    else Toast.makeText(actividad.get(), R.string.perfil_permiso_camara, Toast.LENGTH_SHORT).show();
                });
    }

    /** Pregunta de dónde sacar la foto: galería o cámara. */
    public void elegir() {
        Activity act = actividad.get();
        String[] opciones = {act.getString(R.string.perfil_foto_galeria), act.getString(R.string.perfil_foto_camara)};
        new MaterialAlertDialogBuilder(act)
                .setTitle(R.string.perfil_cambiar_foto)
                .setItems(opciones, (d, cual) -> {
                    if (cual == 0) galeria.launch("image/*");
                    else pedirPermisoCamara();
                })
                .show();
    }

    private void pedirPermisoCamara() {
        if (ContextCompat.checkSelfPermission(actividad.get(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            lanzarCamara();
        } else {
            permisoCamara.launch(Manifest.permission.CAMERA);
        }
    }

    private void lanzarCamara() {
        Activity act = actividad.get();
        File foto = new File(act.getCacheDir(), "perfil_temp.jpg");
        uriCamara = FileProvider.getUriForFile(act, act.getPackageName() + ".fileprovider", foto);
        camara.launch(uriCamara);
    }

    // Sube la foto y, si sale bien, la deja en AvatarUtils para Inicio y Progreso.
    private void subir(Uri uri) {
        Activity act = actividad.get();
        Toast.makeText(act, R.string.perfil_foto_subiendo, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try (InputStream is = act.getContentResolver().openInputStream(uri)) {
                if (is == null) {
                    act.runOnUiThread(() -> Toast.makeText(act, R.string.perfil_foto_error, Toast.LENGTH_SHORT).show());
                    return;
                }
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] trozo = new byte[8192];
                int n;
                while ((n = is.read(trozo)) != -1) buffer.write(trozo, 0, n);
                byte[] bytes = buffer.toByteArray();
                RequestBody cuerpo = RequestBody.create(bytes, MediaType.parse("image/jpeg"));
                MultipartBody.Part parte = MultipartBody.Part.createFormData("foto", "foto.jpg", cuerpo);

                ApiClient.service(UsuarioApi.class).subirFoto(usuarioId, parte).enqueue(new ApiCallback<Void>() {
                    @Override
                    public void onOk(Void body) {
                        Toast.makeText(act, R.string.perfil_foto_ok, Toast.LENGTH_SHORT).show();
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bmp != null) AvatarUtils.ponerFoto(usuarioId, bmp);
                        alSubir.run();
                    }
                    @Override
                    public void onFail(int code, String message) {
                        Toast.makeText(act, R.string.perfil_foto_error, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                act.runOnUiThread(() -> Toast.makeText(act, R.string.perfil_foto_error, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
