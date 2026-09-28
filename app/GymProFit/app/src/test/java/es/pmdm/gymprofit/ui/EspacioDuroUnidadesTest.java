package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
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
// EspacioDuroUnidadesTest — GP-123: una cifra no se separa de su unidad.
//
// Con la letra a 1,3, «72,5 kg» partía línea en Récords: «72,5» al final de una y «kg»
// al principio de la siguiente. Entre cifra y unidad va un espacio duro ( ), que
// Android no parte. Aquí se leen los recursos de texto de todos los idiomas y se falla
// si una cifra, o el hueco de una cifra (%d, %1$.1f…), va seguida de un espacio normal
// y una unidad. Pegada sin espacio («%.1fg») tampoco vale: se lee mal y es la misma
// unidad escrita de dos maneras.
// ============================================================
public class EspacioDuroUnidadesTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final String UNIDADES = "(kg|g|kcal|min|cm|h|L|reps)";
    private static final String CIFRA = "(%(?:\\d+\\$)?[.\\d]*[dfs]|\\d)";
    // Cifra + espacio normal + unidad, o cifra + unidad sin nada en medio.
    private static final Pattern PARTIBLE = Pattern.compile(CIFRA + "( |(?<=f))" + UNIDADES + "\\b");

    @Test
    public void ninguna_unidad_se_separa_de_su_cifra() throws IOException {
        List<Path> ficheros;
        try (Stream<Path> s = Files.walk(RES)) {
            ficheros = s.filter(p -> p.getFileName().toString().equals("strings.xml")).collect(Collectors.toList());
        }
        List<String> fallos = new ArrayList<>();
        for (Path f : ficheros) {
            String[] lineas = new String(Files.readAllBytes(f), StandardCharsets.UTF_8).split("\n");
            for (int i = 0; i < lineas.length; i++) {
                if (!partibles(lineas[i]).isEmpty()) fallos.add(RES.relativize(f) + ":" + (i + 1) + "  " + lineas[i].trim());
            }
        }
        if (!fallos.isEmpty()) {
            fail("Cifra y unidad sin espacio duro (\\u00A0):\n" + String.join("\n", fallos));
        }
    }

    // El detector reconoce los casos; si no, el test de arriba pasaría siempre.
    @Test
    public void el_detector_distingue_los_casos() {
        assertEquals(1, partibles("<string name=\"a\">%.1f kg</string>").size());
        assertEquals(1, partibles("<string name=\"a\">%1$d min de entrenamiento</string>").size());
        assertEquals(1, partibles("<string name=\"a\">Calorías / 100 g</string>").size());
        assertEquals(1, partibles("<string name=\"a\">%2$.1fg prot</string>").size());
        assertEquals(2, partibles("<string name=\"a\">%1$d h %2$d min</string>").size());
        assertEquals(0, partibles("<string name=\"a\">%.1f\\u00A0kg</string>").size());
        assertEquals(0, partibles("<string name=\"a\">%1$d\\u00A0h %2$d\\u00A0min</string>").size());
        assertEquals("un sustantivo contado no es una unidad", 0, partibles("<string name=\"a\">30 días</string>").size());
        assertEquals(0, partibles("<string name=\"a\">%s gramos</string>").size());
    }

    private static List<String> partibles(String linea) {
        List<String> hallados = new ArrayList<>();
        Matcher m = PARTIBLE.matcher(linea);
        while (m.find()) hallados.add(m.group());
        return hallados;
    }
}
