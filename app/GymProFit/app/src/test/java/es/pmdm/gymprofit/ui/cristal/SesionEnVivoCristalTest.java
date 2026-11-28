package es.pmdm.gymprofit.ui.cristal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilderFactory;

// ============================================================
// SesionEnVivoCristalTest — la Sesión en vivo está montada como pide Liquid Glass (GP-285,
// lote 2; DEC-070), leído de sus layouts y de su código.
//
// · La raíz lleva el tema de cristal, y la lista va DENTRO del lienzo (es lo que el cristal
//   vivo desenfoca).
// · Un cristal vivo va siempre FUERA de su lienzo: dentro se dibujaría a sí mismo. Vale para
//   la pantalla y para el descanso, que se incluye en ella.
// · La pantalla coloca sus márgenes del sistema (InsetsUtils la deja), con las barras
//   transparentes de toda pantalla Liquid Glass.
// · Al abrirse no saca el teclado: en Android 8.1 o menos el sistema da el foco al primer
//   campo de la lista y, sin stateHidden, el teclado tapaba media pantalla nada más entrar.
// ============================================================
public class SesionEnVivoCristalTest {

    private static final Path MAIN = Paths.get("src", "main");
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String VIVO = "es.pmdm.gymprofit.ui.cristal.CristalVivo";
    private static final String LIENZO = "es.pmdm.gymprofit.ui.cristal.LienzoCristal";

    @Test
    public void la_lista_va_en_el_lienzo_y_el_cristal_vivo_fuera() throws Exception {
        Document pantalla = xml("res/layout/activity_sesion_en_vivo.xml");
        Element raiz = pantalla.getDocumentElement();
        assertEquals("@style/ThemeOverlay.GymProFit.Cristal", raiz.getAttributeNS(ANDROID, "theme"));
        Element lienzo = (Element) pantalla.getElementsByTagName(LIENZO).item(0);
        assertNotNull("la pantalla tiene su lienzo", lienzo);
        assertEquals("@id/lienzoCristal", lienzo.getAttributeNS(ANDROID, "id"));
        assertTrue("la lista va dentro del lienzo", dentro(porId(pantalla, "rvSesion"), lienzo));
        assertTrue("el fondo ambiental va dentro del lienzo", dentro(porId(pantalla, "fondoAmbiental"), lienzo));
        for (String borde : new String[]{ "bordeSuperior", "bordeInferior" }) {
            assertTrue(borde + " va dentro del lienzo: el cristal vivo desenfoca la lista ya desvanecida",
                    dentro(porId(pantalla, borde), lienzo));
        }
        NodeList vivos = pantalla.getElementsByTagName(VIVO);
        assertTrue("la cabecera, el resumen y el guardado son cristal vivo", vivos.getLength() >= 4);
        for (int i = 0; i < vivos.getLength(); i++) {
            assertFalse("un cristal vivo dentro del lienzo se dibujaría a sí mismo",
                    dentro((Element) vivos.item(i), lienzo));
        }
        // El descanso se incluye fuera del lienzo, y es cristal vivo.
        NodeList incluidos = pantalla.getElementsByTagName("include");
        boolean descanso = false;
        for (int i = 0; i < incluidos.getLength(); i++) {
            Element inc = (Element) incluidos.item(i);
            if (!"@layout/view_descanso".equals(inc.getAttribute("layout"))) continue;
            descanso = true;
            assertFalse("el descanso va fuera del lienzo", dentro(inc, lienzo));
        }
        assertTrue("la pantalla incluye el descanso", descanso);
        Document panel = xml("res/layout/view_descanso.xml");
        assertTrue("el panel del descanso es cristal vivo",
                porId(panel, "panelDescanso").getTagName().equals(VIVO));
        assertTrue("el aviso del fin del descanso es cristal vivo",
                porId(panel, "avisoFinDescanso").getTagName().equals(VIVO));
    }

