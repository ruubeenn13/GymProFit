# Bloque de diseño 2a — GP-077, GP-078, GP-079 y GP-063

Cuatro arreglos salidos de la auditoría de diseño del 21-09, más una
corrección del README de la API. GP-062 no entra en este lote.

Ejecutado el 2026-09-24. Emulador `Medium_Phone_API_36.1` contra la API local
(`10.0.2.2:8080`, MariaDB del 3308). Todo se comprobó **en ejecución**. Lo que
afecta a la base se comprobó **en la base**, y lo que afecta a TalkBack, con
TalkBack encendido y leyendo lo que dice en el log (con sus opciones de
desarrollador en `VERBOSE`).

Láminas en [`bloque-diseno-2a/`](bloque-diseno-2a/). Cada una tiene una fila
por estado («antes», «después») y una columna por combinación: ES/EN ×
claro/oscuro. Las capturas originales, a 1080×2400, se quedaron fuera del
repositorio por tamaño: son más de 190.

| Commit | Qué |
|---|---|
| `4616b2c` | GP-077 · la valoración de la sesión arranca sin valorar |
| `343e1b0` | GP-078 · el medidor de nivel deja de parecer el icono de cobertura |
| `cba7663` | GP-079 · un logro bloqueado parece bloqueado y dice cuánto falta (API + Android) |
| `6ea3b3f` | GP-063 · la paleta pasa AA en los dos temas y el texto deja de llevar alpha |
| `ee4f0ff` | README de la API · el ejemplo de `/sesiones/completa` usa `numero` |

Tests nuevos: 3 + 3 + 2 + 2 + 3 en la app (`ValoracionTest`, `MedidorNivelTest`,
`LogroMedallonTest`, `FechaUtilsTest`, `PaletaContrasteTest`) y 2 + 6 en la API
(`TipoLogroTest`, `LogroProgresoTest`). Suites completas en verde: **372 en la
API** (`mvnw verify`) y **38 en la app**.

---

## GP-077 · La valoración por defecto no es una valoración

![Registrar sesión](bloque-diseno-2a/gp077-registrar-sesion.jpg)

**Qué pasaba.** El `RatingBar` declaraba `android:rating="3"` y la pantalla
mandaba la valoración siempre que estuviera entre 1 y 5. Quien no tocaba las
estrellas guardaba un 3 que no había dado, y desde GP-070 eso es un dato que se
puede consultar.

**Qué se hizo.**

- El `RatingBar` arranca en 0, que significa «sin valorar». La decisión de qué
  se manda vive en `Valoracion.paraEnviar()`: `null` si no hay estrellas, y en
  ese caso **el campo no viaja**.
- El rótulo pasa a «Valoración (opcional)» / «Rating (optional)», el mismo patrón
  que «Notas (opcional)». Debajo, el estado en palabras: «Sin valorar», «4 de 5».
- **Se puede volver a «sin valorar».** Con el dedo, un `RatingBar` no baja a
  cero: el primer toque ya marca una estrella. Por eso, en cuanto hay una,
  aparece «Quitar valoración» / «Clear rating». Va `INVISIBLE` y no `GONE`
  cuando no hay nada que quitar, porque con `GONE` la tarjeta saltaba de alto al
  tocar la primera estrella (se vio en la primera captura y se corrigió).
- El histórico no se toca, como pedía la tarea.

**Comprobado en la base** (`sesiones_entrenamiento` de la cuenta `valora77`):

| Sesión | Qué se hizo en la pantalla | `valoracion` |
|---|---|---|
| 668 | guardar sin tocar las estrellas | `NULL` |
| 669 | tocar la 4.ª estrella | `4` |
| 670 | tocar la 2.ª y pulsar «Quitar valoración» | `NULL` |

**TalkBack, y un hallazgo.** TalkBack trata el `RatingBar` aparte: **añade por su
cuenta «N estrellas de 5» y no lee el `stateDescription`**. Con estrellas eso
basta. Sin ellas diría «0 estrellas de 5», que suena a nota y no a ausencia de
nota, así que en ese caso el «sin valorar» va en la propia descripción. Lo que
dice, leído del log:

