# GP-285 · Liquid Glass, lote 1b: tema claro rehecho, cristal suave, carruseles y récord (y GP-287)

26 y 27-11-2026. Rama `gp-285-liquid-glass-lote-1b`, **PR #145 en borrador: no se fusiona hasta que el propietario apruebe las capturas**. Parte de `main` en `c34f085f` (lote 1 y remate). Solo app: no hay despliegue de API ni versión nueva (DEC-056).

El propietario rechazó el claro del lote 1 («no veo nada de liquid glass en modo claro, solo el fondo») y veía el cristal pixelado en oscuro («se ven píxeles detrás, quiero que sea smooth»).

## Commits

| Hash | Qué |
|---|---|
| `7b7c55b4` | feat(android): el lote 1b: suavidad, tema claro, carruseles y fila del récord |
| `010dd204` | test(android): GP-287, `CampoContrasenaTest` espera el nodo de cada versión de Android |
| `26508c77` | docs(decisiones,documentacion): DEC-070 y la referencia recogen el lote 1b |
| `e7b1eddb` | fix(android): lo que encontró el revisor de UI en la primera pasada |

## 1. Suavidad: diagnóstico antes de tocar nada

Capturas a tamaño real y ampliadas de fondo, bordes y barra en los tres emuladores (API 36, nivel 1; API 32, nivel 2; API 24, nivel 3), con la GPU del equipo, sobre `c34f085f`. Las hipótesis del encargo (degradado sin tramado, imagen pequeña ampliada, radio en px, captura reducida sin filtrar) **no se confirmaron**; lo que había era otra cosa:

| Defecto | Causa, con su evidencia | Arreglo |
|---|---|---|
| «Bordes de mancha» en el fondo | Cada brillo bajaba **en línea recta** hasta apagarse al 70 % del radio (`Ambiente.alfaEn`, el `transparent 70%` de la maqueta). Perfil medido en la columna x = 10 de Inicio en oscuro: sube a ritmo constante hasta 87 y baja igual, y pasa de golpe a plano: ahí el ojo ve un canto. En las tres API. | La misma elipse y el mismo alfa en el centro, con una curva sin esquinas (1 − t²(3 − 2t)): plana en el centro y plana al apagarse. |
| Grano en el fondo (Android 7) | Eran tres degradados de la GPU con `setDither`. En API 36 y 32 el tramado es de 1 nivel entre vecinos (sin bandas: 1.543 cambios de sentido en 1.801 px). En **API 24, saltos de hasta 10 niveles entre píxeles vecinos**, en damero: los tres tramados del sistema se suman. | El fondo es una imagen calculada una vez por tamaño, a resolución completa, con tramado propio de menos de un nivel (`Ambiente.pintar`). Después: salto máximo de 1 nivel por canal en las tres API, y el mismo fondo píxel a píxel (245 colores distintos en la misma columna en API 36 y en API 32). |
| Banda oscura bajo la barra de estado | Los bordes del sistema del remate pintaban el **color base liso** encima del fondo: bajo la barra de estado, a la derecha, el fondo pasaba de (8,9,11) a (62,30,9) en 24 dp, justo donde el brillo naranja es más vivo. | El borde desvanece el contenido hacia **el propio fondo ambiental** (sus mismos píxeles con el alfa de la rampa). Después: de (56,28,9) a (70,33,8), continuo, sin escalón. |
| «Se ven píxeles detrás» de la barra | En el **nivel 3** (Android 11 o menos) la barra no desenfoca y su superficie iba al 98 %: el texto que pasa por detrás («Pecho») se entreveía, nítido. | Opaca del todo (`gp_vivo_opaco_*`). |
| «Poco desenfocado» bajo la barra | **No se reproduce** en los niveles 1 y 2: `cristal_desenfoque` es un `dimen` de 22 dp (57,75 px a 420 ppp), el radio que se pide a `RenderEffect` es el que da esa desviación y el lienzo no se reduce antes. En las capturas, siluetas y texto desaparecen bajo la barra. | Sin cambios; un test lo ata a la maqueta. |
| Esquinas y filos de 1 dp | Sin dientes en las tres API (ampliados ×8): trazos y recortes con suavizado. | Sin cambios. |

