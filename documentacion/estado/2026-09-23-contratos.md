# Contratos de API que se congelan al publicar — GP-069 y GP-010

Dos cambios que tocan el contrato de la API. Se hacen **ahora**, antes de publicar,
porque en cuanto la app esté en la tienda dejan de poder hacerse.

Ejecutado el 2026-09-23. Emulador `Medium_Phone_API_36.1` contra la API local
(`10.0.2.2:8080`), con el distintivo `PRUEBAS · LOCAL` visible en las capturas.
Capturas en [`contratos-2026-09-23/`](contratos-2026-09-23/).

---

## GP-069 · La API respondía 404 cuando una lista estaba vacía

Decisión registrada como **DEC-033** en
[`PRODUCT-DECISIONS.md`](../PRODUCT-DECISIONS.md).

### El tamaño real del problema

No era un endpoint. `if (lista.isEmpty()) throw new NotFoundEntityException(...)`
aparecía **58 veces en 13 controladores**:

| Controlador | Sitios | Controlador | Sitios |
|---|---:|---|---:|
| SesionEntrenamiento | 11 | Comida | 5 |
| ObjetivoPersonal | 5 | ProgresoEjercicio | 5 |
| Rutina | 7 | Notificacion | 4 |
| MedicionCorporal | 4 | Alimento | 4 |
| Ejercicio | 4 | EjercicioRealizado | 3 |
| RutinaEjercicio | 3 | AlimentoComida | 2 |
| Usuario | 1 | | |

Era **la** convención de la casa, y la consecuencia es que el cliente no podía
distinguir «no tienes datos» de «ha fallado algo». De ahí salían los apaños de la
app, y de ahí salió GP-060: la pestaña de rutinas pedía las predefinidas **antes**
que las propias precisamente porque las propias «fallaban» cuando no había ninguna.

### La regla aplicada

- **Colección que existe y está vacía → `200` con `[]`.**
- **Recurso padre que no existe → `404`**, comprobado **explícitamente**
  (`existsById` o cargando la entidad), nunca deducido de la lista vacía.

Se aplicó endpoint por endpoint, no en bloque, porque los 58 no son el mismo caso:

| Caso | Sitios | Qué se hizo |
|---|---:|---|
| Sin recurso padre en la ruta (catálogo, listados globales) | 20 | Solo quitar el `throw`; el `404` desaparece también de la documentación Swagger |
| Padre ya comprobado por el servicio (carga la entidad) | 4 | Solo quitar el `throw`; el `404` ya lo daba el servicio |
| Padre **no** comprobado | 34 | Quitar el `throw` **y añadir la comprobación explícita** |

El precedente ya estaba escrito en el propio repositorio:
`LogroService.findByUsuarioId` comprueba propiedad, después existencia, y devuelve
la lista aunque venga vacía. Esto generaliza eso; el helper nuevo de cada servicio
(`exigirUsuarioExistente`, `exigirRutinaExistente`, `exigirEjercicioExistente`,
`exigirAlimentoExistente`) usa los repositorios que el servicio **ya inyectaba**, así
que ningún constructor cambia.

### El orden importa: 403 antes que 404

La comprobación de existencia va **después** de la de propiedad. A quien no es dueño
se le responde `403` sin decirle de paso si el id existe, así que el cambio no abre
un oráculo de enumeración de ids. Comprobado contra la API en marcha:

```
GET /sesiones/usuario/999999        (como USER)   → 403   ← no dice si existe
GET /sesiones/rutina/999999         (como ADMIN)  → 404   ← la rutina no existe
GET /sesiones/rutina/{sinSesiones}  (como ADMIN)  → 200 []
```

Esas dos últimas líneas son el ejemplo del encargo: antes las dos daban `404`.

### El agujero que estaba tapando el 404

`GET /rutinas-ejercicios/rutina/{id}/ordenados` **no hacía ninguna de las dos
comprobaciones** que sí hace su hermano `findByRutinaId`: ni existencia ni acceso.
Mientras la lista vacía respondía `404`, el agujero quedaba medio tapado. Al pasar a
`200` con `[]` habría empezado a devolver **el contenido de la rutina privada de otro
usuario**. Se le han añadido las dos comprobaciones y sus dos tests (403 y 404).

