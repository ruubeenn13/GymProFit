package es.pmdm.gymprofit.ui.inicio;

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

import com.google.android.material.theme.MaterialComponentsViewInflater;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import es.pmdm.gymprofit.R;

// ============================================================
// TuDiaSinPartirTest — GP-288: con letra grande, «Tu día» de Inicio no parte palabras (DEC-019).
//
// A letra 2,0, en un móvil de 360 dp, los tres tiles partían «Calori / as» y «Protei / na» y
// cortaban la cifra. Se mide lo que Android ha colocado en el rótulo, la cifra y su apoyo de
// cada tile: cada línea acaba donde acaba una palabra (nunca en medio, ni con guion), no hay
// elipsis y se ve el texto entero. A letra 1,0, 1,3, 1,5 y 2,0, en español y en inglés, con las
// cifras más anchas que salen a diario. Y cada texto, al mismo tamaño en los tres tiles.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class TuDiaSinPartirTest {

    private static final int[] TILES = { R.id.cardDiaKcal, R.id.cardDiaProteina, R.id.cardDiaAgua };
    private static final int[] ROTULOS = { R.string.inicio_dia_calorias, R.string.inicio_dia_proteina,
            R.string.inicio_dia_agua };
    // El apoyo más largo de cada tile.
    private static final int[] APOYOS = { R.string.inicio_dia_quedan, R.string.inicio_dia_kcal_hoy_g,
            R.string.inicio_dia_agua_hoy };

    @Test
    public void tu_dia_no_parte_palabras_ni_corta_la_cifra() {
        for (String idioma : new String[]{ "es", "en" }) {
            for (float letra : new float[]{ 1f, 1.3f, 1.5f, 2f }) {
                final float escala = letra;
                final Locale locale = new Locale(idioma);
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> comprobar(locale, escala));
            }
        }
    }

    private void comprobar(Locale locale, float escala) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration conf = new Configuration(base.getResources().getConfiguration());
        conf.fontScale = escala;
        conf.setLocale(locale);
        Context c = new ContextThemeWrapper(base.createConfigurationContext(conf), R.style.Theme_GymProFit);
        float dp = c.getResources().getDisplayMetrics().density;
        // Con la fábrica de Material, como en la app: sin ella, en Android 7 los TextView a secas
        // no cogen las fuentes de res/font y el texto sale más ancho que en la pantalla de verdad.
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
        View inicio = inflador.inflate(R.layout.fragment_inicio, new FrameLayout(c), false);
        ViewGroup fila = inicio.findViewById(R.id.filaTuDia);
        // Como en la app (InicioTuDia): cada texto, al mismo tamaño en los tres tiles.
        InicioTuDia.igualarTextos(inicio);
        boolean ingles = locale.getLanguage().equals("en");
        // Las cifras más anchas de un día normal: miles con separador, tres cifras y un decimal.
        String[] cifras = {
                ingles ? "2,850" : "2.850",
                c.getString(R.string.inicio_cifra_g, 188),
                c.getString(R.string.inicio_cifra_l, ingles ? "2.5" : "2,5") };
        for (int i = 0; i < TILES.length; i++) {
            View tile = fila.findViewById(TILES[i]);
            ((TextView) tile.findViewById(R.id.tvRotuloDia)).setText(ROTULOS[i]);
            ((TextView) tile.findViewById(R.id.tvCifraDia)).setText(cifras[i]);
            ((TextView) tile.findViewById(R.id.tvApoyoDia)).setText(APOYOS[i]);
        }
        // La fila, al ancho que le deja el margen de la pantalla en un móvil de 360 dp.
        int ancho = Math.round((360 - 2 * 20) * dp);
        fila.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        fila.layout(0, 0, ancho, fila.getMeasuredHeight());
        for (int texto : new int[]{ R.id.tvRotuloDia, R.id.tvCifraDia, R.id.tvApoyoDia }) {
            float tamano = -1f;
            for (int id : TILES) {
                TextView t = fila.findViewById(id).findViewById(texto);
                sinPartir(t, locale, escala, dp);
                // Y los tres del mismo tipo, al mismo tamaño: «Agua» no más grande que «Calorías».
                // Con un dp de holgura: el reparto de pesos deja un tile un píxel más ancho.
                if (tamano < 0) tamano = t.getTextSize();
                assertTrue("[" + locale + ", letra " + escala + "] «" + t.getText() + "» a "
                        + t.getTextSize() / dp + " dp y su hermano a " + tamano / dp,
                        Math.abs(t.getTextSize() - tamano) < dp);
            }
        }
    }

    // Cada línea acaba en un espacio (o en el final del texto), sin elipsis y sin pasarse de ancho.
    private static void sinPartir(TextView t, Locale locale, float escala, float dp) {
        Layout l = t.getLayout();
        CharSequence s = t.getText();
        String donde = "[" + locale + ", letra " + escala + "] «" + s + "» a " + t.getTextSize() / dp + " dp";
        assertTrue(donde + ": sin colocar", l != null);
        int lineas = l.getLineCount();
        assertTrue(donde + ": no se ve entero (" + l.getLineEnd(lineas - 1) + " de " + s.length() + ")",
                l.getLineEnd(lineas - 1) == s.length());
        for (int i = 0; i < lineas; i++) {
            assertTrue(donde + ": con elipsis en la línea " + i, l.getEllipsisCount(i) == 0);
            // Sin el espacio del final, que no se ve y getLineWidth sí cuenta.
            assertTrue(donde + ": la línea " + i + " mide " + l.getLineMax(i) / dp + " dp en "
                    + l.getWidth() / dp + " dp", l.getLineMax(i) <= l.getWidth() + 1);
            int fin = l.getLineEnd(i);
            if (i < lineas - 1) {
                assertTrue(donde + ": parte una palabra en «" + s.subSequence(l.getLineStart(i), fin) + "»",
                        Character.isWhitespace(s.charAt(fin - 1)) && s.charAt(fin - 1) != ' ');
            }
        }
    }
}
