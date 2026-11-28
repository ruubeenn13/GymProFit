# Rediseño Liquid Glass — dirección A (aprobada por el propietario el 25-11-2026)

Referencia de diseño para GP-285. Los tres `.dc.html` son las maquetas aprobadas (Inicio, Sesión en vivo y Nutrición, 390×844, tema oscuro). Son HTML con estilos en línea: se leen como especificación, no se ejecutan. Los valores de abajo mandan sobre cualquier lectura aproximada de las maquetas.

## Principio

El cristal es la superficie de toda la app. Hay dos clases de cristal:

1. **Cristal estático** (tarjetas, chips, filas, tiles): está sobre el fondo ambiental, que ya es difuso. No necesita desenfocar nada en tiempo real: relleno translúcido + borde claro + brillo superior. Funciona igual en todas las versiones de Android.
2. **Cristal vivo** (barra de navegación y «+», cabeceras que flotan, panel de descanso, hojas inferiores): tiene contenido que se mueve por debajo al hacer scroll, así que desenfoca (y en API 33+ refracta) lo que hay detrás en tiempo real.

## Tres niveles por versión de Android (solo afectan al cristal vivo)

| Nivel | Condición | Cristal vivo |
|---|---|---|
| 1 | API 33+ | desenfoque + refracción en los bordes (lente) |
| 2 | API 31–32 | solo desenfoque |
| 3 | API ≤ 30, o texto de alto contraste activado, o bajo rendimiento medido | superficie translúcida más opaca, sin desenfoque |

Distribución de Android (Google, octubre de 2026): API 33+ ≈ 68 % · API 31–32 ≈ 10 % · resto ≈ 22 %.

## Tokens (tema oscuro, valores de las maquetas)

- **Fondo base:** `#08090B`.
- **Fondo ambiental (estático, un drawable, no se recalcula):** tres brillos radiales sobre el fondo base: naranja `rgba(255,106,0,.55)` arriba a la derecha (≈ 90 %, 8 %; elipse 420×320); rojo `rgba(190,40,20,.45)` a la izquierda a media altura (≈ 0 %, 42 %; 360×300); azul `rgba(60,90,200,.35)` abajo a la derecha (≈ 85 %, 78 %; 380×320). Cada pantalla puede variar posición y mezcla (Nutrición usa azul y verde `rgba(76,195,160,.3)`; Sesión, dorado `rgba(232,184,75,.28)`).
- **Cristal estático:** relleno en degradado de 160° blanco 16 % → 5 % (al 55 %) → 9 %; borde 1 dp blanco 18 %; brillo interior superior 1 dp blanco 35 %; sombra suave negra 35 %, 32 dp de difuminado, 12 dp hacia abajo.
- **Cristal vivo (barra):** degradado `rgba(40,42,48,.55)` → `rgba(20,22,26,.40)` sobre desenfoque 22 dp con saturación 180 %; mismo borde y brillo.
- **Selección «lente» (pestaña activa, día elegido, serie en curso):** cápsula blanco 14 % con brillo superior blanco 40 % y resplandor interior blanco 8 %; icono y etiqueta en `#FF8A3D`.
- **Radios:** tarjeta héroe 30 dp · tarjetas 26–28 dp · tiles 22 dp · chips y botones en píldora.
- **Botón principal:** `#FF6A00` con texto `#1A0E00` y brillo superior blanco 45 %. Secundario: cristal estático. **Desde el 27-11-2026, el principal y el «+» son el botón de marca** (abajo, «Botones de acción»).
- **«+»:** círculo de 64 dp, degradado radial `#FFB27A` → `#FF6A00`, icono `#1A0E00`. El 27-11-2026 pasó a cristal vivo teñido de naranja (abajo, «El «+», en cristal») y, en el cierre del lote 1c, **al botón de marca, el mismo que el principal** (abajo, «Botones de acción»).
- **Barra:** cápsula flotante de 64 dp a 16 dp de los lados y 26 dp del borde inferior (más el hueco de gestos), con el «+» separado a su derecha. El contenido pasa por debajo.
- **Texto:** principal `#F2F2F3`; secundario `#C9CDD6`; acento `#FFB27A`/`#FF8A3D`. Títulos y cifras en Barlow Condensed 700–800; texto en Barlow.
- **Récord:** fila con degradado dorado `rgba(232,184,75,.28)` → `.08` y brillo `rgba(255,236,190,.4)`; dorado `#E8B84B`.

