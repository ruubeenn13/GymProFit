package com.gymprofit.api.config;

import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// ============================================================
// ErroresTareasConfig — una excepción que se escapa de una tarea @Scheduled llega a
// Sentry (GP-221).
//
// El planificador de Spring la recoge y solo la escribe en el log, así que para Sentry no
// es «sin capturar» y no la vería. Este manejador hace lo mismo que el de serie (ERROR en
// el log) y además la manda. Sin SENTRY_DSN, Sentry.captureException no hace nada.
// ============================================================
@Configuration
public class ErroresTareasConfig {

    private static final Logger logger = LoggerFactory.getLogger(ErroresTareasConfig.class);

    /**
     * Pone el manejador de errores al planificador que crea Spring Boot.
     *
     * @return el ajuste que aplica Spring Boot al crear el planificador
     */
    @Bean
    public ThreadPoolTaskSchedulerCustomizer avisarErroresDeTareas() {
        return planificador -> planificador.setErrorHandler(error -> {
            logger.error("Error inesperado en una tarea programada", error);
            Sentry.captureException(error);
        });
    }
}
