package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.ejercicio.EjercicioDTO;
import com.gymprofit.api.dto.entity.logro.LogroDTO;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.entity.Logro;
import com.gymprofit.api.enums.TipoLogro;
import com.gymprofit.api.mappers.EjercicioMapper;
import com.gymprofit.api.mappers.LogroMapper;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.repository.jpa.ILogroRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// IdiomasCatalogoTest — ningún ejercicio ni logro sale igual en español y en inglés (GP-220)
//
// Mira lo que sirve la API, no la base: cada fila pasa por su mapper con la petición en
// español y en inglés (Accept-Language → LocaleContextHolder), y un nombre o una
// descripción que salga igual en los dos es una fuga, salvo los nombres que en un gimnasio
// español se dicen en inglés (NOMBRES_IGUALES).
//
// Tres partes. La guarda sobre lo que haya en la base: en local, el catálogo entero; en CI,
// lo que siembran las migraciones. Las fugas que se encontraron en producción, sembradas tal
// cual estaban, con la migración V202611121810 aplicada encima: sin ella, este test es el
// que se pone en rojo. Y una fuga hecha a propósito, para que la guarda no pase por no mirar.
// Contra la MariaDB de verdad y dentro de la transacción del test.
// ============================================================
@SpringBootTest
@Transactional
@DisplayName("GP-220 — ningún ejercicio ni logro sale igual en español y en inglés")
class IdiomasCatalogoTest {

    /**
     * Los nombres que en un gimnasio español se dicen igual que en inglés: los tres del
     * glosario (sección 6) y los de los ejercicios viejos de wger que el inventario de GP-220
     * dio por buenos. En minúsculas: se compara sin mayúsculas («Dead bug» y «Dead Bug»).
     */
    static final Set<String> NOMBRES_IGUALES = Set.of(
            "superman", "good morning", "bird dog", "dead bug", "muscle up", "step jack",
            "pallof press", "jm press", "hindu pushups", "stair master", "shotgun row", "pendular hack");

    private static final String MIGRACION = "db/migration/V202611121810__Catalogo_textos_en.sql";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private IEjercicioRepository ejercicioRepository;

    @Autowired
    private ILogroRepository logroRepository;

    @Autowired
    private EjercicioMapper ejercicioMapper;

    @Autowired
    private LogroMapper logroMapper;

    @AfterEach
    void idiomaPorDefecto() {
        LocaleContextHolder.resetLocaleContext();
    }

    // ── La guarda ────────────────────────────────────────────

