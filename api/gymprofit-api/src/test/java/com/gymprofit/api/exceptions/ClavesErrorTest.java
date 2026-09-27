package com.gymprofit.api.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// ClavesErrorTest — GP-109
//
// Los mensajes de error se escriben como claves. Una clave que falte en español
// revienta al lanzar la excepción, que es justo cuando no se quiere descubrir; una
// que falte en inglés sale en español sin avisar. Este test lee el código y exige
// que toda clave usada esté en los dos ficheros, y que los dos tengan las mismas.
// ============================================================
@DisplayName("GP-109 — toda clave de error del código está en español y en inglés")
class ClavesErrorTest {

    // "error.x.y" o "validacion.x.y" como literal, o {validacion.x} en una anotación.
    private static final Pattern CLAVE = Pattern.compile("[\"{]((?:error|validacion)\\.[A-Za-z0-9.]*[A-Za-z0-9])[\"}]");

    private static Properties cargar(String recurso) throws IOException {
        Properties p = new Properties();
        try (InputStream in = ClavesErrorTest.class.getResourceAsStream(recurso)) {
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    private static Set<String> clavesDelCodigo() throws IOException {
        Set<String> claves = new TreeSet<>();
        try (Stream<Path> ficheros = Files.walk(Path.of("src/main/java"))) {
            for (Path f : ficheros.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = CLAVE.matcher(Files.readString(f));
                while (m.find()) claves.add(m.group(1));
            }
        }
        return claves;
    }

    @Test
    @DisplayName("cada clave usada en el código existe en messages y en messages_en")
    void claves_usadas_existen() throws IOException {
        Set<String> usadas = clavesDelCodigo();
        assertThat(usadas).as("el patrón no encuentra claves: ¿ha cambiado la forma de escribirlas?")
                .hasSizeGreaterThan(50);
        assertThat(cargar("/messages.properties").stringPropertyNames()).containsAll(usadas);
        assertThat(cargar("/messages_en.properties").stringPropertyNames()).containsAll(usadas);
    }

    @Test
    @DisplayName("los dos idiomas tienen exactamente las mismas claves")
    void mismas_claves() throws IOException {
        assertThat(cargar("/messages_en.properties").stringPropertyNames())
                .containsExactlyInAnyOrderElementsOf(cargar("/messages.properties").stringPropertyNames());
    }
}
