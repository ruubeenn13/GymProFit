package es.pmdm.gymprofit.ui.adapters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.textfield.TextInputLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.widget.CamposSerie;

// ============================================================
// FilaSerieRotulosTest — GP-133: los rótulos de las columnas de Registrar se leen
// enteros, también con la letra grande.
//
// Se infla la fila de una serie con la escala de letra y el idioma pedidos y se mide
// al ancho que tiene en un móvil de 1080 px (el de la tarjeta de Registrar menos sus
// márgenes). Un rótulo cabe si su texto, a la letra del campo, entra en el hueco del
// campo sin sus rellenos: es el mismo hueco en que Material lo dibuja.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class FilaSerieRotulosTest {

    /** Ancho de la fila de series en el AVD de 1080 px, medido en la captura. */
    private static final int ANCHO_FILA = 908;

    private static View fila(float escala, String idioma) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration conf = new Configuration(base.getResources().getConfiguration());
        conf.fontScale = escala;
        conf.setLocale(new Locale(idioma));
        Context c = new ContextThemeWrapper(base.createConfigurationContext(conf), R.style.Theme_GymProFit);
        View fila = LayoutInflater.from(c).inflate(R.layout.item_serie, null, false);
        // Con un rango de repeticiones, como en una rutina de programa.
        ((TextInputLayout) fila.findViewById(R.id.tilRepsSerie)).setHint("6–10");
        fila.measure(View.MeasureSpec.makeMeasureSpec(ANCHO_FILA, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        fila.layout(0, 0, ANCHO_FILA, fila.getMeasuredHeight());
        return fila;
    }

    private static void rotuloEntero(View fila, int idCapa, String caso) {
        TextInputLayout capa = fila.findViewById(idCapa);
        EditText campo = capa.getEditText();
        String rotulo = String.valueOf(capa.getHint());
        float texto = campo.getPaint().measureText(rotulo);
        int hueco = campo.getWidth() - campo.getCompoundPaddingLeft() - campo.getCompoundPaddingRight();
        assertTrue(caso + ": «" + rotulo + "» mide " + texto + " px y el hueco es de " + hueco,
                texto <= hueco);
    }

    private static void comprobar(float escala, String idioma) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View f = fila(escala, idioma);
            String caso = idioma + " a " + escala;
            rotuloEntero(f, R.id.tilPesoSerie, caso);
            rotuloEntero(f, R.id.tilRepsSerie, caso);
        });
    }

    @Test
    public void a_1_0_en_espanol_y_en_ingles() {
        comprobar(1.0f, "es");
        comprobar(1.0f, "en");
        // Donde caben, los campos siguen en fila: solo se apilan cuando hace falta.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            assertFalse(((CamposSerie) fila(1.0f, "es").findViewById(R.id.camposSerie)).apilados());
            assertFalse(((CamposSerie) fila(1.0f, "en").findViewById(R.id.camposSerie)).apilados());
        });
    }

    @Test
    public void a_1_3_en_espanol_y_en_ingles() {
        comprobar(1.3f, "es");
        comprobar(1.3f, "en");
    }

    @Test
    public void a_2_0_en_espanol_y_en_ingles() {
        comprobar(2.0f, "es");
        comprobar(2.0f, "en");
    }
}
