package com.gymprofit.api.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.pattern.ValidatePattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.Arrays;

// ============================================================
// FlywayConfig — configuración manual de Flyway para migraciones de BD
// Define el bean Flyway con las opciones de migración (rutas, baseline,
// patrones a ignorar) y repara el historial de migraciones automáticamente
// en el perfil "dev" antes de aplicar los cambios.
// ============================================================
@Configuration
public class FlywayConfig {

    @Autowired
    private Environment environment;

    // Construye y ejecuta Flyway: configura datasource, ubicación de scripts y baseline,
    // repara el historial en dev y aplica las migraciones pendientes.
    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .ignoreMigrationPatterns(
                        ValidatePattern.fromPattern("*:missing"),
                        ValidatePattern.fromPattern("*:future"))
                .load();

        if (isDevProfile()) {
            flyway.repair();
        }

        flyway.migrate();

        return flyway;
    }

    // Inicializador que engancha el bean Flyway al ciclo de vida de Spring sin volver a migrar
    // (la migración ya se ejecuta en el propio bean flyway()).
    @Bean
    public FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway, f -> {});
    }

    // Comprueba si el perfil activo (o el por defecto si no hay ninguno activo) es "dev".
    private boolean isDevProfile() {
        String[] profiles = environment.getActiveProfiles();

        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }

        return Arrays.asList(profiles).contains("dev");
    }
}