| | ES | EN |
|---|---|---|
| Al enfocar, sin tocar | «Valoración de la sesión, opcional, sin valorar» «0 estrellas de 5» | «Session rating, optional, not rated» «0 stars out of 5» |
| Al subir con el teclado | … «4 estrellas de 5» | … «3 stars out of 5» «4 stars out of 5» |
| «Quitar valoración» | «Quitar valoración» «Button» | «Clear rating» «Button» |
| Tras pulsarlo | el foco vuelve a las estrellas: «…opcional, sin valorar» «0 estrellas de 5» | «…optional, not rated» «0 stars out of 5» |

«0 estrellas de 5» sigue sonando detrás de «sin valorar» porque lo pone
TalkBack y no se puede quitar sin sustituir el widget. Con «sin valorar» delante
no se presta a confusión.

---

## GP-078 · El medidor de nivel parecía el icono de cobertura

![Paso 5 del onboarding](bloque-diseno-2a/gp078-onboarding-nivel.jpg)

![El mismo paso en escala de grises](bloque-diseno-2a/gp078-onboarding-nivel-grises.jpg)

**Qué pasaba.** Cuatro barras ascendentes, que es el glifo de señal del móvil, y
lleno contra contorno en el mismo gris: en la segunda lámina se ve que en grises
no se distingue nada.

**Qué se hizo.** Cuatro segmentos iguales en horizontal, 45×10 dp. Los llenos van
en `colorPrimary` y los vacíos en una pista neutra nueva, `gp_meter_track`. Los
colores viven **dentro del vector** y no en un `app:tint` de la tarjeta, que
pintaría los cuatro segmentos iguales. La idea de escala se queda.

**Contraste medido**, lleno contra vacío (WCAG 1.4.11 pide 3:1):

| | Lleno | Vacío | Ratio |
|---|---|---|---|
| Claro | `#B83E00` | `#D6D6D0` | **3,86:1** |
| Oscuro | `#FF6A00` | `#353A43` | **3,98:1** |

El contraste WCAG es de luminancia, así que en escala de grises es el mismo; la
lámina en grises lo confirma a ojo. `gp_meter_track` **no reutiliza
`gp_stroke`** a propósito: el borde tenía que oscurecerse (GP-063), y cuanto más
oscuro, menos se distingue de la marca.

**TalkBack**: «Nivel 2 de 4, Intermedio, Entreno regularmente con cierta
experiencia, selected» / «Level 2 of 4, Intermediate, …». De paso, la tarjeta
elegida **anuncia que está elegida**: antes solo se veía por el borde y el check,
que TalkBack no lee. «selected» sale en inglés en los dos idiomas porque lo pone
TalkBack con el idioma del sistema.

`MedidorNivelTest` fija cuántos segmentos van llenos en cada nivel y el 3:1 en
los dos temas, leyendo los XML.

---

## GP-079 · Un logro bloqueado parece bloqueado y dice cuánto falta

![Cuenta nueva: todo bloqueado](bloque-diseno-2a/gp079-logros-cuenta-nueva.jpg)

![Cuenta con logros conseguidos](bloque-diseno-2a/gp079-logros-con-logro.jpg)

### API — cambio aditivo

**Los umbrales, en un solo sitio.** Estaban en un `switch` de
`LogroService.evaluarLogros` y **repetidos a mano** en
`RecordatorioNotificacionesTask` (el aviso de «logro próximo» tenía un `6` y un
`29` escritos a mano). Ahora cada valor de `TipoLogro` lleva su métrica y su
umbral, y la única comparación es `TipoLogro.alcanzado(valor)`. De ahí leen la
concesión, el progreso y el aviso. `contarMetricas()` es el único método que
sabe contar cada métrica.

**Ruta nueva sin id: `GET /logros/progreso`.** Devuelve el catálogo con el
estado del usuario del token: `conseguido` y `fechaObtenido`, o `metrica`,
`umbral` y `progreso` (recortado al umbral, para que la app no pinte «9 de 7»).
No lleva id, así que no hay id ajeno que pedir: **la clase de IDOR no existe**,
en vez de estar protegida. Un invitado recibe 403: la regla va en
`SecurityConfig` antes del `GET /logros/**` general, que sí le deja ver el
catálogo. Nada cambia en las rutas que ya había.

