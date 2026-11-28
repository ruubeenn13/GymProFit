package es.pmdm.gymprofit.ui.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.theme.MaterialComponentsViewInflater;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import es.pmdm.gymprofit.R;

// ============================================================
// FilaSerieRotulosTest — GP-133 y GP-286: los rótulos de las columnas de una serie («Serie»,
// «Anterior», «kg», «Reps») se leen enteros, también con la letra grande, en «Apuntar entreno»
// (item_apuntar_ejercicio) y en la Sesión en vivo (item_envivo_ejercicio).
//
// Nació sobre item_serie, la fila de una serie del formulario viejo, donde el rótulo era la
// pista de cada campo. GP-241 rehízo la pantalla y borró esa fila: ahora los rótulos son una
// fila de textos encima de las series de cada ejercicio.
//
// Con letra 2,0 «SERIE» y «ANTERIOR» / «PREVIOUS» no cabían en su columna y Android los partía
// por la mitad de la palabra (163 px en 126 y 281 px en 231, en un móvil de 1080 px a 420 ppp).
// Desde GP-286 son TextoSinPartir, la regla de GP-288: si la palabra no cabe, el rótulo encoge lo
// justo, nunca por debajo de su tamaño a letra 1,0, y no se parte. Y los cuatro van al mismo
// tamaño (FilaRotulos): «Serie», en la columna más estrecha, encogía más que los demás.
//
// Se infla la tarjeta de un ejercicio con la escala de letra y el idioma pedidos y se mide al
// ancho que tiene en la pantalla (el del móvil menos los márgenes de la pantalla). Un rótulo
// se lee entero si no acaba en puntos suspensivos, si ninguna de sus palabras es más ancha que
// su columna (se partiría por la mitad) y si su vista tiene el alto de todas sus líneas.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class FilaSerieRotulosTest {

    private static ViewGroup filaDeRotulos(int layout, int margen, float escala, String idioma) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration conf = new Configuration(base.getResources().getConfiguration());
        conf.fontScale = escala;
        conf.setLocale(new Locale(idioma));
        Context c = new ContextThemeWrapper(base.createConfigurationContext(conf), R.style.Theme_GymProFit);
        // La Sesión en vivo es una pantalla de Liquid Glass: sus vistas leen el tema de cristal.
        if (layout == R.layout.item_envivo_ejercicio) {
            c = new ContextThemeWrapper(c, R.style.ThemeOverlay_GymProFit_Cristal);
        }
        // Como lo infla una pantalla de verdad: con las vistas de AppCompat y Material. Sin
        // eso, en Android 7 un TextView a secas no aplica la fuente del estilo (las fuentes de
        // res/font son de Android 8) y el rótulo sale en la del sistema, más ancha.
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
        View tarjeta = inflador.inflate(layout, null, false);
        int ancho = c.getResources().getDisplayMetrics().widthPixels
                - 2 * c.getResources().getDimensionPixelSize(margen);
        tarjeta.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        tarjeta.layout(0, 0, ancho, tarjeta.getMeasuredHeight());
        return (ViewGroup) tarjeta.findViewById(R.id.tvRotuloKg).getParent();
    }

    private static void rotuloEntero(TextView rotulo, String caso) {
        Layout lineas = rotulo.getLayout();
        assertNotNull(caso + ": sin medir", lineas);
        // Lo que se pinta (el estilo puede pasarlo a mayúsculas), no lo que se le dio.
        String texto = lineas.getText().toString();
        int hueco = rotulo.getWidth() - rotulo.getPaddingLeft() - rotulo.getPaddingRight();
        for (int i = 0; i < lineas.getLineCount(); i++) {
            assertEquals(caso + ": «" + texto + "» acaba en puntos suspensivos", 0, lineas.getEllipsisCount(i));
        }
        for (String palabra : texto.trim().split("\\s+")) {
            float mide = rotulo.getPaint().measureText(palabra);
            assertTrue(caso + ": «" + palabra + "» mide " + mide + " px y su columna es de " + hueco,
                    mide <= hueco);
        }
        int alto = rotulo.getHeight() - rotulo.getPaddingTop() - rotulo.getPaddingBottom();
        assertTrue(caso + ": «" + texto + "» necesita " + lineas.getHeight() + " px de alto y tiene " + alto,
                lineas.getHeight() <= alto);
    }

    // Las dos pantallas que llevan la fila: {layout de la tarjeta, margen de la pantalla}.
    private static final int[][] PANTALLAS = {
            { R.layout.item_apuntar_ejercicio, R.dimen.margen_pantalla },
            { R.layout.item_envivo_ejercicio, R.dimen.envivo_margen },
    };

    private static void comprobar(float escala, String idioma) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (int[] pantalla : PANTALLAS) {
                ViewGroup fila = filaDeRotulos(pantalla[0], pantalla[1], escala, idioma);
                String caso = fila.getResources().getResourceEntryName(pantalla[0]) + ", " + idioma + " a " + escala;
                int rotulos = 0;
                float tamano = -1f;
                for (int i = 0; i < fila.getChildCount(); i++) {
                    if (!(fila.getChildAt(i) instanceof TextView)) continue;
                    TextView rotulo = (TextView) fila.getChildAt(i);
                    rotuloEntero(rotulo, caso);
                    // Los cuatro, al mismo tamaño: si uno encoge, encogen todos (FilaRotulos).
                    if (tamano < 0f) tamano = rotulo.getTextSize();
                    assertEquals(caso + ": «" + rotulo.getText() + "» va a otro tamaño que el primero",
                            tamano, rotulo.getTextSize(), 0.01f);
                    rotulos++;
                }
                // «Serie», «Anterior», «kg» y «Reps».
                assertEquals(caso + ": columnas con rótulo", 4, rotulos);
            }
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