**Si el propietario lo veía en un móvil con Android 11 o menos, lo que veía era el nivel 3** (texto nítido a través de la barra y, en Android 7, el grano). No sé qué móvil es: ver «Dudas».

Lo que salió por el camino: pintar una pantalla entera de píxeles en el hilo de la interfaz tardaba **1,2 s** (0,4–0,6 s con el bucle en enteros) en el emulador con la compilación de pruebas, porque ese bucle corre interpretado en el primer arranque. El fondo se calcula en un hilo aparte y aparece con un fundido corto; los bordes lo siguen. Pesa lo que la pantalla (10 MB a 1080 × 2400).

## 2. Tema claro

Con los valores del propietario: fondo `#F2EDE8` con cuatro brillos (naranja 55 %, coral 30 %, azul 36 % y verde 26 %), cristal al 62–34–48 % con borde blanco 80 %, brillo superior, línea inferior, filo de 0,5 dp y **sombra solo por fuera** (28 dp de difuminado, 10 hacia abajo), el mismo relleno en la barra, lente blanca al 75 % con sombra, botón principal `#FF6A00` con texto `#1A0E00`, gráfica de kilos en naranja.

La sombra usa la técnica de `944a604e` (silueta difuminada en una imagen de un canal, teñida al pintar) con dos cambios, porque aquí el cristal deja ver lo de debajo en todas las versiones: el hueco de la forma va vaciado en la imagen en vez de recortado al dibujar, y se pinta en ocho trozos alrededor de la tarjeta (`SombraCristal`, `TarjetaCristal`).

**Los brillos y el cristal son los pedidos, sin tocar.** Con los valores tal cual, 12 pares no llegaban a su mínimo (visto en rojo en `ContrasteCristalTest`); manda la regla (DEC-070) y cede lo que menos se nota:

| Valor pedido | Medido | Implementado |
|---|---|---|
| Secundario `#4A4F58` | 4,47:1 sobre el brillo naranja | `#494E57` (4,54:1) |
| Acento `#A83A00` sobre cristal | 4,32:1 | `#A33800` (4,53:1) |
| Enlace `#A83A00` sobre el fondo | 3,48:1 | `#8B3000` (4,52:1) |
| Dorado de texto `#7A5C18` (lote 1) | 4,19:1 | `#745717` (4,53:1) |
| Gráfica de kilos, `#FF6A00` y naranja al 28 % | 1,93:1 y 1,22:1 contra el cristal | los colores pedidos, con un filo de 1 dp `#A33800` en cada barra (4,53:1) |
| Pista casi negra al 10 % | proteína 2,82:1 y agua 2,71:1 contra ella | la pista pedida; rellenos `#2C67C1` y `#0D7385` (3,05:1 y 3,06:1) |
| Relleno de la barra en la hoja del «+» | su secundario, 3,44:1 con el velo detrás | barra y barra de la sesión, el pedido; la hoja, blanco 80 % → 70 % (5,84:1) |
| Acento en «Volver» de la barra de la sesión | 2,69:1 fuera de la lente | `#2A2E35` (5,70:1) |
| Silueta muscular `#D2D2D6` (revisor) | 1,39:1 contra el cristal | `#737579` sobre cristal (3,11:1) |

Pares que pasan con el valor pedido: texto `#15171B` (9,73:1 sobre el fondo, 12,08:1 sobre el cristal), pestaña activa `#A83A00` sobre la lente (5,27:1), las demás `#2A2E35` (5,70:1), botón principal (6,61:1). Todos los pares de los dos temas están en `ContrasteCristalTest`, medidos sobre el punto más desfavorable, y en `DESIGN.md` («Reglas de legibilidad» y «Tema claro»).

## 3. Carruseles

«Hoy toca» y «Tus últimos entrenos» son `CarruselView`: llegan a los dos bordes de su contenedor, el último elemento a la vista se desvanece en 28 dp (medido por el revisor: 73–78 px según la densidad) y encajan por elemento al soltar (`EncajeCarrusel`). Cada ejercicio de «Hoy toca» es una parada de TalkBack; los entrenos ya lo eran.

Dos cosas que solo se vieron en pantalla: el borde difuminado de Android cae donde acaba el relleno de la vista, no en su borde (los últimos 20 dp reaparecían sin fundir), y con elementos pulsables el primer toque no llega al carrusel (no encajaba al soltarlo despacio después de un lanzamiento).

