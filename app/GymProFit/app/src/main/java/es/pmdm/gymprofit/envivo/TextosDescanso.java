package es.pmdm.gymprofit.envivo;

import android.content.Context;

import androidx.annotation.NonNull;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.utils.Marcas;

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

    /**
     * Lo que viene tras el descanso, para el panel y el Live Update (GP-210): «Siguiente:
     * serie 4 · 85 kg × 7» si es otra serie del mismo ejercicio y se sabe qué se hizo la
     * última vez, «Siguiente: serie 4» si no, y «Siguiente: Sentadilla, serie 1» si toca
     * cambiar de ejercicio.
     */
    @NonNull
    public static String siguiente(@NonNull Context ctx, @NonNull SesionEnCurso s) {
        LogicaDescanso.Siguiente sig = LogicaDescanso.siguiente(s);
        if (sig == null) return ctx.getString(R.string.envivo_descanso_sin_despues);
        SesionEnCurso.Ejercicio actual = s.descanso != null
                ? SesionEnCursoRepositorio.ejercicioDeSerie(s, s.descanso.serieId) : null;
        if (actual != null && actual != sig.ejercicio) {
            return ctx.getString(R.string.envivo_descanso_siguiente_otro,
                    ejercicio(ctx, sig.ejercicio.nombre, sig.ejercicio.ejercicioId), sig.numero);
        }
        String marca = marcaPrevista(ctx, sig.ejercicio, sig.numero);
        return marca == null ? ctx.getString(R.string.envivo_descanso_siguiente_solo, sig.numero)
                : ctx.getString(R.string.envivo_descanso_siguiente_serie, sig.numero, marca);
    }

    // «85 kg × 7», «12 reps» o «0:45»: lo que se hizo en esa serie la última vez; null si no se sabe.
    @androidx.annotation.Nullable
    private static String marcaPrevista(Context ctx, SesionEnCurso.Ejercicio e, int numero) {
        SesionEnCurso.SerieAnterior a = LogicaSesion.anterior(e, numero);
        if (a == null) return null;
        if (LogicaSesion.porTiempo(e)) {
            return a.segundos != null && a.segundos > 0 ? Marcas.tiempo(a.segundos) : null;
        }
        if (a.repeticiones <= 0) return null;
        double kilos = 0;
        try {
            kilos = a.peso != null ? new java.math.BigDecimal(a.peso).doubleValue() : 0;
        } catch (NumberFormatException ignorada) {
            // Un peso que no se lee se trata como peso corporal: se enseñan solo las repeticiones.
        }
        return kilos > 0
                ? ctx.getString(R.string.record_peso_por_reps, Marcas.kilos(ctx, kilos), a.repeticiones)
                : ctx.getResources().getQuantityString(R.plurals.record_repeticiones, a.repeticiones, a.repeticiones);
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
