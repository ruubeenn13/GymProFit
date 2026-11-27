package es.pmdm.gymprofit.ui.inicio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.theme.MaterialComponentsViewInflater;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.widget.TextoSinPartir;

// ============================================================
// HoyTocaSinPartirTest — GP-288: con letra grande, el carrusel de ejercicios de «Hoy toca» no
// parte palabras (DEC-019).
//
// A letra 2,0 las tarjetas (118 dp) partían «Domina / s» y cortaban el nombre con elipsis a media
// palabra («mancue…»). Se mide lo que Android ha colocado en el nombre y la pauta de cada tarjeta:
// cada línea acaba donde acaba una palabra (o en un guion que ya trae el nombre), el tamaño no
// baja del de letra 1,0 y es el mismo en todas las tarjetas, y si el nombre no cabe en sus líneas
// las usa todas y acaba en «…» justo tras una palabra entera. A letra 1,0, 1,3, 1,5 y 2,0, en
// español y en inglés, con nombres largos de verdad del catálogo.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class HoyTocaSinPartirTest {

    // Del catálogo (ejercicios-es.csv): el de la captura, palabras largas, un guion y el más largo.
    private static final String[] NOMBRES_ES = {
            "Dominadas",
            "Curl martillo alterno con mancuernas",
            "Limpiaparabrisas isométrico",
            "Estiramiento de isquiotibiales con pierna elevada",
            "Elevación glúteo-femoral en máquina",
            "Elevaciones para deltoides posterior sentado con el torso inclinado con mancuernas" };
    private static final String[] NOMBRES_EN = {
            "Pullups",
            "Alternate Hammer Curl",
            "Hyperextensions With No Hyperextension Bench",
            "Full Range-Of-Motion Lat Pulldown",
            "Brachialis-SMR",
            "Standing Dumbbell Straight-Arm Front Delt Raise Above Head" };

    @Test
    public void el_carrusel_no_parte_palabras_ni_corta_a_media_palabra() {
        for (String idioma : new String[]{ "es", "en" }) {
            for (float letra : new float[]{ 1f, 1.3f, 1.5f, 2f }) {
                final float escala = letra;
                final Locale locale = new Locale(idioma);
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> comprobar(locale, escala));
            }
        }
    }

    private void comprobar(Locale locale, float escala) {
        Context c = contexto(locale, escala);
        float dp = c.getResources().getDisplayMetrics().density;
        LinearLayout carrusel = carrusel(c, locale);
        // El tamaño de cada texto a letra 1,0: por debajo no se baja.
        LinearLayout aLetraNormal = carrusel(contexto(locale, 1f), locale);

        for (int texto : new int[]{ R.id.tvNombreEjercicio, R.id.tvMetaEjercicio }) {
            float suelo = ((TextoSinPartir) aLetraNormal.getChildAt(0).findViewById(texto)).getTextSize();
            float tamano = -1f;
            for (int i = 0; i < carrusel.getChildCount(); i++) {
                TextoSinPartir t = carrusel.getChildAt(i).findViewById(texto);
                String donde = "[" + locale + ", letra " + escala + "] «" + t.getTextoCompleto() + "» a "
                        + t.getTextSize() / dp + " dp";
                sinPartir(t, donde, dp);
                assertTrue(donde + ": por debajo de su tamaño a letra 1,0 (" + suelo / dp + " dp)",
                        t.getTextSize() >= suelo - 0.5f);
                if (tamano < 0) tamano = t.getTextSize();
                assertEquals(donde + ": no va al tamaño de sus hermanas", tamano, t.getTextSize(), 0.01f);
            }
        }
    }

    private static Context contexto(Locale locale, float escala) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration conf = new Configuration(base.getResources().getConfiguration());
        conf.fontScale = escala;
        conf.setLocale(locale);
        // El tema de Inicio: el carrusel va dentro de la tarjeta héroe de cristal.
        return new ContextThemeWrapper(new ContextThemeWrapper(base.createConfigurationContext(conf),
                R.style.Theme_GymProFit), R.style.ThemeOverlay_GymProFit_Cristal);
    }

    // El carrusel como lo monta InicioHoyDetalle, ya medido y colocado.
    private static LinearLayout carrusel(Context c, Locale locale) {
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
        boolean ingles = locale.getLanguage().equals("en");
        // La pauta más ancha que sale (series, rango y por lado) con la última marca debajo.
        String pauta = c.getString(R.string.pauta_por_pierna, c.getString(R.string.pauta_series, 4,
                c.getString(R.string.pauta_rango, 10, 12)));
        String marca = c.getString(R.string.inicio_ejercicio_ultima, ingles ? "102.5 kg" : "102,5 kg");
        LinearLayout carrusel = new LinearLayout(c);
        carrusel.setOrientation(LinearLayout.HORIZONTAL);
        for (String nombre : ingles ? NOMBRES_EN : NOMBRES_ES) {
            View item = inflador.inflate(R.layout.item_hoy_ejercicio, carrusel, false);
            ((TextoSinPartir) item.findViewById(R.id.tvNombreEjercicio)).setText(nombre);
            ((TextoSinPartir) item.findViewById(R.id.tvMetaEjercicio))
                    .setText(c.getString(R.string.inicio_dos_lineas, pauta, marca));
            carrusel.addView(item);
        }
        InicioHoyDetalle.igualarTextos(carrusel);
        int libre = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        // Dos pasadas, como en pantalla: el recorte del nombre pide medirse otra vez.
        for (int pasada = 0; pasada < 2; pasada++) {
            carrusel.measure(libre, libre);
            carrusel.layout(0, 0, carrusel.getMeasuredWidth(), carrusel.getMeasuredHeight());
        }
        return carrusel;
    }

    // Cada línea acaba entre palabras, nada se pasa de ancho, y la única elipsis es la «…» que va
    // tras una palabra entera del nombre, con todas las líneas ya usadas.
    private static void sinPartir(TextoSinPartir t, String donde, float dp) {
        Layout l = t.getLayout();
        assertTrue(donde + ": sin colocar", l != null);
        String visto = t.getText().toString();
        String entero = t.getTextoCompleto().toString();
        int lineas = l.getLineCount();
        assertTrue(donde + ": no se ve entero lo que hay puesto",
                l.getLineEnd(lineas - 1) == visto.length() && t.getMeasuredHeight() >= l.getHeight());
        for (int i = 0; i < lineas; i++) {
            assertTrue(donde + ": Android corta con elipsis en la línea " + i, l.getEllipsisCount(i) == 0);
            assertTrue(donde + ": la línea " + i + " mide " + l.getLineMax(i) / dp + " dp en "
                    + l.getWidth() / dp + " dp", l.getLineMax(i) <= l.getWidth() + 1);
            int fin = l.getLineEnd(i);
            if (i < lineas - 1) {
                char ultimo = visto.charAt(fin - 1);
                // Entre palabras: tras un espacio (no el duro), un salto o un guion del propio nombre.
                assertTrue(donde + ": parte una palabra en «" + visto.substring(l.getLineStart(i), fin) + "»",
                        (Character.isWhitespace(ultimo) && ultimo != ' ') || ultimo == '-');
            }
        }
        assertTrue(donde + ": lleva guion blando", visto.indexOf('­') < 0);
        if (visto.equals(entero)) return;
        // Recortado: todas las líneas usadas antes que la elipsis, y «…» tras una palabra entera.
        assertTrue(donde + ": se ve «" + visto + "», que no acaba en «…»", visto.endsWith("…"));
        String queda = visto.substring(0, visto.length() - 1);
        assertTrue(donde + ": se ve «" + visto + "», que no es su principio", entero.startsWith(queda));
        assertTrue(donde + ": «" + visto + "» corta a media palabra",
                Character.isLetterOrDigit(queda.charAt(queda.length() - 1))
                        && Character.isWhitespace(entero.charAt(queda.length())));
        assertEquals(donde + ": recorta sin usar todas sus líneas", t.getMaxLines(), lineas);
    }
}
