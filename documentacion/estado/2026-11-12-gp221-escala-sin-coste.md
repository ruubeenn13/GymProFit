# GP-221 — escalar sin coste: ShedLock, Sentry y la prueba de carga de referencia

Rama `gp-221-escala-sin-coste`. Solo la parte sin coste de la ficha: pasos 2 y 4 y la prueba de carga. Los pasos 1, 3 y 5 (plan de pago, límite de peticiones compartido entre instancias, fotos a R2) no entran. Hikari, Tomcat y el plan no se tocan: esto mide y prepara.

## Commits

| Commit | Qué |
|---|---|
| `7871aa9` feat(api): cada tarea programada corre una sola vez con varias instancias | ShedLock con proveedor JDBC sobre la base (migración `V202611121000__Tabla_shedlock`), en `NotificacionProgramadaTask`, los diez generadores de `RecordatorioNotificacionesTask` y `RefreshTokenCleanupTask`. `VigilanteMemoria` queda fuera. |
| `35ec5b2` feat(api): los errores 500 de la API llegan a Sentry, sin datos personales | `sentry-spring-boot-starter-jakarta` 8.64.0, `FiltroDatosSentry`, `ErroresTareasConfig`, los 500 de `ControllerExceptionHandler` y `SENTRY_DSN` en `render.yaml` con `sync: false`. |
| `4b72d59` test(carga): prueba de carga reproducible de la API con k6 | `scripts/carga/` (`recorrido.js`, `correr.sh`, `README.md`). |
| `7ff185c` test(api): ShedLock dice en el log de los tests cuándo se salta una tarea | Log de ShedLock a DEBUG en los tests; con él se vio la causa del rojo del CI. |
| `35e2fc3` test(api): el test de dos instancias comprueba que no deja bloqueos | |
| `4399d3b` fix(api): el test de dos instancias suelta los bloqueos en vez de borrar sus filas | Ver abajo. |

## 1. ShedLock

- **Tabla** `shedlock` con el esquema de ShedLock para MySQL/MariaDB; `lock_until` con `DEFAULT` explícito para que un servidor con `explicit_defaults_for_timestamp` apagado no le ponga `ON UPDATE`. Sin FK ni entidad: `ddl-auto=validate` no la mira y la versión anterior arranca si se vuelve atrás.
- **`usingDbTime()`**: la hora del bloqueo la pone la base, no cada instancia.
- **Nombres**: `notificaciones-programadas`, `purga-refresh-tokens` y uno por generador (`recordatorio-desayuno`, `-almuerzo`, `-comida`, `-merienda`, `-cena`, `-inactividad`, `resumen-semanal`, `logro-proximo`, `recordatorio-medicion`, `recordatorio-objetivo`). Tras su primer ciclo, la tabla de producción tendrá doce filas: las tres tareas, con una por cada cron de los recordatorios.
- **`lockAtMostFor`, medido** (local, 300 usuarios con aviso, sin push; prueba de usar y tirar, no versionada):

  | Tarea | Medido | Tope | Por qué |
  |---|---|---|---|
  | Purga de tokens | 26 ms (300 caducados) | 10 min | Dos `DELETE`; si la instancia muere con el bloqueo, la purga sigue al día siguiente. |
  | Notificaciones programadas | 149 ms (300 vencidas, sin push) | 5 min | Pesa el envío a FCM (100-300 ms cada una): unas mil en el mismo minuto. Es también lo más que se retrasarían si una instancia muere con el bloqueo. |
  | Recordatorios (el más caro, la cena) | 2,3 s (≈8 ms por usuario) | 30 min | En producción, con FCM y la red hasta Aiven, ≈0,3 s por usuario: unos 6000. Cada cron es diario (el resumen, semanal): un bloqueo largo no tapa el ciclo siguiente. |

