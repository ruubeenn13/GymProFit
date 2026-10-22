package com.gymprofit.api.service.ejercicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ============================================================
// CatalogoEsTest — el español del catálogo de ejercicios (GP-217)
//
// Dos cosas: que el lector del CSV entiende lo que debe (comillas, saltos de línea dentro
// de la descripción, filas rotas) y que el CSV de verdad cumple lo que se prometió: un
// nombre y una descripción por ejercicio, nombres únicos, de tú, sin inglés, sin calorías
// de entrenamiento (DEC-004) y con los nombres que el propietario aprobó para las
// plantillas.
// ============================================================
@DisplayName("GP-217 — el catálogo de ejercicios en español")
class CatalogoEsTest {

    private static final String CABECERA = "fed_id,nombre_es,descripcion_es\n";

    private static CatalogoEs real() {
        try (InputStream in = CatalogoEs.class.getClassLoader().getResourceAsStream(CatalogoEs.RECURSO)) {
            assertThat(in).as("falta " + CatalogoEs.RECURSO).isNotNull();
            return CatalogoEs.de(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ── El lector ────────────────────────────────────────────

    @Test
    @DisplayName("lee comillas, comas, comillas dobles y saltos de línea dentro de un campo")
    void lee_el_csv() {
        CatalogoEs c = CatalogoEs.de(new StringReader(CABECERA
                + "A_B,\"Remo, con barra\",\"1. Agarra \"\"así\"\".\n2. Tira.\"\n"
                + "C,Plancha,\n"));

        assertThat(c.tamano()).isEqualTo(2);
        assertThat(c.de("A_B")).get().satisfies(e -> {
            assertThat(e.nombre()).isEqualTo("Remo, con barra");
            assertThat(e.descripcion()).isEqualTo("1. Agarra \"así\".\n2. Tira.");
        });
        assertThat(c.de("C").orElseThrow().descripcion()).isEmpty();
        assertThat(c.de("no_existe")).isEmpty();
    }

    @Test
    @DisplayName("acepta fin de línea de Windows y una marca de orden de bytes")
    void lee_crlf_y_bom() {
        CatalogoEs c = CatalogoEs.de(new StringReader("﻿" + CABECERA.replace("\n", "\r\n")
                + "A,Uno,\"1. a\r\n2. b\"\r\nB,Dos,x\r\n"));

        assertThat(c.tamano()).isEqualTo(2);
        assertThat(c.de("A").orElseThrow().descripcion()).isEqualTo("1. a\n2. b");
    }

    @Test
    @DisplayName("un CSV mal formado no arranca: cabecera, columnas, repetidos y comillas sin cerrar")
    void rechaza_lo_roto() {
        assertThatThrownBy(() -> CatalogoEs.de(new StringReader("a,b,c\nA,Uno,x\n")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("fed_id,nombre_es,descripcion_es");
        assertThatThrownBy(() -> CatalogoEs.de(new StringReader(CABECERA + "A,Uno\n")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("3 columnas");
        assertThatThrownBy(() -> CatalogoEs.de(new StringReader(CABECERA + "A,Uno,x\nA,Dos,y\n")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("fed_id repetido");
        assertThatThrownBy(() -> CatalogoEs.de(new StringReader(CABECERA + "A,Uno,x\nB,uno,y\n")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Nombre repetido");
        assertThatThrownBy(() -> CatalogoEs.de(new StringReader(CABECERA + "A,,x\n")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("vacíos");
        assertThatThrownBy(() -> CatalogoEs.de(new StringReader(CABECERA + "A,Uno,\"sin cerrar\n")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("comillas");
    }

    // ── El CSV de verdad ─────────────────────────────────────

    @Test
    @DisplayName("trae los 873 ejercicios activos del catálogo, cada uno con su nombre")
    void trae_todos() {
        CatalogoEs c = real();

        assertThat(c.tamano()).isEqualTo(873);
        assertThat(c.todas()).allSatisfy(e -> assertThat(e.nombre()).isNotBlank());
    }

    @Test
    @DisplayName("los nombres son únicos y caben en la columna (100)")
    void nombres_unicos() {
        Set<String> vistos = new HashSet<>();
        for (CatalogoEs.Entrada e : real().todas()) {
            assertThat(e.nombre().length()).as(e.fedId()).isLessThanOrEqualTo(100);
            assertThat(vistos.add(e.nombre().toLowerCase())).as("nombre repetido: " + e.nombre()).isTrue();
        }
    }

    @Test
    @DisplayName("ningún nombre en español es igual al inglés salvo los del glosario")
    void ningun_nombre_igual_al_ingles() throws IOException {
        Map<String, String> ingles = nombresEn();
        Set<String> permitidos = exceptionsDelGlosario();
        List<String> mal = new ArrayList<>();
        for (CatalogoEs.Entrada e : real().todas()) {
            String en = ingles.get(e.fedId());
            assertThat(en).as("sin nombre en inglés de referencia para " + e.fedId()).isNotNull();
            if (e.nombre().equalsIgnoreCase(en) && !permitidos.contains(e.fedId())) mal.add(e.fedId() + " = " + en);
        }
        assertThat(mal).as("nombres iguales al inglés fuera del glosario").isEmpty();
        assertThat(permitidos).as("el glosario lista ejercicios que ya no coinciden").allSatisfy(f ->
                assertThat(real().de(f).orElseThrow().nombre()).isEqualToIgnoringCase(ingles.get(f)));
    }

    @Test
    @DisplayName("las descripciones son de tú: ni «usted», ni «recuéstese», ni «asegúrese»…")
    void descripciones_de_tu() {
        // Una forma de usted en imperativo: al principio de un paso o de una frase, o la que lleva
        // «se» enclítico (recuéstese, asegúrese, colóquese). «agarre», «doble» o «sea» solas son
        // sustantivos o adjetivos y no cuentan.
        Pattern reflexivo = Pattern.compile("(?iu)\\b\\p{L}{3,}[eé]se\\b");
        Pattern usted = Pattern.compile("(?iu)\\b(usted|ustedes)\\b");
        Pattern imperativo = Pattern.compile("(?iu)(?:^|\\n)\\d+\\.\\s+(coloque|sujete|sostenga|mantenga|levante|baje|"
                + "comience|empiece|adopte|extienda|apoye|tome|flexione|gire|repita|lleve|suba|regrese|utilice|realice|"
                + "haga|empuje|estire|junte|separe|relaje|contraiga|exhale|inhale|permita|descienda|retorne|vuelva|"
                + "lance|salte|corra|camine|ponga|sitúe|elija|ejecute|continúe|evite|procure|intente|trate|deje|cuide|"
                + "agarre|doble)\\b");
        // Palabras que acaban en «ese» y no son una orden.
        Set<String> sinOrden = Set.of("pese", "ese", "interese");
        for (CatalogoEs.Entrada e : real().todas()) {
            String d = e.descripcion();
            assertThat(usted.matcher(d).find()).as("usted en " + e.fedId()).isFalse();
            Matcher m = reflexivo.matcher(d);
            while (m.find()) {
                assertThat(sinOrden).as("forma de usted «" + m.group() + "» en " + e.fedId())
                        .contains(m.group().toLowerCase());
            }
            Matcher i = imperativo.matcher(d);
            assertThat(i.find()).as("imperativo de usted en " + e.fedId() + (i.hitEnd() ? "" : ": " + i.group())).isFalse();
        }
    }

    @Test
    @DisplayName("ninguna descripción está en inglés")
    void descripciones_sin_ingles() {
        Pattern ingles = Pattern.compile("(?i)\\b(the|and|your|with|then|until|that|from|position|starting|repeat|"
                + "this|keep|should|will|while|lower|raise|hold|back|body|you|are|for|into|each|arms|legs|feet|hands|"
                + "slowly|grip|exercise)\\b");
        for (CatalogoEs.Entrada e : real().todas()) {
            Matcher m = ingles.matcher(e.descripcion());
            int n = 0;
            while (m.find()) n++;
            assertThat(n).as("palabras en inglés en la descripción de " + e.fedId()).isLessThan(3);
        }
    }

    @Test
    @DisplayName("cada descripción son de 1 a 6 pasos numerados, y solo siete quedan vacías (sin texto original)")
    void pasos_numerados() {
        List<String> vacias = new ArrayList<>();
        for (CatalogoEs.Entrada e : real().todas()) {
            if (e.descripcion().isEmpty()) {
                vacias.add(e.fedId());
                continue;
            }
            String[] pasos = e.descripcion().split("\n");
            assertThat(pasos.length).as("pasos de " + e.fedId()).isBetween(1, 6);
            for (int i = 0; i < pasos.length; i++) {
                assertThat(pasos[i]).as("paso " + (i + 1) + " de " + e.fedId()).startsWith((i + 1) + ". ");
                assertThat(pasos[i].length()).as("paso vacío en " + e.fedId()).isGreaterThan(5);
            }
        }
        assertThat(vacias).containsExactlyInAnyOrder("Hip_Circles_prone", "Iron_Cross", "One-Arm_Kettlebell_Swings",
                "Power_Clean", "Quad_Stretch", "Romanian_Deadlift", "Side_Jackknife");
    }

    @Test
    @DisplayName("no hay calorías estimadas de entrenamiento en ninguna descripción (DEC-004)")
    void sin_calorias() {
        for (CatalogoEs.Entrada e : real().todas()) {
            assertThat(e.descripcion().toLowerCase()).as("calorías en " + e.fedId()).doesNotContain("calor");
        }
    }

    @Test
    @DisplayName("los 62 ejercicios de las plantillas y los programas conservan el nombre que aprobó el propietario")
    void plantillas_con_su_nombre() throws IOException {
        JsonNode catalogo = new ObjectMapper().readTree(Files.readAllBytes(raiz().resolve(
                "api/gymprofit-api/src/main/resources/db/semillas/catalogo-plantillas-v1.json")));
        CatalogoEs c = real();
        assertThat(catalogo.get("ejercicios")).hasSize(62);
        for (JsonNode e : catalogo.get("ejercicios")) {
            assertThat(c.de(e.get("fedId").asText())).as(e.get("fedId").asText()).isPresent();
            assertThat(c.de(e.get("fedId").asText()).orElseThrow().nombre()).isEqualTo(e.get("nombre").asText());
        }
    }

    // ── Apoyos ───────────────────────────────────────────────

    private static Path raiz() {
        Path p = Paths.get("").toAbsolutePath();
        while (p != null && !Files.isDirectory(p.resolve("documentacion"))) p = p.getParent();
        assertThat(p).as("no se encuentra la raíz del repositorio").isNotNull();
        return p;
    }

    // fed_id → nombre en inglés, de la copia de referencia de los tests.
    private static Map<String, String> nombresEn() throws IOException {
        Map<String, String> m = new HashMap<>();
        try (InputStream in = CatalogoEsTest.class.getClassLoader().getResourceAsStream("catalogo/ejercicios-en.csv")) {
            assertThat(in).isNotNull();
            List<List<String>> filas = CatalogoEs.filas(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (List<String> f : filas.subList(1, filas.size())) m.put(f.get(0), f.get(1));
        }
        return m;
    }

    // Los fed_id de la sección «Nombres que coinciden con el inglés» del glosario.
    private static Set<String> exceptionsDelGlosario() throws IOException {
        String md = Files.readString(raiz().resolve("documentacion/catalogo/glosario-ejercicios.md"), StandardCharsets.UTF_8);
        int desde = md.indexOf("## 6. Nombres que coinciden con el inglés");
        assertThat(desde).as("falta la sección 6 del glosario").isGreaterThanOrEqualTo(0);
        int hasta = md.indexOf("\n## ", desde + 5);
        String seccion = md.substring(desde, hasta < 0 ? md.length() : hasta);
        Set<String> ids = new HashSet<>();
        Matcher m = Pattern.compile("(?m)^- `([^`]+)`").matcher(seccion);
        while (m.find()) ids.add(m.group(1));
        return ids;
    }
}
