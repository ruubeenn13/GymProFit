package es.pmdm.gymprofit.ui.nutricion;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

// ============================================================
// PluralesNutricionTest — lo que se cuenta en Nutrición, Comida y Añadir tiene su plural en
// los dos idiomas (DEC-020). En el lote 3 de GP-216 el revisor oyó «1 gramos de proteína» a
// TalkBack, y «Tu semana» pasó a dos columnas con «día(s) con tu proteína» de pie. Se leen los
// strings.xml de ES y EN, sin Android: cada plural tiene «one» y «other».
// ============================================================
public class PluralesNutricionTest {

    private static final String[] PLURALES = {"nutricion_semana_proteina_pie", "nutricion_semana_apuntados_pie",
            "a11y_kilocalorias", "a11y_gramos", "a11y_gramos_proteina"};

    private static Set<String> cantidades(String carpeta, String nombre) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new File("src/main/res/" + carpeta + "/strings.xml"));
        NodeList plurales = doc.getElementsByTagName("plurals");
        Set<String> q = new HashSet<>();
        for (int i = 0; i < plurales.getLength(); i++) {
            Element p = (Element) plurales.item(i);
            if (!nombre.equals(p.getAttribute("name"))) continue;
            NodeList items = p.getElementsByTagName("item");
            for (int j = 0; j < items.getLength(); j++) q.add(((Element) items.item(j)).getAttribute("quantity"));
        }
        return q;
    }

    @Test
    public void lo_que_se_cuenta_tiene_singular_y_plural_en_los_dos_idiomas() throws Exception {
        for (String carpeta : new String[]{"values", "values-en"}) {
            for (String nombre : PLURALES) {
                Set<String> q = cantidades(carpeta, nombre);
                assertTrue(carpeta + "/" + nombre + " sin «one»: " + q, q.contains("one"));
                assertTrue(carpeta + "/" + nombre + " sin «other»: " + q, q.contains("other"));
            }
        }
    }
}
