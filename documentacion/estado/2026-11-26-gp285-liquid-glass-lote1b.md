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
| `e064ff0a` | docs(changelog,documentacion): este informe y el CHANGELOG, en su primera versión |
| `80bd7c2d` | feat(android): el «+» de la barra pasa a cristal vivo teñido de naranja |
| `79662ec5` | docs(decisiones,documentacion): DEC-070 y la referencia recogen el «+» de cristal |
| `8b683df9` | fix(android): segunda pasada del revisor: las etiquetas de la barra en Android 8 o menos y la capa de pulsado del «+» |
| `4e6b181e` | fix(android): segunda pasada del revisor: la silueta muscular en claro |

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

## 6. El «+» de la barra, en cristal

Pedido el 27-11, con el lote ya en revisión: el «+» macizo chirriaba junto a la barra de cristal, en los dos temas. Pasa a **cristal vivo teñido de naranja**: sigue siendo la acción principal, con el material de la barra. Commit aparte, `80bd7c2d`.

- **El mismo componente y los mismos niveles que la cápsula** (`CristalVivo` con `app:cristalAccion`): refracción en Android 13 o más, desenfoque en Android 12. **En el nivel 3 queda como estaba**: macizo, con su degradado radial `#FFB27A` → `#FF6A00`, sin sombra ni reflejo.
- **Tinte** a 160° de `#FF6A00` sobre el desenfoque: 78 % → 60 % en claro (lo pedido) y 72 % → **65 %** en oscuro (se pidió 55 %: ver abajo). **Borde** de 1 dp blanco al 35 % (oscuro) y al 60 % (claro). **Brillo** interior de arriba, blanco al 55 %. **Reflejo**: una línea blanca al 45 % de 1,5 dp, a 5 dp del canto y solo en el cuarto de arriba, que se apaga hacia sus extremos; no llega al icono, que empieza a 19 dp. **Sombra**, la de la barra.
- **Sin cambios**: los 64 dp, su sitio, el velo y la hoja. El giro a «×» también, pero ahora **gira el icono y no el cristal**, que habría girado con su reflejo. El cambio de forma al pulsar (de 32 a 12 dp) se conserva: mueve el radio del cristal.
- Para TalkBack sigue siendo un botón con su nombre y su estado; su icono no es un nodo aparte.

**Contraste del icono** (`#1A0E00`, un gráfico: 3:1). El encargo pedía medirlo sobre el punto más claro; el icono es casi negro, así que su punto más desfavorable es el más **oscuro** de lo que puede quedar detrás (el fondo bajo el velo de la hoja abierta, que es cuando se ve la «×»). El test mide el peor y apunta los dos:

| | Oscuro | Claro |
|---|---|---|
| Niveles 1 y 2, peor punto | **3,18:1** (con el 55 % pedido, **2,51:1**: no llegaba) | 5,37:1 |
| Niveles 1 y 2, punto más claro | 5,21:1 | 9,57:1 |
| Nivel 3, peor punto | 6,61:1 | 6,61:1 |
| Nivel 3, punto más claro | 10,75:1 | 10,75:1 |

Como pedía el encargo, donde no llegaba se subió el tinte en vez de cambiar el icono: el tono de abajo en oscuro, del 55 % al 65 % (al 63 % daba 3,02:1, sin margen).

Medido además en captura, contra el cristal que rodea al icono:

| Emulador | Oscuro, cerrado | Oscuro, abierto | Claro, cerrado | Claro, abierto |
|---|---|---|---|---|
| API 36 (nivel 1) | 3,67–4,11:1 | 3,38–3,76:1 | 7,60–8,36:1 | 5,88–6,13:1 |
| API 32 (nivel 2) | 3,59–4,05:1 | 3,38–3,74:1 | 7,70–8,48:1 | 5,94–6,19:1 |
| API 24 (nivel 3) | 6,93–10,75:1 | 6,93–10,75:1 | 6,93–10,75:1 | 6,93–10,75:1 |

## Tests

