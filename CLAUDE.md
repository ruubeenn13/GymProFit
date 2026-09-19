# CLAUDE.md — reglas de trabajo en GymProFit

Producto comercial real: app Android (Java, vistas XML) + API Spring Boot 3.5 / Java 21 / MariaDB.
Monorepo: `api/gymprofit-api` · `app/GymProFit` · `db` · `documentacion`.

## Antes de tocar código

1. Lee `documentacion/PRODUCT-DECISIONS.md`. Son decisiones vigentes, no sugerencias.
2. Inspecciona lo que ya existe antes de escribir: puede estar resuelto, o resuelto a medias.
3. Comprueba API y BD si la tarea las toca. La documentación puede estar desfasada; **el código manda**.
4. Plan breve, cambio mínimo coherente, y nada de refactors no relacionados.

## Comandos

```bash
# API: build + tests (MariaDB local o perfil ci)
cd api/gymprofit-api && sh ./mvnw -B verify

# Android: build de depuración y tests unitarios
cd app/GymProFit && sh ./gradlew assembleDebug && sh ./gradlew test
```

`sh ./` y no `./`: los wrappers están versionados sin bit de ejecución (modo `100644`), así que en un clon limpio `./mvnw` falla. El CI ya lo invoca así.

jOOQ no se regenera por defecto: las clases versionadas están en `src/generated/jooq`.
Migraciones Flyway en `src/main/resources/db/migration`, nombre `V<AAAAMMDDHHmm>__Descripcion.sql`. Una migración publicada no se edita: se añade otra.

## Prohibiciones duras

- **No reintroducir calorías estimadas de entrenamiento** en ninguna forma (DEC-004). Las de nutrición sí son reales.
- **No derivar la identidad del cliente**: el usuario sale del token, siempre (DEC-013).
- **Ninguna ruta nueva con id de recurso sin su test de acceso ajeno** (DEC-014). Los tests de controlador con el servicio simulado no valen: hay que levantar el contexto (`AbstractOwnershipTest`). Qué tiene que comprobar ese test depende de **quién es el dueño del id** (DEC-027): si es un recurso con dueño —una sesión, una comida, un usuario—, **403**; si es un id del catálogo público —un ejercicio, un alimento—, no hay id ajeno que rechazar y lo que se comprueba es el **aislamiento**: lista vacía o `count: 0`. Los contadores y los `exists` cuentan como datos ajenos y se protegen igual.
- **Ningún secreto en el repositorio, ningún valor por defecto en el perfil `prod`** (DEC-016). Si falta una variable sensible, la app no arranca.
- **Ningún `onFail` vacío.** Cada fallo se muestra, se reintenta, o se ignora con un comentario que explique por qué es seguro.
- **Ninguna cadena visible a fuego**: recursos en ES y EN, plurales donde se cuenta (DEC-020).
- **Ninguna vista dibujada a mano sin accesibilidad** (DEC-019).
- No migrar a Compose en bloque (DEC-010). No introducir microservicios (DEC-011). No añadir dependencias ni abstracciones sin necesidad demostrada.
- No optimizar sin medir antes y después (DEC-021).
- No cambiar contratos de API a la ligera. No es porque haya una app publicada: **no la hay**, no se ha subido nada a Play todavía, ni siquiera a la pista interna. Es porque hay builds repartidas fuera de Play que consumen esos contratos y no se actualizan solas, y porque en cuanto se publique la restricción pasa a ser permanente: mejor no coger el hábito antes.

## Terminado

No basta con que compile (DEC-022). Una tarea está hecha cuando: funciona, no rompe lo existente, cubre carga/error/vacío/éxito, contempla los casos límite, **trae tests nuevos si trae código nuevo**, es accesible, está en ES y EN, se ve bien en claro y oscuro, y no deja deuda innecesaria.

Que los tests existentes sigan en verde no significa que lo nuevo esté probado.

## Commits

Convención `tipo(ámbito): resumen` — `feat`, `fix`, `refactor`, `docs`, `test`; ámbitos `api`, `android`, `api,android`.

`test` es para lo que solo añade o corrige pruebas sin tocar el comportamiento. Un cambio de código que trae sus tests, como pide *Terminado*, va con el tipo del cambio (`feat` o `fix`), no con `test`.
El cuerpo explica **por qué**, no qué líneas cambiaron.
Todo cambio funcional se anota en `CHANGELOG.md` con su hash.

## Dónde vive cada cosa

- **Decisiones vigentes** → `documentacion/PRODUCT-DECISIONS.md` (este repositorio).
- **Backlog, roadmap y prioridades** → fuera del repositorio, en el proyecto de planificación. Fuente única; no se duplican aquí.
- **Auditorías con fecha en el nombre** → fotos del pasado. Útiles como contexto, nunca como estado actual.