## 4. Fila del último récord

`view_record_inicio` y `FilaRecord`: el nombre en una línea (puntos suspensivos si no cabe), la marca a su derecha, entera, y el detalle debajo, en una línea, desde el nombre hasta el borde: «Último récord · hace 6 días · antes 95 × 12» (la marca anterior va sin «kg»; TalkBack la lee con su unidad).

Medido con las fuentes de la app en un móvil de 1080 px a 420 ppp: el detalle mide 588 px a letra 1,0 y la línea tiene 664: cabe. A letra 1,3 mide 761: **la marca baja debajo del texto** y el detalle empieza bajo el trofeo (806 px). Y si ni así cabe (letra 1,3 en un móvil de menos de 400 dp de ancho: en el emulador de API 24 le sobraban 3 px), pierde su rótulo: «Hace 6 días · antes 95 × 12».

Comprobado con el nombre más largo del catálogo, «Elevaciones para deltoides posterior sentado con el torso inclinado con mancuernas» (82 caracteres; `FilaRecordTest` vigila que lo siga siendo leyendo el CSV del catálogo).

## 5. GP-287

Los dos tests de `CampoContrasenaTest` que fallaban en Android 7: **la causa era el test, no la app.** Desde Android 8 el nodo de accesibilidad tiene sitio para la pista (`getHintText`) y su texto es solo lo escrito; antes no existe y Material mete la pista en el texto, detrás de lo escrito: «••••••••••••, Contraseña». `CampoContrasena.enmascarar` ya lo tenía en cuenta. El test espera ahora el nodo de la versión en la que corre. Pasa en API 24, 32 y 36.

## Tests

**Unitarios**: 1.429 en local (de ellos, 4 `*WebTest` fallan solo en local por los restos sin versionar de «Un día» en `web/`; en el CI, con el árbol limpio, pasan). **Instrumentados**: 34 en cada emulador, 33 pasan y el otro es el apartado de GP-286, en API 36, 32 y 24.

Nuevos, y cómo se vieron en rojo:

| Test | Rojo |
|---|---|
| `AmbienteTest` (4): el brillo sin canto, el tramado, lo pintado frente a la cuenta exacta, sin degradados de la GPU | escritos antes del código: con la recta y sin tramado, la peor ventana quedaba a 0,40 niveles con y sin tramado |
| `ContrasteCristalTest`: los pares del claro, la gráfica de kilos con filo, la hoja, el botón principal, la silueta | con los valores pedidos tal cual, 4 tests y 12 pares por debajo de su mínimo; y quitando el ajuste del enlace (3,48:1) |
| `CristalTest`: el desenfoque en dp y el de la maqueta (leído de `inicio.dc.html`, sin cifras a mano), el nivel 3 opaco, TalkBack en los carruseles | poniendo el `dimen` en px, el opaco al 98 % y quitando la marca de TalkBack |
| `EncajeCarruselTest` (5) | quitando la regla de «un lanzamiento avanza al menos uno» |
| `FilaRecordTest` (7) | con `marcaAbajo` devolviendo siempre falso y el detalle a tres líneas |
| `TextoQueEncogeTest` (4) | el fallo real: con el ajuste en el layout, Inicio no abría a letra 1,0 |
| `FilaRecordPintadaTest` (instrumentado, 2): el nombre más largo a letra 1,0 y 1,3, ES y EN | con la mutación (el detalle acababa en puntos suspensivos) y, de verdad, en API 24 (3 px) |
| `BordeSistemaPintadoTest` (+2): con fondo ambiental, el tramo macizo es ese fondo | con el borde volviendo al color liso |
| `CampoContrasenaTest` (GP-287) | los dos que fallaban en API 24 |

CI de la PR #145 en verde en `26508c77`: `android-build-test`, `api-build-test`, `app-release-r8`, `secretos` y `cambios`.

## Fotogramas lentos (DEC-021)

Mismo guion que en el lote 1 (24 deslizamientos, unos 1.290 fotogramas por pasada en Inicio, debug compilada entera, GPU del equipo, variantes intercaladas), ahora en oscuro y en claro, con tres variantes: `main` antes del rediseño (`4dfa4006`), el lote 1 (`c34f085f`) y el final.

