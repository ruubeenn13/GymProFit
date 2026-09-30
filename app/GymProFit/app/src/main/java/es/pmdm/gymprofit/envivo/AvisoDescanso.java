package es.pmdm.gymprofit.envivo;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.annotation.NonNull;

// ============================================================
// AvisoDescanso — el fin del descanso con la app delante (GP-013).
//
// Con la app delante no se publica notificación (la pantalla ya lo dice con «¡A por la
// serie N!»), así que lo que haría el sistema con ella se hace aquí, respetando lo que
// diga el móvil: normal, suena y vibra; vibración, solo vibra; silencio o No molestar,
// nada (LogicaDescanso.aviso, con sus tests). El sonido es el de notificación del
// sistema, por el flujo de notificaciones: su volumen es el que el usuario ya eligió.
// ============================================================
public final class AvisoDescanso {

    private AvisoDescanso() { }

    /** Dos toques cortos: distinto del «corta» y «doble» del cronómetro. */
    private static final long[] PATRON = {0, 250, 150, 250, 150, 250};

    /** Avisa del fin del descanso según el modo del móvil y No molestar. */
    public static void avisarDelante(@NonNull Context ctx) {
        LogicaDescanso.Aviso a = LogicaDescanso.aviso(timbre(ctx), noMolestar(ctx));
        if (a.vibrar) vibrar(ctx);
        if (a.sonar) sonar(ctx);
    }

    @NonNull
    static LogicaDescanso.Timbre timbre(@NonNull Context ctx) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        int modo = am != null ? am.getRingerMode() : AudioManager.RINGER_MODE_NORMAL;
        if (modo == AudioManager.RINGER_MODE_SILENT) return LogicaDescanso.Timbre.SILENCIO;
        if (modo == AudioManager.RINGER_MODE_VIBRATE) return LogicaDescanso.Timbre.VIBRACION;
        return LogicaDescanso.Timbre.NORMAL;
    }

    static boolean noMolestar(@NonNull Context ctx) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return false;
        int filtro = nm.getCurrentInterruptionFilter();
        return filtro != NotificationManager.INTERRUPTION_FILTER_ALL
                && filtro != NotificationManager.INTERRUPTION_FILTER_UNKNOWN;
    }

    private static void vibrar(Context ctx) {
        Vibrator v;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = ctx.getSystemService(VibratorManager.class);
            v = vm != null ? vm.getDefaultVibrator() : null;
        } else {
            v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (v == null || !v.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(PATRON, -1));
        } else {
            v.vibrate(PATRON, -1);
        }
    }

    private static void sonar(Context ctx) {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (uri == null) return;
        Ringtone r = RingtoneManager.getRingtone(ctx, uri);
        if (r == null) return;
        r.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        r.play();
    }
}
