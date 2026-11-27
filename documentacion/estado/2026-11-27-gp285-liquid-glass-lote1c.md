# GP-285 · Liquid Glass, lote 1c: fusión del 1b, el «+» con más vida, mediciones y «Ya tengo cuenta» (27-11-2026)

## Resumen

- **Lote 1b fusionado** en `main`: PR #145, merge `49c9a14b`, con la rama borrada. Antes se dejó anotado en su informe que API 32 y API 24 se fusionaban sin medida válida y se medían aquí (`a7806a7a`).
- **El «+» de cristal**, rama `gp-285-liquid-glass-lote-1c`, PR #147 en borrador, sin fusionar hasta que el propietario lo apruebe:
  - en los niveles 1 y 2 pasa al naranja de la marca, `#FF6A00` del 92 % al 85 %, sin luz debajo;
  - lleva un brillo radial pequeño arriba, el borde de 1,5 dp en degradado y un reflejo en media luna;
  - el nivel 3 queda como estaba.
- **«Ya tengo cuenta» a letra 1,3:** se alcanza con scroll y abre Entrar. Sin cambios.
- **Fotogramas lentos** (presupuesto, 2 %): API 32, de 1,51–1,52 % a 1,51–1,52 %; API 24, de 0,00 % a 0,00 %; API 36, de 1,57 % a 1,47–1,48 %.

## Commits

| Hash | Qué |
|---|---|
| `a7806a7a` | (rama del 1b) el informe del 1b anota que API 32 y API 24 se fusionan sin medida válida |
| `49c9a14b` | merge de la PR #145 en `main` |
| `24170d90` | el «+» con sus capas nuevas: suelo de luz, tinte a 160°, brillo radial, borde en degradado y media luna. El test de contraste mide el icono sobre la luz y el tinte, y un conjunto de superficies vacío ya no cuenta como aprobado |
| `877bf4d5` | DEC-070 y la referencia con lo anterior |
| `24e89260` | el tinte final: `#FF6A00` del 92 % al 85 %, sin luz y con el brillo pequeño arriba |
| `67fdde20` | DEC-070 y la referencia con el tinte final y la decisión sobre la transparencia |

## El «+», en tres vueltas

Lo pedido, de abajo arriba sobre el desenfoque:
- luz blanca al 14 % en oscuro y al 8 % en claro;
- tinte `#FF7A1A` del 58 % al 46 % a 160°;
- brillo radial blanco al 35 % en el tercio superior izquierdo;
- borde de 1,5 dp, blanco del 70 % arriba al 15 % abajo;
- reflejo en media luna, blanco del 40 % a transparente.

Objetivo: que se vea más luminoso y más naranja que el del 1b, y que se lea como cristal.

1. **Lo pedido (A).**
   - Salía más luminoso que el del 1b (L de 0,39 a 0,47), pero no más naranja: la saturación bajaba de 0,80 a 0,40. Era salmón en oscuro y melocotón en claro, medido en captura en API 32. A través de medio cristal se ve el fondo azul oscuro.
   - En oscuro el icono se quedaba en **2,86:1** con la luz al 14 %. Al 18 % daba 3,07:1 y al 20 %, 3,21:1.
   - **El test, visto en rojo:** sin la capa de luz, 2,15:1.
2. **B, elegida por el propietario con capturas:** tinte `#FF7A1A` del 82 % al 70 %, con la luz pedida.
   - Icono a 4,44:1 en oscuro y 6,37:1 en claro.
   - Con este tinte la luz ya no sostenía el contraste: sin ella daba 3,90:1.
   - Revisor de UI en API 32 y API 24: ningún alto ni medio.
   - En API 36, dos medios: el naranja quedaba a ΔE 32–40 de `#FF6A00` (terracota en oscuro, melocotón en claro) y dejaba ver lo de detrás 3–4 veces menos que la barra.
3. **Final, pedida por el propietario:** tinte de la marca, luz solo hasta el 3:1 y ΔE de 15 o menos.
   - `#FF6A00` del 82 % al 70 % sin luz: el icono ya daba 3,57:1, pero el ΔE se quedaba en 26–28. En oscuro lo oscurece el fondo que se ve por el 18–30 % que deja pasar el tinte; en claro lo aclaran el brillo radial, que cubría casi todo el botón, y el fondo claro.
   - Con el visto bueno del propietario, **`#FF6A00` del 92 % al 85 %, sin luz, y el brillo radial al 20 %, pequeño y solo en el tercio de arriba.**

