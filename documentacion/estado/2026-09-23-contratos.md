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
