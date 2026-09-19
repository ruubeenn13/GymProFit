package com.gymprofit.api.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.service.email.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

// ============================================================
// CorreoAsyncConfigTest — el correo sale del hilo de la petición, y sale acotado
//
// Tres cosas que tienen que seguir siendo verdad y que no se ven compilando:
// que el pool tenga techo, que el trabajo ocurra de verdad en otro hilo, y que
// desbordarlo no se convierta en un error de la petición.
// ============================================================
@DisplayName("CorreoAsyncConfig — el pool acotado de correo")
class CorreoAsyncConfigTest {

    private ListAppender<ILoggingEvent> registro;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void engancharElLog() {
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CorreoAsyncConfig.class);
        registro = new ListAppender<>();
        registro.start();
        logger.addAppender(registro);
    }

    @AfterEach
    void soltarElLog() {
        logger.detachAppender(registro);
        registro.stop();
    }

    @Test
    @DisplayName("el pool tiene techo de hilos y de cola")
    void el_pool_esta_acotado() {
        ThreadPoolTaskExecutor executor = nuevoExecutor();

        // Un pool sin techo no arregla el problema, lo traslada: la instancia se queda
        // sin memoria creando hilos en vez de sin hilos de petición.
        assertThat(executor.getMaxPoolSize())
                .as("el número de hilos tiene que estar acotado y ser pequeño")
                .isPositive()
                .isLessThanOrEqualTo(4);

        // Core y max iguales: con este executor el pool solo crece por encima del core
        // cuando la cola está llena, así que un max mayor no daría más paralelismo.
        assertThat(executor.getCorePoolSize()).isEqualTo(executor.getMaxPoolSize());

        // La cola también tiene tope. Sin él, una caída del proveedor se acumula en
        // memoria en vez de notarse.
        assertThat(capacidadDeCola(executor))
                .as("la cola tiene que estar acotada")
                .isPositive()
                .isLessThan(Integer.MAX_VALUE);

        executor.shutdown();
    }

    @Test
    @DisplayName("la tarea anotada se ejecuta en un hilo del pool de correo, no en el que llama")
    void el_trabajo_sale_del_hilo_que_llama() throws Exception {
        try (AnnotationConfigApplicationContext contexto =
                     new AnnotationConfigApplicationContext(CorreoAsyncConfig.class, ConfigDePrueba.class)) {

            TareaDePrueba tarea = contexto.getBean(TareaDePrueba.class);
            String hiloQueLlama = Thread.currentThread().getName();

            tarea.ejecutar();

            // Por el método y no por el campo: el bean es un proxy y sus campos están vacíos.
            String hiloQueEjecuta = tarea.hilo().get(5, TimeUnit.SECONDS);
            assertThat(hiloQueEjecuta).isNotEqualTo(hiloQueLlama);
            assertThat(hiloQueEjecuta)
                    .as("y es un hilo del pool de correo, no el executor general de Spring")
                    .startsWith("correo-");
        }
    }

    @Test
    @DisplayName("con la cola llena se descarta el envío y queda un ERROR, sin reventar al que llama")
    void la_cola_llena_descarta_y_deja_error_en_el_log() {
        ThreadPoolTaskExecutor executor = nuevoExecutor();
        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
        AtomicBoolean seEjecuto = new AtomicBoolean(false);

        // La política por defecto lanzaría aquí, en el hilo de la petición, que es justo
        // el hilo del que queríamos sacar el envío: una saturación del correo se
        // convertiría en un 500 de POST /auth/forgot-password.
        assertThatCode(() -> pool.getRejectedExecutionHandler()
                .rejectedExecution(() -> seEjecuto.set(true), pool))
                .doesNotThrowAnyException();

        // Tampoco vale correrla en el hilo que llama (CallerRunsPolicy): sería volver a
        // bloquear la petición justo cuando el sistema está saturado.
        assertThat(seEjecuto).isFalse();

        assertThat(registro.list)
                .as("descartar un correo en silencio no dejaría forma de enterarse")
                .anyMatch(evento -> evento.getLevel() == Level.ERROR);

        executor.shutdown();
    }

    @Test
    @DisplayName("el envío del código sigue marcado como asíncrono y en el pool de correo")
    void el_envio_del_codigo_esta_marcado_como_asincrono() throws Exception {
        Method envio = EmailService.class.getMethod(
                "enviarCodigoRecuperacion", Usuario.class, String.class, int.class);

        Async async = envio.getAnnotation(Async.class);
        assertThat(async).as("sin @Async el envío vuelve al hilo de la petición").isNotNull();
        assertThat(async.value())
                .as("y tiene que ir al pool de correo, no al executor general")
                .isEqualTo(CorreoAsyncConfig.EXECUTOR);

        // El código viaja como parámetro y no se relee: EmailService no tiene por dónde
        // hacerlo. Es la salvaguarda contra la trampa de @Async + @Transactional, donde la
        // tarea arranca antes de que confirme la transacción que guardó el código.
        assertThat(EmailService.class.getDeclaredFields())
                .as("el que envía no debe poder consultar la base de datos")
                .noneMatch(campo -> campo.getType().getSimpleName().endsWith("Repository"));
    }

    // --- Andamiaje ----------------------------------------------------------

    private ThreadPoolTaskExecutor nuevoExecutor() {
        return (ThreadPoolTaskExecutor) new CorreoAsyncConfig().correoExecutor();
    }

    // queueCapacity no tiene getter público; se lee del campo.
    private int capacidadDeCola(ThreadPoolTaskExecutor executor) {
        try {
            Field campo = ThreadPoolTaskExecutor.class.getDeclaredField("queueCapacity");
            campo.setAccessible(true);
            return (int) campo.get(executor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo leer la capacidad de la cola", e);
        }
    }

    @Configuration
    static class ConfigDePrueba {
        @Bean
        TareaDePrueba tareaDePrueba() {
            return new TareaDePrueba();
        }
    }

    // Sustituye al envío real: lo único que interesa es en qué hilo acaba corriendo.
    static class TareaDePrueba {

        private final CompletableFuture<String> hilo = new CompletableFuture<>();

        public CompletableFuture<String> hilo() {
            return hilo;
        }

        @Async(CorreoAsyncConfig.EXECUTOR)
        public void ejecutar() {
            hilo.complete(Thread.currentThread().getName());
        }
    }
}
