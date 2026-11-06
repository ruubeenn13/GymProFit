package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// IdiomaMarcadoTest — qué idioma marca el diálogo de idioma (GP-233).
//
// Sin idioma elegido en la app, se ve el de los recursos: inglés con el sistema en
// inglés, español con el sistema en español o en un idioma que la app no tiene. El
// diálogo marcaba «Español» siempre que no hubiera nada elegido.
// ============================================================
public class IdiomaMarcadoTest {

    @Test
    public void sin_elegir_y_la_app_en_ingles_marca_ingles() {
        assertEquals("en", PreferencesManager.idiomaMarcado("", true));
        assertEquals("en", PreferencesManager.idiomaMarcado(null, true));
    }

    @Test
    public void sin_elegir_y_la_app_en_espanol_marca_espanol() {
        assertEquals("es", PreferencesManager.idiomaMarcado("", false));
    }

    @Test
    public void lo_elegido_manda_sobre_los_recursos() {
        assertEquals("es", PreferencesManager.idiomaMarcado("es", true));
        assertEquals("en", PreferencesManager.idiomaMarcado("en", false));
    }
}