## Reglas de legibilidad (obligatorias)

1. Ningún brillo ambiental intenso detrás de texto pequeño (< 18 sp). En la maqueta de Inicio, «La última vez, hace 3 días» queda ≈ 3:1 sobre el brillo naranja: es un fallo de la maqueta, no la referencia.
2. Cada par texto/superficie pasa 4,5:1 (3:1 en texto ≥ 24 sp o gráficos) medido sobre el **punto más claro** de lo que puede haber detrás (fondo + brillo + cristal compuestos).
3. Las cifras y textos clave nunca dependen del desenfoque para leerse: tienen que pasar también en el nivel 3.

## Tema claro

No hay maqueta. El del lote 1 (cristal blanco casi opaco sobre un fondo lavado) se rechazó: «no veo nada de liquid glass en modo claro, solo el fondo». El cristal solo se lee si el fondo tiene color y la tarjeta deja pasar parte de él. Estos son los valores del propietario (lote 1b, 26-11-2026) y lo que se implementó: donde uno choca con una regla de legibilidad, manda la regla (DEC-070) y queda con su cifra. Pendiente de aprobación con capturas.

| | Valor pedido | Implementado |
|---|---|---|
| Fondo base | `#F2EDE8` | el pedido |
| Brillos | naranja `rgba(255,122,40,.55)` arriba a la derecha · coral `rgba(255,96,110,.30)` a la izquierda a media altura · azul `rgba(80,125,255,.36)` abajo a la derecha · verde `rgba(60,190,160,.26)` abajo a la izquierda | los pedidos (el verde, en 6 %, 88 %; elipse 360 × 300) |
| Cristal estático | 160°, blanco 62 % → 34 % (al 55 %) → 48 %; borde 1 dp blanco 80 %; brillo superior blanco 100 % y línea inferior blanca 40 %; filo exterior `rgba(60,30,10,.06)` de 0,5 dp; sombra `rgba(90,40,10,.12)`, 28 dp y 10 hacia abajo | lo pedido; la sombra, solo por fuera de la forma |
| Cristal vivo (barra) | el mismo relleno sobre el desenfoque | el pedido en la barra y en la barra de la sesión; la hoja del «+», blanco 80 % → 70 % (su secundario, 3,44:1 → 5,84:1) |
| Texto | `#15171B`; secundario `#4A4F58`; acento y enlaces `#A83A00` | `#15171B`; secundario `#494E57` (4,47 → 4,54:1); acento sobre cristal `#A33800` (4,32 → 4,53:1); enlace sobre el fondo `#8B3000` (3,48 → 4,52:1) |
| Botón principal | `#FF6A00` con texto `#1A0E00`; secundario en cristal | el pedido (6,61:1) |
| Pestaña activa | lente blanca 75 % con brillo superior blanco y sombra `rgba(90,40,10,.12)`; icono y etiqueta `#A83A00`; las demás `#2A2E35` | lo pedido (5,27:1 y 5,70:1); la sombra, 6 dp de difuminado y 2 hacia abajo |
| Gráfica de kilos | barras en naranja al 28 % y la semana en curso en `#FF6A00` | los colores pedidos (1,22:1 y 1,93:1 contra el cristal) con un filo de 1 dp en `#A33800` en cada barra (4,53:1) |
| Pistas de las barras de progreso | `rgba(21,23,27,.10)` | la pedida; los rellenos de proteína y agua, `#2C67C1` y `#0D7385` (2,82 y 2,71 → 3,05 y 3,06:1 contra ella) |
| Bordes de las barras del sistema y velo del «+» | con el tono del fondo nuevo, sin escalón | el borde es el propio fondo ambiental, con sus brillos; el velo, negro 35 %, los cubre igual que al resto |

Otros dos ajustes por contraste, sobre valores que venían del lote 1: el dorado de texto sobre cristal, `#7A5C18` → `#745717` (4,19 → 4,53:1), y la acción de la barra de la sesión («Volver»), que en claro va en el texto de la barra y no en el acento (2,69 → 5,70:1).

### El «+», en cristal (27-11-2026)

El «+» macizo chirriaba junto a la barra de cristal, en los dos temas. Pasa a cristal vivo teñido de naranja: sigue siendo la acción principal, pero con el mismo material que la barra.

