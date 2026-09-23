# El guardado de la sesión — GP-070, GP-006 y la corrección de DEC-033

Dos tareas que tocan el mismo sitio —lo que pasa al pulsar «Guardar» en una sesión
de entrenamiento— más una corrección de una decisión escrita hoy mismo.

Ejecutado el 2026-09-23. Emulador `Medium_Phone_API_36.1` contra la API local
(`10.0.2.2:8080`). Capturas en
[`guardado-sesion-2026-09-23/`](guardado-sesion-2026-09-23/).

| Commit | Qué |
|---|---|
| `c0c0238` | GP-070 · la valoración pasa a ser un campo, y se recupera la ya guardada |
| `7442a53` | GP-006 · la sesión se guarda entera o no se guarda, y no se duplica al reintentar |
| `f38800e` | GP-006 + GP-070 en Android · la pantalla deja de mentir |
| `721d256` | DEC-033 · la decisión dice lo que el código hace con 403 y 404 |

---

## GP-070 · La valoración era una línea de texto dentro de las notas del usuario

`RegistrarSesionActivity` formateaba las estrellas con un recurso de idioma y las
metía como **primera línea de `notas`**, delante de lo que hubiera escrito el
usuario: `valoracion + "\n" + notas`.

Tres problemas, y ninguno es cosmético:

- **No se puede consultar.** «Media de valoración» o «sesiones de 5 estrellas»
  exigen parsear texto libre.
- **Se congela en el idioma del momento.** Una sesión guardada en español dice
  `Valoración: 3/5` para siempre aunque la app se ponga en inglés.
- **La app escribe dentro del texto del usuario.** Sus notas no son suyas.

### La columna

`V202609231430__Valoracion_sesion.sql`. `INT NULL` con
`CHECK (valoracion IS NULL OR valoracion BETWEEN 1 AND 5)`.

**`INT` y no `TINYINT` a propósito**, y no por gusto: con `TINYINT` el arranque
falla en la validación de esquema —«*wrong column type encountered in column
[valoracion] … found [tinyint], but expecting [integer]*»— porque la entidad la
declara `Integer`. Está anotado en la propia migración para que no se «optimice»
luego.

### Qué formatos reconoce la recuperación, y por qué esos

Salen del **historial de git de `sesiones_valoracion_fmt`** en `values/` y
`values-en/`, no de suponer. Han existido exactamente tres literales:

| Literal | Dónde | Desde |
|---|---|---|
| `⭐ %d/5` | ES y EN | `6ca53c3` (2026-05-22) hasta `5e0dee3` (2026-09-23) |
| `Valoración: %d/5` | ES | `40c686b` (2026-09-23) |
| `Rating: %d/5` | EN | `40c686b` (2026-09-23) |

El emoji salió de las cadenas en `40c686b` (GP-061) y el texto se hizo explícito
porque esto **se guarda**, no se lee en pantalla.

### Por qué la coincidencia es tan estricta

El patrón ocupaba **siempre la primera línea entera**, así que eso es justo lo que
se exige: que la nota empiece por el literal exacto y que lo siguiente sea el final
de la nota o un salto de línea. Un «me salió un 4/5» escrito por el usuario en
mitad de una frase **no se toca**.

Y se compara con `utf8mb4_bin` a propósito: la colación por defecto de la base
ignora mayúsculas **y acentos**, así que sin eso un «valoracion: 3/5» escrito a
mano por el usuario casaría y le borraríamos su línea.

### Los números, medidos contra la base local antes de dejarla lista

```
8 sesiones en total, las 8 con notas.
3 casan con «⭐ N/5», 2 con «Valoración: N/5», 0 con «Rating: N/5».
5 sesiones reciben valoración.
5 notas se modifican, y las 5 quedan vacías (la valoración era su única línea)
  → pasan a NULL.
3 notas de siembra («Sesion de siembra») no se tocan.
0 notas con texto del usuario detrás de la valoración.
```

Como esa última cifra es **cero**, la rama que conserva el texto del usuario se
quedaría sin comprobar. Se ensayó a mano sobre 14 casos escritos para eso
(`[salto]` es un salto de línea real):

| Nota | Resultado |
|---|---|
| `⭐ 3/5` | 3, notas `NULL` |
| `⭐ 4/5 [salto] Buenas sensaciones` | 4, notas «Buenas sensaciones» |
| `Valoración: 5/5 [salto] linea1 [salto] linea2` | 5, notas con las **dos** líneas |
| `Rating: 1/5 [salto] felt weak today` | 1, notas «felt weak today» |
| `me salió un 4/5, muy bien` | no casa |
| `Valoracion: 3/5` (sin tilde) | no casa |
| `valoración: 3/5` (minúscula) | no casa |
| `⭐ 0/5` y `⭐ 6/5` | no casan |
| `⭐ 3/5 y encima lloviendo` | no casa (no ocupa la línea entera) |
| `Hoy flojo [salto] Valoración: 2/5` | no casa (no es la **primera** línea) |