**El borde, con test** (`LogroProgresoTest`, con el contexto levantado):

| Caso | Constancia (umbral 7) |
|---|---|
| Cuenta nueva | 0 de 7, sin conseguir |
| 6 sesiones | **6 de 7, sin conceder** |
| 7 sesiones | **7 de 7, concedido**, con fecha |
| 8 sesiones | sigue siendo 7 de 7 |
| El atacante, con el dueño a 6 | ve su 0, no el 6 ajeno |
| Token de invitado | 403 |

**Comprobado que los tests muerden**, igual que en los bloques de seguridad:
adelantar la concesión un paso pone rojo «6 → sin conceder», y quitar la regla
de `SecurityConfig` pone rojo el 403 del invitado (salió 200). Todo restaurado
después.

### Android

- **Una sola llamada** en vez de dos (catálogo más obtenidos por id), también en
  el resumen de sesión. `Logro`, `UsuarioLogro` y los dos métodos viejos de
  `LogroApi` desaparecen de la app porque ya no los usa nadie. Las rutas
  **siguen en la API** para las builds repartidas.
- **Iconos**: Material Symbols Rounded, peso 400, importados sin redibujar de
  `@material-symbols/svg-400@0.47.5` (carpeta `rounded`, variantes `-fill`).
  Primera sesión `flag`, Constancia `event_available`, Dedicado
  `local_fire_department`, Centenario `workspace_premium`, Objetivo cumplido
  `target`, Máquina `crown`, tipo desconocido `trophy`. Al convertirlos, el
  `viewBox` `0 -960 960 960` se resuelve con un `<group android:translateY="960">`
  sobre un viewport de 960×960. Se borran los siete `ic_logro*` dibujados a mano.
  Los nuevos se llaman `ic_ms_<símbolo>[_fill]`: dicen de dónde vienen.
- **Medallón de 48 dp.** Bloqueado: fondo `gp_surface_2` y glifo en contorno con
  `gp_text_secondary`. Conseguido: fondo `gp_primary_container` y glifo relleno
  con `gp_primary`, y debajo «Conseguido · 18 sept 2026» / «Earned · Sep 18,
  2026», con la fecha en el idioma de la app.
- **Barra de progreso solo si el logro tiene más de un paso**, con «6 de 7
  sesiones» debajo y plurales en ES y EN (sesiones, ejercicios, objetivos).
  La barra reutiliza `gp_meter_track` como pista. El `LinearProgressIndicator`
  de Material 3 pintaba un punto de parada naranja al final de la pista, que a 0
  parecía progreso; se apagó.
- **Ningún `setAlpha`** en el adapter ni en el layout. La descripción va con
  `gp_text_secondary`.
- **Bloqueado y conseguido se distinguen sin el color**: contorno frente a
  relleno, y barra frente a «Conseguido · fecha».

**Contraste del glifo contra su medallón**, medido y fijado en
`LogroMedallonTest`:

| | Claro | Oscuro |
|---|---|---|
| Bloqueado (`gp_text_secondary` / `gp_surface_2`) | 5,44:1 | 6,10:1 |
| Conseguido (`gp_primary` / `gp_primary_container`) | **4,50:1** | 5,21:1 |

**TalkBack**: cada tarjeta es un nodo. «Dedicado. Completa 30 sesiones de
entrenamiento. Bloqueado, 7 de 30 sesiones» / «Dedicated. … Locked, 7 of 30
sessions». «Constancia. … Conseguido el 24 sept 2026» / «Consistency. … Earned
on Sep 24, 2026».

**El borde, en ejecución.** La cuenta `prueba` iba 6 de 7. Se registró la 7.ª
sesión desde la app: el resumen de sesión la da por desbloqueada y la pantalla de
logros pasa Constancia a conseguida.

