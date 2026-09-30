package es.pmdm.gymprofit.envivo;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.ui.activities.SesionEnVivoActivity;

// ============================================================
// NotificacionSesion — la notificación continua de la sesión en curso (GP-012).
//
// Con el reloj (lo lleva el sistema: setUsesChronometer desde el inicio de la sesión,
// sin servicio ni refrescos), en un canal propio «Sesión en curso» sin sonido. Tocarla
// abre la sesión, con Inicio detrás. Se quita al guardar o descartar. Sin permiso de
// notificaciones no se pinta y queda solo la barra de la app.
//
// La mueve el observador de la sesión que monta GymProFitApp: cada cambio de la sesión
// la vuelve a publicar (silenciosa, sin repetir aviso) o la quita si ya no hay.
// ============================================================
public final class NotificacionSesion {

    /** Canal propio, sin sonido: es un reloj, no un aviso. */
    public static final String CANAL = "6";
    private static final int ID = 6001;

    private NotificacionSesion() { }

    /** Publica o quita la notificación según haya sesión o no. */
    public static void pintar(@NonNull Context ctx, @Nullable SesionEnCurso s) {
        if (s == null) {
            quitar(ctx);
            return;
        }
        if (!permitida(ctx)) return;
        crearCanal(ctx);

        String nombre = s.rutinaNombre != null && !s.rutinaNombre.isEmpty()
                ? s.rutinaNombre : ctx.getString(R.string.sesiones_entrenamiento_libre);
        boolean sinGuardar = s.guardado == SesionEnCurso.Guardado.FALLO;

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CANAL)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(ctx.getString(R.string.envivo_notif_titulo))
                .setContentText(sinGuardar ? ctx.getString(R.string.envivo_notif_sin_guardar) : nombre)
                .setSubText(sinGuardar ? nombre : null)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_WORKOUT)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(abrir(ctx));
        if (!sinGuardar) {
            b.setWhen(s.inicioMs).setShowWhen(true).setUsesChronometer(true);
        }
        try {
            NotificationManagerCompat.from(ctx).notify(ID, b.build());
        } catch (SecurityException e) {
            // Se ignora a propósito: el permiso se retiró entre la comprobación y el aviso.
            // La barra de la app sigue diciendo que hay una sesión en curso.
        }
    }

    /** Quita la notificación (al guardar, al descartar, al cambiar de cuenta). */
    public static void quitar(@NonNull Context ctx) {
        NotificationManagerCompat.from(ctx).cancel(ID);
    }

    private static boolean permitida(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled();
    }

    private static void crearCanal(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CANAL) != null) return;
        NotificationChannel canal = new NotificationChannel(CANAL, ctx.getString(R.string.envivo_canal),
                NotificationManager.IMPORTANCE_LOW);
        canal.setSound(null, null);
        canal.enableVibration(false);
        canal.setShowBadge(false);
        nm.createNotificationChannel(canal);
    }

    // Abre la sesión encima de lo que haya. Si la app no estaba abierta, la sesión queda
    // sola en la tarea y, al minimizarla, ella misma abre Inicio (ver la pantalla).
    private static PendingIntent abrir(Context ctx) {
        Intent sesion = new Intent(ctx, SesionEnVivoActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(ctx, ID, sesion,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
