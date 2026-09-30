package es.pmdm.gymprofit.envivo;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// VigilanteDescanso — quien ve terminar el descanso con la app delante (GP-013).
//
// Mientras alguna pantalla de la app está a la vista, mira el descanso una vez por
// segundo y, al terminar, avisa (AvisoDescanso) una sola vez. Está en la aplicación y no
// en una pantalla porque el aviso tiene que llegar esté donde esté el usuario: en la
// sesión, en Inicio con la barra o en el detalle de un ejercicio. Con la app fuera no
// corre nada (entre series no hay nada en segundo plano): avisa la alarma.
//
// Al volver a cualquier pantalla revisa también el permiso de alarmas exactas, por si
// se acaba de dar en Ajustes con un descanso en marcha.
//
// «Delante» es que haya alguna actividad empezada: con la pantalla apagada no hay
// ninguna y avisa la alarma con su notificación.
// ============================================================
public final class VigilanteDescanso implements Application.ActivityLifecycleCallbacks {

    @Nullable private static VigilanteDescanso instancia;

    private final Application app;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int empezadas;

    private final Runnable tic = new Runnable() {
        @Override public void run() {
            mirar();
            if (empezadas > 0) handler.postDelayed(this, 1000);
        }
    };

    private VigilanteDescanso(@NonNull Application app) {
        this.app = app;
    }

    /** Lo instala en la aplicación (una vez, al arrancar el proceso). */
    public static void instalar(@NonNull Application app) {
        if (instancia != null) return;
        instancia = new VigilanteDescanso(app);
        app.registerActivityLifecycleCallbacks(instancia);
    }

    /** Si hay alguna pantalla de la app a la vista. */
    public static boolean delante() {
        return instancia != null && instancia.empezadas > 0;
    }

    private void mirar() {
        SesionEnCursoRepositorio repo = SesionEnCursoRepositorio.get(app);
        if (repo.comprobarDescanso() == LogicaDescanso.Estado.TERMINA) {
            AvisoDescanso.avisarDelante(app);
        }
    }

    @Override public void onActivityStarted(@NonNull Activity a) {
        if (empezadas++ == 0) {
            handler.removeCallbacks(tic);
            handler.post(tic);
        }
    }

    @Override public void onActivityStopped(@NonNull Activity a) {
        if (empezadas > 0 && --empezadas == 0) handler.removeCallbacks(tic);
    }

    @Override public void onActivityCreated(@NonNull Activity a, @Nullable Bundle b) { }
    // Al volver de Ajustes del sistema el permiso de alarmas exactas puede haber
    // llegado: la alarma de un descanso en marcha se vuelve a poner exacta.
    @Override public void onActivityResumed(@NonNull Activity a) {
        SesionEnCursoRepositorio.get(app).revisarPermisoAlarma();
    }
    @Override public void onActivityPaused(@NonNull Activity a) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) { }
    @Override public void onActivityDestroyed(@NonNull Activity a) { }
}
