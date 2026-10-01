package com.gymprofit.api.datos;

import com.gymprofit.api.controller.AlimentoController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// BasicosCsvTest — recorre todos los alimentos básicos (GP-127)
// La lista curada vive en datos/basicos/basicos.csv y de ella sale la migración.
// Aquí se comprueba fila a fila lo que una revisión a ojo deja pasar: categoría
// válida, nombres en los dos idiomas, sin duplicados, valores posibles y kcal que
// cuadran con los macros. Lo que no cuadra tiene que decir por qué en el propio CSV.
// No levanta Spring: lee el fichero.
// ============================================================
class BasicosCsvTest {

    // Los mismos márgenes que generar_basicos.py: si se cambian allí, aquí.
    private static final double MARGEN_ABSOLUTO = 12.0;
    private static final double MARGEN_RELATIVO = 0.08;

    private static List<Map<String, String>> basicos;
    private static List<Map<String, String>> raciones;

    @BeforeAll
    static void leer() throws IOException {
        basicos = BasicosCsvTestApoyo.basicos();
        raciones = BasicosCsvTestApoyo.raciones();
    }

    @Test
    @DisplayName("hay entre 300 y 500 básicos y todos traen fuente y código de origen")
    void tamano_y_origen() {
        assertThat(basicos).hasSizeBetween(300, 500);
        for (Map<String, String> b : basicos) {
            assertThat(b.get("fuente")).as(describir(b)).isIn("CIQUAL", "USDA");
            assertThat(b.get("codigo")).as(describir(b)).matches("\\d+");
        }
    }

    @Test
    @DisplayName("cada básico tiene una de las 14 categorías de la API")
    void categoria_valida() {
        for (Map<String, String> b : basicos) {
            assertThat(AlimentoController.CATEGORIAS).as(describir(b)).contains(b.get("categoria"));
        }
    }

    @Test
    @DisplayName("cada básico tiene nombre en español y en inglés, de como mucho 100 caracteres")
    void nombres_en_los_dos_idiomas() {
        for (Map<String, String> b : basicos) {
            assertThat(b.get("nombre_es")).as(describir(b)).isNotBlank().hasSizeLessThanOrEqualTo(100);
            assertThat(b.get("nombre_en")).as(describir(b)).isNotBlank().hasSizeLessThanOrEqualTo(100);
        }
    }

    @Test
    @DisplayName("no hay dos básicos con el mismo origen ni con el mismo nombre en ningún idioma")
    void sin_duplicados() {
        Set<String> origenes = new HashSet<>();
        Set<String> nombresEs = new HashSet<>();
        Set<String> nombresEn = new HashSet<>();
        for (Map<String, String> b : basicos) {
            assertThat(origenes.add(b.get("fuente") + ":" + b.get("codigo"))).as("origen repetido: " + describir(b)).isTrue();
            assertThat(nombresEs.add(b.get("nombre_es").toLowerCase())).as("nombre repetido: " + describir(b)).isTrue();
            assertThat(nombresEn.add(b.get("nombre_en").toLowerCase())).as("name repeated: " + describir(b)).isTrue();
        }
    }

    @Test
    @DisplayName("los valores por 100 g son posibles: nada negativo ni por encima de 100 g")
    void valores_posibles() {
        for (Map<String, String> b : basicos) {
            double p = numero(b, "proteinas");
            double c = numero(b, "carbohidratos");
            double g = numero(b, "grasas");
            double f = b.get("fibra").isEmpty() ? 0 : numero(b, "fibra");
            assertThat(List.of(p, c, g, f)).as(describir(b)).allMatch(v -> v >= 0 && v <= 100);
            assertThat(p + c + g + f).as(describir(b)).isLessThanOrEqualTo(100.5);
            assertThat(numero(b, "kcal")).as(describir(b)).isBetween(0.0, 900.0);
        }
    }

    @Test
    @DisplayName("las kcal cuadran con 4·P + 4·C + 9·G + 2·fibra, o el CSV explica por qué no")
    void kcal_cuadran_o_estan_justificadas() {
        for (Map<String, String> b : basicos) {
            double kcal = numero(b, "kcal");
            double calculadas = 4 * numero(b, "proteinas") + 4 * numero(b, "carbohidratos")
                    + 9 * numero(b, "grasas") + 2 * (b.get("fibra").isEmpty() ? 0 : numero(b, "fibra"));
            boolean cuadran = Math.abs(kcal - calculadas)
                    <= Math.max(MARGEN_ABSOLUTO, MARGEN_RELATIVO * Math.max(kcal, calculadas));
            if (!cuadran) {
                assertThat(b.get("excepcion_kcal"))
                        .as("%s: %s kcal frente a %.0f calculadas, sin justificar", describir(b), kcal, calculadas)
                        .isNotBlank()
                        .doesNotContain("SIN EXPLICAR");
            }
        }
    }

    @Test
    @DisplayName("los habituales de la búsqueda vacía son los marcados en el CSV, ni uno más")
    void hay_habituales() throws IOException {
        Set<String> marcados = new HashSet<>();
        basicos.stream().filter(b -> "s".equals(b.get("habitual")))
                .forEach(b -> marcados.add(b.get("fuente") + ";" + b.get("codigo")));
        assertThat(marcados).hasSizeBetween(10, 40);

        List<String> lista = java.nio.file.Files.readAllLines(
                        java.nio.file.Path.of("src/main/resources/busqueda/habituales.txt"))
                .stream().map(String::strip).filter(l -> !l.isEmpty() && !l.startsWith("#")).toList();
        assertThat(lista).doesNotHaveDuplicates();
        assertThat(new HashSet<>(lista)).isEqualTo(marcados);
    }

    @Test
    @DisplayName("cada ración es de un básico, con nombre en los dos idiomas, gramos y la fuente del peso")
    void raciones_validas() {
        Set<String> origenes = new HashSet<>();
        basicos.forEach(b -> origenes.add(b.get("fuente") + ":" + b.get("codigo")));
        Map<String, Set<String>> nombresPorBasico = new HashMap<>();
        for (Map<String, String> r : raciones) {
            String clave = r.get("fuente") + ":" + r.get("codigo");
            assertThat(origenes).as("ración sin básico: " + r).contains(clave);
            assertThat(r.get("nombre_es")).as(r.toString()).isNotBlank().hasSizeLessThanOrEqualTo(60);
            assertThat(r.get("nombre_en")).as(r.toString()).isNotBlank().hasSizeLessThanOrEqualTo(60);
            assertThat(r.get("fuente_peso")).as(r.toString()).isNotBlank().hasSizeLessThanOrEqualTo(255);
            assertThat(Double.parseDouble(r.get("gramos"))).as(r.toString()).isBetween(0.1, 2000.0);
            assertThat(nombresPorBasico.computeIfAbsent(clave, k -> new HashSet<>()).add(r.get("nombre_es")))
                    .as("ración repetida: " + r).isTrue();
        }
    }

    // --- Andamiaje ----------------------------------------------------------

    private static double numero(Map<String, String> fila, String campo) {
        return Double.parseDouble(fila.get(campo));
    }

    private static String describir(Map<String, String> b) {
        return b.get("fuente") + " " + b.get("codigo") + " «" + b.get("nombre_es") + "»";
    }
}
