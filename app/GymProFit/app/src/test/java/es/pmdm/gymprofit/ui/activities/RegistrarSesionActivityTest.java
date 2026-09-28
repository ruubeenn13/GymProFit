package es.pmdm.gymprofit.ui.activities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

// ============================================================
// RegistrarSesionActivityTest — GP-016: la pantalla pinta, no guarda ni llama a la red.
//
// El borrador vive en RegistrarSesionViewModel y la red en RegistroSesionRepositorio.
// Si la Activity vuelve a tener su propia llamada o su propia clave, lo tecleado y la
// clave vuelven a perderse al girar. Se comprueba leyendo el código, como el resto de
// reglas de este tipo (OnFailVacioTest, AvisoDescartarUsoTest).
// ============================================================
public class RegistrarSesionActivityTest {

    @Test
    public void la_activity_no_llama_a_la_red_ni_guarda_la_clave() throws IOException {
        String codigo = new String(Files.readAllBytes(Paths.get("src", "main", "java", "es", "pmdm",
                "gymprofit", "ui", "activities", "RegistrarSesionActivity.java")), StandardCharsets.UTF_8);

        for (String prohibido : new String[]{"ApiClient", "RutinaApi", "SesionApi", ".enqueue(", "UUID"}) {
            assertFalse("RegistrarSesionActivity usa " + prohibido, codigo.contains(prohibido));
        }
        assertTrue(codigo.contains("RegistrarSesionViewModel"));
        assertTrue("sigue con el aviso de descartar (GP-098)", codigo.contains("AvisoDescartar.instalar("));
    }
}
