package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.service.programa.SembradorPlantillas;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.sql.SQLException;
import java.util.Set;

// ============================================================
// CatalogoPlantillasDePrueba — el catálogo de plantillas v1 dentro de un test (GP-074)
//
// La migración V202609292002 siembra el catálogo al arrancar, pero la base de CI no trae
// los ejercicios del catálogo, así que allí las plantillas salen vacías. Esto las vuelve
// a sembrar DENTRO de la transacción del test, con los 62 ejercicios presentes: si ya
// están (la base local), los deja activos; si no, los crea con su fed_id. Todo se deshace
// al acabar el test.
// ============================================================
final class CatalogoPlantillasDePrueba {

    static final String CATALOGO = "db/semillas/catalogo-plantillas-v1.json";

    /**
     * Los ejercicios del catálogo que son de peso corporal en la base (equipamiento
     * PESO_CORPORAL, GP-085). A estos no les afecta «Ganar fuerza». Sacado de la base
     * local el 2026-09-29, ya con Goblet_Squat pasado a MANCUERNAS por la semilla.
     */
    static final Set<String> PESO_CORPORAL = Set.of(
            "Single_Leg_Glute_Bridge", "Dead_Bug", "Floor_Glute-Ham_Raise", "Step-up_with_Knee_Raise",
            "Plank", "Glute_Kickback", "Pullups", "Push-Ups_-_Close_Triceps_Position",
            "Push-Ups_With_Feet_Elevated", "Pushups", "Bench_Dips", "Reverse_Crunch", "Bodyweight_Squat",
            "Bodyweight_Walking_Lunge", "Butt_Lift_Bridge", "Side_Bridge", "Chin-Up");

    private CatalogoPlantillasDePrueba() { }

    /** El catálogo, tal como lo lee la migración. */
    static JsonNode catalogo() {
        try (InputStream in = new ClassPathResource(CATALOGO).getInputStream()) {
            return new ObjectMapper().readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Quita programas, plantillas y lo que cuelga de ellos. */
    static void vaciar(JdbcTemplate jdbc) {
        jdbc.update("UPDATE rutinas SET plantilla_id = NULL, programa_usuario_id = NULL "
                + "WHERE plantilla_id IS NOT NULL OR programa_usuario_id IS NOT NULL");
        jdbc.update("DELETE FROM programas_usuario");
        jdbc.update("DELETE FROM programa_rutina");
        jdbc.update("DELETE re FROM rutina_ejercicio re JOIN rutinas r ON r.id = re.rutina_id WHERE r.es_plantilla = 1");
        jdbc.update("DELETE FROM rutinas WHERE es_plantilla = 1");
        jdbc.update("DELETE FROM programas");
    }

    /** Deja los 62 ejercicios del catálogo en la base y activos. */
    static void asegurarEjercicios(JdbcTemplate jdbc) {
        for (JsonNode e : catalogo().get("ejercicios")) {
            String fedId = e.get("fedId").asText();
            int hay = jdbc.update("UPDATE ejercicios SET activo = 1 WHERE fed_id = ?", fedId);
            if (hay == 0) {
                jdbc.update("""
                        INSERT INTO ejercicios (nombre, nombre_en, grupo_muscular, dificultad, activo, fed_id,
                                                equipamiento, nombre_revisado)
                        VALUES (?, ?, 'CARDIO', 'PRINCIPIANTE', 1, ?, ?, 0)""",
                        fedId, fedId, fedId, PESO_CORPORAL.contains(fedId) ? "PESO_CORPORAL" : "OTRO");
            }
        }
    }

    /** Vacía y vuelve a sembrar con la conexión de la transacción del test. */
    static SembradorPlantillas.Informe resembrar(JdbcTemplate jdbc, DataSource dataSource) {
        vaciar(jdbc);
        try (InputStream in = new ClassPathResource(CATALOGO).getInputStream()) {
            return SembradorPlantillas.sembrar(DataSourceUtils.getConnection(dataSource), in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Ejercicios presentes y catálogo re-sembrado: lo que necesita un test de programas. */
    static void sembrarCompleto(JdbcTemplate jdbc, DataSource dataSource) {
        asegurarEjercicios(jdbc);
        resembrar(jdbc, dataSource);
    }
}
