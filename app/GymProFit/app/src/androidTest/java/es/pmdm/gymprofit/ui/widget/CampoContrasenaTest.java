package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.textfield.TextInputLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

import es.pmdm.gymprofit.R;

// ============================================================
// CampoContrasenaTest — GP-131: el nodo de accesibilidad de una contraseña oculta no
// lleva lo escrito.
//
// Se monta como en los formularios: TextInputLayout con el ojo (password_toggle) y el
// campo dentro, para que sea el delegado real de Material el que rellena el nodo. Se
// pide el nodo como lo pide un servicio de accesibilidad (createAccessibilityNodeInfo)
// y se mira su texto con la contraseña oculta y con el ojo abierto.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class CampoContrasenaTest {

    private static final String CLAVE = "Secreta1234.";
    private static final String PISTA = "Contraseña";

    private static TextInputLayout montar(Context c) {
        TextInputLayout capa = new TextInputLayout(c);
        capa.setHint(PISTA);
        CampoContrasena campo = new CampoContrasena(capa.getContext());
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        capa.addView(campo);
        capa.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        return capa;
    }

    private static Context contexto() {
        return new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_GymProFit);
    }

    private static String textoDelNodo(CampoContrasena campo) {
        AccessibilityNodeInfo nodo = campo.createAccessibilityNodeInfo();
        CharSequence t = nodo.getText();
        return t == null ? null : t.toString();
    }

    @Test
    public void con_la_contrasena_oculta_el_nodo_no_lleva_lo_escrito() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            TextInputLayout capa = montar(contexto());
            CampoContrasena campo = (CampoContrasena) capa.getEditText();
            campo.setText(CLAVE);

            assertTrue("la contraseña debería empezar oculta", campo.oculta());
            String texto = textoDelNodo(campo);
            assertFalse("el nodo lleva la contraseña: " + texto, texto != null && texto.contains(CLAVE));
            assertEquals("•".repeat(CLAVE.length()), texto);
            // Sigue siendo un campo de contraseña, con su pista.
            AccessibilityNodeInfo nodo = campo.createAccessibilityNodeInfo();
            assertTrue(nodo.isPassword());
            assertEquals(PISTA, nodo.getHintText().toString());
        });
    }

    @Test
    public void con_el_ojo_abierto_el_nodo_lleva_la_contrasena() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            TextInputLayout capa = montar(contexto());
            CampoContrasena campo = (CampoContrasena) capa.getEditText();
            campo.setText(CLAVE);

            capa.findViewById(com.google.android.material.R.id.text_input_end_icon).performClick();

            assertFalse("el ojo debería haber mostrado la contraseña", campo.oculta());
            assertEquals(CLAVE, textoDelNodo(campo));

            // Y al cerrarlo, vuelve a enmascararse.
            capa.findViewById(com.google.android.material.R.id.text_input_end_icon).performClick();
            assertTrue(campo.oculta());
            assertEquals("•".repeat(CLAVE.length()), textoDelNodo(campo));
        });
    }

    @Test
    public void vacio_el_nodo_lleva_la_pista_como_hoy() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            TextInputLayout capa = montar(contexto());
            CampoContrasena campo = (CampoContrasena) capa.getEditText();
            assertEquals(PISTA, textoDelNodo(campo));
        });
    }

    /** Los siete campos: cada formulario con contraseña, y cuántas lleva. */
    @Test
    public void los_siete_campos_de_contrasena_son_campo_contrasena() {
        int[][] formularios = {
                {R.layout.activity_login, 1},
                {R.layout.activity_guarda_plan, 1},
                {R.layout.activity_recuperar_password, 1},
                {R.layout.activity_cambiar_password, 2},
                {R.layout.activity_cambiar_correo, 1},
                {R.layout.activity_eliminar_cuenta, 1},
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context c = contexto();
            int total = 0;
            for (int[] f : formularios) {
                View raiz = LayoutInflater.from(c).inflate(f[0], null, false);
                List<EditText> contrasenas = new ArrayList<>();
                buscarContrasenas(raiz, contrasenas);
                String nombre = c.getResources().getResourceEntryName(f[0]);
                assertEquals("campos de contraseña en " + nombre, f[1], contrasenas.size());
                for (EditText e : contrasenas) {
                    assertTrue(nombre + ": " + c.getResources().getResourceEntryName(e.getId())
                            + " no es CampoContrasena", e instanceof CampoContrasena);
                }
                total += contrasenas.size();
            }
            assertEquals(7, total);
        });
    }

    private static void buscarContrasenas(View v, List<EditText> salida) {
        if (v instanceof EditText) {
            int variacion = ((EditText) v).getInputType() & InputType.TYPE_MASK_VARIATION;
            if (variacion == InputType.TYPE_TEXT_VARIATION_PASSWORD) salida.add((EditText) v);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) buscarContrasenas(g.getChildAt(i), salida);
        }
    }

    @Test
    public void el_error_se_sigue_anunciando() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            TextInputLayout capa = montar(contexto());
            CampoContrasena campo = (CampoContrasena) capa.getEditText();
            campo.setText(CLAVE);
            capa.setError("Demasiado corta");
            AccessibilityNodeInfo nodo = campo.createAccessibilityNodeInfo();
            assertEquals("Demasiado corta", nodo.getError().toString());
            assertFalse(nodo.getText().toString().contains(CLAVE));
        });
    }
}
