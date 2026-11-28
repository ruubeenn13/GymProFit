package es.pmdm.gymprofit.ui.cristal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

// ============================================================
// BordesSistemaTest — GP-285 (remate): el contenido se desvanece antes de llegar a los iconos
// del sistema, y lo último de cada pestaña acaba a un margen de la barra flotante que sale de
// un token.
//
//   · Los dos bordes existen en los tres niveles del cristal (no dependen del nivel) y en
//     todas las versiones de Android: arriba tapan la barra de estado entera y abajo, con
//     botones, la franja de los botones entera.
//   · Con gestos el de abajo queda corto: no llega a la cápsula. El hueco del asa, macizo.
//   · Van dentro del lienzo y debajo del velo del «+»: el velo los cubre con su mismo tono y
//     el cristal vivo los ve.
//   · El contraste forzado del sistema se apaga: pintaba su propia franja encima del velo.
//   · El margen de la barra flotante es barra_margen_boton, no un número suelto.
// ============================================================
public class BordesSistemaTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path JAVA = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit");
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";

    // Un móvil de 420 ppp: barra de estado de 24 dp, botones de 48, gestos de 16, la barra
    // flotante de 64 a 26 del hueco y 24 de desvanecido arriba.
    private static final int ESTADO = 63, BOTONES = 126, GESTOS = 42, MARGEN = 68, BARRA = 168, DESVANECIDO = 63;
    // Una versión por nivel del cristal (1, 1, 2, 2, 3, 3) y las dos de antes del contraste forzado.
    private static final int[] VERSIONES = {36, 33, 32, 31, 30, 28, 26, 24};

    @Test
    public void los_bordes_existen_en_los_tres_niveles() throws IOException {
        for (int api : VERSIONES) {
            boolean botones = BordesSistema.hayBotones(api, BOTONES, BOTONES);
            assertTrue("API " + api + ": con botones", botones);
            // Arriba: la barra de estado entera, maciza, y el tramo en que el contenido se va.
            assertEquals(ESTADO + DESVANECIDO, BordesSistema.altoSuperior(ESTADO, DESVANECIDO));
            // Abajo: la franja de los botones entera, maciza, y el degradado por encima de ella.
            int alto = BordesSistema.altoInferior(BOTONES, botones, MARGEN, BARRA);
            int macizo = BordesSistema.macizoInferior(BOTONES);
            assertEquals("API " + api + ": los botones del sistema van sobre el fondo base", BOTONES, macizo);
            assertTrue("API " + api + ": el contenido se desvanece antes de los botones", alto > macizo + MARGEN);
        }
        // No dependen del nivel del cristal: ni la cuenta ni quien los coloca lo miran.
        assertFalse(leer(JAVA.resolve("ui/cristal/BordesSistema.java")).contains("NivelCristal"));
        assertFalse(leer(JAVA.resolve("ui/cristal/BordeSistema.java")).contains("NivelCristal"));
        String main = leer(JAVA.resolve("ui/activities/MainActivity.java"));
        String insets = cuerpo(main, "private void aplicarInsets(");
        assertFalse(insets.contains("NivelCristal") || insets.contains("nivel()"));
        assertTrue(insets.contains("BordesSistema.altoSuperior("));
        assertTrue(insets.contains("BordesSistema.altoInferior("));
        assertTrue(insets.contains("BordesSistema.macizoInferior("));
    }

    @Test
    public void con_gestos_el_borde_de_abajo_queda_corto() {
        for (int api : new int[]{36, 33, 32, 31, 30, 29}) {
            // Con gestos el hueco no es tocable: no hay botones que proteger.
            boolean botones = BordesSistema.hayBotones(api, GESTOS, 0);
            assertFalse("API " + api, botones);
            int alto = BordesSistema.altoInferior(GESTOS, botones, MARGEN, BARRA);
            assertEquals("no llega a la cápsula", GESTOS + MARGEN, alto);
            // El hueco de gestos, macizo: el asa del sistema nunca tiene texto detrás (con el
            // degradado hasta el canto, el asa tachaba lo que pasaba por ahí a 2,0:1).
            assertEquals("el asa va sobre el fondo base", GESTOS, BordesSistema.macizoInferior(GESTOS));
            assertTrue(alto < BordesSistema.altoInferior(GESTOS, true, MARGEN, BARRA));
        }
        // Antes de Android 10 no hay gestos: un hueco de navegación son botones, diga lo que diga lo tocable.
        assertTrue(BordesSistema.hayBotones(28, BOTONES, 0));
        assertTrue(BordesSistema.hayBotones(24, BOTONES, 0));
        // Sin barra de navegación no hay nada que proteger.
        assertFalse(BordesSistema.hayBotones(36, 0, 0));
        assertFalse(BordesSistema.hayBotones(24, 0, 0));
    }

    @Test
    public void el_degradado_va_de_transparente_al_fondo_base() {
        // Fila a fila, de la punta transparente a la maciza: 278 px con 126 de franja (botones),
        // 110 con 42 (gestos) y 126 con 63 (la barra de estado).
        for (int[] caso : new int[][]{ {278, 126}, {110, 42}, {126, 63}, {110, 0} }) {
            int alto = caso[0], macizo = caso[1], rampa = alto - macizo;
            int[] alfas = BordesSistema.alfas(alto, macizo);
            assertEquals("una fila por píxel", alto, alfas.length);
            assertEquals("empieza transparente", 0, alfas[0]);
            for (int y = 1; y < alto; y++) {
                assertTrue("el alfa no baja (y=" + y + ")", alfas[y] >= alfas[y - 1]);
                assertTrue("sin escalones: de fila a fila, 8 niveles como mucho (y=" + y + ")",
                        alfas[y] - alfas[y - 1] <= 8 || y == rampa);
            }
            // El tramo macizo lo es de verdad: opaco desde donde empieza hasta el canto.
            for (int y = rampa; y < alto; y++) assertEquals("y=" + y, 255, alfas[y]);
            // Despacio junto al contenido y deprisa junto a la barra: a media rampa, un cuarto.
            assertEquals(64, alfas[rampa / 2], 3);
            assertTrue("la última fila de la rampa ya es casi el fondo", alfas[rampa - 1] > 240);
        }
        // Un macizo mayor que el alto no desborda.
        assertEquals(10, BordesSistema.alfas(10, 40).length);
        assertEquals(255, BordesSistema.alfas(10, 40)[0]);
        assertEquals(0, BordesSistema.alfas(0, 0).length);
    }

    @Test
    public void sin_contraste_del_sistema_la_barra_lleva_fondo_propio() throws IOException {
        int velo = 0x9E000000;
        // El fondo de los botones es el tramo macizo del borde, en todas las versiones: va en la
        // ventana transparente para que el velo del «+» lo cubra igual que al resto.
        for (int api : VERSIONES) {
            assertEquals("API " + api + " en oscuro", 0, BordesSistema.colorBarraNavegacion(api, false, velo));
        }
        for (int api : new int[]{36, 33, 31, 30, 29, 28, 26}) {
            assertEquals("API " + api + " en claro", 0, BordesSistema.colorBarraNavegacion(api, true, velo));
        }
        // Android 7: los botones son siempre blancos; en claro, un velo oscuro detrás.
        assertEquals(velo, BordesSistema.colorBarraNavegacion(25, true, velo));
        assertEquals(velo, BordesSistema.colorBarraNavegacion(24, true, velo));

        // Desde Android 10 el sistema pinta su franja de contraste bajo los tres botones, por
        // encima del velo (en claro quedaba más clara que la pantalla): se apaga, el fondo es el nuestro.
        // Una sola fuente para toda pantalla Liquid Glass (VentanaCristal): la navegación
        // principal y, desde el lote 2, la Sesión en vivo.
        String barras = cuerpo(leer(JAVA.resolve("ui/cristal/VentanaCristal.java")),
                "public static void barrasTransparentes(");
        assertTrue(barras.contains("setNavigationBarContrastEnforced(false)"));
        assertTrue(barras.contains("BordesSistema.colorBarraNavegacion("));
        for (String pantalla : new String[]{ "MainActivity", "SesionEnVivoActivity" }) {
            assertTrue(pantalla, leer(JAVA.resolve("ui/activities/" + pantalla + ".java"))
                    .contains("VentanaCristal.barrasTransparentes(this)"));
        }
    }

    @Test
    public void el_velo_cubre_los_bordes_y_el_cristal_los_ve() throws Exception {
        Document doc = xml(RES.resolve("layout/activity_main.xml"));
        Element lienzo = (Element) doc.getElementsByTagName("es.pmdm.gymprofit.ui.cristal.LienzoCristal").item(0);
        List<String> hijos = new ArrayList<>();
        for (Node n = lienzo.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) hijos.add(((Element) n).getAttributeNS(ANDROID, "id"));
        }
        int pager = hijos.indexOf("@+id/contenedorPager");
        int arriba = hijos.indexOf("@+id/bordeSuperior");
        int abajo = hijos.indexOf("@+id/bordeInferior");
        int velo = hijos.indexOf("@+id/veloAcciones");
        assertTrue("los dos bordes van dentro del lienzo", arriba >= 0 && abajo >= 0);
        assertTrue("encima del contenido", arriba > pager && abajo > pager);
        assertTrue("y debajo del velo, que los cubre con su mismo tono", arriba < velo && abajo < velo);
        // No son contenido: TalkBack no se para en ellos.
        for (Node n = lienzo.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element)) continue;
            Element e = (Element) n;
            if (e.getAttributeNS(ANDROID, "id").startsWith("@+id/borde")) {
                assertEquals("no", e.getAttributeNS(ANDROID, "importantForAccessibility"));
                assertEquals("", e.getAttributeNS(ANDROID, "clickable"));
            }
        }
        // La rampa no se monta al dibujar: una imagen de 1 px de ancho, hecha al cambiar de
        // tamaño, de color o de tramo macizo, que al pintar solo se estira.
        String borde = leer(JAVA.resolve("ui/cristal/BordeSistema.java"));
        String dibujar = cuerpo(borde, "public void draw(");
        assertFalse(dibujar.contains("createBitmap") || dibujar.contains("alfas("));
        assertTrue(dibujar.contains("canvas.drawBitmap("));
        // Y es una imagen, no un degradado de la GPU: el de ocho paradas con tramado tiraba el
        // emulador del CI (SwiftShader) al pintarse Inicio.
        assertFalse(borde.contains("new LinearGradient") || borde.contains("import android.graphics.LinearGradient"));
    }

    @Test
    public void el_margen_de_la_barra_flotante_sale_del_token() throws IOException {
        String dimens = leer(RES.resolve("values/dimens.xml"));
        assertTrue(dimens.contains("<dimen name=\"barra_margen_boton\">16dp</dimen>"));
        assertTrue(dimens.contains("<dimen name=\"borde_sistema_desvanecido\">24dp</dimen>"));
        // Lo último del scroll acaba a ese margen del pie: lo pone MainActivity con el token.
        String hueco = cuerpo(leer(JAVA.resolve("ui/activities/MainActivity.java")), "private void aplicarHuecoInferior(");
        assertTrue(hueco.contains("pie.getHeight() + getResources().getDimensionPixelSize(R.dimen.barra_margen_boton)"));
        // Y el botón de la tarjeta héroe, en reposo, se mide contra el mismo.
        String encaje = cuerpo(leer(JAVA.resolve("ui/fragments/InicioFragment.java")), "private void encajarHeroe(");
        assertTrue(encaje.contains("R.dimen.barra_margen_boton"));
        assertTrue(encaje.contains("EncajeHeroe.cede("));
        // El token viejo no queda en ningún sitio.
        try (java.util.stream.Stream<Path> todos = Files.walk(Paths.get("src", "main"))) {
            for (Path p : (Iterable<Path>) todos.filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".xml") || f.toString().endsWith(".java"))::iterator) {
                assertFalse(p + " usa el token retirado", leer(p).contains("barra_aire_contenido"));
            }
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
}
