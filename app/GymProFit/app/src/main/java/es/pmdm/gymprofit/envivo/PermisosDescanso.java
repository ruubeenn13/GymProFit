package es.pmdm.gymprofit.envivo;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

// ============================================================
// PermisosDescanso — lo que hace falta para avisar con la pantalla apagada (GP-013).
//
// Dos cosas: alarmas exactas («Alarmas y recordatorios»; en Android 14 o más, las
// instalaciones nuevas lo tienen denegado de serie) y poder publicar notificaciones.
// Sin la segunda el aviso de fuera no puede salir, así que cuenta igual como «sin
// permiso». «Activar» lleva a lo que falte, primero las alarmas.
// ============================================================
public final class PermisosDescanso {

    private PermisosDescanso() { }

    /** Lo que falta, en el orden en que se pide. */
    public enum Falta { NADA, ALARMAS, NOTIFICACIONES }

    /** Si se pueden poner alarmas exactas. Antes de Android 12 no hacía falta pedirlo. */
    public static boolean alarmasExactas(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    /** Si se pueden publicar notificaciones (permiso y que no estén apagadas). */
    public static boolean notificaciones(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled();
    }

    @NonNull
    public static Falta falta(@NonNull Context ctx) {
        if (!alarmasExactas(ctx)) return Falta.ALARMAS;
        if (!notificaciones(ctx)) return Falta.NOTIFICACIONES;
        return Falta.NADA;
    }

    /** Abre la pantalla del sistema de lo que falte. */
    public static void activar(@NonNull Activity a) {
        Falta f = falta(a);
        Intent i;
        if (f == Falta.ALARMAS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + a.getPackageName()));
        } else if (f == Falta.NOTIFICACIONES && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName());
        } else if (f == Falta.NADA) {
            return;
        } else {
            i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.getPackageName()));
        }
        try {
            a.startActivity(i);
        } catch (ActivityNotFoundException e) {
            // Algún fabricante no tiene esa pantalla: los ajustes de la app, que siempre están.
            a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + a.getPackageName())));
        }
    }
}
