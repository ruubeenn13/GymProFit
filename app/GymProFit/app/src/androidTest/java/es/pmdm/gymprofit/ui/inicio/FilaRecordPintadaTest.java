package es.pmdm.gymprofit.ui.inicio;

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
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.theme.MaterialComponentsViewInflater;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import es.pmdm.gymprofit.R;

// ============================================================
// FilaRecordPintadaTest — GP-285, lote 1b: la fila del último récord de Inicio, medida de
// verdad en esta versión de Android (se lanza en los tres emuladores), con el nombre más
// largo del catálogo, en español y en inglés, a letra 1,0 y 1,3.
//
// Se infla view_record_inicio con el tema de cristal y se mide al ancho que tiene en Inicio
// (el de la pantalla menos el margen de la pantalla y el relleno de la tarjeta «Tu progreso»).
// Ningún texto se parte: el nombre va en una línea (con puntos suspensivos: no cabe en ningún
// móvil); el detalle, en una línea y entero; y la marca, entera, a la derecha del nombre o, si
// no caben, debajo del texto.
//
// Que NOMBRE es el más largo del catálogo lo vigila FilaRecordTest, que sí puede leer el CSV.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class FilaRecordPintadaTest {

    private static final String NOMBRE = "Elevaciones para deltoides posterior sentado con el torso inclinado con mancuernas";
    // «100 kg × 12», con los espacios duros que le pone Inicio para que no se parta.
    private static final String MARCA = "100 kg × 12";

    private static View fila(float escala, String idioma) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration conf = new Configuration(base.getResources().getConfiguration());
        conf.fontScale = escala;
        conf.setLocale(new Locale(idioma));
        Context tema = new ContextThemeWrapper(base.createConfigurationContext(conf), R.style.Theme_GymProFit);
        Context c = new ContextThemeWrapper(tema, R.style.ThemeOverlay_GymProFit_Cristal);
        // Como lo infla una pantalla de verdad: con las vistas de AppCompat y Material. Sin
        // eso, en Android 7 un TextView a secas no aplica la fuente del estilo y sale más ancho.
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
        View tarjeta = inflador.inflate(R.layout.view_record_inicio, null, false);
        tarjeta.setVisibility(View.VISIBLE);
        ((TextView) tarjeta.findViewById(R.id.tvRecordEjercicio)).setText(NOMBRE);
        // «Último récord · hace 6 días · antes 95 × 12», con las cadenas de la app en ese idioma.
        String ultimo = c.getString(R.string.hoy_toca_separador, c.getString(R.string.ultimo_record),
                c.getResources().getQuantityString(R.plurals.tiempo_hace_dias, 6, 6));
        String hace = c.getResources().getQuantityString(R.plurals.tiempo_hace_dias, 6, 6);
        String antes = c.getString(R.string.record_peso_por_reps, "95", 12);
        FilaRecord fila = tarjeta.findViewById(R.id.filaRecord);
        fila.setDetalle(c.getString(R.string.inicio_record_detalle, ultimo, antes),
                c.getString(R.string.inicio_record_detalle, hace.substring(0, 1).toUpperCase(new Locale(idioma)) + hace.substring(1), antes));
        fila.setMarca(MARCA);
        int ancho = c.getResources().getDisplayMetrics().widthPixels
                - 2 * c.getResources().getDimensionPixelSize(R.dimen.margen_pantalla)
                - 2 * c.getResources().getDimensionPixelSize(R.dimen.espacio_16);
        // Tres pasadas, como en pantalla: la fila decide su disposición y su texto al medirse.
        for (int i = 0; i < 3; i++) {
            tarjeta.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            tarjeta.layout(0, 0, ancho, tarjeta.getMeasuredHeight());
        }
        return tarjeta;
    }

    // «Último récord · hace 6 días · antes 95 × 12», con las cadenas de la app en el idioma del contexto.
    private static String ultimoRecord(Context c) {
        String ultimo = c.getString(R.string.hoy_toca_separador, c.getString(R.string.ultimo_record),
                c.getResources().getQuantityString(R.plurals.tiempo_hace_dias, 6, 6));
        return c.getString(R.string.inicio_record_detalle, ultimo, c.getString(R.string.record_peso_por_reps, "95", 12));
    }

    private static Layout unaLinea(TextView tv, String caso) {
        Layout l = tv.getLayout();
        assertNotNull(caso + ": sin medir", l);
        assertEquals(caso + ": «" + tv.getText() + "» se parte", 1, l.getLineCount());
        return l;
    }

    private static void entero(TextView tv, String caso) {
        Layout l = unaLinea(tv, caso);
        assertEquals(caso + ": «" + tv.getText() + "» acaba en puntos suspensivos", 0, l.getEllipsisCount(0));
        float mide = tv.getPaint().measureText(tv.getText().toString());
        int hueco = tv.getWidth() - tv.getPaddingLeft() - tv.getPaddingRight();
        assertTrue(caso + ": «" + tv.getText() + "» mide " + mide + " px y tiene " + hueco, mide <= hueco + 1f);
    }

    private static void comprobar(float escala, String idioma) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            String caso = idioma + " a " + escala;
            View tarjeta = fila(escala, idioma);
            FilaRecord fila = tarjeta.findViewById(R.id.filaRecord);
            TextView nombre = tarjeta.findViewById(R.id.tvRecordEjercicio);
            TextView detalle = tarjeta.findViewById(R.id.tvRecordAntes);
            TextView derecha = tarjeta.findViewById(R.id.tvRecordMarca);
            TextView abajo = tarjeta.findViewById(R.id.tvRecordMarcaAbajo);

            // El nombre más largo no cabe en ningún móvil: una línea, con puntos suspensivos.
            Layout n = unaLinea(nombre, caso + ", nombre");
            assertTrue(caso + ": el nombre más largo del catálogo tendría que acabar en puntos suspensivos",
                    n.getEllipsisCount(0) > 0);
            entero(detalle, caso + ", detalle");
            // El rótulo solo se quita si el detalle entero no cabe ni a todo el ancho de la fila.
            String completo = ultimoRecord(detalle.getContext());
            float anchoCompleto = detalle.getPaint().measureText(completo);
            int dentro = fila.getWidth() - fila.getPaddingLeft() - fila.getPaddingRight();
            assertEquals(caso + ": el detalle va sin rótulo (mide " + anchoCompleto + " px entero, y hay " + dentro + ")",
                    anchoCompleto > dentro, fila.detalleEsCorto());
            assertEquals(caso + ": con rótulo o sin él", !fila.detalleEsCorto(),
                    detalle.getText().toString().startsWith(detalle.getContext().getString(R.string.ultimo_record)));

            // La marca está en un solo sitio, entera.
            boolean baja = fila.marcaEstaAbajo();
            assertEquals(caso + ": la marca de la derecha", baja ? View.GONE : View.VISIBLE, derecha.getVisibility());
            assertEquals(caso + ": la marca de abajo", baja ? View.VISIBLE : View.GONE, abajo.getVisibility());
            TextView marca = baja ? abajo : derecha;
            entero(marca, caso + ", marca");
            if (baja) {
                assertTrue(caso + ": la marca baja debajo del detalle", marca.getTop() >= detalle.getBottom());
                assertEquals(caso + ": con la marca abajo el detalle empieza en el borde", fila.getPaddingLeft(), detalle.getLeft());
            } else {
                assertTrue(caso + ": la marca va a la derecha del nombre", marca.getLeft() >= nombre.getRight());
                assertTrue(caso + ": el detalle empieza donde el nombre", detalle.getLeft() > fila.getPaddingLeft());
                assertTrue(caso + ": el nombre se queda sin sitio (" + nombre.getWidth() + " px)",
                        nombre.getWidth() >= marca.getWidth());
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
}