| | Valor pedido | Implementado |
|---|---|---|
| Material | el componente y los niveles de la barra: refracción en API 33+, desenfoque en 31–32; en el nivel 3, macizo `#FF6A00` como hasta ahora | lo pedido; en el nivel 3 conserva su degradado radial `#FFB27A` → `#FF6A00`, sin sombra ni reflejo |
| Tinte sobre el desenfoque | a 160°, naranja `#FF6A00` del 72 % al 55 % (oscuro) y del 78 % al 60 % (claro) | en claro, el pedido; en oscuro, **72 % → 65 %** (ver abajo) |
| Borde | 1 dp blanco al 35 % (oscuro) y al 60 % (claro) | el pedido |
| Brillo interior superior | blanco al 55 % | el pedido |
| Reflejo | una línea en el cuarto superior que se lea como una cápsula de cristal | una línea blanca al 45 % de 1,5 dp, a 5 dp del canto, solo en el cuarto de arriba, que se apaga hacia sus extremos; no llega al icono (que empieza a 19 dp) |
| Sombra | la suave de la barra, sin halo | la de la barra |
| Icono «+» y «×» | `#1A0E00`, 3:1 o más (es un gráfico) en los dos temas y los tres niveles; si no llega, se sube el tinte | `#1A0E00`; cifras abajo |
| 64 dp, giro a «×», velo y hoja | sin cambios | sin cambios; gira el icono y no el cristal, que giraría con su reflejo |

Contraste del icono (`ContrasteCristalTest`, sobre todo lo que puede quedar detrás, con el velo de la hoja abierta incluido). El icono es casi negro, así que su punto más desfavorable es el más **oscuro** de lo de detrás, no el más claro; se dan los dos:

| | Oscuro | Claro |
|---|---|---|
| Niveles 1 y 2, peor punto (el más oscuro) | 3,18:1 (con el 55 % pedido, 2,51:1) | 5,37:1 |
| Niveles 1 y 2, punto más claro | 5,21:1 | 9,57:1 |
| Nivel 3, peor punto | 6,61:1 | 6,61:1 |
| Nivel 3, punto más claro | 10,75:1 | 10,75:1 |

Medido además en captura, contra el cristal que rodea al icono: en API 36 y API 32, 3,67–4,11:1 cerrado y 3,38–3,76:1 abierto en oscuro, y 7,60–8,48:1 y 5,88–6,19:1 en claro; en API 24, 6,93–10,75:1.

### El «+» con más vida (lote 1c, 27-11-2026)

**Esto sustituye a la tabla de arriba en los niveles 1 y 2.** En oscuro, el tinte al 65 % sobre el fondo oscuro daba un naranja apagado, casi marrón, que se leía macizo, y el reflejo era una línea fina. El naranja tiene que ser el de la marca sea cual sea el fondo, y tiene que parecer cristal.

| Capa (de abajo arriba, sobre el desenfoque o la refracción) | Valor pedido | Implementado |
|---|---|---|
| Suelo de luz | blanco al 14 % (oscuro) y al 8 % (claro) | **ninguno** (`gp_mas_luz` transparente), ver abajo |
| Tinte | `#FF7A1A` del 58 % al 46 % a 160°, en los dos temas | **`#FF6A00` del 92 % al 85 %** (`gp_mas_tinte_alto`/`_bajo`), ver abajo |
| Brillo radial interior | en el tercio superior izquierdo, blanco al 35 %, muy difuminado | **blanco al 20 %, pequeño y solo en el tercio de arriba**: centrado al 32 % · 14 % de la caja, radio del 19 % del lado, a nada con un tramo al 40 % de su alfa en la mitad (`gp_mas_brillo_interior`) |
| Borde | 1,5 dp, degradado blanco del 70 % arriba al 15 % abajo | el pedido (`gp_mas_borde_alto`/`_bajo`, `barra_mas_borde`) |
| Reflejo de arriba | una media luna ancha, no una línea, blanco del 40 % a transparente | la forma metida 3 dp, menos ella misma bajada hasta el cuarto de arriba: ancha arriba y en punta a los lados, del 40 % a nada de arriba abajo; acaba a 16 dp, antes del icono (19 dp) (`gp_mas_reflejo`, `barra_mas_reflejo_dentro`) |
| Nivel 3 | macizo `#FF6A00` | sin cambios: el degradado radial `#FFB27A` → `#FF6A00` de siempre |
| Icono | `#1A0E00`, 3:1 o más en el peor punto; si no llega, se sube la luz, no el tinte | `#1A0E00`; cifras abajo |