- **`lockAtLeastFor` = 50 s** (`ShedLockConfig.BLOQUEO_MINIMO`, propiedad `app.tareas.bloqueo-minimo`): sin él, si una instancia acaba en milisegundos y la otra dispara su cron un poco después, repetiría el ciclo. Cabe en el ciclo más corto (60 s). Los tests lo ponen a cero en `src/test/resources/config/application.properties` para poder llamar a una tarea dos veces seguidas.

### Lo que salió por el camino: el bloqueo confirmaba la transacción

Sin gestor de transacciones explícito, ShedLock crea uno de JDBC sobre el mismo pool. Llamado desde dentro de una transacción de JPA, la confirmaba: la primera medición, en un test `@Transactional`, dejó **303 cuentas** escritas en la base local (las tres de `AbstractOwnershipTest` y 300 de la medición, con 300 dispositivos y 900 notificaciones), y la suite entera cayó con «Duplicate entry '__idor_owner__'». En producción, una tarea `@Transactional` que fallara a medias habría dejado escrita la mitad. Arreglo: el `PlatformTransactionManager` de JPA va en el proveedor, como pide el README de ShedLock con Hibernate. Las filas se borraron de la base local con permiso del propietario (por id, comprobado antes que no tuvieran nada en otras tablas), y las tres `__idor_*` que dejó escapar el rojo a propósito, también.

### Y otra en el CI: borrar filas de `shedlock` deja tareas sin correr

Los tres primeros runs del CI del PR cayeron en `RecordatorioNotificacionesTaskTest` (4 de 4, «expected: <1> but was: <0>»), y en local no se reproducía, ni con la JVM en UTC ni con una MariaDB 11 como la del CI. Con el log de ShedLock a DEBUG se vio: «Not executing 'recordatorio-cena'. It's locked.», 20 s después de que `TareasUnaVezConDosInstanciasTest` borrara la tabla y comprobara que quedaba vacía.

La causa está en ShedLock (`StorageBasedLockProvider.doLock`, visto en el bytecode de 7.11.0): cada proveedor recuerda en memoria las filas que ya creó (`LockRecordRegistry`) y desde entonces solo hace `UPDATE … WHERE name = ? AND lock_until <= ahora`. El contexto compartido de la suite ya había creado la fila de la cena en `AvisosPorTipoTest`; el `DELETE` del test de dos instancias la quitó, y el `UPDATE` sobre nada se lee como «cogido». En local el orden de las clases lo tapaba.

Arreglo: el test suelta los bloqueos (`UPDATE shedlock SET lock_until = locked_at`) en vez de borrar las filas. **En producción vale igual**: borrar a mano una fila de `shedlock` con la API en marcha deja esa tarea sin correr en esa instancia hasta reiniciarla. Queda escrito en `ShedLockConfig`.

## 2. Sentry

- **Solo con `SENTRY_DSN`.** Ningún fichero define `sentry.dsn`; la autoconfiguración del SDK la pide, y Spring lee la variable como esa propiedad. Sin ella, ni beans ni SDK. **Con la variable vacía** (un `sync: false` sin rellenar) la autoconfiguración arranca, pero el SDK se queda apagado y no sale nada: probado con `SENTRY_DSN=` en el entorno.
- **Qué sale**: los 500 (`ControllerExceptionHandler.registrarError500`, porque ese manejador resuelve todas las excepciones antes que el resolver de Sentry), las excepciones de las tareas programadas (`ErroresTareasConfig`, el planificador solo las escribía en el log) y las no capturadas (integración de serie). **No salen** los 4xx ni el 502/503 (Open Food Facts caído o sin cupo): ya están en el log y no hay nada que arreglar.
- **Sin datos personales**: `send-default-pii=false`, `max-request-body-size=none`, sin trazas. `FiltroDatosSentry` lo garantiza aunque alguien cambie esas opciones: de la petición, solo método y ruta; sin usuario, breadcrumbs ni extras; correos, JWT y `Bearer …` tapados en el mensaje y en cada excepción.
- Lo que llegó al servidor falso con un 500 provocado (JWT en `Authorization`, correo y contraseña en el cuerpo y el correo en el mensaje): tipo de excepción, pila, `"request":{"url":"http://localhost/prueba-sentry/500","method":"POST"}` y `"value":"No se pudo guardar a [correo tapado]"`. Ni cabeceras, ni cuerpo, ni usuario, ni IP.

