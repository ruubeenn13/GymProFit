package es.pmdm.gymprofit.ui;

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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import es.pmdm.gymprofit.utils.NavTabs;

// ============================================================
// AtrasTest — GP-097: atrás vuelve a Inicio y no sale de la app.
//
// Dos reglas:
//   · Desde cualquier pestaña que no sea Inicio, atrás vuelve a Inicio; solo desde
//     Inicio sale de la app. Es lo que decide si el callback de MainActivity está activo.
//   · Ningún onBackPressed(), onKeyDown() ni KEYCODE_BACK en la app. Con targetSdk 36,
//     Android 16 no llama a onBackPressed(): el que había en el resumen de crear rutina
//     estaba muerto y el gesto cerraba la pantalla sin devolver la lista. Atrás se
//     gestiona con OnBackPressedCallback. Pedirle al dispatcher que retroceda
//     (getOnBackPressedDispatcher().onBackPressed()) sí vale: no es un override.
//
// El callback en sí no se puede probar aquí sin un marco de Android (no hay
// Robolectric en el proyecto); se comprueba en el dispositivo, en API 34 y 36.
// ============================================================
public class AtrasTest {

    private static final Path JAVA = Paths.get("src", "main", "java");
    private static final Pattern PROHIBIDO = Pattern.compile(
            "(?<!Dispatcher\\(\\)\\.)\\bonBackPressed\\s*\\(|\\bonKeyDown\\s*\\(|KEYCODE_BACK");

    @Test
    public void desde_inicio_atras_sale() {
        assertFalse(NavTabs.atrasVuelveAInicio(NavTabs.HOME));
    }

    @Test
    public void desde_las_demas_pestanas_atras_vuelve_a_inicio() {
        for (int tab : new int[]{NavTabs.RUTINAS, NavTabs.EJERCICIOS, NavTabs.NUTRICION, NavTabs.PERFIL}) {
            assertTrue("pestaña " + tab, NavTabs.atrasVuelveAInicio(tab));
        }
    }

    @Test
    public void ningun_onBackPressed_ni_onKeyDown() throws IOException {
        List<Path> ficheros;
        try (Stream<Path> s = Files.walk(JAVA)) {
            ficheros = s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        List<String> fallos = new ArrayList<>();
        for (Path f : ficheros) {
            List<String> lineas = Files.readAllLines(f, StandardCharsets.UTF_8);
            for (int i = 0; i < lineas.size(); i++) {
                String l = lineas.get(i).trim();
                if (l.startsWith("//") || l.startsWith("*")) continue;
                if (PROHIBIDO.matcher(l).find()) fallos.add(JAVA.relativize(f) + ":" + (i + 1) + "  " + l);
            }
        }
        if (!fallos.isEmpty()) fail("Atrás gestionado fuera de OnBackPressedCallback:\n" + String.join("\n", fallos));
    }
}