**El tinte, en tres vueltas con el propietario.**

1. Con lo pedido (`#FF7A1A` 58 % → 46 % sobre la luz) el «+» salía más luminoso que el del lote 1b, pero no más naranja: a través de medio cristal se ve el fondo y la saturación bajaba de 0,80 a 0,40 (salmón en oscuro, melocotón en claro, medido en captura en API 32). Y en oscuro el icono no llegaba: 2,86:1 con la luz al 14 % (2,15:1 sin ella; 3,21:1 con la luz al 20 %).
2. El propietario eligió `#FF7A1A` 82 % → 70 % con la luz pedida (icono 4,44 · 6,37:1). El revisor de UI en API 36 lo midió a ΔE 32–40 de `#FF6A00` (terracota en oscuro, melocotón en claro) y dejando ver lo de detrás 3–4 veces menos que la barra.
3. El propietario pidió el tinte de la marca, la luz solo hasta el 3:1 y ΔE 15 o menos. `#FF6A00` 82 % → 70 % sin luz (el icono ya daba 3,57:1) se quedaba en ΔE 26–28: en oscuro lo oscurece el fondo que se ve por el 18–30 % que deja pasar, y en claro lo aclaran el brillo radial y el fondo claro. **Lo final: `#FF6A00` 92 % → 85 %, sin luz, y el brillo radial al 20 %, pequeño y solo en el tercio de arriba.** En captura en API 36 (mediana alrededor del icono, mitad de abajo): **ΔE 11,7 en oscuro y 9,2–10,1 en claro** (peor píxel, 12,4).

Que casi no deje ver lo de detrás es **decisión del propietario** (DEC-070): el «+» es la acción principal y manda el color de la marca; el cristal lo dan el borde, el brillo y la media luna, como en los botones teñidos de Liquid Glass.

Contraste del icono (`ContrasteCristalTest`, el tinte sobre todo lo que puede quedar detrás, con el velo de la hoja abierta; el brillo, el borde y el reflejo solo aclaran, y el icono es oscuro):

| | Oscuro | Claro |
|---|---|---|
| Niveles 1 y 2, peor punto (el más oscuro) | 4,92:1 | 6,05:1 |
| Niveles 1 y 2, punto más claro | 6,20:1 | 7,51:1 |
| Nivel 3 | 6,61:1 | 6,61:1 |

## Botones de acción (cierre del lote 1c, 27-11-2026; opción B del propietario)

**Regla: el principal y el «+» son sólidos de marca con acabado; el secundario, cristal estático; nunca dos principales en la misma vista.** Si todo fuera del mismo material, el usuario no distinguiría la acción principal. Ningún botón cambia de forma al pulsarlo.

| Botón | Material | Pulsado |
|---|---|---|
| Principal y «+» (`BotonMarca`, `bg_marca`) | **No es cristal.** `#FF6A00` macizo con acabado: degradado vertical muy suave `#FF7514` → `#F06000` (ΔE76 5,8 y 5,5), brillo radial blanco 20 % arriba (en el «+», pequeño a la izquierda; en el principal, una elipse centrada de en torno al 60 % de su ancho, pedido del propietario en el cierre), borde 1,5 dp blanco 70 % → 15 %, media luna blanca 40 % → transparente en el cuarto de arriba y sombra suave naranja al 35 % (10 dp de difuminado, 3 hacia abajo). Igual en los dos temas y en los tres niveles. Texto e icono `#1A0E00`, 5,76:1 en el peor punto | Se hunde a 0,94 en 120 ms (curva estándar) y el brillo pasa al 32 % y la media luna al 58 %; sin capa de pulsado. Con «Quitar animaciones», solo la luz |
| Secundario (`BotonCristal`) | Cristal estático en píldora con los tokens de las tarjetas (relleno a 160°, borde 1 dp, filo de luz arriba), sin desenfoque. En el nivel 3, la superficie opaca del cristal vivo. Texto `gp_cristal_texto`, 4,5:1 o más sobre lo más claro que pueda quedar detrás | Se hunde a 0,94; con «Quitar animaciones», nada |

