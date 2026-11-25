package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// ============================================================
// UnDiaImpactoWebTest — la «Política de impacto» de «Un día» (web/PLAN-UN-DIA.md, §1), en
// lo que se puede comprobar sin abrir un navegador. Lee web/src y web/dist:
//
// · El reloj es el protagonista: su ancho, en el CSS, entre el 55 y el 70 % de la pantalla en
//   escritorio y el 90 % en móvil; y cada cifra es una columna de cuentakilómetros.
// · La luz son lamas: capas con su degradado pintado una vez, que el movimiento solo gira,
//   estira y funde. Ningún módulo de movimiento escribe un degradado.
// · Sin JavaScript (o con movimiento reducido) cada escena está completa: el récord, la marca
//   tachada y todas las frases están en el HTML, fuera de la interfaz decorativa.
// · El cursor propio deja el del sistema en los enlaces, los botones y los campos.
// · Las chispas del récord: entre 12 y 20, y solo donde el equipo da la talla.
// · SplitText, DrawSVG y ScrollTrigger llegan con import(), nunca en el módulo inicial.
// ============================================================
public class UnDiaImpactoWebTest {

    @Test
    public void elRelojOcupaDel55Al70PorCientoEnEscritorioYEl90EnMovil() throws IOException {
        String css = css();
        Matcher m = Pattern.compile("--reloj-ancho:\\s*([0-9.]+)vw").matcher(css);
        List<Double> anchos = new ArrayList<>();
        while (m.find()) anchos.add(Double.parseDouble(m.group(1)));
        assertTrue("un-dia.css no fija --reloj-ancho en vw", !anchos.isEmpty());
        assertTrue("el reloj en móvil tiene que ocupar el 90 % del ancho: " + anchos, anchos.contains(90.0));
        assertTrue("el reloj en escritorio tiene que ocupar del 55 al 70 %: " + anchos,
                anchos.stream().anyMatch(a -> a >= 55 && a <= 70));
        assertTrue("el tamaño del reloj no sale de --reloj-ancho",
                Pattern.compile("\\.reloj-dia\\s*\\{[^}]*font-size:[^;]*var\\(--reloj-ancho\\)").matcher(css).find());
    }

    @Test
    public void cadaCifraDelRelojEsUnaColumnaQueRueda() throws IOException {
        String html = pagina();
        Matcher col = Pattern.compile("<span class=\"odo-tira\"[^>]*>(.*?)</span></span>").matcher(html);
        int columnas = 0;
        while (col.find()) {
            columnas++;
            int cifras = cuenta(col.group(1), "<span>");
            assertTrue("una columna del reloj solo tiene " + cifras + " cifras: tiene que poder rodar", cifras >= 4);
        }
        assertTrue("el reloj gigante tiene que tener sus cuatro columnas (hay " + columnas + ")", columnas >= 4);
    }

    @Test
    public void laLuzSonLamasQueSeMuevenSinRepintarDegradados() throws IOException {
        String html = pagina();
        assertTrue("cada hora necesita su capa de lamas", cuenta(html, "class=\"lamas\"") >= 3);
        assertTrue("las lamas se pintan con un degradado que se repite",
                Pattern.compile("\\.lamas\\s*\\{[^}]*repeating-linear-gradient").matcher(css()).find());
        for (Path js : movimiento()) {
            String c = leer(js);
            assertFalse(js.getFileName() + " escribe un degradado: la luz solo se gira, se estira y se funde",
                    c.contains("gradient("));
        }
    }

