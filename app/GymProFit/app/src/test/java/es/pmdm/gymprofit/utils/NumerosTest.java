package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.math.BigDecimal;

// ============================================================
// NumerosTest — regresión de los dos fallos que cerraban la app.
//
// Ambos se descubrieron en la auditoría de septiembre de 2026 y son fáciles de
// reintroducir, porque el código que los provocaba (Integer.parseInt y
// Double.parseDouble directos) parece inofensivo al leerlo.
// ============================================================
public class NumerosTest {

    // ---- Enteros: el desbordamiento cerraba la app al crear una rutina ----

    @Test
    public void entero_numero_mas_largo_que_un_int_no_revienta() {
        // Antes: Integer.parseInt("99999999999") -> NumberFormatException -> cierre.
        assertNull(Numeros.entero("99999999999", 5, 300));
    }

    @Test
    public void entero_dentro_de_rango() {
        assertEquals(Integer.valueOf(45), Numeros.entero("45", 5, 300));
    }

    @Test
    public void entero_en_los_limites_se_acepta() {
        assertEquals(Integer.valueOf(5), Numeros.entero("5", 5, 300));
        assertEquals(Integer.valueOf(300), Numeros.entero("300", 5, 300));
    }

    @Test
    public void entero_fuera_de_rango_se_rechaza() {
        assertNull(Numeros.entero("4", 5, 300));
        assertNull(Numeros.entero("301", 5, 300));
    }

    @Test
    public void entero_texto_vacio_nulo_o_no_numerico() {
        assertNull(Numeros.entero("", 5, 300));
        assertNull(Numeros.entero(null, 5, 300));
        assertNull(Numeros.entero("treinta", 5, 300));
    }

    @Test
    public void entero_admite_espacios_alrededor() {
        assertEquals(Integer.valueOf(60), Numeros.entero("  60  ", 5, 300));
    }

    // ---- Decimales: la coma del teclado español cerraba la app ----

    @Test
    public void decimal_acepta_coma_como_separador() {
        // Antes: Double.parseDouble("175,5") -> NumberFormatException -> cierre.
        Double altura = Numeros.decimal("175,5", 100, 250);
        assertNotNull(altura);
        assertEquals(175.5, altura, 0.0001);
    }

    @Test
    public void decimal_acepta_punto_como_separador() {
        assertEquals(75.5, Numeros.decimal("75.5", 30, 300), 0.0001);
    }

    @Test
    public void decimal_rechaza_dos_separadores() {
        assertNull(Numeros.decimal("1,75.5", 100, 250));
    }

    @Test
    public void decimal_altura_en_metros_queda_fuera_de_rango() {
        // Caso real: el usuario escribe su estatura en metros. No es un error de
        // formato, es un valor imposible en centímetros, y hay que detectarlo
        // para poder sugerirle el valor correcto.
        assertNull(Numeros.decimal("1,75", 100, 250));
        assertEquals(1.75, Numeros.decimal("1,75", 1, 2.5), 0.0001);
    }

    @Test
    public void decimal_fuera_de_rango_se_rechaza() {
        assertNull(Numeros.decimal("29", 30, 300));
        assertNull(Numeros.decimal("301", 30, 300));
    }

    // ---- BigDecimal: mismo problema en mediciones corporales ----

    @Test
    public void exacto_acepta_coma_y_conserva_los_decimales() {
        // Antes: new BigDecimal("75,5") -> NumberFormatException, y el mensaje que
        // se enseñaba era "error de conexión con el servidor".
        BigDecimal peso = Numeros.exacto("75,5", 30, 300);
        assertNotNull(peso);
        assertEquals(new BigDecimal("75.5"), peso);
    }

    @Test
    public void exacto_rechaza_valores_imposibles() {
        assertNull(Numeros.exacto("750", 30, 300));
        assertNull(Numeros.exacto("0,1", 1, 70));
    }

    @Test
    public void exacto_texto_no_numerico() {
        assertNull(Numeros.exacto("setenta y cinco", 30, 300));
        assertNull(Numeros.exacto("", 30, 300));
    }
}