    private static <T> T enIdioma(String idioma, Supplier<T> paso) {
        LocaleContextHolder.setLocale(Locale.forLanguageTag(idioma));
        try {
            return paso.get();
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    private static boolean igual(String es, String en) {
        return es != null && !es.isBlank() && en != null && es.trim().equalsIgnoreCase(en.trim());
    }

    /** Los ejercicios que salen igual en los dos idiomas, como «id campo: texto». */
    private List<String> ejerciciosIguales(List<Ejercicio> ejercicios) {
        List<String> mal = new ArrayList<>();
        for (Ejercicio e : ejercicios) {
            EjercicioDTO es = enIdioma("es", () -> ejercicioMapper.toDTO(e));
            EjercicioDTO en = enIdioma("en", () -> ejercicioMapper.toDTO(e));
            if (igual(es.getNombre(), en.getNombre())
                    && !NOMBRES_IGUALES.contains(es.getNombre().trim().toLowerCase(Locale.ROOT))) {
                mal.add(e.getId() + " nombre: " + es.getNombre());
            }
            if (igual(es.getDescripcion(), en.getDescripcion())) {
                mal.add(e.getId() + " descripcion de " + es.getNombre());
            }
        }
        return mal;
    }

    /** Los logros que salen igual en los dos idiomas. */
    private List<String> logrosIguales(List<Logro> logros) {
        List<String> mal = new ArrayList<>();
        for (Logro l : logros) {
            LogroDTO es = enIdioma("es", () -> logroMapper.toDTO(l));
            LogroDTO en = enIdioma("en", () -> logroMapper.toDTO(l));
            if (igual(es.getNombre(), en.getNombre())) mal.add("logro " + l.getTipo() + " nombre: " + es.getNombre());
            if (igual(es.getDescripcion(), en.getDescripcion())) mal.add("logro " + l.getTipo() + " descripcion");
        }
        return mal;
    }

    // ── Sobre lo que hay ─────────────────────────────────────

    @Test
    @DisplayName("el catálogo activo y los logros salen en su idioma")
    void lo_que_hay_en_la_base() {
        assertThat(ejerciciosIguales(ejercicioRepository.findByActivoTrue()))
                .as("ejercicios activos iguales en ES y EN").isEmpty();
        List<Logro> logros = logroRepository.findAll();
        assertThat(logros).as("los logros los siembran las migraciones").isNotEmpty();
        assertThat(logrosIguales(logros)).as("logros iguales en ES y EN").isEmpty();
    }

    // ── Las fugas de producción ──────────────────────────────

    private Integer filaWger(String nombre, String nombreEn, String descripcion, String descripcionEn) {
        jdbc.update("""
                INSERT INTO ejercicios (nombre, nombre_en, descripcion, descripcion_en, grupo_muscular, dificultad,
                                        activo, equipamiento, nombre_revisado)
                VALUES (?, ?, ?, ?, 'BRAZOS', 'INTERMEDIO', 0, 'OTRO', 0)""",
                nombre, nombreEn, descripcion, descripcionEn);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    /** La fila de free-exercise-db con ese fed_id y sin inglés: la que hay (en local) o una nueva (en CI). */
    private Integer filaFedSinIngles(String fedId, String nombre, String nombreEn, String descripcion) {
        List<Integer> hay = jdbc.queryForList("SELECT id FROM ejercicios WHERE fed_id = ?", Integer.class, fedId);
        if (!hay.isEmpty()) {
            jdbc.update("UPDATE ejercicios SET descripcion_en = NULL WHERE id = ?", hay.get(0));
            return hay.get(0);
        }
        jdbc.update("""
                INSERT INTO ejercicios (nombre, nombre_en, descripcion, descripcion_en, grupo_muscular, dificultad,
                                        activo, fed_id, equipamiento, nombre_revisado)
                VALUES (?, ?, ?, NULL, 'HOMBROS', 'INTERMEDIO', 1, ?, 'MANCUERNAS', 1)""",
                nombre, nombreEn, descripcion, fedId);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    private void aplicarMigracion() {
        // La conexión de la transacción del test: Spring la reutiliza y se deshace al acabar.
        ScriptUtils.executeSqlScript(DataSourceUtils.getConnection(dataSource), new ClassPathResource(MIGRACION));
    }

    @Test
    @DisplayName("las 8 descripciones y los 2 nombres de producción salen bien con la migración aplicada")
    void fugas_de_produccion() {
        Integer curl = filaWger("Curl De Muñeca Con Barra", null,
                "Es un ejercicio de aislamiento para la cara interna del antebrazo.",
                "This is a variation of the flat bench press designed to emphasize the clavicular head.");
        Integer flexion = filaWger("Push-Up", null,
                "La flexión es un ejercicio fundamental con peso corporal.",
                "The push-up is a fundamental bodyweight exercise.");
        List<Integer> ids = new ArrayList<>(List.of(curl, flexion));
        for (String[] f : new String[][]{
                {"Zancadas Caminando con Barra", "Barbell Lunges Walking", "Cómo realizarlo: coloca una barra sobre la espalda."},
                {"Extensión de Tríceps a una Mano en Polea", "One Arm Triceps Extensions on Cable", "Este ejercicio es ideal para aislar el tríceps."},
                {"Curl femoral sentado", "Leg Curls (sitting)", "Ajusta la palanca de la máquina para adaptarla a tu altura."},
                {"Press de hombro con maquina", "Shoulder Press, on Machine", "Sentarse en la máquina con las piernas separadas."},
                {"Extensión de Cuádriceps a una Pierna", "Single Leg Extension", "Esta variante es excelente para aislar cada cuádriceps."}}) {
            ids.add(filaWger(f[0], f[1], f[2], f[2]));
        }
        ids.add(filaFedSinIngles("Iron_Cross", "Cruz de hierro con mancuernas", "Iron Cross", "1. De pie, con una mancuerna ligera en cada mano."));
        ids.add(filaFedSinIngles("One-Arm_Kettlebell_Swings", "Swing con kettlebell a un brazo", "One-Arm Kettlebell Swings", "1. De pie, con una kettlebell en el suelo."));
        ids.add(filaFedSinIngles("Side_Jackknife", "Abdominal navaja lateral", "Side Jackknife", "1. Túmbate de lado en el suelo."));

        aplicarMigracion();

        assertThat(ejerciciosIguales(ejercicioRepository.findAllById(ids))).as("fugas tras la migración").isEmpty();

        Map<String, Object> c = jdbc.queryForMap("SELECT * FROM ejercicios WHERE id = ?", curl);
        assertThat(c.get("nombre")).isEqualTo("Curl de muñeca con barra");
        assertThat(c.get("nombre_en")).isEqualTo("Barbell Wrist Curl");
        assertThat((String) c.get("descripcion_en")).startsWith("An isolation exercise for the inner (front) side of the forearm.")
                .doesNotContain("bench press").contains("\n- Posture:").doesNotContain("\r");
        Map<String, Object> p = jdbc.queryForMap("SELECT * FROM ejercicios WHERE id = ?", flexion);
        assertThat(p.get("nombre")).isEqualTo("Flexión");
        assertThat(p.get("nombre_en")).isEqualTo("Push-Up");
        assertThat(p.get("descripcion_en")).as("la descripción en inglés de la flexión ya estaba bien")
                .isEqualTo("The push-up is a fundamental bodyweight exercise.");
    }

    @Test
    @DisplayName("la migración no pisa un inglés que ya está bien ni toca un ejercicio con fed_id del mismo nombre")
    void no_pisa_lo_que_esta_bien() {
        Integer bien = filaWger("Curl femoral sentado", "Seated Leg Curl",
                "Ajusta la palanca de la máquina.", "Adjust the machine lever.");

        aplicarMigracion();

        assertThat(jdbc.queryForObject("SELECT descripcion_en FROM ejercicios WHERE id = ?", String.class, bien))
                .isEqualTo("Adjust the machine lever.");
    }

    // ── La guarda ve las fugas ───────────────────────────────

    @Test
    @DisplayName("la guarda ve un ejercicio o un logro sin inglés, y deja pasar los nombres que se dicen igual")
    void la_guarda_ve_una_fuga() {
        Ejercicio sinIngles = new Ejercicio();
        sinIngles.setId(-1);
        sinIngles.setNombre("Remo con barra");
        sinIngles.setDescripcion("1. Inclina el torso.");
        Ejercicio superman = new Ejercicio();
        superman.setId(-2);
        superman.setNombre("Superman");
        superman.setNombreEn("Superman");
        superman.setDescripcion("1. Túmbate boca abajo.");
        superman.setDescripcionEn("1. Lie face down.");

        assertThat(ejerciciosIguales(List.of(sinIngles, superman)))
                .containsExactly("-1 nombre: Remo con barra", "-1 descripcion de Remo con barra");

        Logro logro = new Logro();
        logro.setNombre("Constancia");
        logro.setDescripcion("Completa 7 sesiones de entrenamiento");
        logro.setTipo(TipoLogro.CONSTANCIA);
        assertThat(logrosIguales(List.of(logro))).hasSize(2);
    }
}
