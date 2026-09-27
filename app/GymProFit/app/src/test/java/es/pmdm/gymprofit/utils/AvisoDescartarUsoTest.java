package es.pmdm.gymprofit.utils;

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

// ============================================================
// AvisoDescartarUsoTest — GP-108: los formularios que quedaban sin aviso lo llevan.
//
// Editar perfil, cambiar contraseña y correo, crear alimento, la cantidad al añadir
// un alimento y el diálogo de mediciones. Salir con algo escrito pregunta antes de
// tirarlo; sin nada escrito, sale directo (lo prueba AvisoDescartarTest). Aquí se
// comprueba, leyendo el código, que cada pantalla engancha el aviso: sin él, la
// regla existe y nadie la usa. El registro, recuperar contraseña, el onboarding y la
// administración quedan fuera a propósito: se rehacen o salen de la app.
// ============================================================
public class AvisoDescartarUsoTest {

    private static final Path JAVA = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit");

    @Test
    public void los_formularios_enganchan_el_aviso() throws IOException {
        List<String> faltan = new ArrayList<>();
        for (String pantalla : new String[]{"EditarPerfilActivity", "CambiarPasswordActivity",
                "CambiarCorreoActivity", "CrearAlimentoActivity"}) {
            if (!leer(JAVA.resolve("ui/activities/" + pantalla + ".java")).contains("AvisoDescartar.instalar(")) {
                faltan.add(pantalla);
            }
        }
        if (!leer(JAVA.resolve("ui/activities/AnadirAlimentoActivity.java")).contains("AvisoDescartar.instalarEnDialogo(")) {
            faltan.add("AnadirAlimentoActivity (diálogo de cantidad)");
        }
        if (!leer(JAVA.resolve("utils/InputDialog.java")).contains("AvisoDescartar.instalarEnDialogo(")) {
            faltan.add("InputDialog (diálogo de mediciones)");
        }
        if (!faltan.isEmpty()) fail("Sin aviso de descartar:\n" + String.join("\n", faltan));
    }

    @Test
    public void varios_campos_cuentan_como_cambio_si_cambia_cualquiera() {
        String[] iniciales = {"78.5", "180", "30"};
        assertFalse(AvisoDescartar.distintos(iniciales, "78.5", "180", "30"));
        assertFalse(AvisoDescartar.distintos(iniciales, "78.5 ", "180", "30"));
        assertTrue(AvisoDescartar.distintos(iniciales, "79", "180", "30"));
        assertTrue(AvisoDescartar.distintos(iniciales, "78.5", "180", ""));
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
