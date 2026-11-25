# Plan del rediseño de gymprofit.app: «Un día» (fase 0)

Plan, no código. Sustituye a la portada de GP-205 («el dorsal») cuando el propietario acepte la
propuesta de decisión DEC-069 (`documentacion/PRODUCT-DECISIONS.md`, en «Pendientes de decidir»).
No va en el zip, como `DISENO.md` y `PRODUCT.md`: está en la lista de lo que se queda fuera, en
`README.md` y en `scripts/servir-web.py` (`FUERA_DEL_ZIP`).

Escrito el 25-11-2026 contra `main` en `4451a0c0`. Donde el código y la documentación no coinciden,
manda el código y aquí se dice. Puesto al día el mismo día con las respuestas del propietario a las
dudas del §8 y sus correcciones (la luz, el récord, las transiciones entre páginas y el cursor).

---

## 0. Lo que el código cambia del guion de partida

Lo que se ha comprobado en `main` y obliga a ajustar el guion que venía en el encargo:

1. **El desayuno de la cuenta demo no se escanea.** `scripts/sembrar_demo.py` apunta desayunos de
   básicos del catálogo propio (café con leche, tostada con tomate y aceite, avena con plátano, pan con
   jamón). El escáner (`EscanerActivity`, `activity_escaner.xml`) lee productos envasados de
   `productos_off` (DEC-040, DEC-041): su hoja dice «Producto envasado · <marca>». Una escena de
   escaneo necesita un producto envasado que la demo no tiene. Resuelto (§8, 1): a las 8:00 se busca el
   desayuno, y el escáner pasa a las 17:00 con un yogur envasado.
2. **Las series no se rellenan solas con lo de la última vez.** En la sesión en vivo
   (`SesionEnVivoAdapter`, `LogicaSesion.pistaPeso`/`pistaReps`) la última vez sale en la columna
   «Anterior» (`60 × 8`) y como **pista gris** dentro de los campos de kg y repeticiones; quien entrena
   escribe el número (o lo deja igual) y marca la serie con ✓. La web enseña eso: la pista, el número que
   entra y el ✓. No enseña un relleno automático que la app no hace.
3. **El descanso empieza al marcar la serie**, con el de la pauta del ejercicio (`LogicaDescanso`, si el
   ajuste «Empezar el descanso al marcar una serie» está encendido, que es el de serie). En Torso A el
   press de banca lleva 3 × 6–10 y **120 s** de descanso (`db/semillas/catalogo-plantillas-v1.json`,
   `GIM-TORSO-A`). Botones reales: −15 s, +15 s y Saltar.
4. **El récord no tiñe la serie entera de golpe**: la fila de la serie que lo bate pasa a
   `gold-row` (`#2A2414` en oscuro), sale el aviso «¡Récord! <marca>» con «Tu mejor marca era <marca>»
   sobre `gold-container`, y el trofeo crece de 0,5 a 1 (`Movimiento.celebrarRecord`, momento 25). En la
   app ese crecimiento va con `REBOTE`; la web no rebota (`MovimientoWebTest.ningunaCurvaRebota`), así que
   aquí va con la enfatizada. La demo no tiene un récord «en directo»: el de las 19:40 es el del día de
   ejemplo, y se lee como tal.
5. **«La semana por zonas» no está en Progreso, está en Inicio**: la tarjeta «Tu semana»
   (`fragment_inicio.xml`, `cardMusculos`: «<n> de <m> zonas», la silueta `SiluetaMuscularView` y las barras
   de `item_zona_semana.xml`). La gráfica de peso sí está en Progreso («Tu cuerpo», `cardCuerpo`,
   MPAndroidChart). A las 23:00 salen dos pantallas distintas, y se dice.
6. **Torso A es de lunes.** La demo sigue «Torso y pierna» (`GIM-TP`) con lunes torso A, martes pierna A,
   jueves torso B y viernes pierna B, y entrena entre las 18:05 y las 19:35. El día de ejemplo es **un
   lunes**: así «Hoy toca» dice Torso A sin forzar nada.
7. **Profi ya está en la app** (`filaProfi` en Inicio, `view_profi_copiar.xml` con
   `profi_senala_base.webp`), aunque `app/GymProFit/PRODUCT.md` diga «Todavía no está en la app». Esa
   frase está desfasada. Su material vive en el repositorio de marketing. Resuelto (§8, 4): Profi no
   sale en la web.
8. **Las cifras de la demo animadas contradicen `DISENO.md`** («Lo que se valoró y no entra»: animarlas
   las convierte en afirmaciones del producto). El encargo lo resuelve presentándolas como «un día de
   ejemplo»; DEC-069 lo deja escrito y `DISENO.md` se reescribe en la fase 2.
9. **Los tests de la web no corren en el CI cuando solo cambia `web/`.** Viven en el módulo Android
   (`app/GymProFit/app/src/test/.../ui/*WebTest.java`) y el job `app` de `ci.yml` solo arranca si cambia
   `app/` o el propio workflow. Hoy una PR que solo toca la web pasa el CI sin ellos. Se arregla en la
   fase 1, antes de mover nada.
10. **Los correos piden `/img/logo-correo.png`** a `gymprofit.app` (`PlantillaCorreo`, GP-266). Esa ruta
    no puede cambiar con el nuevo build.
11. **La portada actual tiene tema claro** (`portada.css`, `prefers-color-scheme: light`). «Un día» se
    piensa sobre carbón. Resuelto (§8, 2): la portada nueva va siempre en carbón; las páginas legales y
    la beta conservan claro y oscuro.
12. **El récord sale de `LogicaRecordVivo.supera`**
    (`app/GymProFit/app/src/main/java/es/pmdm/gymprofit/envivo/LogicaRecordVivo.java`), la misma regla que
    `CalculadoraRecords` en la API (`api/gymprofit-api/src/main/java/com/gymprofit/api/service/record/CalculadoraRecords.java`,
    GP-088): con peso, la marca es la serie **más pesada** y, a igual peso, la de **más repeticiones**;
    igualar no es récord; con peso, sin peso y por tiempo no compiten. `LogicaRecordVivo.alMarcar` lo
    celebra **una sola vez por ejercicio y sesión**, en la primera serie que supera la marca. No es un 1RM
    estimado ni el volumen. La versión anterior de este plan subía las series 1 y 2 a +2,5 kg: con esa
    regla el récord saltaba ya en la serie 1, no en la 3. Se corrige en las 19:00 y las 19:40 (§1).
13. **El descanso no dice «Después:»**: el texto de la app es «Siguiente: …» (`envivo_descanso_siguiente_*`
    de `values/strings.xml`). La web usa el de la app.

---

## 1. Guion hora a hora

### Reglas comunes a todas las escenas

- **El día es un lunes de ejemplo de la cuenta demo** (`@dani.entrena`, sembrada con
  `scripts/sembrar_demo.py`). Cada cifra que se enseña sale de esa cuenta en el entorno local, se copia
  en la fase 1 a `src/datos/dia-ejemplo.json` con el comando y la fecha con que se sacó, y no se escribe a
  mano en ningún componente. Junto a la primera pantalla, una vez, la etiqueta **«Un día de ejemplo»**
  (no por escena: decirlo en cada una lo convierte en ruido).