Se aplica en lo que ya es Liquid Glass (Inicio y la barra). Las demás pantallas los adoptan con su lote; el estilo global de los botones no cambia hasta entonces. Los círculos naranjas de las filas son iconos, no botones.

## Lote 2: Sesión en vivo (28-11-2026)

Lo que cambia al llevar `sesion-en-vivo.dc.html` a la app. Como en el lote 1: donde un valor de la maqueta choca con una regla de legibilidad, manda la regla (DEC-070), y queda con su cifra. Los valores que valen son los de `app/GymProFit/DESIGN.md` («Sesión en vivo») y `colors.xml`, medidos en `ContrasteCristalTest`. Pendiente de aprobación con capturas.

| Valor de la maqueta | Problema medido | Implementado |
|---|---|---|
| Fondo: naranja `rgba(255,106,0,.5)` en 100 %, 0 %; dorado `rgba(232,184,75,.28)` en 0 %, 60 %; azul `rgba(60,90,200,.4)` en 90 %, 95 % | en oscuro, sobre una tarjeta que pasa por el brillo dorado, «Tu récord» (dorado) quedaba en 3,70:1 y el secundario en 4,28:1; con el naranja al 50 %, el secundario en 3,44:1 | las mismas elipses; naranja al **26 %**, dorado al **18 %** y azul al 40 %. En claro (sin maqueta): naranja `#FF7A28` al 55 %, dorado al 45 % y azul `#507DFF` al 36 % |
| Cabecera: minimizar y «Terminar» de cristal, y el título suelto | con la lista pasando por detrás, el título suelto no se lee, y el texto asoma nítido entre las piezas | las dos piezas, de cristal (estático desde el remate, abajo), y el título suelto, como en la maqueta; **la lista se desvanece antes de esa fila** (el borde de arriba es macizo hasta donde empieza el resumen y degrada 24 dp, ya detrás de él), como el contenido de Inicio bajo la barra de estado |
| Resumen con las tres cifras a 30 px | la escala de letra es la de la app (lote 1) | `Cifra.Pequena` (26 sp) y el apoyo («kg», «de 18») al 60 %; se conserva debajo la barra de la sesión, que la maqueta no trae |
| Tarjeta de ejercicio: «La última vez, 80 × 8 · Tu récord: 82,5 × 6» en una línea | la app ya da cada dato en su línea, con la pauta y las notas | las líneas de siempre, con «Tu récord» en el dorado de texto (4,60 · 4,53:1) |
| Fila del récord: tinte dorado 28 % → 8 %, número en `#E8B84B` y «¡Récord!» en `#F0DDA8` | dorado sobre dorado: `#E8B84B` da 3,88:1 sobre el tinte aun sin brillo detrás, 2,83:1 en el peor punto; `#F0DDA8`, 3,88:1 | el tinte de la maqueta **sobre un surco negro al 25 %**, y el número y «¡Récord!» en `#F0DDA8` (4,80:1). En claro, sin surco y en `#6B4F12` (4,76:1). «¡Récord!» va donde iba la marca anterior, como en la maqueta, y el trofeo, en el botón de marcar |
| Serie en curso: lente blanca 12 %, número y kilos en `#FFB27A`, la marca anterior en el secundario | la lente es la del sistema (7 %, lote 1); sobre ella `#FFB27A` da 3,95:1 y el secundario 4,37:1 | la lente del sistema en radio 16; número, kilos y aro en `#FFC9A3` (4,69:1); la marca anterior, en el texto principal |
| Kilos y repeticiones sueltos, sin caja | son campos que se escriben: necesitan un borde que se vea y un estado de foco | un surco (la pista de las barras) con borde a 3,09:1; con foco, 2 dp en el naranja de los gráficos |
| Serie hecha: solo el check verde; sin hacer: un aro blanco al 50 % | hecha y sin hacer no pueden distinguirse solo por el color (DEC-019), y el aro tiene que pasar 3:1 | hecha, círculo relleno con borde y check en `#8FE3B0`; sin hacer, aro vacío a 3,09:1; la del récord, círculo dorado con el trofeo |
| Botones de 44 px | zona táctil de 48 dp | 48 dp |
| «Añadir serie» con borde a trazos blanco 30 % | un control: su borde, a 3:1 | el borde a trazos, a 3,09:1, en una píldora de 40 dp dentro de 48 de zona táctil |
| Tarjeta «Después» con el ejercicio siguiente, al 85 % | quitaría las tarjetas de los demás ejercicios, que la app enseña y deja editar | todas las tarjetas, como hasta ahora |
| Descanso: «−15» y «+15» de cristal y «Saltar» naranja | — | lo de la maqueta, con los componentes de DEC-070: «Saltar» es el botón de marca (el único principal) y los otros dos, de cristal. Con sus textos de siempre («−15 s», «+15 s») |
| Descanso: cuenta a 40 px, barra de 6 px en degradado `#FFB27A` → `#FF6A00`, «Siguiente: serie 4 · 85 kg» | en claro, `#FF6A00` contra su pista no llega a 3:1 | lo de la maqueta en oscuro; en claro, la barra en `#A33800` (3,52:1). Sustituye al aro que había: mismo tiempo y misma curva |
| Con letra grande | la maqueta no lo dibuja | los botones del descanso bajan a su propia fila si no caben al lado de la cuenta; título y programa, hasta dos líneas sin partir palabras; los rótulos de columna encogen lo justo y los cuatro van al mismo tamaño (GP-286) |

