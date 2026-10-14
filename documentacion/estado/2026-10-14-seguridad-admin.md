# Seguridad — lo que una cuenta ADMIN veía de las demás (S24, S25, GP-197 y S26)

Ejecutado el 2026-10-14, en cuatro entregas, cada una desplegada y comprobada en producción
antes de la siguiente. Este informe se publica con los cuatro arreglos ya en producción y
confirmados por el propietario.

| Entrega | Commit | Fusión | Qué |
|---|---|---|---|
| S24 · GP-195 | `7e9c22e` | `ceadf31` | La búsqueda de alimentos del panel, solo del catálogo |
| S25 | `adface8` | `fdec8f7` | Los alimentos, los platos y el diario de un usuario, solo suyos también para un ADMIN; DEC-027 corregida |
| GP-197 | `8ca9908` | `2201f7a` | Las comidas, igual; y lo que se crea es siempre de quien llama |
| S26 · GP-198 | `175fae2` | `1d952c9` | Todo lo demás del usuario —entrenamientos, medidas, objetivos, avisos, rutinas, cuenta—, igual; DEC-027 ampliada |

Tests de la API: **1202 en verde** (1189 al empezar). Ninguno simula el servicio: los cuatro
nuevos levantan el contexto con JWT de verdad (`AbstractOwnershipTest`).

## Qué quedaba expuesto

Solo para quien tuviera una cuenta con rol **ADMIN**. Un USER y el invitado ya recibían 403 o
no veían nada de otros, y el rol ADMIN no se obtiene desde el registro (cerrado el 2026-07-02).
Este informe no puede decir si se llegó a usar: no hay un registro de peticiones por ruta y
cuenta que lo permita.

**Nutrición (S24, S25 y GP-197):**

- **Los alimentos y platos que crea cada usuario.** Con su nombre, sus valores, su dueño y
  también los desactivados: en la búsqueda de alimentos del panel, en los listados y los
  contadores de `/alimentos`, por id y en `/alimentos/usuario/{id}`. Un ADMIN también podía
  cambiarlos, desactivarlos, reactivarlos o borrarlos del todo.
- **El diario.** Las líneas de cada comida —qué alimento, cuánto, kcal y macros— de todas las
  cuentas, en el listado de `/alimentos-comida`, por alimento y en sus contadores, y por id;
  también cambiarlas o borrarlas. Y meter en una comida propia el alimento de otro, con lo que
  la respuesta traía sus datos.
- **Las comidas.** Fecha, tipo, notas y totales de las comidas de todos, en el listado de
  `/comidas`, por tipo, por fecha y en el contador por tipo, y por id o por usuario; también
  cambiarlas o borrarlas.

**El resto de los datos del usuario (S26):**

- **Entrenamientos.** Las sesiones de todos —fechas, duración, valoración, notas— y los
  ejercicios hechos en ellas —series, repeticiones, pesos, tiempos—, en sus listados
  (`/sesiones`, completadas, pendientes, por fecha, por rutina, `/ejercicios-realizados`, por
  ejercicio) y contadores, y por id o por usuario, con su volumen y sus estadísticas; también
  cambiarlos o borrarlos.
- **Medidas corporales y objetivos.** Peso, grasa, perímetros y objetivos personales de todos,
  en sus listados y por id o por usuario; también cambiarlos o borrarlos.
- **Avisos.** Las notificaciones de cada cuenta, en su listado y por id o por usuario; también
  marcarlas o borrarlas.
- **Rutinas propias.** Las rutinas que crea cada usuario y sus ejercicios, en los listados y por
  id o por usuario; también cambiarlas, desactivarlas o borrarlas.
- **Logros y récords** de cada cuenta, por usuario.
- **La cuenta.** Por id, por nombre o por correo: los datos del perfil, con peso, altura y edad,
  la foto y las estadísticas; y en los listados de `/usuarios` y `/jooq/usuarios`, todas las
  cuentas. También cambiar el perfil y desactivar, reactivar o borrar una cuenta fuera de
  `/admin`.