![La 7.ª sesión, en el resumen y en logros](bloque-diseno-2a/gp079-borde-7-de-7.jpg)

**Licencia.** La app **no tiene ningún sitio** que liste software de terceros: ni
«Acerca de» ni una pantalla de licencias. La tarea decía «si existe ese sitio», y
no existe. La familia, la versión y la licencia Apache 2.0 quedan anotadas en la
cabecera de cada vector y en este informe. **Queda abierto**: MPAndroidChart,
Retrofit y el resto tampoco se listan en ningún sitio, y Apache 2.0 pide
acompañar la redistribución con su aviso. Antes de publicar hace falta una
pantalla de licencias (la de `play-services-oss-licenses` lo hace sola).

---

## GP-063 · Contraste del tema claro y alpha sobre texto

### La medición, antes de tocar nada

**La paleta coincide con la auditoría al céntimo**: dorado 2,80 y 3,17, carbos
3,39, chip de éxito 3,63, éxito sobre tarjeta 4,18, borde 1,66 en claro y 2,06
en oscuro. Salieron **dos pares más** que la auditoría no midió:

- éxito sobre fondo, 3,69:1;
- proteínas sobre fondo, **4,31:1**.

**Los alpha no contradicen la auditoría: la matizan.** Los «104» de la auditoría
eran todos los `android:alpha` de los layouts, no solo los que van sobre texto.
Hoy hay 96 en total, **34 sobre `TextView`** (uno en `item_logro`, que ya quitó
GP-079) y 62 sobre iconos, separadores y un `LinearLayout` del splash. La
corrección reduce el trabajo, no lo cambia de naturaleza, así que se siguió sin
parar.

**Un matiz que ningún XML dice.** `Widget.GymProFit.Card` lleva 1 dp de
elevación, y Material 3 **tiñe** la tarjeta con un 5 % de la marca. Medido en
píxeles de captura: `#FBF5F2` en claro y `#262122` en oscuro, no `#FFFFFF` ni
`#1B1E24`. Todos los pares se miden también contra esa tarjeta teñida.

### Qué se cambió

| Token | Claro, antes → después | Oscuro | Por qué |
|---|---|---|---|
| `gp_gold` (gráfico) | `#B58A2A` → `#AE8528` | sin cambio | 3:1 sobre fondo, tarjeta y tarjeta teñida |
| `gp_gold_text` (nuevo) | `#8A6920` | `#E8B84B` | el dorado **como letra** necesita 4,5:1 |
| `gp_macro_carbos` | `#C27C12` → `#99620E` | sin cambio | 3,39 → 5,11 sobre tarjeta |
| `gp_macro_proteinas` | `#2F6FD0` → `#2E6CCB` | sin cambio | 4,31 → 4,50 sobre fondo |
| `gp_success` | `#1E8E4A` → `#1A7C41` | sin cambio | chip 3,63 → 4,55 |
| `gp_stroke` | `#C9C9C2` → `#8C8C7D` | `#4A505B` → `#676F7E` | borde a 3:1 |
| `gp_divider` (nuevo) | `#C9C9C2` | `#4A505B` | el separador conserva el valor antiguo |
| `gp_on_primary_secondary` (nuevo) | `#F8ECE6` | `#482000` | sustituye a los alpha sobre la marca |

Todos mantienen el tono y solo bajan (o suben) la luminosidad lo justo.

**El dorado, en dos tokens.** Con uno solo había que elegir entre un icono color
bronce y un texto que no se leía. `gp_gold` se queda para borde e icono de la
tarjeta de récord; `gp_gold_text` para «TU RÉCORD» y «80 kg».

**El borde, y la decisión que se revierte.** El `colors.xml` oscuro dejaba el
borde en 2,06:1 **a propósito**, para que no se leyera como un wireframe y porque
WCAG 1.4.11 solo lo exige para identificar un control. Se cambia porque muchas de
esas tarjetas **son controles** —se pulsan— y su canto es lo único que dice dónde.
Las láminas enseñan el resultado. Si pesa demasiado, es un solo token.

