package es.pmdm.gymprofit.ui.inicio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilderFactory;

// ============================================================
// EncajeHeroeTest — GP-285 (remate): la tarjeta «Hoy toca» frente a la barra flotante.
//
//   · Si su último botón queda a menos del margen de la barra (o cortado por ella) y ceder
//     arregla eso, la tarjeta cede justo lo que falta; si no llega ni cediendo todo, no se toca.
//   · La cuenta parte del alto natural: con lo ya cedido da lo mismo y no oscila.
//   · «La última vez, hace 1 semana» nunca se parte: una línea, a todo el ancho de la tarjeta,
//     fuera de la fila de las siluetas.
// ============================================================
public class EncajeHeroeTest {

    private static final String ANDROID = "http://schemas.android.com/apk/res/android";

    // El botón acaba en 1000 y la barra empieza en 1010; margen de 42 px y hasta 63 de cesión.
    private static final int MARGEN = 42, MAXIMO = 63;

    @Test
    public void con_sitio_de_sobra_no_cede() {
        assertEquals(0, EncajeHeroe.cede(900, 0, 1010, MARGEN, MAXIMO));
        // Justo en el margen: tampoco.
        assertEquals(0, EncajeHeroe.cede(968, 0, 1010, MARGEN, MAXIMO));
    }

    @Test
    public void justo_de_sitio_cede_lo_que_falta() {
        // A 4 px de la barra (el 1,5 dp del lote 1): faltan 38 para el margen.
        assertEquals(38, EncajeHeroe.cede(1006, 0, 1010, MARGEN, MAXIMO));
        // Cortado por la barra: 20 px debajo de su borde más el margen.
        assertEquals(62, EncajeHeroe.cede(1030, 0, 1010, MARGEN, MAXIMO));
        // En el límite de lo que puede ceder.
        assertEquals(MAXIMO, EncajeHeroe.cede(1031, 0, 1010, MARGEN, MAXIMO));
    }

    @Test
    public void si_no_llega_ni_cediendo_todo_no_se_toca() {
        assertEquals(0, EncajeHeroe.cede(1032, 0, 1010, MARGEN, MAXIMO));
        assertEquals(0, EncajeHeroe.cede(1600, 0, 1010, MARGEN, MAXIMO));
        // Sin nada que ceder (el carrusel no está).
        assertEquals(0, EncajeHeroe.cede(1006, 0, 1010, MARGEN, 0));
    }

    @Test
    public void con_lo_ya_cedido_da_lo_mismo() {
        // Tras ceder 38 el botón acaba en 968: la cuenta parte del alto natural y repite 38.
        assertEquals(38, EncajeHeroe.cede(968, 38, 1010, MARGEN, MAXIMO));
        // Si la barra de la sesión aparece (el tope sube 150 px), ya no llega: vuelve a su alto.
        assertEquals(0, EncajeHeroe.cede(968, 38, 860, MARGEN, MAXIMO));
        // Y si deja de hacer falta, también.
        assertEquals(0, EncajeHeroe.cede(968, 38, 1200, MARGEN, MAXIMO));
    }

    @Test
    public void la_ultima_vez_no_se_parte() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(Paths.get("src", "main", "res", "layout", "fragment_inicio.xml").toFile());
        Element ultima = porId(doc, "@+id/tvHoyUltima");
        Element silueta = porId(doc, "@+id/siluetaHoy");
        assertEquals("una línea", "1", ultima.getAttributeNS(ANDROID, "maxLines"));
        assertEquals("end", ultima.getAttributeNS(ANDROID, "ellipsize"));
        // No comparte fila con las siluetas: cuelga de la columna de la tarjeta, a todo su ancho.
        Element padre = (Element) ultima.getParentNode();
        assertEquals("vertical", padre.getAttributeNS(ANDROID, "orientation"));
        assertEquals("@drawable/bg_cristal_heroe", padre.getAttributeNS(ANDROID, "background"));
        for (Node n = ultima.getParentNode(); n != null; n = n.getParentNode()) {
            assertNotEquals("la etiqueta no puede ir dentro de la fila de las siluetas", silueta.getParentNode(), n);
        }
        // Y va después de esa fila (de la cabecera que la lleva): debajo de las siluetas.
        Node cabecera = silueta.getParentNode().getParentNode();
        assertEquals(padre, cabecera.getParentNode());
        boolean despues = false;
        for (Node n = cabecera.getNextSibling(); n != null; n = n.getNextSibling()) {
            if (n == ultima) despues = true;
        }
        assertTrue("debajo de la fila del título y las siluetas", despues);
        // El bloque que cede tiene nombre: es el del icono de cada ejercicio.
        Document item = f.newDocumentBuilder().parse(Paths.get("src", "main", "res", "layout", "item_hoy_ejercicio.xml").toFile());
        assertEquals("@dimen/hoy_ejercicio_icono_alto",
                porId(item, "@+id/bloqueIconoEjercicio").getAttributeNS(ANDROID, "layout_height"));
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