    @Test
    public void la_pantalla_coloca_sus_margenes_y_no_saca_el_teclado_al_abrirse() throws Exception {
        String insets = leer("java/es/pmdm/gymprofit/utils/InsetsUtils.java");
        assertTrue("InsetsUtils deja a la sesión colocar sus márgenes", insets.contains("\"SesionEnVivoActivity\""));
        String pantalla = leer("java/es/pmdm/gymprofit/ui/activities/SesionEnVivoActivity.java");
        assertTrue(pantalla.contains("VentanaCristal.barrasTransparentes(this)"));
        assertTrue("el fondo es el de la sesión, con el dorado", pantalla.contains("FondoAmbiental.sesion(this)"));

        NodeList pantallas = xml("AndroidManifest.xml").getElementsByTagName("activity");
        for (int i = 0; i < pantallas.getLength(); i++) {
            Element a = (Element) pantallas.item(i);
            if (!".ui.activities.SesionEnVivoActivity".equals(a.getAttributeNS(ANDROID, "name"))) continue;
            String teclado = a.getAttributeNS(ANDROID, "windowSoftInputMode");
            assertTrue("la lista se recoloca con el teclado: " + teclado, teclado.contains("adjustResize"));
            assertTrue("el teclado no sale solo al abrir la pantalla: " + teclado, teclado.contains("stateHidden"));
            return;
        }
        throw new AssertionError("la Sesión en vivo no está en el manifiesto");
    }

    // Lo que una fila dice con su fondo lo dice también con palabras (DEC-019; revisor de UI):
    // la del récord y la serie en curso tienen su nombre para TalkBack, en los dos idiomas.
    @Test
    public void la_fila_del_record_y_la_serie_en_curso_se_nombran_para_talkback() throws Exception {
        String adapter = leer("java/es/pmdm/gymprofit/ui/adapters/SesionEnVivoAdapter.java");
        assertTrue(adapter.contains("f.oro ? R.string.envivo_serie_record_a11y"));
        assertTrue(adapter.contains("f.lente || (f.siguiente && !f.hecha) ? R.string.envivo_serie_siguiente_a11y"));
        assertTrue(leer("res/values/strings.xml").contains("<string name=\"envivo_serie_record_a11y\">Serie %1$d, récord</string>"));
        assertTrue(leer("res/values-en/strings.xml").contains("<string name=\"envivo_serie_record_a11y\">Set %1$d, record</string>"));
        // Las piezas de cristal que solo se quedan los toques no son paradas de foco.
        Document pantalla = xml("res/layout/activity_sesion_en_vivo.xml");
        for (String id : new String[]{ "barraResumen", "cardGuardado" }) {
            Element pieza = porId(pantalla, id);
            assertEquals(id, "true", pieza.getAttributeNS(ANDROID, "clickable"));
            assertEquals(id + " no es una parada de foco", "false", pieza.getAttributeNS(ANDROID, "focusable"));
            assertEquals(id, "no", pieza.getAttributeNS(ANDROID, "importantForAccessibility"));
        }
    }

    private static String leer(String ruta) throws IOException {
        return new String(Files.readAllBytes(MAIN.resolve(ruta)), StandardCharsets.UTF_8);
    }

    private static Document xml(String ruta) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        return f.newDocumentBuilder().parse(MAIN.resolve(ruta).toFile());
    }

    private static boolean dentro(Element e, Element antepasado) {
        for (Node n = e.getParentNode(); n != null; n = n.getParentNode()) {
            if (n == antepasado) return true;
        }
        return false;
    }

    private static Element porId(Document doc, String id) {
        NodeList todos = doc.getElementsByTagName("*");
        for (int i = 0; i < todos.getLength(); i++) {
            Element e = (Element) todos.item(i);
            String suyo = e.getAttributeNS(ANDROID, "id");
            if (suyo.equals("@+id/" + id) || suyo.equals("@id/" + id)) return e;
        }
        throw new AssertionError("no existe " + id);
    }
}