- **Solo funciones que existen en `main`.** Si una desaparece de la app, su escena se quita.
- **Sin calorías de entrenamiento en ninguna forma** (DEC-004): ni en la sesión, ni en el resumen, ni en
  un texto del tipo «quemas…». Las kcal que salen son las de la comida apuntada. En la sesión, el número
  grande es el volumen en kilos, como en la app.
- **Dos familias de curvas.** Lo que pasa *dentro* de la interfaz de la app usa los tiempos de
  `Movimiento.java` y sus curvas (`ENFATIZADA`, `ESTANDAR`; `REBOTE` se sustituye por `ENFATIZADA`).
  Lo que pasa *en la página* (reloj, luz, texto, cambio de hora) usa las de `DISENO.md` (`--salida`,
  `--recorrido`). Así la app se siente como la app y la página, como la página.
- **Dos tipos de movimiento.** Lo que es tiempo del día (el reloj, la luz, la cuenta atrás del descanso,
  el trazo de la gráfica) va **atado al scroll** (`scrub`). Lo que en la app es un toque (marcar una serie,
  el código leído, copiar) se **dispara** al cruzar un punto, dura lo que dura en la app y se deshace al
  volver hacia arriba.
- **Una cosa se mueve cada vez, y lo que se lee no se mueve mientras se lee** (`DISENO.md`).
- **Rangos de ancho.** Móvil, 320–599 px: versión corta, una columna, la interfaz a todo el ancho menos
  20 px por lado y **sin marco de móvil** (a 360 px un marco dejaría la interfaz a 300 y no se leería).
  Tableta, 600–1199 px: versión larga, marco de móvil centrado a 360 px de pantalla, texto encima.
  Escritorio, 1200 px o más: versión larga, texto a la izquierda y móvil fijo a la derecha, pantalla de
  390 × 844. Desde 1600 px el contenido se queda en su ancho máximo y solo crece el reloj.
- **La luz tiene que notarse.** Las opacidades de la primera versión (4–10 %) no se veían en un móvil.
  Ahora cada hora usa casi todo el margen hasta el tope de luminancia relativa **0,045** que fija
  `LuzDelDiaWebTest` (§5), medido en el punto más claro de su capa sobre el carbón `#0B0C0E`
  (luminancia 0,0037). Opacidad máxima que deja cada color sin pasar del tope, y la que se usa (con
  un margen de 0,01–0,02 para el redondeo del navegador):

  | Hora | Color | Opacidad límite | Se usa | Forma |
  |---|---|---|---|---|
  | 7:00 amanecer | `#FFB27A` | 0,26 | **0,24** | baja, desde la izquierda |
  | 14:00 mediodía | `#F2F2F3` | 0,205 | **0,18** | cenital, la más blanca |
  | 19:00 gimnasio | `#FF6A00` | 0,355 | **0,33** | lateral derecha, la más cálida |
  | 19:40 récord | `#E8B84B` | 0,27 | **0,25** | la de las 19:00 funde a esta |
  | 23:00 noche | `#56C2D6` | 0,285 | **0,14** | la más oscura, a propósito |

  Lo que distingue una hora de otra es el color, la forma y la posición de la capa, no solo la opacidad.
  **Criterio de aceptación.** Fase 1: en el vídeo del Xiaomi 17T se distingue a simple vista la luz de
  las 7:00 de la de las 19:00, y el paso al dorado del récord. Fase 3: las cuatro horas (7:00, mediodía,
  19:00 y 23:00) distinguibles entre sí en el mismo vídeo. Si no se ven, se cambia el color o la forma
  dentro del tope; el tope no se sube.
- **Alturas.** Se miden en `svh` (nunca `100vh`, que en el navegador de TikTok salta con la barra).
  Cada escena tiene una altura de scroll fija decidida en CSS (ver §5, «La versión sin JavaScript»).

Abreviaturas: **M** = versión móvil, **E** = escritorio (vale para tableta salvo que se diga).

### 7:00 · Entrada

- **Qué se ve.** El reloj, `07:00`, enorme, en Barlow Condensed 700 (`clamp(120px, 38vw, 320px)`), y
  debajo el titular (el único `h1`) y una línea. La acción «Entra en la beta», visible desde el primer
  pantallazo. La etiqueta «Un día de ejemplo».
- **Luz.** Amanecer: una luz baja desde la izquierda, en `#FFB27A` (`on-primary-container-dark`), que
  sube del 0 al 24 % de opacidad en su punto más claro (tabla de la luz, arriba).
- **Movimiento (CSS, sin esperar al JavaScript).** Cada cifra del reloj sube desde el 100 % de su alto
  dentro de su máscara, 650 ms con `--salida`, escalonadas 70 ms (la gramática de los titulares de
  GP-205). La luz entra en 1200 ms. El titular **no** parte de opacidad 0: entra con `clip-path` desde
  abajo, para que cuente como LCP desde el primer pintado. Todo acaba antes de 900 ms.
- **Al bajar.** El reloj grande se encoge hasta el **reloj de cabecera** (28 px, arriba a la izquierda,
  junto al logo), que acompaña el resto del día: es el motivo que une las horas. Escala de 1 a la del
  reloj pequeño con `--recorrido`, atada al scroll en los primeros 40 svh.
- **M.** Igual, con el reloj a 38vw. Cabe en 360 × 640 con la acción entera a la vista.
- **E.** El reloj ocupa la columna izquierda; a la derecha, la pantalla del móvil apagada (`#0B0C0E` con
  el canto), que se enciende con la G del arranque quieta (`img/logo.svg`) al terminar la entrada.

### Transición entre horas (vale para todas)

- El reloj de cabecera **rueda** de una hora a la siguiente: cada cifra que cambia sube un 100 % y entra
  la nueva desde abajo, 420 ms con `--recorrido`, disparado al cruzar el inicio de la escena. Los
  minutos que pasan por en medio no se enseñan: es un corte, no un cronómetro.
- La luz **funde** de la de una hora a la de la siguiente a lo largo de 60 svh (ver §5, contrastes).
- El título de la escena (`h2`, «8:00 · Desayuno») entra por líneas, 650 ms con `--salida`.

### 8:00 · Desayuno, buscándolo (solo E)

*Respuesta del propietario a la duda 1: a las 8:00 se **busca** el desayuno básico de la demo, solo en
escritorio; el escáner pasa a una escena nueva a las 17:00.*

- **Pantallas.** Añadir de Nutrición con la búsqueda (`activity_anadir_alimento.xml`): se escribe
  «café con leche» y sale el básico del catálogo propio, sin marca, con su ración; luego la tostada.
- **Dato.** El desayuno del lunes de ejemplo de la demo (`scripts/sembrar_demo.py`, `DESAYUNOS`), con
  sus kcal y macros de la API local.
- **Luz.** Mañana: la del amanecer, más alta y más blanca, camino de la del mediodía.
- **Movimiento.** Las letras entran en el campo como en `RELLENO` (150 ms cada una, sin teclado
  dibujado); la fila del resultado entra con `CASCADA`; al cruzar el punto, se pulsa y se apunta.
- **M.** No sale: en móvil la comida sale a las 14:00 y el escáner a las 17:00.

### 11:00 · El agua (solo E)

- **Pantalla.** La tarjeta del agua de Nutrición (`fragment_nutricion.xml`, `cardAgua`): litros, «de
  <objetivo>», la fila de vasos y el botón del vaso.