**La excepción documentada: `gp_divider`.** `colorOutlineVariant` pasa a un token
propio que conserva el valor antiguo (1,66:1 en claro). Es el separador
decorativo **de dentro** de una tarjeta (entre filas del perfil, en las
mediciones, en el menú contextual) y no delimita ningún componente. Subirlo a
3:1 habría convertido cada lista en una rejilla. Los bordes de tarjeta que
usaban `colorOutlineVariant` en XML (inicio, onboarding 4 y 5, editores e items
de administración) y en Java (los pasos 4 y 5 del onboarding) pasan a
`colorOutline`.

**Resultado, todos por encima de su mínimo** (el más justo, en cada caso):

| Par | Claro | Oscuro |
|---|---|---|
| Texto secundario (fondo, tarjeta, teñida, superficie 2) | 5,44 | 6,10 |
| Marca como texto | 4,98 | 5,53 |
| Dorado como texto | 4,50 | 8,61 |
| Proteínas / carbos / grasas | 4,50 / 4,52 / 4,77 | ≥ 6,3 |
| Éxito (chip, fondo, tarjeta) | 4,55 | 7,89 |
| Error (chip, fondo, tarjeta) | 4,82 | 5,74 |
| Texto secundario sobre la marca | 4,86 | 4,93 |
| Borde (fondo, tarjeta, teñida) | **3,01** | **3,11** |
| Dorado gráfico | 3,00 | 8,61 |

### Alpha sobre texto

Los 33 `TextView` que quedaban pierden el `android:alpha`. Sobre superficie
pasan a `colorOnSurfaceVariant` (`gp_text_secondary`). Sobre la marca pasan a
`gp_on_primary_secondary`, que es la mezcla ya hecha y pasa 4,5:1. Tampoco queda
texto con alpha metido en el color (`#AARRGGBB`): se buscó.

Quedan 62 `android:alpha` en `res/layout`, **ninguno sobre texto**: 54 iconos, 7
vistas y el `LinearLayout` del splash, que arranca a 0 para animarse.

`PaletaContrasteTest` mide todos los pares en los dos temas desde los XML,
tarjeta teñida incluida, y falla si un `TextView` vuelve a llevar alpha. Se
comprobó metiendo uno a propósito. El propio test cazó el primer valor del borde
oscuro (`#646C7B`, 2,98:1 contra la tarjeta teñida calculada), y por eso quedó
en `#676F7E`.

### Pantallas

Inicio (tarjeta de récord y racha):

![Inicio](bloque-diseno-2a/gp063-inicio.jpg)

Nutrición, comida, confirmación, buscador y cantidad:

![Nutrición](bloque-diseno-2a/gp063-nutricion.jpg)
![Comida](bloque-diseno-2a/gp063-comida.jpg)
![Diálogo](bloque-diseno-2a/gp063-dialogo.jpg)
![Añadir alimento](bloque-diseno-2a/gp063-anadir.jpg)
![Cantidad](bloque-diseno-2a/gp063-gramos.jpg)

Perfil, sesiones, rutinas, ejercicios y ficha:

![Perfil](bloque-diseno-2a/gp063-perfil.jpg)
![Sesiones](bloque-diseno-2a/gp063-sesiones.jpg)
![Rutinas](bloque-diseno-2a/gp063-rutinas.jpg)
![Ejercicios](bloque-diseno-2a/gp063-ejercicios.jpg)
![Ficha de ejercicio](bloque-diseno-2a/gp063-detalle-ejercicio.jpg)

Administración (chip de éxito, alpha sobre la marca, editores):

![Panel](bloque-diseno-2a/gp063-admin.jpg)
![Alimentos](bloque-diseno-2a/gp063-admin-alimentos.jpg)
![Ejercicios](bloque-diseno-2a/gp063-admin-ejercicios.jpg)
![Editar ejercicio](bloque-diseno-2a/gp063-admin-editar-ejercicio.jpg)
![Editar rutina](bloque-diseno-2a/gp063-admin-editar-rutina.jpg)

Sin sesión, y resumen del onboarding (carbohidratos):

