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
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.theme.MaterialComponentsViewInflater;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.cristal.CristalVivo;

// ============================================================
// CabeceraSesionTest — GP-285, remate del lote 2: la cabecera de la Sesión en vivo con letra
// grande, medida de verdad en esta versión de Android (se lanza en los tres emuladores).
//
// El caso de las capturas de la revisión: a letra 2,0, «Cuerpo completo C» con su programa
// «Cuerpo completo» ocupaba cuatro líneas («Cuerpo / completo C / Cuerpo / completo») y empujaba
// el resumen y la lista. La regla (la de GP-288):
//
// · El título, como mucho en dos líneas, y sin partir palabras.
// · Desde letra 1,5, el programa solo se ve si no repite el título y cabe entero en una línea;
//   si no, se oculta, y TalkBack lo sigue leyendo (el grupo lleva la rutina y el programa).
// · «Terminar» no se corta nunca: su texto entero, en su única línea.
// · Minimizar y «Terminar» son cristal estático (bajo ellos no pasa nada), con su zona táctil.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class CabeceraSesionTest {

    private static View pantalla(float escala, String idioma, int anchoDp, String rutina, String programa) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration conf = new Configuration(base.getResources().getConfiguration());
        conf.fontScale = escala;
        conf.setLocale(new Locale(idioma));
        Context c = new ContextThemeWrapper(base.createConfigurationContext(conf), R.style.Theme_GymProFit);
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
        View raiz = inflador.inflate(R.layout.activity_sesion_en_vivo, new FrameLayout(c), false);
        ((TituloSesion) raiz.findViewById(R.id.tituloSesion)).poner(rutina, programa);
        float dp = c.getResources().getDisplayMetrics().density;
        int ancho = Math.round(anchoDp * dp), alto = Math.round(800 * dp);
        raiz.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(alto, View.MeasureSpec.EXACTLY));
        raiz.layout(0, 0, ancho, alto);
        return raiz;
    }

    /** @param seVe si el programa tiene que verse; null, lo que decida la regla (solo se comprueba que cabe). */
    private static void comprobar(float escala, String idioma, int anchoDp, String rutina, String programa,
                                  Boolean seVe) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View raiz = pantalla(escala, idioma, anchoDp, rutina, programa);
            String caso = idioma + " a " + escala + " en " + anchoDp + " dp, «" + rutina + "» / «" + programa + "»";
            float dp = raiz.getResources().getDisplayMetrics().density;

            // El título: dos líneas como mucho, entero y sin partir palabras.
            TextoSinPartir titulo = raiz.findViewById(R.id.tvTituloSesion);
            Layout lt = titulo.getLayout();
            assertTrue(caso + ": el título ocupa " + lt.getLineCount() + " líneas", lt.getLineCount() <= 2);
            assertEquals(caso + ": el título va recortado", rutina, titulo.getText().toString());
            for (int i = 0; i < lt.getLineCount() - 1; i++) {
                int fin = lt.getLineEnd(i);
                assertTrue(caso + ": el título se parte a media palabra tras «"
                                + titulo.getText().subSequence(lt.getLineStart(i), fin) + "»",
                        Character.isWhitespace(titulo.getText().charAt(fin - 1)));
            }

            // El programa.
            TituloSesion grupo = raiz.findViewById(R.id.tituloSesion);
            TextoSinPartir sub = raiz.findViewById(R.id.tvSubtituloSesion);
            boolean visible = sub.getVisibility() == View.VISIBLE;
            assertEquals(caso + ": lo que dice el grupo y lo que se ve", visible, grupo.programaALaVista());
            if (seVe != null) assertEquals(caso + ": el programa a la vista", seVe, visible);
            int lineas = lt.getLineCount();
            if (visible) {
                Layout ls = sub.getLayout();
                lineas += ls.getLineCount();
                if (escala >= TituloSesion.LETRA_GRANDE) {
                    assertEquals(caso + ": con letra grande el programa va en una línea", 1, ls.getLineCount());
                    assertEquals(caso + ": el programa va recortado", programa, sub.getText().toString());
                    assertTrue(caso + ": el programa encoge para caber",
                            sub.getTextSize() >= sub.tamanoPedido() - 0.5f);
                }
            }
            if (escala >= TituloSesion.LETRA_GRANDE) {
                assertTrue(caso + ": título y programa ocupan " + lineas + " líneas", lineas <= 3);
            }
            // TalkBack lee la rutina y el programa de una vez, se vea o no el programa.
            String leido = String.valueOf(grupo.getContentDescription());
            assertTrue(caso + ": TalkBack no lee la rutina: " + leido, leido.contains(rutina));
            assertTrue(caso + ": TalkBack no lee el programa: " + leido, leido.contains(programa));
            assertEquals(caso + ": el título se leería dos veces", View.IMPORTANT_FOR_ACCESSIBILITY_NO,
                    titulo.getImportantForAccessibility());
            assertEquals(caso + ": el programa se leería dos veces", View.IMPORTANT_FOR_ACCESSIBILITY_NO,
                    sub.getImportantForAccessibility());

            // «Terminar»: entero, en su línea, dentro de la fila; y de cristal estático.
            View fila = raiz.findViewById(R.id.filaCabecera);
            MaterialButton terminar = raiz.findViewById(R.id.btnTerminar);
            Layout l = terminar.getLayout();
            assertEquals(caso + ": «" + terminar.getText() + "» no lleva su texto entero en su línea",
                    terminar.getText().length(), l.getLineEnd(0));
            assertEquals(caso + ": «Terminar» acaba en puntos suspensivos", 0, l.getEllipsisCount(0));
            int hueco = terminar.getWidth() - terminar.getPaddingLeft() - terminar.getPaddingRight();
            assertTrue(caso + ": «Terminar» mide " + l.getLineWidth(0) + " px y tiene " + hueco,
                    l.getLineWidth(0) <= hueco + 0.5f);
            assertTrue(caso + ": «Terminar» se sale de la fila", terminar.getRight() <= fila.getWidth());
            View minimizar = raiz.findViewById(R.id.btnMinimizar);
            for (View boton : new View[]{ minimizar, terminar }) {
                String nombre = caso + ", " + raiz.getResources().getResourceEntryName(boton.getId());
                assertTrue(nombre + " es cristal estático", boton instanceof BotonCristal);
                assertFalse(nombre + " no es cristal vivo", boton.getParent() instanceof CristalVivo);
                assertTrue(nombre + ": " + (boton.getHeight() / dp) + " dp de alto",
                        boton.getHeight() >= Math.round(48 * dp) - 1);
                assertTrue(nombre + ": " + (boton.getWidth() / dp) + " dp de ancho",
                        boton.getWidth() >= Math.round(48 * dp) - 1);
                assertTrue(nombre + ": sin nombre para TalkBack", boton.getContentDescription() != null
                        || ((MaterialButton) boton).getText().length() > 0);
            }
            // El título no se queda sin sitio: le queda ancho entre los dos botones.
            assertTrue(caso + ": al título le quedan " + (grupo.getWidth() / dp) + " dp", grupo.getWidth() > 0);
        });
    }

    // El caso de las capturas: API 36 (411 dp de ancho) y un móvil de 360 dp.
    @Test
    public void a_letra_2_0_el_programa_que_repite_el_titulo_no_se_ve() {
        comprobar(2.0f, "es", 411, "Cuerpo completo C", "Cuerpo completo", false);
        comprobar(2.0f, "es", 360, "Cuerpo completo C", "Cuerpo completo", false);
        comprobar(2.0f, "en", 360, "Full body C", "Full body", false);
    }

    @Test
    public void desde_letra_1_5_tampoco() {
        comprobar(1.5f, "es", 411, "Cuerpo completo C", "Cuerpo completo", false);
        comprobar(1.5f, "es", 360, "Cuerpo completo C", "Cuerpo completo", false);
    }

    @Test
    public void con_letra_grande_el_programa_que_no_cabe_en_una_linea_no_se_ve() {
        comprobar(2.0f, "es", 360, "Torso A", "Torso y pierna en cuatro días", false);
        comprobar(2.0f, "es", 411, "Torso A", "Torso y pierna en cuatro días", false);
        // Lo que decida la regla; si se ve, en una línea, entero y sin encoger.
        comprobar(1.5f, "es", 360, "Torso A", "Torso y pierna", null);
        comprobar(2.0f, "en", 360, "Push", "Push pull legs", null);
    }

    @Test
    public void con_letra_grande_el_programa_que_dice_otra_cosa_y_cabe_se_ve() {
        comprobar(1.5f, "es", 411, "Pierna", "PPL", true);
        comprobar(2.0f, "es", 411, "Pierna", "PPL", true);
    }

    @Test
    public void a_letra_normal_el_programa_se_ve_siempre() {
        comprobar(1.0f, "es", 360, "Cuerpo completo C", "Cuerpo completo", true);
        comprobar(1.3f, "es", 360, "Cuerpo completo C", "Cuerpo completo", true);
        comprobar(1.0f, "en", 411, "Torso A", "Torso y pierna", true);
    }
}
