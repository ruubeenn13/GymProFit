package es.pmdm.gymprofit.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.activities.LogrosActivity;
import es.pmdm.gymprofit.ui.activities.MedicionesActivity;
import es.pmdm.gymprofit.ui.activities.RutinasActivity;
import es.pmdm.gymprofit.ui.activities.SesionesActivity;

public class NotificationHelper {

    private static final String CANAL_SESIONES   = "1";
    private static final String CANAL_MEDICIONES  = "2";
    private static final String CANAL_RUTINAS     = "3";
    private static final String CANAL_LOGROS      = "4";

    /** Notificación simple al completar una sesión de entrenamiento. */
    public static void notificarSesionCompletada(Context ctx, int duracion, int calorias) {
        // 1.- Crear la notificación con sus propiedades
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CANAL_SESIONES);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        builder.setContentTitle(ctx.getString(R.string.notif_sesion_titulo));
        builder.setContentText(ctx.getString(R.string.notif_sesion_texto, duracion, calorias));
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setAutoCancel(true);

        Intent intent = new Intent(ctx, SesionesActivity.class);
        PendingIntent pending = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pending);

        enviar(ctx, CANAL_SESIONES, ctx.getString(R.string.notif_canal_sesiones), 1, builder);
    }

    /** Notificación simple al guardar una medición corporal. */
    public static void notificarMedicionGuardada(Context ctx) {
        // 1.- Crear la notificación con sus propiedades
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CANAL_MEDICIONES);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        builder.setContentTitle(ctx.getString(R.string.notif_medicion_titulo));
        builder.setContentText(ctx.getString(R.string.notif_medicion_texto));
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setAutoCancel(true);

        Intent intent = new Intent(ctx, MedicionesActivity.class);
        PendingIntent pending = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pending);

        enviar(ctx, CANAL_MEDICIONES, ctx.getString(R.string.notif_canal_mediciones), 2, builder);
    }

    /** Notificación simple al crear una rutina. */
    public static void notificarRutinaCreada(Context ctx, String nombre) {
        // 1.- Crear la notificación con sus propiedades
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CANAL_RUTINAS);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        builder.setContentTitle(ctx.getString(R.string.notif_rutina_titulo));
        builder.setContentText(ctx.getString(R.string.notif_rutina_texto, nombre));
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setAutoCancel(true);

        Intent intent = new Intent(ctx, RutinasActivity.class);
        PendingIntent pending = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pending);

        enviar(ctx, CANAL_RUTINAS, ctx.getString(R.string.notif_canal_rutinas), 3, builder);
    }

    /** Notificación expandible (InboxStyle) con los logros desbloqueados. */
    public static void notificarLogrosDesbloqueados(Context ctx, List<String> nombres) {
        // 1.- Crear la notificación con sus propiedades (estilo expandible)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CANAL_LOGROS);
        builder.setSmallIcon(android.R.drawable.star_big_on);
        builder.setContentTitle(ctx.getString(R.string.notif_logros_titulo));
        builder.setContentText(ctx.getString(R.string.notif_logros_texto, nombres.size()));
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setAutoCancel(true);

        NotificationCompat.InboxStyle estilo = new NotificationCompat.InboxStyle();
        estilo.setBigContentTitle(ctx.getString(R.string.notif_logros_titulo));
        for (String n : nombres) {
            estilo.addLine(n);
        }
        builder.setStyle(estilo);

        Intent intent = new Intent(ctx, LogrosActivity.class);
        PendingIntent pending = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pending);

        enviar(ctx, CANAL_LOGROS, ctx.getString(R.string.notif_canal_logros), 4, builder);
    }

    private static void enviar(Context ctx, String canalId, String canalNombre,
                                int notifId, NotificationCompat.Builder builder) {
        // 2.- Añadir el canal al sistema de notificaciones
        NotificationManager manager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    canalId, canalNombre, NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(canal);
        }

        // 3.- Enviar la notificación al canal para mostrarla
        Notification notificacion = builder.build();
        manager.notify(notifId, notificacion);
    }
}
