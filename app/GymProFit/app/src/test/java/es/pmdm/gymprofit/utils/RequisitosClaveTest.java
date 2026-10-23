package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// RequisitosClaveTest — los requisitos que se marcan al escribir la contraseña nueva
// (GP-216, tablero 20, DEC-034): 8 caracteres, distinta de la actual, 72 bytes como mucho
// y, sin ser obligatoria, mejor una frase que una palabra. Y el mismo patrón para el correo.
// ============================================================
public class RequisitosClaveTest {

    @Test
    public void lasContrasenasCortasNoCumplenLaLongitud() {
        assertFalse(RequisitosClave.de("1234567", "").longitud);
        assertTrue(RequisitosClave.de("12345678", "").longitud);
    }

    @Test
    public void laLongitudSeCuentaEnCaracteresNoEnUnidadesUtf16() {
        // Siete emojis son siete caracteres (14 unidades UTF-16): no llegan a ocho.
        String siete = "😀".repeat(7);
        assertFalse(RequisitosClave.de(siete, "").longitud);
        assertTrue(RequisitosClave.de(siete + "a", "").longitud);
    }

    @Test
    public void noPuedeSerLaMismaQueLaActual() {
        assertFalse(RequisitosClave.de("misma-clave-1", "misma-clave-1").distinta);
        assertTrue(RequisitosClave.de("otra-clave-12", "misma-clave-1").distinta);
    }

    @Test
    public void sinContrasenaActualEscritaNoSeAcusaDeSerLaMisma() {
        assertTrue(RequisitosClave.de("otra-clave-12", "").distinta);
    }

    @Test
    public void masDeSetentaYDosBytesNoCabe() {
        // 36 eñes son 72 bytes en UTF-8; 37, 74.
        assertTrue(RequisitosClave.de("ñ".repeat(36), "").cabe);
        assertFalse(RequisitosClave.de("ñ".repeat(37), "").cabe);
    }

    @Test
    public void losEspaciosDeLosExtremosNoCuentan() {
        // DEC-034: la app recorta antes de enviar.
        assertFalse(RequisitosClave.de("   1234567   ", "").longitud);
        assertTrue(RequisitosClave.de("  12345678  ", "").longitud);
    }

    @Test
    public void esAceptableSoloConLosTresObligatorios() {
        assertTrue(RequisitosClave.de("ganar musculo en invierno", "vieja").aceptable());
        assertFalse(RequisitosClave.de("corta", "vieja").aceptable());
        assertFalse(RequisitosClave.de("", "vieja").aceptable());
        assertFalse(RequisitosClave.de("vieja-clave-1", "vieja-clave-1").aceptable());
        assertFalse(RequisitosClave.de("ñ".repeat(40), "vieja").aceptable());
    }

    @Test
    public void laFraseEsUnExtraOpcionalQueNoImpideGuardar() {
        RequisitosClave palabra = RequisitosClave.de("contrasena99", "vieja");
        RequisitosClave frase = RequisitosClave.de("ganar musculo en invierno", "vieja");
        assertFalse(palabra.frase);
        assertTrue(palabra.aceptable());
        assertTrue(frase.frase);
    }

    @Test
    public void laFortalezaSubeEnCuatroSegmentos() {
        assertEquals(0, RequisitosClave.de("", "").segmentos);
        assertEquals(1, RequisitosClave.de("abc", "").segmentos);
        assertEquals(2, RequisitosClave.de("abcdefgh", "").segmentos);
        assertEquals(3, RequisitosClave.de("abcdefghijklmn", "").segmentos);
        assertEquals(4, RequisitosClave.de("ganar musculo en invierno", "").segmentos);
    }

    @Test
    public void unaContrasenaMuyLargaPeroInvalidaNoLlenaLaBarra() {
        assertTrue(RequisitosClave.de("ñ".repeat(40), "").segmentos < 4);
    }

    // ── El mismo patrón para el correo ──────────────────────────────────────────

    @Test
    public void elCorreoNuevoTieneForma() {
        assertTrue(RequisitosClave.correo("dani@gymprofit.app", "otro@gymprofit.app").forma);
        assertFalse(RequisitosClave.correo("dani@", "otro@gymprofit.app").forma);
        assertFalse(RequisitosClave.correo("dani gymprofit.app", "").forma);
        assertFalse(RequisitosClave.correo("", "").forma);
    }

    @Test
    public void elCorreoNuevoNoPuedeSerElActualAunqueCambienLasMayusculas() {
        assertFalse(RequisitosClave.correo("Dani@GymProFit.app", "dani@gymprofit.app").distinto);
        assertTrue(RequisitosClave.correo("nuevo@gymprofit.app", "dani@gymprofit.app").distinto);
        // Sin saber el actual (aún no ha cargado) no se acusa a nadie.
        assertTrue(RequisitosClave.correo("nuevo@gymprofit.app", null).distinto);
    }

    @Test
    public void elCorreoNoPasaDeCien() {
        String largo = "a".repeat(96) + "@x.es";
        assertFalse(RequisitosClave.correo(largo, "").cabeCorreo);
        assertTrue(RequisitosClave.correo("a@x.es", "").cabeCorreo);
    }

    @Test
    public void elCorreoEsAceptableConLosTres() {
        assertTrue(RequisitosClave.correo("nuevo@gymprofit.app", "dani@gymprofit.app").aceptable());
        assertFalse(RequisitosClave.correo("dani@gymprofit.app", "dani@gymprofit.app").aceptable());
        assertFalse(RequisitosClave.correo("nuevo", "dani@gymprofit.app").aceptable());
    }
}
