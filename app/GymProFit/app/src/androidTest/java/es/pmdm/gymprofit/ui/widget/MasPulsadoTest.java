package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.cristal.CristalVivo;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// MasPulsadoTest — GP-285, lote 1c (DEC-070): el «+» de cristal pulsado sigue siendo un círculo.
//
// En Liquid Glass un botón pulsado no cambia de forma: se hunde (escala 0,94) y le sube la luz.
// El cambio de forma de los botones expresivos (DEC-039, BotonExpresivo) no le toca. Se toca
// de verdad, con el toque que manda Android, y se espera a que acabe el movimiento (TOQUE).
// Con «Quitar animaciones» el «+» no se mueve: tampoco se hunde.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class MasPulsadoTest {

    // Lo que dura el movimiento del toque, con margen.
    private static final long ESPERA = Movimiento.TOQUE * 3;

    @Test
    public void el_mas_pulsado_sigue_siendo_un_circulo_y_se_hunde() {
        Context c = new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_GymProFit);
        CristalVivo[] mas = new CristalVivo[1];
        float[] reposo = new float[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            BarraNavegacion barra = new BarraNavegacion(c);
            int ancho = Math.round(360 * c.getResources().getDisplayMetrics().density);
            barra.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            barra.layout(0, 0, ancho, barra.getMeasuredHeight());
            mas[0] = barra.findViewById(R.id.btnAcciones);
            reposo[0] = mas[0].radioEsquinas();
            assertEquals("en reposo, un círculo", mas[0].getHeight() / 2f, reposo[0], 0.5f);
            tocar(mas[0], MotionEvent.ACTION_DOWN);
        });
        SystemClock.sleep(ESPERA);
        boolean quieto = Movimiento.quieto(c);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            assertEquals("pulsado, sigue siendo un círculo", reposo[0], mas[0].radioEsquinas(), 0.5f);
            float hundido = quieto ? 1f : 0.94f;
            assertEquals("pulsado, se hunde", hundido, mas[0].getScaleX(), 0.005f);
            assertEquals("pulsado, se hunde", hundido, mas[0].getScaleY(), 0.005f);
            tocar(mas[0], MotionEvent.ACTION_UP);
        });
        SystemClock.sleep(ESPERA);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            assertEquals("al soltar, un círculo", reposo[0], mas[0].radioEsquinas(), 0.5f);
            assertEquals("al soltar, vuelve", 1f, mas[0].getScaleX(), 0.005f);
        });
    }

    private static void tocar(View v, int accion) {
        long ahora = SystemClock.uptimeMillis();
        MotionEvent e = MotionEvent.obtain(ahora, ahora, accion, v.getWidth() / 2f, v.getHeight() / 2f, 0);
        v.dispatchTouchEvent(e);
        e.recycle();
    }
}