    @Test
    public void sinJavaScriptElRecordYLasFrasesEstanEnElHtml() throws IOException {
        String html = pagina();
        JsonObject sesion = datos().getAsJsonObject("sesion");
        int figura = html.indexOf("<figure class=\"movil");
        int fin = html.indexOf("</figure>", figura);
        String fuera = html.substring(0, Math.max(0, figura)) + html.substring(Math.max(0, fin));
        String marca = sesion.get("aviso").getAsString().replace("¡Récord! ", "").replace(" ", " ");
        assertTrue("la marca nueva («" + marca + "») tiene que leerse fuera de la interfaz",
                texto(fuera).contains(marca));
        String antes = sesion.get("avisoAntes").getAsString().replace(" ", " ");
        Matcher tachada = Pattern.compile("<s[ >][^<]*" + Pattern.quote(antes))
                .matcher(fuera.replace("&nbsp;", " ").replace(" ", " "));
        assertTrue("la marca de antes va tachada con <s> en el HTML", tachada.find());
        Matcher frases = Pattern.compile("<ol class=\"frases\"[^>]*>(.*?)</ol>", Pattern.DOTALL).matcher(html);
        assertTrue("las frases de la escena van en una lista", frases.find());
        assertTrue("faltan frases: cada paso de la sesión tiene la suya", cuenta(frases.group(1), "<li") >= 6);
    }

    @Test
    public void elCursorPropioDejaElDelSistemaEnEnlacesBotonesYCampos() throws IOException {
        String css = css();
        assertTrue("el cursor propio se esconde solo con su clase en html",
                Pattern.compile("\\.cursor-propio[^{]*\\{[^}]*cursor:\\s*none").matcher(css).find());
        Matcher regla = Pattern.compile("([^{}]*)\\{[^}]*cursor:\\s*(auto|pointer|text)").matcher(css);
        Set<String> devueltos = new HashSet<>();
        while (regla.find()) {
            String sel = regla.group(1);
            if (!sel.contains("cursor-propio")) continue;
            for (String s : new String[]{"a", "button", "input"}) {
                if (Pattern.compile("(^|[\\s,(>])" + s + "(\\b|$)").matcher(sel).find()) devueltos.add(s);
            }
        }
        assertEquals("el cursor del sistema tiene que volver en a, button e input", 3, devueltos.size());
    }

    @Test
    public void lasChispasSonEntre12Y20YSoloDondeElEquipoDaLaTalla() throws IOException {
        String escena = leer(WebDist.web().resolve("src/movimiento/escena-entreno.js"));
        Matcher m = Pattern.compile("CHISPAS\\s*=\\s*(\\d+)").matcher(escena);
        assertTrue("escena-entreno.js no dice cuántas chispas hay", m.find());
        int n = Integer.parseInt(m.group(1));
        assertTrue("chispas: " + n + ", tienen que ser entre 12 y 20", n >= 12 && n <= 20);
        assertTrue("las chispas tienen que mirar el equipo (hardwareConcurrency)", escena.contains("hardwareConcurrency"));
    }

    @Test
    public void losPluginsDeLasEscenasLleganConImport() throws IOException {
        String html = pagina();
        Path astro = WebDist.dist().resolve("_astro");
        Matcher m = Pattern.compile("<script type=\"module\" src=\"/_astro/([^\"]+\\.js)\"").matcher(html);
        Set<String> iniciales = new HashSet<>();
        while (m.find()) iniciales.add(m.group(1));
        boolean split = false, draw = false;
        try (Stream<Path> s = Files.list(astro)) {
            for (Path js : s.filter(p -> p.toString().endsWith(".js")).collect(Collectors.toList())) {
                String c = leer(js);
                boolean tieneSplit = c.contains("SplitText"), tieneDraw = c.contains("DrawSVG");
                if (iniciales.contains(js.getFileName().toString())) {
                    assertFalse("el módulo inicial trae SplitText", tieneSplit);
                    assertFalse("el módulo inicial trae DrawSVG", tieneDraw);
                    assertFalse("el módulo inicial trae ScrollTrigger", c.contains("ScrollTrigger"));
                }
                split |= tieneSplit;
                draw |= tieneDraw;
            }
        }
        assertTrue("no hay ningún trozo con SplitText (titulares por caracteres)", split);
        assertTrue("no hay ningún trozo con DrawSVG (el check y el tachado)", draw);
    }

