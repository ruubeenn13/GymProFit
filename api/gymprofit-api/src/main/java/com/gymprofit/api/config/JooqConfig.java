package com.gymprofit.api.config;

import org.jooq.impl.DataSourceConnectionProvider;
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

    // Contexto DSL de jOOQ, punto de entrada para construir consultas fluidas.
    @Bean
    public DefaultDSLContext dsl(org.jooq.Configuration configuration) {
        return new DefaultDSLContext(configuration);
    }
}
