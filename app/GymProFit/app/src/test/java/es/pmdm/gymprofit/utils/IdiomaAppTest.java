package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GP-240: sin idioma elegido, la app sigue la lista del sistema entre los suyos (español e
 * inglés) y, si no tiene ninguno, español; y fechas, números y librerías van en ese idioma.
 */
public class IdiomaAppTest {

    @Test
    public void catalanYEspanolEsEspanol() {
        assertEquals("es", IdiomaApp.resolver("", Arrays.asList("ca-ES", "es-ES")));
    }

    @Test
    public void soloFrancesEsEspanol() {
        assertEquals("es", IdiomaApp.resolver(null, Collections.singletonList("fr-FR")));
    }

    @Test
    public void francesEInglesEsIngles() {
        assertEquals("en", IdiomaApp.resolver("", Arrays.asList("fr-FR", "en-US")));
    }

    @Test
    public void alemanEInglesEsIngles() {
        assertEquals("en", IdiomaApp.resolver("", Arrays.asList("de-DE", "en-GB")));
    }

    @Test
    public void elPrimeroDeLosSuyosManda() {
        assertEquals("en", IdiomaApp.resolver("", Arrays.asList("fr", "en-AU", "es-MX")));
        assertEquals("es", IdiomaApp.resolver("", Arrays.asList("pt-BR", "es-419", "en")));
    }

    @Test
    public void sinListaEsEspanol() {
        assertEquals("es", IdiomaApp.resolver("", Collections.<String>emptyList()));
        assertEquals("es", IdiomaApp.resolver("", null));
    }

    @Test
    public void elElegidoGanaAlSistema() {
        assertEquals("en", IdiomaApp.resolver("en", Collections.singletonList("es-ES")));
        assertEquals("es", IdiomaApp.resolver("es", Arrays.asList("en-US", "fr")));
    }

    @Test
    public void elLocaleEsElDeLosRecursos() {
        assertEquals(Locale.US, IdiomaApp.locale(true));
        assertEquals(new Locale("es", "ES"), IdiomaApp.locale(false));
        assertEquals(Locale.US, IdiomaApp.locale("en"));
        assertEquals(new Locale("es", "ES"), IdiomaApp.locale("es"));
    }

    // Android vuelve a poner el idioma por defecto del proceso en el del sistema tras abrir la
    // app cuando ninguno de la lista es suyo: con [francés], el calendario de Material salía en
    // francés (visto en el emulador). Se recoloca al crear cada pantalla.
    @Test
    public void cadaPantallaRecolocaElIdiomaPorDefecto() throws IOException {
        Path p = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit", "utils", "ScaleUtils.java");
        String s = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        if (!s.contains("IdiomaApp.fijarPorDefecto(base);")) throw new AssertionError("ScaleUtils.wrap no fija el idioma por defecto");
        if (!s.contains("IdiomaApp.aplicarSinElegir(base, override);")) throw new AssertionError("ScaleUtils.wrap no pone el idioma de la app");
    }

    // Sin el filtro, la APK lleva las traducciones de AppCompat y Material (francés,
    // catalán…) y Android elige una de ellas antes que el inglés de la lista del sistema.
    @Test
    public void laApkSoloLlevaEspanolEIngles() throws IOException {
        Path gradle = Paths.get("build.gradle");
        if (!Files.exists(gradle)) gradle = Paths.get("app", "build.gradle");
        String texto = new String(Files.readAllBytes(gradle), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("localeFilters\\s*\\+?=\\s*\\[([^\\]]*)\\]").matcher(texto);
        if (!m.find()) throw new AssertionError("app/build.gradle no filtra los idiomas (localeFilters)");
        String lista = m.group(1).replaceAll("[\\s'\"]", "");
        assertEquals("es,en", lista);
    }
}
