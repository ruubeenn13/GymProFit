package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// TarjetaEstadosTest — los estados de Widget.GymProFit.Card (GP-227).
//
// La tarjeta llevaba de capa gp_primary_container opaco: con el foco del teclado tapaba el texto
// (3,55:1 y 2,26:1 medidos en Historial). Ahora, como el selector de opciones (GP-224): pulsada,
// una capa del 10 % de gp_on_surface; con foco, ninguna capa y un anillo de 2 dp en gp_on_surface
// (FocoVisible, el mismo para todo lo pulsable). Aquí se fija el estilo y se mide, en claro y en oscuro, que el texto sobre la
// tarjeta con foco y pulsada pasa 4,5:1 (salvo el naranja pulsado
// en oscuro, 4,40:1, acotado aparte) y que el anillo pasa 3:1 contra la tarjeta y contra el fondo.
// ============================================================
public class TarjetaEstadosTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path JAVA = Paths.get("src", "main", "java");
    private static final String[] TEMAS = { "values", "values-night" };
    private static final double CAPA = 0.10;

    @Test
    public void elEstiloLlevaLaCapaDelDiezPorCiento() throws IOException {
        String temas = leer(RES.resolve("values/themes.xml"));
        Matcher m = Pattern.compile("<style name=\"Widget.GymProFit.Card\"[\\s\\S]*?</style>").matcher(temas);
        assertTrue("no está Widget.GymProFit.Card", m.find());
        assertTrue("la capa de la tarjeta es color/tarjeta_capa, no un color opaco",
                m.group().contains("<item name=\"rippleColor\">@color/tarjeta_capa</item>"));

        String capa = leer(RES.resolve("color/tarjeta_capa.xml"));
        assertTrue("pulsada: el 10 % de gp_on_surface", Pattern.compile(
                "<item android:state_pressed=\"true\" android:color=\"@color/gp_on_surface\" android:alpha=\"0.10\"\\s*/>")
                .matcher(capa).find());
        assertTrue("con foco no hay capa: la última opción es transparente",
                capa.contains("<item android:color=\"@android:color/transparent\" />"));
        assertTrue("ninguna capa propia para el foco", !capa.contains("state_focused"));
    }

    @Test
    public void elAnilloEsDeDosDpEnElColorDelTexto() throws IOException {
        String foco = leer(JAVA.resolve(Paths.get("es", "pmdm", "gymprofit", "ui", "widget", "FocoVisible.java")));
        assertTrue(foco.contains("R.dimen.selector_anillo_foco"));
        assertTrue("el color del texto sobre lo que hay detrás (el padre)",
                foco.contains("MaterialColors.getColor(fondo, com.google.android.material.R.attr.colorOnSurface)"));
        assertTrue(leer(RES.resolve("values/dimens.xml")).contains("<dimen name=\"selector_anillo_foco\">2dp</dimen>"));
        String app = leer(JAVA.resolve(Paths.get("es", "pmdm", "gymprofit", "GymProFitApp.java")));
        assertTrue("se instala en todas las pantallas", app.contains("FocoVisible.instalar(a)"));
    }

    @Test
    public void lasFilasYLosIconosSueltosTienenLosMismosEstados() throws IOException {
        // El resaltado gris de Material en las filas daba 1,18:1 en claro y 1,45:1 en oscuro
        // (revisor, GP-228): el fondo pulsable del tema es el mismo de la tarjeta.
        String temas = leer(RES.resolve("values/themes.xml"));
        assertTrue(temas.contains("<item name=\"selectableItemBackground\">@drawable/fondo_pulsable</item>"));
        assertTrue(temas.contains("<item name=\"selectableItemBackgroundBorderless\">@drawable/fondo_pulsable_redondo</item>"));
        assertTrue(temas.contains("<item name=\"actionBarItemBackground\">@drawable/fondo_pulsable_redondo</item>"));
        for (String d : new String[]{ "fondo_pulsable", "fondo_pulsable_redondo", "bg_circulo_superficie" }) {
            String xml = leer(RES.resolve("drawable/" + d + ".xml"));
            assertTrue(d + ": pulsado, la capa de la tarjeta", xml.contains("android:color=\"@color/tarjeta_capa\""));
            assertTrue(d + ": el anillo lo pone FocoVisible, no el fondo (saldría dos veces)",
                    !xml.contains("state_focused"));
        }
        String main = leer(JAVA.resolve(Paths.get("es", "pmdm", "gymprofit", "ui", "activities", "MainActivity.java")));
        assertTrue("la página del pager no se vela con el foco", main.contains("setDefaultFocusHighlightEnabled(false)"));
        assertTrue("ni es una parada del tabulador", main.contains("pager.getChildAt(0).setFocusable(false);"));
        assertTrue("las pestañas que no se ven no toman el foco en su raíz",
                main.contains("v.setFocusable(esta && Boolean.TRUE.equals(v.getTag(R.id.tag_enfocable_original)));"));
    }

    @Test
    public void elAnilloLlegaATodoLoPulsable() throws IOException {
        // Los botones de Material solo cambiaban su capa (1,13 a 1,19:1); los redondos y las filas sin
        // fondo pulsable, el gris del sistema (1,17 a 1,47:1); un scroll velaba la pantalla entera
        // (revisor, grupo B). FocoVisible decide para cualquier vista.
        String foco = leer(JAVA.resolve(Paths.get("es", "pmdm", "gymprofit", "ui", "widget", "FocoVisible.java")));
        // Por fuera y separado del borde: por dentro pisaba el texto de las filas sin aire, quedaba a
        // 1,4:1 dentro de un botón relleno y no se distinguía en una tarjeta oscura (segunda pasada).
        assertTrue("por fuera, a 2 dp del borde", foco.contains("return porDentro(v) ? -Math.round(HUECO_DP * d) : Math.round(HUECO_DP * d) + ancho;")
                && foco.contains("private static final int HUECO_DP = 2;"));
        assertTrue("en la capa de la ventana, que no recorta ningún padre", foco.contains("decor.getOverlay().add(capa);"));
        assertTrue("sigue a la vista", foco.contains("public boolean onPreDraw()"));
        // Tercera pasada: la flecha girada de Estadísticas tenía el anillo desplazado su tamaño; los
        // iconos sueltos lo sacaban de la pantalla; un carrusel a sangre dejaba un lado fuera.
        assertTrue("sin las transformaciones de la vista", foco.contains("decor.offsetDescendantRectToMyCoords(v, enDecor);")
                && !foco.contains(".getLocationInWindow("));
        assertTrue("por dentro en un icono suelto sin relleno; no en el avatar ni en el «+» relleno",
                foco.contains("return v instanceof ImageView && !(v instanceof ShapeableImageView) && sinRelleno(v.getBackground());")
                        && foco.contains("if (r.getId(i) != android.R.id.mask) return false;"));
        // Cuarta pasada: alrededor de una fila medio escondida el anillo se pintaba sobre la cabecera;
        // la última tarjeta de un carrusel a sangre perdía un lado; el tabulador saltaba a «Volver».
        assertTrue("recortado a lo que se ve de sus listas, solo en el eje en que se desplaza cada una",
                foco.contains("capa.recortarA(visible(v));") && foco.contains("c.clipRect(zona);")
                        && foco.contains("if (eje == EJE_X) {") && foco.contains("if (a instanceof HorizontalScrollView) return EJE_X;"));
        assertTrue("el carrusel llega hasta su relleno", foco.contains("mostrarEnCarrusel(v, m);")
                && foco.contains("h.scrollBy(r.right + fuera - der, 0);"));
        String historial = leer(JAVA.resolve(Paths.get("es", "pmdm", "gymprofit", "ui", "fragments", "HistorialFragment.java")));
        assertTrue("sin filas dispuestas fuera de la vista: el foco volvía a ellas desde «Volver» (quinta pasada)",
                !historial.contains("calculateExtraLayoutSpace("));
        assertTrue("se desplaza a la vista con el anillo entero, también por dentro, y otra vez tras el layout",
                foco.contains("int m = Math.max(0, fuera(v));") && foco.contains("v.requestRectangleOnScreen(")
                        && foco.contains("enfocada.post(() -> {"));
        // Sexta pasada: con el teclado abierto, el campo que pierde el foco pide otro layout y el scroll
        // vuelve a alinear solo la vista enfocada, sin su anillo (la valoración de Registrar sesión).
        // Se vuelve a enseñar tras cada layout mientras el anillo esté puesto.
        assertTrue("otra vez tras cada layout, no solo en el siguiente mensaje",
                foco.contains("public void onGlobalLayout()") && foco.contains("if (conAnillo != null) mostrar(conAnillo);")
                        && foco.contains("addOnGlobalLayoutListener(this)") && foco.contains("removeOnGlobalLayoutListener(this)"));
        // Y un carrusel deja sitio al anillo fuera de sus lados: sin relleno, su borde es el de la
        // primera y la última tarjeta (Inicio, sexta pasada).
        assertTrue("el recorte de un carrusel deja el sitio del anillo",
                foco.contains("recortar(new int[]{ r.left, r.top, r.right, r.bottom },") && foco.contains("eje, Math.max(0, fuera(v)));"));
        assertTrue("con el radio de la tarjeta", foco.contains("((MaterialCardView) v).getRadius()"));
        assertTrue("lo que se desplaza y los campos: sin anillo ni velo", foco.contains("v instanceof ScrollView")
                && foco.contains("v instanceof NestedScrollView") && foco.contains("v instanceof HorizontalScrollView")
                && foco.contains("v instanceof RecyclerView") && foco.contains("v instanceof EditText"));
        assertTrue("sin el resaltado del sistema", foco.contains("ahora.setDefaultFocusHighlightEnabled(false)"));
        assertTrue("con el radio del botón de Material", foco.contains("getShapeAppearanceModel()"));
        assertTrue("al tocar la pantalla se quita", foco.contains("if (tactil) quitar();"));
        assertTrue("un carrusel con algo enfocable dentro no es parada: el foco pasa a lo primero",
                foco.contains("ahora.setFocusable(false);") && foco.contains("dentro.get(0).requestFocus();"));
        String selector = leer(JAVA.resolve(Paths.get("es", "pmdm", "gymprofit", "ui", "widget", "SelectorPildora.java")));
        assertTrue("las opciones del selector llevan el suyo: no sale doble", selector.contains("o.setTag(R.id.tag_anillo_propio, Boolean.TRUE);"));
        String barra = leer(JAVA.resolve(Paths.get("es", "pmdm", "gymprofit", "ui", "widget", "BarraNavegacion.java")));
        assertTrue("la pestaña lo lleva en su píldora", barra.contains("p.setTag(R.id.tag_anillo_propio, Boolean.TRUE);"));
    }

    @Test
    public void losBloquesPulsablesSinRellenoDejanAireAlAnillo() throws IOException {
        // La fila de cada macro de Nutrición y la cabecera de «Tu programa» no tenían relleno: el anillo
        // iba a 2 dp del texto y sus esquinas cruzaban la «G» de «Gimnasio», el punto de la macro y
        // «Te faltan» (revisor, sexta pasada). Llevan aire dentro y el mismo margen negativo fuera,
        // para que el texto no se mueva.
        String dimens = leer(RES.resolve("values/dimens.xml"));
        assertTrue(dimens.contains("<dimen name=\"aire_anillo\">8dp</dimen>")
                && dimens.contains("<dimen name=\"aire_anillo_fuera\">-8dp</dimen>"));
        String macro = leer(RES.resolve("layout/view_macro_barra.xml"));
        assertTrue("la fila de la macro lleva aire",
                macro.contains("android:paddingStart=\"@dimen/aire_anillo\"") && macro.contains("android:paddingEnd=\"@dimen/aire_anillo\""));
        String nutricion = leer(RES.resolve("layout/fragment_nutricion.xml"));
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<include[^>]*layout=\"@layout/view_macro_barra\"[^>]*/>").matcher(nutricion);
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue("y su margen negativo, para no mover el texto: " + m.group(),
                    m.group().contains("android:layout_marginStart=\"@dimen/aire_anillo_fuera\"")
                            && m.group().contains("android:layout_marginEnd=\"@dimen/aire_anillo_fuera\""));
        }
        assertEquals(3, n);
        String programa = leer(RES.resolve("layout/view_tu_programa.xml"));
        java.util.regex.Matcher c = java.util.regex.Pattern
                .compile("<LinearLayout[^>]*android:id=\"@\\+id/cabeceraPrograma\"[^>]*>").matcher(programa);
        assertTrue(c.find());
        // Lado a lado: android:layout_margin negativo no se aplica (séptima pasada, el texto se movió 8 dp).
        assertTrue("la cabecera del programa lleva aire y su margen negativo, lado a lado",
                c.group().contains("android:padding=\"@dimen/aire_anillo\"")
                        && c.group().contains("android:layout_marginStart=\"@dimen/aire_anillo_fuera\"")
                        && c.group().contains("android:layout_marginTop=\"@dimen/aire_anillo_fuera\"")
                        && c.group().contains("android:layout_marginEnd=\"@dimen/aire_anillo_fuera\"")
                        && c.group().contains("android:layout_marginBottom=\"@dimen/aire_anillo_fuera\""));
        // La fila de kcal de Nutrición: con letra 1,3 se apila y el anillo pasaba a 1 dp de «kcal para
        // hoy», con la esquina tocándola (octava pasada).
        java.util.regex.Matcher k = java.util.regex.Pattern
                .compile("<LinearLayout[^>]*android:id=\"@\\+id/filaKcal\"[^>]*>").matcher(nutricion);
        assertTrue(k.find());
        assertTrue("la fila de kcal lleva aire y su margen negativo, lado a lado",
                k.group().contains("android:padding=\"@dimen/aire_anillo\"")
                        && k.group().contains("android:layout_marginStart=\"@dimen/aire_anillo_fuera\"")
                        && k.group().contains("android:layout_marginTop=\"@dimen/aire_anillo_fuera\"")
                        && k.group().contains("android:layout_marginEnd=\"@dimen/aire_anillo_fuera\"")
                        && k.group().contains("android:layout_marginBottom=\"@dimen/aire_anillo_fuera\""));
        // La fila de la macro: el aire de arriba y abajo también se devuelve, o la tarjeta crecía unos
        // 5 dp (séptima pasada).
        assertTrue(dimens.contains("<dimen name=\"aire_anillo_alto\">4dp</dimen>")
                && dimens.contains("<dimen name=\"aire_anillo_alto_fuera\">-4dp</dimen>"));
        assertTrue("arriba y abajo, 4 dp", macro.contains("android:paddingTop=\"@dimen/aire_anillo_alto\"")
                && macro.contains("android:paddingBottom=\"@dimen/aire_anillo_alto\""));
        m.reset();
        while (m.find()) {
            assertTrue("y lo devuelve abajo: " + m.group(),
                    m.group().contains("android:layout_marginBottom=\"@dimen/aire_anillo_alto_fuera\""));
        }
    }

    @Test
    public void ningunMargenNegativoDondeAndroidLoIgnora() throws IOException {
        // MarginLayoutParams solo aplica android:layout_margin, layout_marginHorizontal y
        // layout_marginVertical si son 0 o más: uno negativo se ignora sin aviso. La cabecera de
        // «Tu programa» llevaba layout_margin="-8dp" y su texto se movió 8 dp (revisor, séptima
        // pasada). Los márgenes negativos van lado a lado (marginStart, marginTop…), que sí se aplican.
        java.util.Set<String> negativos = new java.util.HashSet<>();
        Matcher d = Pattern.compile("<dimen name=\"(\\w+)\">\\s*-[0-9.]+\\w*\\s*</dimen>")
                .matcher(leer(RES.resolve("values/dimens.xml")));
        while (d.find()) negativos.add(d.group(1));
        Pattern atributo = Pattern.compile("android:layout_margin(Horizontal|Vertical)?=\"([^\"]+)\"");
        List<String> mal = new ArrayList<>();
        try (java.util.stream.Stream<Path> ficheros = Files.list(RES.resolve("layout"))) {
            for (Path f : (Iterable<Path>) ficheros::iterator) {
                Matcher a = atributo.matcher(leer(f));
                while (a.find()) {
                    String v = a.group(2);
                    boolean negativo = v.startsWith("-")
                            || (v.startsWith("@dimen/") && negativos.contains(v.substring(7)));
                    if (negativo) mal.add(f.getFileName() + ": " + a.group());
                }
            }
        }
        assertTrue("márgenes negativos que Android ignora: " + mal, mal.isEmpty());
    }

    @Test
    public void volverYLosEnlacesTambien() throws IOException {
        // «Volver» de la MaterialToolbar no usaba el fondo del tema (1,18:1 con foco), y el anillo de
        // un enlace de texto sin aire cortaba sus letras (revisor, GP-228, segunda pasada).
        String temas = leer(RES.resolve("values/themes.xml"));
        assertTrue(temas.contains("<item name=\"toolbarNavigationButtonStyle\">@style/Widget.GymProFit.Cabecera.Volver</item>"));
        Matcher m = Pattern.compile("<style name=\"Widget.GymProFit.Cabecera.Volver\"[\\s\\S]*?</style>").matcher(temas);
        assertTrue(m.find());
        assertTrue(m.group().contains("@drawable/fondo_pulsable_redondo"));
        String sistema = leer(RES.resolve("values/estilos_sistema.xml"));
        m = Pattern.compile("<style name=\"Widget.GymProFit.CabeceraSeccion.Enlace\"[\\s\\S]*?</style>").matcher(sistema);
        assertTrue(m.find());
        assertTrue("aire a los lados para el anillo", m.group().contains("<item name=\"android:paddingStart\">@dimen/espacio_8</item>")
                && m.group().contains("<item name=\"android:paddingEnd\">@dimen/espacio_8</item>"));
        assertTrue("sin margen negativo: el padre recortaba el lado derecho del anillo",
                !m.group().contains("layout_marginEnd"));
    }

    @Test
    public void elAnilloDeLaBarraVaEnLaPildora() throws IOException {
        // Un óvalo en toda la pestaña cruzaba la etiqueta (revisor, tercera pasada): como en
        // Material 3, el foco se marca en la píldora del icono.
        String pestana = leer(RES.resolve("layout/view_pestana.xml"));
        assertTrue(pestana.contains("android:background=\"@drawable/fondo_pestana\""));
        assertTrue(pestana.contains("android:duplicateParentState=\"true\"")
                && pestana.contains("android:foreground=\"@drawable/anillo_pildora\""));
        String fondo = leer(RES.resolve("drawable/fondo_pestana.xml"));
        assertTrue("el fondo de la pestaña no dibuja el foco", !fondo.contains("state_focused"));
        String anillo = leer(RES.resolve("drawable/anillo_pildora.xml"));
        assertTrue(anillo.contains("android:state_focused=\"true\"")
                && anillo.contains("<stroke android:width=\"@dimen/selector_anillo_foco\" android:color=\"@color/gp_on_surface\""));
    }

    @Test
    public void elTextoSobreLaTarjetaPulsadaPasaCuatroYMedio() throws IOException {
        List<String> fallos = new ArrayList<>();
        String[] textos = { "gp_on_surface", "gp_text_secondary", "gp_primary" };
        for (String tema : TEMAS) {
            String tarjeta = color(tema, "gp_surface");
            String pulsada = mezcla(color(tema, "gp_on_surface"), tarjeta, CAPA);
            for (String t : textos) {
                for (String[] fondo : new String[][]{ { "con foco", tarjeta }, { "pulsada", pulsada } }) {
                    // Excepción medida y aceptada: el naranja sobre la tarjeta pulsada en oscuro da
                    // 4,40:1, solo mientras el dedo está encima (la capa es la del selector, GP-224).
                    if (t.equals("gp_primary") && fondo[0].equals("pulsada") && tema.equals("values-night")) continue;
                    double r = MedidorNivelTest.contraste(color(tema, t), fondo[1]);
                    if (r < 4.5) fallos.add(String.format("%s: %s sobre la tarjeta %s = %.2f", tema, t, fondo[0], r));
                }
            }
        }
        if (!fallos.isEmpty()) fail("Texto por debajo de 4,5:1:\n" + String.join("\n", fallos));
    }

    @Test
    public void elNaranjaPulsadoEnOscuroSigueEnTresAUnoComoMinimo() throws IOException {
        // La excepción de arriba, acotada: si la capa o la paleta la bajan más, se ve aquí.
        String pulsada = mezcla(color("values-night", "gp_on_surface"), color("values-night", "gp_surface"), CAPA);
        double r = MedidorNivelTest.contraste(color("values-night", "gp_primary"), pulsada);
        assertTrue(String.format("naranja sobre la tarjeta pulsada en oscuro = %.2f", r), r >= 4.3);
    }

    @Test
    public void elAnilloPasaTresAUno() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            for (String fondo : new String[]{ "gp_surface", "gp_background" }) {
                double r = MedidorNivelTest.contraste(color(tema, "gp_on_surface"), color(tema, fondo));
                if (r < 3.0) fallos.add(String.format("%s: anillo sobre %s = %.2f", tema, fondo, r));
            }
        }
        if (!fallos.isEmpty()) fail("Anillo por debajo de 3:1:\n" + String.join("\n", fallos));
    }

    // El color del tema, o el de values/ si el tema no lo redefine.
    private static String color(String tema, String nombre) throws IOException {
        String v = buscar(leer(RES.resolve(tema + "/colors.xml")), nombre);
        if (v == null) v = buscar(leer(RES.resolve("values/colors.xml")), nombre);
        assertTrue("no está " + nombre, v != null);
        return v;
    }

    private static String buscar(String xml, String nombre) {
        Matcher m = Pattern.compile("<color name=\"" + nombre + "\">(#[0-9A-Fa-f]{6})</color>").matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    private static String mezcla(String fg, String bg, double alfa) {
        StringBuilder sb = new StringBuilder("#");
        for (int i = 0; i < 3; i++) {
            int f = Integer.parseInt(fg.substring(1 + 2 * i, 3 + 2 * i), 16);
            int b = Integer.parseInt(bg.substring(1 + 2 * i, 3 + 2 * i), 16);
            sb.append(String.format("%02X", Math.round(alfa * f + (1 - alfa) * b)));
        }
        return sb.toString();
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
