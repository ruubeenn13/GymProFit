package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilderFactory;

// ============================================================
// EncajeCarruselTest — GP-285, lote 1b: los carruseles de Inicio encajan por elemento y no
// cortan en seco su último elemento.
//
// La cuenta del encaje (EncajeCarrusel) con un carrusel de cuatro elementos de 100 de ancho y
// 12 de hueco, en una ventana que deja 188 de recorrido; y, leído de los layouts, que los dos
// carruseles de Inicio son CarruselView, con el fundido en su token.
// ============================================================
public class EncajeCarruselTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final int[] BORDES = { 0, 112, 224, 336 };
    private static final int MAXIMO = 188;

    @Test
    public void soltado_sin_lanzar_va_al_elemento_mas_cercano() {
        assertEquals(0, EncajeCarrusel.destino(BORDES, 40, 0, MAXIMO));
        assertEquals(112, EncajeCarrusel.destino(BORDES, 70, 0, MAXIMO));
        assertEquals(112, EncajeCarrusel.destino(BORDES, 130, 0, MAXIMO));
    }

    @Test
    public void un_lanzamiento_avanza_al_menos_un_elemento_en_su_sentido() {
        // Corto: sin la regla se quedaría en el que ya está.
        assertEquals(112, EncajeCarrusel.destino(BORDES, 5, 50, MAXIMO));
        assertEquals(0, EncajeCarrusel.destino(BORDES, 110, -50, MAXIMO));
        // Desde un elemento encajado, al siguiente y al anterior.
        assertEquals(188, EncajeCarrusel.destino(BORDES, 112, 50, MAXIMO));
        assertEquals(0, EncajeCarrusel.destino(BORDES, 112, -50, MAXIMO));
    }

    @Test
    public void un_lanzamiento_fuerte_puede_pasar_mas_de_un_elemento() {
        int[] muchos = { 0, 112, 224, 336, 448, 560 };
        int destino = EncajeCarrusel.destino(muchos, 0, 1500, 600);
        assertTrue("con 1500 px/s tiene que pasar del primero: " + destino, destino > 112);
        // Y siempre acaba en el canto de un elemento.
        boolean enCanto = false;
        for (int b : muchos) if (b == destino) enCanto = true;
        assertTrue("acaba en " + destino + ", que no es el canto de ningún elemento", enCanto);
    }

    @Test
    public void nunca_pasa_del_final_ni_del_principio() {
        assertEquals(MAXIMO, EncajeCarrusel.destino(BORDES, 150, 4000, MAXIMO));
        assertEquals(MAXIMO, EncajeCarrusel.destino(BORDES, MAXIMO, 300, MAXIMO));
        assertEquals(0, EncajeCarrusel.destino(BORDES, 30, -4000, MAXIMO));
        assertEquals(0, EncajeCarrusel.destino(BORDES, 0, -300, MAXIMO));
        // Si todo cabe, no hay a dónde ir.
        assertEquals(0, EncajeCarrusel.destino(BORDES, 0, 900, 0));
        assertEquals(0, EncajeCarrusel.destino(new int[0], 0, 900, MAXIMO));
    }

    // Los dos carruseles de Inicio son CarruselView: funden su último elemento a la vista
    // (carrusel_fundido, 28 dp) y encajan. Y llegan al borde de su contenedor: no recortan a su
    // relleno, que es lo que deja el primer elemento alineado con el resto de la tarjeta.
    @Test
    public void los_dos_carruseles_de_inicio_funden_y_encajan() throws Exception {
        String dimens = new String(Files.readAllBytes(RES.resolve("values/dimens.xml")), StandardCharsets.UTF_8);
        assertTrue(dimens.contains("<dimen name=\"carrusel_fundido\">28dp</dimen>"));
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(RES.resolve("layout/fragment_inicio.xml").toFile());
        assertEquals("un carrusel de Inicio sigue siendo un HorizontalScrollView a secas", 0,
                doc.getElementsByTagName("HorizontalScrollView").getLength());
        NodeList carruseles = doc.getElementsByTagName("es.pmdm.gymprofit.ui.widget.CarruselView");
        assertEquals(2, carruseles.getLength());
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        NodeList conEspacio = f.newDocumentBuilder().parse(RES.resolve("layout/fragment_inicio.xml").toFile())
                .getElementsByTagName("es.pmdm.gymprofit.ui.widget.CarruselView");
        for (int i = 0; i < conEspacio.getLength(); i++) {
            Element c = (Element) conEspacio.item(i);
            assertEquals(c.getAttributeNS(ANDROID, "id"), "false", c.getAttributeNS(ANDROID, "clipToPadding"));
        }
        String vista = new String(Files.readAllBytes(Paths.get("src", "main", "java", "es", "pmdm", "gymprofit", "ui",
                "widget", "CarruselView.java")), StandardCharsets.UTF_8);
        assertTrue(vista.contains("setHorizontalFadingEdgeEnabled(true)"));
        assertTrue(vista.contains("R.dimen.carrusel_fundido"));
        assertTrue(vista.contains("EncajeCarrusel.destino("));
        // El fundido va en el borde de la vista y no donde acaba su relleno: si no, los últimos
        // 20 dp se veían sin fundir.
        assertTrue(vista.contains("protected boolean isPaddingOffsetRequired()"));
        assertTrue(vista.contains("return getPaddingRight();"));
        // La marca del lanzamiento se borra al acabar el gesto y no al empezarlo: con elementos
        // pulsables el primer toque no llega al carrusel, y con la marca de un lanzamiento
        // anterior puesta ya no encajaba al soltarlo despacio.
        String toque = vista.substring(vista.indexOf("public boolean onTouchEvent("), vista.indexOf("private void encajar("));
        assertTrue(toque.contains("if (!lanzado) encajar(0);"));
        assertTrue(toque.indexOf("lanzado = false;") > toque.indexOf("if (!lanzado) encajar(0);"));
        assertFalse(toque.contains("ACTION_DOWN"));
    }
}