Es un hallazgo de GP-069, no una petición del encargo: si no se hubiera mirado
endpoint por endpoint, este cambio habría introducido una IDOR.

### Los tests

- **`ListaVaciaContratoTest`** (nuevo, 14 tests, contexto completo): seis rutas con
  la cuenta vacía → `200` con `[]`; cinco padres inexistentes → `404`; las dos
  respuestas de `/sesiones/rutina/{id}`; y dos que fijan que el `403` sigue ganando.
- **`EjercicioRealizadoOwnershipTest.getPorEjercicio_noDevuelveRegistrosAjenos`**:
  pasa de afirmar `404` a afirmar `200` con lista vacía. Es lo que DEC-027 siempre
  quiso decir para un id de catálogo —aislamiento, no permisos—; el `404` era un
  efecto colateral del controlador y el test lo estaba dando por bueno.
- **`RutinaEjercicioOwnershipTest`**: dos tests nuevos para `/ordenados`.
- **Diez tests unitarios** de servicio pasan a simular la existencia del padre. Es
  la señal de que la comprobación está donde debe: un mock devuelve `false` en
  `existsById` por defecto, así que si no se simula, el test lo nota.

`sh ./mvnw -B verify` → **345 tests, 0 fallos** (antes 329).

### En el cliente el 404 vuelve a ser un error

El encargo pedía retirar el apaño de `RutinasFragment`. El apaño estaba en **nueve
sitios**, y el más importante era global: `UiFeedback.toastError` hacía
`if (code == 404) return;` **para toda la app**, así que un 404 de verdad —un id
borrado, una ruta mal construida— no se veía nunca.

| Fichero | Antes | Ahora |
|---|---|---|
| `UiFeedback` | `404` en silencio | `404` → `feedback_error_no_encontrado` (ES y EN) |
| `RutinasFragment` | `404` = estado vacío | Se retira; el vacío llega como `200 []` |
| `ComidaActivity` | `404` = comida sin alimentos | Se avisa; `404` = la comida no existe |
| `DetalleRutinaActivity` | `onFail` en silencio | Se avisa |
| `LogrosActivity` | `404` = nada desbloqueado | Se avisa |
| `MedicionesActivity` | comentario desfasado | Comentario corregido |
| `SesionesActivity` | comentario desfasado | Comentario corregido |
| `NutricionFragment` | `if (code != 404)` | Se avisa siempre |
| `DetalleEjercicioActivity` | `onFail` vacío | Traza en el log; la tarjeta sigue oculta a propósito |
| `EliminarCuentaActivity` | rama `404` para compensar el silencio | Rama retirada: ya no hace falta |

Dejar esos apaños habría sido peor que antes: con la lista vacía llegando ya como
`200`, seguir callando el `404` solo escondería errores de verdad.

### Verificación en runtime

Cuenta `vacia` (sin un solo dato), API local con el código nuevo:

| Ruta | Respuesta |
|---|---|
| `/rutinas/usuario/{id}/activas` | `200 []` |
| `/sesiones/usuario/{id}` | `200 []` |
| `/notificaciones/usuario/{id}` | `200 []` |
| `/comidas/usuario/{id}` | `200 []` |
| `/objetivos-personales/usuario/{id}` | `200 []` |
| `/progreso-ejercicios/usuario/{id}` | `200 []` |
| `/rutinas-ejercicios/ejercicio/999999` | `404` |
| `/ejercicios-realizados/ejercicio/999999` | `404` |

En el emulador, con esa misma cuenta: la pestaña de rutinas sigue mostrando el
estado vacío con las sugerencias (`gp069-rutinas-vacia.png`), y el recorrido por
Inicio, Nutrición y Perfil no dispara **ni un toast de error**. `adb logcat` no
registra **ningún 4xx ni 5xx** en todo el recorrido, que antes estaba lleno de 404.