Una valoración de **0 estrellas no se reconoce**: la columna admite 1..5 y un
`⭐ 0/5` no es una valoración válida, así que esa línea se queda donde está en vez
de inventar un valor. En la base local no había ninguna.

### Lo demás del commit

- `valoracion` en la entidad y en los tres DTO (`@Min(1) @Max(5)` en los de
  escritura).
- **`@Valid` en `patchSesion`**, que solo tenía `@RequestBody`: las restricciones
  del DTO de patch no se evaluaban, así que un `PATCH` con `valoracion: 9` entraba
  tan campante. Ahora es `400`.
- `ValoracionSesionTest`, 6 pruebas: POST con 4 deja las notas intactas · POST sin
  valoración es válido · POST con 0 y con 6 → `400` · PATCH cambia la valoración sin
  tocar las notas · PATCH con 9 → `400`.

---

## GP-006 · La sesión se guardaba a trozos y sin mirar el resultado

La pantalla creaba la sesión, lanzaba **un POST por ejercicio** con el callback
vacío, y se cerraba sin esperar a ninguno. Si fallaba uno de los de en medio,
quedaba una sesión a medias en la base y el usuario ya había visto «Sesión
registrada correctamente».

### Endpoint nuevo, no campo opcional — y por qué

El encargo pedía un contrato **aditivo**, y las dos formas lo son. Se elige
`POST /sesiones/completa` porque la otra no sostiene lo importante: si los
ejercicios viajaran como campo anidado opcional de `POST /sesiones`, la **clave de
idempotencia tendría que ser opcional también** —las builds repartidas no la
mandan—, y una clave que se puede omitir no protege de nada: el camino que duplica
sesiones seguiría abierto y sería justo el que usan los clientes viejos.

Con ruta nueva, la clave es **obligatoria** ahí y `POST /sesiones` se queda exactamente
como estaba para quien ya lo usa.

### La idempotencia

`V202609231500__Sesion_idempotencia.sql`: columna `idempotencia_clave VARCHAR(64)
NULL` y **`UNIQUE (usuario_id, idempotencia_clave)`**.

- **Columna y no tabla aparte**: la clave pertenece a la sesión que creó y muere con
  ella; una tabla de claves exigiría limpiarla y podría quedar desincronizada.
- **El par con `usuario_id` y no la clave sola**: un UUID de otro usuario no debe
  poder bloquear el guardado de nadie, y `NULL` no cuenta en un índice único, así
  que las sesiones del camino viejo conviven sin estorbar.

El servicio hace `flush()` **dentro** del `try` para que el choque salga como
`DataIntegrityViolationException` donde se puede atender, y ahí relee por clave:
es la carrera de dos peticiones simultáneas con la misma clave, donde la
comprobación previa no basta.

### Los tests

`GuardadoSesionCompletaTest`, 6 pruebas. **Deliberadamente NO `@Transactional`**:
un test transaccional revierte al terminar y por tanto *no puede demostrar* que el
servicio revierte. La limpieza se hace a mano en `@BeforeEach`/`@AfterEach`.

- Guarda sesión, ejercicios y series de una vez.
- Un ejercicio inválido **en mitad de la lista** no deja nada: ni la sesión, ni los
  ejercicios anteriores.
- Dos peticiones con la misma clave crean **una** sesión y la segunda devuelve la
  primera.
- Claves distintas crean dos.
- Sin clave → `400`.
- Una sesión sin ejercicios es válida (entrenamiento libre).

---

## La pantalla, en Android

- Todo va en `POST /sesiones/completa`.
- **No se cierra hasta el éxito.** Si falla: «No se ha guardado», con
  «Reintentar» / «Ahora no», y **lo tecleado sigue donde estaba**.
- El reintento **reutiliza la misma clave**: se genera al pulsar Guardar y solo se
  limpia al guardar bien. Si la petición anterior sí llegó y lo que se perdió fue la
  respuesta, el servidor devuelve la sesión que ya creó.
- La valoración viaja como campo y **deja de escribirse en las notas**.
- Fuera los dos `onFail` vacíos. El de los ejercicios de la rutina ahora avisa:
  callarlo dejaba al usuario creyendo que esa rutina no tiene ejercicios.

Retoque mínimo de interfaz a propósito: **GP-012 rehará esta pantalla como sesión
en vivo**.

### Dos cosas que aparecieron al hacerlo