## 3. Prueba de carga

**Cómo**: la imagen del `Dockerfile` de Render en Docker con `--memory=512m` y `--cpus` 0,1 o 0,25, perfil `prod`, la MariaDB local y los topes de producción sin tocar (heap 160 MB, Hikari 5, Tomcat 20). Cada usuario virtual repite el recorrido de la app con 1-3 s de pausa entre pantallas (unas 19 peticiones por vuelta). Tres minutos por nivel. Nunca contra producción. Las cuentas `carga_*` se borraron al acabar cada pasada: 0 en la base al terminar.

**Calibración.** Render free da 0,1 de CPU. El arranque en Render ronda los 185-207 s (memoria de GP-187); aquí, a 0,1, tardó **150 s** en las dos últimas pasadas, así que el 0,1 local es comparable o algo más generoso que Render. La primera serie a 0,1 arrancó en **480 s** con la misma imagen y los mismos límites; no identifiqué qué cargaba el equipo en ese momento. Se descarta para las cifras y solo se usa para ver qué falla después de la CPU.

### Resultados

p95 de todas las peticiones del recorrido. Umbral: p95 < 800 ms y 0 % de errores.

**0,1 de CPU (Render free)**

| Usuarios | p95 | p99 | Mediana | Errores | Peticiones/s |
|---|---|---|---|---|---|
| 1 | 1663 ms | 5084 ms | 286 ms | 0 % | 0,8 |
| 2 | 2656 ms | 5082 ms | 485 ms | 0 % | 1,5 |
| 3 | 2668 ms | 5864 ms | 403 ms | 0 % | 2,1 |
| 5 | 6873 ms | 9362 ms | 699 ms | 0 % | 2,4 |
| 10 | 10789 ms | 17940 ms | 1596 ms | 0 % | 2,5 |

p95 por paso con 1 usuario: login 4943 ms, usuario 2477, Inicio 802, guardar sesión 4277, resumen 420, diario 643, buscar 659. Con 10: login 23483 ms.

**0,25 de CPU (referencia: lo que da una instancia algo mayor)**

| Usuarios | p95 | p99 | Mediana | Errores | Peticiones/s |
|---|---|---|---|---|---|
| 1 | 301 ms | 977 ms | 24 ms | 0 % | 1,4 |
| 2 | 172 ms | 480 ms | 14 ms | 0 % | 3,3 |
| 5 | 186 ms | 501 ms | 13 ms | 0 % | 7,8 |
| 10 | 228 ms | 746 ms | 14 ms | 0 % | 14,4 |
| 20 | 1460 ms | 2354 ms | 197 ms | 0 % | 19,8 |
| 40 | 4202 ms | 6447 ms | 700 ms | 0 % | 20,5 |

### La cifra

- **En el plan actual (0,1 de CPU), ningún nivel cumple**: con **un solo usuario** el p95 ya es 1,66 s. Sin errores hasta 10 usuarios, pero lento desde el primero. El paso más caro es el **login** (p95 4,9 s con uno, 23 s con diez): el hash de la contraseña es CPU pura, y con 0,1 de núcleo cada uno tarda segundos. Guardar una sesión le sigue (4,3 s).
- **Con 0,25 de CPU: 10 usuarios simultáneos** (p95 228 ms, 0 % de errores). Se rompe entre 10 y 20.
- Diez usuarios simultáneos con estas pausas son unos 14 peticiones/s: muchos más usuarios activos al día, porque casi nadie está a la vez.