    @Test
    public void elTitularSeLeeEnteroYUnaSolaVez() throws IOException {
        String html = pagina();
        Matcher h1 = Pattern.compile("<h1[^>]*>(.*?)</h1>", Pattern.DOTALL).matcher(html);
        assertTrue("no hay h1", h1.find());
        assertFalse("el h1 no lleva aria-label: se leería dos veces",
                h1.group(0).substring(0, h1.group(0).indexOf('>')).contains("aria-label"));
        String leido = normal(paraLector(h1.group(1)));
        assertEquals("el lector tiene que leer el titular entero y una sola vez",
                "Tu entreno y tu dieta, por fin juntos.", leido);
        // Lo que se ve (las letras troceadas) dice lo mismo que lo que se lee.
        String todo = normal(sinEtiquetas(h1.group(1)));
        String visto = todo.replaceFirst(Pattern.quote(leido), "");
        assertEquals("las letras del titular no dicen lo mismo que su texto", leido.replace(" ", ""), visto.replace(" ", ""));
    }

    @Test
    public void todoLoTroceadoVaOcultoAlLectorConSuTextoEnteroAlLado() throws IOException {
        String html = pagina();
        Matcher m = Pattern.compile("<span class=\"(letras|tit-letras)\"([^>]*)>").matcher(html);
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue("un texto troceado no va oculto al lector: " + m.group(0), m.group(2).contains("aria-hidden=\"true\""));
            int antes = html.lastIndexOf("<span class=\"solo-lector\">", m.start());
            assertTrue("un texto troceado no tiene su texto entero para el lector delante", antes >= 0 && m.start() - antes < 400);
        }
        assertTrue("faltan textos troceados (titular, transición, frases y récord): " + n, n >= 10);
    }

    // ── Ayudantes ──────────────────────────────────────────────────────────

    /** El texto que llega al lector de pantalla: sin lo que cuelga de un aria-hidden="true". */
    private static String paraLector(String html) {
        StringBuilder sb = new StringBuilder();
        java.util.ArrayDeque<Boolean> oculto = new java.util.ArrayDeque<>();
        Matcher t = Pattern.compile("<(/?)([a-zA-Z0-9]+)([^>]*?)(/?)>|([^<]+)").matcher(html);
        while (t.find()) {
            if (t.group(5) != null) {
                if (!oculto.contains(true)) sb.append(t.group(5));
            } else if (t.group(1).isEmpty() && t.group(4).isEmpty()) {
                oculto.push(t.group(3).contains("aria-hidden=\"true\""));
            } else if (!t.group(1).isEmpty() && !oculto.isEmpty()) {
                oculto.pop();
            }
        }
        return sb.toString();
    }

    private static String sinEtiquetas(String html) {
        return html.replaceAll("<[^>]+>", "");
    }

    private static String normal(String s) {
        return s.replace("&nbsp;", " ").replace(" ", " ").replaceAll("\\s+", " ").trim();
    }

    private static String pagina() throws IOException {
        return leer(WebDist.dist().resolve("un-dia.html"));
    }

    private static String css() throws IOException {
        return leer(WebDist.web().resolve("src/estilos/un-dia.css"));
    }

    private static JsonObject datos() throws IOException {
        return JsonParser.parseString(leer(WebDist.web().resolve("src/datos/dia-ejemplo.json"))).getAsJsonObject();
    }

    private static List<Path> movimiento() throws IOException {
        try (Stream<Path> s = Files.list(WebDist.web().resolve("src/movimiento"))) {
            return s.filter(p -> p.toString().endsWith(".js")).sorted().collect(Collectors.toList());
        }
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    private static String texto(String html) {
        return html.replaceAll("<[^>]+>", "").replace("&nbsp;", " ").replace(" ", " ");
    }

    private static int cuenta(String texto, String trozo) {
        int n = 0;
        for (int i = texto.indexOf(trozo); i >= 0; i = texto.indexOf(trozo, i + 1)) n++;
        return n;
    }
}
