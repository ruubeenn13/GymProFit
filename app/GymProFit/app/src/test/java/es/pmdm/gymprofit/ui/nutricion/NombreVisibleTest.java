package es.pmdm.gymprofit.ui.nutricion;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

// ============================================================
// NombreVisibleTest — los botones de copiar se anuncian empezando por lo que se ve (DEC-019).
// Con control por voz se dice el texto del botón; si el nombre accesible no lo contiene, no
// se encuentra. El revisor lo vio dos veces en el lote 3 de GP-216: «Copiar ayer» en
// Nutrición y «Copiar la de ayer» en la comida vacía leían «Sí, copiar…». Se comprueba en
// los dos idiomas leyendo los strings.xml, sin Android.
// ============================================================
public class NombreVisibleTest {

    private static final String[] COMIDAS = {"desayuno", "almuerzo", "comida", "merienda", "cena"};

    private static Map<String, String> cadenas(String carpeta) throws Exception {
        File f = new File("src/main/res/" + carpeta + "/strings.xml");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
        NodeList lista = doc.getElementsByTagName("string");
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < lista.getLength(); i++) {
            Element e = (Element) lista.item(i);
            m.put(e.getAttribute("name"), e.getTextContent().replace("\\'", "'"));
        }
        return m;
    }

    private static void empiezaPor(Map<String, String> c, String visible, String prefijo, String idioma) {
        String texto = c.get(visible);
        assertNotNull(visible + " en " + idioma, texto);
        for (String comida : COMIDAS) {
            String nombre = c.get(prefijo + comida);
            assertNotNull(prefijo + comida + " en " + idioma, nombre);
            assertTrue(idioma + ": «" + nombre + "» no empieza por «" + texto + "»", nombre.startsWith(texto));
        }
    }

    @Test
    public void copiar_la_de_ayer_y_copiar_ayer_empiezan_por_su_texto_en_los_dos_idiomas() throws Exception {
        for (String carpeta : new String[]{"values", "values-en"}) {
            Map<String, String> c = cadenas(carpeta);
            empiezaPor(c, "copiar_profi_boton", "copiar_ayer_", carpeta);
            empiezaPor(c, "nutricion_copiar_ayer", "nutricion_copiar_ayer_", carpeta);
        }
    }
}
