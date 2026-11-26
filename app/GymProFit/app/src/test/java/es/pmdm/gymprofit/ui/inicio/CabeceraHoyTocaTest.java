package es.pmdm.gymprofit.ui.inicio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilderFactory;

// ============================================================
// CabeceraHoyTocaTest — GP-285 (remate): el subtítulo de «Hoy toca» no pasa de dos líneas junto
// a las siluetas; si necesita más, va debajo, a todo el ancho de la tarjeta.
// ============================================================
public class CabeceraHoyTocaTest {

    private static final String ANDROID = "http://schemas.android.com/apk/res/android";

    @Test
    public void hasta_dos_lineas_va_junto_a_las_siluetas() {
        assertFalse(CabeceraHoyToca.vaDebajo(0, true));
        assertFalse(CabeceraHoyToca.vaDebajo(1, true));
        assertFalse(CabeceraHoyToca.vaDebajo(2, true));
    }

    @Test
    public void con_tres_lineas_o_mas_va_debajo() {
        assertTrue(CabeceraHoyToca.vaDebajo(3, true));
        assertTrue(CabeceraHoyToca.vaDebajo(5, true));
    }

    @Test
    public void sin_siluetas_ya_tiene_todo_el_ancho_y_no_se_mueve() {
        assertFalse(CabeceraHoyToca.vaDebajo(3, false));
        assertFalse(CabeceraHoyToca.vaDebajo(6, false));
    }

    @Test
    public void el_texto_de_debajo_va_fuera_de_la_fila_de_las_siluetas_y_con_su_misma_letra() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(Paths.get("src", "main", "res", "layout", "fragment_inicio.xml").toFile());
        Element junto = porId(doc, "@+id/tvHoySub");
        Element ancho = porId(doc, "@+id/tvHoySubAncho");
        Element silueta = porId(doc, "@+id/siluetaHoy");
        Element cabecera = porId(doc, "@+id/cabeceraHoy");
        assertEquals("es.pmdm.gymprofit.ui.inicio.CabeceraHoyToca", cabecera.getTagName());
        assertEquals("vertical", cabecera.getAttributeNS(ANDROID, "orientation"));
        // El de debajo cuelga de la cabecera, después de la fila; el otro, dentro de la fila.
        Node fila = silueta.getParentNode();
        assertEquals(cabecera, fila.getParentNode());
        assertEquals(cabecera, ancho.getParentNode());
        assertNotEquals(fila, ancho.getParentNode());
        assertEquals(fila, junto.getParentNode().getParentNode());
        // Los dos textos se ven igual, y el de debajo nace escondido y a todo el ancho.
        for (String atributo : new String[]{ "textSize", "textColor" }) {
            assertEquals(atributo, junto.getAttributeNS(ANDROID, atributo), ancho.getAttributeNS(ANDROID, atributo));
        }
        assertEquals("gone", ancho.getAttributeNS(ANDROID, "visibility"));
        assertEquals("match_parent", ancho.getAttributeNS(ANDROID, "layout_width"));
        // Ninguno de los dos se recorta: el subtítulo se lee entero.
        assertEquals("", junto.getAttributeNS(ANDROID, "maxLines"));
        assertEquals("", ancho.getAttributeNS(ANDROID, "maxLines"));

        // La cabecera solo toca sus vistas cuando el sitio cambia: tocarlas en cada medida
        // pediría otra medida, sin fin (y eso son fotogramas).
        String codigo = new String(Files.readAllBytes(Paths.get("src", "main", "java", "es", "pmdm", "gymprofit",
                "ui", "inicio", "CabeceraHoyToca.java")), StandardCharsets.UTF_8);
        int guarda = codigo.indexOf("if (!cambia && !otroTexto) return;");
        assertTrue("falta la guarda", guarda > 0);
        assertTrue("las vistas se tocan antes de la guarda", codigo.indexOf("setVisibility(") > guarda);
        assertTrue(codigo.indexOf("ancho.setText(") > guarda);
    }

    private static Element porId(Document doc, String id) {
        NodeList todos = doc.getElementsByTagName("*");
        for (int i = 0; i < todos.getLength(); i++) {
            Element e = (Element) todos.item(i);
            if (id.equals(e.getAttributeNS(ANDROID, "id"))) return e;
        }
        throw new AssertionError("no hay " + id);
    }
}
