package es.pmdm.gymprofit.ui.cristal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

// ============================================================
// CristalTest — GP-285, DEC-070: lo que el sistema Liquid Glass promete, leído de res/ y del código.
//
//   · Las medidas del diseño (documentacion/diseno/2026-11-25-liquid-glass/README.md) son las
//     de dimens.xml.
//   · El cristal vivo va FUERA de su lienzo (dentro se dibujaría a sí mismo), y el contenido y
//     el velo, dentro.
//   · El cristal vivo no crea su efecto al dibujar: la lente de la barra anterior creaba un
//     RenderEffect por fotograma y por eso se retiró (18-09-2026).
//   · La tarjeta de siempre no cambia: las pantallas sin fondo ambiental siguen con ella.
//   · Inicio va entero en cristal: tema de cristal en la raíz y ninguna tarjeta opaca.
// ============================================================
public class CristalTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path JAVA = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit");
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";

    @Test
    public void las_medidas_son_las_del_diseno() throws IOException {
        String d = leer(RES.resolve("values/dimens.xml"));
        String[][] medidas = {
                {"radio_cristal_heroe", "30dp"}, {"radio_cristal", "26dp"}, {"radio_cristal_tile", "22dp"},
                {"cristal_borde", "1dp"}, {"cristal_desenfoque", "22dp"},
                {"barra_alto", "64dp"}, {"barra_margen_lado", "16dp"}, {"barra_margen_abajo", "26dp"},
                {"barra_hueco_mas", "10dp"}, {"barra_mas_radio", "32dp"},
        };
        for (String[] m : medidas) {
            assertTrue(m[0] + " tiene que valer " + m[1],
                    d.contains("<dimen name=\"" + m[0] + "\">" + m[1] + "</dimen>"));
        }
        // La barra y el «+» miden lo mismo de alto, y toda pestaña pasa de 48 dp.
        String barra = leer(RES.resolve("layout/view_barra_navegacion.xml"));
        assertTrue(barra.contains("android:id=\"@+id/btnAcciones\"\n        android:layout_width=\"@dimen/barra_alto\"\n        android:layout_height=\"@dimen/barra_alto\""));
        assertTrue(leer(RES.resolve("layout/view_pestana.xml")).contains("android:layout_height=\"match_parent\""));
        assertEquals(1.8f, CristalVivo.SATURACION, 0.0001f);
    }

    @Test
    public void el_cristal_vivo_va_fuera_de_su_lienzo() throws Exception {
        Document doc = xml(RES.resolve("layout/activity_main.xml"));
        NodeList lienzos = doc.getElementsByTagName("es.pmdm.gymprofit.ui.cristal.LienzoCristal");
        assertEquals("un solo lienzo en la pantalla principal", 1, lienzos.getLength());
        Element lienzo = (Element) lienzos.item(0);
        assertEquals("@id/lienzoCristal", lienzo.getAttributeNS(ANDROID, "id"));

        List<String> dentro = new ArrayList<>();
        recoger(lienzo, dentro);
        // Lo que se ve a través del cristal: el fondo, las pestañas y el velo del «+».
        assertTrue(dentro.contains("@+id/fondoAmbiental"));
        assertTrue(dentro.contains("@+id/pager"));
        assertTrue(dentro.contains("@+id/veloAcciones"));
        // Y fuera, lo que es cristal vivo: la hoja, la barra de la sesión y la barra.
        for (String tag : new String[]{ "es.pmdm.gymprofit.ui.widget.BarraNavegacion", "include" }) {
            NodeList hijos = lienzo.getElementsByTagName(tag);
            assertEquals("no puede haber <" + tag + "> dentro del lienzo", 0, hijos.getLength());
        }
        assertEquals(0, lienzo.getElementsByTagName("es.pmdm.gymprofit.ui.cristal.CristalVivo").getLength());
        // La raíz no recorta: la sombra de la barra se pinta fuera de ella.
        assertEquals("false", doc.getDocumentElement().getAttributeNS(ANDROID, "clipChildren"));
        // Las tres piezas de cristal vivo lo son de verdad.
        assertTrue(leer(RES.resolve("layout/view_barra_navegacion.xml")).contains("<es.pmdm.gymprofit.ui.cristal.CristalVivo\n        android:id=\"@+id/capsulaBarra\""));
        assertTrue(leer(RES.resolve("layout/view_barra_sesion.xml")).contains("<es.pmdm.gymprofit.ui.cristal.CristalVivo "));
        assertTrue(leer(RES.resolve("layout/view_acciones_rapidas.xml")).contains("<es.pmdm.gymprofit.ui.cristal.CristalVivo\n"));
    }

    @Test
    public void el_cristal_vivo_no_crea_su_efecto_al_dibujar() throws IOException {
        String c = leer(JAVA.resolve("ui/cristal/CristalVivo.java"));
        String dibujar = cuerpo(c, "protected void onDraw(");
        String pintar = cuerpo(c, "void pintar(");
        String antesDeDibujar = cuerpo(c, "public boolean onPreDraw(");
        for (String m : new String[]{ dibujar, pintar, antesDeDibujar }) {
            assertFalse("un efecto creado por fotograma", m.contains("RenderEffect.create"));
            assertFalse("un efecto puesto por fotograma", m.contains("setRenderEffect"));
            assertFalse("un shader creado por fotograma", m.contains("new RuntimeShader"));
            assertFalse("el efecto se prepara al cambiar de tamaño o de nivel", m.contains("prepararEfecto"));
        }
        // El efecto se pone en un solo sitio.
        assertEquals(1, veces(c, "setRenderEffect("));
        assertTrue(cuerpo(c, "void preparar(").contains("nodo.setRenderEffect(efecto)"));
        // Y el lienzo no se invalida solo: graba cuando Android le pide dibujar.
        String lienzo = leer(JAVA.resolve("ui/cristal/LienzoCristal.java"));
        assertFalse(cuerpo(lienzo, "protected void dispatchDraw(").contains("invalidate"));
    }

    @Test
    public void la_tarjeta_de_siempre_no_cambia() throws IOException {
        String temas = leer(RES.resolve("values/themes.xml"));
        String tarjeta = estilo(temas, "Widget.GymProFit.Card");
        assertTrue(tarjeta.contains("<item name=\"cardBackgroundColor\">?attr/colorSurface</item>"));
        assertTrue(tarjeta.contains("<item name=\"cardCornerRadius\">@dimen/radio_tarjeta</item>"));
        // El cristal es un estilo aparte, transparente: el cristal lo pinta su primer hijo.
        String cristal = estilo(leer(RES.resolve("values/estilos_cristal.xml")), "Widget.GymProFit.Card.Cristal");
        assertTrue(cristal.contains("<item name=\"cardBackgroundColor\">@android:color/transparent</item>"));
        assertTrue(cristal.contains("<item name=\"cardCornerRadius\">@dimen/radio_cristal</item>"));
    }

    @Test
    public void inicio_va_entero_en_cristal() throws Exception {
        Path inicio = RES.resolve("layout/fragment_inicio.xml");
        Document doc = xml(inicio);
        assertEquals("@style/ThemeOverlay.GymProFit.Cristal", doc.getDocumentElement().getAttributeNS(ANDROID, "theme"));
        assertEquals("sin fondo propio: detrás va el fondo ambiental",
                "", doc.getDocumentElement().getAttributeNS(ANDROID, "background"));
        for (String layout : new String[]{ "fragment_inicio", "item_tu_dia", "item_ultimo_entreno" }) {
            Document d = xml(RES.resolve("layout/" + layout + ".xml"));
            List<Element> tarjetas = new ArrayList<>();
            NodeList todas = d.getElementsByTagName("com.google.android.material.card.MaterialCardView");
            for (int i = 0; i < todas.getLength(); i++) tarjetas.add((Element) todas.item(i));
            assertFalse(layout + " no tiene tarjetas", tarjetas.isEmpty());
            for (Element t : tarjetas) {
                String id = t.getAttributeNS(ANDROID, "id");
                String estilo = t.getAttribute("style");
                assertTrue(layout + " " + id + " tiene que ser de cristal y es " + estilo,
                        estilo.startsWith("@style/Widget.GymProFit.Card.Cristal"));
                // El cristal lo pinta el primer hijo, con el dibujo de su mismo radio.
                Element hijo = primerHijo(t);
                String fondo = hijo == null ? "" : hijo.getAttributeNS(ANDROID, "background");
                String esperado = estilo.endsWith(".Heroe") ? "@drawable/bg_cristal_heroe"
                        : estilo.endsWith(".Tile") ? "@drawable/bg_cristal_tile"
                        : estilo.endsWith(".Oro") ? "@drawable/bg_cristal_oro" : "@drawable/bg_cristal";
                assertEquals(layout + " " + id, esperado, fondo);
            }
        }
        // Los enlaces de sección van sobre el fondo: su naranja es el medido ahí.
        String texto = leer(inicio);
        assertFalse(texto.contains("style=\"@style/Widget.GymProFit.CabeceraSeccion.Enlace\""));
        // «Tu día» son tres tiles a lo ancho, no un carrusel.
        Element fila = porId(doc, "@+id/filaTuDia");
        assertEquals("LinearLayout", fila.getTagName());
        assertEquals(3, fila.getElementsByTagName("include").getLength());
    }

    // Quien cambie un token o un patrón de cristal actualiza DESIGN.md (regla de CLAUDE.md).
    @Test
    public void DESIGN_recoge_liquid_glass() throws IOException {
        String d = leer(Paths.get("..", "DESIGN.md"));
        for (String clave : new String[]{ "## Liquid Glass (GP-285, DEC-070)", "### Tres niveles del cristal vivo",
                "### Reglas de legibilidad", "NivelCristal.elegir", "ContrasteCristalTest", "gp_ambiente_base",
                "Widget.GymProFit.Card.Cristal", "ThemeOverlay.GymProFit.Cristal", "bg_lente", "radio_cristal_heroe",
                "cristal_refraccion.agsl", "VigiaCristal" }) {
            assertTrue("DESIGN.md no recoge «" + clave + "»", d.contains(clave));
        }
        // Y los valores del fondo y del texto son los de colors.xml.
        String oscuro = leer(RES.resolve("values-night/colors.xml"));
        for (String[] par : new String[][]{ {"gp_ambiente_base", "#08090B"}, {"gp_cristal_texto_2", "#C9CDD6"},
                {"gp_cristal_acento", "#FFB27A"}, {"gp_vivo_texto", "#E4E6EB"} }) {
            assertTrue(par[0], oscuro.contains("<color name=\"" + par[0] + "\">" + par[1] + "</color>"));
            assertTrue("DESIGN.md no trae " + par[1], d.contains(par[1]));
        }
    }

    // ── Ayudas ──────────────────────────────────────────────────────────────

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static Document xml(Path p) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        return f.newDocumentBuilder().parse(p.toFile());
    }

    private static void recoger(Element e, List<String> ids) {
        String id = e.getAttributeNS(ANDROID, "id");
        if (!id.isEmpty()) ids.add(id);
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) recoger((Element) n, ids);
        }
    }

    private static Element primerHijo(Element e) {
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) return (Element) n;
        }
        return null;
    }

    private static Element porId(Document doc, String id) {
        NodeList todos = doc.getElementsByTagName("*");
        for (int i = 0; i < todos.getLength(); i++) {
            Element e = (Element) todos.item(i);
            if (id.equals(e.getAttributeNS(ANDROID, "id"))) return e;
        }
        throw new AssertionError("no hay " + id);
    }

    private static String estilo(String xml, String nombre) {
        Matcher m = Pattern.compile("<style name=\"" + Pattern.quote(nombre) + "\"[^>]*>(.*?)</style>", Pattern.DOTALL).matcher(xml);
        if (!m.find()) throw new AssertionError("no existe el estilo " + nombre);
        return m.group(1);
    }

    // El cuerpo de un método, de su firma a la llave que lo cierra.
    private static String cuerpo(String codigo, String firma) {
        int i = codigo.indexOf(firma);
        if (i < 0) throw new AssertionError("no existe " + firma);
        int abre = codigo.indexOf('{', i), nivel = 0;
        for (int j = abre; j < codigo.length(); j++) {
            char c = codigo.charAt(j);
            if (c == '{') nivel++;
            if (c == '}' && --nivel == 0) return codigo.substring(abre, j + 1);
        }
        throw new AssertionError("sin cierre: " + firma);
    }

    private static int veces(String texto, String trozo) {
        int n = 0;
        for (int i = texto.indexOf(trozo); i >= 0; i = texto.indexOf(trozo, i + 1)) n++;
        return n;
    }
}
