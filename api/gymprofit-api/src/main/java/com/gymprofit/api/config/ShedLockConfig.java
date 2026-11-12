package com.gymprofit.api.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

// ============================================================
// ShedLockConfig — una sola ejecución de cada tarea programada aunque haya varias
// instancias de la API (GP-221).
//
// Cada instancia tiene su propio planificador: con dos, el recordatorio de las 08:00
// saldría dos veces y cada notificación programada se mandaría dos veces. Las tareas
// con @SchedulerLock escriben una fila en la tabla shedlock antes de correr; la que la
// consigue corre, la otra se salta el ciclo.
//
//   · usingDbTime(): las horas del bloqueo las pone la base (UTC), no el reloj de cada
//     instancia, que no tienen por qué ir a la par. Además usa el SQL propio de
//     MySQL/MariaDB que evita el choque de INSERT cuando las dos llegan a la vez.
//   · defaultLockAtMostFor: solo es el tope si una tarea se olvida de poner el suyo.
//     Cada tarea lleva el propio, medido, y explica de dónde sale.
//
//   · NO se borran filas de shedlock con la API en marcha: ShedLock recuerda en memoria
//     qué filas existen y desde entonces solo hace UPDATE, así que una fila borrada deja
//     esa tarea sin correr en esa instancia hasta que se reinicie. Para soltar un bloqueo,
//     UPDATE shedlock SET lock_until = locked_at WHERE name = '...'.
//
// VigilanteMemoria NO se bloquea: cada instancia vigila su propia memoria.
// ============================================================
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ShedLockConfig {

    /**
     * Lo menos que se queda cogido un bloqueo, aunque la tarea acabe antes. Sin esto, si
     * una instancia termina en 20 ms y la otra dispara su cron 300 ms más tarde (sus relojes
     * no van exactos a la par), encontraría el bloqueo ya suelto y repetiría el ciclo.
     * 50 s cubre de sobra ese desfase y cabe en el ciclo más corto, el barrido de cada 60 s
     * de las notificaciones programadas: así ese barrido sigue saliendo una vez por minuto.
     * Los tests lo ponen a cero (config/application.properties de test) para poder llamar
     * a una tarea dos veces seguidas; se cambia con {@code app.tareas.bloqueo-minimo}.
     */
    public static final String BLOQUEO_MINIMO = "PT50S";

    /** El valor que leen las anotaciones: la propiedad, y si no está, {@link #BLOQUEO_MINIMO}. */
    public static final String BLOQUEO_MINIMO_PROPIEDAD = "${app.tareas.bloqueo-minimo:" + BLOQUEO_MINIMO + "}";

    /**
     * Proveedor de bloqueos sobre la base de la API (tabla {@code shedlock}, migración
     * V202611121000). Las operaciones de bloqueo van en su propia transacción.
     *
     * <p>El gestor de transacciones se pasa a mano, el de JPA: sin él, ShedLock crea uno
     * de JDBC sobre el mismo pool, y si la tarea ya estaba dentro de una transacción de JPA
     * (la de su {@code @Transactional}, o la de un test) el bloqueo la confirmaba a medias.
     * Se vio en el primer intento de GP-221: un test transaccional dejó sus filas escritas
     * en la base en vez de deshacerlas. Con el de JPA, el bloqueo suspende la transacción
     * que haya y abre la suya.
     *
     * @param dataSource el mismo pool de Hikari que usa la API
     * @param transacciones el gestor de transacciones de JPA de la API
     * @return el proveedor que usan las tareas con {@code @SchedulerLock}
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource, PlatformTransactionManager transacciones) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .withTransactionManager(transacciones)
                        .usingDbTime()
                        .build());
    }
}
