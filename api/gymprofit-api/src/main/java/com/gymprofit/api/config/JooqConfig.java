package com.gymprofit.api.config;

import org.jooq.impl.DataSourceConnectionProvider;
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

// ============================================================
// JooqConfig — configuración de jOOQ para consultas SQL complejas
// Registra el DSLContext de jOOQ usando el DataSource de Spring, envuelto
// en un proxy que respeta las transacciones gestionadas por Spring.
// Se usa para consultas/joins complejos que no encajan bien en JPA.
// ============================================================
@Configuration
public class JooqConfig {

    // Proveedor de conexiones jOOQ que reutiliza la conexión de la transacción Spring activa.
    @Bean
    public DataSourceConnectionProvider connectionProvider(DataSource dataSource) {
        return new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource));
    }

    // Sin el esquema en el SQL (GP-149): las clases generadas llevan el de la base con la
    // que se generaron, `gymprofit_db`, y jOOQ lo escribía delante de cada tabla. Con una
    // base de otro nombre en el mismo servidor —la que se crea para probar en local—,
    // las consultas leían `gymprofit_db` y no la de la conexión. Sin él, mandan la URL y
    // la base de la conexión, como en JPA.
    @Bean
    public DefaultConfigurationCustomizer sinEsquemaEnElSql() {
        return c -> c.settings().withRenderSchema(false);
    }

    // Contexto DSL de jOOQ, punto de entrada para construir consultas fluidas.
    @Bean
    public DefaultDSLContext dsl(org.jooq.Configuration configuration) {
        return new DefaultDSLContext(configuration);
    }
}