**Decisión del propietario:** que el «+» deje ver poco de lo de detrás se acepta y queda en DEC-070. Es la acción principal y manda el color de la marca; el cristal lo dan el borde, el brillo y la media luna, como en los botones teñidos de Liquid Glass. Ese medio no cuenta para cerrar la revisión.

### Valores finales

| Capa | Valor |
|---|---|
| Suelo de luz | ninguno: `gp_mas_luz` transparente y sin pintar. El token se queda para cuando haga falta |
| Tinte | `#FF6A00` del 92 % al 85 % a 160°, en los dos temas |
| Brillo radial | blanco al 20 %, centrado al 32 % · 14 % de la caja, con radio del 19 % del lado |
| Borde | 1,5 dp, blanco del 70 % arriba al 15 % abajo |
| Reflejo | media luna metida 3 dp, del 40 % a transparente; acaba a 16 dp y el icono empieza a 19 |
| Nivel 3 | sin cambios: degradado radial `#FFB27A` → `#FF6A00` |

### Contrastes del icono `#1A0E00`

| | Oscuro | Claro |
|---|---|---|
| Test, niveles 1 y 2, peor punto | 4,92:1 | 6,05:1 |
| Test, niveles 1 y 2, punto más claro | 6,20:1 | 7,51:1 |
| Test, nivel 3 | 6,61:1 | 6,61:1 |
| Captura en API 32 y API 36, cerrado / abierto / pulsado | 5,31 · 5,15 · 4,62:1 como mínimo | 6,89 · 6,28 · 7,45:1 como mínimo |
| Captura en API 24 (nivel 3) | 7,42:1 como mínimo | 7,42:1 como mínimo |

En captura no cuenta el antialias del propio icono: se excluyen 4 px alrededor del trazo.

### Distancia al naranja de la marca (ΔE76, CIELAB)

Se mide en la mediana del relleno alrededor del icono, en la mitad de abajo, sin el brillo.

| | Oscuro | Claro |
|---|---|---|
| B (`#FF7A1A` 82–70 % con luz), API 36 | 30,3–32,8 | 32,4–35,7 |
| `#FF6A00` 82–70 % sin luz, API 36 | 25,6–26,7 | 24,4–28,0 |
| **Final, API 36** | **11,7** (peor píxel 12,4) | **9,2–10,1** |
| **Final, API 32** | **11,4–11,6** | **9,2–9,9** |

El revisor de UI midió lo mismo en API 36 por su cuenta: de 9,4 a 11,7 en reposo, con letra 1,0 y 1,3.

### Tests

- `ContrasteCristalTest`:
  - el icono se mide sobre la luz (si la hay) y el tinte;
  - un conjunto vacío de superficies ya no pasa;
  - **se vio en rojo con la capa de luz quitada**, con los valores pedidos: 2,15:1 frente a 3:1. Con el tinte final la luz ya no hace falta, así que quitarla ya no lo pone en rojo; lo que lo sostiene es el tinte.
- `CristalTest`: el orden de las capas de `Cristal.pintarAccion`. **Visto en rojo** al intercambiar el suelo y el tinte. El reflejo va por dentro del borde nuevo de 1,5 dp.
- Batería de unitarios: 1.431. Fallan solo los 4 `*WebTest` locales de siempre, que necesitan la web construida; en el CI pasan.

## «Ya tengo cuenta» a letra 1,3

En la bienvenida, en API 24 (393 × 851 dp), a letra 1,3:
- al abrir, la demo y el texto empujan «Empezar» al borde y «Ya tengo cuenta» no se ve;
- **con scroll se alcanza** (`NestedScrollView` con `fillViewport`), y al tocarlo abre Entrar.

Lo mismo en el móvil más bajo que se puede simular en ese emulador, 360 × 640 dp (`wm size 720x1280`, `wm density 320`): se alcanza con scroll y abre `LoginActivity`. **No se toca;** queda anotado.

## Revisor de UI (Opus): la barra y el «+»

