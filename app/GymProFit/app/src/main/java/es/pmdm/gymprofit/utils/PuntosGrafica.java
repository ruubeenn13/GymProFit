package es.pmdm.gymprofit.utils;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

// ============================================================
// PuntosGrafica — qué enseña una gráfica por fecha según cuántos días tiene (GP-229).
//
// Las tres gráficas por fecha (el peso de «Tu cuerpo», Medidas y la progresión de la ficha del
// ejercicio) se escondían con menos de dos puntos: con un pesaje, Medidas decía «Aún no hay datos»
// y la ficha no enseñaba nada. Ahora:
//   0 puntos → el vacío de cada pantalla;
//   1 punto  → el punto, con su valor encima y su fecha en el eje, y la pista de apuntar otro;
//   2 o más  → la línea.
// Los puntos son días (EjeFechas.porDia): dos pesajes del mismo día son uno.
// ============================================================
public final class PuntosGrafica {

    /** Lo que se pinta. */
    public enum Que { VACIO, UN_PUNTO, LINEA }

    private PuntosGrafica() { }

    /**
     * @param puntos cuántos puntos tiene la gráfica, uno por día.
     * @return el vacío, el punto solo o la línea.
     */
    public static Que de(int puntos) {
        if (puntos <= 0) return Que.VACIO;
        return puntos == 1 ? Que.UN_PUNTO : Que.LINEA;
    }

    /**
     * La cuadrícula del eje Y, solo con línea: con un punto solo y letra 1,3 una de sus rayas
     * cruzaba la cifra escrita encima del punto (revisor, GP-225), y para un punto no dice nada.
     */
    public static void cuadricula(@NonNull com.github.mikephil.charting.charts.LineChart c, @NonNull Que que) {
        c.getAxisLeft().setDrawGridLines(que == Que.LINEA);
    }

    /**
     * Prepara la serie de un solo punto: su valor escrito encima, en el color del texto, y el
     * punto algo más grande, que solo no se ve como parte de una línea. La fecha la pone el eje
     * ({@link EjeFechas#llevaEtiqueta}, que con un punto solo etiqueta el suyo).
     *
     * @param ds    la serie, ya con el estilo de la app.
     * @param valor el valor como se escribe («79,2 kg»).
     */
    public static void unPunto(@NonNull LineDataSet ds, @NonNull Context ctx, @NonNull String valor) {
        ds.setDrawValues(true);
        ds.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPointLabel(Entry e) {
                return valor;
            }
        });
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tv, true);
        ds.setValueTextColor(tv.data);
        ds.setValueTextSize(ChartStyler.textoSp(ctx));
        // La condensada de las cifras, como el eje (revisor, GP-225: salía en la del sistema).
        ds.setValueTypeface(ChartStyler.fuente(ctx));
        ds.setCircleRadius(6f);
        ds.setDrawCircleHole(false);
        ds.setDrawFilled(false);
        // Valor y fecha ya están escritos: el tooltip al tocarlo los tapaba (revisor, GP-225).
        ds.setHighlightEnabled(false);
    }
}