**Unitarios**: 1.431 en local con el código final (de ellos, 4 `*WebTest` fallan solo en local por los restos sin versionar de «Un día» en `web/`; en el CI, con el árbol limpio, pasan). **Instrumentados**, con el código final (`4e6b181e`): 35 en cada emulador, 34 pasan y el otro es el apartado de GP-286, en API 36, 32 y 24. Lint pasa.

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
| `ContrasteCristalTest`: el icono del «+» de cristal en los dos temas y los tres niveles | con el tinte pedido en oscuro (72 % → 55 %): **2,51:1** contra un mínimo de 3 |
| `CristalTest`: el «+» es cristal vivo con su tinte y su sombra, su reflejo no llega al icono y solo gira el icono | con tres mutaciones: sin `cristalAccion`, girando el cristal y con el icono a 15 dp del borde |
| `BarraNavegacionRecorteTest` (instrumentado, +1): a letra 1,3 la caja de cada etiqueta es la de su texto | antes del arreglo, en API 24: caja de 123 px para un texto de 99 |
| `ContrasteCristalTest`: en claro, la zona trabajada de la silueta se distingue del cuerpo | con los colores de la primera pasada: **1,22:1** contra un mínimo de 3 |

CI de la PR #145 en verde en `4e6b181e`, el código final: `android-build-test`, `api-build-test`, `app-release-r8`, `secretos` y `cambios`.

## Fotogramas lentos (DEC-021)

Mismo guion que en el lote 1 (24 deslizamientos en Inicio, debug compilada entera, GPU del equipo, variantes intercaladas), en oscuro y en claro, con el emulador recién arrancado, solo y asentado. **El resultado es desigual y lo cuento entero: solo API 36 tiene cifra válida del código final.**

| Emulador | `main` (`4dfa4006`) | `c34f085f` (lote 1) | Final (`4e6b181e`) |
|---|---|---|---|
| API 36 (nivel 1), oscuro | 0,73 % (8 de 1.097) | 1,28 % (medida del lote 1, 26-11) | **1,47 % y 1,48 %** (16 de 1.088 y 16 de 1.083) |
| API 36 (nivel 1), claro | 0,73 % (8 de 1.093) | sin medida válida | **1,47 % y 1,48 %** (16 de 1.087 y 16 de 1.084) |
| API 32 (nivel 2) | 0,74 % (medida del lote 1, oscuro); sin medida válida del 27-11 | 1,51 % (medida del lote 1, oscuro) | **sin medida válida** |
| API 24 (nivel 3), oscuro y claro | 0,00 % (26-11) | 0,00 % (26-11) | 0,00 % con `7b7c55b4` (26-11); **sin medida válida del código final** |

**API 36, el nivel más caro (ahí el «+» de cristal desenfoca y además refracta): dentro del 2 %.** Las cuatro pasadas del código final (dos rondas, dos temas) dan 1,47–1,48 %, con p50 17–18 ms y p99 19–20 ms. `main` da 0,73 % en sus dos pasadas de la primera ronda, la misma cifra que en el lote 1. El camino es 0,73 % → 1,28 % → 1,47 %: el «+» de cristal y lo demás del lote 1b cuestan dos fotogramas de cada mil sobre el lote 1.

**Lo que no vale, y por qué.** El 27-11 el equipo estuvo en uso mientras se medía (el propietario trabajando, otras sesiones abiertas), y se nota en `main`, que es la app de antes del rediseño y no ha cambiado:

| Tanda | Qué salió | Por qué se descarta |
|---|---|---|
| Mañana, cadena de los tres emuladores (lanzada antes de un cambio de sesión) | API 36: `main` 17 % y 12 % en una ronda y 3 % en la otra. API 32: `main` 5–20 %, final 19–26 %. API 24: `main` 2–5 %, final 0,1–12 % | `main` fuera de rango y distinto de una ronda a otra; en API 24 el mismo APK había dado 0,00 % la noche anterior. Mientras medía se estaba trabajando en el equipo |
| Mediodía, API 36, segunda ronda de `main` | 14,1 % y 14,0 %, con p50 de 25 ms | las otras seis pasadas de esa tanda tienen p50 de 17–18 ms: en esas dos el emulador entero iba lento, no la app |
| Mediodía, API 32 | `main` 15–35 %, final 20–52 %, p50 de 23–28 ms en todas | el emulador entero iba lento en las ocho pasadas, también con `main` |
| Mediodía, API 24 | `main` 0,00–7,5 %, final 0,15–8,8 % | la misma variante va de 0 a 8 % entre rondas: el ruido es mayor que cualquier diferencia |
| Tercera ronda de API 36 | `main` 16,6 % y 16,4 %; final 14,9 % y 23,8 %; p50 de 29–36 ms | el equipo estaba al 60 % de carga al empezar |