| Emulador | Altos | Medios | Notas |
|---|---|---|---|
| API 24 (nivel 3) | 0 | 1, fuera del «+» | a letra 1,3, «Empezar» y «Cambiar» de «Hoy toca» quedan bajo la barra con el scroll arriba (393 × 851 dp). Ya estaba anotado en el 1b; las restricciones del lote dejan fuera Inicio. Se alcanza desplazando |
| API 32 (nivel 2), con B | 0 | 0 | el naranja se leía melocotón o cobre junto a «Empezar» (lo corrigió la vuelta final) |
| API 36 (nivel 1), con B | 0 | 2 | el naranja lejos de la marca (corregido) y la transparencia (decisión del propietario) |
| **API 36, con lo final, solo el «+»** | **0** | **0** | ΔE de 9,4 a 11,7 en reposo; icono a 5,22:1 o más; ni la media luna ni el brillo pisan el icono; borde, foco y capa de pulsado siguen la forma |

Bajos anotados, sin tocar:
- **Al pulsar, la capa de pulsado** (`boton_capa`, el 10 % de `colorOnPrimary`) oscurece el «+» en oscuro (ΔE ~18) y lo aclara en claro (ΔE ~17,5, e icono a `#2D2216`, 6,1:1). Solo dura mientras se mantiene el dedo; viene del 1b.
- El trazo del «+» (`ic_ms_add`, peso 400) mide 1,75 dp; la maqueta, 2,6.
- El giro de «+» a «×» usa `config_shortAnimTime`, no `Movimiento`.
- A letra 1,3 las etiquetas de la barra crecen ×1,21: es el tope `escalaEtiqueta`.
- Un trozo de «Ver el día» asoma en el hueco entre la cápsula y el «+».
- Nodos sin id del carrusel «Tus últimos entrenos» en el orden de foco.
- No se probó con TalkBack real: el estado abierto o cerrado del «+» sale del código (`setStateDescription`), y en API 24 el volcado no lo muestra.

## Producción

Release (R8, firma de release) contra `https://api.gymprofit.app`, en el AVD de API 34 para no perder la sesión de los tres emuladores de medir. Commit en producción: `20d234d6`; el lote no toca la API. Se hizo dos veces:

1. Con la variante B (`24170d90`): cuenta `prueba1c2711` creada por la API (201).
   - Entrar, el alta en cuatro pasos (con «Prefiero no decirlo»), el plan y «Ahora no» llevan a Inicio.
   - Scroll bajo la barra y hoja del «+» abierta y cerrada, en oscuro y, tras pasar a claro desde Perfil → Tema, en claro.
   - 0 `FATAL EXCEPTION`.
   - Cuenta borrada (`DELETE /usuarios/me`, 200) y comprobado: el login da **401**. La clave se borró después, no antes.
2. **Con el código final** (`24e89260`; release de 6.613.446 bytes): cuenta `prueba1c2712`, el mismo recorrido en oscuro, Inicio, scroll y «+».
   - 0 `FATAL EXCEPTION`.
   - Borrada (200) y comprobado el **401**.
   - La release se desinstaló del emulador.

## Fotogramas lentos (DEC-021)

**Guion fijo, el del lote 1:**
- `dumpsys gfxinfo reset`;
- cuatro veces tres swipes hacia arriba y tres hacia abajo, del 71 % al 29 % del alto, de 300 ms cada uno y con 450 ms de pausa;
- `dumpsys gfxinfo`.

Inicio, tema oscuro, debug, cuenta con datos.

**Las dos variantes**, compiladas desde cero en worktrees aparte: `main` (`49c9a14b`, ya con el 1b) y la rama (`67fdde20`). Las dos APK pesan lo mismo (24,95 MB); la compilación incremental de la rama pesaba 30,5 MB y se descartó.

**Protocolo:**
- cada emulador recién arrancado, con 3 GB y la GPU del equipo, solo;
- espera de 90 s y después hasta que su carga media baje de 1,5;
- la app compilada entera (`speed`) tras cada instalación;
- variantes intercaladas;
- una ronda de calentamiento que no cuenta y dos que sí;
- la carga del equipo, apuntada antes de cada pasada (0–13 %).

El propietario no usó el equipo.

| Emulador | `main` (2 rondas) | Rama (2 rondas) | Percentiles (los dos) |
|---|---|---|---|
| API 32 (nivel 2) | 1,51 % · 1,52 % (16 de 1.056) | **1,51 % · 1,52 %** (16 de 1.056) | p50 17 · p90 18 · p95 18–19 · p99 19 ms |
| API 24 (nivel 3) | 0,00 % · 0,00 % | **0,00 % · 0,00 %** | p50 5 · p90 5 · p95 5 · p99 6 ms |
| API 36 (nivel 1) | 1,57 % · 1,57 % (17 de 1.083) | **1,48 % · 1,47 %** (16 de 1.085) | p50 17–18 · p90 18–19 · p95 18–19 · p99 19 ms |

