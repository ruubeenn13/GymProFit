package com.gymprofit.api.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.pattern.ValidatePattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
public class FlywayConfig {

    @Autowired
    private Environment environment;

    @Bean(initMethod = "migrate")
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

    private boolean isDevProfile() {
        String[] profiles = environment.getActiveProfiles();

        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }

        return Arrays.asList(profiles).contains("dev");
    }
}