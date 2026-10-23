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

`Widget.GymProFit.Card`: superficie `colorSurface` (#1B1E24 en oscuro), radio 24 y **elevación 0**. En oscuro **sin borde**: se distingue por el tono contra el fondo #0B0C0E. En claro, borde de 1 dp con `colorOutlineVariant` (`tarjeta_borde` vale 1 dp en `values/` y 0 en `values-night/`). Esto corrige el «borde, no sombra» de GP-209. Las tarjetas dibujadas a mano (grupos de filas, hoja del escáner) usan `gp_tarjeta_borde`, que en oscuro es el color de la propia superficie. `Card.Pequena` baja el radio a 20.

### Tarjeta protagonista

`Widget.GymProFit.Card.Protagonista`, para «Hoy toca» y los resúmenes: fondo #3A2110 (`gp_hero`), radio 28, sin borde, título en #FFF4E8 (`gp_on_hero`, 13,78:1) y apoyo en #FFD3B0 (`gp_on_hero_variant`, 10,82:1). Es igual en los dos temas. Lleva su propio tema (`ThemeOverlay.GymProFit.Hero`): lo de dentro lee texto, apoyo y borde de botón de la tarjeta, y el botón principal es el naranja de oscuro (#FF6A00 sobre #3A2110, 5,21:1; el #B83E00 de claro daba 2,1:1 y el botón se perdía).

### Cabecera de sección

`Widget.GymProFit.CabeceraSeccion` (fila) con `.Titulo` (20 sp, Barlow SemiBold, `TextAppearance.GymProFit.Seccion`) y, opcional a la derecha, `.Enlace` (15 sp, Barlow 600, en `colorPrimary`, 44 dp de alto). No sustituye al rótulo en mayúsculas de dentro de una tarjeta (`Widget.GymProFit.Seccion`, 13 sp), que se queda.

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

`BotonExpresivo` (`ui/widget`) es el botón principal que cambia de forma al pulsarlo: las esquinas pasan de 26 a 12 dp en 120 ms con la curva estándar (`Movimiento.TOQUE`) y vuelven al soltar. Lo son «Empezar» (Inicio, Entrenar y «Tu programa»), «Hecho» (resumen y barra de Añadir) y el «+» de la barra (`BotonExpresivo.alPulsar`). **Botones conectados** (`MaterialButtonToggleGroup`, `Widget.GymProFit.GrupoConectado` y `BotonConectado`) en vez de chips sueltos cuando las opciones se excluyen y son pocas: período de las estadísticas de nutrición, nivel al crear y editar una rutina, métrica y rango de Medidas y minutos al seguir un programa. Los filtros largos y desplazables se quedan en chips. **Indicador de carga con forma** (`LoadingIndicator`) solo en esperas cortas dentro de un botón o una tarjeta: «Crear cuenta», el guardado de la sesión y el programa del alta.

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

**Escala de letra y ancho.** Todo en `sp`; los controles de ancho fijo no cortan el texto con letra 1,3 (el chip de zona «nunca parte su texto»; las pestañas de Progreso van en `wrap_content`, GP-128).

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
- **Pressed:** hundimiento del 3 % en 120 ms con la curva estándar (`animator/toque.xml`, momento 1 de DEC-039); en `BotonExpresivo`, además, las esquinas pasan de 26 a 12 dp en esos mismos 120 ms.
- **«Respira»:** el botón principal crece un 3 % una vez para decir «aquí» (`Movimiento.respirar`): Inicio, Bienvenida, Avisos y el plan del alta.

### Chips
- **Botones conectados** (`MaterialButtonToggleGroup`, `Widget.GymProFit.GrupoConectado` y `BotonConectado`): opciones que se excluyen y son pocas (período, nivel, métrica y rango, minutos). El marcado lleva `gp_primary_container` y su texto, como el chip. Los filtros largos se quedan en chips.
- **Filtro** (`Widget.GymProFit.Chip`): 12 dp, borde 1 dp `gp_stroke`; marcado = fondo `gp_primary_container` con check naranja, en reposo `gp_surface_2` (`color/chip_background_selector.xml`). En hojas, sin relleno (`Chip.Hoja`, GP-134).
- **Zona de «Esta semana»** (`Widget.GymProFit.ChipZona`, `item_chip_zona.xml`): 44 dp, 12 dp, nunca parte el texto; informativo, sin nombre propio para TalkBack (lo lleva el grupo). Hecha = relleno `colorPrimaryContainer` (`bg_chip_zona_hecha`); pendiente = sin relleno y borde de 1 dp en `colorOutline` (`bg_chip_zona_pendiente`).

### Cards / Containers
- **Corner Style:** 24 dp (`Card`), 20 (`Card.Pequena`) o 28 (`Card.Protagonista`).
- **Background:** `colorSurface`; la protagonista, #3A2110. Elevación 0; borde de 1 dp en `colorOutlineVariant` solo en claro (ver «Cimientos»).
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
- **Tus datos** (`TusDatosActivity`): botones −/+ de 48 dp que repiten al mantenerlos (tocar el valor abre la hoja para escribirlo), botones conectados para sexo, actividad y nivel, cuatro opciones de objetivo (`item_objetivo_dato`) y, fija abajo, el plan recalculado antes de guardar con `CalculadoraNutricional` (DEC-054), con el mínimo eficaz de proteína. «Guardar» está apagado mientras nada cambie.
- **Requisitos al escribir** (`RequisitosUi`, `view_requisito`, `view_fortaleza`): contraseña nueva y correo nuevo. El estado se dice con el icono y con texto para TalkBack, no solo con el color.
- **Hoja de elegir** (`HojaElegir`, `dialog_hoja_elegir`, `item_opcion_hoja`): la misma para Tema (con miniatura de cada tema), Idioma, Vaso de agua y Descanso sin pauta; dos columnas, radio y borde naranja de 2 dp en la elegida; se aplica al tocar.
- **Eliminar cuenta**: botón relleno en `gp_eliminar_fondo` (#C62828) con texto blanco (5,62:1) en los dos temas, apagado hasta marcar la casilla; con la contraseña vacía, pulsar la pide.

### Inputs / Fields
- `TextInputLayout` de Material (142 apariciones en `res/layout`), tematizado por `Theme.GymProFit` (`colorOutline` y la escala de la app). Error: `UIHelper.marcarError` pone el mensaje en el `TextInputLayout` (sin él, un toast) y lo quita al editar; `Movimiento.temblar` (momento 10) agita el campo o la tarjeta en `CrearAlimentoActivity` y `CrearPlatoActivity`. Contraseña con `ui/widget/CampoContrasena.java`.
- Selectores: `Spinner` con flecha Material Symbols y 48 dp de alto mínimo (`Widget.GymProFit.Spinner`); píldora deslizante para opciones (`ui/alta/SelectorPildora.java`).

### Navigation
- **Barra inferior** (`BarraNavegacion`): Inicio · Entrenar · «+» · Nutrición · Progreso. Pestaña activa con píldora `gp_primary_container` e icono relleno; el resto, icono en contorno y `gp_text_secondary`. El «+» abre las acciones rápidas y, abierto, gira a «×». Cada botón lleva nombre, rol de pestaña, posición y estado para TalkBack, y su tooltip.
- **Cabecera** de pantalla secundaria: `MaterialToolbar` de 56 dp (`Widget.GymProFit.Cabecera`) con «Volver».
- **Transición entre pantallas:** las de Material motion de «Motion» (contenedor que crece y eje X); el resto, el deslizamiento lateral con parallax (`GymProFit.WindowAnimation`, `res/anim/slide_*`). Entre pestañas, el fundido de GP-105.

### Feedback
- **Toast propio** con icono (`UIHelper.mostrarToastInfo|Exito|Error`, `layout/toast_custom.xml`), **Snackbar** de Material sobre `gp_surface_2` (`Widget.GymProFit.Snackbar`; con «Deshacer» en `gp_accion_inversa`) y **diálogo** M3 de 22 dp (`UIHelper.mostrarDialogo*`).
- **Cargando, error y vacío** dentro del contenido: `VistaEstado` (`layout/view_estado.xml`); ver «Huecos».

### Signature components
- **Anillos de macros** (`ui/alta/AnilloMacros.java`, `ui/nutricion/AnilloDia.java`, `AnilloComida.java`): arcos por macro con los colores `gp_macro_*`; se cierran y se ponen verdes con el día completo.
- **Mapa muscular** (`ui/widget/SiluetaMuscularView.java`, `body_silhouette`, `body_highlight`): silueta neutra y músculo en naranja.
- **Hoja del «+»** y hojas inferiores: velo `gp_velo` y hoja que sube con `HOJA`.
- **Marco del escáner** (`ui/nutricion/MarcoEscaner.java`): esquinas que pasan a verde al leer.
- **Iconos:** Material Symbols Rounded, peso 400, 24 dp, 126 vectores `drawable/ic_ms_*` importados sin redibujar de `@material-symbols/svg-400`, teñidos por el tema; cada icono trae variante `_fill` para el estado activo. Los iconos anatómicos `ic_body_*` son propios.

## Motion

El movimiento sale de `Movimiento` (`utils/Movimiento.java`) y de nada más: las pantallas piden un momento, no escriben milisegundos ni curvas (DEC-039). **Nativo**: animadores, interpoladores, vectores y la capa superpuesta de las vistas; ni Lottie ni librerías de animación (decidido el 22-10-2026). `MovimientoTest` lee la tabla de DEC-039 y el código y falla si los números no coinciden.

**Curvas** (`Movimiento.java:48-56`; la estándar también en `res/interpolator/estandar.xml`):

| Curva | Valor | Para |
|---|---|---|
| `ENFATIZADA` | `cubic-bezier(.05,.7,.1,1)` | lo que entra |
| `ESTANDAR` | `cubic-bezier(.2,0,0,1)` | lo que cambia de sitio o de tamaño |
| `REBOTE` | `cubic-bezier(.34,1.56,.64,1)` | lo que salta |
| `TIEMBLA` | `cubic-bezier(.36,.07,.19,.97)` | el error |
| `LINEAL` | lineal | lo que gira |

**Duraciones** (`Movimiento.java:62-160`, ms): `TOQUE` 120 · `RELLENO` 150 · `CHECK` 260 · `ICONO` 420 · `ENCIENDE` 380 · `ENTRA` 350 (+40 por escalón) · `CAPITULO` 450 · `LATIDO` 900 · `BARRA` 500 · `DESTELLO` 700 · `PILDORA` 320 · `DIA` 280 (+35) · `CIFRAS` 1200 · `HALO` 900 · `CARTA` 450 · `FILA` 300 (+100) · `BRILLO` 900 · `CIRCULO` 300 · `GIRO` 700 · `TRAZO` 420 · `TEMBLOR` 380 · `MENSAJE` 250 · `AVISO` 5000 · `TROFEO` 450 · `CHISPAS` 700 · `HUNDE` 70 · `VERDE` 120 · `CASCADA` 350 (+40) · `BARRIDO` 2400 · `ENCAJA` 200 · `HOJA` 450 · `PLIEGA` 180 · `CAE` 360 (+120) · `CORAZON` 450 · `RESALTA` 1400 · `SALE` 380 · `RECOLOCA` 250 · `CIFRA` 250 · `VELO` 250 · `SALTA_OK` 450 (+200) · `CIERRA` 600 · `PUNTO` 450 · `VASO` 450 · `RESPIRA` 1400 · `SUMA` 600 · `ORO` 900 · `BAJA` 250.

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

**Transiciones** (Material motion de Material 1.14, `utils/Transiciones.java`; filas sin número de DEC-039). Se piden con una línea desde el origen y el destino no sabe nada: `GymProFitApp` monta su lado de llegada al crearlo.

| Caso | Transición | Tiempo y curva |
|---|---|---|
| Tarjeta → detalle: rutina, sesión del historial, alimento → ficha, récord → progresión | contenedor que crece (`MaterialContainerTransform`, `Transiciones.abrirTarjeta`) | 400 ms enfatizada al abrir (`CONTENEDOR_ABRE`) y 350 ms estándar al volver (`CONTENEDOR_VUELVE`) |
| Avanzar en un flujo: alta (entre pasos y hacia «Guarda tu plan»), crear rutina, seguir un programa | eje X compartido, 30 dp (`MaterialSharedAxis`, `Transiciones.avanzar`) | 300 ms estándar (`EJE_X`) |
| Entre pestañas | el fundido de GP-105 | — |
| Atrás predictivo en una hoja propia (la del «+», la del plato guardado) | la hoja sigue al dedo hasta el 90 %, con esquinas de 28 dp (`AtrasHoja`) | al soltar, 250 ms estándar (`ATRAS_SUELTA`) |

Con «Quitar animaciones» nada de esto se mueve: `Transiciones` abre la pantalla sin transición de escena. `windowActivityTransitions` está activo en el tema, pero solo llegan con transición las pantallas que la piden; las hojas con `BottomSheetDialog` (alta, elegir rutina, seguir programa, elegir comida) llevan el atrás predictivo de Material. `res/anim/tab_*` se borraron: no las usaba nadie.

**Hápticos** (`Movimiento.vibrar`, `Movimiento.java:174-197`; por `performHapticFeedback`, así que respetan el ajuste de vibración al tocar): `LIGERA` → `CLOCK_TICK` (elegir, «+», copiar); `MEDIA` → `CONTEXT_CLICK` (abrir capítulo); `EXITO` → `CONFIRM` en Android 11+ (`LONG_PRESS` antes): cuenta creada, récord, código leído, guardar plato, día completo (una vez al día); `ERROR` → `REJECT` en Android 11+ (`LONG_PRESS` antes).

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
- **Don't** escribir un `sp`, un hex, un radio o una elevación a mano en un layout, ni dejar un chip suelto donde las opciones se excluyen (botones conectados).
- **Don't** añadir Lottie ni librerías de animación (decidido el 22-10-2026).
- **Don't** apoyar un dato solo en el color: lleva icono, texto o forma.

## Huecos

Lo que hoy es incoherente o no existe. Aquí no se arregla nada.

1. **Estados de vacío, error y carga sin sistema (GP-064).** Existe `VistaEstado` (`layout/view_estado.xml`, `utils/VistaEstado.java`) y la usan 10 layouts, pero hay 8 pantallas con su propio vacío a mano (`activity_ejercicios`, `activity_programas`, `activity_resumen_sesion`, `fragment_entrenar`, `fragment_historial`, `fragment_logros`, `fragment_medidas`, `fragment_records`, con ids distintos: `tvVacio`, `tvEmpty`, `layoutVacio`, `layoutRutinasVacio`…). La carga es un `ProgressBar` redondo o un diálogo modal (`LoadingDialog`); no hay esqueletos. El error es sobre todo un toast (84 usos de toast frente a 5 de Snackbar), no un estado en pantalla.
2. **Animaciones a mano sin sistema.** Fuera de `Movimiento` quedan duraciones y curvas escritas a pelo: `GuardaPlanActivity` (250 ms), `SplashActivity` (800 ms con `AccelerateDecelerateInterpolator`), `SelectorPildora` (200), `SemanaEjemplo` (220), `PasoPlanFragment` (700 con 1100 de retraso), `BarraNavegacion` (`config_shortAnimTime`, `DecelerateInterpolator`) y el deslizamiento entre pantallas en `res/anim/slide_*` (las `tab_*` ya se borraron). DEC-039 ya dice que las de antes del alta siguen con lo suyo hasta que se toquen.
3. **Material motion y Expressive, a medias.** Ya están el contenedor que crece, el eje X, el atrás predictivo de las hojas propias, el botón que cambia de forma, los botones conectados y el indicador de carga con forma (ver «Cimientos» y «Motion»). Faltan `MaterialFadeThrough` entre pestañas (se queda el fundido de GP-105 por decisión), Dynamic Color y el resto de variantes Expressive de Material 1.14.0. Detalles que quedan: el contenedor que crece no cubre los alimentos que se abren desde Comida, Escáner o Crear plato (solo Añadir), ni el «Ver» de la tarjeta de último récord de Inicio; el eje X de «seguir un programa» llega hasta el detalle, no hasta Entrenar.
4. **Profi, en pocos sitios.** Desde GP-216 (lote 1) sale en Inicio (el bocadillo), en Tus datos y en Notificaciones (cuando falta el permiso), con `drawable-nodpi/profi_cara.webp`. Sigue pendiente en el alta, las pantallas vacías, los logros y «Tu semana». Profi solo va donde lo dibuja un tablero.
5. **La escala de espaciado y de radios existe, pero no está aplicada a todos los layouts.** Los tokens están (ver «Cimientos»), pero los layouts anteriores siguen con márgenes sueltos (6, 10, 14…) hasta que GP-216 rehaga cada pantalla en su lote; los radios de tarjeta, botón, hoja y avatar sí están llevados a la escala. Las medidas «del lienzo» viven aún en estilos sueltos (`estilos_estructura.xml`, `estilos_alta.xml`).
6. **Contraste del borde de la tarjeta en claro.** La tarjeta estándar en claro se delimita con `outlineVariant` (#C9C9C2, 1,66:1 contra blanco), no con `gp_stroke` a 3:1. Es decisión del paso 2 (el borde no delimita un control) y `PaletaContrasteTest` ya no mide la tarjeta tintada; si algún día se exige 3:1 a la tarjeta, se sube el color del borde en `values/colors.xml`.
7. **Alias de color heredados sin retirar** (`primary_light`, `golden`, `orange_dark`, `text_primary_dark`, `green_chip`, `red_chip`…), con valores que ya no coinciden en parte con los `gp_*`.
8. **Contenido de base de datos sin traducir** (DEC-020): 752 de 873 ejercicios con el nombre igual en ES y EN. Se ve en la pantalla de Entrenar.
9. **Títulos grandes y letra 1,3: no es un fallo nuestro.** Ver «The System Curve Rule» en Typography. Se queda aquí solo para que el medidor, que divide por la escala lineal, no lo vuelva a dar por defecto.
10. **Chips que no se han convertido.** Siguen siendo chips los filtros largos y desplazables (Ejercicios, las cuatro listas de administración, Añadir ejercicios, equipamiento y días de Programas) y la ficha del resumen; solo pasan a botones conectados las opciones que se excluyen y son pocas.
11. **Borde discontinuo sin documentar.** La tarjeta del fundador (`drawable/bg_tarjeta_fundador.xml`), el botón discontinuo (`bg_boton_discontinuo.xml`) y «nueva rutina» (`bg_nueva_rutina.xml`) usan trazo discontinuo, una tercera forma de borde que «Components» no recoge y que el diseño de Inicio aprobado no tiene.
12. **Inicio con errores de carga (resuelto en GP-216, lote 1).** Cada bloque dice su fallo dentro con `VistaEstado` y «Reintentar»; ya no hay toast duplicado.