**Validez.** En los tres, `main` sale estable entre sus dos rondas: la tanda vale.

**Lectura:**
- Las tres dentro del 2 %.
- El «+» nuevo no cambia nada medible: su diferencia con `main` es de un fotograma o ninguno.
- API 36 se midió porque el «+» cambia su ruta de dibujo: dos `drawPath` más y un degradado radial.

**Lo que no valió:**
- **Primer intento de la cadena:**
  - en API 32 y API 36 no midió nada por un fallo del guion: en Android 10 o más la línea es `topResumedActivity`;
  - en API 24, `main` dio 34,6 % y 0,00 % en sus dos rondas. Las pasadas malas tenían p50 de 10–22 ms en las dos variantes: el emulador aún no se había asentado tras arrancar. **Se repitió una vez**, como permite el encargo, esperando también a que bajara la carga del emulador, y valió.
- En la repetición, la ronda de calentamiento de API 24 volvió a salir alta en `main` (65 %), y luego todo a 0,00 %: la primera pasada tras arrancar no vale nunca. Para eso está el calentamiento.

Las cifras de API 32 y API 24 son también **la medida pendiente del lote 1b**, porque `main` ya lo lleva:
- API 32: 1,51–1,52 %, frente al 0,74 % de antes del 1b (lote 1) y al 1,2 % del nivel 2 en el lote 1;
- API 24: 0,00 %, igual.

## Capturas

`Desktop\GymProFit\2026-11-27-gp285-lote-1c\`:
- **Por emulador** (API 36, API 32 y API 24): el «+» cerrado, abierto y pulsado, en claro y en oscuro, a tamaño real. Además, una comparación ampliada ×2 con el lote 1b (1b cerrado, 1c cerrado, 1b abierto, 1c abierto y 1c pulsado) y la tira «al pulsarlo» del 1b.
- **`0 Decidir el tinte`:** la comparación de A y B en API 32, y el «+» junto a «Empezar» en API 36 con B, con `#FF6A00` sin luz y con lo final.

En API 24 (nivel 3) el «+» no cambia respecto al 1b: es la referencia de que el nivel 3 sigue macizo.

## Decisiones tomadas

- El tinte, en tres vueltas con el propietario: ver «El «+», en tres vueltas».
- La transparencia baja del «+» es decisión del propietario (DEC-070).
- «Empezar» bajo la barra a letra 1,3 en API 24 no se toca en este lote: está fuera de las restricciones y viene del 1b.
- La prueba en producción, en el AVD de API 34 y no en uno de los tres de medir: la release no se instala encima de la debug sin desinstalarla, y se perdería la sesión local.

## Cierre: el «+» pulsado (`8e31b98f`)

- **Qué cambia.** Al pulsarlo, el «+» ya no pasa de círculo a cuadrado redondeado: un botón de cristal no cambia de forma (DEC-070). Se hunde a 0,94 y el brillo (20 → 32 %) y la media luna (40 → 58 %) suben a la vez, en `TOQUE` (120 ms) con la curva estándar; al soltar vuelve igual. Con «Quitar animaciones» no se hunde: solo sube la luz. La capa de pulsado (`capa_boton_acciones`) se borra. `BotonExpresivo.alPulsar` pasa a `CristalVivo.hundirAlPulsar`; los demás `BotonExpresivo` no cambian. Vibración y accesibilidad, igual.
- **Tests.**
  - `MasPulsadoTest` (instrumentado, API 36): **visto en rojo** con el código anterior, pulsado pasaba de 84 a 31,5 px de esquina. En verde con el nuevo, también con `animator_duration_scale 0`.
  - `ContrasteCristalTest.el_mas_pulsado_sigue_en_el_naranja_de_la_marca`: **visto en rojo** con la capa de pulsado. Mediana del ΔE76 sobre todo lo que puede quedar detrás: 10,9 en oscuro y 9,8 en claro; peor punto, 15,3 y 14,7 (igual en reposo).
  - Batería: 1.433 unitarios; fallan solo los 4 `*WebTest` locales. Lint limpio.
