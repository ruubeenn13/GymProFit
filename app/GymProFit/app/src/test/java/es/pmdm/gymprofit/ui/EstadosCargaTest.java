package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// ============================================================
// EstadosCargaTest — GP-064: un solo sistema de estados, comprobado leyendo el código.
//
// El lint propio del lote 22. Cuatro reglas:
//  1. Ningún vacío hecho a mano en un layout (tvVacio, tvEmpty, layoutVacio…): va view_estado.
//  2. Ningún fallo de una carga (una petición @GET) avisado solo con un toast: el error se
//     queda en el contenido, con «Reintentar». Un toast puede acompañarlo. Una petición @GET
//     que es la respuesta a un toque (pedir el ejercicio para abrir su ficha) lleva el
//     comentario «GP-064: acción» dentro de su onFail, que dice por qué el toast basta.
//  3. LoadingDialog no tapa una carga (@GET): solo acciones que bloquean. La que sí lo hace a
//     propósito lleva «GP-064: acción» en la línea del show.
//  4. El diálogo de carga saca su color de la paleta, no de un hex a mano.
// ============================================================
public class EstadosCargaTest {

    private static final Path JAVA = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit");
    private static final Path LAYOUT = Paths.get("src", "main", "res", "layout");
    private static final String MARCA = "GP-064: acción";

    /** Vacíos a mano que se quedan, con su porqué (fichero → ids). */
    private static final Map<String, Set<String>> VACIOS_PERMITIDOS = new HashMap<>();
    static {
        // Rótulos dentro del lienzo de una gráfica («sin puntos en este periodo»), no el estado
        // de la pantalla: la carga y el error de esas pantallas sí son view_estado.
        permitir("fragment_medidas.xml", "tvGraficaVacia");
        permitir("activity_detalle_ejercicio.xml", "tvProgresionVacia");
        permitir("activity_estadisticas_nutricion.xml", "tvVacia", "tvMacrosVacia");
        // La comida sin alimentos del tablero 15: el icono de la comida en su círculo, su nombre
        // y «es la que toca», dentro de la tarjeta de la comida. Es el contenido de una comida
        // vacía, no un estado de carga: la carga y el error de Comida son view_estado.
        permitir("activity_comida.xml", "bloqueVacia", "tvVacia", "tvVaciaToca");
    }

    private static void permitir(String fichero, String... ids) {
        VACIOS_PERMITIDOS.computeIfAbsent(fichero, k -> new HashSet<>()).addAll(java.util.Arrays.asList(ids));
    }

    private static final Pattern ID_VACIO = Pattern.compile(
            "@\\+id/((?:tv|layout|bloque)\\w*(?:Vacio|Vacia|Empty|SinDatos|SinEjercicios)\\w*)");
    private static final Pattern GET = Pattern.compile(
            "@GET\\s*\\([^)]*\\)\\s*(?:@\\w+(?:\\([^)]*\\))?\\s*)*[\\w<>, ?]+\\s+(\\w+)\\s*\\(");
    private static final Pattern NO_GET = Pattern.compile(
            "@(?:POST|PUT|PATCH|DELETE|HTTP)\\s*\\([^)]*\\)\\s*(?:@\\w+(?:\\([^)]*\\))?\\s*)*[\\w<>, ?]+\\s+(\\w+)\\s*\\(");
    private static final Pattern ENQUEUE = Pattern.compile("\\.\\s*enqueue\\s*\\(\\s*new\\s+\\w*ApiCallback");
    private static final Pattern ON_FAIL = Pattern.compile("void\\s+onFail\\s*\\([^)]*\\)\\s*\\{");
    private static final Pattern TOAST = Pattern.compile(
            "toastError\\s*\\(|mostrarToastError\\s*\\(|Toast\\.makeText\\s*\\(");
    // Lo que pone el error en el contenido: VistaEstado.error, o un método de la pantalla que
    // lo pinta (fallo, mostrarError, pintarXError, error…).
    private static final Pattern EN_CONTENIDO = Pattern.compile(
            "\\.error\\s*\\(|\\b(?:fallo|fallar|mostrarError|pintar\\w*Error|error\\w*)\\s*\\(");
    private static final Pattern SHOW = Pattern.compile("LoadingDialog\\.show(?:ColdStart)?\\s*\\(");

