package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import es.pmdm.gymprofit.envivo.LogicaRecordVivo;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.record.Record;

// ============================================================
// UnDiaWebTest — la prueba de concepto de «Un día» (fase 1; DEC-069, propuesta;
// web/PLAN-UN-DIA.md). Mira la salida de Astro (web/dist/un-dia.html y sus trozos de JS) y
// los datos de web/src/datos/dia-ejemplo.json:
//
// · La entrada: un solo h1, la etiqueta «Un día de ejemplo» una vez, la acción a /beta antes
//   de la escena, y noindex mientras sea una prueba.
// · Ninguna cifra escrita a mano: cada número de la interfaz rehecha y de su descripción está
//   en dia-ejemplo.json.
// · El récord con la regla de la app, no con una suposición: las series se pasan por
//   LogicaRecordVivo.supera y alMarcar, y el récord tiene que caer en la serie 3.
// · Las cargas de «Hoy toca» son creíbles: cada ejercicio con la suya, y las elevaciones
//   laterales (mancuernas, aislamiento) muy por debajo de un básico con barra.
// · El presupuesto de JS, medido en gzip sobre dist (DEC-069): inicial ≤ 6 KB (el objetivo;
//   el tope es 50), diferido ≤ 80 KB en móvil y ≤ 90 KB en escritorio (con Lenis y el
//   cursor). El panel de medida no cuenta.
// · Las licencias de GSAP y de Lenis viajan en sus trozos.
// · El scroll táctil es el nativo: ScrollTrigger con ignoreMobileResize, sin pin ni
//   normalizeScroll (la escena se fija con sticky), y ninguna curva que rebote.
// ============================================================
public class UnDiaWebTest {

    private static final int KB = 1024;

    @Test
    public void laEntradaTieneUnSoloH1LaEtiquetaYLaAccion() throws IOException {
        String html = pagina();
        assertEquals("tiene que haber un solo h1", 1, cuenta(html, "<h1"));
        assertEquals("la etiqueta «Un día de ejemplo» va una vez", 1, cuenta(html, "Un día de ejemplo"));
        int accion = html.indexOf("href=\"/beta\"");
        int escena = html.indexOf("data-escena=");
        assertTrue("la acción a /beta tiene que estar en la entrada, antes de la escena", accion >= 0 && accion < escena);
        assertTrue("la prueba no se indexa", html.contains("<meta name=\"robots\" content=\"noindex\">"));
    }

    @Test
    public void ningunaCifraDeLaInterfazEstaEscritaAMano() throws IOException {
        String html = pagina();
        Set<String> delJson = numeros(texto(new String(Files.readAllBytes(json()), StandardCharsets.UTF_8)));
        int desde = html.indexOf("<figure class=\"movil");
        int hasta = html.indexOf("</figure>", desde);
        assertTrue("no se encuentra la interfaz de la escena", desde > 0 && hasta > desde);
        String interfaz = texto(html.substring(desde, hasta));
        Set<String> vistos = numeros(interfaz);
        assertTrue("la interfaz tiene que enseñar cifras", vistos.size() >= 10);
        for (String n : vistos) {
            assertTrue("la cifra «" + n + "» no sale de dia-ejemplo.json", delJson.contains(n));
        }
    }

    @Test
    public void elRecordCaeEnLaTerceraSerieConLaReglaDeLaApp() throws IOException {
        JsonObject sesion = datos().getAsJsonObject("sesion");
        JsonObject guardada = sesion.getAsJsonObject("marcaGuardada");
        SesionEnCurso.Marca mejor = new SesionEnCurso.Marca(Record.TIPO_PESO,
                guardada.get("peso").getAsString(), guardada.get("repeticiones").getAsInt(), 0);

        SesionEnCurso.Ejercicio e = new SesionEnCurso.Ejercicio();
        e.marca = mejor;
        e.marcaCargada = true;
        int record = 0;
        int n = 0;
        for (JsonElement el : sesion.getAsJsonArray("series")) {
            JsonObject s = el.getAsJsonObject();
            n++;
            SesionEnCurso.Serie serie = new SesionEnCurso.Serie();
            serie.id = n;
            serie.peso = s.get("pesoExacto").getAsString();
            serie.repeticiones = s.get("reps").getAsString();
            serie.hecha = true;
            e.series.add(serie);
            boolean celebra = LogicaRecordVivo.alMarcar(e, n) != null;
            assertEquals("serie " + n + ": dia-ejemplo.json dice récord=" + s.get("record").getAsBoolean()
                    + " y la app dice " + celebra, celebra, s.get("record").getAsBoolean());
            if (celebra) record = n;
        }
        assertEquals("el récord tiene que caer en la serie 3", 3, record);
    }

    @Test
    public void lasCargasDeHoyTocaSonCreibles() throws IOException {
        Set<String> cargas = new HashSet<>();
        double laterales = -1, banca = -1;
        int n = 0;
        for (JsonElement el : datos().getAsJsonObject("hoyToca").getAsJsonArray("ejercicios")) {
            JsonObject e = el.getAsJsonObject();
            if (e.get("ultima") == null || e.get("ultima").isJsonNull()) continue;
            n++;
            String ultima = e.get("ultima").getAsString();
            assertTrue("«" + e.get("nombre").getAsString() + "» repite la carga " + ultima + " de otro ejercicio",
                    cargas.add(ultima));
            Matcher k = Pattern.compile("([0-9]+(?:,[0-9]+)?)").matcher(ultima);
            assertTrue("«" + ultima + "» no lleva kilos", k.find());
            double kg = Double.parseDouble(k.group(1).replace(',', '.'));
            if (e.get("nombre").getAsString().startsWith("Elevaciones laterales")) laterales = kg;
            if (e.get("nombre").getAsString().startsWith("Press de banca")) banca = kg;
        }
        assertTrue("«Hoy toca» tiene que enseñar la última carga de sus ejercicios", n >= 5);
        assertTrue("faltan las elevaciones laterales o el press de banca en Torso A", laterales > 0 && banca > 0);
        assertTrue("elevaciones laterales a " + laterales + " kg con la banca a " + banca + ": no es creíble",
                laterales <= banca / 4);
    }

