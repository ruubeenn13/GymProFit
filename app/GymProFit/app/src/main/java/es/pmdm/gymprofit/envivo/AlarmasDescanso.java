package es.pmdm.gymprofit.envivo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

// ============================================================
// AlarmasDescanso — el AlarmManager del fin del descanso (GP-013).
//
// Una sola alarma (un PendingIntent fijo a FinDescansoReceiver): poner otra la sustituye.
// Exacta con setExactAndAllowWhileIdle, que suena aunque el móvil esté en reposo, si
// hay permiso de alarmas exactas; sin él, setWindow, que el sistema puede retrasar. Se
// pide SCHEDULE_EXACT_ALARM y nunca USE_EXACT_ALARM: Google Play reserva ese a las apps
// de alarmas, temporizadores y calendarios.
// ============================================================
public final class AlarmasDescanso implements ProgramadorAlarma.Alarmas {

    private static final int CODIGO = 7001;

    private final Context ctx;

    public AlarmasDescanso(@NonNull Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    @Override
    public boolean puedeExactas() {
        return PermisosDescanso.alarmasExactas(ctx);
    }

    @Override
    public void exacta(long cuandoMs) {
        AlarmManager am = manager();
        if (am == null) return;
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuandoMs, intento());
        } catch (SecurityException e) {
            // El permiso se retiró entre la comprobación y la llamada: la ventana llega
            // igual, aunque pueda retrasarse. Es lo que pasa sin permiso.
            am.setWindow(AlarmManager.RTC_WAKEUP, cuandoMs, ProgramadorAlarma.VENTANA_MS, intento());
        }
    }

    @Override
    public void ventana(long cuandoMs, long largoMs) {
        AlarmManager am = manager();
        if (am != null) am.setWindow(AlarmManager.RTC_WAKEUP, cuandoMs, largoMs, intento());
    }

    @Override
    public void cancelar() {
        AlarmManager am = manager();
        if (am != null) am.cancel(intento());
    }

    private AlarmManager manager() {
        return (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
    }

    private PendingIntent intento() {
        Intent i = new Intent(ctx, FinDescansoReceiver.class).setAction(FinDescansoReceiver.ACCION);
        return PendingIntent.getBroadcast(ctx, CODIGO, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
