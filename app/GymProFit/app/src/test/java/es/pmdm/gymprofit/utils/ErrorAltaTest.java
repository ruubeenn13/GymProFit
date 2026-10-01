package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// ErrorAltaTest — cada error del alta, a su campo (GP-103, tablero 10 del lienzo).
// ============================================================
public class ErrorAltaTest {

    @Test
    public void sinRespuestaEsSinRed() {
        assertEquals(ErrorAlta.Tipo.SIN_RED, ErrorAlta.de(-1, null));
    }

    @Test
    public void elCorreoEnUsoVaAlCorreo() {
        assertEquals(ErrorAlta.Tipo.CORREO_EN_USO,
                ErrorAlta.de(400, "{\"code\":400,\"message\":\"El email ya está en uso\",\"cause\":\"EMAIL_EN_USO\"}"));
    }

    @Test
    public void lasReglasDeLaContrasenaVanALaContrasena() {
        assertEquals(ErrorAlta.Tipo.PASSWORD_COMUN, ErrorAlta.de(400, "{\"cause\":\"PASSWORD_COMUN\"}"));
        assertEquals(ErrorAlta.Tipo.PASSWORD_CONTIENE_NOMBRE,
                ErrorAlta.de(400, "{\"cause\":\"PASSWORD_CONTIENE_NOMBRE\"}"));
    }

    @Test
    public void elRestoVaPorElAvisoComun() {
        assertEquals(ErrorAlta.Tipo.OTRO, ErrorAlta.de(429, "{\"cause\":\"EMAIL_EN_USO\"}"));
        assertEquals(ErrorAlta.Tipo.OTRO, ErrorAlta.de(500, null));
        assertEquals(ErrorAlta.Tipo.OTRO, ErrorAlta.de(400, "{\"cause\":\"EDAD_MINIMA\"}"));
    }
}
