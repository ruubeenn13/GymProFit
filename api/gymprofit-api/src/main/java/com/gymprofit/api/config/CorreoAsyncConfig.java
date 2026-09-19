package com.gymprofit.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// ============================================================
// CorreoAsyncConfig — el pool que entrega los correos fuera del hilo de la petición
//
// El envío del código de recuperación era un POST síncrono a Brevo DENTRO de
// POST /auth/forgot-password. Con el proveedor lento, cada petición retenía su hilo
// de Tomcat hasta el timeout, y en una instancia de 512 MB los hilos ocupados se
// acumulan hasta que deja de responder todo lo demás, no solo el correo.
//
// El pool es pequeño y ACOTADO a propósito. Un executor sin límite —un cachedThreadPool,
// o una cola sin tope— no arregla el problema, lo traslada: en vez de quedarse sin hilos
// de petición, la instancia se queda sin memoria creando hilos o encolando tareas que
// nadie llega a atender. Dos hilos y cincuenta huecos de cola son de sobra para el
// volumen de correo de esta aplicación, y ponen un techo conocido al daño.
//
// Cuando la cola se llena NO se rechaza hacia el que llama. La política por defecto
// (AbortPolicy) lanza en el hilo de la petición, que es justo el hilo del que queríamos
// sacar esto: convertiría una saturación del correo en un 500 del endpoint. Se descarta
// dejando un ERROR en el log, que es la única señal posible cuando el fallo ya no puede
// viajar en la respuesta.
// ============================================================
@Configuration
@EnableAsync
public class CorreoAsyncConfig {

    private static final Logger logger = LoggerFactory.getLogger(CorreoAsyncConfig.class);

    // Nombre del bean, para qualificar el @Async y no depender del executor por defecto
    // de Spring Boot, que lo comparte todo lo demás.
    public static final String EXECUTOR = "correoExecutor";

    // Hilos fijos. Core y max iguales: con ThreadPoolTaskExecutor el pool solo crece por
    // encima del core cuando la cola está LLENA, así que un max mayor no daría más
    // paralelismo hasta las 50 tareas encoladas.
    static final int HILOS = 2;

    // Techo de tareas en espera. Con cola acotada, una caída del proveedor se nota como
    // correos descartados con su ERROR, y no como memoria que crece sin parar.
    static final int COLA = 50;

    // Margen de cortesía en el apagado: lo justo para no cortar un envío a medias en un
    // redespliegue, sin retrasar el arranque de la instancia nueva.
    static final int SEGUNDOS_DE_CIERRE = 15;

    /**
     * Pool acotado para los correos transaccionales.
     *
     * @return executor de dos hilos, cola de 50 y descarte registrado al desbordar.
     */
    @Bean(name = EXECUTOR)
    public Executor correoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(HILOS);
        executor.setMaxPoolSize(HILOS);
        executor.setQueueCapacity(COLA);
        executor.setThreadNamePrefix("correo-");

        // Descartar y dejar rastro. El usuario no recibirá ese correo, pero la petición
        // que lo pidió ya ha respondido y no hay forma de contárselo por ahí.
        executor.setRejectedExecutionHandler((tarea, pool) ->
                logger.error("Cola de correo llena ({} en espera, {} hilos activos): se DESCARTA un envío. "
                                + "Alguien no va a recibir su correo. Revisar si el proveedor está respondiendo.",
                        pool.getQueue().size(), pool.getActiveCount()));

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(SEGUNDOS_DE_CIERRE);
        executor.initialize();
        return executor;
    }
}
