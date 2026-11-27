package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.cristal.CristalVivo;
import es.pmdm.gymprofit.ui.cristal.NivelCristal;

// ============================================================
// BarraNavegacionRecorteTest — GP-118: el «+» de la barra se ve entero. Y desde GP-285 (Liquid
// Glass), la barra flotante mide lo que dice el diseño.
//
// Lo que se afirma es lo que se ve: el rectángulo visible del botón, con los recortes de todos
// sus padres, mide lo mismo que el botón. Se mide en un dispositivo porque ese cálculo es el
// del propio Android (getChildVisibleRect), el mismo que decide qué se dibuja.
//
// De la barra nueva: la cápsula de 64 dp a 16 de los lados y 26 del borde de abajo (más el
// hueco del sistema), el «+» de 64 a 10 de ella y a su misma altura, y cada pestaña con 48 dp
// o más en un móvil de 360 dp de ancho. Y que mirar el nivel del cristal no falla en esta
// versión de Android (lee un ajuste del sistema que no tiene API pública hasta Android 16).
// ============================================================
@RunWith(AndroidJUnit4.class)
public class BarraNavegacionRecorteTest {

    // Una barra medida y colocada a ese ancho, con ese hueco del sistema por debajo.
    private static BarraNavegacion barra(Context c, int ancho, int huecoSistema) {
        BarraNavegacion barra = new BarraNavegacion(c);
        barra.aplicarInsetInferior(huecoSistema);
        barra.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        barra.layout(0, 0, ancho, barra.getMeasuredHeight());
        return barra;
    }

    private static Context tema() {
        return new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_GymProFit);
    }

    @Test
    public void el_boton_de_acciones_se_ve_entero() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            BarraNavegacion barra = barra(tema(), 1080, 0);
            View boton = barra.findViewById(R.id.btnAcciones);
            Rect visible = new Rect();
            assertTrue("el botón no se ve", boton.getGlobalVisibleRect(visible));
            assertEquals("alto visible del «+» frente a su alto", boton.getHeight(), visible.height());
            assertEquals("ancho visible del «+» frente a su ancho", boton.getWidth(), visible.width());
        });
    }

    @Test
    public void la_barra_flotante_mide_lo_del_diseno() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context c = tema();
            float dp = c.getResources().getDisplayMetrics().density;
            int ancho = Math.round(360 * dp), sistema = Math.round(48 * dp);
            BarraNavegacion barra = barra(c, ancho, sistema);
            View capsula = barra.findViewById(R.id.capsulaBarra);
            View boton = barra.findViewById(R.id.btnAcciones);

            assertEquals("cápsula de 64 dp", Math.round(64 * dp), capsula.getHeight());
            assertEquals("a 16 dp del lado", Math.round(16 * dp), capsula.getLeft());
            assertEquals("a 26 dp del borde de abajo, más el hueco del sistema",
                    Math.round(26 * dp) + sistema, barra.getHeight() - capsula.getBottom());
            // El «+»: círculo de 64, a 10 de la cápsula, a su misma altura y a 16 del otro lado.
            assertEquals(Math.round(64 * dp), boton.getWidth());
            assertEquals(Math.round(64 * dp), boton.getHeight());
            assertEquals(Math.round(10 * dp), boton.getLeft() - capsula.getRight());
            assertEquals(capsula.getTop(), boton.getTop());
            assertEquals(Math.round(16 * dp), ancho - boton.getRight());
            // Cada pestaña, 48 dp o más en los dos sentidos, también en un móvil estrecho.
            for (int id : new int[]{ R.id.tabInicio, R.id.tabEntrenar, R.id.tabNutricion, R.id.tabProgreso }) {
                View p = barra.findViewById(id);
                assertTrue("pestaña de " + p.getWidth() / dp + " dp de ancho", p.getWidth() >= 48 * dp);
                assertTrue("pestaña de " + p.getHeight() / dp + " dp de alto", p.getHeight() >= 48 * dp);
            }
        });
    }

    // A letra 1,3 las etiquetas no caben a su tamaño entero en un móvil estrecho, y encogen. La
    // caja de cada una tiene que ser la de su texto ya encogido. Si encogen al colocarse, en
    // Android 8 o menos se quedan con la caja del tamaño grande (ahí AppCompat marca para medir
    // solo la etiqueta, no a sus padres, y ninguna medida posterior llega hasta ella) y el texto
    // sale descentrado: se veía al recrearse la pantalla (revisor de UI, GP-285).
    @Test
    public void a_letra_grande_la_caja_de_cada_etiqueta_es_la_de_su_texto() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context base = tema();
            Configuration grande = new Configuration(base.getResources().getConfiguration());
            grande.fontScale = 1.3f;
            Context c = new ContextThemeWrapper(base.createConfigurationContext(grande), R.style.Theme_GymProFit);
            float dp = c.getResources().getDisplayMetrics().density;
            BarraNavegacion barra = barra(c, Math.round(360 * dp), 0);

            TypedValue valor = new TypedValue();
            c.getResources().getValue(R.dimen.text_eyebrow, valor, true);
            float entero = TypedValue.complexToFloat(valor.data) * dp * BarraNavegacion.ESCALA_MAXIMA_ETIQUETA;
            for (int id : new int[]{ R.id.tabInicio, R.id.tabEntrenar, R.id.tabNutricion, R.id.tabProgreso }) {
                TextView etiqueta = barra.findViewById(id).findViewById(R.id.tvEtiqueta);
                // La prueba vale porque encogen: a 360 dp de ancho no caben a 1,3.
                assertTrue("«" + etiqueta.getText() + "» no ha encogido", etiqueta.getTextSize() < entero - 0.5f);
                float texto = etiqueta.getPaint().measureText(etiqueta.getText().toString());
                assertEquals("ancho de la caja de «" + etiqueta.getText() + "» frente al de su texto",
                        texto, etiqueta.getWidth(), 1.5f);
            }
        });
    }

    @Test
    public void el_nivel_del_cristal_se_puede_leer_en_esta_version() {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NivelCristal nivel = NivelCristal.actual(c);
        // Nunca mejor que lo que la versión permite.
        NivelCristal porVersion = NivelCristal.elegir(Build.VERSION.SDK_INT, false, NivelCristal.REFRACCION);
        assertTrue(nivel + " frente a " + porVersion, nivel.numero >= porVersion.numero);
        // Con el texto de alto contraste del sistema encendido (adb shell settings put secure
        // high_text_contrast_enabled 1), el nivel es el opaco en cualquier versión.
        boolean altoContraste = android.provider.Settings.Secure.getInt(
                c.getContentResolver(), "high_text_contrast_enabled", 0) == 1;
        if (altoContraste) assertEquals(NivelCristal.OPACO, nivel);
        // Y un cristal vivo sin lienzo detrás (suelto, como aquí) no desenfoca nada.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            CristalVivo capsula = barra(tema(), 1080, 0).findViewById(R.id.capsulaBarra);
            assertEquals(NivelCristal.OPACO, capsula.nivel());
        });
    }
}
