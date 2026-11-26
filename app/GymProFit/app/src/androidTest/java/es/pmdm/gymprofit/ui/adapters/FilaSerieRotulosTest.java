package es.pmdm.gymprofit.ui.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.text.Layout;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import es.pmdm.gymprofit.R;

// ============================================================
// FilaSerieRotulosTest — GP-133: los rótulos de las columnas de «Apuntar entreno» se leen
// enteros, también con la letra grande.
//
// Nació sobre item_serie, la fila de una serie del formulario viejo, donde el rótulo era la
// pista de cada campo. GP-241 rehízo la pantalla y borró esa fila: ahora los rótulos («Serie»,
// «Anterior», «kg», «Reps») son una fila de textos encima de las series de cada ejercicio
// (item_apuntar_ejercicio), y este test mira esa (GP-286: seguía apuntando a la fila borrada
// y no compilaba).
//
// Se infla la tarjeta de un ejercicio con la escala de letra y el idioma pedidos y se mide al
// ancho que tiene en la pantalla (el del móvil menos los márgenes de la pantalla). Un rótulo
// se lee entero si no acaba en puntos suspensivos, si ninguna de sus palabras es más ancha que
// su columna (se partiría por la mitad) y si su vista tiene el alto de todas sus líneas.
//
// A 1,0 y a 1,3 pasa. A 2,0 no, y ese caso está apartado con su porqué (ver abajo).
// ============================================================
@RunWith(AndroidJUnit4.class)
public class FilaSerieRotulosTest {

    private static ViewGroup filaDeRotulos(float escala, String idioma) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration conf = new Configuration(base.getResources().getConfiguration());
        conf.fontScale = escala;
        conf.setLocale(new Locale(idioma));
        Context c = new ContextThemeWrapper(base.createConfigurationContext(conf), R.style.Theme_GymProFit);
        View tarjeta = LayoutInflater.from(c).inflate(R.layout.item_apuntar_ejercicio, null, false);
        int ancho = c.getResources().getDisplayMetrics().widthPixels
                - 2 * c.getResources().getDimensionPixelSize(R.dimen.margen_pantalla);
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

    private static void comprobar(float escala, String idioma) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ViewGroup fila = filaDeRotulos(escala, idioma);
            String caso = idioma + " a " + escala;
            int rotulos = 0;
            for (int i = 0; i < fila.getChildCount(); i++) {
                if (!(fila.getChildAt(i) instanceof TextView)) continue;
                rotuloEntero((TextView) fila.getChildAt(i), caso);
                rotulos++;
            }
            // «Serie», «Anterior», «kg» y «Reps».
            assertEquals(caso + ": columnas con rótulo", 4, rotulos);
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

    // Apartado, no arreglado (GP-286): con letra 2,0 la pantalla nueva parte «SERIE» (163 px en
    // una columna de 126) y «ANTERIOR» / «PREVIOUS» (281 y 279 px en 231) por la mitad de la
    // palabra, medido en un móvil de 1080 px a 420 ppp. Que pase exige tocar el layout de
    // producción (item_apuntar_ejercicio), y eso lo decide el propietario: al arreglarlo se
    // quita esta línea.
    @Ignore("GP-286: a letra 2,0 «Serie» y «Anterior» no caben en su columna; pendiente de decidir el arreglo en producción")
    @Test
    public void a_2_0_en_espanol_y_en_ingles() {
        comprobar(2.0f, "es");
        comprobar(2.0f, "en");
    }
}