| Emulador | `main` | `c34f085f` | Final |
|---|---|---|---|
| API 24 (nivel 3), oscuro | 0,00 % (0 de 1.288 y 0 de 1.291) | 0,00 % (0 de 1.288 y 0 de 1.289) | **0,00 %** (0 de 1.291 y 0 de 1.286) |
| API 24 (nivel 3), claro | 0,00 % (0 de 1.288 y 0 de 1.290) | 0,00 % (0 de 1.297 y 0 de 1.289) | **0,00 %** (0 de 1.288 y 0 de 1.289) |
| API 36 (nivel 1) | pendiente | pendiente | pendiente |
| API 32 (nivel 2) | pendiente | pendiente | pendiente |

Percentiles en API 24, iguales en las tres variantes: p50 5 · p90 5 · p99 5–6 ms. La columna «Final» de API 24 se midió con `7b7c55b4`, antes de los arreglos del revisor (`e7b1eddb`).

**API 36 y API 32 están sin medir**: ver «Pendiente». Las cifras del lote 1 para comparar (solo oscuro): API 36, 0,73 % → 1,28 %; API 32, 0,74 % → 1,51 %.

Lo aprendido al medir: con el emulador abierto desde hacía horas, el **mismo** APK de `main` dio 0 lentos de 1.308 en una ronda y 133 de 1.302 en la siguiente. Esas tandas se tiraron; cada tanda se mide con el emulador recién arrancado, con 3 GB, y sin tocar el equipo mientras.

## Producción

Release (R8, firma de release, `app-release.apk` de 6.612.033 bytes, compilada con `7b7c55b4`) en el emulador de API 36, contra `https://api.gymprofit.app` (commit en producción: `20d234d6`; el lote no toca la API).

- Cuenta de prueba `prueba-gp285b-a@gymprofit.app`, creada por la API (201).
- Entrar → cuestionario del alta (cuatro pasos) → plan → avisos → **Inicio** de una cuenta nueva: fondo suave, «Hoy toca» con su carrusel y el fundido, «Primeros pasos», «Tu día» a cero; el carrusel deslizado, el scroll bajo la barra y la hoja del «+» abierta y cerrada.
- **Tema claro** puesto desde Perfil → Tema: Inicio, scroll y hoja del «+».
- Sin errores de la app en logcat.
- **Cuenta borrada** (`DELETE /usuarios/me`, 200) y comprobado: el login devuelve **401**. La clave se eliminó entonces. La release se desinstaló del emulador.

La release probada es anterior a los arreglos del revisor (`e7b1eddb`).

## Revisor de UI

Inicio y la barra, las **ocho** combinaciones (ES y EN, claro y oscuro, letra 1,0 y 1,3) en cada emulador, con la hoja del «+».

**Primera pasada** (sobre `7b7c55b4`):

| Emulador | Altos | Medios | Qué |
|---|---|---|---|
| API 24 | 1 | 3 | alto: la sombra de las tarjetas de «Tus últimos entrenos» se cortaba en recto en claro (escalón de 6–12 niveles). Medios: la silueta muscular casi no se veía en claro (1,39:1); «HOY TOCA · DÍA 1 DE 4» dejaba el «4» en una segunda línea a letra 1,3; las etiquetas de la barra quedaban descentradas hasta 4,4 dp al volver a la app a letra 1,3 |
| API 32 | 0 | 2 | el carrusel de entrenos no encajaba al soltarlo despacio después de un lanzamiento (60 dp fuera); la sombra de los entrenos (el mismo alto de arriba) |
| API 36 | 0 | 2 | la sombra de los entrenos y la silueta (los mismos) |

Los cinco, arreglados en `e7b1eddb`, y **cada uno comprobado a mano con una medida** en API 24 y API 32:

| Arreglo | Medida después |
|---|---|
| Sombra de los entrenos | bajo el canto de las tarjetas, salto máximo entre filas de 3 niveles (antes, escalón de 6–12); bajo la primera tarjeta, de (205,212,210) a (211,226,223) en 80 px (API 24) |
| Encaje tras un arrastre lento | tras lanzar y luego arrastrar 180 px en 1,5 s, el canto izquierdo de la tarjeta queda en 55 px (API 24) y 53 px (API 32): el margen de 20 dp |
| Silueta | cuerpo en `#737579` (9.451 px de ese color en la silueta de «Hoy toca») |
| Rótulo de «Hoy toca» | ES, claro, letra 1,3: 61 px de alto (una línea; antes, 119) en API 24 y 59 en API 32 |
| Etiquetas de la barra | ES, oscuro, letra 1,3: 0 px distintos en la barra antes y después de salir al lanzador y volver |

