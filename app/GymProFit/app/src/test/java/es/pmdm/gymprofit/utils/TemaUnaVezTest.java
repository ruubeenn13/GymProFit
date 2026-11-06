package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// ============================================================
// TemaUnaVezTest — el tema guardado se fija una vez, al abrir el proceso (GP-234).
//
// Cada pantalla llamaba a applyTheme() en su onCreate, muchas después de super.onCreate.
// Si una era la primera del proceso y el tema guardado no era el del sistema, AppCompat
// la creaba con el del sistema y la volvía a crear con el guardado. Ahora lo fija
// GymProFitApp antes de la primera pantalla, y solo se vuelve a fijar al cambiarlo en
// Ajustes (PreferencesManager.cambiarTema). El recuento de creaciones en el emulador va
// en el informe del lote; aquí se vigila que nadie lo vuelva a poner en una pantalla.
// ============================================================
public class TemaUnaVezTest {

    private static final Path JAVA = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit");

    @Test
    public void ninguna_pantalla_fija_el_tema_al_crearse() throws IOException {
        List<String> culpables = new ArrayList<>();
        List<Path> ficheros;
        try (Stream<Path> s = Files.walk(JAVA)) {
            ficheros = s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        for (Path f : ficheros) {
            String nombre = f.getFileName().toString();
            if (nombre.equals("GymProFitApp.java") || nombre.equals("PreferencesManager.java")) continue;
            // Un Drawable que recibe el tema de su pantalla (Drawable.applyTheme, BurbujaProfi) no
            // fija nada: se quitan su declaración y la llamada a la de su padre.
            String codigo = new String(Files.readAllBytes(f), StandardCharsets.UTF_8)
                    .replace("void applyTheme(", "").replace("super.applyTheme(", "");
            if (codigo.contains("applyTheme(") || codigo.contains("setDefaultNightMode(")) culpables.add(nombre);
        }
        assertTrue("Fijan el tema por su cuenta: " + culpables, culpables.isEmpty());
    }

    @Test
    public void la_aplicacion_lo_fija_al_abrir_el_proceso() throws IOException {
        String app = new String(Files.readAllBytes(JAVA.resolve("GymProFitApp.java")), StandardCharsets.UTF_8);
        assertTrue(app.contains("setDefaultNightMode(PreferencesManager.temaGuardado(this))"));
    }
}
