package com.gymprofit.api.integration;

import com.gymprofit.api.GymProFitApiApplication;
import com.gymprofit.api.config.ShedLockConfig;
import com.gymprofit.api.repository.jpa.IDeviceTokenRepository;
import com.gymprofit.api.repository.jpa.INotificacionRepository;
import com.gymprofit.api.repository.jpa.IPasswordResetCodigoRepository;
import com.gymprofit.api.repository.jpa.IRefreshTokenRepository;
import com.gymprofit.api.service.auth.RefreshTokenCleanupTask;
import com.gymprofit.api.service.notificacion.NotificacionProgramadaTask;
import com.gymprofit.api.service.notificacion.RecordatorioNotificacionesTask;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// TareasUnaVezConDosInstanciasTest — con dos instancias de la API contra la misma base,
// cada tarea programada corre UNA vez por ciclo (GP-221).
//
// Levanta dos contextos completos de la aplicación, como dos instancias en Render, y
// dispara a la vez en las dos cada método @Scheduled de las tres tareas bloqueadas. El
// recuento sale de la primera consulta que hace cada tarea, contada en un envoltorio de
// su repositorio que además devuelve vacío: así la tarea no toca datos de nadie y lo
// único que se mide es cuántas veces entró.
//
//   · Los métodos salen por reflexión de las clases: si mañana se añade un @Scheduled
//     sin @SchedulerLock a una de ellas, este test se pone rojo solo.
//   · El planificador real de los dos contextos se sustituye por uno que no programa
//     nada: si no, el barrido de cada minuto de las notificaciones podría coger el
//     bloqueo por su cuenta en mitad de la prueba.
//   · El bloqueo mínimo es el de producción. El resto de la suite lo pone a cero
//     (config/application.properties de test) para poder llamar a una tarea dos veces
//     seguidas; aquí se devuelve a su valor, que es justo lo que se prueba.
// ============================================================
@DisplayName("ShedLock — con dos instancias, cada tarea programada corre una vez por ciclo")
class TareasUnaVezConDosInstanciasTest {

    /** Primera consulta de cada tarea: entra una vez por ejecución. */
    private static final Map<Class<?>, List<String>> ENTRADAS = Map.of(
            IDeviceTokenRepository.class, List.of(
                    "findUsuarioIdsConAvisosComidas",
                    "findUsuarioIdsConAvisosEntrenar",
                    "findUsuarioIdsConAvisosProgreso"),
            INotificacionRepository.class, List.of("findByPushEnviadaFalseAndFechaProgramadaLessThanEqual"),
            IRefreshTokenRepository.class, List.of("borrarRevocadosOExpirados"),
            IPasswordResetCodigoRepository.class, List.of());

    /** Ejecuciones contadas, compartidas por las dos instancias. */
    private static final AtomicInteger ENTRADAS_CONTADAS = new AtomicInteger();

    private static ConfigurableApplicationContext instanciaA;
    private static ConfigurableApplicationContext instanciaB;

    @BeforeAll
    static void levantarDosInstancias() {
        instanciaA = arrancar();
        instanciaB = arrancar();
    }

    @AfterAll
    static void apagar() {
        // Los bloqueos que deja la prueba duran el mínimo de producción: si se quedaran,
        // otro test que corra en el mismo minuto vería sus tareas saltadas.
        try {
            if (instanciaA != null) quitarBloqueos();
        } finally {
            if (instanciaA != null) instanciaA.close();
            if (instanciaB != null) instanciaB.close();
        }
    }

    @BeforeEach
    void sinBloqueosNiCuentas() {
        quitarBloqueos();
        ENTRADAS_CONTADAS.set(0);
    }

    /**
     * Suelta los bloqueos y comprueba que no queda ninguno cogido: si quedara, otro test lo
     * pagaría. Se sueltan, NO se borran las filas: ShedLock recuerda en memoria qué filas
     * existen y desde entonces solo hace UPDATE, así que un contexto que ya conocía la fila
     * la daría por cogida para siempre. Pasó en el CI: el DELETE de esta prueba dejaba sin
     * correr los recordatorios en el contexto compartido de los demás tests.
     */
    private static void quitarBloqueos() {
        JdbcTemplate jdbc = instanciaA.getBean(JdbcTemplate.class);
        jdbc.update("UPDATE shedlock SET lock_until = locked_at");
        Integer cogidos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM shedlock WHERE lock_until > UTC_TIMESTAMP(3)", Integer.class);
        assertThat(cogidos).as("bloqueos aún cogidos").isZero();
    }

