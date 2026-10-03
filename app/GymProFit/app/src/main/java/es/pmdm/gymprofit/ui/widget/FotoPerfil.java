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

import java.io.File;
import java.util.function.Supplier;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.utils.AvatarUtils;
import es.pmdm.gymprofit.utils.ErrorFoto;
import es.pmdm.gymprofit.utils.FotoParaSubir;
import es.pmdm.gymprofit.utils.UiFeedback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

// ============================================================
// FotoPerfil — cambiar la foto de perfil desde la galería o la cámara (GP-105).
//
// Estaba dentro de la pestaña Perfil; ahora la usan el avatar de Progreso y «Foto de
// perfil» de Ajustes, así que vive aquí una vez. Los launchers se registran al crear
// la pantalla (antes de STARTED), por eso se construye en onCreate.
// Desde la 1.6.5 (GP-188) la foto se reduce antes de subirla (FotoParaSubir: 512 × 512,
// JPEG 85, sin metadatos), fuera del hilo principal; si falla, el aviso dice por qué
// (ErrorFoto); y la foto temporal de la cámara se borra al acabar.
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
        File foto = temporal(act);
        uriCamara = FileProvider.getUriForFile(act, act.getPackageName() + ".fileprovider", foto);
        camara.launch(uriCamara);
    }

    private static File temporal(Activity act) {
        return new File(act.getCacheDir(), "perfil_temp.jpg");
    }

    // Reduce la foto fuera del hilo principal, la sube y, si sale bien, la deja en
    // AvatarUtils para Inicio y Progreso. Al acabar, bien o mal, borra la de la cámara.
    private void subir(Uri uri) {
        Activity act = actividad.get();
        Toast.makeText(act, R.string.perfil_foto_subiendo, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            byte[] bytes;
            try {
                bytes = FotoParaSubir.preparar(act.getContentResolver(), uri);
            } catch (Exception | OutOfMemoryError e) {
                act.runOnUiThread(() -> {
                    borrarTemporal(act);
                    Toast.makeText(act, R.string.perfil_foto_ilegible, Toast.LENGTH_LONG).show();
                });
                return;
            }
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            RequestBody cuerpo = RequestBody.create(bytes, MediaType.parse("image/jpeg"));
            MultipartBody.Part parte = MultipartBody.Part.createFormData("foto", "foto.jpg", cuerpo);
            ApiClient.service(UsuarioApi.class).subirFoto(usuarioId, parte).enqueue(new ApiCallback<Void>() {
                @Override
                public void onOk(Void body) {
                    borrarTemporal(act);
                    Toast.makeText(act, R.string.perfil_foto_ok, Toast.LENGTH_SHORT).show();
                    if (bmp != null) AvatarUtils.ponerFoto(usuarioId, bmp);
                    alSubir.run();
                }

                @Override
                public void onFail(int code, String message) {
                    borrarTemporal(act);
                    Toast.makeText(act, aviso(act, code, message), Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    // El aviso por su causa: sin conexión, demasiado grande o lo que diga UiFeedback.
    private static String aviso(Activity act, int code, String message) {
        switch (ErrorFoto.causa(code, hayRed(act))) {
            case SIN_CONEXION:   return act.getString(R.string.perfil_foto_sin_conexion);
            case PESA_DEMASIADO: return act.getString(R.string.perfil_foto_pesa);
            default:             return act.getString(R.string.perfil_foto_no_subida, UiFeedback.mensaje(act, code, message));
        }
    }

    private static boolean hayRed(Activity act) {
        android.net.ConnectivityManager cm = act.getSystemService(android.net.ConnectivityManager.class);
        if (cm == null) return true;
        android.net.NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return nc != null && nc.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    // La foto de la cámara solo hacía falta para subirla.
    private static void borrarTemporal(Activity act) {
        File f = temporal(act);
        if (f.exists() && !f.delete()) android.util.Log.w("GymProFit", "No se ha podido borrar la foto temporal");
    }
}
