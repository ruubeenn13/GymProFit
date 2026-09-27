package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Rect;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import es.pmdm.gymprofit.R;

// ============================================================
// BarraNavegacionRecorteTest — GP-118: el «+» de la barra se ve entero.
//
// El botón sube 2 dp (marginTop -2dp, como en el diseño) por encima de su columna.
// Si algún contenedor de la barra recorta a sus hijos, esos 2 dp no se dibujan. Lo
// que se afirma es lo que se ve: el rectángulo visible del botón, con los recortes
// de todos sus padres, mide lo mismo que el botón. Se mide en un dispositivo porque
// ese cálculo es el del propio Android (getChildVisibleRect), el mismo que decide qué
// se dibuja.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class BarraNavegacionRecorteTest {

    @Test
    public void el_boton_de_acciones_se_ve_entero() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context c = new ContextThemeWrapper(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_GymProFit);
            BarraNavegacion barra = new BarraNavegacion(c);
            int ancho = 1080;
            barra.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            barra.layout(0, 0, ancho, barra.getMeasuredHeight());

            View boton = barra.findViewById(R.id.btnAcciones);
            Rect visible = new Rect();
            assertTrue("el botón no se ve", boton.getGlobalVisibleRect(visible));
            assertEquals("alto visible del «+» frente a su alto", boton.getHeight(), visible.height());
            assertEquals("ancho visible del «+» frente a su ancho", boton.getWidth(), visible.width());
        });
    }

    @Test
    public void el_boton_sigue_2dp_por_encima_de_su_columna() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context c = new ContextThemeWrapper(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_GymProFit);
            BarraNavegacion barra = new BarraNavegacion(c);
            barra.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            barra.layout(0, 0, 1080, barra.getMeasuredHeight());

            // Arreglar el recorte no puede mover el botón: sigue a -2 dp de su columna.
            View boton = barra.findViewById(R.id.btnAcciones);
            float dp = c.getResources().getDisplayMetrics().density;
            assertEquals(Math.round(-2 * dp), boton.getTop());
        });
    }
}