- **Dato.** La demo hoy lleva 1500 ml; el objetivo es el de la EFSA para un hombre (DEC-065).
- **Movimiento.** Un vaso más (momento 22, `VASO` 450 ms): el vaso se llena de abajo arriba y la cifra
  pasa de 1,5 a 1,75 l (`CIFRA`, 250 ms).
- **M.** No sale (versión corta).

### 14:00 · La comida, copiando la de ayer

- **Pantallas.** La comida del lunes vacía y el botón «Copiar ayer» de Nutrición
  (`btnCopiarAyerDiario`; sin Profi, respuesta a la duda 4), y después el anillo del día
  (`AnilloDia`, «quedan <n> kcal») con las tres barras de macros (`view_macro_barra.xml`).
- **Dato.** La comida del domingo de la demo (en el plan de la demo, por ejemplo, merluza con patata y
  judías) y sus kcal, proteína, hidratos y grasa de verdad, calculadas por la API local.
- **Luz.** Mediodía: cenital y neutra, `#F2F2F3` al 18 %, la más blanca del día.
- **Movimiento.** Al cruzar el punto, «Copiar la de ayer» se pulsa (`scale(0.97)`, 140 ms); la propuesta
  sale (momento 18, `ESTANDAR`, 250 ms) y los alimentos entran en cascada (`CASCADA`, 350 ms y 40 ms por
  fila, 12 px de recorrido). El anillo avanza de lo del desayuno a lo de desayuno y comida (`BARRA`,
  500 ms, `ESTANDAR`) y las barras de macros, igual. «Quedan» cambia de cifra con `CIFRA` (250 ms), sin
  contar número a número: contar dice «mira qué cifra», y la cifra es de ejemplo.
- **M.** Primero la comida vacía (60 svh), luego el anillo (60 svh). Escena de 140 svh.
- **E.** En el móvil fijo, la comida y el anillo en la misma pantalla, como en la app. 180 vh.

### 17:00 · La merienda, con el escáner (M y E)

- **Pantallas.** El escáner (`activity_escaner.xml`): el marco de lectura (`MarcoEscaner`), el aviso
  «Apunta al código de barras» y, al leer, la hoja con la píldora «Leído», el nombre del producto,
  «Producto envasado · …» y sus cuatro cifras (kcal, proteína, hidratos, grasa).
- **Dato.** Un **yogur envasado real** de `productos_off` en el entorno local, con sus valores y la
  ración del envase o la del producto (DEC-042). **Sin enseñar la marca**: la hoja lleva un nombre
  genérico («Yogur natural») y «Producto envasado» sin la marca, y el código de barras va dibujado en
  SVG (no la foto del envase). Si la demo no lo tiene, se añade a `scripts/sembrar_demo.py` como
  merienda del lunes de ejemplo.
- **Luz.** Tarde: el blanco del mediodía empieza a calentarse hacia el naranja de las 19:00.
- **Movimiento.** Un código de barras dibujado en SVG (sin foto, sin marca) entra en el marco; la línea
  de lectura lo barre una vez (`BARRIDO`, 2400 ms en la app; aquí atado al scroll en 30 svh). Al cruzar el
  punto, el marco se pone en `success` (`VERDE`, 120 ms) y la hoja sube desde abajo el 100 % de su alto,
  450 ms con `ENFATIZADA` (`HOJA`). Las cifras **no cuentan**: aparecen con la hoja.
- **M.** Pantalla del escáner a todo el ancho, alto 70 svh, y la hoja encima. Escena de 120 svh.
- **E.** En el móvil fijo. A la izquierda, el texto: buscar o escanear, en el mismo sitio. Escena de
  160 vh.

### 19:00 · Torso A (la escena estrella, fijada también en móvil)

- **Pantallas, en orden.**
  1. «Hoy toca» de Inicio (`cardHoyToca`): «Torso A», «unos <n> min · la última, <cuándo>» (los textos de
     `hoy_toca_unos_min` y `hoy_toca_ultima`, con los minutos que da la app a la demo), la lista de
     ejercicios y «Empezar».
  2. La sesión en vivo (`activity_sesion_en_vivo.xml`): la cabecera con tiempo, volumen y «0 de 21» series
     (Torso A son 7 ejercicios de 3 series en la plantilla);
     el primer ejercicio, press de banca (`item_envivo_ejercicio.xml`), con su pauta «3 × 6–10 ·
     Descanso 2 min», «Tu récord: …» en dorado y «La última vez, <cuándo>»; y sus tres filas de serie
     (`item_envivo_serie.xml`) con la columna «Anterior» y las pistas grises.
  3. El descanso (`view_descanso.xml`): el aro, `2:00` en cuenta atrás, el «Siguiente: …» de la app
     (§0, 13) y −15 s, +15 s, Saltar.
- **Dato.** Los kilos y repeticiones de la última sesión de Torso A de la demo (la del lunes anterior),
  que son la columna «Anterior» y las pistas. En la demo local, sembrada el 23-11-2026 (un lunes, que es
  el día de ejemplo), esa sesión es la del 16-11: press de banca 80 × 10, 80 × 10 y 80 × 9, y esa es
  también su mejor marca (80 kg × 10: la demo sube los kilos cada semana, así que la última es la más
  pesada). Las series de hoy se fijan con la regla del récord (§0, 12) para que el récord caiga en la 3
  y no antes:
  - **Series 1 y 2: 80 × 10.** Igualan la mejor marca: con `supera`, igualar no es récord.
  - **Serie 3: 82,5 × 8.** Más pesada que 80 kg: es récord aunque tenga menos repeticiones, porque la
    marca con peso se mide primero por los kilos. 82,5 es el escalón de los básicos de la demo
    (`KILOS["BASICO"]`, +2,5 kg) y 8, el centro de la pauta 6–10. «Tu mejor marca era 80 kg × 10».
  - Antes: «Tu récord: 80 kg × 10». Después: «Tu récord: 82,5 kg × 8, de hoy» (`envivo_record_tu_hoy`).
  Las cifras viven en `src/datos/dia-ejemplo.json`, con el comando y la fecha con que se sacaron, y la
  regla aplicada; un test comprueba que la serie 3 supera la marca y que la 1 y la 2 no. Duración,
  pauta y descanso, los de la plantilla.
- **Luz.** Gimnasio por la tarde: naranja `#FF6A00`, lateral, 33 %, la más cálida del día.
- **Fijado (pin).** La pantalla del móvil se queda fija y el scroll recorre la sesión. En M es la única
  escena fijada: la interfaz ocupa el ancho y el texto aparece en una sola línea encima, que cambia
  con cada paso (sin tapar la interfaz).
