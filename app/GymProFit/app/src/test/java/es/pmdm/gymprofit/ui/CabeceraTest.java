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
// CabeceraTest — GP-062: un solo patrón de cabecera y 48 dp en lo pulsable.
//
// Convivían la Toolbar (9 pantallas) y una fila hecha a mano con un ImageView de
// 40 dp y sin nombre accesible (18). Este test fija, leyendo res/layout:
//   · no vuelve la cabecera a mano (ningún btnBack) ni la Toolbar de AppCompat;
//   · toda MaterialToolbar lleva el estilo común Widget.GymProFit.Cabecera;
//   · el estilo común pone nombre a la flecha de volver;
//   · ninguna vista pulsable declara un ancho o alto por debajo de 48 dp;
//   · ninguna vista pulsable tiene contentDescription="@null".
// Lo que se crea desde código lo mide MedidorAccesibilidad (variante debug).
// ============================================================
public class CabeceraTest {

    private static final Path LAYOUT = Paths.get("src", "main", "res", "layout");
    private static final Path TEMAS = Paths.get("src", "main", "res", "values", "themes.xml");
    private static final Pattern ELEMENTO = Pattern.compile("<([\\w.]+)\\b([^<>]*?)/?>");

    @Test
    public void no_vuelve_la_cabecera_a_mano() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (Path f : layouts()) {
            String s = leer(f);
            if (s.contains("@+id/btnBack")) fallos.add(f.getFileName() + ": btnBack");
            if (s.contains("<androidx.appcompat.widget.Toolbar")) fallos.add(f.getFileName() + ": Toolbar de AppCompat");
        }
        if (!fallos.isEmpty()) fail("Cabecera fuera del patrón común:\n" + String.join("\n", fallos));
    }

    @Test
    public void toda_toolbar_lleva_el_estilo_comun() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (Path f : layouts()) {
            Matcher m = ELEMENTO.matcher(leer(f));
            while (m.find()) {
                if (m.group(1).endsWith("MaterialToolbar")
                        && !m.group(2).contains("style=\"@style/Widget.GymProFit.Cabecera\"")) {
                    fallos.add(f.getFileName().toString());
                }
            }
        }
        if (!fallos.isEmpty()) fail("MaterialToolbar sin Widget.GymProFit.Cabecera:\n" + String.join("\n", fallos));
    }

    @Test
    public void la_flecha_de_volver_tiene_nombre() throws IOException {
        Matcher m = Pattern.compile("<style name=\"Widget\\.GymProFit\\.Cabecera\"[\\s\\S]*?</style>")
                .matcher(leer(TEMAS));
        assertTrue("Falta Widget.GymProFit.Cabecera", m.find());
        assertTrue("La cabecera común no nombra el botón de volver",
                m.group().contains("<item name=\"navigationContentDescription\">@string/btn_volver</item>"));
    }

    @Test
    public void nada_pulsable_por_debajo_de_48dp() throws IOException {
        Pattern medida = Pattern.compile("android:layout_(width|height)=\"(\\d+)dp\"");
        List<String> fallos = new ArrayList<>();
        for (Path f : layouts()) {
            String s = leer(f);
            Matcher m = ELEMENTO.matcher(s);
            while (m.find()) {
                if (!esPulsable(m.group(1), m.group(2))) continue;
                Matcher d = medida.matcher(m.group(2));
                while (d.find()) {
                    int dp = Integer.parseInt(d.group(2));
                    // 0dp es un peso de LinearLayout, no una medida.
                    if (dp > 0 && dp < 48) {
                        fallos.add(f.getFileName() + ":" + linea(s, m.start()) + " " + d.group(1) + "=" + dp + "dp");
                    }
                }
            }
        }
        if (!fallos.isEmpty()) fail("Zonas pulsables por debajo de 48 dp:\n" + String.join("\n", fallos));
    }

    @Test
    public void nada_pulsable_sin_nombre() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (Path f : layouts()) {
            String s = leer(f);
            Matcher m = ELEMENTO.matcher(s);
            while (m.find()) {
                if (esPulsable(m.group(1), m.group(2))
                        && m.group(2).contains("android:contentDescription=\"@null\"")) {
                    fallos.add(f.getFileName() + ":" + linea(s, m.start()));
                }
            }
        }
        if (!fallos.isEmpty()) fail("Vistas pulsables sin nombre accesible:\n" + String.join("\n", fallos));
    }

    // Un TextView pulsable es un enlace: mide lo que su texto (22-38 dp) salvo que
    // pida los 48. Los de «Saltar» del onboarding y «¿Ya tienes cuenta?» del registro
    // se escaparon a la primera pasada porque no declaran medida que leer.
    @Test
    public void todo_texto_pulsable_pide_48dp_de_alto() throws IOException {
        Pattern textView = Pattern.compile("<TextView\\b([^<>]*?)/?>");
        List<String> fallos = new ArrayList<>();
        for (Path f : layouts()) {
            String s = leer(f);
            Matcher m = textView.matcher(s);
            while (m.find()) {
                String a = m.group(1);
                if (a.contains("android:clickable=\"true\"") && !a.contains("android:minHeight=\"48dp\"")) {
                    fallos.add(f.getFileName() + ":" + linea(s, m.start()));
                }
            }
        }
        if (!fallos.isEmpty()) fail("Texto pulsable sin minHeight de 48 dp:\n" + String.join("\n", fallos));
    }

    @Test
    public void los_desplegables_miden_48dp() throws IOException {
        Matcher m = Pattern.compile("<style name=\"Widget\\.GymProFit\\.Spinner\"[\\s\\S]*?</style>")
                .matcher(leer(TEMAS));
        assertTrue("Falta Widget.GymProFit.Spinner", m.find());
        assertTrue("El desplegable común no pide 48 dp de alto",
                m.group().contains("<item name=\"android:minHeight\">48dp</item>"));
    }

    // Pulsable: la marca clickable, un botón, o un id de botón (btn…, fab…).
    private static boolean esPulsable(String etiqueta, String atributos) {
        Matcher id = Pattern.compile("android:id=\"@\\+id/(\\w+)\"").matcher(atributos);
        String nombre = id.find() ? id.group(1) : "";
        return atributos.contains("android:clickable=\"true\"")
                || etiqueta.endsWith("Button")
                || nombre.startsWith("btn") || nombre.startsWith("fab");
    }

    private static List<Path> layouts() throws IOException {
        try (Stream<Path> s = Files.list(LAYOUT)) {
            return s.filter(p -> p.toString().endsWith(".xml")).sorted().collect(Collectors.toList());
        }
    }

    private static int linea(String s, int pos) {
        int n = 1;
        for (int i = 0; i < pos; i++) if (s.charAt(i) == '\n') n++;
        return n;
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
