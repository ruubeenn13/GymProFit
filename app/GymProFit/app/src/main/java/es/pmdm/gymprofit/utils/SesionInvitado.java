package es.pmdm.gymprofit.utils;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.activities.BienvenidaActivity;

// ============================================================
// SesionInvitado — lo que queda del invitado en la app (GP-150, lote 1.5.3).
//
// La 1.4.0 dejaba entrar como invitado (rol GUEST) y guardaba esa sesión como cualquier
// otra. La app ya no tiene invitado: el plan se ve sin cuenta desde la 1.5.1. Una sesión
// de invitado que siga en el móvil se cierra al abrir y lleva a la bienvenida, sin
// mensajes: quien la tenía no guardaba nada en ella. La API no se toca: POST /auth/guest y
// el rol se quedan mientras haya 1.4.0 repartidas.
// ============================================================
public final class SesionInvitado {

    private SesionInvitado() {}

    /** El rol que la 1.4.0 guardaba para el invitado. */
    public static final String ROL = "ROLE_GUEST";

    /** Si la sesión guardada es de invitado. Sin Android, para probarlo en la JVM. */
    public static boolean esDeInvitado(boolean haySesion, @Nullable String rol) {
        return haySesion && ROL.equals(rol);
    }

    /**
     * Si la sesión guardada es de invitado, la cierra y abre la bienvenida en una tarea
     * nueva, y cierra la pantalla que llama.
     *
     * @return true si la ha cerrado: quien llama no debe seguir montando su pantalla.
     */
    public static boolean cerrarSiHay(@NonNull Activity desde, @NonNull PreferencesManager prefs) {
        if (!esDeInvitado(prefs.haySesion(), prefs.getRol())) return false;
        UtilREST.clearToken();
        prefs.cerrarSesion();
        prefs.olvidarRol();
        desde.startActivity(new Intent(desde, BienvenidaActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        desde.finish();
        return true;
    }
}
