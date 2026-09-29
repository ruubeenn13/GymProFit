package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilderFactory;

import es.pmdm.gymprofit.ui.widget.BarraNavegacion;

// ============================================================
// LetraGrandeTest — GP-128: nada se corta a letra 2,0.
//
// Las pestañas de Progreso no caben a letra grande: van en un carril que se desplaza
// en horizontal, nunca recortadas. Las etiquetas de la barra no caben ni en dos
// líneas («Nutrición» es una palabra): crecen con la letra del sistema hasta 1,3 y ahí
// se quedan, como la barra de iOS; el nombre entero lo dan el tooltip y TalkBack.
// Lo que se ve de verdad se comprobó en el emulador a 1,0, 1,3 y 2,0, en ES y EN.
// ============================================================
public class LetraGrandeTest {

    private static final Path LAYOUT = Paths.get("src", "main", "res", "layout");

    @Test
    public void la_etiqueta_de_la_barra_sigue_a_la_letra_del_sistema_hasta_1_3() {
        assertEquals(1.0f, BarraNavegacion.escalaEtiqueta(1.0f), 0.0001f);
        assertEquals(0.85f, BarraNavegacion.escalaEtiqueta(0.85f), 0.0001f);
        assertEquals(1.15f, BarraNavegacion.escalaEtiqueta(1.15f), 0.0001f);
        assertEquals(1.3f, BarraNavegacion.escalaEtiqueta(1.3f), 0.0001f);
    }

    @Test
    public void por_encima_de_1_3_la_etiqueta_se_queda_en_1_3() {
        assertEquals(1.3f, BarraNavegacion.escalaEtiqueta(1.5f), 0.0001f);
        assertEquals(1.3f, BarraNavegacion.escalaEtiqueta(2.0f), 0.0001f);
    }

    @Test
    public void las_pestanas_de_progreso_van_en_un_carril_que_se_desplaza() throws Exception {
        Element pestanas = porId(LAYOUT.resolve("fragment_progreso.xml"), "@+id/pestanasProgreso");
        assertNotNull("falta pestanasProgreso", pestanas);

        Node padre = pestanas.getParentNode();
        assertEquals("si no caben, se desplazan en horizontal",
                "HorizontalScrollView", padre.getNodeName());
        assertEquals("con sitio, el carril ocupa el ancho y reparte las pestañas",
                "true", ((Element) padre).getAttribute("android:fillViewport"));
        assertEquals("el carril mide lo que sus pestañas: si no, las recorta",
                "wrap_content", pestanas.getAttribute("android:layout_width"));
        assertEquals("alineadas por línea base, a letra grande se descuelgan y se recortan",
                "false", pestanas.getAttribute("android:baselineAligned"));
    }

    @Test
    public void cada_pestana_de_progreso_mide_su_texto() throws Exception {
        // Un 0dp con peso dentro del carril se reparte a partes iguales y recorta la larga.
        String estilos = new String(java.nio.file.Files.readAllBytes(
                Paths.get("src", "main", "res", "values", "estilos_estructura.xml")), "UTF-8");
        int i = estilos.indexOf("name=\"Widget.GymProFit.PestanaSeccion\"");
        assertTrue(i >= 0);
        String estilo = estilos.substring(i, estilos.indexOf("</style>", i));
        assertTrue("la pestaña tiene que medir lo que su texto",
                estilo.contains("<item name=\"android:layout_width\">wrap_content</item>"));
    }

    @Test
    public void la_etiqueta_de_la_barra_no_se_recorta_a_una_altura_fija() throws Exception {
        Element etiqueta = porId(LAYOUT.resolve("view_pestana.xml"), "@+id/tvEtiqueta");
        assertNotNull(etiqueta);
        assertTrue("la etiqueta mide lo que su letra",
                "wrap_content".equals(etiqueta.getAttribute("android:layout_height")));
    }

    private static Element porId(Path xml, String id) throws Exception {
        NodeList nodos = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(xml.toFile()).getElementsByTagName("*");
        for (int i = 0; i < nodos.getLength(); i++) {
            Element e = (Element) nodos.item(i);
            if (id.equals(e.getAttribute("android:id"))) return e;
        }
        return null;
    }
}
