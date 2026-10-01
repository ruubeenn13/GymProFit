package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

// ============================================================
// SesionInvitadoTest — GP-150: el invitado sale de la app.
// ============================================================
public class SesionInvitadoTest {

    @Test
    public void soloUnaSesionConRolDeInvitadoEsDeInvitado() {
        assertTrue(SesionInvitado.esDeInvitado(true, "ROLE_GUEST"));
        assertFalse(SesionInvitado.esDeInvitado(true, "ROLE_USER"));
        assertFalse(SesionInvitado.esDeInvitado(true, "ROLE_ADMIN"));
        assertFalse(SesionInvitado.esDeInvitado(true, null));
        assertFalse("sin sesión no hay nada que cerrar", SesionInvitado.esDeInvitado(false, "ROLE_GUEST"));
    }

    @Test
    public void ningunaPantallaPreguntaYaPorElInvitado() throws IOException {
        // Las ramas de invitado se fueron: solo SesionInvitado sabe que existió.
        Path java = Paths.get("src", "main", "java");
        try (Stream<Path> s = Files.walk(java)) {
            s.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.getFileName().toString().equals("SesionInvitado.java"))
                    .forEach(p -> {
                        String codigo;
                        try {
                            codigo = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        for (String resto : new String[]{"isGuest()", "verificarAccesoRegistrado(", "auth/guest",
                                "ROLE_GUEST", "error_solo_usuarios_registrados", "login_error_invitado"}) {
                            assertFalse(p + " sigue con " + resto, codigo.contains(resto));
                        }
                    });
        }
    }

    @Test
    public void susTextosYaNoEstan() throws IOException {
        for (String carpeta : new String[]{"values", "values-en"}) {
            String xml = new String(Files.readAllBytes(Paths.get("src", "main", "res", carpeta, "strings.xml")),
                    StandardCharsets.UTF_8);
            for (String nombre : new String[]{"login_entrar_invitado", "login_error_invitado",
                    "error_solo_usuarios_registrados"}) {
                assertFalse(carpeta + ": sigue " + nombre, xml.contains("name=\"" + nombre + "\""));
            }
        }
    }
}
