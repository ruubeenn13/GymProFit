package es.pmdm.gymprofit.envivo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;

// ============================================================
// PlanNotificacionSesion — qué lleva la notificación de la sesión en este momento
// (GP-210 paso 3, momento 27), decidido sin tocar Android para poder probarlo.
//
//   · promovida: se pide a Android 16 que la trate como Live Update (chip en la barra de
//     estado y tarjeta en la pantalla de bloqueo). Solo mientras la sesión se puede
//     seguir: una que no se pudo guardar sigue siendo la notificación de siempre.
//   · masQuince y saltar: los dos botones, solo con un descanso en marcha y la sesión
//     editable. «+15 s» deja de ofrecerse en el tope de 10 minutos.
//   · ejercicioDelDescanso y series: el ejercicio al que toca volver y cuántas series
//     lleva hechas, para la barra por tramos del Live Update.
// ============================================================
public final class PlanNotificacionSesion {

    public final boolean promovida;
    public final boolean masQuince;
    public final boolean saltar;
    @Nullable public final SesionEnCurso.Ejercicio ejercicioDelDescanso;
    public final int seriesHechas;
    public final int seriesTotales;

    private PlanNotificacionSesion(boolean promovida, boolean masQuince, boolean saltar,
                                   @Nullable SesionEnCurso.Ejercicio ejercicio, int hechas, int totales) {
        this.promovida = promovida;
        this.masQuince = masQuince;
        this.saltar = saltar;
        this.ejercicioDelDescanso = ejercicio;
        this.seriesHechas = hechas;
        this.seriesTotales = totales;
    }

    @NonNull
    public static PlanNotificacionSesion de(@NonNull SesionEnCurso s, long ahoraMs) {
        boolean editable = s.claveIdempotencia == null;
        SesionEnCurso.Descanso d = s.descanso;
        boolean enDescanso = editable && d != null;
        SesionEnCurso.Ejercicio e = null;
        int hechas = 0, totales = 0;
        if (enDescanso) {
            LogicaDescanso.Siguiente sig = LogicaDescanso.siguiente(s);
            e = sig != null ? sig.ejercicio : SesionEnCursoRepositorio.ejercicioDeSerie(s, d.serieId);
            if (e != null) {
                totales = e.series.size();
                for (SesionEnCurso.Serie serie : e.series) if (serie.hecha) hechas++;
            }
        }
        return new PlanNotificacionSesion(editable, enDescanso && LogicaDescanso.puedeSumar(d, ahoraMs),
                enDescanso, e, hechas, totales);
    }
}