**Escribir en otra cuenta.** Un ADMIN podía crear alimentos, comidas, sesiones, mediciones,
objetivos y rutinas a nombre de otro usuario.

Los platos y su lista de ingredientes existen desde la 1.6.6 (`4140c5b`), del mismo día.

## Desde cuándo

- La **excepción de ADMIN** en la comprobación de dueño, que cubría casi todo lo anterior —el
  diario, las comidas, los entrenamientos, las medidas, los objetivos, los avisos, las rutinas
  propias, los logros, los récords, la cuenta por id y la creación a nombre de otro—, nace con esa comprobación, el
  **2026-07-02** (`8ba39a7`). Antes de esa fecha no había comprobación de dueño para nadie; eso
  ya está contado en la auditoría de julio.
- Los **listados de cuentas** fuera de `/admin` (`/usuarios`, `/jooq/usuarios`) eran solo de
  ADMIN y devolvían todas las cuentas desde antes de julio.
- La **búsqueda de alimentos del panel** devolvía toda la tabla desde que se escribió, el
  **2026-05-26** (`e8d8524`).
- En los **listados y contadores de alimentos, de ejercicios de rutinas y de ejercicios
  realizados por ejercicio**, el ADMIN conservó la vista completa cuando se filtraron para el resto (el
  **2026-09-19**, `66bf7b1`, y el **2026-09-24**, `7e93af5`, GP-048; antes los veía cualquiera, y
  está contado en sus informes).

## Cómo se arregló

La regla es la de DEC-042 aplicada sin excepción de rol a todo lo que guarda un usuario: es solo
suyo, también para un ADMIN. Un id de otro da el mismo 403 que a un USER (DEC-014). Las rutas que
siguen siendo de ADMIN fuera de `/admin` devuelven lo de quien llama. Lo que se crea es siempre
de quien llama (DEC-013).

- **S24 (`7e9c22e`).** La búsqueda del panel se limita al catálogo, activo o no, igual que el
  listado del catálogo de la web de administración. Se revisaron las demás consultas de
  `/admin`: ninguna tenía el mismo problema.
- **S25 (`adface8`).** `SecurityUtils.checkDueno` y `checkDuenoSiLoTiene`, sin excepción de rol,
  sustituyen a la comprobación anterior en alimentos, platos, líneas del diario, añadir a una
  comida y avisos de alimentos. Los listados y contadores de alimentos dan al ADMIN el catálogo y
  lo suyo.
- **GP-197 (`8ca9908`).** Lo mismo en `/comidas`. El `usuarioId` del cuerpo al crear **se ignora
  en vez de rechazarse**, porque la app lo manda siempre, también en una cuenta ADMIN, y un 400
  rompería las builds repartidas fuera de Play. Un ADMIN que no lo manda sigue creando catálogo.
- **S26 (`175fae2`).** Lo mismo en sesiones, ejercicios realizados, mediciones, objetivos,
  notificaciones, rutinas propias y sus ejercicios, logros, récords, el token del móvil y la
  cuenta. La comprobación anterior, `checkOwnership`, que dejaba pasar a un ADMIN, desaparece:
  queda una sola, `checkDueno`. Crear una rutina a nombre de otro ya daba 403 a un USER, y ahora
  también a un ADMIN.

### Lo que sigue siendo de administración, y por qué

- **El catálogo** (alimentos y ejercicios sin dueño): es de todos y lo mantiene el ADMIN.
- **Las rutinas sin dueño** (predefinidas y plantillas de programa): son catálogo; el ADMIN las
  ve, las cambia y las activa, y la pantalla de administración de la app las usa.
- **La gestión de cuentas de `/admin`**: listar, activar, cambiar el rol y borrar; enseña datos de
  la cuenta, no de salud, y la usan el panel y la app.