**Un crash que solo se veía en ejecución.** Hasta GP-070 `notas` nunca llegaba
vacía, porque la app metía dentro la valoración. `ResumenSesionActivity` hacía
`getNotas().isEmpty()` sin más, así que la primera sesión guardada **sin notas**
reventaba la pantalla entera al volver de guardar. Ya es null-safe; la fecha
también. Compilaba perfectamente.

**La valoración se habría quedado invisible.** Se veía porque iba dentro de las
notas. Se añade su propia fila al resumen, oculta cuando no se valoró.

Y dos apoyos: `UiFeedback.mensaje(context, code)` —el mismo mapeo sirve para el
toast de siempre y para el diálogo, y así no se desincronizan— y una sobrecarga de
`UIHelper` con el texto de los botones, porque «Confirmar / Cancelar» no dice nada
en un aviso de error.

### Verificación en el emulador

Cuenta `vacia` (una sola sesión previa), rutina Full Body, 55 min, valoración 3.

| Paso | Captura |
|---|---|
| Formulario relleno | [`gp006-antes.png`](guardado-sesion-2026-09-23/gp006-antes.png) |
| Modo avión al guardar | [`gp006-sin-red.png`](guardado-sesion-2026-09-23/gp006-sin-red.png) |
| Tras reintentar con red | [`gp006-reintento.png`](guardado-sesion-2026-09-23/gp006-reintento.png) |

```
12:53:40  --> POST /api/sesiones/completa   (cuerpo de 847 bytes)   ← modo avión, sin respuesta
12:54:14  --> POST /api/sesiones/completa   (cuerpo de 847 bytes)   ← MISMO cuerpo = misma clave
12:54:14  <-- 200 /api/sesiones/completa

id 667 | 55 min | valoracion 3 | clave 8f02abfa-… | 3 ejercicios | 10 series
sesiones del usuario 5080: 587 (la de antes) y 667. Ninguna más.
crashes en logcat: 0
```

Con el modo avión no se creó ninguna sesión, los datos siguieron en pantalla, y el
reintento creó **exactamente una**.

---

## DEC-033 · La decisión afirmaba algo que el código no hace

Decía «**403 antes que 404**» a secas. Es cierto en las rutas que cuelgan del
usuario (`/sesiones/usuario/{id}`), donde el dueño del id **es el id** y se compara
contra el token sin tocar la base. No lo es en las que cuelgan de un recurso con
dueño (`/rutinas-ejercicios/rutina/{id}/ordenados`): ahí hay que **cargar el
recurso para saber de quién es**, así que un id inexistente da `404` y uno ajeno da
`403` — que es lo que hace `RutinaEjercicioService.findByRutinaIdOrdenado` y lo que
afirman sus propios tests, uno por código.

Generalizaba un caso al otro y **contradecía los tests que ella misma hizo
escribir**.

**Se corrige la decisión, no el código.** La fuga que queda —saber si un id
existe— es despreciable con ids **secuenciales y autonuméricos**: quien quiera
contar las rutinas de la base crea una suya y mira el número. Taparla costaría
devolver `404` al recurso ajeno, o sea mentirle a quien simplemente se equivoca de
id, que es el caso frecuente.

El informe de contratos repetía la misma frase y se corrige igual.

---

## Estado final

```
sh ./mvnw -B verify      → BUILD SUCCESS, 363 tests
sh ./gradlew assembleDebug → BUILD SUCCESSFUL
sh ./gradlew test          → BUILD SUCCESSFUL
```

Las columnas viejas **no se tocan**: `sesiones_entrenamiento.notas` sigue ahí con
el texto del usuario, y la valoración recuperada está en su columna nueva. No hay
`DROP` en ninguna de las dos migraciones.

### Lo que queda sin confirmar: el despliegue

`https://api.gymprofit.app/api/actuator/health` responde `UP` después del push, y el
catálogo de ejercicios sirve `200`. **Eso no prueba que corra la versión nueva**: si
un despliegue falla, la instancia vieja sigue sirviendo y la salud sigue en verde.

Desde fuera no se distingue: `actuator/info` da `401` —solo `health` es público—,
`v3/api-docs` da `500`, y el token de `POST /auth/guest` recibe **`403` igual** en
`/sesiones/completa` que en una ruta inventada, así que no discrimina. `OPTIONS`
tampoco.

La comprobación directa —registrar una cuenta de usar y tirar, pedir token y hacer
`POST /api/sesiones/completa` con `{}`, donde `400` significa desplegado y `404` que
no— **escribe en producción** y se dejó sin hacer a propósito. Queda **pendiente**:
panel de Render, o esa prueba con permiso explícito. Y no es un despliegue
cualquiera: lleva **dos migraciones** y los dos cambios de contrato del bloque
anterior.