- **Coreografía, paso a paso (los % son del recorrido de la escena).**
  - 0–12 %: «Hoy toca» → «Empezar» se pulsa; la sesión sube 40 px y entra (`SESION_SUBE_DP`, 350 ms,
    `ENFATIZADA`).
  - 12–30 %: serie 1. En el campo de kg la pista (los kilos de la vez anterior) deja sitio a los de hoy,
    cifra a cifra (`RELLENO`, 150 ms cada una); reps, igual. El ✓ se rellena de `primary` (`CHECK`,
    260 ms) y la fila se marca (momento 24). La cabecera suma volumen (`CIFRA`, 250 ms) y «1 de 21».
  - 30–55 %: el descanso sube desde abajo (momento 26, `ENFATIZADA`, 450 ms) y el aro se vacía **atado al
    scroll** de `2:00` a `0:00`; los segundos se ven pasar, lineal (el aro va lineal en la app). A mitad,
    «+15 s» se pulsa y el aro recupera ese trozo: es la forma de enseñar los botones sin explicarlos.
  - 55–65 %: el aviso de fin («¡A por la serie 2!») baja y se va (`ESTANDAR`, 250 ms); serie 2 igual que
    la 1, más corta.
  - 65–100 %: lo sigue la escena de las 19:40, dentro del mismo pin.
- **Altura.** M: 320 svh (con las 19:40). E: 420 vh.

### 19:40 · El récord

- **Pantalla.** La misma sesión, serie 3 del press de banca.
- **Dato.** La serie 3 de hoy, **82,5 kg × 8**, que supera la mejor marca guardada de la demo,
  **80 kg × 10**, por los kilos (`LogicaRecordVivo.supera`: con peso manda el peso; a igual peso, las
  repeticiones). Es la primera serie de hoy que la supera, así que es la que celebra
  `LogicaRecordVivo.alMarcar` (una vez por ejercicio y sesión). Si la demo cambia y deja de dar este
  récord, se recalcula con la misma regla, nunca con una cifra a mano.
- **Luz.** Funde del naranja al dorado `#E8B84B`, 25 %, solo mientras dura el récord.
- **Movimiento.** Al marcar la serie 3: la fila pasa a `gold-row` (`ORO`, 900 ms); el aviso de récord
  entra (`TROFEO`, 450 ms, `ENFATIZADA`) con el trofeo de 0,5 a 1; seis chispas de 6 px en `gold` salen
  del trofeo y se apagan (`CHISPAS`, 700 ms; solo E). «Tu récord: …» cambia a «Tu récord: …, de hoy».
  El reloj de cabecera se tiñe de dorado durante el aviso, y vuelve.
- **M.** Sin chispas: el aviso y la fila dorada bastan, y son la parte barata.

### 20:10 · El resumen del entreno (solo E)

- **Pantalla.** El resumen (`activity_resumen_sesion.xml`): los kilos movidos frente a la sesión
  anterior de la misma rutina (DEC-055) y las series por ejercicio.
- **Movimiento.** Las barras de comparación crecen (`BARRA`, 500 ms). Nada de kcal (DEC-004).
- **M.** No sale.

### 23:00 · La gráfica de peso y la semana por zonas

- **Pantallas.** «Tu cuerpo» de Progreso (`cardCuerpo`: el peso de hoy, el cambio y la gráfica) y «Tu
  semana» de Inicio (`cardMusculos`: «<n> de 6 zonas», la silueta y una barra por zona con las series de
  la semana).
- **Dato.** Las mediciones de la demo (una cada 3 días: 78,6 → 76,9 kg en 22 días) y las series por zona
  de la semana del lunes de ejemplo, de la API local.
- **Luz.** Noche: la más oscura; un azul `#56C2D6` (`agua-dark`) al 14 %.
- **Movimiento.** La línea de la gráfica se dibuja de izquierda a derecha (`stroke-dashoffset`, atada al
  scroll en 50 svh); los puntos aparecen al pasar la línea por ellos (`PUNTO`, 450 ms). Luego las zonas:
  la silueta se colorea zona a zona y cada barra crece (`BARRA`, 500 ms, 100 ms de escalón, `FILA`).
- **M.** Primero la gráfica (80 svh), luego la semana (80 svh).
- **E.** Las dos tarjetas una al lado de la otra dentro del móvil no caben: el móvil enseña «Tu cuerpo» y,
  a la izquierda, la tarjeta «Tu semana» sale fuera del móvil, a escala 1, como una pieza suelta. Es la
  única vez que una pieza sale del móvil, y por eso se nota.

### Cierre · «Mañana, otra vez»

- El reloj de cabecera vuelve a crecer hasta el tamaño de la entrada y marca `07:00` otra vez (rueda las
  cuatro cifras, 420 ms). La luz vuelve al carbón sin luz; el único color es el naranja de la acción.
- Debajo: «Entra en la beta» (enlace a `/beta`), los tres pasos para entrar (el contenido de hoy, en el
  mundo nuevo), las preguntas (las aprobadas, `PortadaWebTest`) y el pie legal.
- Nada de esto se anima salvo la entrada de la acción (650 ms, `--salida`). Los pasos, las preguntas y el
  pie, quietos (`DISENO.md`, «Qué no se anima»).

### Versión corta frente a larga

| Hora | M (320–599) | E (≥ 600) |
|---|---|---|
| 7:00 entrada | sí | sí |
| 8:00 buscar el desayuno | no | sí |
| 11:00 agua | no | sí |
| 14:00 copiar la comida | sí | sí |
| 17:00 escanear el yogur | sí | sí |
| 19:00 Torso A + 19:40 récord | sí, fijada | sí, fijada |
| 20:10 resumen | no | sí |
| 23:00 peso y zonas | sí | sí |
| cierre | sí | sí |

Las escenas que no salen en M **no están en el HTML de M ocultas con CSS**: están en el HTML (la página
es una), pero en M ocupan una tarjeta quieta de una línea, sin interfaz animada, para que el lector de
pantalla y quien no tiene JavaScript reciban lo mismo en todos los anchos.

---

## 2. Inventario de componentes que se rehacen en DOM

Cada componente se rehace en HTML y CSS a partir de su layout, con los tokens de
`app/GymProFit/DESIGN.md` copiados como propiedades CSS (los mismos valores, como ya hace `portada.css`
con `colors.xml`). La interfaz rehecha es **una imagen hecha de HTML**: no es interactiva, va en un
`<figure>` con `aria-hidden="true"` en la interfaz y una descripción en texto en `<figcaption>` (o
`sr-only`) que dice lo que se ve. Ningún botón falso recibe el foco.

Medidas: 1 dp = 1 px CSS a 360 px de pantalla.

