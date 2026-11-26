package es.pmdm.gymprofit.ui.inicio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

// ============================================================
// FilaRecordTest — GP-285, lote 1b: la fila del último récord de Inicio no parte ningún texto.
//
// Aquí, lo que no necesita un móvil: la regla que decide si la marca baja (FilaRecord.marcaAbajo),
// que el layout tiene cada texto en una línea, que la línea de detalle existe en los dos
// idiomas y que el nombre con el que FilaRecordPintadaTest (instrumentado) mide la fila es de
// verdad el más largo del catálogo. La fila pintada, a letra 1,0 y 1,3, la mira aquel.
// ============================================================
public class FilaRecordTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path CATALOGO = Paths.get("..", "..", "..", "api", "gymprofit-api", "src", "main",
            "resources", "catalogo", "ejercicios-es.csv");
    private static final Path PINTADA = Paths.get("src", "androidTest", "java", "es", "pmdm", "gymprofit", "ui",
            "inicio", "FilaRecordPintadaTest.java");
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";

    @Test
    public void la_marca_se_queda_a_la_derecha_mientras_todo_cabe() {
        // Una columna de 600: el detalle cabe y la marca ocupa menos de la mitad.
        assertFalse(FilaRecord.marcaAbajo(580, 280, 600));
        assertFalse(FilaRecord.marcaAbajo(600, 300, 600));
    }

    @Test
    public void la_marca_baja_si_el_detalle_no_cabe_en_su_linea() {
        assertTrue(FilaRecord.marcaAbajo(601, 100, 600));
    }

    @Test
    public void la_marca_baja_si_dejaria_al_nombre_con_menos_de_media_linea() {
        assertTrue(FilaRecord.marcaAbajo(300, 301, 600));
    }

    // Si el detalle no cabe ni a todo el ancho de la fila, va sin su rótulo.
    @Test
    public void el_detalle_pierde_el_rotulo_solo_si_no_cabe_a_todo_el_ancho() {
        assertFalse(FilaRecord.detalleCorto(794, 794));
        assertTrue(FilaRecord.detalleCorto(797, 794));
    }

    @Test
    public void cada_texto_de_la_fila_va_en_una_linea() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(RES.resolve("layout/view_record_inicio.xml").toFile());
        NodeList textos = doc.getElementsByTagName("TextView");
        assertEquals("nombre, marca, detalle y la marca de abajo", 4, textos.getLength());
        for (int i = 0; i < textos.getLength(); i++) {
            Element t = (Element) textos.item(i);
            String id = t.getAttributeNS(ANDROID, "id");
            assertEquals(id, "1", t.getAttributeNS(ANDROID, "maxLines"));
            // El nombre y el detalle avisan con puntos suspensivos si no caben; la marca no se corta nunca.
            boolean marca = id.contains("Marca");
            assertEquals(id, marca ? "" : "end", t.getAttributeNS(ANDROID, "ellipsize"));
        }
        // E Inicio la incluye: ya no lleva la fila escrita dentro.
        String inicio = new String(Files.readAllBytes(RES.resolve("layout/fragment_inicio.xml")), StandardCharsets.UTF_8);
        assertTrue(inicio.contains("<include layout=\"@layout/view_record_inicio\" />"));
        assertFalse(inicio.contains("@+id/tvRecordAntes"));
    }

    @Test
    public void la_linea_de_detalle_esta_en_los_dos_idiomas() throws IOException {
        for (String carpeta : new String[]{ "values", "values-en" }) {
            String s = new String(Files.readAllBytes(RES.resolve(carpeta).resolve("strings.xml")), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("<string name=\"inicio_record_detalle\">([^<]+)</string>").matcher(s);
            assertTrue(carpeta + ": falta inicio_record_detalle", m.find());
            assertTrue(carpeta + ": «" + m.group(1) + "»", m.group(1).contains("%1$s") && m.group(1).contains("%2$s"));
            assertFalse(carpeta + ": el detalle va en una línea", m.group(1).contains("\\n"));
        }
    }

    // La fila se mide con el nombre más largo del catálogo: que lo sea, y lo siga siendo si el
    // catálogo cambia.
    @Test
    public void la_fila_se_mide_con_el_nombre_mas_largo_del_catalogo() throws IOException {
        String csv = new String(Files.readAllBytes(CATALOGO), StandardCharsets.UTF_8);
        String masLargo = "";
        // fed_id,nombre_es,"descripción con saltos de línea": el nombre es el segundo campo de
        // cada línea que empieza un registro (las de la descripción empiezan por un número o comillas).
        Matcher m = Pattern.compile("(?m)^([A-Za-z0-9_'.()&+/-]+),(\"[^\"]*\"|[^,\\n]*),").matcher(csv);
        int nombres = 0;
        while (m.find()) {
            if (m.group(1).equals("fed_id")) continue;
            String nombre = m.group(2).replace("\"", "");
            nombres++;
            if (nombre.length() > masLargo.length()) masLargo = nombre;
        }
        assertTrue("el catálogo trae cientos de ejercicios y se han leído " + nombres, nombres > 500);
        String pintada = new String(Files.readAllBytes(PINTADA), StandardCharsets.UTF_8);
        assertTrue("el más largo del catálogo es «" + masLargo + "» y FilaRecordPintadaTest mide con otro",
                pintada.contains("\"" + masLargo + "\""));
    }
}