El criterio para separar una pasada válida de una estropeada no mira el resultado: es la **mediana del tiempo de fotograma**. En una pasada estropeada todos los fotogramas van lentos (p50 de 23 a 36 ms), también los de `main`; en una válida de API 36 el p50 es de 17–18 ms.

De API 32 y API 24 no se deduce ni que el lote cumpla ni que no: no hay regresión a la vista (en API 24, la mejor pasada de cada variante es 0,00 % y 0,15 %; y en el nivel 3 el «+» sigue macizo, como estaba), pero tampoco cifra. Queda en «Pendiente»: es una orden y unos 35 minutos con el equipo sin usar.

Lo aprendido al medir: cada tanda, con el emulador recién arrancado, con 3 GB, **y sin nadie usando el equipo**; y si `main` no sale estable entre rondas, la tanda no vale.

## Producción

Release (R8, firma de release, `app-release.apk` de 6.613.342 bytes, compilada con el código final, `4e6b181e`) en el emulador de API 32, contra `https://api.gymprofit.app` (commit en producción: `20d234d6`; el lote no toca la API). 27-11.

- Cuenta de prueba `prueba-gp285c-a@gymprofit.app`, creada por la API (201); login 200.
- Entrar → cuestionario del alta (cuatro pasos) → plan → avisos («Ahora no») → **Inicio** de una cuenta nueva: fondo suave, «Hoy toca» con su carrusel y el fundido, la silueta, «Primeros pasos», «Tu día» a cero; el carrusel deslizado, el scroll bajo la barra y **el «+» de cristal**, con su hoja abierta y cerrada.
- **Tema claro** puesto desde Perfil → Tema: Inicio, scroll y hoja del «+».
- Sin errores de la app en logcat (0 `FATAL EXCEPTION`).
- **Cuenta borrada** (`DELETE /usuarios/me`, 200) y comprobado: el login devuelve **401**. La clave se eliminó entonces, no antes. La release se desinstaló del emulador.

El 26-11 se había hecho la misma prueba con `7b7c55b4` en API 36 (cuenta `prueba-gp285b-a`, borrada y comprobada igual); esta la repite con el «+» y los arreglos del revisor.

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

**Segunda pasada** (sobre `79662ec5`, ya con el «+» de cristal; con Opus, porque el cupo del modelo habitual de los subagentes se había agotado). Alcance acordado con el propietario: las ocho combinaciones en API 36; en API 32 y API 24, lo que cambia por nivel (ES, claro y oscuro, a letra 1,3).

| Emulador | Altos | Medios | Qué |
|---|---|---|---|
| API 36 (8 combinaciones) | 0 | 2 | la capa de pulsado del «+» era un círculo fijo dentro del cuadrado redondeado en que se convierte al pulsarlo (zona «+»); y en claro, al oscurecer el cuerpo de la silueta en la primera pasada, la zona trabajada se había quedado a 1,14:1 de él (zona tema claro) |
| API 32 | 0 | 0 | un bajo: la misma capa de pulsado |
| API 24 | **1** | 0 | **alto**: las etiquetas de la barra seguían descentrándose a letra 1,3 (hasta 12 px) al recrearse la pantalla; el arreglo de la primera pasada no había cambiado nada ahí. Y dos bajos del «+»: la capa de pulsado y un canto en dos líneas |

Los seis arreglos de la primera pasada, comprobados con medida por los tres revisores: resueltos (el de las etiquetas, solo en API 36 y API 32).

