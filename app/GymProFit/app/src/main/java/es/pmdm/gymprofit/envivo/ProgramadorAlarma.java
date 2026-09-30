package es.pmdm.gymprofit.envivo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;

// ============================================================
// ProgramadorAlarma — una alarma por descanso, a su hora de fin (GP-013), sin Android.
//
// Con la app fuera o la pantalla apagada no corre nada entre series: lo único que queda
// es una alarma del sistema a la hora de fin. Con el permiso de alarmas exactas, exacta
// y aunque el móvil esté en reposo; sin él, una ventana corta, que el sistema puede
// retrasar. Saltar el descanso, empezar otro o terminar la sesión la cambia o la quita.
//
// Se sincroniza con cada cambio de la sesión, pero solo toca el sistema cuando cambia
// algo: la hora de fin, que ya no haya descanso o que el permiso haya cambiado.
// ============================================================
public class ProgramadorAlarma {

    /** Lo que hace falta del AlarmManager. Interfaz para probarlo sin Android. */
    public interface Alarmas {
        /** Si se pueden poner alarmas exactas (permiso «Alarmas y recordatorios»). */
        boolean puedeExactas();

        /** Exacta, aunque el móvil esté en reposo. */
        void exacta(long cuandoMs);

        /** En una ventana desde {@code cuandoMs}: puede llegar tarde. */
        void ventana(long cuandoMs, long largoMs);

        /** Quita la que haya. */
        void cancelar();
    }

    /** Lo que puede tardar como mucho sin permiso de alarmas exactas, si el sistema quiere. */
    public static final long VENTANA_MS = 10_000;

    private final Alarmas alarmas;
    /** Hora de la alarma puesta, o null si no hay. */
    @Nullable private Long puesta;
    private boolean puestaExacta;

    public ProgramadorAlarma(@NonNull Alarmas alarmas) {
        this.alarmas = alarmas;
    }

    /** La deja como pide el descanso: puesta a su hora, o quitada si no hay. */
    public void sincronizar(@Nullable SesionEnCurso.Descanso d) {
        if (d == null) {
            if (puesta != null) alarmas.cancelar();
            puesta = null;
            return;
        }
        if (puesta != null && puesta == d.finMs && puestaExacta == alarmas.puedeExactas()) return;
        poner(d.finMs);
    }

    /**
     * El permiso de alarmas exactas ha cambiado (o puede que sí): si hay una puesta y ya
     * no es del tipo que toca, se vuelve a poner. Con el permiso recién dado, exacta.
     */
    public void revisarPermiso() {
        if (puesta != null && puestaExacta != alarmas.puedeExactas()) poner(puesta);
    }

    /** Si la alarma puesta es exacta (para decirlo en pantalla o en los tests). */
    public boolean exacta() { return puesta != null && puestaExacta; }

    /** La hora de la puesta, o null. */
    @Nullable public Long puesta() { return puesta; }

    private void poner(long cuando) {
        puestaExacta = alarmas.puedeExactas();
        if (puestaExacta) alarmas.exacta(cuando);
        else alarmas.ventana(cuando, VENTANA_MS);
        puesta = cuando;
    }
}
