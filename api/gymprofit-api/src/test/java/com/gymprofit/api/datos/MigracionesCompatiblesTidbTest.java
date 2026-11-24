package com.gymprofit.api.datos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// MigracionesCompatiblesTidbTest — ninguna migración nueva añade una columna y
// una restricción o un índice sobre ella en el MISMO `ALTER TABLE` (GP-273).
//
// Producción corre en TiDB, y TiDB rechaza ese patrón («Key column 'x' doesn't
// exist in table»), mientras que MySQL —el del CI y el de desarrollo— lo acepta.
// Sin este test, una migración así pasaría el CI en verde y la API no arrancaría
// en producción: Render se quedaría con la versión anterior y el lote no saldría.
//
// La forma buena son dos sentencias: primero `ADD COLUMN`, luego, en otro
// `ALTER`, `ADD CONSTRAINT` o `ADD INDEX`.
//
// Las cuatro migraciones de LISTA_BLANCA ya están publicadas y no se editan; en
// producción no se vuelven a ejecutar porque la base trae su historial de
// Flyway. Solo romperían una base nueva creada desde cero en TiDB.
//
// No mira las migraciones Java (`src/main/java/db/migration`): hoy solo
// insertan datos.
// ============================================================
@DisplayName("GP-273 — las migraciones no añaden columna y restricción en el mismo ALTER (TiDB)")
class MigracionesCompatiblesTidbTest {

    /** Migraciones ya publicadas con el patrón. No se amplía: una nueva se parte en dos ALTER. */
    private static final Set<String> LISTA_BLANCA = Set.of(
            "V202605261100",
            "V202609292000",
            "V202610011900",
            "V202610021100");

    private static final Pattern ALTER = Pattern.compile(
            "^\\s*ALTER\\s+TABLE\\s+\\S+\\s+(.*)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // ADD [COLUMN] nombre ...  — excluye las palabras que abren una restricción o un índice.
    private static final Pattern ADD_COLUMNA = Pattern.compile(
            "^ADD\\s+(?:COLUMN\\s+)?(?!(?:CONSTRAINT|INDEX|KEY|UNIQUE|PRIMARY|FOREIGN|FULLTEXT|SPATIAL|CHECK|PARTITION)\\b)"
                    + "`?(\\w+)`?\\s+(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // ADD [CONSTRAINT nombre] {UNIQUE|PRIMARY KEY|FOREIGN KEY|INDEX|KEY|FULLTEXT|SPATIAL} ... (columnas)
    private static final Pattern ADD_RESTRICCION = Pattern.compile(
            "^ADD\\s+(?:CONSTRAINT\\s+`?\\w*`?\\s*)?(?:UNIQUE|PRIMARY\\s+KEY|FOREIGN\\s+KEY|INDEX|KEY|FULLTEXT|SPATIAL)\\b[^(]*\\(([^)]*)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // Restricción escrita dentro de la propia definición de la columna.
    private static final Pattern RESTRICCION_EN_LINEA = Pattern.compile(
            "\\b(?:UNIQUE|PRIMARY\\s+KEY|REFERENCES)\\b", Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("Ninguna migración fuera de la lista blanca usa el patrón")
    void ningunaMigracionNuevaUsaElPatron() throws Exception {
        Map<String, List<String>> infractoras = infractorasPorVersion();
        infractoras.keySet().removeAll(LISTA_BLANCA);

        assertThat(infractoras)
                .as("""
                        Estas migraciones añaden una columna y una restricción o índice sobre ella en el \
                        mismo ALTER TABLE. TiDB (producción) lo rechaza. Pártelas en dos ALTER: \
                        primero ADD COLUMN, después ADD CONSTRAINT / ADD INDEX.""")
                .isEmpty();
    }

    @Test
    @DisplayName("La lista blanca sigue siendo exacta: el detector encuentra las cuatro publicadas")
    void laListaBlancaSigueSiendoExacta() throws Exception {
        // Si el detector dejara de ver el patrón, el test de arriba pasaría siempre.
        // Y si una de la lista ya no lo tuviera, sobraría en la lista.
        assertThat(infractorasPorVersion().keySet()).containsAll(LISTA_BLANCA);
    }

    /** Versión de la migración → columnas que la hacen infractora. */
    private static Map<String, List<String>> infractorasPorVersion() throws IOException, URISyntaxException {
        Map<String, List<String>> resultado = new TreeMap<>();
        try (Stream<Path> ficheros = Files.list(directorioMigraciones())) {
            for (Path fichero : ficheros.filter(f -> f.getFileName().toString().endsWith(".sql")).toList()) {
                String nombre = fichero.getFileName().toString();
                String version = nombre.substring(0, nombre.indexOf("__"));
                List<String> columnas = columnasConRestriccionEnElMismoAlter(
                        Files.readString(fichero, StandardCharsets.UTF_8));
                if (!columnas.isEmpty()) {
                    resultado.put(version, columnas);
                }
            }
        }
        return resultado;
    }

    private static Path directorioMigraciones() throws URISyntaxException {
        URL url = MigracionesCompatiblesTidbTest.class.getClassLoader().getResource("db/migration");
        assertThat(url).as("db/migration en el classpath").isNotNull();
        return Path.of(url.toURI());
    }

    /**
     * Devuelve las columnas que un mismo {@code ALTER TABLE} añade y restringe a la vez.
     *
     * @param sql contenido completo de un fichero de migración
     * @return columnas infractoras, en orden de aparición; vacía si no hay ninguna
     */
    static List<String> columnasConRestriccionEnElMismoAlter(String sql) {
        List<String> infractoras = new ArrayList<>();
        for (String sentencia : sinComentarios(sql).split(";")) {
            Matcher alter = ALTER.matcher(sentencia);
            if (!alter.matches()) {
                continue;
            }
            Set<String> anadidas = new LinkedHashSet<>();
            List<String> restringidas = new ArrayList<>();
            for (String clausula : clausulas(alter.group(1))) {
                Matcher columna = ADD_COLUMNA.matcher(clausula);
                Matcher restriccion = ADD_RESTRICCION.matcher(clausula);
                if (restriccion.find()) {
                    for (String c : restriccion.group(1).split(",")) {
                        restringidas.add(normalizar(c.replaceAll("\\(.*", "")));
                    }
                } else if (columna.matches()) {
                    String nombre = normalizar(columna.group(1));
                    anadidas.add(nombre);
                    if (RESTRICCION_EN_LINEA.matcher(columna.group(2)).find()) {
                        infractoras.add(nombre);
                    }
                }
            }
            for (String c : restringidas) {
                if (anadidas.contains(c) && !infractoras.contains(c)) {
                    infractoras.add(c);
                }
            }
        }
        return infractoras;
    }

    private static String sinComentarios(String sql) {
        return sql.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)--.*$", " ").replaceAll("(?m)#.*$", " ");
    }

    /** Parte el cuerpo de un ALTER por las comas de primer nivel (no las de dentro de paréntesis). */
    private static List<String> clausulas(String cuerpo) {
        List<String> partes = new ArrayList<>();
        int profundidad = 0;
        StringBuilder actual = new StringBuilder();
        for (char ch : cuerpo.toCharArray()) {
            if (ch == '(') profundidad++;
            if (ch == ')') profundidad--;
            if (ch == ',' && profundidad == 0) {
                partes.add(actual.toString().trim());
                actual.setLength(0);
            } else {
                actual.append(ch);
            }
        }
        partes.add(actual.toString().trim());
        return partes;
    }

    private static String normalizar(String identificador) {
        return identificador.replace("`", "").trim().toLowerCase(Locale.ROOT);
    }
}