- **Crear un usuario** (`POST /usuarios`): da de alta una cuenta nueva, no lee la de nadie.
- **Saber si un nombre o un correo existen** (`/usuarios/exists/…`): no devuelve datos, y el
  registro ya lo dice (DEC-028).
- **Enviar una notificación a otra cuenta**: es un aviso del administrador, y la respuesta solo
  trae lo que él ha escrito. Los avisos del sistema no pasan por un token.

Ni la app ni el panel dependían de nada de lo que se cierra: la app usa las rutas de su propio
usuario, y el panel y la pantalla de administración trabajan sobre `/admin`, el catálogo y las
rutinas sin dueño.

**Un cambio visible:** un id de usuario que no es el propio da 403 exista o no, también a un
ADMIN, porque la propiedad va antes que la existencia. El 404 de «usuario inexistente» en esas
rutas ya no lo ve nadie.

### Tests, vistos en rojo sin el arreglo

| Test | En rojo antes del arreglo |
|---|---|
| `BusquedaAdminCatalogoTest` | `solo_catalogo` |
| `NutricionSoloDelDuenoTest` | `listados_admin`, `ids_admin`, `diario_admin` (`el_dueno_si`, en verde antes y después) |
| `ComidasSoloDelDuenoTest` | `listados_admin`, `ids_admin`, `crear_es_de_quien_llama` (en rojo por la comida y, con solo esa parte arreglada, por el alimento) |
| `DatosSoloDelDuenoTest` | `listados_admin`, `ids_admin`, `crear_es_de_quien_llama` (`administracion_se_conserva` y `el_dueno_si`, en verde antes y después) |

Se ajustaron los tests que daban por hecha la vista del ADMIN: los unitarios de alimentos,
líneas, comidas, sesiones, mediciones y objetivos, y `SesionOwnershipTest`,
`UsuarioOwnershipTest` y `ListaVaciaContratoTest`, que ahora fijan el 403 del ADMIN con un id
de otro.

### Comprobado en producción

Con una cuenta de prueba por entrega (`prueba-195-a`, `prueba-197-a` y `prueba-198-a`), con
datos en cada cosa que se cerraba, y la cuenta ADMIN de pruebas:

- Como ADMIN, nada de la cuenta de prueba en ningún listado ni contador: la búsqueda del panel,
  `/alimentos`, `/alimentos-comida`, `/comidas`, `/sesiones` y sus filtros, `/ejercicios-realizados`,
  `/mediciones-corporales`, `/objetivos-personales`, `/notificaciones`, `/rutinas`,
  `/rutinas-ejercicios`, `/usuarios` y `/jooq/usuarios`. El catálogo sigue saliendo (457
  alimentos, «arroz» da 9, 31 rutinas sin dueño).
- Como ADMIN, todo id de la cuenta de prueba da 403, también al escribir (`PATCH`, `PUT`).
- Lo que el ADMIN crea con el `usuarioId` de la cuenta de prueba queda a su nombre (se borró al
  momento); una rutina para otro, 403.
- Se conserva la administración: el aviso del ADMIN le llega a la cuenta de prueba, las rutinas
  sin dueño se leen con sus ejercicios, y `/admin/cuentas` y `/admin/resumen` responden.
- La cuenta de prueba sigue viendo lo suyo.
- Las tres cuentas de prueba se borraron (`DELETE /usuarios/me`; después, login 401).

## DEC-027, corregida

DEC-027 decía que la vista global de ADMIN se conservaba donde ya estaba declarada, y DEC-042
que el administrador no ve los alimentos de nadie. Mandaba DEC-042. Desde `adface8`, DEC-027
dice que un ADMIN no tiene una vista global de la nutrición de nadie, con una nota de qué
decía antes; desde `175fae2`, de ningún dato del usuario, y enumera lo que sigue siendo de
administración.