- **Revisor de UI (Opus), API 36, claro y oscuro:** ni altos, ni medios, ni bajos. Pulsado mide 158 px (60,2 dp) y es un círculo; ΔE de la mitad de abajo, reposo → pulsado: oscuro 11,4 → 11,7, claro 10,0 → 10,0; icono a 5,37:1 (oscuro) y 7,10:1 (claro); la luz acaba 7,6 dp por encima del icono. Sin animaciones, 64 dp pulsado y ΔE 11,8 / 10,0. Capturas en `Desktop\GymProFit6-11-27-gp285-lote-1c\pulsado\`.

## Cierre: botones de acción en Liquid Glass (opción B; rama `gp-285-botones`, PR #148)

Llegó con la PR #147 ya fusionada: va en rama y PR propias.

**Commits.** `916f72cf` (componentes y aplicación en Inicio), `63682498` («Empezar» no se corta a letra grande), `413ab3f4` (sombra cortada por abajo), `153b3047` (canto de «Cambiar» en claro).

**Qué hay.**
- **Principal y «+», el mismo material** (`bg_marca`, `Cristal.MARCA`): naranja macizo con degradado vertical `#FF7514` → `#F06000`, brillo 20 % pequeño arriba, borde 1,5 dp en degradado, media luna y sombra naranja al 35 % (10 dp, 3 hacia abajo). El «+» deja de ser cristal vivo. Igual en los tres niveles. La media luna, que la decisión no enumera, se queda: era parte del acabado aprobado del «+» y así hay una sola fuente.
- **Secundario** (`BotonCristal`): cristal estático en píldora con los tokens de las tarjetas; en el nivel 3, la superficie opaca del cristal vivo; en claro, borde de tinta al 20 % (`gp_cristal_boton_borde`).
- **Pulsado** (`Hundir`, una sola fuente): se hunden a 0,94 en 120 ms (curva estándar) y, en los de marca, suben brillo y media luna. Sin capa de pulsado. Con «Quitar animaciones», sin escala, solo la luz. `BotonMarca` y `BotonCristal` son `BotonExpresivo` que no cambian de forma (`cambiaForma()`); los demás `BotonExpresivo` siguen igual.
- **`FilaBotones`**: si el principal no cabe entero al lado del secundario, van uno sobre otro a todo el ancho.
- Solo Inicio y la barra. Fuera: `brillo_boton`, `Cristal.BRILLO`, `gp_mas_*`, `cristalAccion`, `VIVO_ACCION*`, `barra_mas_radio` y `MasPulsadoTest` (sustituido).

**Tests vistos en rojo.**
- `PulsadoSinCambioDeFormaTest` (instrumentado): con `main`, la esquina de «Empezar» pulsado pasaba de 3.094 a 3.731 px macizos (cambio de forma).
- `BotonesHoySinCorteTest`: con el código anterior, a letra 2,0 «Empezar» llevaba 5 de 7 letras en su línea.
- `BotonMarcaSombraTest`: el contenido de la tarjeta recortaba a su relleno.
- `ContrasteCristalTest.el_canto_del_boton_secundario_se_ve_sobre_la_tarjeta`: 1,05:1 con el borde blanco.
- Nuevos y en verde: `el_boton_de_marca_se_lee_y_sigue_en_la_marca`, `el_boton_secundario_se_lee_en_los_tres_niveles` y `CristalTest.el_principal_y_el_mas_comparten_el_boton_de_marca`.
- Batería: 1.433 unitarios; fallan solo los 4 `*WebTest` locales. Instrumentados de `ui.widget` (19) en verde en API 36; los tres nuevos, también en API 24. Lint sin errores.

**Contrastes medidos.**

| | Test (peor punto) | Captura API 36 | Captura API 24 |
|---|---|---|---|
| Texto e icono de marca `#1A0E00` | 5,76:1 (sobre `#F06000`) | 5,83–6,60:1 | 6,41:1 (≈5,8 abajo) |
| ΔE76 del relleno de marca | 5,8 arriba · 5,5 abajo | 4,3–5,2 | 4,1–4,3 |
| Secundario, oscuro | 7,59:1 | 8,01–9,44:1 | 13,5–13,9:1 (nivel 3) |
| Secundario, claro | 12,08:1 (16,94 sobre lo más claro) | 15,1–15,8:1 | 16,9:1 (nivel 3) |
| Canto del secundario sobre la tarjeta | 1,48:1 claro · 1,60:1 oscuro | — | 1,28:1 en claro en el píxel más marcado (antes 1,18, y blanco) · 3,83:1 en oscuro |

