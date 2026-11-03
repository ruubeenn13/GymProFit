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

// ============================================================
// NombresFilasTest — nombres propios en las filas de Medidas y en las papeleras del historial
// (GP-227, DEC-019).
//
// Las filas de Medidas no tenían nombre: TalkBack juntaba «Peso» y «79,2 kg» y leía la unidad
// letra a letra. Las papeleras del historial decían todas «Eliminar sesión». Aquí se fija que cada
// fila pulsable de fragment_medidas.xml se nombra en MedidasFragment y que la papelera dice la
// rutina y la fecha, en los dos idiomas.
// ============================================================
public class NombresFilasTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path UI = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit", "ui");

    @Test
    public void cadaFilaDeMedidasTieneNombre() throws IOException {
        String layout = leer(RES.resolve("layout/fragment_medidas.xml"));
        String codigo = leer(UI.resolve("fragments/MedidasFragment.java"));
        Matcher m = Pattern.compile("android:id=\"@\\+id/(row\\w+)\"[^>]*?android:clickable=\"true\"").matcher(layout);
        List<String> filas = new ArrayList<>(), sinNombre = new ArrayList<>();
        while (m.find()) {
            String fila = m.group(1);
            filas.add(fila);
            boolean nombrada = codigo.contains("nombrar(R.id." + fila + ",")
                    || codigo.contains("findViewById(R.id." + fila + ").setContentDescription(");
            if (!nombrada) sinNombre.add(fila);
        }
        assertTrue("se esperaban las filas de Medidas, y no se encontró ninguna", filas.size() >= 9);
        if (!sinNombre.isEmpty()) fail("Filas de Medidas sin nombre propio: " + sinNombre);
    }

    @Test
    public void cadaPapeleraDiceQueSesionBorra() throws IOException {
        String adapter = leer(UI.resolve("adapters/SesionAdapter.java"));
        assertTrue(adapter.contains("btnEliminar.setContentDescription("));
        assertTrue(adapter.contains("R.string.sesiones_eliminar_a11y"));
        for (String tema : new String[]{ "values", "values-en" }) {
            String xml = leer(RES.resolve(tema + "/strings.xml"));
            Matcher m = Pattern.compile("<string name=\"sesiones_eliminar_a11y\">([^<]*)</string>").matcher(xml);
            assertTrue(tema + ": falta sesiones_eliminar_a11y", m.find());
            assertTrue(tema + ": la papelera dice la rutina y la fecha",
                    m.group(1).contains("%1$s") && m.group(1).contains("%2$s"));
        }
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