**El alto: la causa, con traza.** A letra 1,3 las etiquetas encogen hasta lo que cabe en su pestaña, y se encogían al **colocar** la barra. En Android 8 o menos, cuando el tamaño de un texto cambia durante la colocación, AppCompat marca para medir solo la etiqueta, sin avisar a sus padres. Con trazas en la barra se vio la cadena: la etiqueta, marcada; todo lo que hay entre ella y la barra, sin marcar. Ninguna medida posterior bajaba hasta la etiqueta, que se quedaba con la caja del tamaño grande (111 px) y el texto, ya encogido (98 px), a la izquierda. En un arranque en frío lo tapaba una medida posterior de la propia pestaña; al recrearse la pantalla (cambiar de tema o de idioma, volver tras un rato), no. El arreglo de la primera pasada pedía otra medida a la barra, que por lo mismo no llegaba. **Arreglo** (`8b683df9`): el tamaño se decide al medir la barra, que se vuelve a medir en la misma pasada. Medido en API 24, a letra 1,3, tras recrear la pantalla: cajas de 98 / 157 / 165 / 162 px (antes, 111 / 178 / 188 / 183).

**Los dos medios**, de las zonas que se arreglan en esta rama («+» y tema claro), cada uno comprobado a mano con su medida:

| Arreglo | Medida después |
|---|---|
| Capa de pulsado del «+» (`8b683df9`): su máscara cambia de esquinas con el botón | en captura con el «+» pulsado, API 24 y API 32, los dos temas: el cuadrado es uniforme hasta las esquinas (API 24: `#FF720D` en claro y `#F36500` en oscuro, sin ningún píxel `#FF6A00` dentro; antes, un círculo) |
| Silueta en claro (`4e6b181e`): el cuerpo en tinta `#45474C` y la zona trabajada en `#FF6A00` | cuerpo, 8,56:1 contra el cristal; zona contra el cuerpo, 3,24:1 a pleno color (por cálculo) y 2,28:1 (espalda) y 1,87:1 (pecho) con el alfa de la demo (antes, 1,14–1,24:1); zona contra el cristal, 3,76–4,62:1 |

De paso, en el nivel 3 el canto del «+» vuelve a medirse en píxeles enteros, como antes de ser cristal.

**Tercera pasada** (sobre `4e6b181e`), la vuelta extra por el alto, solo en API 24: **0 altos y 0 medios**. Las etiquetas, en claro y en oscuro, a letra 1,3:

| Caso | Inicio | Entrenar | Nutrición | Progreso |
|---|---|---|---|---|
| En frío · recreada · HOME y lanzador · HOME y recreada · otra pestaña y vuelta · tras la hoja del «+» (los seis, iguales) | 98 px, +0,5 | 157 px, +1,5 | 165 px, +1,0 | 162 px, 0,0 |

(ancho de la caja y descentrado de la tinta respecto a su pestaña, en px). A letra 1,0, como siempre: 84 / 135 / 143 / 140 px.

Bajos que quedan anotados, sin arreglar: en claro, la semana en curso a 0 kg no se distingue de las otras semanas vacías en la gráfica de kilos; a letra 1,3 el icono del rótulo de los tiles de «Tu día» queda 2 dp por encima del centro de su texto; con la hoja del «+» abierta en claro, los tres botones del sistema bajan a 1,99:1 bajo el velo; el bocadillo de Profi conserva el borde gris de la tarjeta opaca; y en la silueta, una zona con pocas series se acerca al cuerpo (1,87:1 el pecho en la demo): es lo que cuenta la escala. Y los conocidos: a letra 1,3 en un móvil bajo (393 × 851 dp, API 24) «Empezar» queda detrás de la barra; en Android 7, en claro, los botones del sistema llevan su velo oscuro.

## Decisiones tomadas