    @Test
    public void ningun_vacio_a_mano_en_los_layouts() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (Path f : ficheros(LAYOUT, ".xml")) {
            Matcher m = ID_VACIO.matcher(leer(f));
            while (m.find()) {
                String nombre = f.getFileName().toString();
                Set<String> permitidos = VACIOS_PERMITIDOS.get(nombre);
                if (permitidos != null && permitidos.contains(m.group(1))) continue;
                fallos.add(nombre + ": " + m.group(1));
            }
        }
        if (!fallos.isEmpty()) fail("Vacío hecho a mano (va view_estado):\n" + String.join("\n", fallos));
    }

    @Test
    public void ningun_fallo_de_carga_solo_en_un_toast() throws IOException {
        Set<String> gets = metodosGet();
        List<String> fallos = new ArrayList<>();
        for (Path f : ficheros(JAVA.resolve("ui"), ".java")) {
            for (String s : toastSolo(leer(f), gets)) fallos.add(JAVA.relativize(f) + "  " + s);
        }
        if (!fallos.isEmpty()) {
            fail("Fallo de una carga avisado solo con un toast (el error va en el contenido, con "
                    + "«Reintentar»):\n" + String.join("\n", fallos));
        }
    }

    @Test
    public void loading_dialog_no_tapa_una_carga() throws IOException {
        Set<String> gets = metodosGet();
        List<String> fallos = new ArrayList<>();
        for (Path f : ficheros(JAVA.resolve("ui"), ".java")) {
            for (String s : dialogoEnCarga(leer(f), gets)) fallos.add(JAVA.relativize(f) + "  " + s);
        }
        if (!fallos.isEmpty()) {
            fail("LoadingDialog sobre una carga (va la de view_estado):\n" + String.join("\n", fallos));
        }
    }

    @Test
    public void el_dialogo_de_carga_no_lleva_un_hex() throws IOException {
        assertFalse("dialog_loading.xml con un color a mano",
                Pattern.compile("#[0-9A-Fa-f]{6}\"").matcher(leer(LAYOUT.resolve("dialog_loading.xml"))).find());
    }

    // El propio detector reconoce los casos: si no, las reglas pasarían siempre.
    @Test
    public void el_detector_distingue_los_casos() {
        Set<String> gets = new HashSet<>();
        gets.add("getTodo");
        String soloToast = "api.getTodo(1).enqueue(new ApiCallback<X>() {\n"
                + " public void onOk(X x) { }\n"
                + " public void onFail(int c, String m) { UiFeedback.toastError(a, c, m); }\n});";
        String conEstado = "api.getTodo(1).enqueue(new ApiCallback<X>() {\n"
                + " public void onFail(int c, String m) { UiFeedback.toastError(a, c, m); estado.error(t, this::r); }\n});";
        String marcado = "api.getTodo(1).enqueue(new ApiCallback<X>() {\n"
                + " public void onFail(int c, String m) { // GP-064: acción, abre la ficha al tocar\n"
                + " UiFeedback.toastError(a, c, m); }\n});";
        String accion = "api.borrar(1).enqueue(new ApiCallback<X>() {\n"
                + " public void onFail(int c, String m) { UiFeedback.toastError(a, c, m); }\n});";
        assertEquals(1, toastSolo(soloToast, gets).size());
        assertEquals(0, toastSolo(conEstado, gets).size());
        assertEquals(0, toastSolo(marcado, gets).size());
        assertEquals(0, toastSolo(accion, gets).size());

        String dialogo = "void cargar() {\n LoadingDialog.show(this);\n api.getTodo(1).enqueue(new ApiCallback<X>() { });\n}";
        String dialogoAccion = "void borrar() {\n LoadingDialog.show(this);\n api.borrar(1).enqueue(new ApiCallback<X>() { });\n}";
        assertEquals(1, dialogoEnCarga(dialogo, gets).size());
        assertEquals(0, dialogoEnCarga(dialogoAccion, gets).size());
        assertTrue(ID_VACIO.matcher("android:id=\"@+id/tvVacio\"").find());
        assertTrue(ID_VACIO.matcher("android:id=\"@+id/layoutRutinasVacio\"").find());
        assertFalse(ID_VACIO.matcher("android:id=\"@+id/estadoMedidas\"").find());
    }

    // ── Detectores ───────────────────────────────────────────

    /** Los onFail de una carga @GET que solo avisan con un toast. */
    static List<String> toastSolo(String codigo, Set<String> gets) {
        List<String> malos = new ArrayList<>();
        Matcher m = ENQUEUE.matcher(codigo);
        while (m.find()) {
            String metodo = metodoAntes(codigo, m.start());
            if (metodo == null || !gets.contains(metodo)) continue;
            int abre = codigo.indexOf('{', m.end());
            if (abre < 0) continue;
            String clase = codigo.substring(abre + 1, cierre(codigo, abre));
            Matcher f = ON_FAIL.matcher(clase);
            if (!f.find()) continue;
            String cuerpo = clase.substring(f.end(), cierre(clase, f.end() - 1));
            if (!TOAST.matcher(cuerpo).find()) continue;
            if (cuerpo.contains(MARCA)) continue;
            String sinToast = TOAST.matcher(cuerpo).replaceAll("");
            if (EN_CONTENIDO.matcher(sinToast).find()) continue;
            malos.add(metodo + "(): onFail {" + cuerpo.trim().replaceAll("\\s+", " ") + "}");
        }
        return malos;
    }

    /** Los LoadingDialog.show cuya siguiente petición es una carga @GET. */
    static List<String> dialogoEnCarga(String codigo, Set<String> gets) {
        List<String> malos = new ArrayList<>();
        Matcher m = SHOW.matcher(codigo);
        while (m.find()) {
            int finLinea = codigo.indexOf('\n', m.start());
            String linea = codigo.substring(m.start(), finLinea < 0 ? codigo.length() : finLinea);
            if (linea.contains(MARCA)) continue;
            Matcher e = ENQUEUE.matcher(codigo);
            if (!e.find(m.end())) continue;
            // Solo cuenta si la petición está cerca (la del mismo método): 40 líneas como mucho.
            String entre = codigo.substring(m.end(), e.start());
            if (entre.split("\n", -1).length > 40) continue;
            String metodo = metodoAntes(codigo, e.start());
            if (metodo != null && gets.contains(metodo)) malos.add(metodo + "()");
        }
        return malos;
    }

    // El nombre del método de la llamada que acaba justo antes de «.enqueue»: api.getX(…).
    private static String metodoAntes(String codigo, int punto) {
        int i = punto - 1;
        while (i >= 0 && Character.isWhitespace(codigo.charAt(i))) i--;
        if (i < 0 || codigo.charAt(i) != ')') return null;
        int nivel = 0;
        for (; i >= 0; i--) {
            char c = codigo.charAt(i);
            if (c == ')') nivel++;
            else if (c == '(' && --nivel == 0) break;
        }
        int fin = i;
        while (i > 0 && Character.isJavaIdentifierPart(codigo.charAt(i - 1))) i--;
        return i < fin ? codigo.substring(i, fin) : null;
    }

    // La posición de la llave que cierra la que abre en «abre».
    private static int cierre(String s, int abre) {
        int nivel = 0;
        for (int i = abre; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') nivel++;
            else if (c == '}' && --nivel == 0) return i;
        }
        return s.length();
    }

    // Los métodos de las interfaces de Retrofit que son @GET y no tienen tocayo que escriba.
    private static Set<String> metodosGet() throws IOException {
        Set<String> gets = new HashSet<>();
        Set<String> otros = new HashSet<>();
        for (Path f : ficheros(JAVA.resolve("network"), "Api.java")) {
            String s = leer(f);
            Matcher g = GET.matcher(s);
            while (g.find()) gets.add(g.group(1));
            Matcher o = NO_GET.matcher(s);
            while (o.find()) otros.add(o.group(1));
        }
        gets.removeAll(otros);
        assertTrue("No se encontró ningún @GET: el detector está roto", gets.size() > 20);
        return gets;
    }

    private static List<Path> ficheros(Path raiz, String fin) throws IOException {
        try (Stream<Path> s = Files.walk(raiz)) {
            return s.filter(p -> p.toString().endsWith(fin)).collect(Collectors.toList());
        }
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