| # | Componente | Layout de origen | Piezas | Tokens | Escena |
|---|---|---|---|---|---|
| 1 | Tarjeta «Hoy toca» | `fragment_inicio.xml` (`cardHoyToca`), filas de `item_hoy_ejercicio.xml` | rótulo, nombre, «unos <n> min · la última…», silueta pequeña, lista de ejercicios, botón expresivo | `hero` `#3A2110`, `on-hero`, `on-hero-variant`; radio `protagonista` 28; `eyebrow` 13/700/+0,08 em; `display-large` 34; `button-primary` 48 de alto, radio 26 (12 al pulsar) | 19:00 |
| 2 | Escáner y su hoja | `activity_escaner.xml` (`marcoEscaner`, `hojaEscaner`, `pildoraEncontrado`, `cifrasHoja`) | marco, línea de lectura, aviso, hoja con píldora, nombre, subtítulo, cuatro cifras, dos botones | `gp_escaner_texto`, `success`, macros; radio 28 (hoja); `headline` 20/600; `bg_cifra_progreso` | 8:00 |
| 3 | Anillo del día | `fragment_nutricion.xml` (`cardDia`, `AnilloDia`, `grupoQuedan`) | anillo, kcal, objetivo, «quedan» | `primary`, `meter-track`; `number` 44/700; `caption` 14; tarjeta radio 24 | 14:00 |
| 4 | Barra de macro | `view_macro_barra.xml` (×3) | nombre, g de g, barra | `macro-proteinas` `#6BA6F5`, `macro-carbos` `#E8A94B`, `macro-grasas` `#B295E8`; `meter-track` `#353A43`; `body` 16, `caption` 14 | 14:00 |
| 5 | Comida con «Copiar la de ayer» | `activity_comida.xml` + `view_profi_copiar.xml`; filas de alimento | propuesta, botón, filas que entran | `surface`, `primary`, `fila` 16; radio 24; botón píldora | 14:00 |
| 6 | Cabecera de la sesión en vivo | `activity_sesion_en_vivo.xml` (`Cabecera.Sesion`) | tiempo, volumen, «n de m», barra de progreso | `number-hero` 56; `label` 14/+0,12 em; `primary-container`; `BarraProgreso` | 19:00 |
| 7 | Ejercicio en vivo | `item_envivo_ejercicio.xml` | nombre, pauta, «Tu récord», «La última vez», rótulos de columna, aviso de récord | `display` 26; `caption` 14; `gold-text` `#E8B84B`; `gold-container` `#3A2E12`; trofeo `ic_ms_trophy_fill` | 19:00, 19:40 |
| 8 | Fila de serie | `item_envivo_serie.xml` | número, «Anterior», campo kg, campo reps (con pista), ✓ | `Number`; `bg_envivo_campo`; `colorOnSurfaceVariant` (pista); `primary` (✓); `gold-row` `#2A2414` (récord); columna de 48 | 19:00, 19:40 |
| 9 | Descanso | `view_descanso.xml` | aro (`CircularProgressIndicator`), cuenta atrás, «Después: …», −15 s, +15 s, Saltar, aviso de fin | `primary`; `meter-track`; cifra 46 (`envivo_descanso_numero`); `headline`, `label`; `BotonPildora` | 19:00 |
| 10 | Resumen del entreno | `activity_resumen_sesion.xml` | kilos movidos, comparación con la anterior, series por ejercicio | `number-hero`; `success`/`text-secondary` para la comparación | 20:10 (E) |
| 11 | Agua | `fragment_nutricion.xml` (`cardAgua`) | litros, «de …», vasos, botón | `agua-dark` `#56C2D6`, `agua-container-dark` `#10323A` | 11:00 (E) |
| 12 | Gráfica de peso | `fragment_progreso.xml` (`cardCuerpo`, `chartPesoProgreso`) | peso, cambio, línea y puntos (hoy MPAndroidChart; aquí SVG) | `primary` en la línea; `divider` en la rejilla; `display` 26; tarjeta radio 24 | 23:00 |
| 13 | Tu semana por zonas | `fragment_inicio.xml` (`cardMusculos`), filas de `item_zona_semana.xml` | «n de 6 zonas», silueta, barra por zona | `primary-container` (hecha), `meter-track`; `Apoyo`; silueta desde los trazados de `SiluetaMuscularView` | 23:00 |
| 14 | Primitivas | `values/themes.xml`, `estilos_sistema.xml` | tarjeta (radio 24, sin borde en oscuro), tarjeta protagonista (28), píldora, botón redondo 48, `Number`/`Display`/`Label`, iconos | `surface` `#1B1E24`, `surface-2` `#252A31`, `on-surface` `#F2F2F3`, `text-secondary` `#A1A9B6`; espaciado 4–36; margen 20 | todas |

Notas del inventario:

- **Iconos.** Los `ic_ms_*` son Material Symbols Rounded (Apache 2.0) en vector de Android. Se convierten
  a un sprite SVG propio con solo los que se usan (unos 15), con su licencia al lado.
- **Silueta muscular.** `SiluetaMuscularView` dibuja trazados; se pasan a SVG con un script en la fase 2
  (como `trazar-g-arranque.py` hizo con la G), sin redibujar a mano.
- **Fuentes.** Las mismas seis de `web/fuentes/`. La interfaz de la app usa Barlow 400/500/600 y Barlow
  Condensed 500/600/700: no hace falta ninguna más. Se precargan dos (Condensed 700 para el reloj y el
  titular, Barlow 400 para el texto).
- **Cifras.** Formato de `es-ES`: coma decimal, espacio duro antes de la unidad (`62,5 kg`), como la app.
- **Fuente de verdad de los tokens.** Un `src/estilos/tokens-app.css` con los valores de `DESIGN.md`, y un
  test (§3) que lo cruza con `values/colors.xml`, `values-night/colors.xml` y `values/dimens.xml`, para que
  la web no se separe de la app en silencio.

---

## 3. Stack, despliegue y tests

### Propuesta

- **Astro con salida estática** (`output: 'static'`), la última versión estable al empezar la fase 1,
  fijada en `package-lock.json`. Aporta componentes para la interfaz de la app (cada pieza del
  inventario, una vez, usada en varias escenas y en la 404), HTML sin JavaScript por defecto y los
  recursos con el hash en el nombre. No aporta ni se usa: islas de React u otro framework, adaptador de
  servidor, `prefetch`, el `ClientRouter` de Astro (sus transiciones de vista con JavaScript), la barra
  de herramientas de desarrollo.
- **`build.format: 'file'` y `trailingSlash: 'never'`**: `src/pages/privacidad.astro` sale como
  `privacidad.html`, que es lo que hay hoy, y Pages sigue sirviendo `/privacidad`. Con el formato por
  defecto (`directory`) saldría `privacidad/index.html` y Pages redirigiría a `/privacidad/`: cambiaría
  una URL que Google Play tiene declarada. Un test lo fija sobre la salida (abajo).
- **La telemetría de Astro, apagada** (`ASTRO_TELEMETRY_DISABLED=1` en los scripts de `package.json` y en
  el CI): Astro la trae encendida, y el repositorio no manda datos a nadie sin pedirlo, como con las
  skills.
- **GSAP con ScrollTrigger** (y SplitText solo si en la fase 1 se demuestra que hace falta; hoy no: los
  titulares van partidos en el HTML). **Lenis, solo con ratón** (`(hover: hover) and (pointer: fine)`):
  en táctil no se descarga (§5).
- **Cómo se cargan.** El HTML lleva un único módulo inicial pequeño (objetivo ≤ 6 KB gzip; tope duro
  50 KB): decide si hay movimiento, vigila con `IntersectionObserver` qué escena está a un viewport de
  entrar y, entonces, hace `import()` de GSAP, ScrollTrigger y del módulo de esa escena. Primero tras el
  `load` y la entrada del reloj (como hoy, para no competir con el LCP) o al primer gesto de scroll, lo
  que llegue antes. Con `prefers-reduced-motion: reduce` el módulo inicial sale antes de cualquier
  `import()`.
- **Las librerías llegan por npm**, no copiadas a mano en `js/vendor/`: Vite las mete en trozos propios
  dentro de `/_astro/`, con el hash en el nombre, servidos desde nuestro dominio. Su licencia va con
  ellas: los comentarios legales (`/*!`) se conservan en los trozos, y un test lo comprueba (abajo).

### Para fases posteriores (no en la fase 1)

