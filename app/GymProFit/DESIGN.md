---
name: GymProFit (app Android)
description: "Cuaderno de atleta sobre carbón: cifras condensadas, un solo naranja y el dorado para lo conseguido."
# Todos los valores salen de app/src/main/res (rutas relativas a esa carpeta) y de
# app/src/main/java/es/pmdm/gymprofit/utils/Movimiento.java. Dos temas: «-dark» sale de
# values-night/colors.xml (el tema por defecto) y «-light» de values/colors.xml.
colors:
  # Marca (values/colors.xml y values-night/colors.xml, sección «Marca»)
  primary: "#FF6A00"   # tema oscuro (por defecto); el claro es primary-light
  on-primary-dark: "#1A0E00"
  primary-container-dark: "#3A2110"
  on-primary-container-dark: "#FFB27A"
  primary-light: "#B83E00"
  on-primary-light: "#FFFFFF"
  primary-container-light: "#FFE0CC"
  on-primary-container-light: "#5C1E00"
  # Dorado: récords y logros (DEC-018)
  gold-dark: "#E8B84B"
  gold-container-dark: "#3A2E12"
  gold-light: "#AE8528"
  gold-text-light: "#8A6920"
  gold-container-light: "#FFF4D9"
  # Fila de la serie que batió el récord en la sesión en vivo (GP-210)
  gold-row-dark: "#2A2414"
  gold-row-light: "#FFF8E6"
  # Tarjeta protagonista: igual en los dos temas (GP-210 paso 2)
  hero: "#3A2110"
  on-hero: "#FFF4E8"
  on-hero-variant: "#FFD3B0"
  # Superficies y bordes
  background-dark: "#0B0C0E"
  surface-dark: "#1B1E24"
  surface-2-dark: "#252A31"
  stroke-dark: "#676F7E"
  divider-dark: "#4A505B"
  background-light: "#F1F1EE"
  surface-light: "#FFFFFF"
  surface-2-light: "#E8E8E3"
  stroke-light: "#8C8C7D"
  divider-light: "#C9C9C2"
  # Texto
  on-surface-dark: "#F2F2F3"
  text-secondary-dark: "#A1A9B6"
  on-surface-light: "#17181C"
  text-secondary-light: "#5A5C62"
  # Macros (un color por macro)
  macro-proteinas-dark: "#6BA6F5"
  macro-carbos-dark: "#E8A94B"
  macro-grasas-dark: "#B295E8"
  macro-proteinas-light: "#2E6CCB"
  macro-carbos-light: "#99620E"
  macro-grasas-light: "#7A54C4"
  # Estados y agua
  success-dark: "#4ADE80"
  success-container-dark: "#143323"
  error-dark: "#F87171"
  error-container-dark: "#3A1414"
  success-light: "#1A7C41"
  success-container-light: "#DFF4E7"
  error-light: "#C62828"
  error-container-light: "#FDE9E9"
  agua-dark: "#56C2D6"
  agua-container-dark: "#10323A"
  agua-light: "#0E7C8F"
  agua-container-light: "#D7F2F6"
  meter-track-dark: "#353A43"
  meter-track-light: "#D6D6D0"
typography:
  # Familias: font/barlow.xml (400, 500, 600) y font/barlow_condensed.xml (500, 600, 700).
  # Unidades: el linter de DESIGN.md solo admite unidades CSS, así que aquí 1px = 1dp = 1sp (los lienzos
  # aprobados usan px = dp); en el texto de abajo se dan en dp y sp. Tamaños: values/dimens.xml. Estilos: values/themes.xml (Font.GymProFit.* y TextAppearance.GymProFit.*).
  number-hero:
    fontFamily: "Barlow Condensed"
    fontSize: "56px"
  number-gigante:
    fontFamily: "Barlow Condensed"
    fontSize: "72px"
    fontWeight: 700
  number:
    fontFamily: "Barlow Condensed"
    fontSize: "44px"
    fontWeight: 700
    letterSpacing: "0em"
  display-large:
    fontFamily: "Barlow Condensed"
    fontSize: "34px"
    fontWeight: 700
    letterSpacing: "0.01em"
  display:
    fontFamily: "Barlow Condensed"
    fontSize: "26px"
  headline:
    fontFamily: "Barlow Condensed"
    fontSize: "20px"
    fontWeight: 600
    letterSpacing: "0.01em"
  title:
    fontFamily: "Barlow"
    fontSize: "20px"
    fontWeight: 600
  subtitle:
    fontFamily: "Barlow Condensed"
    fontSize: "16px"
  fila:
    fontFamily: "Barlow"
    fontSize: "16px"
  body:
    fontFamily: "Barlow"
    fontSize: "16px"
  enlace:
    fontFamily: "Barlow"
    fontSize: "15px"
    fontWeight: 600
  caption:
    fontFamily: "Barlow"
    fontSize: "14px"
  label:
    fontFamily: "Barlow Condensed"
    fontSize: "14px"
    letterSpacing: "0.12em"
  eyebrow:
    fontFamily: "Barlow Condensed"
    fontSize: "13px"
    fontWeight: 700
    letterSpacing: "0.08em"
rounded:
  # Escala de radios (values/dimens.xml, radio_*). Botones y píldoras: la mitad de su altura.
  chip: "12px"          # chips y campos pequeños
  miniatura: "16px"
  card-pequena: "20px"
  card: "24px"          # tarjeta estándar
  protagonista: "28px"  # tarjeta protagonista y hojas inferiores
  dialog: "22px"
  button: "26px"        # botón principal en reposo (Expressive); al pulsar, 12px
  circle: "9999px"      # cornerSize 50% en Android (ShapeAppearance.GymProFit.Circulo)
spacing:
  # Escala de espaciado (values/dimens.xml, espacio_*): 4, 8, 12, 16, 20, 24, 28 y 36.
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "20px"
  xxl: "24px"
  seccion-subpantalla: "28px"
  seccion-pestana: "36px"
  margen-pantalla: "20px"
components:
  # Referencias al tema oscuro, que es el de por defecto; en claro se cambia -dark por -light.
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary-dark}"
    typography: "{typography.fila}"
    rounded: "{rounded.button}"
    height: "48px"
  button-outline:
    backgroundColor: "{colors.surface-dark}"
    textColor: "{colors.on-surface-dark}"
    typography: "{typography.body}"
    rounded: "{rounded.circle}"
    height: "48px"
  card:
    backgroundColor: "{colors.surface-dark}"
    rounded: "{rounded.card}"
  card-pequena:
    backgroundColor: "{colors.surface-dark}"
    rounded: "{rounded.card-pequena}"
  card-protagonista:
    backgroundColor: "{colors.hero}"
    textColor: "{colors.on-hero}"
    rounded: "{rounded.protagonista}"
  card-record:
    backgroundColor: "{colors.gold-container-dark}"
    textColor: "{colors.gold-dark}"
    rounded: "{rounded.card-pequena}"
  chip-zona-hecha:
    backgroundColor: "{colors.primary-container-dark}"
    rounded: "{rounded.chip}"
    height: "44px"
  chip-zona-pendiente:
    rounded: "{rounded.chip}"
    height: "44px"
  fila:
    backgroundColor: "{colors.surface-dark}"
    textColor: "{colors.on-surface-dark}"
    typography: "{typography.fila}"
    rounded: "{rounded.card}"
    height: "64px"
  avatar:
    backgroundColor: "{colors.hero}"
    textColor: "{colors.on-hero}"
    rounded: "{rounded.circle}"
    size: "48px"
  boton-redondo:
    backgroundColor: "{colors.surface-2-dark}"
    textColor: "{colors.on-surface-dark}"
    rounded: "{rounded.circle}"
    size: "48px"
  barra-navegacion:
    backgroundColor: "{colors.surface-dark}"
    textColor: "{colors.text-secondary-dark}"
    height: "60px"
  fab:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary-dark}"
---

# Design System: GymProFit (app Android)

Documenta lo que **existe** en `app/GymProFit` tras GP-210 paso 2 (22-10-2026, lote 1.8.0): los cimientos de GP-216 y las transiciones y estilos Expressive. Contexto del producto en [PRODUCT.md](PRODUCT.md). Las decisiones que mandan: DEC-018 (oscuro, naranja, dorado), DEC-019 (accesibilidad), DEC-020 (ES y EN) y DEC-039 (movimiento), en `documentacion/PRODUCT-DECISIONS.md`. Los diseños aprobados, en `documentacion/diseno/` (el de referencia para estructura: `2026-09-27-estructura/`). **Si un lienzo y un token no coinciden, manda el token** (sus contrastes están medidos, GP-063).

Quien cambie un token o un patrón en `res/` o en `Movimiento.java` actualiza este fichero (regla en `CLAUDE.md`).

## Overview

**Creative North Star: "El cuaderno del atleta"**

La app se lee como un cuaderno de entreno: los datos primero, las cifras grandes y condensadas sobre un carbón casi negro, y una sola voz de color, el naranja, que dice dónde se pulsa o qué toca hoy. El dorado es una medalla: aparece solo cuando algo se ha conseguido (un récord, un logro) y por eso significa algo. Las tarjetas se separan del fondo por el tono en oscuro y por un borde de 1 dp en claro, nunca con sombras, y el movimiento es corto, nativo y nunca hace esperar.

Es una interfaz de uso, no de exhibición (modo *Operate*): se usa entre series, con una mano, así que manda la legibilidad y la zona pulsable. El carácter está en los detalles: la tipografía condensada en cifras y títulos, el hundimiento de un 3 % al tocar, el trofeo que salta.

**Key Characteristics:**
- Oscuro por defecto; el claro es un tema completo con los mismos roles, no una inversión (DEC-018).
- Un único acento, el naranja, que es el mismo en los dos temas (en claro se oscurece a `#B83E00` para llegar a 4,5:1).
- Dorado reservado a récords y logros.
- Cifras y títulos en Barlow Condensed; el texto de lectura en Barlow de ancho normal.
- Material Components 1.14.0 sobre vistas XML, tematizados con tokens `gp_*` (Material 3 estructura; la marca va en el tema), con transiciones de Material motion y estilos Expressive (ver «Cimientos» y «Motion»).
- Sin emojis; iconos Material Symbols Rounded teñidos por el tema.
- Sin estética gaming, sin exceso de efectos (DEC-018).

## Cimientos

Un solo sistema de espaciado, radios, tarjetas, filas y textos (GP-210 paso 2, base de las pantallas rehechas de GP-216). Todo vive en `values/dimens.xml`, `values/colors.xml`, `values/themes.xml` y `values/estilos_sistema.xml`; **un layout nuevo pide estos nombres y no escribe números**. `CimientosTest` lee `res/` y falla si un valor se separa de lo que dice aquí o si un layout vuelve a escribir un radio, una elevación o un `sp`.

### Espaciado

4, 8, 12, 16, 20, 24, 28 y 36 dp (`espacio_4` … `espacio_36`). Margen lateral de pantalla: 20 (`margen_pantalla`). Entre secciones: 36 en las pestañas (`separacion_pestana`) y 28 en las subpantallas (`separacion_subpantalla`). De un título de sección a su contenido: 14 (`titulo_a_contenido`).

### Radios

| Radio | Para | Nombre |
|---|---|---|
| 12 | chips y campos pequeños | `radio_chip` |
| 16 | miniaturas | `radio_miniatura` |
| 20 | tarjetas pequeñas | `radio_tarjeta_pequena` |
| 24 | tarjeta estándar | `radio_tarjeta` |
| 28 | tarjeta protagonista y hojas inferiores | `radio_protagonista` |
| mitad de la altura | botones y píldoras | `ShapeAppearance.GymProFit.Circulo` |

### Tarjeta estándar