Las hojas inferiores de la pantalla (terminar, cronómetro, permiso de alarmas) no entran en este lote.

### Remate del lote 2 (28-11-2026)

Lo que cambió tras revisar el propietario las capturas. Las reglas, en DEC-070; el detalle, en `app/GymProFit/DESIGN.md` («Sesión en vivo»).

| Antes | Problema | Ahora |
|---|---|---|
| Minimizar y «Terminar», de cristal vivo | la lista se desvanece antes de esa fila: no hay nada que desenfocar | **cristal estático** (`BotonCristal`). Regla: un elemento flotante bajo el que nunca pasa contenido es cristal estático |
| La lista pasa bajo el resumen y bajo el descanso tal cual | el texto de la tarjeta de debajo asomaba nítido justo bajo el canto, con media línea ya tapada | **un velo junto al canto** (el fondo ambiental): opaco 2 dp por fuera del canto y una rampa de 22 dp hacia la lista; por dentro del cristal la lista sigue pasando desenfocada. En los tres niveles. La lista deja 8 dp más bajo el resumen para que en reposo no toque la primera tarjeta |
| Campos de kg y reps en claro: un surco (tinta al 10 %) con borde | sobre la tarjeta clara se leían como cajas grises opacas | **cristal estático**: el relleno de las tarjetas (blanco 62 % → 34 % → 48 %) y canto de tinta al 50 % (3,10:1). Lo escrito, 13,85:1; la pista, 6,46:1. El oscuro no cambia |
| Título y programa, hasta dos líneas cada uno | a letra 2,0, cuatro líneas («Cuerpo / completo C / Cuerpo / completo») que empujaban todo | el título, dos líneas como mucho, encogiendo lo justo para caber entero; **desde letra 1,5 el programa solo se ve si no repite el título y cabe en una línea** (TalkBack lo lee siempre). «Terminar» no se corta |

## Ajustes al implementar (GP-285, lote 1, 26-11-2026)

Donde un valor de arriba chocaba con una regla de legibilidad, manda la regla (DEC-070). Los valores que valen son los de `app/GymProFit/DESIGN.md` («Liquid Glass») y `colors.xml`; cada uno está medido en `ContrasteCristalTest`. Para los lotes siguientes:

| Valor de la maqueta | Problema medido | Valor implementado |
|---|---|---|
| Brillo naranja al 55 % | el fondo es fijo y el contenido se desplaza: sobre el brillo, el texto secundario de una tarjeta quedaba en 3,19:1 y el acento en 2,87:1 | 26 % (el peor par, 4,61:1) |
| Lente al 14 % con icono y etiqueta en `#FF8A3D` | la etiqueta de la pestaña activa, 3,19:1 sobre la barra (3,74:1 en el nivel 3) | lente al 7 % y `#FFB27A` (5,65:1 y 6,45:1) |
| Tinte de la barra al 55 % → 40 % | con contenido claro pasando por detrás (el círculo dorado del trofeo), la etiqueta de la pestaña activa bajaba a 4,15:1 | 72 % → 62 % |
| Pista de las barras en blanco al 14 % | el relleno contra su pista, 2,56:1 | surco oscuro, negro al 40 % (5,43:1) |
| Barras apagadas de la gráfica en blanco al 20 % | menos de 3:1 contra el cristal | blanco al 45 % |
| Sombra del cristal estático | es translúcido: la sombra se vería a través, y sobre `#08090B` no se distingue | sin sombra; solo la lleva el cristal vivo |
| «La última vez, hace 3 días» suelto sobre el brillo | el fallo que ya señala la regla 1 | en un chip de cristal, con el texto principal |
| Ejercicios de «Hoy toca» en chips («Press de banca», «+4») | quitaría la pauta y la última marca de cada ejercicio, que la app ya enseña | se conserva el carrusel, con cada ejercicio en un tile de cristal |
| «Tu día» con «1.420 · de 2.450» | cambiaría el texto («2.180 · te quedan») | tres tiles con los textos de siempre y su barra |
| Títulos de sección en Barlow Condensed 22/700, «Torso A» a 44/800 y cifras de los tiles a 28 | la escala de letra de la app (`CimientosTest`: sección en Barlow SemiBold de 20 sp, título de 34, cifra pequeña de 26) es de todas las pantallas, y cambiarla solo en Inicio la partiría en dos | la escala de siempre; si se quiere la de la maqueta, se cambia para toda la app en su propio lote |

### Remate del lote 1 (26-11-2026)

Lo que el propietario corrigió al aprobar el diseño con las capturas. Vale para los lotes siguientes:

| Qué | Cómo queda |
|---|---|
| El botón principal traía un resplandor naranja al 45 % alrededor (las maquetas lo conservan en su CSS) | retirado de esta referencia: la sombra de Android no lo da y no se pinta |
| El contenido que pasa bajo la barra flotante llegaba hasta los iconos del sistema (las cifras bajo ◁ ● ■; arriba, cortado en seco bajo el reloj) | dos bordes que lo desvanecen hacia el fondo base antes de llegar: macizos en la franja de cada barra del sistema y en degradado hacia el contenido; con gestos, el de abajo queda corto. Con la hoja del «+» abierta, el velo los cubre con su mismo tono |
| «La última vez, hace 1 semana» se partía en dos líneas junto a las siluetas con letra grande | una línea siempre, debajo de la fila del título y las siluetas, a todo el ancho de la tarjeta |
| «Empezar» quedaba a 1,5 dp de la barra flotante con letra 1,3 | margen mínimo de 16 dp (`barra_margen_boton`) entre el último botón de una tarjeta y la barra |

Detalle y medidas en `app/GymProFit/DESIGN.md` («Bordes del sistema» y «Cristal vivo»).

El desenfoque de la maqueta (`blur(22px)`) es la desviación (σ); en Android el «radio» de `RenderEffect` que da esa σ es (σ − 0,5) / 0,57735.

### Lote 1b (26-11-2026): suavidad

Lo que el propietario veía en el oscuro («se ven píxeles detrás, quiero que sea smooth»), medido en los tres emuladores antes de tocar nada, y cómo queda. Vale para los lotes siguientes:

| Qué | Causa | Cómo queda |
|---|---|---|
| «Bordes de mancha» en el fondo | cada brillo bajaba en línea recta hasta apagarse al 70 % del radio (el `transparent 70%` de la maqueta): la pendiente pasa de golpe a cero y se ve el canto | la misma elipse y el mismo alfa en el centro, con una curva plana en el centro y plana al apagarse |
| Grano en el fondo en Android 7 | tres degradados de la GPU, cada uno con el tramado de su versión: saltos de hasta 10 niveles entre píxeles vecinos | el fondo es una imagen calculada a resolución completa con tramado propio de menos de un nivel, igual en todas las versiones |
| Banda oscura bajo la barra de estado | los bordes del sistema pintaban el color base liso encima del brillo | el borde es el propio fondo ambiental |
| Texto nítido a través de la barra (Android 11 o menos) | el nivel 3 no desenfoca y su superficie iba al 98 % | opaca del todo |
| «Poco desenfocado» bajo la barra (Android 12 o más) | no se reproduce: el desenfoque es el de la maqueta (σ 22 dp, convertido a px con la densidad) y el lienzo no se reduce antes | sin cambios; un test lo ata a la maqueta |