### Lo que queda fuera a propósito

Los endpoints `count/...` y `exists/...` devuelven `0` o `false` para un usuario que
no existe, en vez de `404`. Es el mismo defecto de fondo, pero no estaba en el
inventario de los 58 (no usan el patrón `isEmpty()`) y tocarlos no cambia nada para
la app. Queda anotado.

---

## GP-010 · Las calorías estimadas de entrenamiento salen del producto

Decisión actualizada: **DEC-004** pasa de «deuda conocida» a **aplicada**.

### El origen estaba en la API, no en la app

`api/.../entity/Rutina.java` tenía un `@Formula` de Hibernate con
`SUM(series × repeticiones × calorias_quemadas)`: **literalmente la fórmula que
DEC-004 declara sin fundamento**, calculada en el servidor en cada consulta de
rutina. La ficha original de GP-010 solo cubría Home y el resumen de sesión, así
que ese `@Formula` seguía vivo y habría repuesto la cifra en cuanto alguien
pintara una rutina.

Una rutina es además una **plantilla**: no tiene pesos. El número no era solo
infundado, es que ahí no podía significar nada. «Movilidad Activa · 35 min ·
~2385 kcal», más que la ingesta diaria completa del usuario.

### Qué se ha retirado

**API**

| Sitio | Qué era |
|---|---|
| `Rutina.@Formula` | El cálculo del servidor |
| `RutinaDTO`, `RutinaPatchDTO` | `caloriasAproximadas` |
| `RutinaEjercicioDTO` | `caloriasEjercicio`, enriquecido desde el catálogo |
| `EjercicioDTO`, `EjercicioCreateDTO`, `EjercicioPatchDTO`, `EjercicioJooqDTO` | `caloriasQuemadas` deja de exponerse |
| `GET /jooq/ejercicios/calorias` y el filtro `caloriasMax` | Buscar por un número que ya no se enseña |
| `SesionEntrenamientoDTO`/`Create`/`Patch` y el parámetro de `PUT /sesiones/{id}/completar` | `calorias_quemadas` deja de escribirse |
| `UsuarioEstadisticasDTO` | `totalCaloriasQuemadas` |
| Resumen semanal por notificación | Las kcal del mensaje (ES y EN) |
| `WgerImportService` | La tabla de kcal por grupo muscular que rellenaba el catálogo al importarlo |

Esa última es la que cierra el círculo: el número de la ficha de ejercicio —«5
kcal»— salía de una constante por grupo muscular, **la misma para cualquiera que
hiciera el ejercicio**.

**Android** — las siete pantallas del inventario, más las tres que aparecieron al
seguir el hilo: el resumen de creación de rutina, y los dos formularios de
administración donde un ADMIN podía teclear el valor a mano.

### Qué se ha puesto en su lugar

- **Tarjeta de rutina: nada.** La tarjeta queda en «N ejercicios · X min». Una
  plantilla no tiene pesos y el volumen ahí no significa nada; inventar otro
  número para tapar el hueco habría repetido el error con otra cara.
- **Resumen de sesión:** la fila de calorías desaparece. La tarjeta de estadística
  «Calorías quemadas» pasa a **«Ejercicios hechos»**, que la API ya devolvía
  (`totalEjerciciosRealizados`) y que es un recuento, no una estimación.
- **Home: se quita la columna, y hay que decirlo.** No se pudo poner el volumen de
  la semana porque **la app no lo tiene**: agrega la semana en el cliente a partir
  de la lista de sesiones, y ahí no viene el peso movido. Ponerlo pediría un dato
  nuevo de la API. «Esta semana» queda en dos tarjetas, Entrenos y Minutos.

### Las columnas siguen en la base de datos

`ejercicios.calorias_quemadas` y `sesiones_entrenamiento.calorias_quemadas`
**no se borran**: dejan de leerse y de escribirse, y se retiran en una migración
posterior, como planteaba la ficha original. Anotado en el CHANGELOG.

### Las calorías de nutrición: comprobado explícitamente

No se ha tocado nada de nutrición, y hay un test que lo fija. Contra la API en
marcha, con el código nuevo:

