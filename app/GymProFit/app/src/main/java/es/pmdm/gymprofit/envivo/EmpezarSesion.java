package es.pmdm.gymprofit.envivo;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.ui.activities.SesionEnVivoActivity;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// EmpezarSesion — lo que hace cualquier «Empezar» de la app (GP-012): Inicio, Entrenar,
// el detalle de una rutina, el «+» y «Empezar sin rutina». Todos abren la sesión en vivo
// con el reloj en marcha desde el toque; sin rutina, vacía.
//
// Una a la vez. Con otra en curso no se pisa: se pregunta —«Ya tienes una sesión en
// curso. Pierna B lleva 23 min.»— con volver a ella, descartarla y empezar la nueva, o
// cancelar. Empezar la misma rutina que está en curso es volver a ella.
// ============================================================
public final class EmpezarSesion {

    private EmpezarSesion() { }

    /** Empieza (o vuelve a) la sesión de esa rutina; null = sin rutina. */
    public static void empezar(@NonNull Activity act, @Nullable Rutina r) {
        if (r == null) empezar(act, null, null, null);
        else empezar(act, r.getId(), r.getNombre(), r.getProgramaNombre());
    }

    /** Igual, con los datos sueltos (el detalle de la rutina no tiene el objeto). */
    public static void empezar(@NonNull Activity act, @Nullable Integer rutinaId, @Nullable String nombre,
                               @Nullable String programa) {
        SesionEnCursoRepositorio repo = SesionEnCursoRepositorio.get(act);
        repo.usarCuenta(new PreferencesManager(act).getUsuarioId());
        switch (repo.empezar(rutinaId, nombre, programa)) {
            case EMPEZADA:
            case ES_LA_MISMA:
                abrir(act);
                break;
            case YA_HAY_OTRA:
                preguntar(act, repo, rutinaId, nombre, programa);
                break;
        }
    }

    /** Abre la sesión en curso. */
    public static void abrir(@NonNull Activity act) {
        act.startActivity(new Intent(act, SesionEnVivoActivity.class));
    }

    /** Nombre de la sesión para la vista: su rutina, o «Entrenamiento libre». */
    @NonNull
    public static String nombre(@NonNull android.content.Context ctx, @Nullable String rutinaNombre) {
        return rutinaNombre != null && !rutinaNombre.isEmpty()
                ? rutinaNombre : ctx.getString(R.string.sesiones_entrenamiento_libre);
    }

    private static void preguntar(Activity act, SesionEnCursoRepositorio repo, @Nullable Integer rutinaId,
                                  @Nullable String nombre, @Nullable String programa) {
        SesionEnCurso enCurso = repo.actual();
        if (enCurso == null) return;
        String actual = nombre(act, enCurso.rutinaNombre);
        String nueva = nombre(act, nombre);
        int minutos = LogicaSesion.minutosReloj(enCurso.inicioMs, repo.ahora());
        android.view.View v = android.view.LayoutInflater.from(act).inflate(R.layout.dialog_ya_hay_sesion, null, false);
        ((android.widget.TextView) v.findViewById(R.id.tvMensajeYaHay))
                .setText(act.getString(R.string.envivo_ya_hay, actual, minutos));
        androidx.appcompat.app.AlertDialog dialogo = new MaterialAlertDialogBuilder(act)
                .setTitle(R.string.envivo_ya_hay_titulo)
                .setView(v)
                .create();
        android.widget.Button volver = v.findViewById(R.id.btnVolverA);
        volver.setText(act.getString(R.string.envivo_volver_a, actual));
        volver.setOnClickListener(b -> {
            dialogo.dismiss();
            abrir(act);
        });
        android.widget.Button descartar = v.findViewById(R.id.btnDescartarYEmpezar);
        descartar.setText(act.getString(R.string.envivo_descartar_y_empezar, nueva));
        descartar.setOnClickListener(b -> {
            dialogo.dismiss();
            repo.descartar();
            if (repo.empezar(rutinaId, nombre, programa) != SesionEnCursoRepositorio.Empezar.YA_HAY_OTRA) {
                abrir(act);
            }
        });
        v.findViewById(R.id.btnCancelarYaHay).setOnClickListener(b -> dialogo.dismiss());
        dialogo.show();
    }
}
