package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.text.Layout;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.button.MaterialButton;

import org.junit.Test;
import org.junit.runner.RunWith;

import es.pmdm.gymprofit.R;

// ============================================================
// BotonesHoySinCorteTest — GP-285, lote 1c: los botones de «Hoy toca» no se cortan (DEC-019).
//
// Con letra 2,0 en un móvil de 360 dp, «Empezar» al lado de «Cambiar» se cortaba a «Empeza»
// (revisor de UI). Se mide lo que Android ha colocado: la única línea de cada botón lleva su
// texto entero y cabe en él. A letra 1,0 y 2,0.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class BotonesHoySinCorteTest {

    @Test
    public void empezar_y_cambiar_no_se_cortan_a_letra_1_y_2() {
        for (float letra : new float[]{ 1f, 2f }) {
            final float escala = letra;
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
                Configuration conf = new Configuration(base.getResources().getConfiguration());
                conf.fontScale = escala;
                Context c = new ContextThemeWrapper(base.createConfigurationContext(conf), R.style.Theme_GymProFit);
                float dp = c.getResources().getDisplayMetrics().density;
                View inicio = LayoutInflater.from(c).inflate(R.layout.fragment_inicio, new FrameLayout(c), false);
                ViewGroup fila = inicio.findViewById(R.id.filaBotonesHoy);
                MaterialButton empezar = fila.findViewById(R.id.btnHoyPrincipal);
                MaterialButton cambiar = fila.findViewById(R.id.btnHoySecundario);
                empezar.setText(R.string.btn_empezar);
                cambiar.setText(R.string.btn_cambiar);
                cambiar.setVisibility(View.VISIBLE);
                // La fila, al ancho que le deja la tarjeta en un móvil de 360 dp.
                int ancho = Math.round((360 - 2 * 16) * dp);
                fila.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                fila.layout(0, 0, ancho, fila.getMeasuredHeight());
                for (MaterialButton b : new MaterialButton[]{ empezar, cambiar }) {
                    Layout l = b.getLayout();
                    // Con una sola línea, lo que no cabe pasa a una segunda que no se ve: la
                    // primera tiene que llevar el texto entero, y caber en el ancho del texto.
                    float texto = l.getLineWidth(0);
                    assertTrue("a letra " + escala + ", «" + b.getText() + "» se corta: "
                                    + l.getLineEnd(0) + " de " + b.getText().length() + " letras, "
                                    + texto / dp + " dp en " + l.getWidth() / dp + " dp",
                            l.getLineEnd(0) == b.getText().length() && l.getEllipsisCount(0) == 0
                                    && texto <= l.getWidth() + 1);
                }
            });
        }
    }
}
