package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

// ============================================================
// IdiomaPeticionesTest — la cabecera Accept-Language es el idioma en que se pinta la app, no el
// Locale por defecto del proceso (GP-251).
//
// Con la app en inglés y el sistema en español, un servicio de accesibilidad del propio proceso
// devolvía el Locale por defecto al español y la API contestaba en español: «Press de banca» en
// «Frente a la última vez» junto a «Bench press» en el récord. El idioma de la app en Android
// manda sobre el sistema.
// ============================================================
public class IdiomaPeticionesTest {

    @Test
    public void conLaAppEnInglesYElSistemaEnEspanolSePideEnIngles() {
        assertEquals("en", IdiomaApp.peticiones("", Collections.singletonList("en"), Arrays.asList("es-ES")));
    }

    @Test
    public void elIdiomaElegidoEnLaAppMandaSobreTodo() {
        assertEquals("es", IdiomaApp.peticiones("es", Collections.singletonList("en"), Arrays.asList("en-US")));
        assertEquals("en", IdiomaApp.peticiones("en", Collections.emptyList(), Arrays.asList("es-ES")));
    }

    @Test
    public void sinIdiomaDeLaAppVaElDelSistemaEntreLosDos() {
        assertEquals("en", IdiomaApp.peticiones("", Collections.emptyList(), Arrays.asList("fr-FR", "en-GB")));
        assertEquals("es", IdiomaApp.peticiones("", Collections.emptyList(), Arrays.asList("de-DE")));
    }
}