    static Stream<Arguments> tareasProgramadas() {
        return Stream.of(NotificacionProgramadaTask.class, RecordatorioNotificacionesTask.class,
                        RefreshTokenCleanupTask.class)
                .flatMap(clase -> Arrays.stream(clase.getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(Scheduled.class))
                        .map(m -> Arguments.of(clase.getSimpleName() + "." + m.getName(), clase, m)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tareasProgramadas")
    void corre_una_sola_vez_aunque_las_dos_instancias_la_disparen(String nombre, Class<?> clase, Method metodo)
            throws Exception {
        Object enA = instanciaA.getBean(clase);
        Object enB = instanciaB.getBean(clase);

        // Las dos a la vez, como dos planificadores con el mismo cron.
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = hilos.submit(() -> invocarTrasSalida(salida, enA, metodo));
            Future<?> b = hilos.submit(() -> invocarTrasSalida(salida, enB, metodo));
            salida.countDown();
            a.get(30, TimeUnit.SECONDS);
            b.get(30, TimeUnit.SECONDS);
        } finally {
            hilos.shutdownNow();
        }

        assertThat(ENTRADAS_CONTADAS.get())
                .as("%s se ha ejecutado en las dos instancias en el mismo ciclo", nombre)
                .isEqualTo(1);
    }

    private static Void invocarTrasSalida(CountDownLatch salida, Object bean, Method metodo) throws Exception {
        salida.await();
        try {
            metodo.invoke(bean);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        }
        return null;
    }

    // --- Arranque de una instancia ---------------------------------------------

    private static ConfigurableApplicationContext arrancar() {
        return new SpringApplicationBuilder(GymProFitApiApplication.class)
                .initializers(contexto -> {
                    contexto.getBeanFactory().registerSingleton("taskScheduler", planificadorQueNoProgramaNada());
                    contexto.getBeanFactory().addBeanPostProcessor(new ContarEntradas());
                })
                .run("--server.port=0",
                        "--app.tareas.bloqueo-minimo=" + ShedLockConfig.BLOQUEO_MINIMO);
    }

    /** Envuelve los repositorios de entrada para contar y devolver vacío. */
    private static final class ContarEntradas implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(Object bean, String nombre) {
            for (Map.Entry<Class<?>, List<String>> e : ENTRADAS.entrySet()) {
                if (e.getKey().isInstance(bean)) return envolver(e.getKey(), bean, e.getValue());
            }
            return bean;
        }

        private static Object envolver(Class<?> interfaz, Object real, List<String> entradas) {
            InvocationHandler h = (proxy, m, args) -> {
                if (entradas.contains(m.getName())) {
                    ENTRADAS_CONTADAS.incrementAndGet();
                    return vacio(m.getReturnType());
                }
                // Lo que no es de entrada (purga de códigos, etc.) tampoco toca la base.
                if (m.getDeclaringClass() == interfaz && m.getName().startsWith("borrar")) {
                    return vacio(m.getReturnType());
                }
                try {
                    return m.invoke(real, args);
                } catch (InvocationTargetException ex) {
                    throw ex.getCause();
                }
            };
            return Proxy.newProxyInstance(interfaz.getClassLoader(), new Class<?>[]{interfaz}, h);
        }
    }

    private static Object vacio(Class<?> tipo) {
        if (tipo == int.class || tipo == Integer.class) return 0;
        if (tipo == long.class || tipo == Long.class) return 0L;
        if (tipo == boolean.class || tipo == Boolean.class) return false;
        if (List.class.isAssignableFrom(tipo)) return List.of();
        return null;
    }

    /** Un TaskScheduler que acepta todo y no ejecuta nada. */
    private static TaskScheduler planificadorQueNoProgramaNada() {
        InvocationHandler futuro = (p, m, a) -> switch (m.getName()) {
            case "cancel", "isCancelled", "isDone" -> true;
            case "getDelay" -> 0L;
            case "compareTo" -> 0;
            case "hashCode" -> System.identityHashCode(p);
            case "equals" -> p == a[0];
            case "toString" -> "tarea sin programar";
            default -> null;
        };
        InvocationHandler planificador = (p, m, a) -> {
            if (m.getDeclaringClass() == Object.class) {
                return switch (m.getName()) {
                    case "hashCode" -> System.identityHashCode(p);
                    case "equals" -> p == a[0];
                    default -> "planificador que no programa nada";
                };
            }
            if (m.getName().equals("getClock")) return java.time.Clock.systemDefaultZone();
            return Proxy.newProxyInstance(TaskScheduler.class.getClassLoader(),
                    new Class<?>[]{java.util.concurrent.ScheduledFuture.class}, futuro);
        };
        return (TaskScheduler) Proxy.newProxyInstance(TaskScheduler.class.getClassLoader(),
                new Class<?>[]{TaskScheduler.class}, planificador);
    }
}