`Widget.GymProFit.Card`: superficie `colorSurface` (#1B1E24 en oscuro), radio 24 y **elevación 0**. En oscuro **sin borde**: se distingue por el tono contra el fondo #0B0C0E. En claro, borde de 1 dp con `colorOutlineVariant` (`tarjeta_borde` vale 1 dp en `values/` y 0 en `values-night/`). Esto corrige el «borde, no sombra» de GP-209. Las tarjetas dibujadas a mano (grupos de filas, hoja del escáner) usan `gp_tarjeta_borde`, que en oscuro es el color de la propia superficie; para un grupo de filas, `bg_tarjeta_filas` (radio 24 y ese borde), con un relleno de `tarjeta_borde` en el contenedor para que las filas no lo tapen; si las filas son opacas, se recorta la lista por dentro (`bg_tarjeta_filas_dentro`, radio 23), no la tarjeta, o las esquinas tapan el borde. `Card.Pequena` baja el radio a 20.

### Tarjeta protagonista

`Widget.GymProFit.Card.Protagonista`, para «Hoy toca» y los resúmenes: fondo #3A2110 (`gp_hero`), radio 28, sin borde, título en #FFF4E8 (`gp_on_hero`, 13,78:1) y apoyo en #FFD3B0 (`gp_on_hero_variant`, 10,82:1). Es igual en los dos temas, y los macros de dentro también: la barra de reparto y los puntos usan `gp_hero_macro_*` (los de oscuro, 5,95 a 7,27:1 sobre #3A2110; los de claro daban menos de 3:1). Lleva su propio tema (`ThemeOverlay.GymProFit.Hero`): lo de dentro lee texto, apoyo y borde de botón de la tarjeta, y el botón principal es el naranja de oscuro (#FF6A00 sobre #3A2110, 5,21:1; el #B83E00 de claro daba 2,1:1 y el botón se perdía).

### Cabecera de sección

`Widget.GymProFit.CabeceraSeccion` (fila) con `.Titulo` (20 sp, Barlow SemiBold, `TextAppearance.GymProFit.Seccion`) y, opcional a la derecha, `.Enlace` (15 sp, Barlow 600, en `colorPrimary`, 48 dp de alto y de ancho mínimo: DEC-019). No sustituye al rótulo en mayúsculas de dentro de una tarjeta (`Widget.GymProFit.Seccion`, 13 sp), que se queda.

### Fila de lista

`Widget.GymProFit.Fila`: mínimo 64 dp y 16 dp a los lados. Icono en círculo de 40 dp sobre #252A31 (`Fila.Icono`, `bg_fila_icono`); título de 16 sp (`Fila.Titulo`) y apoyo de 13 sp en #A1A9B6 (`Fila.Apoyo`); separador de 1 dp en `surface_2` con sangría hasta el texto, 70 dp (`Widget.GymProFit.Separador`, `div_grupo_ajustes`). Las filas van dentro de una tarjeta de radio 24. Las filas que ya existían (Ajustes, hoja del «+») usan estos mismos números.

### Roles de texto

| Rol | Tamaño y fuente | TextAppearance |
|---|---|---|
| Título de pantalla | 34 sp, Barlow Condensed Bold | `TextAppearance.GymProFit.Display` (`text_display_large`) |
| Cifra protagonista | 26 a 48 sp, Barlow Condensed | `Cifra.Pequena` (26), `Number` (44), `Cifra.Grande` (48) |
| Sección | 20 sp, Barlow SemiBold | `TextAppearance.GymProFit.Seccion` (`text_title`) |
| Cuerpo | 16 sp, Barlow | `TextAppearance.GymProFit.Cuerpo` (`text_body`, `text_fila`) |
| Apoyo | 13 a 14 sp, Barlow | `Apoyo` (14) y `Apoyo.Pequeno` (13) |

Las cifras de 56 sp (héroe) y 72 sp (resumen del entreno) siguen existiendo como excepciones de cabecera. La escala `text_*` de abajo apunta a estos roles.

### Avatar de cabecera

48 dp, borde de 2 dp en naranja (`colorPrimary`), fondo #3A2110 e inicial en Barlow Condensed (`Widget.GymProFit.Avatar`, `view_avatar`). Cierra GP-214: en claro el fondo se distingue del de la pantalla a 13,2:1 (antes ~1,1:1) y en oscuro lo hace el borde (6,82:1).

### Expressive: botones

`BotonExpresivo` (`ui/widget`) es el botón principal que cambia de forma al pulsarlo: las esquinas pasan de 26 a 12 dp en 120 ms con la curva estándar (`Movimiento.TOQUE`) y vuelven al soltar. Lo son «Empezar» (Inicio, Entrenar y «Tu programa»), «Hecho» (resumen y barra de Añadir) y el «+» de la barra (`BotonExpresivo.alPulsar`). **Selector de opciones** (`SelectorPildora`, GP-224) donde Material 3 Expressive pone los botones conectados: una pista con una píldora naranja que viaja a la elegida. Sustituyó a los botones conectados y a la unidad de la ficha, que se borraron. Los filtros largos y desplazables se quedan en chips. **Indicador de carga con forma** (`LoadingIndicator`) solo en esperas cortas dentro de un botón o una tarjeta: «Crear cuenta», el guardado de la sesión y el programa del alta.

## Colors

Paleta atlética de dos temas con nombres semánticos. Los layouts no escriben hex: usan `@color/gp_*` o `?attr/...` (`values/colors.xml`, cabecera). Fuentes: `values/colors.xml` (claro, y los alias heredados) y `values-night/colors.xml` (oscuro, por defecto). El tema mapea los tokens a roles M3 en `values/themes.xml` (`Theme.GymProFit`).

### Primary
- **Naranja de marca** (oscuro `#FF6A00` · claro `#B83E00`, `gp_primary`): botón principal, FAB, píldora de la pestaña activa, barra de kcal, enlaces de acción («Registrar comida»). En claro se oscurece porque `#FF6A00` daba 2,56:1 como texto sobre blanco; `#B83E00` da 5,63:1 (comentario en `values/colors.xml`).
- **Sobre el naranja** (oscuro `#1A0E00` · claro `#FFFFFF`, `gp_on_primary`): en oscuro el texto es carbón cálido, no blanco: el blanco daba 2,87:1 y `#1A0E00` da 6,61:1.
- **Contenedor naranja** (oscuro `#3A2110` / `#FFB27A` · claro `#FFE0CC` / `#5C1E00`, `gp_primary_container` y `gp_on_primary_container`): zonas hechas, chips marcados, fondo del icono de acción, píldora del bottom nav.

### Secondary
- **Dorado** (`gp_gold`; oscuro `#E8B84B` · claro `#AE8528`) y **dorado de texto** (`gp_gold_text`; claro `#8A6920`, en oscuro el mismo): récords y logros, nada más (DEC-018). Son dos tokens porque en claro un solo dorado no cumplía a la vez 3:1 gráfico y 4,5:1 como texto (GP-063). **Contenedor dorado** (`gp_gold_container`; oscuro `#3A2E12` · claro `#FFF4D9`): fondo de la tarjeta «Último récord» y del medallón de un logro.

### Tertiary (semánticos de dato)
- **Macros** (`gp_macro_proteinas`, `gp_macro_carbos`, `gp_macro_grasas`): proteína azul (oscuro `#6BA6F5` · claro `#2E6CCB`), carbohidratos ámbar (`#E8A94B` · `#99620E`), grasas violeta (`#B295E8` · `#7A54C4`). Los tres llegan a 4,5:1 en claro (GP-063).
- **Agua** (`gp_agua`, oscuro `#56C2D6` · claro `#0E7C8F`; contenedor `#10323A` · `#D7F2F6`): vaso y gota de la hidratación (lote 1.6.8).
- **Éxito** (`gp_success`, `#4ADE80` · `#1A7C41`) y **error** (`gp_error`, `#F87171` · `#C62828`) con sus contenedores (`gp_success_container`, `gp_error_container`).

### Neutral
- **Fondo** (`gp_background`, oscuro `#0B0C0E` · claro `#F1F1EE`): fondo de ventana y de las barras del sistema.
- **Superficie** (`gp_surface`, `#1B1E24` · `#FFFFFF`) y **superficie 2** (`gp_surface_2`, `#252A31` · `#E8E8E3`): tarjetas y filas; superficie 2 para el botón redondo, el snackbar y el chip en reposo.
- **Borde** (`gp_stroke`, `#676F7E` · `#8C8C7D`): delimita tarjeta, campo y chip; 3:1 contra fondo y tarjeta desde GP-063. **Separador** (`gp_divider`, `#4A505B` · `#C9C9C2`): línea decorativa interna, no delimita nada.
- **Texto** (`gp_on_surface`, `#F2F2F3` · `#17181C`) y **texto secundario** (`gp_text_secondary`, `#A1A9B6` · `#5A5C62`): el secundario sustituye a los `android:alpha` sobre texto, que bajaban a 2,14:1.
- **Pista del medidor** (`gp_meter_track`, `#353A43` · `#D6D6D0`): se mide contra la marca, no contra la tarjeta (GP-078).
- **Velo del «+»** (`gp_velo`, `#9E000000`, igual en los dos temas) y el escáner (`gp_escaner_texto` `#F2F2F3`, `gp_escaner_leido` `#4ADE80`, `gp_escaner_velo` `#C70B0C0E`): siempre sobre imagen o sobre la capa oscura, sin variante por tema.

### Named Rules
**The One Voice Rule.** El naranja es el único acento y el mismo en los dos temas; su rareza es la jerarquía. Si todo es naranja, nada lo es.
**The Medal Rule.** El dorado solo celebra algo conseguido (récord, logro). No se usa como decoración ni como segundo acento (DEC-018).
**The One Logo Rule.** Hay un solo logo, la G dorada con el trazo naranja del icono (`drawable/logo_marca`, DEC-057), y no se recolorea ni tiene variante por tema: sobre oscuro va sola y sobre claro dentro de su baldosa `logo_baldosa` (#0B0C0E, transparente en `values-night`), con la G en la proporción del icono en el lanzador y en el mismo sitio en los dos temas. Su dorado y su baldosa son de la marca, no de la interfaz: no cuentan para la Medal Rule. Sale de los trazados de `g_arranque` con `scripts/trazar-g-arranque.py logo`; no se retoca a mano.
**The Measured Contrast Rule.** Un color nuevo trae su contraste medido (4,5:1 en texto, 3:1 en borde o gráfico) en el comentario de `colors.xml`, como los existentes. Los alias heredados (`primary_light`, `golden`, `orange_dark`…) no se usan en pantallas nuevas.

## Typography

**Display Font:** Barlow Condensed (`font/barlow_condensed.xml`, pesos 500, 600 y 700; sin 400 empaquetado).
**Body Font:** Barlow, ancho normal (`font/barlow.xml`, pesos 400, 500 y 600; un `bold` cae en el 600 y no se sintetiza).

**Character:** una familia en dos anchos. La condensada es contundente y lleva cifras, títulos y etiquetas; el texto de lectura va en ancho normal porque a tamaño de lectura la condensada cierra las contraformas (`values/themes.xml`, comentario de GP-089). Todo en `sp`; ningún texto baja de 13 sp. Los roles de texto de GP-210 paso 2 («Cimientos») son los que mandan; la escala `text_*` de abajo apunta a ellos.

### Hierarchy
Los tamaños son los once escalones de `values/dimens.xml` (más `envivo_descanso_numero` 46 sp). Los estilos M3 los llevan a los componentes de Material (`Font.GymProFit.*`, `values/themes.xml`).

| Rol | Familia · tamaño | Dónde (fichero) |
|---|---|---|
| Cifra héroe | Condensada · `text_number_hero` 56 sp | La cifra protagonista de una pantalla |
| Cifra del resumen | Condensada 700 · `text_number_gigante` 72 sp | Los kilos que cuentan desde cero en el resumen del entreno (GP-210, paso 1); la única cifra por encima del héroe |
| Cifra | Condensada 700 · `text_number` 44 sp | `TextAppearance.GymProFit.Number` y `Font.GymProFit.DisplayLarge`: kcal de hoy, peso, estadísticas |
| Título de pantalla | Condensada 700 · `text_display_large` 34 sp, interletraje 0,01 | `TextAppearance.GymProFit.Display`; título de las cuatro pestañas (`Widget.GymProFit.TituloPestana`) |
| Cifra pequeña | Condensada · `text_display` y `text_cifra_pequena` 26 sp | `TextAppearance.GymProFit.Cifra.Pequena`; `Font.GymProFit.DisplaySmall` y `HeadlineLarge` |
| Titular de tarjeta | Condensada 600 · `text_headline` 20 sp | `TextAppearance.GymProFit.Headline`; título de la cabecera (`Widget.GymProFit.Cabecera`) |
| Sección | Barlow 600 · `text_title` 20 sp | `TextAppearance.GymProFit.Seccion`; `Font.GymProFit.TitleLarge` |
| Título de fila o tarjeta | Condensada · `text_subtitle` 16 sp | `Font.GymProFit.TitleMedium`; nombre en `GymProFit.MealTitle` |
| Fila de lista | Barlow · `text_fila` 16 sp | Hoja del «+», comidas, plantillas, récords, botón píldora |
| Cuerpo | Barlow · `text_body` 16 sp | El tamaño más usado; `TextAppearance.GymProFit.Cuerpo`, `Font.GymProFit.BodyLarge` |
| Secundario | Barlow · `text_caption` 14 sp | Descripciones y metadatos; `Font.GymProFit.BodyMedium` |
| Etiqueta | Condensada · `text_label` 14 sp, MAYÚSCULAS, interletraje 0,12 | `TextAppearance.GymProFit.Label`; botones y chips (`Font.GymProFit.LabelLarge`) |
| Título junto a Profi | Barlow 600 · `text_titulo_profi` 17 sp | «Lo que te encaja» de Añadir, con la cara de Profi de 32 dp (`profi_cara_pequena`; tablero 25, GP-225) |
| Rótulo de sección | Condensada, negrita · `text_eyebrow` 13 sp, interletraje 0,08 | `Widget.GymProFit.Seccion` («ESTA SEMANA», «NUTRICIÓN DE HOY»); suelo de la escala |

### Named Rules
**The No-Raw-sp Rule.** Ningún layout escribe un `sp` a mano: usa un `@dimen/text_*`. Hubo 21 tamaños distintos antes de la escala (`dimens.xml`, cabecera).
**The Floor Rule.** Nada baja de 13 sp (GP-089). El contenido de lectura no baja de 16 sp.
**The Two Widths Rule.** Cifras, títulos y etiquetas en condensada; todo texto que se lea de corrido, en Barlow normal.
**The System Curve Rule.** Desde Android 14 la letra grande no escala en línea recta: cuanto mayor el tamaño, menos crece (escalado no lineal del sistema). Medido en Inicio a letra 1,3 (API 36, GP-212, con los tamaños de entonces: título 30, cifra 26, titular 22, cuerpo 15): el título se queda en 30 (×1,00), la cifra de 26 sp ×1,05, el titular de 22 sp ×1,14 y el cuerpo de 15 sp ×1,29; el «D» del avatar crece (×1,18) pero su círculo mide 48 dp fijos, así que sus límites no cambian. Nada de eso es un tope nuestro (los `dimens` van en sp, sin tamaño automático ni alturas fijas): es la curva del sistema y no se corrige. Quien mida con `MedidorAccesibilidad` debe saber que sus «sp equivalentes» dividen por la escala lineal y por eso bajan en los títulos.

## Layout

Una pantalla es una columna de tarjetas sobre el fondo. Las cuatro pestañas (Inicio, Entrenar, Nutrición, Progreso) comparten cabecera: título de 34 sp, botón redondo o avatar de 48 dp a la derecha, `margen_pantalla` (20 dp) a los lados, 18 arriba y 6 abajo (`Widget.GymProFit.CabeceraPestana`, `values/estilos_estructura.xml`). Las pantallas secundarias llevan una sola cabecera, la `MaterialToolbar` con «Volver» (`Widget.GymProFit.Cabecera`; `CabeceraTest` vigila que no vuelva la fila hecha a mano).

La navegación principal es una barra fija inferior de cuatro pestañas más el «+» central (`ui/widget/BarraNavegacion.java`, mínimo 60 dp). El hueco de la barra de gestos se suma ahí y en ningún otro sitio. Las etiquetas crecen con la letra del sistema hasta 1,3.

**Espaciado.** La escala es la de «Cimientos» (4, 8, 12, 16, 20, 24, 28 y 36 dp, con `espacio_*`). Quedan layouts anteriores a ella con 6, 10, 14 y otros valores sueltos; se llevan a la escala cuando GP-216 rehaga cada pantalla. Piezas repetidas: cabecera de pestaña 20/18/6; fila de lista 64 dp con 16 a los lados; icono de comida 44 dp con un icono de 24; tarjeta de comida con 14 dp de relleno.

**Zonas pulsables.** 48 dp como mínimo en todo (DEC-019): `Widget.GymProFit.BotonIcono` y `BotonRedondo` miden 48 dp; el botón principal tiene `minHeight` de 52 dp (`Widget.GymProFit.Button`; `BotonExpresivo` en los principales) y el píldora, de 48 dp; el chip de zona mide 44 dp pero es informativo (`importantForAccessibility="no"` en `item_chip_zona.xml`; la descripción la lleva el grupo). El medidor de depuración (`depuracion/MedidorAccesibilidad.java`) marca las que bajan de 48.

**Escala de letra y ancho.** Todo en `sp`; los controles de ancho fijo no cortan el texto con letra 1,3 (el chip de zona «nunca parte su texto»; el selector de opciones parte por sílabas y crece, GP-224).

## Elevation & Depth

**Plana: la separa el tono (oscuro) o un borde de 1 dp (claro), no la sombra.** `Widget.GymProFit.Card` (`values/themes.xml`) declara elevación 0: la elevación tonal de M3 tiñe de naranja la tarjeta en oscuro y la sombra no se ve. En oscuro la tarjeta (#1B1E24) se distingue del fondo (#0B0C0E) por el tono y no lleva borde; en claro lleva borde de 1 dp en `colorOutlineVariant` (`@dimen/tarjeta_borde`). `CimientosTest` impide que una tarjeta fije su elevación o su radio a mano.

### Shadow Vocabulary
- **Menú emergente** (`android:popupElevation` 12 dp, `values/themes.xml`, `Widget.GymProFit.PopupMenu`): fondo `gp_surface`, radio 16 y borde `gp_stroke` (`drawable/bg_popup_menu.xml`).
- **Aviso propio** (`layout/toast_custom.xml`, 4 dp) y **vista de la bienvenida** (`layout/view_demo_bienvenida.xml:152`, 8 dp) son los únicos elevados a mano además de la hoja de Añadir (`layout/activity_anadir_alimento.xml:227`, 8 dp).
- **Velo del «+»** (`gp_velo`, negro al 62 %) para separar la hoja del contenido, sin sombra.

### Named Rules
**The Tone-Not-Shadow Rule.** Una tarjeta se separa del fondo por el tono en oscuro y por un borde de 1 dp en claro, nunca con una sombra que en oscuro no se ve. El borde de la tarjeta (`outlineVariant`, 1,66:1 contra blanco) no delimita un control; los bordes de campos y chips siguen siendo `gp_stroke` a 3:1.

## Shapes

Una escala de radios (ver «Cimientos»): 12 (chips y campos pequeños), 16 (miniaturas), 20 (tarjetas pequeñas), 24 (tarjeta estándar), 28 (tarjeta protagonista y hojas inferiores) y la mitad de la altura en botones y píldoras (`ShapeAppearance.GymProFit.Circulo`, cornerSize 50 %). Los botones principales son Expressive: 26 dp en reposo y 12 al pulsar. Otras piezas:

| Pieza | Radio | Fuente |
|---|---|---|
| Chip (filtro y zona) | 12 dp | `Widget.GymProFit.Chip` (`chipCornerRadius`), `Widget.GymProFit.ChipZona` |
| Botón de Material y píldora | 50 % | `Widget.GymProFit.Button`, `BotonPildora`, `BotonPildora.Contorno` (`shapeAppearanceOverlay`) |
| Tarjeta | 24 dp | `Widget.GymProFit.Card`; también el grupo de Ajustes (`bg_grupo_ajustes`) |
| Tarjeta pequeña | 20 dp | `Widget.GymProFit.Card.Pequena` |
| Tarjeta protagonista, hojas | 28 dp | `Card.Protagonista`, `bg_hoja_escaner`, hoja del «+» |
| Diálogo | 22 dp | `ThemeOverlay.GymProFit.Dialog` (`dialogCornerRadius`) |
| Botón redondo, avatar | 50 % | `ShapeAppearance.GymProFit.Circulo` |

El borde de una pieza con borde es siempre de 1 dp (`strokeWidth`, `chipStrokeWidth`, `<stroke>`); el avatar, de 2.

## Components

Lo que está en uso. Todo se tematiza por `Theme.GymProFit` (`values/themes.xml`): los widgets de Material 3 toman estilo de ahí (`materialCardViewStyle`, `materialButtonStyle`, `chipStyle`, `bottomNavigationStyle`, `floatingActionButtonStyle`, `snackbarStyle`…).

### Buttons
- **Shape:** mitad de la altura (`BotonPildora`, `values/estilos_estructura.xml`; el botón base es `Widget.GymProFit.Button`, 52 dp de alto mínimo). Los principales («Empezar», «Hecho», el «+») son `BotonExpresivo`: 26 dp en reposo y 12 al pulsar, en 120 ms.
- **Primary:** relleno `gp_primary`, texto `gp_on_primary`, Barlow 16 sp, sin mayúsculas, 48 dp de alto.
- **Outline:** `Widget.GymProFit.BotonPildora.Contorno`, borde `colorOutline`, texto `colorOnSurface`, Barlow 15 sp negrita («Cambiar», zonas de la biblioteca en `BotonZona`).
- **Icon / round:** `BotonIcono` (48 dp, sin fondo) y `BotonRedondo` (48 dp, fondo `bg_circulo_superficie`).
- **Estados:** pulsado, la capa del 10 % del color del texto (`color/boton_capa` en el relleno, `tarjeta_capa` en el de contorno); con el foco, ninguna capa: el anillo de 2 dp de `FocoVisible`, como en todo lo pulsable (GP-225: la capa de foco de Material pasaba el naranja de `#B83E00` a `#BF521A`). Ningún componente de Material lleva capa de foco: `values/material_foco.xml` (y sus copias en `values-v21`, `v28`, `v33` y `v34`, donde la librería las redefine) pone a 0 las opacidades de las que Material la saca, y los fondos pulsables propios usan `tarjeta_capa` (o `boton_capa` sobre naranja), nunca `colorControlHighlight`. Un bloque de texto que toma el foco lleva `aire_anillo` por dentro y su margen negativo por fuera en los cuatro lados.
- **Pressed:** hundimiento del 3 % en 120 ms con la curva estándar (`animator/toque.xml`, momento 1 de DEC-039); en `BotonExpresivo`, además, las esquinas pasan de 26 a 12 dp en esos mismos 120 ms.
- **«Respira»:** el botón principal crece un 3 % una vez para decir «aquí» (`Movimiento.respirar`): Inicio, Bienvenida, Avisos y el plan del alta.

### Chips
- **Selector de opciones** (`ui/widget/SelectorPildora`, GP-224): el de la app para opciones que se excluyen: desde GP-216 (lote 4) no queda ningún selector propio. Los colores se leen del tema una vez, al crearse: si la configuración cambia con la pantalla abierta, no se repinta con los del otro tema. Pista de 48 dp (`zona_tactil`) con radio completo, 4 dp de relleno (`selector_relleno`), `gp_surface_2` (#E8E8E3 · #252A31) y sin borde; píldora de 40 dp (`selector_opcion_alto`) en `gp_primary` (#B83E00 · #FF6A00), 4,58:1 · 5,03:1 contra la pista. Texto en Barlow Condensed 600 a 16 sp, sin mayúsculas: el elegido en `gp_on_primary` (5,63:1 · 6,61:1), los demás en `gp_text_secondary` (5,44:1 · 6,10:1). Sin opción elegida no hay píldora.
  - **Uso**: de 2 a 4 opciones cortas, o hasta 5 si son cifras (los días del alta), todas del mismo ancho. Con más opciones o textos largos, la hoja de opciones del alta. La unidad del alimento: con 2, selector; con 3 o más, el desplegable (`btnUnidadVarias`). Los filtros largos siguen en chips.
  - **Movimiento**: la píldora viaja con `Movimiento.PILDORA` y `REBOTE` (momento 6); el texto cambia de color en `Movimiento.TEXTO_OPCION` (200 ms); la opción se hunde un 3 % al pulsarla (`animator/toque`). Con «Quitar animaciones» (`Movimiento.quieto`) la píldora salta sin recorrido.
  - **Estados**: pulsado, capa del 10 % de `gp_on_surface` (`selector_capa_pulsado`); con foco de teclado, anillo de 2 dp en `gp_on_surface` (`selector_anillo_foco`); desactivado, al 38 % (`alfa_desactivado`).
  - **Accesibilidad**: TalkBack anuncia el grupo con su nombre (`app:selectorNombreGrupo`: «Unidad», «Sexo»…) y cada opción como botón de opción, con su estado y su posición; con `app:selectorPestanas` (Añadir y la ficha del ejercicio), como pestaña («pestaña, 1 de 3», seleccionada). Cada opción responde al toque en los 48 dp de la pista (la píldora, la capa y el anillo van 4 dp hacia dentro). A letra 1,3 la opción parte en dos líneas (una palabra que no cabe, por sílabas con guion: «Mode-rada») y la pista crece: nunca corta; la píldora conserva el radio completo. La elegida sobrevive al giro (la vista guarda su índice si tiene id).
  - **Dónde**: ficha del alimento (unidad), Añadir (pestañas), ficha del ejercicio (pestañas y período), «Kilos movidos» de Progreso (período), Tus datos (sexo, actividad y nivel), crear y editar rutina (nivel), estadísticas de nutrición (período), Medidas (métrica y rango), seguir un programa (minutos) y el alta (días y minutos).
- **Filtro** (`Widget.GymProFit.Chip`): 12 dp, borde 1 dp `gp_stroke`; marcado = fondo `gp_primary_container` con check naranja, en reposo `gp_surface_2` (`color/chip_background_selector.xml`). En hojas, sin relleno (`Chip.Hoja`, GP-134).
- **Zona de «Esta semana»** (`Widget.GymProFit.ChipZona`, `item_chip_zona.xml`): 44 dp, 12 dp, nunca parte el texto; informativo, sin nombre propio para TalkBack (lo lleva el grupo). Hecha = relleno `colorPrimaryContainer` (`bg_chip_zona_hecha`); pendiente = sin relleno y borde de 1 dp en `colorOutline` (`bg_chip_zona_pendiente`).

### Cards / Containers
- **Corner Style:** 24 dp (`Card`), 20 (`Card.Pequena`) o 28 (`Card.Protagonista`).
- **Background:** `colorSurface`; la protagonista, #3A2110. Elevación 0; borde de 1 dp en `colorOutlineVariant` solo en claro (ver «Cimientos»).
- **Estados (GP-227)**, los del selector de opciones y de todo lo pulsable: **pulsado**, una capa del 10 % de `gp_on_surface` (`color/tarjeta_capa`: el `rippleColor` de `Widget.GymProFit.Card` y los fondos `fondo_pulsable`, `fondo_pulsable_redondo` y `bg_circulo_superficie`, que el tema da a `selectableItemBackground`, `…Borderless` y «Volver»); antes la capa de la tarjeta era `gp_primary_container` opaco y con el foco tapaba el texto (3,55:1 y 2,26:1 en Historial). **Con foco** de teclado o DPAD, ninguna capa y un anillo de 2 dp (`selector_anillo_foco`) **por fuera** de lo enfocado, separado 2 dp de su borde y con su forma (el radio de la tarjeta o del botón de Material, un círculo si es un botón redondo, el de los chips si no), en el `colorOnSurface` de lo que hay detrás (dentro de la tarjeta protagonista, `gp_on_hero`). Lo pone `ui/widget/FocoVisible`, instalado en todas las pantallas desde `GymProFitApp`, en la capa de encima de la ventana; lo enfocado se desplaza a la vista con el anillo entero (un carrusel a sangre lo dejaba pegado al borde), el anillo la sigue mientras se desplaza y al tocar la pantalla se quita. Un icono suelto sin relleno («Volver», papeleras, el calendario de un campo) lo lleva por dentro, 2 dp hacia dentro de su borde: no tiene texto que pisar, y por fuera se salía de la pantalla o cruzaba el borde de su campo; con relleno (el «+» de la barra), por fuera. El anillo se recorta a lo que se ve de las listas que contienen lo enfocado, cada una solo en el eje en que se desplaza (un carrusel, por los lados; una lista, arriba y abajo), para no pintarse sobre la cabecera alrededor de una fila medio escondida sin dejar las tarjetas de un carrusel sin arriba ni abajo, y un carrusel se desplaza hasta su relleno para que la última tarjeta lo enseñe entero; además, un carrusel deja fuera de sus lados el sitio del anillo, porque sin relleno su borde es el de su primera y su última tarjeta («Tu día» y «Tus últimos entrenos» de Inicio). Lo enfocado vuelve a desplazarse tras cada layout mientras lleva el anillo: con el teclado abierto, el campo que pierde el foco pedía otro y el scroll lo alineaba sin el anillo. Un bloque pulsable sin relleno propio (la fila de una macro de Nutrición, la cabecera de «Tu programa») lleva 8 dp de aire dentro (`aire_anillo`; 4 arriba y abajo entre filas apiladas, `aire_anillo_alto`) y el mismo margen negativo fuera (`aire_anillo_fuera`), siempre lado a lado (`android:layout_margin` negativo se ignora), para que el texto no se mueva y el anillo no vaya pegado a él ni lo crucen sus esquinas. Por fuera porque por dentro no sirve: pisaba el texto de las filas sin aire, quedaba a 1,4:1 dentro de un botón relleno y no se distinguía del borde de una tarjeta oscura (revisor, GP-228). Sin anillo ni velo: lo que se desplaza (scrolls y listas) y los campos de texto, que tienen su borde de foco. Con el suyo propio (`R.id.tag_anillo_propio`): las opciones del selector y la pestaña de la barra, que como en Material 3 lo lleva alrededor de la píldora del icono (`anillo_pildora`). Un enlace de texto de cabecera de sección lleva 8 dp de aire a cada lado (su texto queda 8 dp por dentro del borde de la tarjeta). Medido: texto sobre la tarjeta con foco y pulsada a 4,5:1 o más en los dos temas, salvo el naranja pulsado en oscuro (4,40:1, solo mientras se pulsa); anillo a 3:1 o más contra lo que lo rodea (`TarjetaEstadosTest`). Lo de una hoja inferior o un diálogo (otra ventana) no lleva anillo todavía.
- **Cabecera de sección, fila de lista y avatar:** ver «Cimientos».
- **Texto sobre la marca:** `gp_on_primary_secondary` (`#F8ECE6` claro · `#482000` oscuro) para el texto secundario encima de un fondo `gp_primary`.
- **Tarjeta de récord** (`layout/fragment_inicio.xml`): `cardBackgroundColor` `gp_gold_container` y `strokeColor` `gp_gold`, con el trofeo.
- **Grupo de Ajustes:** una tarjeta de 24 dp con filas de 64 dp, icono en círculo de 40 y separadores con sangría de 70 (`GrupoAjustes`, `FilaAjuste`).

- **Ficha del resumen** (`item_ficha_resumen.xml`, `bg_ficha_resumen*.xml`): píldora informativa de 32 dp (16 dp de radio, `colorSurface`) con un icono de 18 dp; verde (`gp_success_container`) si la sesión sube frente a la anterior de la misma rutina. Bajar no se pinta de rojo.
- **Sesión en vivo** (`activity_sesion_en_vivo.xml`, `item_envivo_*.xml`, `view_descanso.xml`; GP-210 paso 3): cabecera con el tiempo en 56 sp que se encoge hasta caber (`autoSize`, 28–56 sp), el volumen (26 sp) y las series (22 sp) a su derecha, y la barra de la sesión debajo. Cada ejercicio lleva «Tu récord: 85 kg × 7» (trofeo y texto `gp_gold` / `gp_gold_text`, «, de hoy» si lo ha superado hoy) o, sin historial, «Primera vez: es tu punto de partida» en neutro; no sale mientras no se sabe la marca. El botón de marcar es un círculo de 48 dp teñido por estado (`bg_check`): gris `colorSurfaceVariant`, `colorPrimary` la primera sin marcar de la sesión y `gp_success_container` con su check `gp_success` si está hecha. La fila del récord va en `gp_gold_fila` y su aviso (`bg_aviso_record`: `gp_gold_container`, borde `gp_gold`, 14 dp) baja encima de las columnas. El panel del descanso (`bg_panel_descanso`, sin velo) lleva un aro de 72 dp (`CircularProgressIndicator`, 7 dp, `colorPrimary` sobre `gp_divider`) que se vacía, la cuenta atrás de 46 sp, «Siguiente: serie 4 · 85 kg × 7» y −15 s, +15 s, Saltar. Notificación: Live Update con barra por tramos (una por serie del ejercicio que toca) y «+15 s» y «Saltar».
- **Tarjeta de récords del resumen** (`activity_resumen_sesion.xml`): `Card.Estructura` con `gp_gold_container` y borde `gp_gold`; una fila `item_record` por récord, con la marca anterior debajo del nombre y las rayas en dorado al 30 %.
- **«Frente a la última vez»** (`item_ultima_vez.xml`): fila de 48 dp con el ejercicio, sus kilos y el % (verde si sube, neutro si baja o es igual); la primera vez lleva «Primera vez: es tu punto de partida» y ningún %. Entra en cascada (`CASCADA`).
- **«Para recuperar»** (`view_para_recuperar.xml`, `ui/nutricion/ParaRecuperarSeccion.java`): tarjeta con la proteína del día, su barra (`gp_macro_proteinas`, dorada al conseguirla) y hasta tres ideas con su «+» de 44 dp (`item_idea_recuperar.xml`), el mismo `Anadidos` de Añadir.
- **Barras de la semana** (`ui/widget/BarrasSemanaView.java`): siete barras con raya discontinua en el objetivo, la letra del día debajo y, en la proteína, un trazo naranja bajo los días entrenados. Vista dibujada a mano: lleva una sola descripción para TalkBack con los siete valores (DEC-019).
- **Tarjeta de los lunes** (`fragment_nutricion.xml`, `cardAvisoSemana`): en Nutrición, solo los lunes, lleva a la semana pasada; se cierra y vuelve el lunes siguiente.

### Inicio, Perfil y ajustes (GP-216, lote 1)

- **Avatar en las cuatro pestañas.** Inicio, Entrenar, Nutrición y Progreso llevan el avatar de 48 dp arriba a la derecha (`avatarCabecera`, `AvatarUtils.cabecera`) y abre Perfil y ajustes. Progreso pierde el engranaje.
- **Inicio** (`fragment_inicio.xml`, `ui/inicio/`): bocadillo de Profi (`bg_burbuja_profi`: esquina de la cola 4, las otras 16; solo sale con un dato real, `FraseProfi`) · «Hoy toca» (tarjeta protagonista con `SiluetaMuscularView` de 120 dp con las zonas del día y un carrusel de `item_hoy_ejercicio`; el icono de cada ejercicio es el de su zona, provisional) · «Tu día» (carrusel de `item_tu_dia`: aro de 72 dp de calorías y de proteína y `VasosAguaView` para el agua, y la fila de la comida que toca) · «Tus músculos esta semana» (silueta y una barra por zona, `item_zona_semana`) · «Tu progreso» (`BarrasKilosView`, kilos de las seis últimas semanas, y la tarjeta dorada del último récord, que crece hasta la progresión) · «Tus últimos entrenos» (carrusel de `item_ultimo_entreno`). Cada bloque de red usa `VistaEstado`: cargando, error con «Reintentar» y vacío, sin toast.
- **Vistas dibujadas a mano**: `BarrasKilosView` y `VasosAguaView` nacen con su descripción para TalkBack; los aros son `CircularProgressIndicator`.
- **Fila de Perfil y ajustes** (`view_fila_ajuste.xml`): una sola fila para las 22 de antes, con segunda línea, valor, interruptor, botón y flecha opcionales. Rótulo de grupo `Widget.GymProFit.RotuloGrupo` y grupo `GrupoAjustes.Pantalla` (tarjeta de 24 con filas de 64).
- **Tus datos** (`TusDatosActivity`): botones −/+ de 48 dp que repiten al mantenerlos (tocar el valor abre la hoja para escribirlo), el selector de opciones para sexo, actividad y nivel, cuatro opciones de objetivo (`item_objetivo_dato`) y, fija abajo, el plan recalculado antes de guardar con `CalculadoraNutricional` (DEC-054), con el mínimo eficaz de proteína. «Guardar» está apagado mientras nada cambie.
- **Requisitos al escribir** (`RequisitosUi`, `view_requisito`, `view_fortaleza`): contraseña nueva y correo nuevo. El estado se dice con el icono y con texto para TalkBack, no solo con el color.
- **Hoja de elegir** (`HojaElegir`, `dialog_hoja_elegir`, `item_opcion_hoja`): la misma para Tema (con miniatura de cada tema), Idioma, Vaso de agua y Descanso sin pauta; dos columnas, radio y borde naranja de 2 dp en la elegida; se aplica al tocar.
- **Eliminar cuenta**: botón relleno en `gp_eliminar_fondo` (#C62828) con texto blanco (5,62:1) en los dos temas, apagado hasta marcar la casilla; con la contraseña vacía, pulsar la pide.

### Entrenar, programa, rutina, Biblioteca y ejercicio (GP-216, lote 2)

- **Entrenar** (`fragment_entrenar.xml`, `view_tu_programa.xml`, `TuProgramaVista`): «Tu programa» en una tarjeta con «día N de M» (los programas son cíclicos: no hay semana), las hechas de la vuelta, una barra con un tramo por día y la **tira del ciclo** (`item_dia_programa`: círculo de 38 dp con las siglas de la rutina, `Siglas`; lleno si va hecha, con borde si es hoy, apagado si falta). «Hoy» es una tarjeta protagonista con `BotonExpresivo` de 64 dp (solo icono). «Rutinas del programa» y «Mis rutinas» son una tarjeta por rutina (`item_rutina_tarjeta`, desde GP-232; ver «Las tarjetas de sesión y de rutina»): cuándo toca arriba a la derecha (`CuandoTocaRutina`: Hoy en naranja, Siguiente, En N entrenos) o ▶ en las propias, zonas en frase, pauta, los kilos de la última vez con sus barras y «La última vez, hace 6 días». «Por tu cuenta» son dos tarjetas pequeñas; «Más programas», un carrusel de `item_programa_carrusel` con la franja del color de su material.
- **Color de material** (`gp_programa_gimnasio` #2A3A6A, `gp_programa_mancuernas` #7A3A10, `gp_programa_corporal` #0E5562): iguales en los dos temas, con texto #FFF4E8 encima. Cabecera del detalle del programa (degradado hasta el fondo) y franja del carrusel.
- **Detalle de programa** (`activity_programa_detalle.xml`): cabecera con datos en píldoras `bg_chip_cabecera`, «El ciclo», «De qué va» con «Leer más» a 3 líneas, rutinas plegables (una abierta cada vez) y «Seguir este programa» fijo abajo sobre `bg_barra_inferior`.
- **Detalle de rutina** (`activity_detalle_rutina.xml`): tarjeta protagonista con las series por zona (`item_zona_rutina`: pista `bg_pista_zona`, relleno naranja) y «La última vez»; cada ejercicio (`item_ejercicio_rutina`) con foto de 52 dp, pauta y descanso y «Última: 80 kg × 8», en dorado y con trofeo si fue el récord vigente (`UltimaMarca`). «Empezar» fijo abajo.
- **Biblioteca** (`activity_ejercicios.xml`): buscador en píldora de 56 dp, chips de grupo con icono (`Widget.GymProFit.Chip.Biblioteca`: 44 dp, naranja al elegir) y una sola lista que se desplaza: «Los que más haces» (`item_mas_haces`, marca en dorado de `/records`; solo sin filtros), cuántos hay y las filas `item_ejercicio` en una tarjeta (primera/media/última/sola). El nivel lleva un punto de color (`NivelColor`: verde, ámbar `gp_nivel_intermedio`, rojo) **y** su nombre.
- **Ficha del ejercicio** (`activity_detalle_ejercicio.xml`): demostración de 238 dp que funde `imagen_url` e `imagen_url_2` (con «Quitar animaciones», solo la primera; sin foto, la silueta de la zona), «Tu mejor marca» en dorado (`bg_marca_oro`) con el máximo estimado, pestañas «Tu progreso» y «Cómo se hace» y gráfica con período `4 sem. · 8 sem. · Todo` (`PeriodoProgreso`), los dos en el selector de opciones (las pestañas, con `selectorPestanas`; desde el lote 4) y «Tus últimas veces» (`item_vez_ejercicio`: una tarjeta por sesión con las series en barras; la del récord, en dorado; el «+50 kg» en `gp_success` sobre `gp_success_container`). Un período sin sesiones lo dice en el sitio de la gráfica («Sin sesiones en estas semanas. Prueba con «Todo».»), también a TalkBack. La pista «La próxima vez…» solo sale con la regla de los programas (`ProximaVez`: todas las series al máximo de la pauta → sube el peso); no se inventa cuánto.
- **Botón de volver flotante** (ficha, rutina y programa): `BarraSuperior` pone detrás un fondo del color de la pantalla en cuanto se desplaza, para que el contenido no se lea cortado por el círculo.
- **Transiciones de contenedor** (`Transiciones.abrirTarjeta`): la fila o tarjeta pulsada crece hasta la pantalla en rutina → detalle, ejercicio → ficha, programa → detalle y récord → progresión.

### Nutrición y apuntar comida (GP-216, lote 3)

- **Nutrición** (`fragment_nutricion.xml`, `item_comida_dia.xml`): de arriba abajo, tira de días, píldora de entreno, tarjeta del día, «Comidas» con el enlace «Copiar ayer» (solo hoy y con la comida de ayer a mano), el agua y «Tu semana». Cada comida enseña sus kcal a la derecha si está apuntada y el «+» si está pendiente; la que toca dice cuánta proteína falta. El agua dice los litros en `Cifra.Pequena` («1,5 L») con «de 2,8» de pie. «Tu semana» es siempre un resumen en dos columnas de cifras condensadas: «3 de 4» en `gp_macro_proteinas` (5,09:1 en claro y 6,68:1 en oscuro) con «días con tu proteína» debajo, y «2.240» con «kcal de media»; si con letra grande no caben, se apilan. Abre la semana y cuenta hasta ayer, o hasta hoy si el día ya está completo (un día a medias no es un día sin la proteína); el nombre accesible de «Copiar ayer» empieza por el texto visible y dice qué comida copia; los vasos del agua se pueden tocar y van tantos por fila como quepan a 48 dp (hasta nueve); la frase del día completo va dentro, en verde. Sin red, `estadoNutricion` (`VistaEstado`) ocupa el sitio de las cifras con «Reintentar». Mirando otro día, el rótulo de las kcal dice «para ese día» (`DiaNutricion.rotuloKcal`). El calendario de «Elegir otro día» lleva los contenedores de superficie en `gp_surface` (`ThemeOverlay.GymProFit.Calendario`), no el lavanda de Material. Los botones de copiar se anuncian empezando por su texto visible (`NombreVisibleTest`).
- **Comida apuntada** (`activity_comida.xml`): resumen en `Card.Protagonista` con la cifra de kcal a 48 sp, «34 % de tu día», `BarraReparto` (12 dp, tres tramos con hueco de 2 dp, colores de macro) y los gramos de cada macro en columnas (apiladas con letra 1,3); la línea verde «Comida alta en proteína» sale con 30 g o más, o con 15 g que sean el 30 % de las kcal (`ResumenComida.altaEnProteina`). «N alimentos» lleva «Guardar como plato» a la derecha y debajo, la pista de deslizar.
- **Comida vacía**: Profi señala (`drawable-nodpi/profi_senala_base.webp`) y su bocadillo (`bg_burbuja_profi`, `view_profi_copiar`) propone copiar la de ayer, sin el icono ni «Aún no has apuntado…» encima (salen solo si no hay nada que copiar o con «Hoy no»); debajo, «Para tus 48 g de proteína» con hasta tres ideas de `LoQueTeEncaja` (favoritos, tus platos, lo habitual) y su «+». La barra de abajo dice «Buscar otro alimento», en superficie 2: el naranja es de copiar. Al desplazar, una raya bajo la fecha separa la cabecera del contenido.
- **Añadir alimento**: la cabecera es la flecha de atrás y «Añadir a la merienda ⌄», que cambia de comida al tocarla; pestañas Recientes, Favoritos y Mis platos en el selector de opciones con semántica de pestañas, con «Lo que te encaja» arriba de las tres en un carrusel de una fila (`item_encaja_carrusel`, el patrón de Inicio con `sep_carrusel`): tarjetas de 220 dp (`tarjeta_carrusel`) con icono, «+», nombre, detalle y la barra de la proteína, todas a la altura de la más alta y con la barra al pie. Al tocar un «+» solo se repintan: el carrusel no se recrea. El buscador lleva el borde `sel_borde_campo` (naranja con foco, `gp_stroke` sin él) y el texto de ayuda en `gp_text_secondary` (el gris de Material daba 2,37:1 en claro); buscador, pestañas y tarjetas van a `margen_pantalla` (20), como las cabeceras de sección; cambiar de pestaña vuelve la lista arriba; las filas, el icono en su círculo (`Fila.Icono`); una comida reciente dice las kcal delante («576 kcal · Arroz, Pollo…») para que el corte no se las lleve. La barra de abajo suma kcal y proteína de lo añadido en la visita, dice cuánta falta de la del día (con una barra fina) y cierra con «Listo».
- **Ficha del alimento**: la cantidad va arriba (unidad, − y + con la cifra a 48 sp y sus gramos) con atajos ½, 1, 2 o 100, 150, 200 g (el marcado en `gp_primary_container`, los demás en superficie 2; no se recrean al marcar uno, para no perder el foco de TalkBack). La unidad, con dos, es el selector de opciones (con tres o más, el desplegable); − y + van de media en media ración o de 10 en 10 g, con el paso como mínimo, y la unidad junto a la cifra va en plural salvo con una. Las cuatro cifras usan `bg_cifra_ficha` (sin borde en oscuro) y cada macro va en su color; el botón fijo dice cuánto suma («Añadir a la merienda · 141 kcal»).
- **Pulido de GP-225**: los separadores de las filas de Añadir, de las comidas recientes, de las ideas de Comida y de las líneas de una comida van hasta el texto (70 dp, `Widget.GymProFit.Separador`), y las filas de Añadir siguen la fila de lista (16 a los lados, texto a 14 del icono). El buscador de Añadir y el de la Biblioteca miden lo mismo: `buscador_alto` (56 dp) y `buscador_radio` (28). La insignia de «datos revisados» tiene su token, `gp_revisado` (el azul de dato). «Lo que te encaja» lleva la cara de Profi y el título a 17 sp, encabezado para TalkBack. Los «+» de las filas, del carrusel y de las ideas se anuncian como botón (`RolBoton`). El bocadillo de Profi (`bg_burbuja_profi`, que dibuja `ui/widget/BurbujaProfi`) tiene cola a la izquierda, a media altura, y el borde de la tarjeta (1 dp en claro, ninguno en oscuro); la vista lleva 8 dp más de relleno a la izquierda (`cola_burbuja`). Los iconos de alimento van teñidos por su categoría (`Categorias.tenir`): proteína en azul, carbohidratos en ámbar, grasas en violeta, verduras en verde y bebidas en el azul del agua; un plato o lo que no tiene categoría, en el color del texto (todos pasan 3:1 sobre el círculo). El corazón de la ficha, a 18 dp del borde; sus cuatro cifras se dicen con «gramos». Los días de la tira de Nutrición miden 51 dp de ancho (2 dp de margen) y su fondo, el del elegido (`bg_dia_elegido`) y la capa de pulsar (`bg_dia_pulsable`, radio 16), va 2 dp hacia dentro: así el anillo del día de al lado no toca el borde naranja. Los chips de filtro de Programas, también los que se crean en código (`item_chip_filtro`), son `Widget.GymProFit.Chip`.
- **Escáner**: la hoja que sube lleva la píldora «¡Encontrado!» (`bg_pildora_encontrado`, contenedor de éxito), el icono del alimento y «Seguir escaneando».
- **Transiciones de contenedor** también desde la fila de una comida, la fila ya en ✓ de Añadir, la hoja del escáner y el ingrediente de un plato hacia la ficha.

### Progreso (GP-216, lote 4)

- **Una sola pantalla** (`fragment_progreso.xml`, `ui/progreso/`), como el tablero 16: cabecera con el avatar (sin pestañas ni el perfil de dentro) y, de arriba abajo, el resumen, «Kilos movidos», «Récords recientes», «Logros», «Tu cuerpo» e «Historial», a `separacion_pestana` (36). Cada «Ver todo» abre la lista completa en `SeccionProgresoActivity` (cabecera con «Volver» y el fragment de siempre: Récords, Logros, Medidas o Historial). Ahí llegan también las notificaciones de Progreso, «Anotar peso» del «+» (Medidas con el diálogo del peso abierto) y los enlaces de Inicio; siempre encima de Progreso, así que atrás vuelve a Progreso.
- **Carga**: seis peticiones a la vez (`PantallaProgreso`, cada una una `Fuente`), con la lista de sesiones una sola vez (resumen, «Todo» e historial). Cada sección tiene su `VistaEstado`: carga, vacío de cuenta nueva (sin ceros) y error con «Reintentar», que pide solo lo que falló. Al volver se recarga sin parpadear: lo que había se queda hasta que llega lo nuevo.
- **Resumen**: las tres cifras de `view_cifra_progreso` (la de récords en dorado), con plural; con todo a cero, el vacío. Su fondo es el de tarjeta pequeña: radio 20 y el borde de la tarjeta estándar, que en oscuro no existe (`bg_cifra_progreso`).
- **Kilos movidos** (`KilosProgreso`): el selector `4 sem. · 8 sem. · Todo` (abre en 8; «Todo», desde la semana de la primera sesión hasta 104), la cifra de esta semana en `Cifra.Pequena` con «kg esta semana» y, a la derecha, «el 62 % de la pasada» en apoyo mientras no la supera o «+12 %» en `gp_success` al superarla; sin kilos la pasada, nada (`KilosMovidos.avance`). Las barras son las de Inicio (`BarrasKilosView`): con muchas semanas el hueco no pasa del 30 % del ancho, las esquinas no pasan de media barra y las etiquetas («14 sep», el lunes) van cada tantas barras como haga falta para no pisarse, contando desde esta semana. TalkBack lee cada cifra hasta 8 semanas; con más, cuántas, la mayor y la de esta.
- **Récords recientes** y **Historial**: filas `item_fila_progreso` (la «Fila de lista» de Cimientos) en una tarjeta. Récord: el más reciente, trofeo en `gp_gold` sobre `bg_fila_icono_oro` y la marca en `gp_gold_text`; los demás, trofeo en contorno sobre `bg_fila_icono` y la marca en `gp_on_surface` (como en la lista de Récords, GP-225; DEC-018); el apoyo, «Hace 4 semanas · antes 82,5 kg × 10», y la marca en `TextAppearance.GymProFit.Headline` (con el cronómetro si es por tiempo); abre la ficha del ejercicio. Sesión (GP-232): fila compacta `item_sesion_fila`, sin icono (el ✓ verde igual en todas se fue), con la rutina, «Ayer, 13.640 kg y 1 récord» (el récord en `gp_gold_text`), las barras de fila a la derecha y la flecha; abre el resumen. **El tablero 16 pinta en dorado todas las filas de récord; desde el lote 7 (GP-225) el dorado va solo en el récord más reciente** (Medal Rule): es lo que manda. «Ver los N» con plural.
- **Logros**: carrusel a sangre (la sección sale del relleno de la columna con `margen_pantalla_fuera`, así que el carrusel se pinta y se toca hasta los bordes; su cabecera y su estado vuelven a `margen_pantalla`, y el carrusel lleva ese relleno con `clipToPadding` en falso) de `item_logro_carrusel` (120 dp de ancho): medallón de 72 dp (`logro_medallon`) con borde de 3 dp; conseguido, `bg_medallon_conseguido` (contenedor y borde dorados, glifo relleno en `gp_gold`); por conseguir, `bg_medallon_pendiente` (superficie y borde `gp_stroke`, glifo en contorno). Trazo continuo y no discontinuo como el tablero (hueco 11). Debajo, el nombre y «Conseguido» o «18 de 30 sesiones». Los dos últimos conseguidos y los dos más cerca (`LogrosProgreso`).
- **Tu cuerpo** (`CuerpoProgreso`): tarjeta pulsable que abre Medidas, con el último peso en `Cifra.Pequena`, el cambio en las 8 semanas que acaban en él («−2,7 kg en 8 semanas», neutro: subir o bajar no es bueno o malo por sí solo; `CambioPeso` dice las semanas que de verdad abarca), una línea de 90 dp en `gp_linea_peso` (el azul de dato del tablero, el tono de la proteína: 5,09:1 en claro y 6,68:1 en oscuro; no el naranja, que es de lo que se pulsa) (`linea_peso_alto`, un punto por día y a su distancia en días, sin ejes ni toques: lo que dice va en la descripción de la tarjeta, DEC-019), «Objetivo: …» y, solo con el aviso de progreso encendido y el permiso de notificaciones, «Te avisamos si pasa un mes sin pesarte». «Apuntar peso» abre el diálogo del peso aquí mismo y guarda una medición por día (`MedicionHoy`, la misma regla que Medidas).

- **Fechas y gráficas (GP-228)**: toda fecha que se enseña sale de `FechaUtils` en el idioma de la app (`localeDeLaApp`): fecha y hora del historial y de Medidas con el formato medio del idioma («30 sept 2026 18:05» / «Sep 30, 2026 6:05 PM», como lo da el formato de Android), fechas cortas con `inicio_fecha_corta_patron` («28 sept» / «Sep 28»). Los días relativos siguen una sola regla (`FechaUtils.diaRelativo`): «Hoy», «Ayer · lunes», «Domingo» (2 a 6 días), «Jueves pasado» (7 a 13) y la fecha corta; la usan Inicio, el historial de Progreso y «Tus últimas veces» de la ficha (bajo «Hoy» o «Ayer» no se repite «hace…»). Las tres gráficas con período en semanas (la línea de «Tu cuerpo», Medidas y la progresión de la ficha) van por fecha (`EjeFechas`): la x es el día, uno por día (el último pesaje del día; en la ficha, la mejor marca del día), y la etiqueta, la fecha corta de esa x; con un solo punto, solo su etiqueta. `FechasLocalizadasTest` falla si vuelve un patrón numérico («dd/MM») o `Locale.getDefault()` al formatear.
- **Nombres propios (GP-227)**: cada fila de Medidas dice su campo y su valor con la unidad entera («Peso, 79,2 kilos», «Grasa corporal, 18,5 por ciento», «Cintura, 84 centímetros»; sin valor, «Peso, Añadir»; `NombreMedida`); cada tarjeta del historial lleva la acción de TalkBack que dice qué borra («Borrar Full body B del 30 de septiembre», `sesiones_eliminar_a11y`); la papelera se fue en GP-232.
- **Gráficas con pocos datos (GP-229)**: las tres gráficas por fecha (la línea de «Tu cuerpo», Medidas y la progresión de la ficha) cuentan días (`EjeFechas.porDia`) y siguen `PuntosGrafica`: con 0, el vacío de cada pantalla (en Medidas, «Aún no has apuntado esta medida» o, si la hay fuera del rango, «Sin medidas en estas fechas. Prueba con «Todo»»); con 1, el punto, más grande, con su valor encima y su fecha en el eje, y la pista en `Apoyo` (en Medidas, arriba, sobre el área de la gráfica, que le deja su sitio con `setExtraTopOffset`: así el selector de rango no se mueve; en «Tu cuerpo» y la ficha, debajo) («Apunta otro peso para ver cómo cambias»; en la ficha, «La próxima vez verás tu progreso»); con 2 o más, la línea. La de Medidas va en `gp_linea_peso`, el azul de dato, como la de «Tu cuerpo». El tooltip (`ChartMarker`) va encima del punto y, si arriba no cabe, debajo: nunca lo tapa.
- **Récords vacíos (GP-229)**: sin entrenos, «Tus récords salen cuando superas una marca tuya. El primer entreno pone tu punto de partida»; con entrenos y sin récords, «Ya tienes tu punto de partida…» (`VacioRecords`; Progreso y la lista lo deciden con la lista de sesiones). Una fila de récord dice lo mismo en la lista y en Progreso (`FilaRecord`): «Hace 1 mes · antes …» y, por tiempo, «1:15 de tiempo». En «Tus últimas veces», la misma marca que la vez anterior lleva el chip «—», neutro. Las listas de «Ver todo» van a `margen_pantalla` (20) y los chevrones de Medidas, en `gp_stroke` (3:1), sin alpha.
- **Estadísticas de nutrición**: los gramos del donut van en su leyenda («Proteína · 88 g»), no dentro de las porciones, y las cuatro cifras de arriba, a la altura de la más alta de su fila.
- **Diálogos**: `ThemeOverlay.GymProFit.Dialog` define los contenedores de superficie (`gp_surface` y `gp_surface_2` el más alto) y apaga la elevación tonal (`elevationOverlayEnabled`, como el calendario); sin eso Material 3 los pintaba en lavanda o con un 11 % de naranja.

### Inputs / Fields
- `TextInputLayout` de Material (142 apariciones en `res/layout`), tematizado por `Theme.GymProFit` (`colorOutline` y la escala de la app). Error: `UIHelper.marcarError` pone el mensaje en el `TextInputLayout` (sin él, un toast) y lo quita al editar; `Movimiento.temblar` (momento 10) agita el campo o la tarjeta en `CrearAlimentoActivity` y `CrearPlatoActivity`. Contraseña con `ui/widget/CampoContrasena.java`.
- Selectores: `Spinner` con flecha Material Symbols y 48 dp de alto mínimo (`Widget.GymProFit.Spinner`); el selector de opciones para las que se excluyen (`ui/widget/SelectorPildora.java`, ver «Selector de opciones»). Su menú va en `bg_menu_desplegable` (superficie, borde `gp_stroke` y radio 12; en oscuro se confundía con el fondo) y, en Registrar sesión, cada opción es `item_desplegable` con la ✓ naranja en la elegida (`color/marca_elegida`).

### Navigation
- **Barra inferior** (`BarraNavegacion`): Inicio · Entrenar · «+» · Nutrición · Progreso. Pestaña activa con píldora `gp_primary_container` e icono relleno; el resto, icono en contorno y `gp_text_secondary`. El «+» abre las acciones rápidas y, abierto, gira a «×». Cada botón lleva nombre, rol de pestaña, posición y estado para TalkBack, y su tooltip.
- **Cabecera** de pantalla secundaria: `MaterialToolbar` de 56 dp (`Widget.GymProFit.Cabecera`, mínimo `cabecera_alto`: el `?attr/actionBarSize` de Material 3 son 64) con «Volver»; el botón de atrás se llama «Volver» en todas las pantallas.
- **Transición entre pantallas:** las de Material motion de «Motion» (contenedor que crece y eje X); el resto, el deslizamiento lateral con parallax (`GymProFit.WindowAnimation`, `res/anim/slide_*`). Entre pestañas, el fundido de GP-105.

### Feedback
- **Toast propio** con icono (`UIHelper.mostrarToastInfo|Exito|Error`, `layout/toast_custom.xml`), **Snackbar** de Material sobre `gp_surface_2` (`Widget.GymProFit.Snackbar`; con «Deshacer» en `gp_accion_inversa`) y **diálogo** M3 de 22 dp (`UIHelper.mostrarDialogo*`).
- **Cargando, error y vacío** dentro del contenido: `VistaEstado` (`layout/view_estado.xml`); ver «Huecos».
- **Aviso en superficie 2** (`layout/view_aviso_superficie.xml`, GP-223): arriba de la pantalla, en `gp_surface_2` con `radio_tarjeta_pequena`, icono de 24 dp en `colorOnSurfaceVariant` (decorativo) y texto de 14 sp; no se pulsa y es región viva. Va fuera del scroll, a `espacio_16` de los lados y `espacio_8` por debajo; bajo una cabecera con fondo propio (la de la sesión en vivo), también `espacio_8` por encima, para no pegarse a ella. Con el teclado abierto, ninguna pieza del scroll de debajo queda a medias en sus bordes —ni el título arriba ni un enlace abajo—: cada una se ve entera o queda fuera entera, con el campo con el foco entero (`ui/widget/PiezasSinCortar`). Lo usan Entrar con el candado («Tu sesión se ha cerrado…», cuando la sesión caduca al abrir la app o a mitad de uso) y la sesión en vivo con `cloud_off` («Sin conexión…», mientras no hay red).

### Las tarjetas de sesión y de rutina (GP-232)

Diseño aprobado el 04-11 en `documentacion/diseno/2026-11-04-tarjetas/` (sus tableros, en `fuente/`). Seis superficies: el Historial completo, «Tus últimos entrenos» de Inicio, el Historial de Progreso, las rutinas de Entrenar, la hoja «¿Qué rutina hoy?» y Programas. Todo sale de `GET /sesiones/historial` (una petición por página de semanas) y de las rutinas activas, que traen `musculos` y `ultimas`: ninguna tarjeta pide nada.

- **Cifra y barras.** Los kilos (Σ peso × reps, GP-011) en `Cifra.Pequena` (condensada 700 a 26 sp) con «kg» en Barlow a 15 sp (14 en el carrusel) en `gp_text_secondary`; a su lado, **las barras de tus últimas veces** (`ui/widget/BarrasVecesView`, lógica en `utils/BarrasVeces`): una por sesión de esa rutina, hasta 6, de la más antigua a esta, que va a la derecha. Empiezan en 0 (la más alta llena el alto). Esta sesión (en una rutina, la última vez) en `gp_primary`; la que batió un récord en `gp_gold`, también si es esta; las demás en `gp_stroke`. Tamaños (ancho, hueco, alto): 10, 6 y 36 dp en tarjeta (`barras_tarjeta_*`), 7, 4 y 28 en el carrusel de Inicio (`barras_carrusel_*`) y 5, 3 y 24 en las filas (`barras_fila_*`); una barra de 0 kg se queda en un filo de 4 dp (`barras_minimo`). Esquinas como `BarrasKilosView`: 8 dp arriba y 4 abajo, sin pasar de media barra. Decorativas para TalkBack: su resumen va en la frase de la tarjeta («Tus 6 últimas veces con esta rutina, de 11.020 a 13.640 kilos»). Contrastes sobre la tarjeta (claro y oscuro): esta sesión 5,63 y 5,81; récord 3,40 y 9,05; las demás 3,41 y 3,30.
- **La cifra, en cascada** (`utils/CifraTarjeta`, una sola decisión para las cinco superficies con cifra; decisión del propietario del 07-11: con los programas de peso corporal, una sesión a 0 kg es un tipo de usuario entero). Ninguna tarjeta enseña «0 kg» ni «0 series». Con kilos, los kilos con sus barras y la comparación. Sin kilos y con series, «3 series» en la misma condensada (plural de `tarjeta_series`), sin barras ni comparación; la línea de abajo solo minutos y récords (en Progreso, «29 sept, 3 series y 1 récord»; en Inicio, la línea destacada solo si hubo récord). Sin series, el aviso en `Cuerpo` y `gp_text_secondary`: «Sin ejercicios apuntados» si no hay ejercicios y «Sin series hechas» si los hay sin ninguna serie, con los minutos debajo (también en Inicio). Dentro de una frase el aviso va en minúscula («Hoy, sin series hechas», `CifraTarjeta.enFrase`). Una rutina cuya última vez fue a 0 kg enseña las series de esa vez, sin barras (las series vienen en `VezRutina.series`); de una vez no se sabe si tenía ejercicios, así que sin series dice «Sin series hechas». Una barra de 0 kg dentro de una rutina con kilos se queda en su filo. La cabecera de una semana sin kilos dice solo «3 entrenos». TalkBack dice «3 series», nunca «0 kilos».
- **Si no caben, bajan.** Kilos y barras van en `FilaQueBaja`: si con letra 1,3 no caben en una línea con 12 dp entre ellos, las barras bajan bajo los kilos, a 8 dp. Ningún texto se corta.
- **En frase, sin «·»** (`utils/FrasesTarjeta`, `utils/CuandoTarjeta`, `ui/tarjetas/TextosTarjeta`): «Pecho, espalda y brazos» (las zonas de `Zonas`, de más a menos series, hasta tres; en inglés con la coma de serie, «Chest, back, and arms»), «58 minutos, 18 series y 1 récord» (el récord en `gp_gold_text` y seminegrita), «7 ejercicios y 55 minutos», «La última vez, hace 6 días, con 2 récords», «Ayer, 18:05» y «Martes 3, 18:05» en el Historial completo (en inglés, «Tuesday 3, 6:05 PM», también sin el mes: lo da la cabecera), «Ayer, martes» en Inicio y Progreso. Solo dentro de estas tarjetas: el resto de la app sigue con sus separadores.
- **Frente a la anterior** (`utils/ComparacionSesion`): «3 % más que la anterior» en `gp_success` con `trending_up` (5,25 y 9,58); «2 % menos» con `trending_down` e «Igual que la anterior» (redondea a 0 %) con `trending_flat`, en neutro (`gp_text_secondary`, 6,68 y 7,05); la primera vez, «Primera vez con esta rutina» con la bandera, sin porcentaje. Un entrenamiento libre, una sesión sin kilos o con la anterior sin kilos no se comparan. Iconos a 18 dp.
- **Tarjeta de sesión** (`item_sesion`, Historial): rutina en `Headline` (condensada 600 a 20 sp; sin rutina, «Entrenamiento libre») y el cuándo arriba a la derecha; zonas; kilos con barras; comparación (14 sp, 600); minutos, series y récords. Libre: solo cifras. Sin kilos, la cascada de arriba.
- **Historial completo** (`fragment_historial`, `SesionAdapter`): por semanas, con su cabecera (`item_semana_historial`: «ESTA SEMANA» en el rótulo de sección y «2 entrenos y 22.050 kg», o «3 entrenos» sin kilos; encabezado para TalkBack, en frase y con el mes entero, que abreviado el sintetizador lo lee a medias: «Semana del 28 de septiembre», «Week of September 28»). Carga, error y vacío con `VistaEstado`, dentro de la sección y sin toast; la página siguiente, al acercarse al final, con su pie de carga o de error con «Reintentar». **Sin papelera**: deslizar a la izquierda (fondo `gp_error_container` con el radio de la tarjeta, la papelera y «Borrar») o la acción de TalkBack quitan la tarjeta y sale «Deshacer» 5 s (`QuitarConDeshacer`); el borrado se manda cuando se va el aviso. Desde el resumen del entreno se borra con la papelera de su cabecera, que sí pregunta.
- **Inicio** (`item_ultimo_entreno`): cuándo, rutina (hasta dos líneas), kilos con las barras del carrusel y una sola línea destacada: los récords con el trofeo en dorado o, si no, la comparación; bajo un aviso, los minutos en neutro y en `Apoyo.Pequeno` (no en la letra de la línea destacada: no destacan). Ya no sale «Sin récords». Todas las tarjetas del carrusel tienen el alto de la más alta (`match_parent` dentro de la fila, que las iguala). Para TalkBack, «345 kilos y 1 récord».
- **Progreso** (`item_sesion_fila`): ver «Progreso».
- **Rutina** (`item_rutina_tarjeta`, `ui/tarjetas/TarjetaRutina`): cuándo toca arriba a la derecha («Hoy» en `gp_primary`) o ▶ de 48 dp sobre superficie 2; zonas; pauta; kilos de la última vez con barras; «La última vez, …» con sus récords. Sin hacer todavía: «Sin hacer todavía» en el sitio de los kilos, sin barras. Las zonas de las del programa salen del catálogo (`ZonasDelDia`); las de las propias, de `musculos`.
- **«¿Qué rutina hoy?»** (`item_elegir_rutina`): la que elige `HoyToca` va la primera, con «Toca hoy» (`bg_chip_zona_hecha`, 22 dp como mínimo y 2 dp arriba y abajo, para que a 1,3 la letra no toque el borde) justo detrás del nombre (`ConstraintLayout` con la cadena empaquetada a la izquierda: el nombre se encoge y el chip no se va a la derecha); zonas, «La última vez, …», barras de fila (solo si la última vez tuvo kilos) y ▶ en `gp_primary`.
- **Programas** (`item_programa`, `item_programa_recomendado`): el recomendado en la tarjeta protagonista (`gp_hero`); cada uno con el nombre en condensada 700 (22 sp; 26 el recomendado), el punto de su nivel (`NivelColor`) y «Intermedio, 4 días, de 50 a 55 minutos», «Lo sigues» en el que sigues, justo detrás del nombre como «Toca hoy» (en el recomendado, `bg_chip_hero`: el borde de 1 dp de las siglas en `gp_on_hero_variant` y el texto en `gp_on_hero`, porque el fondo del chip de siempre es el de `gp_hero` en oscuro), y la tira de su ciclo: círculos de 36 dp (`sigla_ciclo`, `bg_sigla_ciclo`; con borde `gp_on_hero_variant` en el recomendado) con las `Siglas.deCiclo` de `Programa.semana` y `autorenew`. En un ciclo dos rutinas distintas nunca comparten siglas: si chocan, cada una lleva su inicial y la primera letra en que se distingue («Push» PS y «Pull» PL; antes las dos eran PU). El punto de nivel del recomendado lleva el color del tema oscuro en los dos temas (`NivelColor.sobreHero`): el de claro sobre `gp_hero` daba 2,92. Sin barras.
- **Movimiento**, una sola vez al entrar la tarjeta (`ui/tarjetas/EntradaTarjeta` recuerda las que ya entraron, así que al desplazar no se repite): las barras crecen desde abajo una tras otra (`BARRA`, escalón `CASCADA_ESCALON`), los kilos cuentan (`Movimiento.contar` en `CIFRAS`, momento 7) y la barra dorada brilla una vez al acabar (`BRILLO`, hasta ×1,45 de luz). Al tocar, `Transiciones.abrirTarjeta`; borrar y deshacer, con `SALE` y `RECOLOCA` (momento 20). Ninguna duración nueva; con «Quitar animaciones», todo quieto en su estado final.
- **TalkBack**: cada tarjeta se lee de una vez («Pierna A, ayer a las 18:05. Pierna y abdomen. 13.640 kilos, 3 % más que la anterior. 58 minutos, 18 series y 1 récord. Tus 6 últimas veces con esta rutina, de 11.020 a 13.640 kilos.»); lo de dentro no se recorre suelto. Si la primera y la última barra tienen los mismos kilos, «…, la primera y la última con 1.475 kilos».

### El arranque (GP-223)
- **Una sola pantalla de carga.** `MainActivity` es la de entrada (`Theme.GymProFit.Arranque`, API SplashScreen de `core-splashscreen`): el splash del sistema, en `ic_launcher_fondo` (#0B0C0E) también con la app en claro, como el icono, y con la G sin fondo de icono a 160 dp en un icono de 288 (`arranque_icono`), dentro del círculo de 192. Debajo, `PantallaArranque` pone `view_arranque` con la misma G (`VistaG`) en el mismo sitio y al mismo tamaño, centrada en la ventana entera. **El traspaso va a una réplica propia**: el splash no se suelta antes de que la G haya entrado entera, y en la escucha de salida la pantalla de carga pasa a la decoración de la ventana, por encima de todo, con su G estática (el último fotograma del AVD, mismo viewport) casada con el centro del icono de la copia. En Android 12 o más **la copia se queda debajo** hasta que se ha entregado el fotograma que pinta la réplica en su sitio y ha pasado otro, o 1 s como mucho (`TraspasoSplash`): al quitarla, la shell suelta el icono en el acto, sin esperar a ningún fotograma de la app. La salida y el montaje de Inicio no empiezan con la copia puesta. Si el splash no avisa al irse, el respaldo lo da por ido a los 2 s del primer fotograma entregado de la app (por debajo de Android 12, a los 600 ms de pedir la salida), y lo deja en el log y en la traza. La salida y los errores pasan en la réplica. Sin escucha, el sistema funde las dos ventanas y la G se atenúa. El brillo de la G sigue al cambiar la pantalla de carga de padre (`VistaG` lo retoma donde iba). Se infla con la configuración de noche y `ThemeOverlay.GymProFit.Arranque`: los colores son los del tema oscuro sin un hex a mano; los iconos de las barras pasan a claros mientras está.
- **La G** (`drawable/g_arranque`, `g_arranque_inicial`, `g_arranque_animada`): dos trazados sacados con potrace de `documentacion/marca/logo.png`, la fuente de la marca (fuera del APK), el cuerpo en `logo_g_cuerpo` (#EBBB3D) y el trazo en `logo_g_trazo` (#FF6100), los colores del PNG. Fidelidad medida en el informe de GP-223.
- **«Conectando…»**: Barlow a 15 sp (`text_arranque_estado`) en `gp_text_secondary` del oscuro (#A1A9B6), a 28 dp de la G (`espacio_28`), región viva («Conectando» para TalkBack).
- **Errores** («Sin conexión», «No conseguimos conectar»): la G a 72 dp (`arranque_g_error`), el título en Barlow Condensed a 30 sp (`text_arranque_titulo`, `colorOnSurface`, encabezado, con el foco de TalkBack) y el texto en Barlow a 16 sp en `gp_text_secondary`, a 300 dp como mucho (`arranque_texto_max`); «Reintentar» abajo, `Widget.GymProFit.Button` de 52 dp (`arranque_boton_alto`) a `margen_pantalla` de los lados y 32 dp (`arranque_boton_abajo`) sobre la barra del sistema.
- **El brillo**: una franja de `gp_arranque_brillo` (blanco al 40 %) recortada por la G con `SRC_ATOP` sobre una capa.

### Signature components
- **Anillos de macros** (`ui/alta/AnilloMacros.java`, `ui/nutricion/AnilloDia.java`, `AnilloComida.java`): arcos por macro con los colores `gp_macro_*`; se cierran y se ponen verdes con el día completo.
- **Mapa muscular** (`ui/widget/SiluetaMuscularView.java`, `body_silhouette`, `body_highlight`): silueta neutra y músculo en naranja.
- **Hoja del «+»** y hojas inferiores: velo `gp_velo` y hoja que sube con `HOJA`. Las `BottomSheetDialog` llevan `ThemeOverlay.GymProFit.Hoja` (en el tema, `bottomSheetDialogTheme`): fondo `colorSurface`, sin tinte de elevación y las esquinas de 28 dp de Material; sin él salían en lavanda (GP-065). La de seguir un programa tiene un alto mínimo del 85 % de la ventana: su vista previa cambia de alto con los minutos y, ajustada al contenido, la hoja crecía y movía el selector bajo el dedo; su aviso va en `bg_aviso_hoja` (superficie 2).
- **Marco del escáner** (`ui/nutricion/MarcoEscaner.java`): esquinas que pasan a verde al leer.
- **Iconos:** Material Symbols Rounded, peso 400, 24 dp, 128 vectores `drawable/ic_ms_*` importados sin redibujar de `@material-symbols/svg-400`, teñidos por el tema; cada icono trae variante `_fill` para el estado activo. Los iconos anatómicos `ic_body_*` son propios.

## Motion

El movimiento sale de `Movimiento` (`utils/Movimiento.java`) y de nada más: las pantallas piden un momento, no escriben milisegundos ni curvas (DEC-039). **Nativo**: animadores, interpoladores, vectores y la capa superpuesta de las vistas; ni Lottie ni librerías de animación (decidido el 22-10-2026). `MovimientoTest` lee la tabla de DEC-039 y el código y falla si los números no coinciden.

**Curvas** (`Movimiento.java:48-58`; la estándar también en `res/interpolator/estandar.xml`):

| Curva | Valor | Para |
|---|---|---|
| `ENFATIZADA` | `cubic-bezier(.05,.7,.1,1)` | lo que entra |
| `ESTANDAR` | `cubic-bezier(.2,0,0,1)` | lo que cambia de sitio o de tamaño |
| `REBOTE` | `cubic-bezier(.34,1.56,.64,1)` | lo que salta |
| `TIEMBLA` | `cubic-bezier(.36,.07,.19,.97)` | el error |
| `LINEAL` | lineal | lo que gira |
| `ACELERADA` | `cubic-bezier(.3,0,.8,.15)` | lo que sale y se va (la G del arranque, GP-223) |

**Duraciones** (`Movimiento.java:64-199`, ms): `TOQUE` 120 · `RELLENO` 150 · `CHECK` 260 · `ICONO` 420 · `ENCIENDE` 380 · `ENTRA` 350 (+40 por escalón) · `CAPITULO` 450 · `LATIDO` 900 · `BARRA` 500 · `DESTELLO` 700 · `PILDORA` 320 · `DIA` 280 (+35) · `CIFRAS` 1200 · `HALO` 900 · `CARTA` 450 · `FILA` 300 (+100) · `BRILLO` 900 · `CIRCULO` 300 · `GIRO` 700 · `TRAZO` 420 · `TEMBLOR` 380 · `MENSAJE` 250 · `AVISO` 5000 · `TROFEO` 450 · `CHISPAS` 700 · `HUNDE` 70 · `VERDE` 120 · `CASCADA` 350 (+40) · `BARRIDO` 2400 · `ENCAJA` 200 · `HOJA` 450 · `PLIEGA` 180 · `CAE` 360 (+120) · `CORAZON` 450 · `RESALTA` 1400 · `SALE` 380 · `RECOLOCA` 250 · `CIFRA` 250 · `VELO` 250 · `SALTA_OK` 450 (+200) · `CIERRA` 600 · `PUNTO` 450 · `VASO` 450 · `RESPIRA` 1400 · `SUMA` 600 · `ORO` 900 · `BAJA` 250 · `G_CUERPO` 450 · `G_TRAZO` 420 (tras `G_TRAZO_RETRASO` 280) · `G_ENTRADA` 700 · `G_SALE` 300 · `G_BRILLO` 1200 (cada `G_BRILLO_CADA` 1800) · `CONECTANDO` 200 · `G_SUBE` 350.

**Momentos** (tabla completa en DEC-039; aquí el resumen por familia):

| Familia | Momentos | Curvas |
|---|---|---|
| Elegir y encender | 1 Elegir, 2 «Siguiente», 6 Selector, 15 «+» a ✓, 19 Corazón | estándar → rebote |
| Entrar en pantalla | 3 Entrar, 8 El programa llega, 16 Cascada, 23 Plato | enfatizada, con escalón |
| Progreso y cifras | 5 Barra, 7 Cifras que cuentan, 21 Día completo, 22 Un vaso más, 24 Serie marcada | estándar y rebote; cifras con `1 − (1 − t)³` |
| Éxito | 9 Cuenta creada, 12 Récord, 17 Código leído, 25 Récord en mitad de la serie, 28 El resumen del entreno | rebote, enfatizada |
| Error | 10 Error | temblar + mensaje |
| Salida | 18 Copia de ayer, 20 Borrar y deshacer | estándar |
| Descanso | 26 El descanso empieza y acaba (27 es la notificación del sistema) | enfatizada al subir, estándar al bajar, aro lineal |
| Arranque (GP-223, filas sin número) | La G entra, La G sale, El brillo, «Conectando…», La G sube | estándar y rebote al entrar, acelerada al salir, estándar al subir |

**El arranque** (GP-223). La G entra en el splash del sistema (`drawable/g_arranque_animada`, Android 12 o más; en las anteriores, quieta: `g_arranque`): el cuerpo gira de −90° a 0°, crece de 0,72 a 1 y aparece en `G_CUERPO` con la estándar; el trazo llega desde 18 dp arriba a la derecha (`G_TRAZO_DESDE_DP`, sobre una G de 160) y crece de 0,6 a 1 en `G_TRAZO` con el rebote, `G_TRAZO_RETRASO` después, con el pivote en el 74,9 % y el 20 % del logo; su opacidad va con la estándar, porque un `fillAlpha` por encima de 1 se sale del byte de alfa. Sus tiempos y curvas viven en `res/animator/g_*`, `res/interpolator/` y el tema, que no pueden pedir un momento: `GArranqueTest` los cruza con `Movimiento`. La entrada se ve entera (`G_ENTRADA`), salvo con «Quitar animaciones», y es lo único que el arranque espera. A 1 s sin destino, el brillo (`G_BRILLO`, cada `G_BRILLO_CADA`); a 3 s, «Conectando…» (`CONECTANDO`). La salida es la misma vaya a donde vaya: la G crece al 125 % (`G_SALE_ESCALA`) y se desvanece en `G_SALE` con la acelerada; hacia Inicio, el fondo se abre en círculo (`FondoArranque`) y Inicio entra en cascada (`Movimiento.cascada`, `CASCADA` cada `CASCADA_ESCALON`); hacia otra pantalla, la pantalla llega con un fundido (`anim/arranque_entra`, `ENTRA` con la enfatizada) sobre la de carga, que se queda quieta debajo (`anim/arranque_quieta`), y Entrar entra en cascada y la sesión en vivo sube 40 dp (`SESION_SUBE_DP`) con su aviso bajando 12 (`AVISO_BAJA_DP`). En un error, la G sube y pasa a 72 dp (`G_SUBE`, `G_ERROR_DP`).

**Transiciones** (Material motion de Material 1.14, `utils/Transiciones.java`; filas sin número de DEC-039). Se piden con una línea desde el origen y el destino no sabe nada: `GymProFitApp` monta su lado de llegada al crearlo.

| Caso | Transición | Tiempo y curva |
|---|---|---|
| Tarjeta → detalle: rutina, sesión del historial, alimento → ficha, récord → progresión | contenedor que crece (`MaterialContainerTransform`, `Transiciones.abrirTarjeta`) | 400 ms enfatizada al abrir (`CONTENEDOR_ABRE`) y 350 ms estándar al volver (`CONTENEDOR_VUELVE`) |
| Avanzar en un flujo: alta (entre pasos y hacia «Guarda tu plan»), crear rutina, seguir un programa | eje X compartido, 30 dp (`MaterialSharedAxis`, `Transiciones.avanzar`) | 300 ms estándar (`EJE_X`) |
| Entre pestañas | el fundido de GP-105 | — |
| Atrás predictivo en una hoja propia (la del «+», la del plato guardado) | la hoja sigue al dedo hasta el 90 %, con esquinas de 28 dp (`AtrasHoja`) | al soltar, 250 ms estándar (`ATRAS_SUELTA`) |

Con «Quitar animaciones» nada de esto se mueve: `Transiciones` abre la pantalla sin transición de escena. `windowActivityTransitions` está activo en el tema, pero solo llegan con transición las pantallas que la piden; las hojas con `BottomSheetDialog` (alta, elegir rutina, seguir programa, elegir comida) llevan el atrás predictivo de Material. `res/anim/tab_*` se borraron: no las usaba nadie.

**Hápticos** (`Movimiento.vibrar`, `Movimiento.java:226-245`; por `performHapticFeedback`, así que respetan el ajuste de vibración al tocar): `LIGERA` → `CLOCK_TICK` (elegir, «+», copiar); `MEDIA` → `CONTEXT_CLICK` (abrir capítulo); `EXITO` → `CONFIRM` en Android 11+ (`LONG_PRESS` antes): cuenta creada, récord, código leído, guardar plato, día completo (una vez al día); `ERROR` → `REJECT` en Android 11+ (`LONG_PRESS` antes).

**Reglas:**
- Con «Quitar animaciones», todo en su estado final: se anima siempre *hacia* el estado final; lo que va en bucle pregunta a `Movimiento.quieto()` y se queda en su último fotograma. Las vibraciones se quedan.
- Nada espera a una animación: ningún botón se apaga mientras algo se mueve, y lo que entra se puede tocar desde el primer fotograma.
- Sin dependencias nuevas.

## Do's and Don'ts

### Do:
- **Do** leer colores de `@color/gp_*` o `?attr/color*`, y tamaños de `@dimen/text_*` (`values/colors.xml`, `dimens.xml`).
- **Do** usar los cimientos: tarjeta `Widget.GymProFit.Card` (sin borde en oscuro), espaciado `@dimen/espacio_*` y radios `@dimen/radio_*`.
- **Do** dar a toda zona pulsable 48 dp como mínimo y nombre accesible (DEC-019), y a las vistas dibujadas a mano su accesibilidad desde el principio.
- **Do** poner cada cadena visible en `values/strings.xml` y `values-en/strings.xml` con plurales donde se cuenta (DEC-020).
- **Do** pedir un momento a `Movimiento` en vez de escribir una duración o una curva (DEC-039).
- **Do** comprobar la pantalla en claro y oscuro, en ES y EN y con letra 1,3 antes de darla por terminada (DEC-022).

### Don't:
- **Don't** usar el dorado para nada que no sea un récord o un logro (DEC-018).
- **Don't** introducir un segundo acento ni cambiar el naranja entre temas.
- **Don't** usar emojis como iconos ni en la interfaz.
- **Don't** escribir un `sp`, un hex, un radio o una elevación a mano en un layout, ni dejar un chip suelto o un grupo de botones donde las opciones se excluyen (va el selector de opciones).
- **Don't** añadir Lottie ni librerías de animación (decidido el 22-10-2026).
- **Don't** apoyar un dato solo en el color: lleva icono, texto o forma.

## Huecos

Lo que hoy es incoherente o no existe. Aquí no se arregla nada.

1. **Estados de vacío, error y carga sin sistema (GP-064).** Existe `VistaEstado` (`layout/view_estado.xml`, `utils/VistaEstado.java`) y la usan 10 layouts, pero hay 7 pantallas con su propio vacío a mano (`activity_ejercicios`, `activity_programas`, `activity_resumen_sesion`, `fragment_entrenar`, `fragment_logros`, `fragment_medidas`, `fragment_records`, con ids distintos: `tvVacio`, `tvEmpty`, `layoutVacio`, `layoutRutinasVacio`…). La carga es un `ProgressBar` redondo o un diálogo modal (`LoadingDialog`); no hay esqueletos. El error es sobre todo un toast (84 usos de toast frente a 5 de Snackbar), no un estado en pantalla.
2. **Animaciones a mano sin sistema.** Fuera de `Movimiento` quedan duraciones y curvas escritas a pelo: `GuardaPlanActivity` (250 ms), `SemanaEjemplo` (220), `PasoPlanFragment` (700 con 1100 de retraso), `BarraNavegacion` (`config_shortAnimTime`, `DecelerateInterpolator`) y el deslizamiento entre pantallas en `res/anim/slide_*` (las `tab_*` ya se borraron). DEC-039 ya dice que las de antes del alta siguen con lo suyo hasta que se toquen.
3. **Material motion y Expressive, a medias.** Ya están el contenedor que crece, el eje X, el atrás predictivo de las hojas propias, el botón que cambia de forma, el selector de opciones (que ocupa el sitio de los botones conectados) y el indicador de carga con forma (ver «Cimientos» y «Motion»). Faltan `MaterialFadeThrough` entre pestañas (se queda el fundido de GP-105 por decisión), Dynamic Color y el resto de variantes Expressive de Material 1.14.0. Detalles que quedan: el contenedor que crece no cubre los alimentos que se abren desde Comida, Escáner o Crear plato (solo Añadir), ni el «Ver» de la tarjeta de último récord de Inicio; el eje X de «seguir un programa» llega hasta el detalle, no hasta Entrenar.
4. **Profi, en pocos sitios.** Desde GP-216 (lote 1) sale en Inicio (el bocadillo), en Tus datos y en Notificaciones (cuando falta el permiso), con `drawable-nodpi/profi_cara.webp`. Sigue pendiente en el alta, las pantallas vacías, los logros y «Tu semana». Profi solo va donde lo dibuja un tablero.
5. **La escala de espaciado y de radios existe, pero no está aplicada a todos los layouts.** Los tokens están (ver «Cimientos»), pero los layouts anteriores siguen con márgenes sueltos (6, 10, 14…) hasta que GP-216 rehaga cada pantalla en su lote; los radios de tarjeta, botón, hoja y avatar sí están llevados a la escala. Las medidas «del lienzo» viven aún en estilos sueltos (`estilos_estructura.xml`, `estilos_alta.xml`).
6. **Contraste del borde de la tarjeta en claro.** La tarjeta estándar en claro se delimita con `outlineVariant` (#C9C9C2, 1,66:1 contra blanco), no con `gp_stroke` a 3:1. Es decisión del paso 2 (el borde no delimita un control) y `PaletaContrasteTest` ya no mide la tarjeta tintada; si algún día se exige 3:1 a la tarjeta, se sube el color del borde en `values/colors.xml`.
7. **Alias de color heredados sin retirar** (`primary_light`, `golden`, `orange_dark`, `text_primary_dark`, `green_chip`, `red_chip`…), con valores que ya no coinciden en parte con los `gp_*`.
8. **Contenido de base de datos sin traducir** (DEC-020): 752 de 873 ejercicios con el nombre igual en ES y EN, y también el material de cada ejercicio («Barra», «Sin equipo»), sus instrucciones y los nombres y descripciones de los programas (la de «Torso y pierna» cita lunes y martes aunque el programa es cíclico). Se ve en Entrenar, la Biblioteca, la ficha y los detalles de programa y rutina.
9. **Títulos grandes y letra 1,3: no es un fallo nuestro.** Ver «The System Curve Rule» en Typography. Se queda aquí solo para que el medidor, que divide por la escala lineal, no lo vuelva a dar por defecto.
10. **Chips que no se han convertido.** Siguen siendo chips los filtros largos y desplazables (Ejercicios, las cuatro listas de administración, Añadir ejercicios, equipamiento y días de Programas) y la ficha del resumen; solo pasan a botones conectados las opciones que se excluyen y son pocas.
11. **Borde discontinuo sin documentar.** La tarjeta del fundador (`drawable/bg_tarjeta_fundador.xml`), el botón discontinuo (`bg_boton_discontinuo.xml`) y «nueva rutina» (`bg_nueva_rutina.xml`) usan trazo discontinuo, una tercera forma de borde que «Components» no recoge y que el diseño de Inicio aprobado no tiene.
13. **Datos que el tablero 14/29–32 dibuja y la API no tiene (GP-216, lote 2).** Sin «semana 3 de 8» ni días de la semana en el programa (es cíclico: «día 2 de 4»); sin «Añadir a una rutina», «favoritos» ni el rótulo «Bajar · subir» de la ficha del ejercicio; sin filtro de material en la Biblioteca (el botón «Filtros» del tablero no se hizo: la API solo filtra por grupo y nivel); sin «Compartir» en el programa; «Más opciones» de la rutina se queda en «Editar». Los iconos de los chips de grupo son provisionales.
12. **Inicio con errores de carga (resuelto en GP-216, lote 1).** Cada bloque dice su fallo dentro con `VistaEstado` y «Reintentar»; ya no hay toast duplicado.
14. **Datos que los tableros 15, 16 y 24 a 28 dibujan y la API no tiene (GP-216, lotes 3 y 4).** Sin ideas de proteína para una cuenta nueva: salen de lo tuyo (favoritos, platos, lo habitual), no de un catálogo de recomendados. Sin la hora de la comida («14:10») en la cabecera de la comida apuntada. Sin «Merienda de ayer» y «Crear alimento» como botones bajo Recientes: la copia de comidas ya está en «Comidas recientes» y crear, en Mis platos y en «¿No lo encuentras?». La barra de Añadir cuenta la proteína que falta del día, no de la comida. En Progreso (tablero 16): sin logros de racha («4 semanas seguidas») ni de récords («50 récords»), porque `TipoLogro` solo tiene sesiones, ejercicios y objetivos: salen los reales; sin día fijo para pesarse («el día 1 de cada mes»): la API avisa cuando pasan 30 días desde la última medición, y eso es lo que se dice; los kilos por sesión ya no se piden fila a fila: salen de `GET /sesiones/historial` (GP-232).
