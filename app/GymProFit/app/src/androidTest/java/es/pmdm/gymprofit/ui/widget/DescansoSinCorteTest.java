package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.theme.MaterialComponentsViewInflater;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import es.pmdm.gymprofit.R;

// ============================================================
// DescansoSinCorteTest — GP-285, lote 2: el panel del descanso, medido de verdad en esta versión
// de Android (se lanza en los tres emuladores), en español y en inglés, a letra 1,0, 1,3, 1,5 y
// 2,0, en un móvil de 360 dp de ancho.
//
// · Un botón nunca se corta (DEC-070): «−15 s», «+15 s» y «Saltar» llevan su texto entero en
//   su única línea.
// · Zona táctil de 48 dp o más en los tres (DEC-019).
// · Un solo principal: «Saltar» es el único botón de marca; los otros dos, de cristal.
// · A letra 1,0 los botones van al lado de la cuenta atrás, como en la maqueta; cuando no
//   caben (letra grande), bajan a su propia fila (FilaBotones) y se reparten el ancho.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class DescansoSinCorteTest {

    private static final int ANCHO_DP = 360;

    private static View panel(float escala, String idioma) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration conf = new Configuration(base.getResources().getConfiguration());
        conf.fontScale = escala;
        conf.setLocale(new Locale(idioma));
        Context tema = new ContextThemeWrapper(base.createConfigurationContext(conf), R.style.Theme_GymProFit);
        Context c = new ContextThemeWrapper(tema, R.style.ThemeOverlay_GymProFit_Cristal);
        // Como lo infla una pantalla de verdad: con las vistas de AppCompat y Material (en
        // Android 7, sin eso, la fuente del estilo no se aplica y el texto sale más ancho).
        LayoutInflater inflador = LayoutInflater.from(c).cloneInContext(c);
        MaterialComponentsViewInflater vistas = new MaterialComponentsViewInflater();
        inflador.setFactory2(new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(View padre, String nombre, Context contexto, AttributeSet atributos) {
                return vistas.createView(padre, nombre, contexto, atributos, false, false, true, false);
            }

            @Override
            public View onCreateView(String nombre, Context contexto, AttributeSet atributos) {
                return onCreateView(null, nombre, contexto, atributos);
            }
        });
        View zona = inflador.inflate(R.layout.view_descanso, new FrameLayout(c), false);
        zona.findViewById(R.id.panelDescanso).setVisibility(View.VISIBLE);
        ((TextView) zona.findViewById(R.id.tvDescansoTiempo)).setText("12:34");
        ((TextView) zona.findViewById(R.id.tvDescansoDespues)).setText(
                c.getString(R.string.envivo_descanso_siguiente_solo, 3));
        int ancho = Math.round(ANCHO_DP * c.getResources().getDisplayMetrics().density);
        zona.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        zona.layout(0, 0, ancho, zona.getMeasuredHeight());
        return zona;
    }

    private static void comprobar(float escala, String idioma) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View zona = panel(escala, idioma);
            String caso = idioma + " a " + escala;
            float dp = zona.getResources().getDisplayMetrics().density;
            int principales = 0;
            for (int id : new int[]{ R.id.btnDescansoMenos, R.id.btnDescansoMas, R.id.btnDescansoSaltar }) {
                MaterialButton b = zona.findViewById(id);
                String nombre = caso + ", «" + b.getText() + "»";
                Layout l = b.getLayout();
                // Con una sola línea, lo que no cabe pasa a una segunda que no se ve: la primera
                // tiene que llevar el texto entero, y caber en el ancho que el botón le deja.
                assertEquals(nombre + ": su línea no lleva el texto entero", b.getText().length(), l.getLineEnd(0));
                assertEquals(nombre + ": acaba en puntos suspensivos", 0, l.getEllipsisCount(0));
                int hueco = b.getWidth() - b.getPaddingLeft() - b.getPaddingRight();
                assertTrue(nombre + ": mide " + l.getLineWidth(0) + " px y tiene " + hueco,
                        l.getLineWidth(0) <= hueco + 0.5f);
                assertTrue(nombre + ": " + (b.getHeight() / dp) + " dp de alto", b.getHeight() >= Math.round(48 * dp) - 1);
                assertTrue(nombre + ": " + (b.getWidth() / dp) + " dp de ancho", b.getWidth() >= Math.round(48 * dp) - 1);
                // TalkBack lo nombra por lo que hace («Quitar 15 segundos»), no por su «−15 s».
                assertTrue(nombre + ": sin nombre para TalkBack",
                        b.getContentDescription() != null && b.getContentDescription().length() > b.getText().length());
                if (b instanceof BotonMarca) principales++;
                else assertTrue(nombre + " es de cristal", b instanceof BotonCristal);
            }
            // La cuenta atrás es una sola parada de TalkBack (su descripción la calcula la
            // pantalla al llegar a ella); lo de dentro y «Siguiente: …» no se leen aparte.
            View cuenta = zona.findViewById(R.id.grupoCuentaAtras);
            assertTrue(caso + ": la cuenta atrás no es una parada de TalkBack", cuenta.isFocusable()
                    && cuenta.getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            for (int id : new int[]{ R.id.tvDescansoRotulo, R.id.tvDescansoTiempo, R.id.tvDescansoDespues }) {
                assertEquals(caso + ": " + zona.getResources().getResourceEntryName(id) + " se leería dos veces",
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO, zona.findViewById(id).getImportantForAccessibility());
            }
            assertEquals(caso + ": principales en el descanso", 1, principales);
            assertTrue(caso + ": el principal es «Saltar»",
                    zona.findViewById(R.id.btnDescansoSaltar) instanceof BotonMarca);
            // El panel entero cabe en el ancho: nada se sale por la derecha.
            ViewGroup botones = zona.findViewById(R.id.botonesDescanso);
            View panel = zona.findViewById(R.id.panelDescanso);
            int[] p = new int[2], q = new int[2];
            botones.getLocationInWindow(p);
            panel.getLocationInWindow(q);
            assertTrue(caso + ": los botones se salen del panel",
                    p[0] + botones.getWidth() <= q[0] + panel.getWidth());
            // A letra 1,0, al lado de la cuenta, como en la maqueta.
            FilaBotones fila = zona.findViewById(R.id.filaDescanso);
            if (escala == 1.0f) assertFalse(caso + ": a letra 1,0 los botones van al lado de la cuenta", fila.apilada());
        });
    }

    @Test
    public void a_1_0_en_espanol_y_en_ingles() {
        comprobar(1.0f, "es");
        comprobar(1.0f, "en");
    }

    @Test
    public void a_1_3_en_espanol_y_en_ingles() {
        comprobar(1.3f, "es");
        comprobar(1.3f, "en");
    }

    @Test
    public void a_1_5_en_espanol_y_en_ingles() {
        comprobar(1.5f, "es");
        comprobar(1.5f, "en");
    }

    @Test
    public void a_2_0_en_espanol_y_en_ingles() {
        comprobar(2.0f, "es");
        comprobar(2.0f, "en");
    }
}
