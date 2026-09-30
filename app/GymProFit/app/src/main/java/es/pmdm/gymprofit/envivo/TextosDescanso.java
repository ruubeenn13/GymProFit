package es.pmdm.gymprofit.envivo;

import android.content.Context;

import androidx.annotation.NonNull;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;

// ============================================================
// TextosDescanso — lo que se dice del descanso (GP-013), igual en la pantalla de la
// sesión, en la barra de «Sesión en curso» y en las notificaciones.
// ============================================================
public final class TextosDescanso {

    private TextosDescanso() { }

    /** Nombre del ejercicio, o «Ejercicio 12» si no lo tiene. */
    @NonNull
    public static String ejercicio(@NonNull Context ctx, @androidx.annotation.Nullable String nombre, int ejercicioId) {
        return nombre != null ? nombre : ctx.getString(R.string.ejercicio_sin_nombre, ejercicioId);
    }

    /** «Después: Hip thrust con barra, serie 3», o «Última serie» si no queda otra. */
    @NonNull
    public static String despues(@NonNull Context ctx, @NonNull SesionEnCurso s) {
        LogicaDescanso.Siguiente sig = LogicaDescanso.siguiente(s);
        if (sig == null) return ctx.getString(R.string.envivo_descanso_sin_despues);
        return ctx.getString(R.string.envivo_descanso_despues,
                ejercicio(ctx, sig.ejercicio.nombre, sig.ejercicio.ejercicioId), sig.numero);
    }

    /** «1 minuto y 42 segundos», «45 segundos», «2 minutos», con sus plurales. */
    @NonNull
    public static String duracionHablada(@NonNull Context ctx, int segundos) {
        int min = segundos / 60, seg = segundos % 60;
        String m = ctx.getResources().getQuantityString(R.plurals.envivo_minutos, min, min);
        String sg = ctx.getResources().getQuantityString(R.plurals.envivo_segundos, seg, seg);
        if (min == 0) return sg;
        if (seg == 0) return m;
        return ctx.getString(R.string.envivo_min_y_seg, m, sg);
    }

    /** «¡A por la serie 3!», o «Descanso terminado» si ya no queda ninguna. */
    @NonNull
    public static String tituloFin(@NonNull Context ctx, @NonNull SesionEnCursoRepositorio.FinDescanso fin) {
        return fin.numero > 0 ? ctx.getString(R.string.envivo_fin_titulo, fin.numero)
                : ctx.getString(R.string.envivo_fin_titulo_sin_serie);
    }
}
