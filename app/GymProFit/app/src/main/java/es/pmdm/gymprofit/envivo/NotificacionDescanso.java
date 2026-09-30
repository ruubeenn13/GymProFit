package es.pmdm.gymprofit.envivo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.activities.SesionEnVivoActivity;

// ============================================================
// NotificacionDescanso — «Descanso terminado · Hip thrust con barra, serie 3» (GP-013).
//
// Solo con la app fuera o la pantalla apagada: con la app delante avisa la pantalla.
// Va por un canal propio, «Descanso», de importancia alta, con el sonido y la vibración
// del sistema: el modo silencio y No molestar los aplica el sistema, no la app.
// Tocarla abre la sesión.
// ============================================================
public final class NotificacionDescanso {

    public static final String CANAL = "7";
    private static final int ID = 7001;

    private NotificacionDescanso() { }

    public static void publicar(@NonNull Context ctx, @NonNull SesionEnCursoRepositorio.FinDescanso fin) {
        if (!PermisosDescanso.notificaciones(ctx)) return;
        crearCanal(ctx);
        String ejercicio = TextosDescanso.ejercicio(ctx, fin.ejercicio, fin.ejercicioId);
        String texto = fin.numero > 0 ? ctx.getString(R.string.descanso_notif_texto, ejercicio, fin.numero) : null;
        Intent sesion = new Intent(ctx, SesionEnVivoActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent abrir = PendingIntent.getActivity(ctx, ID, sesion,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CANAL)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(ctx.getString(R.string.envivo_fin_titulo_sin_serie))
                .setContentText(texto)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(abrir);
        try {
            NotificationManagerCompat.from(ctx).notify(ID, b.build());
        } catch (SecurityException e) {
            // Se ignora a propósito: el permiso se retiró justo ahora. La sesión ya no tiene
            // el descanso, y al abrir la app se ve la serie que toca, resaltada.
        }
    }

    /** Quita el «Descanso terminado» anterior: al empezar otro descanso ya no dice nada. */
    public static void quitar(@NonNull Context ctx) {
        NotificationManagerCompat.from(ctx).cancel(ID);
    }

    private static void crearCanal(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CANAL) != null) return;
        NotificationChannel canal = new NotificationChannel(CANAL, ctx.getString(R.string.descanso_canal),
                NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription(ctx.getString(R.string.descanso_canal_descripcion));
        canal.enableVibration(true);
        nm.createNotificationChannel(canal);
    }
}
