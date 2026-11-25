package com.gymprofit.api.pruebas;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ContextCustomizerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ============================================================
// BarreraBaseLocalTest — GP-282. Ninguna de estas pruebas abre una conexión: la URL de
// TiDB es inventada y Hikari no conecta hasta el primer getConnection(), que la barrera
// impide que llegue.
// ============================================================
@DisplayName("Barrera de la base local en los tests (GP-282)")
class BarreraBaseLocalTest {

    private static final String TIDB =
            "jdbc:mariadb://gateway01.eu-central-1.prod.aws.tidbcloud.com:4000/gymprofit_db?sslMode=verify-full";
    private static final String LOCAL = "jdbc:mariadb://localhost:3308/gymprofit_db?useUnicode=true";
    private static final String CI = "jdbc:mariadb://127.0.0.1:3306/gymprofit_db?useUnicode=true&characterEncoding=UTF-8";

    @Test
    @DisplayName("una URL de TiDB no pasa, y el mensaje dice por qué")
    void tidbNoPasa() {
        assertThatThrownBy(() -> BarreraBaseLocal.comprobar(TIDB, "spring.datasource.url"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("gateway01.eu-central-1.prod.aws.tidbcloud.com")
                .hasMessageContaining("nunca a la base de producción");
    }

    @ParameterizedTest
    @ValueSource(strings = {LOCAL, CI, "jdbc:mysql://[::1]:3306/db", "jdbc:mariadb://localhost/db",
            "jdbc:mariadb://localhost:3308,127.0.0.1:3306/db"})
    @DisplayName("la local, la del CI e IPv6 local pasan")
    void localesPasan(String url) {
        assertThatCode(() -> BarreraBaseLocal.comprobar(url, "spring.datasource.url")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mariadb://localhost.tidbcloud.com:4000/db",          // empieza por localhost, no lo es
            "jdbc:mariadb://localhost:3308,mysql-x.aivencloud.com:1/db", // un host local y otro no
            "jdbc:mariadb:replication://10.0.0.5:3306/db",              // red privada tampoco
            "jdbc:mariadb://address=(host=remoto)(port=3306)/db",       // forma que no se lee
            "jdbc:postgresql://localhost/db",                           // otro motor
            "jdbc:mariadb:localhost/db"                                 // sin //
    })
    @DisplayName("lo remoto, lo mezclado y lo que no se entiende no pasa")
    void remotasOIlegiblesNoPasan(String url) {
        assertThatThrownBy(() -> BarreraBaseLocal.comprobar(url, "spring.datasource.url"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("un contexto con un DataSource de TiDB no arranca, sin conectarse")
    void contextoConTidbNoArranca() {
        try (GenericApplicationContext contexto = contextoCon(TIDB)) {
            assertThatThrownBy(contexto::refresh)
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .rootCause().hasMessageContaining("tidbcloud.com");
        }
    }

    @Test
    @DisplayName("un contexto con la base local arranca (sin conectarse: Hikari es perezoso)")
    void contextoLocalArranca() {
        try (GenericApplicationContext contexto = contextoCon(CI)) {
            assertThatCode(contexto::refresh).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("spring.flyway.url remota tampoco pasa aunque el DataSource sea local")
    void flywayRemotaNoPasa() {
        try (GenericApplicationContext contexto = contextoCon(LOCAL)) {
            ((MockEnvironment) contexto.getEnvironment()).setProperty("spring.flyway.url", TIDB);
            assertThatThrownBy(contexto::refresh).rootCause().hasMessageContaining("spring.flyway.url");
        }
    }

    @Test
    @DisplayName("la fábrica está registrada en spring.factories, así que entra en todos los contextos")
    void registradaEnSpringFactories() {
        assertThat(SpringFactoriesLoader.loadFactories(ContextCustomizerFactory.class, getClass().getClassLoader()))
                .anyMatch(BarreraBaseLocal.Fabrica.class::isInstance);
    }

    @Test
    @DisplayName("el inicializador, que cubre las SpringApplication arrancadas a mano, también corta TiDB")
    void inicializadorCortaTidb() {
        GenericApplicationContext contexto = contextoSinBarrera(TIDB);
        new BarreraBaseLocal.Inicializador().initialize(contexto);
        try (contexto) {
            assertThatThrownBy(contexto::refresh).rootCause().hasMessageContaining("tidbcloud.com");
        }
    }

    @Test
    @DisplayName("el inicializador está registrado en spring.factories")
    void inicializadorRegistrado() {
        assertThat(SpringFactoriesLoader.forDefaultResourceLocation(getClass().getClassLoader())
                .load(ApplicationContextInitializer.class))
                .anyMatch(BarreraBaseLocal.Inicializador.class::isInstance);
    }

    // Un contexto mínimo con un Hikari a esa URL y la barrera puesta como en los tests reales.
    private static GenericApplicationContext contextoCon(String url) {
        GenericApplicationContext contexto = contextoSinBarrera(url);
        new BarreraBaseLocal.Fabrica().createContextCustomizer(BarreraBaseLocalTest.class, java.util.List.of())
                .customizeContext(contexto, null);
        return contexto;
    }

    private static GenericApplicationContext contextoSinBarrera(String url) {
        GenericApplicationContext contexto = new GenericApplicationContext();
        MockEnvironment entorno = new MockEnvironment().withProperty("spring.datasource.url", url);
        contexto.setEnvironment(entorno);
        contexto.registerBean("dataSource", HikariDataSource.class, () -> {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(url);
            return ds;
        });
        return contexto;
    }
}