- **En el claro manda la regla de contraste (DEC-070)** sobre el valor pedido, tocando lo que menos se nota y dejando los brillos y el cristal como se pidieron. La tabla está arriba; si el propietario prefiere algún valor pedido, es cambiar un token y apuntarlo como excepción en DEC-070.
- **La gráfica de kilos conserva los colores pedidos y gana un filo** de 1 dp, en vez de oscurecer las barras.
- **Los bordes del sistema ya no son el color base liso** que dejó el remate, sino el propio fondo: es lo que quita la banda.
- **El nivel 3 es opaco del todo** (DEC-070 decía «casi opaco»).
- **El fondo pasa de degradados de la GPU a una imagen** calculada en un hilo aparte.
- **La hoja del «+» lleva más tinte que la barra** en claro.
- A letra 1,3 en móviles estrechos **el detalle del récord pierde su rótulo** antes que partirse o cortarse.
- **El tinte del «+» en oscuro baja hasta el 65 %, no hasta el 55 %**: es lo único que se aparta de lo pedido, y por la regla del propio encargo (subir el tinte antes que cambiar el icono).
- **El contraste del icono del «+» se mide sobre su peor punto, que es el más oscuro** de lo que queda detrás, no el más claro que decía el encargo: el icono es casi negro. Se apuntan los dos.
- **Del «+» gira el icono, no el cristal**, y en el nivel 3 no lleva sombra: «queda como ahora».
- **La silueta en claro va en tinta, con la zona trabajada en el naranja vivo.** Es un cambio de aspecto que nadie pidió así: sale de hacer cumplir a la vez que el cuerpo se vea (primera pasada) y que la zona se distinga de él (segunda). Si no convence, la alternativa es volver al cuerpo claro y darle un contorno, que pide rehacer los vectores.
- **El tamaño de las etiquetas de la barra se decide al medir**, no al colocar.
- **Alcance del revisor en la segunda pasada, reducido por el propietario**: las ocho combinaciones solo en API 36; en API 32 y API 24, claro y oscuro en español a letra 1,3. Y la vuelta extra, una y solo donde hubo un alto.
- **Lo publicado va a Drive, no al repo de marketing** (limpieza del escritorio).
- Durante la sesión el propietario autorizó **dos emuladores a la vez** para lo que no mide tiempos (revisor de UI, release contra producción); los fotogramas lentos se siguen midiendo con uno solo y el equipo quieto.

## Pendiente

- **Aprobación de las capturas por el propietario** y fusión de la PR #145 (sigue en borrador).
- **Fotogramas lentos del código final en API 32 y API 24**: tres intentos el 27-11, ninguno válido porque el equipo estaba en uso (ver «Fotogramas lentos»). Hay que medirlos con el equipo sin usar: unos 35 minutos. API 36, el nivel más caro, sí está medido y dentro del 2 %; en API 32 el «+» también desenfoca, y esa es la cifra que más falta.

## Dudas

- **Qué móvil y qué Android tiene el propietario.** Si es Android 11 o menos, lo que veía en la barra era el nivel 3 (y, en Android 7, el grano), y ahí el «+» sigue macizo. Si es Android 12 o más, el desenfoque que he medido es el de la maqueta, y lo que queda por explicar de su captura sería el canto de los brillos y la banda bajo la barra de estado, ya corregidos.
- **Los ajustes de contraste del claro**, en especial el enlace sobre el fondo (`#8B3000` en vez de `#A83A00`), el filo de la gráfica y el tinte de la hoja; y ahora **el 65 % del «+» en oscuro** y **la silueta en tinta**.
- **El «+» en oscuro se lee casi macizo**: con el naranja al 72–65 % sobre un fondo oscuro pasa poco de lo de detrás, y que parezca el material de la barra depende sobre todo del borde y del reflejo (lo anotó el revisor). Es lo que da la especificación; más transparencia choca con el contraste del icono.
- **La silueta en oscuro** sigue como antes del rediseño: la zona trabajada, a 1,2:1 del cuerpo, se distingue solo por el tono. No es de este lote; quedaría para el suyo.
- **El nombre accesible de la silueta de «Hoy toca» corta en tres zonas** (`InicioHoyDetalle`): si la rutina trabaja cuatro, la cuarta se pinta pero TalkBack no la dice. Lo vio el revisor en la tercera pasada; es anterior a este lote y no se ha tocado.
- **La bienvenida a letra 1,3 en un móvil bajo (API 24)**: al volver a entrar con la cuenta demo, «Ya tengo cuenta» no estaba a la vista bajo «Empezar» y hubo que bajar la letra para tocarlo. No es de este lote y no lo he comprobado a fondo (puede que baste con desplazar); lo dejo anotado.

## Capturas

En la carpeta de entregas del escritorio, `GymProFit\2026-11-26-gp285-lote-1b` (70 PNG, 80,8 MB, todas rehechas con el código final `4e6b181e`): **1**, Inicio arriba, con scroll y al final en los dos temas y los tres emuladores, y a letra 1,3 en API 36; **2**, recortes ampliados de fondo, barra de estado y barra, antes y después, en los tres; **3**, los dos carruseles; **4**, la fila del récord; **5**, la release contra producción; y **6**, el «+» de cristal: cerrado y abierto, en claro y oscuro, en API 36, 32 y 24 (la pantalla entera y el «+» ampliado ×4), y cómo cambia de forma al pulsarlo.

