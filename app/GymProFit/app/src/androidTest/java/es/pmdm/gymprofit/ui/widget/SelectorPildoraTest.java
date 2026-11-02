package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import es.pmdm.gymprofit.R;

// ============================================================
// SelectorPildoraTest — GP-224: el único selector de opciones de la app.
//
// La opción elegida (y la falta de elección), lo que anuncia TalkBack (grupo con nombre,
// botón de opción con su estado y su posición, o pestaña con «seleccionada») y que la
// elegida sobrevive a recrear la vista, como en un giro. Los nodos se piden como los pide un
// servicio de accesibilidad (createAccessibilityNodeInfo).
// ============================================================
@RunWith(AndroidJUnit4.class)
public class SelectorPildoraTest {

    private static final CharSequence[] TEXTOS = {"taza", "gramos", "envase"};

    private static Context contexto() {
        return new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                R.style.Theme_GymProFit);
    }

    private static SelectorPildora selector() {
        SelectorPildora[] s = new SelectorPildora[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            s[0] = new SelectorPildora(contexto());
            s[0].setNombreGrupo("Unidad");
            s[0].setOpciones(TEXTOS, new CharSequence[]{"Taza", "Gramos", "Envase"});
        });
        return s[0];
    }

    private static AccessibilityNodeInfo nodo(View v) {
        AccessibilityNodeInfo[] n = new AccessibilityNodeInfo[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> n[0] = v.createAccessibilityNodeInfo());
        return n[0];
    }

    @Test
    public void sin_eleccion_ninguna_va_marcada() {
        SelectorPildora s = selector();
        assertEquals(-1, s.getElegida());
        assertEquals(3, s.getCuantas());
        for (int i = 0; i < 3; i++) assertFalse("opción " + i, nodo(s.getOpcion(i)).isChecked());
    }

    @Test
    public void elegir_cambia_el_indice_y_lo_marca() {
        SelectorPildora s = selector();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> s.setElegida(1, false));
        assertEquals(1, s.getElegida());
        assertTrue(nodo(s.getOpcion(1)).isChecked());
        assertFalse(nodo(s.getOpcion(0)).isChecked());
        // Un índice fuera de rango es no elegir nada.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> s.setElegida(7, false));
        assertEquals(-1, s.getElegida());
    }

    @Test
    public void talkback_anuncia_el_grupo_y_cada_opcion_con_su_estado_y_su_posicion() {
        SelectorPildora s = selector();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> s.setElegida(2, false));
        AccessibilityNodeInfo grupo = nodo(s);
        assertEquals(RadioGroup.class.getName(), grupo.getClassName());
        assertEquals("Unidad", String.valueOf(grupo.getContentDescription()));
        assertNotNull(grupo.getCollectionInfo());
        assertEquals(3, grupo.getCollectionInfo().getColumnCount());

        AccessibilityNodeInfo opcion = nodo(s.getOpcion(2));
        assertEquals(RadioButton.class.getName(), opcion.getClassName());
        assertTrue(opcion.isCheckable());
        assertTrue(opcion.isChecked());
        assertEquals("Envase", String.valueOf(opcion.getContentDescription()));
        assertNotNull(opcion.getCollectionItemInfo());
        assertEquals(2, opcion.getCollectionItemInfo().getColumnIndex());
        assertTrue(opcion.getCollectionItemInfo().isSelected());
    }

    @Test
    public void como_pestanas_anuncia_pestana_y_cual_esta_seleccionada() {
        SelectorPildora s = selector();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            s.setPestanas(true);
            s.setElegida(0, false);
        });
        assertEquals("android.widget.TabWidget", nodo(s).getClassName());
        AccessibilityNodeInfo pestana = nodo(s.getOpcion(0));
        assertTrue(pestana.isSelected());
        assertFalse(pestana.isCheckable());
        assertEquals(contexto().getString(R.string.selector_pestana), String.valueOf(
                androidx.core.view.accessibility.AccessibilityNodeInfoCompat.wrap(pestana).getRoleDescription()));
        assertFalse(nodo(s.getOpcion(1)).isSelected());
        assertEquals(1, nodo(s.getOpcion(1)).getCollectionItemInfo().getColumnIndex());
    }

    @Test
    public void la_elegida_sobrevive_a_recrear_la_vista() {
        SelectorPildora antes = selector();
        SparseArray<Parcelable> estado = new SparseArray<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            antes.setId(R.id.grupoUnidad);
            antes.setElegida(2, false);
            antes.saveHierarchyState(estado);
        });
        SelectorPildora despues = selector();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            despues.setId(R.id.grupoUnidad);
            despues.restoreHierarchyState(estado);
        });
        assertEquals(2, despues.getElegida());
    }

    // Tanda 1 del revisor: la opción respondía en 40 dp (la píldora) y no en los 48 de la pista.
    @Test
    public void cada_opcion_responde_al_toque_en_los_48_dp_de_la_pista() {
        SelectorPildora s = selector();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> s.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)));
        int zona = contexto().getResources().getDimensionPixelSize(R.dimen.zona_tactil);
        for (int i = 0; i < s.getCuantas(); i++) {
            assertTrue("opción " + i + " mide " + s.getOpcion(i).getMeasuredHeight(),
                    s.getOpcion(i).getMeasuredHeight() >= zona);
        }
    }
}