**Revisor de UI (Opus).**
- API 36, pasada 1: un **alto** («Empezar» cortado a letra 2,0), arreglado en `63682498`.
- API 36, pasada 2: sin altos. Dos medios:
  - **Sombra cortada por abajo**: arreglada en `413ab3f4`. Medida a mano en oscuro, letra 1,0: bajo el botón pasa de `#502F1B` al fondo en unos 30 px, sin línea recta.
  - **Letra 1,5: los botones quedan bajo la barra** (−49 dp): sin tocar (ver «Dudas»).
- API 24: sin altos. Un **medio**, «Cambiar» casi invisible en claro: arreglado en `153b3047` y medido a mano (tabla de arriba).
- Ningún botón cambia de forma al pulsarse en ninguna combinación; se hunden entre 0,93 y 0,946.

**Capturas.** En `Desktop\GymProFit\2026-11-27-gp285-lote-1c\botones\`: API 36 y API 24, claro y oscuro, letra 1,0 y 2,0 (y 1,3 y 1,5 en API 36), reposo y pulsado de los tres, y sin animaciones. Las de API 36 son de antes de los dos últimos arreglos (sombra y canto); `API24_claro_letra1.0_reposo_canto-cambiar.png` ya lleva el canto nuevo.

**Fotogramas lentos de Inicio (API 36).** **Sin medida válida.**

- **Montaje:** el guion fijo del lote 1, las dos variantes compiladas desde cero en worktrees (`main` `f6518eae` y la rama `153b3047`, 24,95 y 24,97 MB) y el emulador recién arrancado en frío. Antes de medir, 90 s de espera y carga por debajo de 1,5; después, calentamiento y dos rondas alternando las variantes.
- **Primera tanda:** `main` 6,67 % y 8,33 %, rama 9,21 % y 7,46 %.
- **Segunda tanda:** `main` 8,31 % y 6,25 %, rama 8,11 % y 8,35 %. En las dos, el p50 es de 28 ms (el 27-11 por la mañana era de 17).

`main`, que no lleva el cambio, no sale estable entre rondas y queda muy por encima de su 1,57 % de la mañana: las tandas no valen. El equipo no estaba quieto:
- había un `python` huérfano de una revisión (PID 18884, desde las 17:34) gastando casi un núcleo, que auto mode no me dejó parar;
- estaban abiertos el navegador y la API local.

Entre `main` y la rama no hay diferencia por encima del ruido, pero el «≤ 2 %» queda **sin comprobar**. Hay que repetir con el equipo sin usar.

**Inventario de botones de las pantallas sin rediseñar** (no se tocan; 60 principales y 26 secundarios).

- **Lote 2 · Sesión en vivo** (13 P · 8 S):
  - Apuntar entreno: `btnComoUltima` P, `btnAnadirEjercicios` S, `btnGuardar` P expresivo.
  - Resumen: `btnHechoResumen` P expresivo.
  - Sesión en vivo: `btnTerminar` P, `btnReintentar` P, `btnDescartarGuardado` S.
  - Descanso: `btnDescansoMenos`, `btnDescansoMas` y `btnDescansoSaltar`, los tres P.
  - Final de la lista: `btnReintentarCarga` S, `btnAnadirEjercicio` S.
  - Cronómetro: `btnParar` P, `btnCancelarCrono` S.
  - Terminar sesión: `btnGuardar` P, `btnAjustar` S, `btnSeguir` S.
  - Permiso del descanso: `btnDarPermiso` P.
  - Ya hay sesión: `btnVolverA` P, `btnDescartarYEmpezar` S.
  - Duración: `btnListoDuracion` P.
- **Lote 3 · Nutrición** (11 P · 6 S):
  - Añadir: `btnHecho` P expresivo.
  - Comida: `btnAnadirComida` P, `btnHoyNo` S, `btnSiCopiar` P.
  - Reciente: `btnCopiarReciente` P, uno por fila.
  - «No lo encuentras»: `btnEscanealo` S, `btnCrealo` S.
  - Crear alimento: `btnGuardarAnadir` P.
  - Plato: `btnGuardarPlato` P, `btnHojaAnadirRacion` P.
  - Escáner: `btnAjustesCamara` P, `btnHojaPrincipal` P, `btnHojaSecundario` S.
  - Ficha: `btnAnadirFicha` P, `btnUnidadVarias` S.
  - Semana: `btnVerMas` S.
  - Agua: `btnVaso` P (`UnelevatedButton`).
- **Lote 4 · Entrenar y Progreso** (10 P · 1 S):
  - Tu programa: `btnEmpezarHoy` P expresivo, `btnVolverASeguir` P, `btnDejarPrograma` S.
  - Añadir ejercicios: `btnContinuar` P.
  - Crear rutina: `btnElegirEjercicios` P.
  - Tu rutina: `btnAnadirEjercicios` P, `btnGuardarRutina` P.
  - Pauta: `btnAplicarPauta` P.
  - Detalle de rutina: `btnEntrenarRutina` P.
  - Programa: `btnSeguirPrograma` P.
  - Seguir programa: `btnConfirmarSeguir` P.
  - Progreso, historial, récords, logros, medidas y ejercicios no tienen botones propios.
- **Lote 5 · El resto** (26 P · 11 S):
  - Alta y bienvenida: `btnEmpezar`, `btnSiguientePaso` ×3, `btnGuardaPlan`, `btnListoHoja` ×2, `btnCrearCuenta`, `btnSeguirListo` y `btnActivarAvisos`, P; `btnIdioma` y `btnReintentarPrograma`, S.
  - Entrar y recuperar: `btnEntrar`, `btnEnviarCodigo` y `btnRestablecer`, P.
  - Verificar correo: `btnEnviarOtro` P, `btnAbrirCorreo` S, `btnPegar` S.
  - Perfil y ajustes: `btnCambiarObjetivo` S, `btnCerrarSesion` S, `btnAccion` de la fila P, `btnGuardar` de Tus datos, Cambiar correo y Cambiar contraseña P, `btnPermisoAvisos` P, `btnEliminarCuenta` P.
  - Admin: `btnGuardar` ×2 P, `btnDialogConfirmar` P, `btnDialogCancelar` S.
  - Diálogos comunes: `dialog_custom` (confirmar P, cancelar S); tema e idioma (cancelar S).
  - Arranque: `btnReintentar` P.
  - `view_estado` (en unas 35 pantallas): `btnEstadoAccion` P, `btnEstadoReintentar` S.
- **Choques con «nunca dos principales»**, para su lote:
  - Tu rutina: `btnAnadirEjercicios` + `btnGuardarRutina`.
  - Apuntar entreno: `btnComoUltima` + `btnGuardar`.
  - Sesión en vivo: los tres P del descanso, más `btnTerminar`.
  - Comida: `btnAnadirComida` + `btnSiCopiar`.
  - Añadir: `btnHecho` + un `btnCopiarReciente` por fila.
  - Plato: dos P.
  - `view_estado`, sobre pantallas que ya tienen su propio P.
- **Dudosos:** `btnEliminarCuenta` (acción destructiva con estilo de marca), `btnVaso` (fuera del sistema de estilos), `btnIdioma` (solo icono) y los «Cancelar» de los diálogos, que usan el contorno de Material y no el de la casa.

## Pendiente

- El «+» lo aprueba el propietario en el cierre: la PR #147 se fusiona con el CI en verde.
- Carpeta `Desktop\GymProFit\2026-11-26-gp285-lote-1b\`: borrada el 27-11, a petición del propietario.
- Los bajos del revisor de arriba.

## Dudas
- **Botones, fotogramas:** sin medida válida (ver arriba). Para repetirla hace falta el equipo quieto y el `python` huérfano parado.
- **Botones, letra 1,5:** con el scroll arriba, «Empezar» y «Cambiar» quedan detrás de la barra flotante (−49 dp; `EncajeHeroe` solo puede ceder 24). La regla vigente dice que, si ceder no basta, la tarjeta no se toca, así que no se ha cambiado. ¿Se acepta o se busca otra salida?
- **Botones:** el brillo pequeño de «Empezar» va al 32 % del ancho, como en el «+», y en una píldora ancha se lee como un punto a la izquierda (revisor, bajo). Está dentro de lo pedido; se puede centrar si se prefiere.
- **Botones:** a letra 2,0 el icono de «Empezar» no crece con el texto (24 dp fijos, como en todos los botones con icono).

- Resuelto en el cierre: la capa de pulsado ya no existe y el «+» pulsado queda a ΔE ≤ 15.
- En el peor punto del fondo (el más oscuro, en oscuro) el «+» queda a ΔE 15,3, en reposo y pulsado. La medida del propietario es la mediana (10,9); no se tocó el tinte aprobado.