![Login](bloque-diseno-2a/gp063-login.jpg)
![Registro](bloque-diseno-2a/gp063-registro.jpg)
![Resumen del onboarding](bloque-diseno-2a/gp063-onboarding-resumen.jpg)

---

## De paso · README de la API

El ejemplo de `POST /sesiones/completa` mandaba cada serie con `"numeroSerie"`,
y el campo es `numero`, con `@NotNull`. **Comprobado contra la API local**: el
ejemplo tal cual da `400` («El número de serie es obligatorio») y con `numero`,
`200`. Se añadió una línea con los límites de cada serie. En el mismo README se
corrigió que `/logros/usuario/{id}` figuraba como `GUEST+`, cuando desde julio es
`USER+`.

---

## Pendiente de decisión

- **DEC-018 frente a GP-079.** DEC-018 dice «dorado para lo conseguido: récords
  y logros». GP-079 pedía expresamente el logro conseguido en
  `gp_primary_container` con el glifo en `gp_primary`, es decir, **naranja de
  marca**, y así se hizo. O se ajusta DEC-018 (dorado solo para récords) o el
  medallón conseguido pasa a dorado. Con dorado haría falta un contenedor dorado
  que dé 3:1 contra `gp_gold`; no se ha medido.
- **Despliegue.** La app de este lote llama a `GET /logros/progreso`. Contra una
  API sin desplegar, la pantalla de logros daría 404 y enseñaría el aviso de
  error con la lista vacía. Hay que desplegar la API antes de repartir un APK. El
  despliegue del 23-09 **sigue sin confirmar** (ver la memoria del proyecto).

## Lo que se vio y NO se arregló

Fuera del alcance del lote. Queda apuntado para que no se pierda:

- **No hay pantalla de licencias de terceros** (ver GP-079). Bloquea publicar.
- **Texto a fuego (DEC-020)**: `ComidaActivity:205-208` escribe «kcal», «g
  prot», «g carbos» y «g grasas» (en inglés se lee «0.0g prot»), y
  `AdminAlimentosActivity:133` escribe «Todas» en el filtro de categoría.
- **El chip seleccionado de los filtros es lila** (`#E8DEF8`, el
  `secondaryContainer` por defecto de Material 3), fuera de la paleta. Se ve en
  rutinas, ejercicios y en las listas de administración.
- **El texto sale más pequeño en oscuro que en claro** en la misma pantalla e
  idioma (se ve en «Seguimiento nutricional», que en claro parte en dos líneas y
  en oscuro no). Pasa igual antes y después de este lote, así que no es de aquí.
  No se investigó.
- **«PROTEÍNAS» se parte en «PROTEÍNA / S»** en el resumen del onboarding, en
  claro y ES.
- **Alpha sobre iconos informativos**: el check de serie completada de
  `EjercicioPesoAdapter` (`setAlpha(0.35f)`) y el icono de `activity_eliminar_cuenta`
  (P-36 de la auditoría). WCAG 1.4.11 pide 3:1 a un gráfico que informa, y la
  tarea solo cubría texto.
- **Umbral repetido en prosa**: la descripción de cada logro en la tabla `logros`
  («Completa 7 sesiones…») repite el número en ES y EN. Si cambia un umbral de
  `TipoLogro`, hay que cambiar también esas dos columnas. `TipoLogroTest` fija
  los umbrales publicados para que el cambio sea a propósito.

## Rastro en la base local (3308)

- Cuentas nuevas: **`nuevo79` / `Nuevo1234.`** (sin datos: el caso «todo
  bloqueado»), **`valora77` / `Valora1234.`** (las tres sesiones de GP-077) y
  **`nivel78` / `Nivel1234.`** (onboarding a medias, parado en el paso 5).
- **`prueba`** tiene ahora 8 sesiones completadas y Constancia conseguida, una
  Coca-Cola de 330 g en el desayuno del 24-09 y un récord de 80 kg en press de
  banca (`progreso_ejercicios`), creado para que se viera la tarjeta dorada.
- **El emulador se reinició con `-wipe-data`**: la partición de datos se había
  quedado en 6 GB llenos aunque el AVD pide 12, y no dejaba instalar el APK.