- **Transición entre páginas con View Transitions entre documentos.** Solo CSS: `@view-transition {
  navigation: auto; }` en las hojas de las páginas del mundo nuevo, sin JavaScript y sin el
  `ClientRouter` de Astro. El elemento compartido es el **reloj** (`view-transition-name: reloj` en el
  reloj de cabecera de cada página), así que al ir de la portada a `/beta` o a una legal el reloj se
  queda y el resto funde. Con `prefers-reduced-motion: reduce` no hay animación: la regla va dentro de
  `@media (prefers-reduced-motion: no-preference)`. Un navegador que no la entiende navega como
  siempre. Fase 4, con las páginas secundarias.
- **Cursor propio en escritorio**, como capa extra: solo con `(hover: hover) and (pointer: fine)` y
  movimiento permitido, un punto que sigue al ratón por encima de la página (`pointer-events: none`,
  `transform` y nada más). El cursor del sistema **se queda** en los enlaces, los botones y los campos
  (en ellos el propio no se pinta), y nunca se oculta el del sistema en el resto si el propio no ha
  cargado. Fase 3 o 5; entra en el presupuesto de JS de escritorio.

### Estructura propuesta de `web/`

```
web/
  package.json · package-lock.json · astro.config.mjs
  src/pages/        index.astro, privacidad.astro, terminos.astro, eliminar-cuenta.astro,
                    beta.astro, beta/{revisa,dentro,entrar}.astro, 404.astro
  src/componentes/  app/ (el inventario), dia/ (escenas y reloj), comunes/ (cabecera, pie)
  src/estilos/      tokens-app.css, tokens-web.css, una hoja por página
  src/movimiento/   inicio.js (el módulo inicial), escena-*.js, depurar.js (?debug=motion)
  src/datos/        dia-ejemplo.json
  public/           _headers, fuentes/, img/ (logo.svg, favicon.png, apple-touch-icon.png,
                    logo-correo.png: rutas que no cambian)
  dist/             la salida (no se versiona)
  README.md · DISENO.md · PRODUCT.md · PLAN-UN-DIA.md · .impeccable/   (fuera de dist, fuera del zip)
```

`node_modules/` y `dist/` van al `.gitignore`.

### Qué cambia y qué se conserva

| Pieza | Cambia | Se conserva |
|---|---|---|
| Despliegue | El zip es el **contenido de `web/dist/`** tras `npm ci && npm run build`. La documentación ya no puede colarse: no está en `dist`. | Subida directa al proyecto `gymprofit` de Cloudflare Pages, a mano, sin conectar el repositorio. El zip al escritorio con el formato de `README.md`. |
| `scripts/versionar-hojas-web.py` | Se retira en la fase 4, cuando la última página pase a Astro: el hash va en el nombre del fichero (`/_astro/index.AbC123.css`). Mientras tanto (fases 1–3) las páginas que siguen siendo HTML a mano viven en `public/` tal cual y el script versiona `public/` en vez de `web/`. | La idea: la URL cambia solo cuando cambia el contenido. |
| `_headers` | Vive en `public/_headers`. Regla nueva `/_astro/*` con un año e `immutable`; fuera las de `/estilo.css`, `/beta.css`, `/portada.css` y `/js/*` cuando dejen de existir. Si las fuentes pasan a importarse desde el CSS, salen con hash y pueden ir a `immutable`. | El HTML con `no-cache`, una regla por página, ninguna regla `/*`. |
| `scripts/servir-web.py` | Sirve `web/dist/`; añade **brotli** (el módulo `brotli` de `fonttools` ya se usa para las fuentes) además de gzip, para medir como sirve Pages. | Rutas limpias, la 404 con estado 404, las cabeceras de `_headers`. |
| `scripts/reservas-capturas-web.py` y `img/capturas/` | Se retiran: ya no hay capturas. | — |
| `scripts/texturas-web.py`, `tela.webp`, `tyvek.webp` | La tela y el Tyvek son del dorsal: se retiran en la fase 4 si las páginas secundarias dejan el dorsal. | — |
| CI | Job **`web`** nuevo, que corre si cambia `web/` (o el workflow): Node fijado por versión, `npm ci`, `npm run build` y los `*WebTest` contra `dist/`. Arregla el hueco del punto 9 de §0. | Los tests siguen en Java, en el mismo sitio, sin infraestructura de tests nueva. |

### Los tests de la web

Todos buscan hoy la carpeta `web` subiendo desde el directorio de trabajo y leen `web/*.html`. Pasan a
leer **`web/dist/`** a través de un ayudante común que falla con un mensaje claro si `dist` no existe
(«ejecuta `npm run build` en web/»), y las hojas de estilo de `web/src/estilos/`.

| Test | Qué pasa |
|---|---|
| `HojasVersionadasWebTest` | Se reescribe: cada CSS y JS que pide una página está en `/_astro/` con hash en el nombre y existe en `dist`; `_headers` da `immutable` a `/_astro/*` y `no-cache` a cada página. Mientras haya páginas en `public/`, sigue comprobando su `?v=`. |
| `TitularesWebTest` | Mismas reglas (todo tamaño fluido con tope en px, `--k` en titulares acotados), leyendo `src/estilos/`. Añade el reloj. |
| `MovimientoWebTest` | Mismas reglas (nada se mueve sin parar, ninguna curva rebota, toda animación CSS dentro de `no-preference`), y además mira los `ease` de los módulos de `src/movimiento/`: ni `back`, ni `elastic`, ni `bounce`. |
| `LogoUnicoWebTest` | Igual, sobre `dist/*.html` y `public/img/`. `logo-correo.png` sigue permitido (es de los correos). |
| `PortadaWebTest` | Se reescribe: orden de las escenas por hora, un solo `h1`, la etiqueta «Un día de ejemplo», cero recursos de fuera, solo scripts propios, presupuesto (inicial y diferido, medidos en gzip sobre `dist`), licencias en los trozos, preguntas aprobadas, pie, y que ninguna cifra de una escena falte en `dia-ejemplo.json`. Se quitan las comprobaciones de capturas y reservas. |
| `BetaWebTest` | Mismas comprobaciones sobre `dist/` (el formulario sigue siendo el mismo HTML). «La portada invita a la beta» se adapta al cierre nuevo. |
| `PaginaNoEncontradaWebTest` | En la fase 4: la 404 en el mundo nuevo («la hora que no existe»), sin el JavaScript de la portada, con ruta absoluta en todo lo que carga. |
| `EliminarCuentaWebTest` | Igual, sobre `dist/eliminar-cuenta.html`. |
| Nuevos | `RutasWebTest`: cada URL de §6 existe en `dist` como `<ruta>.html` (no `<ruta>/index.html`), y existe `dist/404.html`. `TokensAppWebTest`: `tokens-app.css` = `colors.xml` y `dimens.xml`. `LuzDelDiaWebTest`: el contraste de §5 con la luz de cada hora. |

---

## 4. Presupuesto y cómo se mide

### Cifras

