package es.pmdm.gymprofit.envivo;

import android.Manifest;
import android.app.Notification;
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
//
// GP-210 paso 3 (momento 27): en Android 16 se pide que el sistema la promueva a Live
// Update —chip en la barra de estado y tarjeta en la pantalla de bloqueo— y, durante el
// descanso, lleva «+15 s» y «Saltar» en todas las versiones. Qué lleva lo decide
// PlanNotificacionSesion; los botones llegan a AccionDescansoReceiver. Si el sistema no la
// promueve (versión anterior, ajuste apagado), sigue siendo la notificación de siempre, ya
// con los dos botones.
// ============================================================
public final class NotificacionSesion {

    /** Canal propio, sin sonido: es un reloj, no un aviso. */
    public static final String CANAL = "6";
    private static final int ID = 6001;

    // La petición de promoción a Live Update de Android 16 (API 36.1: Notification
    // .EXTRA_REQUEST_PROMOTED_ONGOING). compileSdk 36 aún no trae setRequestPromotedOngoing,
    // pero el método solo escribe este extra, así que se escribe a mano y no hace falta subir
    // el SDK. Android 15 y anteriores lo ignoran.
    private static final String EXTRA_PEDIR_PROMOCION = "android.requestPromotedOngoing";

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
        PlanNotificacionSesion plan = PlanNotificacionSesion.de(s, System.currentTimeMillis());

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
        if (plan.promovida) {
            android.os.Bundle extras = new android.os.Bundle();
            extras.putBoolean(EXTRA_PEDIR_PROMOCION, true);
            b.addExtras(extras);
        }
        if (plan.masQuince) b.addAction(0, ctx.getString(R.string.envivo_descanso_mas), accion(ctx, AccionDescanso.ACCION_MAS, 1));
        if (plan.saltar) b.addAction(0, ctx.getString(R.string.envivo_descanso_saltar), accion(ctx, AccionDescanso.ACCION_SALTAR, 2));
        if (s.descanso != null) NotificacionDescanso.quitar(ctx);
        if (!sinGuardar && s.descanso != null) {
            // Descanso en marcha (GP-013): cuenta hacia atrás hasta su fin (lo lleva el
            // sistema, sin refrescos). Al acabar, la sesión cambia y vuelve el reloj.
            // Sin los minutos de sesión (GP-143): sin refrescos, se quedaban parados en
            // los del principio del descanso.
            LogicaDescanso.Siguiente sig = LogicaDescanso.siguiente(s);
            SesionEnCurso.Ejercicio e = sig != null ? sig.ejercicio
                    : SesionEnCursoRepositorio.ejercicioDeSerie(s, s.descanso.serieId);
            String ejercicio = e != null ? TextosDescanso.ejercicio(ctx, e.nombre, e.ejercicioId) : "";
            b.setContentTitle(ctx.getString(R.string.envivo_notif_descanso_titulo, ejercicio))
                    .setContentText(sig != null
                            ? ctx.getString(R.string.envivo_notif_descanso_texto, sig.numero, nombre)
                            : nombre)
                    .setWhen(s.descanso.finMs).setShowWhen(true).setUsesChronometer(true)
                    .setChronometerCountDown(true);
        } else if (!sinGuardar) {
            b.setWhen(s.inicioMs).setShowWhen(true).setUsesChronometer(true);
        }
        Notification n = b.build();
        if (plan.promovida && plan.ejercicioDelDescanso != null) n = conBarraDeSeries(ctx, n, plan);
        try {
            NotificationManagerCompat.from(ctx).notify(ID, n);
        } catch (SecurityException e) {
            // Se ignora a propósito: el permiso se retiró entre la comprobación y el aviso.
            // La barra de la app sigue diciendo que hay una sesión en curso.
        }
    }

    // En Android 16 el Live Update lleva una barra por tramos: una por serie del ejercicio al que
    // toca volver, con las hechas rellenas («Press de banca: 3 de 4 series hechas»). Es el estilo
    // que admite la promoción. Se pone sobre la notificación ya hecha: recoverBuilder conserva
    // sus extras (la petición de promoción), sus botones y su reloj. Antes de Android 16 no
    // existe y la notificación sigue sin barra.
    private static Notification conBarraDeSeries(Context ctx, Notification n, PlanNotificacionSesion plan) {
        if (Build.VERSION.SDK_INT < 36 || plan.seriesTotales < 1) return n;
        int color = ContextCompat.getColor(ctx, R.color.gp_primary);
        java.util.List<Notification.ProgressStyle.Segment> tramos = new java.util.ArrayList<>();
        for (int i = 0; i < plan.seriesTotales; i++) {
            tramos.add(new Notification.ProgressStyle.Segment(1).setColor(color));
        }
        Notification.ProgressStyle estilo = new Notification.ProgressStyle()
                .setStyledByProgress(true)
                .setProgressSegments(tramos)
                .setProgress(plan.seriesHechas);
        return Notification.Builder.recoverBuilder(ctx, n).setStyle(estilo).build();
    }

    // Un botón de la notificación: llega a AccionDescansoReceiver, que no se exporta.
    private static PendingIntent accion(Context ctx, String accion, int codigo) {
        Intent i = new Intent(ctx, AccionDescansoReceiver.class).setAction(accion);
        return PendingIntent.getBroadcast(ctx, ID + codigo, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
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
