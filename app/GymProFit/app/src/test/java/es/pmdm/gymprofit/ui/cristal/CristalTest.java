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
                {"barra_hueco_mas", "10dp"},
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

    // El «+» de la barra es el botón de marca, el mismo que el principal (DEC-070): no es cristal,
    // es bg_marca. Su media luna se queda en el cuarto de arriba, por encima del icono; y lo que
    // gira al abrirse es el icono, no el botón, que giraría con su media luna.
    @Test
    public void el_mas_es_el_boton_de_marca_y_solo_gira_su_icono() throws Exception {
        Element mas = null;
        NodeList marcos = xml(RES.resolve("layout/view_barra_navegacion.xml")).getElementsByTagName("FrameLayout");
        for (int i = 0; i < marcos.getLength(); i++) {
            Element e = (Element) marcos.item(i);
            if ("@+id/btnAcciones".equals(e.getAttributeNS(ANDROID, "id"))) mas = e;
        }
        assertTrue("el «+» tiene que ser un FrameLayout con el botón de marca de fondo", mas != null);
        String app = "http://schemas.android.com/apk/res-auto";
        assertEquals("@drawable/bg_marca", mas.getAttributeNS(ANDROID, "background"));
        assertEquals("@dimen/barra_alto", mas.getAttributeNS(ANDROID, "layout_width"));
        assertEquals("@dimen/barra_alto", mas.getAttributeNS(ANDROID, "layout_height"));
        // Sigue siendo pulsable y con nombre, y su icono no se lee aparte.
        assertEquals("true", mas.getAttributeNS(ANDROID, "clickable"));
        assertEquals("true", mas.getAttributeNS(ANDROID, "focusable"));
        assertEquals("@string/acciones_rapidas", mas.getAttributeNS(ANDROID, "contentDescription"));
        Element icono = (Element) mas.getElementsByTagName("ImageView").item(0);
        assertEquals("@+id/ivAcciones", icono.getAttributeNS(ANDROID, "id"));
        assertEquals("no", icono.getAttributeNS(ANDROID, "importantForAccessibility"));
        assertEquals("@color/gp_marca_texto", icono.getAttributeNS(app, "tint"));
        assertEquals("@dimen/barra_mas_icono_margen", icono.getAttributeNS(ANDROID, "padding"));

        // La media luna acaba antes de donde empieza el icono, y va por dentro del borde.
        String d = leer(RES.resolve("values/dimens.xml"));
        float alto = dp(d, "barra_alto"), margen = dp(d, "barra_mas_icono_margen");
        assertTrue("el reflejo (" + alto * Cristal.REFLEJO_ALTO + " dp) pisa el icono (a " + margen + " dp)",
                alto * Cristal.REFLEJO_ALTO < margen);
        assertTrue("el reflejo va por dentro del borde", dp(d, "marca_reflejo_dentro") > dp(d, "marca_borde"));

        String barra = leer(JAVA.resolve("ui/widget/BarraNavegacion.java"));
        assertTrue(barra.contains("ivAcciones.animate().rotation("));
        assertFalse("el «+» no gira", barra.contains("btnAcciones.animate()"));
        assertTrue("el «+» se hunde como el principal", barra.contains("Hundir.alPulsar(btnAcciones)"));
    }

    // El botón de marca, de abajo arriba: la sombra por fuera, el relleno macizo, el brillo, el
    // borde y la media luna; pulsado, el brillo y la media luna otra vez encima. Es el mismo
    // dibujo para el principal (Widget.GymProFit.Boton.Marca) y para el «+»: una sola fuente.
    @Test
    public void el_principal_y_el_mas_comparten_el_boton_de_marca() throws IOException {
        String cristal = leer(JAVA.resolve("ui/cristal/Cristal.java"));
        // draw() solo decide si hace falta una capa (el dibujo a medias); lo pinta pintar().
        String dibujar = cuerpo(cristal, "private void pintar(");
        assertTrue("la sombra va antes que el botón",
                dibujar.indexOf("sombra.pintar(") < dibujar.indexOf("pintarMarca(canvas)"));
        String pinta = cuerpo(cristal, "private void pintarMarca(");
        String[] orden = {"(forma, relleno)", "(forma, brilloRadial)", "(bordeForma, trazo)", "(reflejoForma, reflejo)"};
        for (int i = 1; i < orden.length; i++) {
            assertTrue(orden[i - 1] + " va debajo de " + orden[i],
                    pinta.indexOf(orden[i - 1]) >= 0 && pinta.indexOf(orden[i - 1]) < pinta.indexOf(orden[i]));
        }
        String estilos = leer(RES.resolve("values/estilos_cristal.xml"));
        String marca = estilo(estilos, "Widget.GymProFit.Boton.Marca");
        assertTrue(marca.contains("<item name=\"android:background\">@drawable/bg_marca</item>"));
        assertTrue(leer(RES.resolve("drawable/bg_marca.xml")).contains("app:cristalTipo=\"marca\""));
        // El secundario es cristal estático, y en el nivel 3, la superficie opaca.
        String secundario = leer(JAVA.resolve("ui/widget/BotonCristal.java"));
        assertTrue(secundario.contains("Cristal.VIVO_OPACO : Cristal.TARJETA"));
        // Inicio usa los dos.
        String inicio = leer(RES.resolve("layout/fragment_inicio.xml"));
        assertTrue(inicio.contains("<es.pmdm.gymprofit.ui.widget.BotonMarca\n                            android:id=\"@+id/btnHoyPrincipal\""));
        assertTrue(inicio.contains("<es.pmdm.gymprofit.ui.widget.BotonCristal\n                            android:id=\"@+id/btnHoySecundario\""));
    }

    // Un dimen en dp de dimens.xml.
    private static float dp(String dimens, String nombre) {
        Matcher m = Pattern.compile("<dimen name=\"" + nombre + "\">([0-9.]+)dp</dimen>").matcher(dimens);
        assertTrue("falta el dimen " + nombre, m.find());
        return Float.parseFloat(m.group(1));
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

    // El desenfoque de la barra es el de la maqueta, y va en dp: se convierte a px con la densidad
    // de cada pantalla (en px fijos, una pantalla densa lo vería a medio desenfocar). Nada de
    // esto son cifras escritas aquí: la desviación y la saturación se leen de la maqueta aprobada.
    @Test
    public void el_desenfoque_va_en_dp_y_es_el_de_la_maqueta() throws IOException {
        String maqueta = leer(Paths.get("..", "..", "..", "documentacion", "diseno", "2026-11-25-liquid-glass", "inicio.dc.html"));
        Matcher css = Pattern.compile("blur\\((\\d+(?:\\.\\d+)?)px\\) saturate\\((\\d+)%\\)").matcher(maqueta);
        assertTrue("la maqueta de Inicio ya no trae el desenfoque de la barra", css.find());
        float sigmaMaqueta = Float.parseFloat(css.group(1));
        assertEquals(Integer.parseInt(css.group(2)) / 100f, CristalVivo.SATURACION, 0.0001f);

        Matcher dimen = Pattern.compile("<dimen name=\"cristal_desenfoque\">(\\d+(?:\\.\\d+)?)([a-z]+)</dimen>")
                .matcher(leer(RES.resolve("values/dimens.xml")));
        assertTrue(dimen.find());
        assertEquals("el desenfoque va en dp, no en " + dimen.group(2), "dp", dimen.group(2));
        assertEquals(sigmaMaqueta, Float.parseFloat(dimen.group(1)), 0.0001f);

        // Quien lo usa lo lee como medida (ya en px de esta pantalla) y no reduce el lienzo antes
        // de desenfocarlo: no hay radio que compensar.
        String c = leer(JAVA.resolve("ui/cristal/CristalVivo.java"));
        assertTrue(c.contains("sigma = getResources().getDimension(R.dimen.cristal_desenfoque)"));
        assertTrue(cuerpo(c, "void preparar(").contains("radioDesenfoque(sigmaPx)"));
        assertFalse(c.contains("createScaledBitmap") || c.contains("canvas.scale(1f /"));

        // Y el «radio» que se le pide a Android da esa desviación en cualquier densidad: Android
        // saca σ = radio · 0,57735 + 0,5.
        for (float densidad : new float[]{ 1f, 1.5f, 2f, 2.625f, 2.75f, 3f, 4f }) {
            float sigmaPx = sigmaMaqueta * densidad;
            assertEquals("densidad " + densidad, sigmaPx, CristalVivo.radioDesenfoque(sigmaPx) * 0.57735f + 0.5f, 0.01f);
        }
    }

    // El nivel 3 no desenfoca: si su superficie dejara pasar algo, lo de detrás se vería nítido
    // a través (al 98 % se entreveía el texto: los «píxeles detrás» de Android 11 o menos).
    @Test
    public void el_nivel_3_es_opaco_del_todo() throws IOException {
        for (String carpeta : new String[]{ "values", "values-night" }) {
            String colores = leer(RES.resolve(carpeta + "/colors.xml"));
            for (String token : new String[]{ "gp_vivo_opaco_alto", "gp_vivo_opaco_bajo" }) {
                Matcher m = Pattern.compile("<color name=\"" + token + "\">#([0-9A-Fa-f]+)</color>").matcher(colores);
                assertTrue(carpeta + ": falta " + token, m.find());
                String hex = m.group(1);
                assertTrue(carpeta + " " + token + " deja pasar lo de detrás: #" + hex,
                        hex.length() == 6 || hex.toUpperCase().startsWith("FF"));
            }
        }
    }

    // Cada ejercicio del carrusel de «Hoy toca» es una parada de TalkBack: dentro de una tarjeta
    // pulsable, sin esto, no se recorrían uno a uno. Los entrenos ya lo son (tarjetas pulsables
    // con su descripción).
    @Test
    public void talkback_recorre_los_elementos_de_los_carruseles() throws IOException {
        String hoy = leer(JAVA.resolve("ui/inicio/InicioHoyDetalle.java"));
        assertTrue(hoy.contains("ViewCompat.setScreenReaderFocusable(item, true)"));
        String entrenos = leer(JAVA.resolve("ui/inicio/InicioEntrenos.java"));
        assertTrue(entrenos.contains("tarjeta.setContentDescription("));
        assertTrue(leer(RES.resolve("layout/item_ultimo_entreno.xml")).contains("android:clickable=\"true\""));
    }

    // La sombra del cristal vivo cae fuera de su vista. Antes de Android 8, recortar «todo menos
    // la forma» parte de la caja de la vista y la dejaba cortada en recto (dos orejas grises en
    // las esquinas de la barra, visto en Android 7). Ahí no se recorta: el cristal es opaco.
    @Test
    public void la_sombra_no_se_recorta_donde_el_recorte_la_corta() throws IOException {
        String c = leer(JAVA.resolve("ui/cristal/CristalVivo.java"));
        String sombra = cuerpo(c, "private void pintarSombra(");
        assertFalse("un recorte por diferencia parte de la caja de la vista", c.contains("Region.Op.DIFFERENCE"));
        assertTrue("desde Android 8, solo por fuera de la forma",
                sombra.contains("Build.VERSION.SDK_INT >= Build.VERSION_CODES.O") && sombra.contains("canvas.clipOutPath(forma)"));
        // Sin recorte la sombra queda también debajo: solo vale si ahí el cristal lo tapa.
        for (int api = 24; api < 26; api++) {
            assertEquals("API " + api, NivelCristal.OPACO, NivelCristal.elegir(api, false, NivelCristal.REFRACCION));
        }
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
        for (String layout : new String[]{ "fragment_inicio", "item_tu_dia", "item_ultimo_entreno", "view_record_inicio" }) {
            Document d = xml(RES.resolve("layout/" + layout + ".xml"));
            List<Element> tarjetas = new ArrayList<>();
            // Toda tarjeta es TarjetaCristal (la MaterialCardView que en claro pinta su sombra por
            // fuera): ninguna MaterialCardView a secas.
            assertEquals(layout + " tiene tarjetas que no son TarjetaCristal", 0,
                    d.getElementsByTagName("com.google.android.material.card.MaterialCardView").getLength());
            NodeList todas = d.getElementsByTagName("es.pmdm.gymprofit.ui.cristal.TarjetaCristal");
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
                // Una tarjeta dentro de otra (la fila del récord) va sin sombra: caería sobre el
                // cristal de la de fuera.
                // La del récord va en su propio layout, incluido dentro de «Tu progreso».
                boolean dentroDeOtra = layout.equals("view_record_inicio");
                for (Node p = t.getParentNode(); p instanceof Element; p = p.getParentNode()) {
                    if (((Element) p).getTagName().endsWith("TarjetaCristal")) dentroDeOtra = true;
                }
                assertEquals(layout + " " + id + ", sombra", dentroDeOtra ? "false" : "",
                        t.getAttributeNS("http://schemas.android.com/apk/res-auto", "cristalSombra"));
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
                "cristal_refraccion.agsl", "VigiaCristal",
                "### Bordes del sistema", "BordesSistema", "borde_sistema_desvanecido", "barra_margen_boton",
                "EncajeHeroe", "hoy_ejercicio_icono_cede" }) {
            assertTrue("DESIGN.md no recoge «" + clave + "»", d.contains(clave));
        }
        // Lo retirado no se queda en el sistema: el halo del botón principal y el token viejo del margen.
        for (String fuera : new String[]{ "gp_boton_halo", "barra_aire_contenido" }) {
            assertFalse("DESIGN.md sigue nombrando «" + fuera + "»", d.contains(fuera));
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
