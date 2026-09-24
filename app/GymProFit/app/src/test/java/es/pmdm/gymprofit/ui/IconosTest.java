package es.pmdm.gymprofit.ui;

import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// ============================================================
// IconosTest — GP-080: un solo sistema de iconos.
//
// Todo icono de interfaz sale de Material Symbols Rounded 400, importado del
// paquete @material-symbols/svg-400 y llamado ic_ms_<símbolo>[_fill]. Este test
// fija dos cosas leyendo res/drawable:
//   · no hay ningún ic_* fuera de la familia, salvo las excepciones decididas;
//   · cada ic_ms_* dice de qué versión del paquete viene, es decir, se importó
//     y no se dibujó a mano.
// ============================================================
public class IconosTest {

    private static final Path DRAWABLE = Paths.get("src", "main", "res", "drawable");

    // Fuera de alcance por decisión del propietario (GP-080): las banderas de
    // idioma, las ilustraciones del cuerpo (SiluetaMuscularView carga ic_musculo_*
    // por nombre), el medidor del onboarding (GP-078) y las capas del icono de la app.
    private static final Pattern EXCEPCIONES = Pattern.compile(
            "ic_(flag|body|musculo|silueta|nivel|launcher)_.*");

    @Test
    public void todo_icono_es_de_la_familia_o_una_excepcion_decidida() throws IOException {
        List<String> fuera = new ArrayList<>();
        try (DirectoryStream<Path> ficheros = Files.newDirectoryStream(DRAWABLE, "ic_*.xml")) {
            for (Path f : ficheros) {
                String nombre = f.getFileName().toString().replace(".xml", "");
                if (!nombre.startsWith("ic_ms_") && !EXCEPCIONES.matcher(nombre).matches()) {
                    fuera.add(nombre);
                }
            }
        }
        if (!fuera.isEmpty()) fail("Iconos fuera de Material Symbols:\n" + String.join("\n", fuera));
    }

    @Test
    public void cada_symbol_viene_del_paquete_oficial() throws IOException {
        List<String> sinOrigen = new ArrayList<>();
        try (DirectoryStream<Path> ficheros = Files.newDirectoryStream(DRAWABLE, "ic_ms_*.xml")) {
            for (Path f : ficheros) {
                String xml = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
                if (!xml.contains("@material-symbols/svg-400@") || !xml.contains("android:translateY=\"960\"")) {
                    sinOrigen.add(f.getFileName().toString());
                }
            }
        }
        if (!sinOrigen.isEmpty()) fail("ic_ms_* sin la marca de importación:\n" + String.join("\n", sinOrigen));
    }
}
