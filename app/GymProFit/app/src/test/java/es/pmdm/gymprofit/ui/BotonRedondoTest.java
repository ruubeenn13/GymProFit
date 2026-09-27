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
import java.util.stream.Stream;

// ============================================================
// BotonRedondoTest — GP-118: los botones redondos de cabecera son todos iguales.
//
// El engranaje de Progreso llevaba un anillo naranja de 2 dp que en el lienzo era el
// resaltado de la selección, no parte del diseño. Se le había dado un fondo propio
// que pisaba el del estilo. Este test lee res/layout y exige que ninguna vista con
// el estilo Widget.GymProFit.BotonRedondo cambie su fondo: el círculo es el del
// estilo, en todas.
// ============================================================
public class BotonRedondoTest {

    private static final Path LAYOUT = Paths.get("src", "main", "res", "layout");
    private static final Pattern ELEMENTO = Pattern.compile("<([\\w.]+)\\b([^<>]*?)/?>");

    @Test
    public void ningun_boton_redondo_cambia_su_fondo() throws IOException {
        List<String> fallos = new ArrayList<>();
        int vistos = 0;
        try (Stream<Path> ficheros = Files.list(LAYOUT)) {
            for (Path f : (Iterable<Path>) ficheros.filter(p -> p.toString().endsWith(".xml"))::iterator) {
                Matcher m = ELEMENTO.matcher(new String(Files.readAllBytes(f), StandardCharsets.UTF_8));
                while (m.find()) {
                    String atributos = m.group(2);
                    if (!atributos.contains("style=\"@style/Widget.GymProFit.BotonRedondo\"")) continue;
                    vistos++;
                    if (atributos.contains("android:background=")) {
                        fallos.add(f.getFileName() + ": " + m.group(1) + " cambia el fondo del botón redondo");
                    }
                }
            }
        }
        assertTrue("no se ha encontrado ningún botón redondo: ¿ha cambiado el estilo?", vistos > 0);
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }
}