### Dónde se rompe, con los datos

- **CPU, siempre primero.** A 0,1 el contenedor va al 100 % de su cupo (9-10 % en `docker stats`) desde 1-2 usuarios, y las peticiones por segundo se quedan en **2,5** aunque se doblen los usuarios (5 → 10). A 0,25, igual a partir de 20: CPU al 25 % (el cupo entero) y el caudal clavado en **~20 peticiones/s** con 20 y con 40 usuarios.
- **Pool, lo siguiente.** Sin errores en las series buenas, y sin «Connection is not available» en sus logs. En la serie degradada (CPU aún más escasa), entre los niveles de 20 y 40 usuarios salieron **23 esperas agotadas** del pool (20 s; `total=5, active=5, idle=0, waiting=14-15`): con la CPU ahogada, cada petición retiene su conexión más tiempo y las cinco se agotan. De ahí el 2,23 % de errores con 20 y el 0,31 % con 40 de esa serie.
- **Hilos.** A 0,25 con 20 y 40 usuarios el contenedor pasa de 32 a 43 hilos: Tomcat en sus 20. Llegan al tope a la vez que la CPU, no antes.
- **Memoria, no.** 370-433 MB de 512 en todas las series, sin OOM ni aviso del vigilante (`Memoria: contenedor 376 de 512 MB (73 %)…` con 40 usuarios a 0,25). El heap usado no pasó de 111 de 154 MB.

### Hallazgos que no se arreglan aquí

- **Un pool agotado en el login se responde como credenciales malas (401).** `Could not open JPA EntityManager for transaction` llega a Spring Security envuelto en una `AuthenticationException` y `ControllerExceptionHandler.handleAuthenticationException` lo traduce en «usuario o contraseña incorrectos». El usuario ve un error que no es, y como no es un 500 no llega a Sentry. Visto en la serie degradada: 14 logins acabaron así, 10 de los 12 fallos con 20 usuarios.
- **El login a 0,1 de CPU** es lo que más pesa del recorrido. Cualquier cambio (coste del hash, cuándo pide la app volver a entrar) se mide antes y después (DEC-021); aquí no se toca.

## Tests

1334 en verde en local (perfil dev), los 15 nuevos incluidos. Vistos en rojo:

- **`TareasUnaVezConDosInstanciasTest`** (12 casos, uno por método `@Scheduled` de las tres tareas, sacados por reflexión): dos contextos completos de la API contra la misma base, el planificador real cambiado por uno que no programa nada, y cada método disparado a la vez en los dos. Sin `@SchedulerLock`, **12 de 12 en rojo** («expected: 1 but was: 2»).
- **`ShedLockNoConfirmaTransaccionTest`**: sin el gestor de transacciones de JPA, rojo («expected: 0 but was: 1»: el usuario creado dentro de la transacción del test era visible desde otra conexión).
- **`SentryConDsnTest`** (2): sin `FiltroDatosSentry`, rojo (el correo llegaba en el mensaje de la excepción); sin `Sentry.captureException` en el manejador, rojo («el 500 tenía que llegar a Sentry»). El 4xx no llega.
- **`SentrySinDsnTest`**: con `SENTRY_DSN=` vacía en el entorno, rojo con la primera versión (la propiedad existía); se dejó el test para los dos casos, y pasa con la variable sin definir y vacía.

## Comprobación en producción

Pendiente de la fusión: `/actuator/info` en el hash de la fusión, Flyway con `V202611121000`, la tabla `shedlock` con sus filas tras el primer ciclo y ningún aviso de Sentry en el arranque. Se completa abajo cuando esté.

## Tareas del propietario

- **Crear el proyecto en Sentry y poner `SENTRY_DSN` en Render** (Settings > Client Keys). Sin ella todo sigue igual.
- **Elegir el workspace de Render** para el MCP si se quiere leer CPU y memoria de producción desde aquí: no lo elegí yo.