```
/rutinas/predefinidas        → 0 apariciones de "calorias"
/ejercicios/1                → 0
/usuarios/{id}/estadisticas  → 0
/alimentos/activos           → {"nombre":"Nutella","calorias":539,...}   ← intacto
```

### Lo que impide que vuelva

`SinCaloriasEntrenamientoTest` (6 tests) afirma **sobre el JSON**, no sobre las
clases Java: lo que DEC-004 prohíbe es enseñar el número, y lo que se enseña es lo
que sale por el cable. Cinco rutas de entrenamiento sin el campo, y una de
nutrición **con** él.

`sh ./mvnw -B verify` → **351 tests, 0 fallos**. `sh ./gradlew assembleDebug` en
verde.

### Verificación en el emulador

| Pantalla | Captura | Qué se ve ahora |
|---|---|---|
| Rutinas, cuenta vacía | `gp010-rutinas-vacia.png` | «2 ejercicios · 60 min». Sin kcal |
| Detalle de rutina | `gp010-detalle-rutina.png` | «Intermedio · 60 min». Sin kcal ni separador suelto |
| Ficha de ejercicio | `gp010-detalle-ejercicio.png` | Músculo · Nivel · Equipamiento |
| Home | `gp010-home.png` | «Esta semana»: Entrenos y Minutos |
| Registrar sesión | `gp010-registrar.png` | Sin la tarjeta de «Calorías estimadas»; la lista de ejercicios/pesos sigue cargando |
| Resumen de sesión | `gp010-resumen-sesion.png` | Sin fila de calorías; «Ejercicios hechos: 3» |

Se empezó por la pestaña de rutinas con la cuenta `vacia`, que es lo primero que
ve un usuario nuevo, y se recorrió el resto del inventario desde ahí.

### Un detalle que salió del cruce de las dos tareas

`GET /usuarios/{id}/foto` responde **404 cuando el usuario no tiene foto**, y eso
es correcto: no es una colección vacía, es un recurso que no existe, así que
DEC-033 no lo cambia. Pero `PerfilFragment.cargarFotoPerfil` tenía un `onFail`
vacío, y con GP-069 quitando el silencio global del 404 convenía dejar por escrito
por qué **ese** sí se calla: la mayoría de las cuentas no tiene foto, el avatar por
defecto es lo que se espera ver, y un toast en cada entrada al perfil sería ruido
por un estado normal.

---

## Lo que apareció al comprobar producción

Render desplegó en verde (`c44ce17`, live a las 14:08). La convención nueva se
ve desde fuera:

```
GET /rutinas/predefinidas     → 200 []      (antes: 404 «No se encontraron…»)
GET /ejercicios/activos       → 0 apariciones de "calorias"
GET /jooq/ejercicios/calorias → la ruta ya no existe
```

Esa primera línea destapó dos cosas:

**1. Producción no tiene rutinas predefinidas sembradas.** La base de datos de
Aiven no trae las seis del catálogo local. No es un fallo de este cambio —era
igual antes, solo que salía como 404—, pero conviene saberlo: **un usuario nuevo
en producción no ve sugerencias**, porque no hay ninguna que ofrecerle.

**2. Un defecto que este cambio sí podía producir.** Con el catálogo vacío,
`RutinasFragment` enseñaba el bloque de estado vacío **con el rótulo «Rutinas
para empezar» y nada debajo**: exactamente el tipo de promesa vacía que GP-060
vino a quitar. Antes no pasaba porque el 404 lo mandaba por la otra rama. Ahora
el bloque solo se enseña si hay algo que sugerir.

**3. Una ruta que no existe responde 500, no 404.** `GET /jooq/ejercicios/calorias`
autenticado devuelve `500` en vez de `404`, y `/api/v3/api-docs` y
`/api/swagger-ui` también dan `500` en producción. Es anterior a este trabajo y
no lo toca —el manejador global convierte en 500 lo que no reconoce—, pero es
del mismo género que GP-069: un código que no dice lo que pasa. Queda anotado.