    @Test
    public void elJsCabeEnElPresupuesto() throws IOException {
        Path astro = WebDist.dist().resolve("_astro");
        String html = pagina();
        Matcher m = Pattern.compile("<script type=\"module\" src=\"/_astro/([^\"]+\\.js)\"").matcher(html);
        Set<String> iniciales = new HashSet<>();
        while (m.find()) iniciales.add(m.group(1));
        assertFalse("la página no pide ningún módulo", iniciales.isEmpty());
        long inicial = 0, movil = 0, lenis = 0;
        for (Path js : trozos(astro)) {
            String nombre = js.getFileName().toString();
            String codigo = new String(Files.readAllBytes(js), StandardCharsets.UTF_8);
            long gz = gzip(Files.readAllBytes(js));
            if (iniciales.contains(nombre)) inicial += gz;
            else if (codigo.contains("Panel de medida")) continue;          // ?debug=motion
            else if (codigo.contains("darkroom.engineering")) lenis += gz;  // solo con ratón
            else movil += gz;
        }
        assertTrue("JS inicial " + inicial + " B gzip: el objetivo es ≤ 6 KB", inicial <= 6 * KB);
        assertTrue("JS diferido en móvil " + movil + " B gzip: tope 80 KB", movil <= 80 * KB);
        assertTrue("JS diferido en escritorio " + (movil + lenis) + " B gzip: tope 90 KB", movil + lenis <= 90 * KB);
        assertTrue("no se encuentra el trozo de Lenis", lenis > 0);
    }

    @Test
    public void lasLicenciasViajanEnSusTrozos() throws IOException {
        boolean gsap = false, lenis = false;
        for (Path js : trozos(WebDist.dist().resolve("_astro"))) {
            String c = new String(Files.readAllBytes(js), StandardCharsets.UTF_8);
            if (c.contains("GSAP 3.") && c.contains("@license")) gsap = true;
            if (c.contains("Lenis") && c.contains("The MIT License")) lenis = true;
        }
        assertTrue("la licencia de GSAP no está en su trozo", gsap);
        assertTrue("la licencia de Lenis no está en su trozo", lenis);
    }

    @Test
    public void elScrollTactilEsElNativoYNadaRebota() throws IOException {
        Path mov = WebDist.web().resolve("src/movimiento");
        // ScrollTrigger se configura una vez, en motor.js, que comparten las escenas.
        String motor = new String(Files.readAllBytes(mov.resolve("motor.js")), StandardCharsets.UTF_8);
        assertTrue("ScrollTrigger sin ignoreMobileResize", motor.contains("ignoreMobileResize: true"));
        try (Stream<Path> s = Files.list(mov)) {
            for (Path js : s.collect(Collectors.toList())) {
                String c = new String(Files.readAllBytes(js), StandardCharsets.UTF_8);
                assertFalse(js.getFileName() + " secuestra el scroll", c.contains("normalizeScroll"));
                assertFalse(js.getFileName() + ": las escenas se fijan con sticky, no con pin",
                        Pattern.compile("\\bpin\\s*:").matcher(c).find());
                assertFalse(js.getFileName() + " usa una curva que rebota",
                        Pattern.compile("['\"](back|elastic|bounce)[.(\"']").matcher(c).find());
            }
        }
    }

    // ── Ayudantes ──────────────────────────────────────────────────────────

    private static String pagina() throws IOException {
        return new String(Files.readAllBytes(WebDist.dist().resolve("un-dia.html")), StandardCharsets.UTF_8);
    }

    private static Path json() {
        return WebDist.web().resolve("src/datos/dia-ejemplo.json");
    }

    private static JsonObject datos() throws IOException {
        return JsonParser.parseString(new String(Files.readAllBytes(json()), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static List<Path> trozos(Path astro) throws IOException {
        try (Stream<Path> s = Files.list(astro)) {
            return s.filter(p -> p.toString().endsWith(".js")).sorted().collect(Collectors.toList());
        }
    }

    private static long gzip(byte[] datos) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(salida) { { def.setLevel(9); } }) {
            gz.write(datos);
        }
        return salida.size();
    }

    /** El texto visible y los atributos data-antes, sin etiquetas y con las entidades básicas. */
    private static String texto(String html) {
        StringBuilder sb = new StringBuilder();
        Matcher antes = Pattern.compile("data-antes=\"([^\"]*)\"").matcher(html);
        while (antes.find()) sb.append(' ').append(antes.group(1));
        return sb.append(' ').append(html.replaceAll("<[^>]+>", " "))
                .toString().replace("&nbsp;", " ").replace(" ", " ").replace("&amp;", "&");
    }

    /** Las cifras de un texto, como se escriben en español: «82,5», «2.260», «5:38», «10». */
    private static Set<String> numeros(String texto) {
        Set<String> n = new HashSet<>();
        Matcher m = Pattern.compile("\\d+(?:[.,:]\\d+)*").matcher(texto);
        while (m.find()) n.add(m.group());
        return n;
    }

    private static int cuenta(String texto, String trozo) {
        int n = 0;
        for (int i = texto.indexOf(trozo); i >= 0; i = texto.indexOf(trozo, i + 1)) n++;
        return n;
    }

}
