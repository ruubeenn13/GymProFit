package es.pmdm.gymprofit.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import okhttp3.ResponseBody;

// ============================================================
// AvatarUtils — el avatar de Inicio y de Progreso (GP-105): la foto de perfil si la
// hay y, si no, la inicial.
//
// La foto se pide una vez por sesión de la app y se guarda aquí: Inicio y Progreso
// están vivas a la vez en el pager, y sin la copia cada una la descargaría. Cambiar
// la foto la sustituye (ponerFoto) y cerrar sesión la olvida (olvidar).
// ============================================================
public final class AvatarUtils {

    private static final UsuarioApi API = ApiClient.service(UsuarioApi.class);

    @Nullable private static Bitmap foto;
    private static int usuarioDeLaFoto = -1;
    private static boolean consultado = false;

    private AvatarUtils() { }

    /**
     * Pinta el avatar: la inicial ya, y la foto en cuanto llegue si la hay.
     *
     * @param avatar     la raíz de view_avatar.
     * @param nombre     de dónde sale la inicial.
     * @param usuarioId  -1 para el invitado (solo inicial).
     * @param colorTexto color de la inicial.
     */
    public static void pintar(View avatar, @Nullable String nombre, int usuarioId, @ColorInt int colorTexto) {
        TextView inicial = avatar.findViewById(R.id.tvInicial);
        ImageView ivFoto = avatar.findViewById(R.id.ivFoto);
        String n = nombre == null ? "" : nombre.trim();
        inicial.setText(n.isEmpty() ? "" : n.substring(0, 1).toUpperCase(Locale.ROOT));
        inicial.setTextColor(colorTexto);

        if (usuarioId != usuarioDeLaFoto) {
            foto = null;
            consultado = false;
            usuarioDeLaFoto = usuarioId;
        }
        mostrar(ivFoto, foto);
        if (usuarioId == -1 || consultado) return;

        consultado = true;
        API.descargarFoto(usuarioId).enqueue(new ApiCallback<ResponseBody>() {
            @Override
            public void onOk(ResponseBody body) {
                if (body == null) return;
                Bitmap bmp = BitmapFactory.decodeStream(body.byteStream());
                if (bmp == null || usuarioDeLaFoto != usuarioId) return;
                foto = bmp;
                mostrar(ivFoto, bmp);
            }
            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito (GP-017): sin foto se queda la inicial, que es lo
                // que tiene la mayoría (la API responde 404 a quien no tiene foto). Si fue
                // un fallo de red, se vuelve a intentar la próxima vez que se pinte.
                if (code != 404) {
                    consultado = false;
                    Log.w("GymProFit", "Foto de perfil no disponible (" + code + ")");
                }
            }
        });
    }

    /** La foto recién subida: la enseñan ya Inicio y Progreso sin volver a pedirla. */
    public static void ponerFoto(int usuarioId, Bitmap bmp) {
        usuarioDeLaFoto = usuarioId;
        foto = bmp;
        consultado = true;
    }

    /** Al cerrar sesión o borrar la cuenta. */
    public static void olvidar() {
        foto = null;
        usuarioDeLaFoto = -1;
        consultado = false;
    }

    private static void mostrar(ImageView iv, @Nullable Bitmap bmp) {
        iv.setImageBitmap(bmp);
        iv.setVisibility(bmp == null ? View.GONE : View.VISIBLE);
    }
}