| Medida | Tope | Dónde |
|---|---|---|
| Lighthouse móvil, rendimiento | ≥ 90 | mediana de 5 |
| Lighthouse móvil, accesibilidad | 100 | mediana de 5 |
| CLS | < 0,05 | Lighthouse y panel |
| LCP en 4G | ≤ 2,5 s | Lighthouse (4G lento simulado) y Xiaomi 17T con 4G real |
| JS inicial | ≤ 50 KB gzip (objetivo ≤ 6 KB) | `PortadaWebTest`, sobre `dist` |
| JS diferido, móvil | ≤ 60 KB gzip (lo que hoy permite DEC-053) | `PortadaWebTest` |
| JS diferido, escritorio | ≤ 70 KB gzip (con Lenis) | `PortadaWebTest` |
| Fotogramas en las escenas | ≥ 50 fps (mediana) en un Xiaomi 17T | panel y DevTools remotas |
| INP | < 200 ms | panel (la página casi no tiene interacción: enlaces y preguntas) |
| Tareas largas durante el scroll | ninguna > 100 ms | panel y DevTools |

Hoy GSAP 3.15.0 pesa 28,3 KB gzip, ScrollTrigger 18,0 KB y Lenis 5,9 KB (medido el 25-11-2026 con
`gzip -9` sobre `web/js/vendor/`): 46,3 KB sin Lenis. Quedan unos 13 KB para los módulos de escena en
móvil. Si no caben, se quita movimiento, no se sube el tope (DEC-053).

### El panel `?debug=motion`

- Solo existe si la URL lleva `?debug=motion`: el módulo inicial hace `import()` de `depurar.js` en ese
  caso y en ningún otro. No pesa en la carga normal.
- Enseña, en una caja fija en una esquina: fotogramas por segundo (mediana y percentil 5 de los
  intervalos de `requestAnimationFrame`) y porcentaje de fotogramas de más de 20 ms; tareas largas
  (`PerformanceObserver` de `longtask`: cuántas y la más larga; donde el navegador no lo da, «no
  disponible»); LCP, CLS e INP aproximado (`event` timing); la escena y la hora activas; los bytes de JS
  transferidos (`transferSize` del Resource Timing); si hay movimiento reducido; y el agente de usuario
  abreviado (para saber si es el navegador de TikTok o de Instagram).
- Botón «Copiar» que deja los números en el portapapeles para pegarlos en el informe.
- No envía nada a ningún sitio.

### Procedimiento

1. **Mismo servidor para las dos portadas**: `scripts/servir-web.py` con brotli, sirviendo la portada
   actual (un **clon aparte** de `main`, no un worktree, en una carpeta fuera del repositorio) y la
   nueva (`web/dist/`), en el mismo equipo y puerto, una después de otra. **Solo en local hasta la
   fase 6** (respuesta a la duda 5): nada se crea en Cloudflare para medir.
2. **Lighthouse por línea de órdenes**, preajuste móvil (4G lento simulado, CPU ×4), caché vacía, 5
   pasadas por portada, se apunta la **mediana** de cada cifra y las 5 en el informe.
3. **Xiaomi 17T** con Chrome y con el navegador integrado de TikTok y de Instagram: abrir la URL de la
   red local (`servir-web.py --red`), recorrer el día dos veces con `?debug=motion` y copiar el panel.
   En Chrome, además, DevTools remotas por `adb` con una grabación de Rendimiento de cada escena, para
   localizar lo que baje de 50 fps.
4. **Producción, después del despliegue** (fase 6): las mismas 5 pasadas contra `https://gymprofit.app`,
   para confirmar que el servidor local no engañaba.

---

## 5. Riesgos y cómo se cubren

| Riesgo | Cómo se cubre |
|---|---|
| **Navegador integrado de TikTok e Instagram** (en Android, un WebView de Chromium que puede ir atrasado; la barra superior cambia el alto al desplazar) | Nada de WebGL ni canvas: todo es DOM, CSS y SVG. Alturas en `svh`/`lvh`, nunca `100vh`. ScrollTrigger con `ignoreMobileResize`. Se prueba en los dos navegadores en la fase 1, no al final. |
| **`prefers-reduced-motion: reduce`** | Versión quieta completa: cada hora en su estado final, el reloj quieto con la hora de cada sección, la luz fija de cada hora como fondo de su sección. El módulo inicial sale antes de cualquier `import()`: ni GSAP ni Lenis se descargan. Las escenas no son altas (ver abajo), así que no hay que recorrer pantallas vacías. |
| **Sin JavaScript** | Todo visible y usable: es el mismo HTML. Lo que se mueve parte de su estado final (se anima siempre *hacia* él). Los enlaces y el formulario de `/beta` no dependen de nada. |
| **Escenas altas sin JS o con movimiento reducido** | La altura extra que necesita una escena fijada (320 svh, 420 vh) la pone el **CSS**, solo dentro de `@media (prefers-reduced-motion: no-preference) and (scripting: enabled)`. Así está decidida antes de que corra ningún script (sin CLS, y los anclajes y el scroll restaurado caen donde deben, como pide `README.md`), y sin JS la escena mide lo que su contenido. En un navegador sin `scripting` (Chromium < 120) la escena no se fija y se anima en su sitio. |
| **Scroll nativo en táctil** | Nada secuestrado: ni Lenis (no se descarga en táctil), ni `normalizeScroll` de ScrollTrigger, ni `overflow` en un contenedor interno; se desplaza siempre el documento. El fijado es `position: sticky` donde se pueda y el `pin` de ScrollTrigger solo donde sticky no llegue. Nada intercepta `touchmove`. |
| **Contraste AA con la luz que cambia** | La luz vive en una capa fija detrás del contenido y **nunca** detrás de texto suelto sin tarjeta salvo el reloj y los títulos. Regla: en ningún punto, a ninguna hora, el fondo pasa de una luminancia relativa de **0,045**; con eso `text-secondary` `#A1A9B6` sigue por encima de 4,5:1 y `on-surface` muy por encima. El naranja `#FF6A00` y el dorado solo van en texto grande, cifras o gráficos (3:1). `LuzDelDiaWebTest` calcula el peor punto de cada hora a partir de los tokens de luz y falla si baja. |
| **Coste de pintar la luz** | La luz de cada hora es una capa propia, precompuesta; cambiar de hora es fundir **opacidades** (compositor), no recalcular un degradado en cada fotograma. |
| **Tareas largas al arrancar las escenas** | Cada escena crea sus ScrollTriggers cuando está a un viewport de entrar y se importa por separado; `will-change` solo mientras la escena está activa. |
| **Cifras de ejemplo leídas como promesa** | Una etiqueta «Un día de ejemplo», cifras sacadas de la demo y fijadas en `dia-ejemplo.json`, ninguna cifra que cuente número a número, y un test que no deja escribir una cifra fuera de ese fichero. |
| **Accesibilidad de la interfaz rehecha** | Es una imagen hecha de HTML: `aria-hidden` en la interfaz y una descripción en texto por escena. Nada de `aria-live`: el lector de pantalla lee el texto de la página a su ritmo. Cada hora es una `<section>` con su `h2`. El reloj de cabecera es decorativo. |
| **Una dependencia de build** (Node en el CI y en la máquina) | Node fijado por versión en el CI, `npm ci` con lockfile, ninguna dependencia de más que `astro`, `gsap` y `lenis`. Si un día Astro sobra, la salida es HTML y CSS que se puede congelar en `public/`. |

---

## 6. Qué no cambia

- **Las URL** `/`, `/privacidad`, `/terminos`, `/eliminar-cuenta` (requisito de Google Play), `/beta`,
  `/beta/revisa`, `/beta/dentro`, `/beta/entrar` (la de los correos de «beta abierta», GP-202) y la 404
  (`404.html`, servida con estado 404). Con `build.format: 'file'`, y `RutasWebTest` lo fija.