## Limpieza del escritorio

Encargo añadido durante la sesión: dejar el escritorio sin material de GymProFit suelto. Inventario primero (por SHA-256, sin mover nada), tabla al propietario, y ejecutado el 27-11 solo lo que aprobó.

| Elemento | Qué era | Destino | Peso |
|---|---|---|---|
| `gymprofit-web-2026-11-12-gp019.zip` y `…-11-13-gp266.zip` | zips de la web | **borrados**: sus ficheros son idénticos a `web/` en `main` | 1,0 MB |
| `tableros-lote11.zip` | tableros del lote 11 | **borrado**: sus 5 ficheros están en `documentacion/diseno/2026-11-06-rutinas-tiempo-y-crear/` | 13 KB |
| `capturas-carruseles\2026-11-06` y `\2026-11-13` | capturas de la app para carruseles | **borradas**: idénticas en `gymprofit-marketing\out\carruseles\capturas\` | 1,2 MB |
| `GP-285 lote 1\` y `GP-285 lote 1 remate\` | capturas del lote 1 | **borradas**: lote fusionado en `c34f085f` | 130,0 MB |
| `capturas-carruseles\publicar\k5-ig` y `k9-ig` | K5 y K9 de Instagram | **borradas**: idénticas en Drive, «Vídeos para subir\Carruseles» (comprobado otra vez antes de borrar) | 14,3 MB |
| `…\publicar\k3-tiktok`, `k6-tiktok`, `k7-tiktok` | PNG de TikTok con versión JPG | **borrados**: se conserva el JPG | 35,7 MB |
| `…\publicar\` K3, K6, K7 y K8 de Instagram; K3, K6 y K7 de TikTok en JPG; K5, K8 y K9 de TikTok en PNG | carruseles ya publicados | **Drive**, `GymProFit · Publicados\Carruseles\<KN>\ig` y `\tiktok` | 74,3 MB |
| `capturas-carruseles\linkedin-tarjetas` y los dos PNG del 19-11 y el 20-11 | posts de LinkedIn | **Drive**, `GymProFit · Publicados\LinkedIn\` | 2,0 MB |
| `gymprofit-web-2026-11-26-privacidad.zip` | web de la rama `privacidad-proveedores`, sin subir | **entregas**, `GymProFit\2026-11-26-privacidad-proveedores\` | 0,5 MB |
| `un-dia-fase-1\` | rama «Un día» | **entregas**, `GymProFit\2026-11-25-un-dia-fase-1\` | 17,4 MB |
| `un-dia-fase-4\` y `capturas-beta-2026-11-26\` | rama «Un día» y sus capturas de `/beta` | **entregas**, `GymProFit\2026-11-26-un-dia-fase-4\` | 63,5 MB |

- **Borrado: 182,3 MB** (178 ficheros). **A Drive: 76,3 MB** (86 ficheros); sus originales se borraron del escritorio después de comprobar cada copia: mismo SHA-256 al releerla de Drive y los 86 ficheros vistos en la nube con su mismo tamaño. **A entregas: 81,4 MB**, sin copiar: es un cambio de carpeta.
- Lo publicado **no fue al repo de marketing**, como se propuso al principio: va a Drive, a una carpeta «Publicados» junto a «Vídeos para subir» (decisión del propietario; el `.git` de marketing ya pesa 1,4 GB).
- En el escritorio, de GymProFit, quedan la carpeta `GymProFit\` y los clones de los repos.

**La regla, en los dos `CLAUDE.md`**: todo lo que se entrega va a `GymProFit\`, una subcarpeta por entrega (`AAAA-MM-DD-<lote o tarea>`), que se borra con el lote fusionado y aprobado salvo un APK pendiente de repartir; y lo ya publicado se archiva en Drive, en «Publicados», nunca en un repositorio.

| Repositorio | Commit | Cómo |
|---|---|---|
| App | `b5a96be1`, fusionado en `main` con `5829ac6c` | rama propia desde `main`, PR #146, CI en verde; rama borrada |
| Marketing | `a845189` | en `main`, subido, como manda su `CLAUDE.md` |
