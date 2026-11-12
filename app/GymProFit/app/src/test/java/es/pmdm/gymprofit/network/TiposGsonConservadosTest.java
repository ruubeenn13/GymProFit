package es.pmdm.gymprofit.network;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// ============================================================
// TiposGsonConservadosTest (GP-247, el aviso de GP-053) — Gson lee y escribe los campos por
// reflexión, así que R8 no puede renombrarlos ni quitar el constructor vacío de lo que pasa por
// él. proguard-rules.pro lo conserva todo bajo es.pmdm.gymprofit.model; lo que esté fuera
// necesita su propia regla -keep. Se buscan los tipos de dos sitios:
//   - fromJson(…, X.class) y fromJson(…, X[].class) en todo el código: lo que se escribe con
//     toJson se vuelve a leer con fromJson en algún sitio (extras y estado guardado);
//   - Call<…> en las interfaces de Retrofit de network/: cada tipo de la respuesta.
// No sustituye a la prueba de humo de la release (app-release-r8), que es la que lo ve de verdad.
// ============================================================
public class TiposGsonConservadosTest {

    private static final Path JAVA = Paths.get("src", "main", "java");
    private static final Path RED = JAVA.resolve(Paths.get("es", "pmdm", "gymprofit", "network"));
    private static final Path REGLAS = Paths.get("proguard-rules.pro");
    private static final String RAIZ = "es.pmdm.gymprofit.";
    private static final String MODELO = RAIZ + "model.";

    private static final Pattern FROM_JSON =
            Pattern.compile("fromJson\\([^;]*?,\\s*([A-Za-z_][\\w.]*?)(?:\\[\\])?\\.class\\s*\\)");
    private static final Pattern CALL = Pattern.compile("Call<([^;(){}<>]+(?:<[^;(){}<>]+>)?)>");  // un nivel: List<X>, Map<A, B>
    private static final Pattern NOMBRE = Pattern.compile("[A-Za-z_][\\w.]*");
    private static final Set<String> AJENOS = new HashSet<>(Arrays.asList(
            "Call", "Void", "String", "Integer", "Long", "Double", "Boolean", "Object", "List", "Map",
            "ResponseBody", "T", "Response"));

    @Test
    public void r8ConservaCadaTipoDeGsonFueraDeModel() throws IOException {
        Set<String> tipos = new TreeSet<>();
        try (Stream<Path> s = Files.walk(JAVA)) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".java"))::iterator) {
                String codigo = leer(p);
                Matcher m = FROM_JSON.matcher(codigo);
                while (m.find()) anadir(tipos, codigo, m.group(1));
                if (!p.startsWith(RED)) continue;
                Matcher c = CALL.matcher(codigo);
                while (c.find()) {
                    Matcher n = NOMBRE.matcher(c.group(1));
                    while (n.find()) anadir(tipos, codigo, n.group());
                }
            }
        }
        assertTrue("se esperaban tipos de Gson en el código", tipos.size() > 20);

        String reglas = leer(REGLAS);
        List<String> sinRegla = new ArrayList<>();
        for (String t : tipos) {
            if (t.startsWith(MODELO)) continue;
            if (!reglas.contains("-keep class " + t + " {")) sinRegla.add(t);
        }
        if (!sinRegla.isEmpty()) {
            fail("Tipos que pasan por Gson fuera de " + MODELO + "** y sin regla en proguard-rules.pro: "
                    + sinRegla);
        }
    }

    // Resuelve un nombre del código (Alimento, Anadidos.Anadido, es.pmdm…X) a su nombre binario
    // (con $ para las anidadas) y lo añade si es de la app. Lo de fuera de la app no se mira.
    private static void anadir(Set<String> tipos, String codigo, String nombre) {
        if (AJENOS.contains(nombre) || nombre.startsWith("java.")) return;
        String fqcn;
        if (nombre.startsWith(RAIZ)) {
            fqcn = nombre;
        } else {
            String[] partes = nombre.split("\\.", 2);
            Matcher imp = Pattern.compile("import\\s+(" + Pattern.quote(RAIZ) + "[\\w.]*\\."
                    + Pattern.quote(partes[0]) + ")\\s*;").matcher(codigo);
            if (imp.find()) {
                fqcn = imp.group(1) + (partes.length > 1 ? "." + partes[1] : "");
            } else {
                Matcher paq = Pattern.compile("package\\s+([\\w.]+)\\s*;").matcher(codigo);
                if (!paq.find()) return;
                fqcn = paq.group(1) + "." + nombre;
            }
        }
        if (fqcn.startsWith(RAIZ)) tipos.add(binario(fqcn));
    }

    // es.pmdm.gymprofit.utils.Anadidos.Anadido → es.pmdm.gymprofit.utils.Anadidos$Anadido: a
    // partir del primer segmento en mayúscula, el resto son clases anidadas.
    private static String binario(String fqcn) {
        String[] s = fqcn.split("\\.");
        StringBuilder b = new StringBuilder();
        boolean enClase = false;
        for (int i = 0; i < s.length; i++) {
            if (i > 0) b.append(enClase ? '$' : '.');
            b.append(s[i]);
            if (Character.isUpperCase(s[i].charAt(0))) enClase = true;
        }
        return b.toString();
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