- **El formulario de `/beta`**, enviado por POST normal a Brevo sin JavaScript, con sus campos (DEC-050,
  `BetaWebTest`). Su página no gana ningún script. La paleta de los vídeos de la beta se queda en sus
  páginas.
- **El bloque `▼ APERTURA DE LA BETA`** de `/beta/entrar` y el procedimiento de apertura (DEC-052).
- **Cero peticiones a terceros al cargar**: fuentes, librerías e iconos desde el dominio.
- **El logo único** (DEC-057): `img/logo.svg`, el favicon y el `apple-touch-icon.png` con la misma ruta;
  `img/logo-correo.png` también, porque lo piden los correos.
- **Las fuentes**: los mismos seis `.woff2` recortados.
- **El idioma.** La web está **en español** (todas las páginas con `lang="es"`, y el formulario manda
  `locale=es` a Brevo). Ninguna decisión vigente obliga a ponerla en inglés: DEC-020 («ES y EN siempre»)
  habla de las cadenas de la app, que van en recursos, y la web no tiene recursos. Lo que sí sale de
  DEC-020 es que **las pantallas de la app que se rehacen enseñan sus textos en español**, los de
  `values/strings.xml`, tal cual. Respuesta a la duda 3: sin inglés por ahora, con la estructura de
  Astro preparada para añadirlo (los textos de la página en un solo sitio por página, nada de rutas que
  impidan un `/en/` después).

---

## 7. Fases, con parada y revisión en cada una

Cada fase acaba con un informe al propietario y **no empieza la siguiente sin su visto bueno**.

### Fase 1 · Prueba de concepto: la entrada y las 19:00, medidas en el móvil

- Antes de nada: el job `web` del CI y el ayudante de los tests sobre `dist` (con los tests de hoy aún en
  verde contra las páginas copiadas a `public/`). Los `*WebTest` ya corren en el CI cuando una PR solo
  toca `web/` desde la rama `ci-tests-web` (job `web-test`); aquí ese job pasa a construir `dist` antes.
- Astro en `web/` (`build.format: 'file'`, `trailingSlash: 'never'`, telemetría apagada), las páginas
  actuales copiadas tal cual a `public/` (cero riesgo para lo que hay). `RutasWebTest` sobre `dist`.
- **La prueba vive en `/un-dia`** (`noindex`), no en `/`: la portada de hoy se queda en `public/` sin
  tocar hasta que se acepten la prueba y DEC-069, y entonces `index.astro` la sustituye (fase 3).
- La entrada (7:00: el reloj, el titular como LCP, la acción visible en el primer pantallazo y la
  etiqueta «Un día de ejemplo») y la escena fijada de Torso A con el récord, en M y en E, con los
  componentes 1, 6, 7, 8 y 9.
- `dia-ejemplo.json` sacado de la demo local con el comando y la fecha; ninguna cifra a mano en los
  componentes.
- Módulo inicial ≤ 6 KB gzip como objetivo; GSAP y ScrollTrigger con `import()` al acercarse la escena;
  ScrollTrigger con `ignoreMobileResize`; `sticky` antes que `pin` donde se pueda.
- El panel `?debug=motion`, con el botón de copiar.
- La versión con movimiento reducido y la versión sin JS de las dos escenas, completas.
- **Parada:** Lighthouse (mediana de 5, frente a la portada actual en el mismo servidor) y el panel en
  el Xiaomi 17T, en Chrome, TikTok e Instagram. Si no se llega a 50 fps o a 90 de rendimiento, se cambia
  la dirección aquí, no en la fase 5.

### Fase 2 · Sistema de componentes de la app en DOM

- Los 14 del inventario, cada uno en sus estados (vacío, a medias, completo; el récord), en una página de
  muestras fuera del zip (`/muestras`, `noindex`, solo en desarrollo).
- `tokens-app.css` y `TokensAppWebTest`; el sprite de iconos con su licencia; la silueta trazada.
- `DISENO.md` reescrito para «Un día» (la dirección de GP-205 se queda en el historial).
- **Parada:** comparación lado a lado con capturas de la app real en la demo, a 360 px.

### Fase 3 · El resto del día

- 8:00, 11:00, 14:00, 20:10, 23:00 y el cierre; las transiciones entre horas; la luz de cada hora y
  `LuzDelDiaWebTest`; `PortadaWebTest` reescrito.
- **Parada:** recorrido completo en M y en E, y otra medida en el móvil.

### Fase 4 · Páginas secundarias y 404 en el mundo nuevo

- Privacidad, términos, eliminar cuenta, la beta y sus tres páginas, y la 404, pasadas a `.astro` con el
  mundo nuevo (sin escenas ni GSAP: solo la tipografía, la luz quieta y el reloj como marca). La beta
  conserva su formulario sin JS y su paleta.
- Fuera `versionar-hojas-web.py`, las capturas, sus reservas y las texturas del dorsal, si ya no las usa
  nadie; `README.md` al día.
- **Parada:** revisión de las nueve URL y de los tests de la beta.

### Fase 5 · Calidad

- Accesibilidad (teclado, lector de pantalla con TalkBack, contraste medido, 200 % de zoom, `forced-colors`),
  rendimiento (las cifras de §4, la mediana de 5) y la versión quieta y sin JS revisadas de arriba abajo.
- Revisor de UI en las combinaciones que apliquen: la portada solo en carbón; las legales y la beta en
  claro y oscuro (duda 2); y los anchos 320, 360, 768, 1280 y 1920. Tras arreglar, se vuelve a pasar como dice CLAUDE.md.
- **Parada:** informe con todas las medidas.

### Fase 6 · Despliegue

- Zip del contenido de `dist/` al escritorio, subido por el propietario (o con su permiso) a Pages.
- Comprobación en producción: las nueve URL, `curl -I` de las cabeceras, la 404 con estado 404, el
  formulario de la beta de punta a punta con una cuenta de prueba que luego se borra, y las 5 pasadas de
  Lighthouse contra `gymprofit.app`.
- DEC-069 pasa a «Aceptada» solo si el propietario lo decide; `CHANGELOG.md` con los hashes.

---

## 8. Dudas del propietario, respondidas el 25-11-2026

1. **Qué se escanea.** A las 8:00 se **busca** el desayuno básico de la demo, solo en escritorio. Escena
   nueva a las 17:00: se **escanea un yogur envasado real** de `productos_off`, sin enseñar la marca
   (nombre genérico, código dibujado en SVG), en móvil y en escritorio. Si hay que sembrarlo en la demo,
   se añade a `scripts/sembrar_demo.py`.
2. **Tema.** La portada, siempre en carbón. Las páginas legales y la beta conservan claro y oscuro.
3. **Inglés.** No por ahora; la estructura de Astro, preparada para añadirlo.
4. **Profi.** No sale en la web: a las 14:00, el botón «Copiar ayer» de Nutrición.
5. **Medición.** Solo en local con `servir-web.py` y brotli hasta la fase 6. No se crea nada en
   Cloudflare.
6. **JS diferido.** ≤ 60 KB gzip en móvil y ≤ 70 KB en escritorio, con Lenis solo con ratón.
