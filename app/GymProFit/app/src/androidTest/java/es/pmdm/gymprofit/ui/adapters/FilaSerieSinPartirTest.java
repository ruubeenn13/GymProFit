package es.pmdm.gymprofit.ui.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.theme.MaterialComponentsViewInflater;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import es.pmdm.gymprofit.R;

// ============================================================
// FilaSerieSinPartirTest — GP-285, lote 2: en la fila de una serie de la Sesión en vivo, la
// columna «Anterior» no parte su texto con la letra grande (GP-288), medido de verdad en esta
// versión de Android (se lanza en los tres emuladores), en un móvil de 360 dp de ancho.
//
// En Android 7, con letra 2,0 (ahí la letra crece el doble de verdad; desde Android 14 lo
// grande crece menos), «¡Récord!» —lo que la fila dorada enseña donde iba la marca anterior—
// salía partido en «¡Réc / ord!», y «80 × 10» dejaba el «10» solo en otra línea. Ahora es un
// TextoSinPartir y la marca va unida con espacios duros: encoge lo justo y cabe en una línea.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class FilaSerieSinPartirTest {

    private static final int ANCHO_DP = 360;

    private static TextView anterior(float escala, String idioma, boolean record) {
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
        View fila = inflador.inflate(R.layout.item_envivo_serie, null, false);
        TextView tv = fila.findViewById(R.id.tvAnterior);
        // Lo que pone el adapter: «¡Récord!» en negrita, o la marca con espacios duros.
        if (record) {
            tv.setText(c.getString(R.string.envivo_record_fila));
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        } else {
            tv.setText(c.getString(R.string.envivo_peso_por_reps, "102,5", 12).replace(' ', '\u00A0'));
        }
        float dp = c.getResources().getDisplayMetrics().density;
        int ancho = Math.round(ANCHO_DP * dp) - 2 * c.getResources().getDimensionPixelSize(R.dimen.envivo_margen);
        fila.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        fila.layout(0, 0, ancho, fila.getMeasuredHeight());
        return tv;
    }

    private static void entero(float escala, String idioma, boolean record) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            TextView tv = anterior(escala, idioma, record);
            String caso = idioma + " a " + escala + (record ? ", récord" : ", marca");
            Layout lineas = tv.getLayout();
            assertNotNull(caso + ": sin medir", lineas);
            String texto = lineas.getText().toString();
            // Una sola pieza: en una línea, entera y sin puntos suspensivos.
            assertEquals(caso + ": «" + texto + "» se reparte en varias líneas", 1, lineas.getLineCount());
            assertEquals(caso + ": «" + texto + "» acaba en puntos suspensivos", 0, lineas.getEllipsisCount(0));
            int hueco = tv.getWidth() - tv.getPaddingLeft() - tv.getPaddingRight();
            assertTrue(caso + ": «" + texto + "» mide " + lineas.getLineWidth(0) + " px y su columna es de " + hueco,
                    lineas.getLineWidth(0) <= hueco + 0.5f);
        });
    }

    @Test
    public void record_y_marca_enteros_a_letra_1_0_y_1_3() {
        for (float escala : new float[]{ 1.0f, 1.3f }) {
            for (String idioma : new String[]{ "es", "en" }) {
                entero(escala, idioma, true);
                entero(escala, idioma, false);
            }
        }
    }

    @Test
    public void record_y_marca_enteros_a_letra_1_5_y_2_0() {
        for (float escala : new float[]{ 1.5f, 2.0f }) {
            for (String idioma : new String[]{ "es", "en" }) {
                entero(escala, idioma, true);
                entero(escala, idioma, false);
            }
        }
    }
}
