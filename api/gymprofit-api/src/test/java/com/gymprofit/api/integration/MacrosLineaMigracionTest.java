package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoComida;
import com.gymprofit.api.mappers.AlimentoComidaMapper;
import com.gymprofit.api.repository.jpa.IAlimentoComidaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// MacrosLineaMigracionTest — las líneas de antes, con los macros que se leían (1.6.6, A1)
// Como RacionAntesQueEnvaseMigracionTest: contra la MariaDB de verdad, volviendo a
// ejecutar el fichero de relleno sobre filas preparadas dentro de la transacción. Lo que
// se compara es lo que devolvía el mapper antes del lote (valor × gramos / 100, a dos
// decimales con la mitad hacia arriba, 0 sin dato), escrito aquí a mano.
// ============================================================
@SpringBootTest
@Transactional
@DisplayName("Lote 1.6.6 — la migración deja cada línea como se leía")
class MacrosLineaMigracionTest {

    private static final String SCRIPT = "db/migration/V202610141001__Macros_en_alimentos_comida_rellenar.sql";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private IAlimentoComidaRepository lineaRepository;

    @Autowired
    private AlimentoComidaMapper mapper;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    @Test
    @DisplayName("Flyway ha aplicado las dos")
    void flyway_las_aplica() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version IN "
                + "('202610141000', '202610141001') AND success = 1", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("cada línea, con los macros de antes; sin dato, 0; y sin cambiar ids")
    void lineas() {
        int usuario = usuario();
        int nueces = alimento("15.23", "13.71", "65.21");
        int agua = alimento(null, null, null);
        int comida = comida(usuario, 999, "0", "0", "0");
        int l1 = linea(comida, nueces, "33.00", 216);
        int l2 = linea(comida, agua, "250.00", 0);

        ejecutar();

        Map<String, Object> f1 = jdbc.queryForMap("SELECT * FROM alimentos_comida WHERE id = ?", l1);
        assertThat((BigDecimal) f1.get("proteinas_totales")).isEqualByComparingTo(antes("15.23", "33"));
        assertThat((BigDecimal) f1.get("carbohidratos_totales")).isEqualByComparingTo(antes("13.71", "33"));
        assertThat((BigDecimal) f1.get("grasas_totales")).isEqualByComparingTo(antes("65.21", "33"));
        assertThat((BigDecimal) f1.get("proteinas_totales")).isEqualByComparingTo("5.03");
        Map<String, Object> f2 = jdbc.queryForMap("SELECT * FROM alimentos_comida WHERE id = ?", l2);
        assertThat((BigDecimal) f2.get("proteinas_totales")).isEqualByComparingTo("0");
        assertThat(((Number) f1.get("calorias_totales")).intValue()).isEqualTo(216);

        // Y lo que sale por la API es lo que salía.
        em.clear();
        AlimentoComida linea = lineaRepository.findById(l1).orElseThrow();
        assertThat(mapper.toDTO(linea).getGrasasTotales()).isEqualByComparingTo(antes("65.21", "33"));
    }

    @Test
    @DisplayName("los totales de cada comida con líneas, la suma de sus líneas; una sin líneas no se toca")
    void totales() {
        int usuario = usuario();
        int nueces = alimento("15.23", "13.71", "65.21");
        int avena = alimento("13", "60", "7");
        int comida = comida(usuario, 1, "999", "1", "1");
        linea(comida, nueces, "33.00", 216);
        linea(comida, avena, "50.00", 190);
        int vacia = comida(usuario, 300, "20", "30", "10");

        ejecutar();

        Map<String, Object> c = jdbc.queryForMap("SELECT * FROM comidas WHERE id = ?", comida);
        assertThat(((Number) c.get("total_calorias")).intValue()).isEqualTo(406);
        assertThat((BigDecimal) c.get("total_proteinas"))
                .isEqualByComparingTo(antes("15.23", "33").add(antes("13", "50")));
        assertThat((BigDecimal) c.get("total_grasas"))
                .isEqualByComparingTo(antes("65.21", "33").add(antes("7", "50")));
        Map<String, Object> v = jdbc.queryForMap("SELECT * FROM comidas WHERE id = ?", vacia);
        assertThat(((Number) v.get("total_calorias")).intValue()).isEqualTo(300);
        assertThat((BigDecimal) v.get("total_proteinas")).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("ejecutarla dos veces deja lo mismo")
    void idempotente() {
        int usuario = usuario();
        int nueces = alimento("15.23", "13.71", "65.21");
        int comida = comida(usuario, 0, "0", "0", "0");
        int l = linea(comida, nueces, "33.00", 216);
        ejecutar();
        ejecutar();
        assertThat(jdbc.queryForObject("SELECT proteinas_totales FROM alimentos_comida WHERE id = ?",
                BigDecimal.class, l)).isEqualByComparingTo("5.03");
        assertThat(jdbc.queryForObject("SELECT total_proteinas FROM comidas WHERE id = ?",
                BigDecimal.class, comida)).isEqualByComparingTo("5.03");
    }

    // --- Ayudas -------------------------------------------------------------

    // La cuenta que hacía AlimentoComidaMapper antes del lote.
    private static BigDecimal antes(String porCien, String gramos) {
        return new BigDecimal(porCien).multiply(new BigDecimal(gramos)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private int usuario() {
        jdbc.update("INSERT INTO usuarios (username, password, email, fecha_registro, activo) "
                + "VALUES ('__kzmigmacros__', 'x', '__kzmigmacros__@test.local', NOW(), 1)");
        return jdbc.queryForObject("SELECT id FROM usuarios WHERE username = '__kzmigmacros__'", Integer.class);
    }

    private int alimento(String p, String c, String g) {
        jdbc.update("INSERT INTO alimentos (nombre, calorias, proteinas, carbohidratos, grasas, activo) "
                + "VALUES ('Alimento kzmigmacros', 100, ?, ?, ?, 1)", dec(p), dec(c), dec(g));
        return jdbc.queryForObject("SELECT MAX(id) FROM alimentos", Integer.class);
    }

    private int comida(int usuario, int kcal, String p, String c, String g) {
        jdbc.update("INSERT INTO comidas (fecha, tipo_comida, total_calorias, total_proteinas, total_carbohidratos, "
                + "total_grasas, usuario_id) VALUES (NOW(), 'CENA', ?, ?, ?, ?, ?)", kcal, dec(p), dec(c), dec(g), usuario);
        return jdbc.queryForObject("SELECT MAX(id) FROM comidas", Integer.class);
    }

    private int linea(int comida, int alimento, String gramos, int kcal) {
        jdbc.update("INSERT INTO alimentos_comida (cantidad_gramos, calorias_totales, comida_id, alimento_id) "
                + "VALUES (?, ?, ?, ?)", new BigDecimal(gramos), kcal, comida, alimento);
        return jdbc.queryForObject("SELECT MAX(id) FROM alimentos_comida", Integer.class);
    }

    private static BigDecimal dec(String s) {
        return s == null ? null : new BigDecimal(s);
    }

    private void ejecutar() {
        new ResourceDatabasePopulator(new ClassPathResource(SCRIPT)).execute(dataSource);
    }
}
