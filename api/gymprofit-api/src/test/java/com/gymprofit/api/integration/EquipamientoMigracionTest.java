package com.gymprofit.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// EquipamientoMigracionTest — GP-085
//
// El relleno de ejercicios.equipamiento y nombre_revisado, contra la MariaDB de verdad:
// que Flyway aplicó las dos migraciones, y el script del relleno vuelto a ejecutar
// sobre filas preparadas, dentro de la transacción del test. Los casos son valores
// reales de equipo_necesario del catálogo (la tabla completa está en el informe de la
// entrega), con los que tienen trampa: varios aparatos, la barra de dominadas, la
// polea que la importación llama «Cable machine» y lo que no es de ninguna lista.
// ============================================================
@SpringBootTest
@Transactional
@DisplayName("GP-085 — migración del equipamiento y del nombre revisado")
class EquipamientoMigracionTest {

    private static final String RELLENO = "db/migration/V202609271301__Ejercicio_equipamiento_relleno.sql";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private void rellenar() {
        Connection conexion = DataSourceUtils.getConnection(dataSource);
        ScriptUtils.executeSqlScript(conexion, new ClassPathResource(RELLENO));
    }

    private int ejercicio(String equipo, String equipoEn, String nombre, String nombreEn) {
        jdbc.update("""
                INSERT INTO ejercicios (nombre, nombre_en, grupo_muscular, dificultad, activo,
                                        equipo_necesario, equipo_necesario_en, equipamiento, nombre_revisado)
                VALUES (?, ?, 'CARDIO', 'PRINCIPIANTE', 1, ?, ?, 'OTRO', 0)""",
                nombre, nombreEn, equipo, equipoEn);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    @Test
    @DisplayName("Flyway ha aplicado las dos migraciones")
    void flyway_las_aplica() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history "
                + "WHERE version IN ('202609271300', '202609271301') AND success = 1", Integer.class))
                .isEqualTo(2);
    }

    @ParameterizedTest(name = "{0} → {2}")
    @CsvSource(delimiter = ';', value = {
            "Barra;Barbell;BARRA",
            "Barra Z;SZ-Bar;BARRA",
            "Barra, Mancuernas, Barra de dominadas;Barbell, Dumbbell, Pull-up bar;BARRA",
            "Banco, Mancuernas;Bench, Dumbbell;MANCUERNAS",
            "Banco, Mancuernas, Kettlebell, Sin equipo;Bench, Dumbbell, Kettlebell, none (bodyweight exercise);MANCUERNAS",
            "Kettlebell;Kettlebell;KETTLEBELL",
            "Polea;Cable machine;POLEA",
            "Máquina;Machine;MAQUINA",
            "Cinta de correr;Treadmill;MAQUINA",
            "Banda elástica, Sin equipo;Resistance band, none (bodyweight exercise);BANDA",
            "Sin equipo;Bodyweight;PESO_CORPORAL",
            "Barra de dominadas;Pull-up bar;PESO_CORPORAL",
            "Barra dominadas;Pull-up bar;PESO_CORPORAL",
            "Esterilla;Gym mat;PESO_CORPORAL",
            "Banco;Bench;OTRO",
            "Balón medicinal;Medicine ball;OTRO",
            "Fitball;Exercise ball;OTRO"
    })
    void equipamiento(String equipo, String equipoEn, String esperado) {
        int id = ejercicio(equipo, equipoEn, "gp085 " + equipo, "gp085 " + equipoEn);
        rellenar();
        assertThat(jdbc.queryForObject("SELECT equipamiento FROM ejercicios WHERE id = ?", String.class, id))
                .isEqualTo(esperado);
    }

    @Test
    @DisplayName("sin equipo de ninguna clase → OTRO")
    void sin_equipo_informado() {
        int id = ejercicio(null, null, "gp085 nada", "gp085 nothing");
        rellenar();
        assertThat(jdbc.queryForObject("SELECT equipamiento FROM ejercicios WHERE id = ?", String.class, id))
                .isEqualTo("OTRO");
    }

    @Test
    @DisplayName("revisado si el español ya es distinto del inglés o no hay inglés; igual, sin revisar")
    void nombre_revisado() {
        int traducido = ejercicio("Barra", "Barbell", "gp085 Sentadilla", "gp085 Squat");
        int igual = ejercicio("Barra", "Barbell", "gp085 Hip thrust", "gp085 Hip thrust");
        int soloMayusculas = ejercicio("Barra", "Barbell", "gp085 hip THRUST", "gp085 Hip thrust");
        int sinIngles = ejercicio("Barra", "Barbell", "gp085 Solo español", null);
        rellenar();
        String sql = "SELECT nombre_revisado FROM ejercicios WHERE id = ?";
        assertThat(jdbc.queryForObject(sql, Boolean.class, traducido)).isTrue();
        assertThat(jdbc.queryForObject(sql, Boolean.class, igual)).isFalse();
        assertThat(jdbc.queryForObject(sql, Boolean.class, soloMayusculas)).isFalse();
        assertThat(jdbc.queryForObject(sql, Boolean.class, sinIngles)).isTrue();
    }
}
