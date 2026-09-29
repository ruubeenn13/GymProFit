package es.pmdm.gymprofit.network;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.utils.PerfilCuenta;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// PerfilRemoto — al entrar, cruza el perfil de la API con el del móvil (GP-111).
//
// La decisión (de quién es lo guardado, qué se trae y qué se sube) es de PerfilCuenta;
// aquí solo se pide el perfil cuando hace falta y se manda el PATCH que salga. Se llama
// desde el login, con el usuario que ya ha pedido, y desde la splash al arrancar con
// sesión. Ninguna de las dos espera a que termine: lo guardado en el móvil ya vale
// para pintar, y lo que llegue se usa en cuanto una pantalla vuelva a leerlo.
// ============================================================
public final class PerfilRemoto {

    private PerfilRemoto() {}

    /**
     * Arranque con sesión: pide el perfil a la API y lo cruza con el del móvil.
     * Los invitados no tienen perfil que guardar.
     */
    public static void alArrancar(@NonNull Context context) {
        Context app = context.getApplicationContext();
        PreferencesManager prefs = new PreferencesManager(app);
        int id = prefs.getUsuarioId();
        if (id == -1 || prefs.isGuest()) return;

        ApiClient.service(UsuarioApi.class).getPorId(id).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                alEntrar(app, u);
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: la app sigue con lo guardado en el móvil, como
                // antes de GP-111, y el siguiente arranque o login lo vuelve a intentar.
                // El 401 ya lo resuelve el aviso global de ApiCallback.
            }
        });
    }

    /**
     * Con el perfil ya pedido (login o arranque): guarda en el móvil lo que traiga la
     * API y sube, una vez, lo que solo esté en el móvil y sea de esta cuenta.
     */
    public static void alEntrar(@NonNull Context context, @Nullable Usuario u) {
        PreferencesManager prefs = new PreferencesManager(context.getApplicationContext());
        String usuario = prefs.getUsername();
        if (u == null || prefs.isGuest() || usuario == null || usuario.isEmpty()) return;

        Map<String, Object> subir = PerfilCuenta.alEntrar(prefs, usuario, u.getSexo(), u.getNivelActividad());
        if (subir.isEmpty()) return;

        ApiClient.service(UsuarioApi.class).patch(u.getId(), subir).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void response) {
                // Nada más que hacer: la próxima vez la API ya los trae.
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: los valores siguen en el móvil y son de esta
                // cuenta, así que se vuelven a subir al entrar la próxima vez. El usuario
                // no ha pedido nada y no hay nada que pueda hacer con el aviso.
            }
        });
    }
}
