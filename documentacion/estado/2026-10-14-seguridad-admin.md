# Seguridad — lo que una cuenta ADMIN veía de las demás (S24, S25 y GP-197)

Ejecutado el 2026-10-14, en tres entregas, cada una desplegada y comprobada en producción
antes de la siguiente. Este informe se publica con los tres arreglos ya en producción y
confirmados por el propietario.

| Entrega | Commit | Fusión | Qué |
|---|---|---|---|
| S24 · GP-195 | `7e9c22e` | `ceadf31` | La búsqueda de alimentos del panel, solo del catálogo |
| S25 | `adface8` | `fdec8f7` | Los alimentos, los platos y el diario de un usuario, solo suyos también para un ADMIN; DEC-027 corregida |
| GP-197 | `8ca9908` | `2201f7a` | Las comidas, igual; y lo que se crea es siempre de quien llama |

Tests de la API: **1197 en verde** (1189 al empezar). Ninguno simula el servicio: los tres
nuevos levantan el contexto con JWT de verdad (`AbstractOwnershipTest`).

## Qué quedaba expuesto

Solo para quien tuviera una cuenta con rol **ADMIN**. Un USER y el invitado ya recibían 403 o
no veían nada de otros, y el rol ADMIN no se obtiene desde el registro (cerrado el 2026-07-02).
Este informe no puede decir si se llegó a usar: no hay un registro de peticiones por ruta y
cuenta que lo permita.

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
- **Escribir en otra cuenta.** Un ADMIN podía crear alimentos y comidas a nombre de otro
  usuario.

Los platos y su lista de ingredientes existen desde la 1.6.6 (`4140c5b`), del mismo día.

## Desde cuándo

- La **excepción de ADMIN** en la comprobación de dueño, que cubría las líneas del diario, las
  comidas, los alimentos por id y la creación a nombre de otro, nace con esa comprobación, el
  **2026-07-02** (`8ba39a7`). Antes de esa fecha no había comprobación de dueño para nadie; eso
  ya está contado en la auditoría de julio.
- La **búsqueda de alimentos del panel** devolvía toda la tabla desde que se escribió, el
  **2026-05-26** (`e8d8524`).
- En los **listados y contadores de alimentos**, el ADMIN conservó la vista completa cuando se
  filtraron para el resto, el **2026-09-24** (`7e93af5`, GP-048; antes los veía cualquiera, y
  está contado en su informe).

## Cómo se arregló

La regla es la de DEC-042 aplicada sin excepción de rol: lo que un usuario guarda de su
nutrición es solo suyo, también para un ADMIN. Un id de otro da el mismo 403 que a un USER
(DEC-014). El catálogo lo ve y lo mantiene el ADMIN como siempre.

- **S24 (`7e9c22e`).** La búsqueda del panel se limita al catálogo, activo o no, igual que el
  listado del catálogo de la web de administración. Se revisaron las demás consultas de
  `/admin`: ninguna tenía el mismo problema.
- **S25 (`adface8`).** `SecurityUtils.checkDueno` y `checkDuenoSiLoTiene`, sin excepción de rol,
  sustituyen a la comprobación anterior en alimentos, platos, líneas del diario, añadir a una
  comida y avisos. Los listados y contadores de alimentos dan al ADMIN el catálogo y lo suyo.
  Las rutas de `/alimentos-comida` que siguen siendo de ADMIN devuelven lo de quien llama.
- **GP-197 (`8ca9908`).** Lo mismo en `/comidas`. Lo que se crea, comidas y alimentos, es siempre
  de quien llama (DEC-013): el `usuarioId` del cuerpo **se ignora en vez de rechazarse**, porque
  la app lo manda siempre, también en una cuenta ADMIN, y un 400 rompería las builds repartidas
  fuera de Play. Un ADMIN que no lo manda sigue creando catálogo.

Ni la app ni el panel dependían de esas rutas como ADMIN: la app usa las de su propio usuario y
el panel trabaja sobre el catálogo y no lee comidas.

### Tests, vistos en rojo sin el arreglo

| Test | En rojo antes del arreglo |
|---|---|
| `BusquedaAdminCatalogoTest` | `solo_catalogo` |
| `NutricionSoloDelDuenoTest` | `listados_admin`, `ids_admin`, `diario_admin` (`el_dueno_si`, en verde antes y después) |
| `ComidasSoloDelDuenoTest` | `listados_admin`, `ids_admin`, `crear_es_de_quien_llama` (en rojo por la comida y, con solo esa parte arreglada, por el alimento) |

Se ajustaron los tests unitarios que daban por hecha la vista del ADMIN
(`AlimentoServiceTest`, `AlimentoComidaServiceTest`, `ComidaServiceTest`).

### Comprobado en producción

Con una cuenta de prueba por entrega (`prueba-195-a` y `prueba-197-a`), cada una con un
alimento, un plato y una comida con una línea, y la cuenta ADMIN de pruebas:

- Como ADMIN, nada de la cuenta de prueba en la búsqueda del panel, en los listados de
  alimentos, de `/alimentos-comida` y de `/comidas`, ni en sus contadores; el catálogo sigue
  saliendo (457 alimentos, «arroz» da 9).
- Como ADMIN, todo id de la cuenta de prueba —alimento, plato, línea, comida, usuario— da 403,
  también al escribir (`PATCH`, `PUT`).
- Una comida y un alimento creados por el ADMIN con el `usuarioId` de la cuenta de prueba quedan
  a nombre del ADMIN (se borraron al momento).
- La cuenta de prueba sigue viendo lo suyo.
- Las dos cuentas de prueba se borraron (`DELETE /usuarios/me`; después, login 401).

## DEC-027, corregida

DEC-027 decía que la vista global de ADMIN se conservaba donde ya estaba declarada, y DEC-042
que el administrador no ve los alimentos de nadie. Mandaba DEC-042. Desde `adface8`, DEC-027
dice que un ADMIN no tiene una vista global de la nutrición de nadie, con una nota de qué
decía antes.
