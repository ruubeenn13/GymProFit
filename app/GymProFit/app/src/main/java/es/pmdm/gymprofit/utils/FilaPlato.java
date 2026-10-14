package es.pmdm.gymprofit.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.Racion;

// ============================================================
// FilaPlato — lo que dice la fila de un plato en «Mis platos» (tablero 9, lote 1.6.6)
//
// «1 ración (1/6) · 220 kcal · 10 g prot.»: una ración, qué parte del plato es y lo que
// vale, que es lo que se apunta con «1 ración». Con una sola ración, sin el paréntesis
// («1 ración · 474 kcal · 25 g prot.»): manda esta regla, no el tablero. Nunca gramos:
// los de un plato sin peso hecho no son de verdad. Sin vistas, para probarlo.
// ============================================================
public final class FilaPlato {

    private FilaPlato() {
    }

    /** En el idioma de la app. */
    @NonNull
    public static String de(@NonNull Context ctx, @NonNull Alimento plato) {
        return texto(Cantidades.Formatos.de(ctx), ctx.getString(R.string.fila_plato_fraccion),
                ctx.getString(R.string.fila_plato), FechaUtils.localeDeLaApp(ctx), plato);
    }

    /**
     * @param fraccion «%1$s (1/%2$d)».
     * @param fila     «%1$s · %2$s kcal · %3$s g prot.».
     */
    @NonNull
    public static String texto(@NonNull Cantidades.Formatos f, @NonNull String fraccion, @NonNull String fila,
                               @NonNull Locale locale, @NonNull Alimento plato) {
        List<Racion> raciones = plato.getRaciones();
        double gramos = raciones.isEmpty() ? CuentaPlato.GRAMOS_SIN_PESO : raciones.get(0).getGramos();
        String una = raciones.isEmpty() ? "1"
                : Cantidades.texto(f, locale, raciones.get(0).getNombre(), raciones.get(0).getUnidad(),
                raciones.get(0).getUnidadPlural(), 1.0, gramos, true);
        if (plato.getPlatoRaciones() > 1) una = String.format(locale, fraccion, una, plato.getPlatoRaciones());
        CuentaPlato.Cifras c = CuentaPlato.linea(plato, gramos);
        NumberFormat nf = NumberFormat.getIntegerInstance(locale);
        return String.format(locale, fila, una, nf.format(c.kcal), nf.format(Math.round(c.proteinas.doubleValue())));
    }
}
