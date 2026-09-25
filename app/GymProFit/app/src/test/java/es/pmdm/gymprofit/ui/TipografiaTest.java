package es.pmdm.gymprofit.ui;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

// ============================================================
// TipografiaTest — GP-089: una familia, dos anchos, y nada por debajo de 13 sp.
//
// Barlow Condensed para títulos, cifras y etiquetas; Barlow para el texto. Este
// test fija, leyendo res/ y el código:
//   · res/font solo trae esas dos familias;
//   · el tema pone Barlow como fuente de todo TextView;
//   · ningún layout ni estilo pide una fuente del sistema;
//   · ningún tamaño de texto escrito en res/ ni en el código baja de 13 sp.
// Lo que se ve en pantalla lo mide MedidorAccesibilidad (variante debug) en el
// árbol de accesibilidad; esto evita que vuelva a entrar por el código.
// ============================================================
public class TipografiaTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path JAVA = Paths.get("src", "main", "java");
    private static final float MINIMO_SP = 13f;

    @Test
    public void solo_hay_barlow_y_barlow_condensed() throws IOException {
        List<String> ajenos = new ArrayList<>();
        try (Stream<Path> fuentes = Files.list(RES.resolve("font"))) {
            for (Path f : fuentes.collect(Collectors.toList())) {
                String nombre = f.getFileName().toString();
                if (!nombre.startsWith("barlow")) ajenos.add(nombre);
            }
        }
        if (!ajenos.isEmpty()) fail("Fuentes fuera de la familia Barlow:\n" + String.join("\n", ajenos));
    }

    @Test
    public void el_tema_pone_barlow_en_todo_el_texto() throws IOException {
        String tema = leer(RES.resolve("values").resolve("themes.xml"));
        assertTrue("El tema no pone Barlow en la apariencia de texto por defecto",
                tema.contains("<item name=\"android:textAppearanceSmall\">@style/TextAppearance.GymProFit.Texto</item>"));
        // Un fontFamily en el tema gana al textAppearance de cada vista y borra la
        // condensada de los titulares: no puede volver.
        Matcher bloque = Pattern.compile("<style name=\"Theme\\.GymProFit\"[\\s\\S]*?</style>").matcher(tema);
        assertTrue("Falta Theme.GymProFit", bloque.find());
        assertTrue("El tema no puede fijar fontFamily: pisa el textAppearance de los titulares",
                !bloque.group().contains("fontFamily\">"));
        for (String cuerpo : new String[]{"TextAppearance.GymProFit.Texto",
                "Font.GymProFit.BodyLarge", "Font.GymProFit.BodyMedium", "Font.GymProFit.BodySmall"}) {
            Matcher m = Pattern.compile("name=\"" + Pattern.quote(cuerpo) + "\"[\\s\\S]*?</style>").matcher(tema);
            assertTrue("Falta el estilo " + cuerpo, m.find());
            assertTrue(cuerpo + " no va en Barlow", m.group().contains("@font/barlow<"));
        }
    }

    @Test
    public void ningun_recurso_pide_una_fuente_del_sistema() throws IOException {
        Pattern sistema = Pattern.compile(
                "(android:)?fontFamily\"?\\s*[=>]\\s*\"?(sans-serif|serif|monospace|casual|cursive)"
                        + "|android:typeface=");
        List<String> fallos = buscar(RES, ".xml", sistema);
        if (!fallos.isEmpty()) fail("Fuente del sistema pedida en:\n" + String.join("\n", fallos));
    }

    @Test
    public void ningun_tamano_escrito_en_res_baja_de_13sp() throws IOException {
        Pattern sp = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)sp\\b");
        List<String> fallos = new ArrayList<>();
        for (Path f : ficheros(RES, ".xml")) {
            String[] lineas = leer(f).split("\n");
            for (int i = 0; i < lineas.length; i++) {
                Matcher m = sp.matcher(lineas[i]);
                while (m.find()) {
                    if (Float.parseFloat(m.group(1)) < MINIMO_SP) {
                        fallos.add(RES.relativize(f) + ":" + (i + 1) + "  " + lineas[i].trim());
                    }
                }
            }
        }
        if (!fallos.isEmpty()) fail("Texto por debajo de 13 sp:\n" + String.join("\n", fallos));
    }

    @Test
    public void ningun_tamano_escrito_en_codigo_baja_de_13() throws IOException {
        // setTextSize(9f), setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f)...: el
        // último número literal de la llamada es el tamaño.
        Pattern llamada = Pattern.compile("set(?:Value|Center)?TextSize\\(([^;]*)\\)\\s*;");
        Pattern numero = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)f?\\s*$");
        List<String> fallos = new ArrayList<>();
        for (Path f : ficheros(JAVA, ".java")) {
            String[] lineas = leer(f).split("\n");
            for (int i = 0; i < lineas.length; i++) {
                Matcher m = llamada.matcher(lineas[i]);
                while (m.find()) {
                    Matcher n = numero.matcher(m.group(1).trim());
                    if (n.find() && Float.parseFloat(n.group(1)) < MINIMO_SP) {
                        fallos.add(JAVA.relativize(f) + ":" + (i + 1) + "  " + lineas[i].trim());
                    }
                }
            }
        }
        // El distintivo PRUEBAS · LOCAL de GymProFitApp solo existe en debug y no
        // es texto del producto: es la marca de que no se está en producción.
        fallos.removeIf(l -> l.contains("GymProFitApp.java") && l.contains("distintivo"));
        if (!fallos.isEmpty()) fail("Texto por debajo de 13 desde código:\n" + String.join("\n", fallos));
    }

    // Una etiqueta en mayúsculas es la etiqueta de la escala (condensada, 13 sp,
    // espaciada): tiene que llevar su apariencia y no un tamaño y un espaciado sueltos,
    // que la dejaban en Barlow normal y hacían partir «PROTEÍNAS» en dos líneas.
    @Test
    public void toda_etiqueta_en_mayusculas_lleva_la_apariencia_label() throws IOException {
        Pattern textView = Pattern.compile("<TextView\\b([^<>]*?)/?>");
        List<String> fallos = new ArrayList<>();
        for (Path f : ficheros(RES.resolve("layout"), ".xml")) {
            Matcher m = textView.matcher(leer(f));
            while (m.find()) {
                String atributos = m.group(1);
                if (atributos.contains("android:textAllCaps=\"true\"")
                        && !atributos.contains("android:textAppearance=\"@style/TextAppearance.GymProFit.Label\"")) {
                    fallos.add(f.getFileName().toString());
                }
            }
        }
        if (!fallos.isEmpty()) fail("Etiqueta en mayúsculas sin TextAppearance.GymProFit.Label:\n" + String.join("\n", fallos));
    }

    private static List<String> buscar(Path raiz, String extension, Pattern patron) throws IOException {
        List<String> fallos = new ArrayList<>();
        for (Path f : ficheros(raiz, extension)) {
            String[] lineas = leer(f).split("\n");
            for (int i = 0; i < lineas.length; i++) {
                if (patron.matcher(lineas[i]).find()) {
                    fallos.add(raiz.relativize(f) + ":" + (i + 1) + "  " + lineas[i].trim());
                }
            }
        }
        return fallos;
    }

    private static List<Path> ficheros(Path raiz, String extension) throws IOException {
        try (Stream<Path> s = Files.walk(raiz)) {
            return s.filter(p -> p.toString().endsWith(extension)).collect(Collectors.toList());
        }
    }

    private static String leer(Path f) throws IOException {
        return new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
    }
}