Arreglando el rótulo metí un fallo y lo cazó esa misma comprobación: con el ajuste de tamaño en el layout, **Inicio no abría a letra 1,0** (el ajuste de AppCompat exige máximo mayor que mínimo y ahí son iguales). No llegó a ningún commit: se activa desde código y solo con margen (`TextoQueEncoge`), con su test. Después, Inicio abierto a letra 1,0 y 0,85 en los dos temas sin errores en logcat.

**La segunda pasada del revisor no se pudo completar**: ver «Pendiente».

Bajos que quedan anotados, sin arreglar: en claro, la semana en curso a 0 kg no se distingue de las otras semanas vacías en la gráfica de kilos; a letra 1,3 el icono del rótulo de los tiles de «Tu día» queda 2 dp por encima del centro de su texto; con la hoja del «+» abierta en claro, los tres botones del sistema bajan a 1,99:1 bajo el velo; el bocadillo de Profi conserva el borde gris de la tarjeta opaca. Y los conocidos: a letra 1,3 en un móvil bajo (393 × 851 dp, API 24) «Empezar» queda detrás de la barra; en Android 7, en claro, los botones del sistema llevan su velo oscuro.

## Decisiones tomadas

- **En el claro manda la regla de contraste (DEC-070)** sobre el valor pedido, tocando lo que menos se nota y dejando los brillos y el cristal como se pidieron. La tabla está arriba; si el propietario prefiere algún valor pedido, es cambiar un token y apuntarlo como excepción en DEC-070.
- **La gráfica de kilos conserva los colores pedidos y gana un filo** de 1 dp, en vez de oscurecer las barras.
- **Los bordes del sistema ya no son el color base liso** que dejó el remate, sino el propio fondo: es lo que quita la banda.
- **El nivel 3 es opaco del todo** (DEC-070 decía «casi opaco»).
- **El fondo pasa de degradados de la GPU a una imagen** calculada en un hilo aparte.
- **La hoja del «+» lleva más tinte que la barra** en claro.
- A letra 1,3 en móviles estrechos **el detalle del récord pierde su rótulo** antes que partirse o cortarse.
- Durante la sesión el propietario autorizó **dos emuladores a la vez** para lo que no mide tiempos (revisor de UI, release contra producción); los fotogramas lentos se siguen midiendo con uno solo y el equipo quieto.

## Pendiente

- **Segunda pasada del revisor de UI** con `e7b1eddb`, en los tres emuladores: las de API 24 y API 32 se cortaron al agotarse el cupo de uso del modelo de los subagentes (se repone el 1-12). Los arreglos están comprobados a mano con medida, pero la regla pide la pasada entera.
- **Fotogramas lentos en API 36 y API 32**, y repetir API 24 con el código final.
- **Aprobación de las capturas por el propietario** y fusión de la PR #145.

## Dudas

- **Qué móvil y qué Android tiene el propietario.** Si es Android 11 o menos, lo que veía en la barra era el nivel 3 (y, en Android 7, el grano). Si es Android 12 o más, el desenfoque que he medido es el de la maqueta, y lo que queda por explicar de su captura sería el canto de los brillos y la banda bajo la barra de estado, ya corregidos.
- **Los ajustes de contraste del claro**, en especial el enlace sobre el fondo (`#8B3000` en vez de `#A83A00`), el filo de la gráfica y el tinte de la hoja.

## Capturas

En la carpeta de entregas del escritorio, `GymProFit\2026-11-26-gp285-lote-1b`: Inicio arriba, con scroll y al final en los dos temas y los tres emuladores; recortes ampliados de fondo, barra de estado y barra, antes y después, en los tres; los dos carruseles; la fila del récord; y la release contra producción.

## Limpieza del escritorio

Encargo añadido durante la sesión. Hecho el inventario (sin mover ni borrar nada) y presentada la tabla al propietario; la ejecución espera su aprobación. Se completará aquí con la tabla final, los commits de cada repositorio y lo que ocupaba lo borrado.
