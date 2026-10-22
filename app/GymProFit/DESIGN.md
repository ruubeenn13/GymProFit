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
  number:
    fontFamily: "Barlow Condensed"
    fontSize: "44px"
    fontWeight: 700
    letterSpacing: "0em"
  display-large:
    fontFamily: "Barlow Condensed"
    fontSize: "30px"
    fontWeight: 700
    letterSpacing: "0.01em"
  display:
    fontFamily: "Barlow Condensed"
    fontSize: "26px"
  headline:
    fontFamily: "Barlow Condensed"
    fontSize: "22px"
    fontWeight: 600
    letterSpacing: "0.01em"
  title:
    fontFamily: "Barlow Condensed"
    fontSize: "19px"
  subtitle:
    fontFamily: "Barlow Condensed"
    fontSize: "17px"
  fila:
    fontFamily: "Barlow"
    fontSize: "16px"
  body:
    fontFamily: "Barlow"
    fontSize: "15px"
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
  # Valores declarados en values/themes.xml, values/estilos_estructura.xml y drawable/
  chip: "12px"
  button: "14px"
  card: "18px"
  card-estructura: "20px"
  dialog: "22px"
  pill: "24px"
  circle: "9999px"   # cornerSize 50% en Android (ShapeAppearance.GymProFit.Circulo)
spacing:
  # NO hay escala declarada (ver «Huecos»). Son los valores más repetidos en res/layout
  # (android:padding*/layout_margin*): 16dp ×398, 8dp ×284, 12dp ×257, 4dp ×216, 14dp ×124.
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
components:
  # Referencias al tema oscuro, que es el de por defecto; en claro se cambia -dark por -light.
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary-dark}"
    typography: "{typography.fila}"
    rounded: "{rounded.pill}"
    height: "48px"
  button-outline:
    backgroundColor: "{colors.surface-dark}"
    textColor: "{colors.on-surface-dark}"
    typography: "{typography.body}"
    rounded: "{rounded.pill}"
    height: "48px"
  card:
    backgroundColor: "{colors.surface-dark}"
    rounded: "{rounded.card}"
  card-estructura:
    backgroundColor: "{colors.surface-dark}"
    rounded: "{rounded.card-estructura}"
  card-record:
    backgroundColor: "{colors.gold-container-dark}"
    textColor: "{colors.gold-dark}"
    rounded: "{rounded.card-estructura}"
  chip-zona-hecha:
    backgroundColor: "{colors.primary-container-dark}"
    rounded: "{rounded.chip}"
    height: "44px"
  chip-zona-pendiente:
    rounded: "{rounded.chip}"
    height: "44px"
  fila-accion:
    backgroundColor: "{colors.surface-dark}"
    textColor: "{colors.on-surface-dark}"
    typography: "{typography.fila}"
    height: "60px"
  fila-ajuste:
    backgroundColor: "{colors.surface-dark}"
    textColor: "{colors.on-surface-dark}"
    typography: "{typography.body}"
    rounded: "{rounded.card}"
    height: "52px"
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

Documenta lo que **existe** en `app/GymProFit` el 22-10-2026; no rediseña nada. Contexto del producto en [PRODUCT.md](PRODUCT.md). Las decisiones que mandan: DEC-018 (oscuro, naranja, dorado), DEC-019 (accesibilidad), DEC-020 (ES y EN) y DEC-039 (movimiento), en `documentacion/PRODUCT-DECISIONS.md`. Los diseños aprobados, en `documentacion/diseno/` (el de referencia para estructura: `2026-09-27-estructura/`). **Si un lienzo y un token no coinciden, manda el token** (sus contrastes están medidos, GP-063).

Quien cambie un token o un patrón en `res/` o en `Movimiento.java` actualiza este fichero (regla en `CLAUDE.md`).

## Overview

**Creative North Star: "El cuaderno del atleta"**

La app se lee como un cuaderno de entreno: los datos primero, las cifras grandes y condensadas sobre un carbón casi negro, y una sola voz de color, el naranja, que dice dónde se pulsa o qué toca hoy. El dorado es una medalla: aparece solo cuando algo se ha conseguido (un récord, un logro) y por eso significa algo. Las tarjetas se delimitan con un borde medido, no con sombras, y el movimiento es corto, nativo y nunca hace esperar.

Es una interfaz de uso, no de exhibición (modo *Operate*): se usa entre series, con una mano, así que manda la legibilidad y la zona pulsable. El carácter está en los detalles: la tipografía condensada en cifras y títulos, el hundimiento de un 3 % al tocar, el trofeo que salta.

**Key Characteristics:**
- Oscuro por defecto; el claro es un tema completo con los mismos roles, no una inversión (DEC-018).
- Un único acento, el naranja, que es el mismo en los dos temas (en claro se oscurece a `#B83E00` para llegar a 4,5:1).
- Dorado reservado a récords y logros.
- Cifras y títulos en Barlow Condensed; el texto de lectura en Barlow de ancho normal.
- Material Components 1.14.0 sobre vistas XML, tematizados con tokens `gp_*` (Material 3 estructura; la marca va en el tema).
- Sin emojis; iconos Material Symbols Rounded teñidos por el tema.
- Sin estética gaming, sin exceso de efectos (DEC-018).

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

**Character:** una familia en dos anchos. La condensada es contundente y lleva cifras, títulos y etiquetas; el texto de lectura va en ancho normal porque a tamaño de lectura la condensada cierra las contraformas (`values/themes.xml`, comentario de GP-089). Todo en `sp`; ningún texto baja de 13 sp.

### Hierarchy
Los tamaños son los once escalones de `values/dimens.xml` (más `envivo_descanso_numero` 46 sp). Los estilos M3 los llevan a los componentes de Material (`Font.GymProFit.*`, `values/themes.xml`).

| Rol | Familia · tamaño | Dónde (fichero) |
|---|---|---|
| Cifra héroe | Condensada · `text_number_hero` 56 sp | La cifra protagonista de una pantalla |
| Cifra | Condensada 700 · `text_number` 44 sp | `TextAppearance.GymProFit.Number` y `Font.GymProFit.DisplayLarge`: kcal de hoy, peso, estadísticas |
| Título de pantalla grande | Condensada 700 · `text_display_large` 30 sp, interletraje 0,01 | `TextAppearance.GymProFit.Display`; título de las cuatro pestañas (`Widget.GymProFit.TituloPestana`) |
| Título de pantalla | Condensada · `text_display` 26 sp | `Font.GymProFit.DisplaySmall` y `HeadlineLarge` |
| Titular de tarjeta | Condensada 600 · `text_headline` 22 sp | `TextAppearance.GymProFit.Headline`; título de la cabecera (`Widget.GymProFit.Cabecera`) |
| Cabecera de sección | Condensada · `text_title` 19 sp | `Font.GymProFit.TitleLarge` |
| Título de fila o tarjeta | Condensada · `text_subtitle` 17 sp | `Font.GymProFit.TitleMedium`; nombre en `GymProFit.MealTitle` |
| Fila de lista | Barlow · `text_fila` 16 sp | Hoja del «+», comidas, plantillas, récords, botón píldora |
| Cuerpo | Barlow · `text_body` 15 sp | El tamaño más usado; `Font.GymProFit.BodyLarge` |
| Secundario | Barlow · `text_caption` 14 sp | Descripciones y metadatos; `Font.GymProFit.BodyMedium` |
| Etiqueta | Condensada · `text_label` 14 sp, MAYÚSCULAS, interletraje 0,12 | `TextAppearance.GymProFit.Label`; botones y chips (`Font.GymProFit.LabelLarge`) |
| Rótulo de sección | Condensada, negrita · `text_eyebrow` 13 sp, interletraje 0,08 | `Widget.GymProFit.Seccion` («ESTA SEMANA», «NUTRICIÓN DE HOY»); suelo de la escala |

### Named Rules
**The No-Raw-sp Rule.** Ningún layout escribe un `sp` a mano: usa un `@dimen/text_*`. Hubo 21 tamaños distintos antes de la escala (`dimens.xml`, cabecera).
**The Floor Rule.** Nada baja de 13 sp (GP-089). El contenido de lectura no baja de 15 sp.
**The Two Widths Rule.** Cifras, títulos y etiquetas en condensada; todo texto que se lea de corrido, en Barlow normal.

## Layout

Una pantalla es una columna de tarjetas sobre el fondo. Las cuatro pestañas (Inicio, Entrenar, Nutrición, Progreso) comparten cabecera: título de 30 sp, botón redondo de 48 dp a la derecha, 20 dp a los lados, 18 arriba y 6 abajo (`Widget.GymProFit.CabeceraPestana`, `values/estilos_estructura.xml`). Las pantallas secundarias llevan una sola cabecera, la `MaterialToolbar` con «Volver» (`Widget.GymProFit.Cabecera`; `CabeceraTest` vigila que no vuelva la fila hecha a mano).

La navegación principal es una barra fija inferior de cuatro pestañas más el «+» central (`ui/widget/BarraNavegacion.java`, mínimo 60 dp). El hueco de la barra de gestos se suma ahí y en ningún otro sitio. Las etiquetas crecen con la letra del sistema hasta 1,3.

**Espaciado.** No hay escala declarada. Los valores que de verdad se usan en `res/layout` (de más a menos): 16 dp (×398), 8 (×284), 12 (×257), 4 (×216), 14 (×124), 6 (×105), 10 (×96), 24 (×81), 20 (×73), 2 (×65). Los de las piezas repetidas: cabecera de pestaña 20/18/6; fila de acción 60 dp de alto con 12 dp a los lados; fila de ajuste 52 dp con 14; icono de comida 44 dp con un icono de 24; tarjeta de comida con 14 dp de relleno.

**Zonas pulsables.** 48 dp como mínimo en todo (DEC-019): `Widget.GymProFit.BotonIcono` y `BotonRedondo` miden 48 dp; el botón principal tiene `minHeight` de 52 dp (`Widget.GymProFit.Button`) y el píldora, de 48 dp; el chip de zona mide 44 dp pero es informativo (`importantForAccessibility="no"` en `item_chip_zona.xml`; la descripción la lleva el grupo). El medidor de depuración (`depuracion/MedidorAccesibilidad.java`) marca las que bajan de 48.

**Escala de letra y ancho.** Todo en `sp`; los controles de ancho fijo no cortan el texto con letra 1,3 (el chip de zona «nunca parte su texto»; las pestañas de Progreso van en `wrap_content`, GP-128).

## Elevation & Depth

**Plana: la separa el borde, no la sombra.** `Widget.GymProFit.Card` (`values/themes.xml`) declara 1 dp de elevación y un borde de 1 dp en `gp_stroke`; en claro esa pizca tiñe la tarjeta de `#FBF5F2`, en oscuro no se percibe y el trabajo lo hace el borde. `Widget.GymProFit.Card.Estructura` (`values/estilos_estructura.xml`) la deja en 0 dp porque la elevación tonal de M3 tiñe de naranja la tarjeta en oscuro; en `res/layout` 21 tarjetas fijan además `cardElevation="0dp"`.

**Decidido, pendiente (GP-210, paso 2):** la regla es «borde, no sombra», así que `Widget.GymProFit.Card` pasa a 0 dp con su borde de 1 dp en el paso 2 de GP-210 (transiciones y Expressive). Hasta entonces el 1 dp de arriba es lo que hay y el hueco 6 queda abierto.

### Shadow Vocabulary
- **Menú emergente** (`android:popupElevation` 12 dp, `values/themes.xml`, `Widget.GymProFit.PopupMenu`): fondo `gp_surface`, radio 16 y borde `gp_stroke` (`drawable/bg_popup_menu.xml`).
- **Aviso propio** (`layout/toast_custom.xml`, 4 dp) y **vista de la bienvenida** (`layout/view_demo_bienvenida.xml:152`, 8 dp) son los únicos elevados a mano además de la hoja de Añadir (`layout/activity_anadir_alimento.xml:227`, 8 dp).
- **Velo del «+»** (`gp_velo`, negro al 62 %) para separar la hoja del contenido, sin sombra.

### Named Rules
**The Border-Not-Shadow Rule.** Una tarjeta se delimita con su borde de 1 dp en `gp_stroke` (3:1), nunca con una sombra que en oscuro no se ve.

## Shapes

Esquinas redondeadas con un radio por familia de pieza, todos declarados en estilos o drawables:

| Pieza | Radio | Fuente |
|---|---|---|
| Chip (filtro y zona) | 12 dp | `Widget.GymProFit.Chip` (`chipCornerRadius`), `Widget.GymProFit.ChipZona` |
| Botón de Material | 14 dp | `Widget.GymProFit.Button` (`cornerRadius`) |
| Tarjeta | 18 dp | `Widget.GymProFit.Card`; también el grupo de Ajustes (`bg_grupo_ajustes`) |
| Tarjeta de la estructura | 20 dp | `Widget.GymProFit.Card.Estructura` |
| Diálogo | 22 dp | `ThemeOverlay.GymProFit.Dialog` (`dialogCornerRadius`) |
| Botón píldora | 24 dp | `Widget.GymProFit.BotonPildora`, `BotonPildora.Contorno` |
| Botón redondo, avatar | 50 % | `ShapeAppearance.GymProFit.Circulo` |

En `res/drawable` y `res/layout` los radios más repetidos son 12 (×36), 16 (×24), 20 (×13), 24 (×9). Hay otros sueltos (4, 10, 13, 17, 26). El borde es siempre de 1 dp (`strokeWidth`, `chipStrokeWidth`, `<stroke>`).

## Components

Lo que está en uso. Todo se tematiza por `Theme.GymProFit` (`values/themes.xml`): los widgets de Material 3 toman estilo de ahí (`materialCardViewStyle`, `materialButtonStyle`, `chipStyle`, `bottomNavigationStyle`, `floatingActionButtonStyle`, `snackbarStyle`…).

### Buttons
- **Shape:** píldora de 24 dp (`BotonPildora`, `values/estilos_estructura.xml`); el botón base de Material, 14 dp con 52 dp de alto mínimo (`Widget.GymProFit.Button`).
- **Primary:** relleno `gp_primary`, texto `gp_on_primary`, Barlow 16 sp, sin mayúsculas, 48 dp de alto.
- **Outline:** `Widget.GymProFit.BotonPildora.Contorno`, borde `colorOutline`, texto `colorOnSurface`, Barlow 15 sp negrita («Cambiar», zonas de la biblioteca en `BotonZona`).
- **Icon / round:** `BotonIcono` (48 dp, sin fondo) y `BotonRedondo` (48 dp, fondo `bg_circulo_superficie`).
- **Pressed:** hundimiento del 3 % en 120 ms con la curva estándar (`animator/toque.xml`, momento 1 de DEC-039).
- **«Respira»:** el botón principal crece un 3 % una vez para decir «aquí» (`Movimiento.respirar`): Inicio, Bienvenida, Avisos y el plan del alta.

### Chips
- **Filtro** (`Widget.GymProFit.Chip`): 12 dp, borde 1 dp `gp_stroke`; marcado = fondo `gp_primary_container` con check naranja, en reposo `gp_surface_2` (`color/chip_background_selector.xml`). En hojas, sin relleno (`Chip.Hoja`, GP-134).
- **Zona de «Esta semana»** (`Widget.GymProFit.ChipZona`, `item_chip_zona.xml`): 44 dp, 12 dp, nunca parte el texto; informativo, sin nombre propio para TalkBack (lo lleva el grupo). Hecha = relleno `colorPrimaryContainer` (`bg_chip_zona_hecha`); pendiente = sin relleno y borde de 1 dp en `colorOutline` (`bg_chip_zona_pendiente`).

### Cards / Containers
- **Corner Style:** 18 dp (`Card`) o 20 dp (`Card.Estructura`).
- **Background:** `colorSurface`. Borde 1 dp `gp_stroke`. Sin sombra apreciable (ver Elevation).
- **Texto sobre la marca:** `gp_on_primary_secondary` (`#F8ECE6` claro · `#482000` oscuro) para el texto secundario encima de un fondo `gp_primary`.
- **Tarjeta de récord** (`layout/fragment_inicio.xml`): `cardBackgroundColor` `gp_gold_container` y `strokeColor` `gp_gold`, con el trofeo.
- **Grupo de Ajustes:** una tarjeta de 18 dp con filas de 52 dp y separadores (`GrupoAjustes`, `FilaAjuste`).

### Inputs / Fields
- `TextInputLayout` de Material (142 apariciones en `res/layout`), tematizado por `Theme.GymProFit` (`colorOutline` y la escala de la app). Error: `UIHelper.marcarError` pone el mensaje en el `TextInputLayout` (sin él, un toast) y lo quita al editar; `Movimiento.temblar` (momento 10) agita el campo o la tarjeta en `CrearAlimentoActivity` y `CrearPlatoActivity`. Contraseña con `ui/widget/CampoContrasena.java`.
- Selectores: `Spinner` con flecha Material Symbols y 48 dp de alto mínimo (`Widget.GymProFit.Spinner`); píldora deslizante para opciones (`ui/alta/SelectorPildora.java`).

### Navigation
- **Barra inferior** (`BarraNavegacion`): Inicio · Entrenar · «+» · Nutrición · Progreso. Pestaña activa con píldora `gp_primary_container` e icono relleno; el resto, icono en contorno y `gp_text_secondary`. El «+» abre las acciones rápidas y, abierto, gira a «×». Cada botón lleva nombre, rol de pestaña, posición y estado para TalkBack, y su tooltip.
- **Cabecera** de pantalla secundaria: `MaterialToolbar` de 56 dp (`Widget.GymProFit.Cabecera`) con «Volver».
- **Transición entre pantallas:** deslizamiento lateral con parallax (`GymProFit.WindowAnimation`, `res/anim/slide_*`).

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

**Duraciones** (`Movimiento.java:62-160`, ms): `TOQUE` 120 · `RELLENO` 150 · `CHECK` 260 · `ICONO` 420 · `ENCIENDE` 380 · `ENTRA` 350 (+40 por escalón) · `CAPITULO` 450 · `LATIDO` 900 · `BARRA` 500 · `DESTELLO` 700 · `PILDORA` 320 · `DIA` 280 (+35) · `CIFRAS` 1200 · `HALO` 900 · `CARTA` 450 · `FILA` 300 (+100) · `BRILLO` 900 · `CIRCULO` 300 · `GIRO` 700 · `TRAZO` 420 · `TEMBLOR` 380 · `MENSAJE` 250 · `AVISO` 5000 · `TROFEO` 450 · `CHISPAS` 700 · `HUNDE` 70 · `VERDE` 120 · `CASCADA` 350 (+40) · `BARRIDO` 2400 · `ENCAJA` 200 · `HOJA` 450 · `PLIEGA` 180 · `CAE` 360 (+120) · `CORAZON` 450 · `RESALTA` 1400 · `SALE` 380 · `RECOLOCA` 250 · `CIFRA` 250 · `VELO` 250 · `SALTA_OK` 450 (+200) · `CIERRA` 600 · `PUNTO` 450 · `VASO` 450 · `RESPIRA` 1400.

**Momentos** (tabla completa en DEC-039; aquí el resumen por familia):

| Familia | Momentos | Curvas |
|---|---|---|
| Elegir y encender | 1 Elegir, 2 «Siguiente», 6 Selector, 15 «+» a ✓, 19 Corazón | estándar → rebote |
| Entrar en pantalla | 3 Entrar, 8 El programa llega, 16 Cascada, 23 Plato | enfatizada, con escalón |
| Progreso y cifras | 5 Barra, 7 Cifras que cuentan, 21 Día completo, 22 Un vaso más | estándar y rebote; cifras con `1 − (1 − t)³` |
| Éxito | 9 Cuenta creada, 12 Récord, 17 Código leído | rebote, enfatizada |
| Error | 10 Error | temblar + mensaje |
| Salida | 18 Copia de ayer, 20 Borrar y deshacer | estándar |

**Hápticos** (`Movimiento.vibrar`, `Movimiento.java:174-197`; por `performHapticFeedback`, así que respetan el ajuste de vibración al tocar): `LIGERA` → `CLOCK_TICK` (elegir, «+», copiar); `MEDIA` → `CONTEXT_CLICK` (abrir capítulo); `EXITO` → `CONFIRM` en Android 11+ (`LONG_PRESS` antes): cuenta creada, récord, código leído, guardar plato, día completo (una vez al día); `ERROR` → `REJECT` en Android 11+ (`LONG_PRESS` antes).

**Reglas:**
- Con «Quitar animaciones», todo en su estado final: se anima siempre *hacia* el estado final; lo que va en bucle pregunta a `Movimiento.quieto()` y se queda en su último fotograma. Las vibraciones se quedan.
- Nada espera a una animación: ningún botón se apaga mientras algo se mueve, y lo que entra se puede tocar desde el primer fotograma.
- Sin dependencias nuevas.

## Do's and Don'ts

### Do:
- **Do** leer colores de `@color/gp_*` o `?attr/color*`, y tamaños de `@dimen/text_*` (`values/colors.xml`, `dimens.xml`).
- **Do** delimitar tarjetas con borde de 1 dp en `gp_stroke`.
- **Do** dar a toda zona pulsable 48 dp como mínimo y nombre accesible (DEC-019), y a las vistas dibujadas a mano su accesibilidad desde el principio.
- **Do** poner cada cadena visible en `values/strings.xml` y `values-en/strings.xml` con plurales donde se cuenta (DEC-020).
- **Do** pedir un momento a `Movimiento` en vez de escribir una duración o una curva (DEC-039).
- **Do** comprobar la pantalla en claro y oscuro, en ES y EN y con letra 1,3 antes de darla por terminada (DEC-022).

### Don't:
- **Don't** usar el dorado para nada que no sea un récord o un logro (DEC-018).
- **Don't** introducir un segundo acento ni cambiar el naranja entre temas.
- **Don't** usar emojis como iconos ni en la interfaz.
- **Don't** escribir un `sp` o un hex a mano en un layout.
- **Don't** añadir Lottie ni librerías de animación (decidido el 22-10-2026).
- **Don't** apoyar un dato solo en el color: lleva icono, texto o forma.

## Huecos

Lo que hoy es incoherente o no existe. Aquí no se arregla nada.

1. **Estados de vacío, error y carga sin sistema (GP-064).** Existe `VistaEstado` (`layout/view_estado.xml`, `utils/VistaEstado.java`) y la usan 10 layouts, pero hay 8 pantallas con su propio vacío a mano (`activity_ejercicios`, `activity_programas`, `activity_resumen_sesion`, `fragment_entrenar`, `fragment_historial`, `fragment_logros`, `fragment_medidas`, `fragment_records`, con ids distintos: `tvVacio`, `tvEmpty`, `layoutVacio`, `layoutRutinasVacio`…). La carga es un `ProgressBar` redondo o un diálogo modal (`LoadingDialog`); no hay esqueletos. El error es sobre todo un toast (84 usos de toast frente a 5 de Snackbar), no un estado en pantalla.
2. **Animaciones a mano sin sistema.** Fuera de `Movimiento` hay duraciones y curvas escritas a pelo: `GuardaPlanActivity` (250 ms), `SplashActivity` (800 ms con `AccelerateDecelerateInterpolator`), `SelectorPildora` (200), `SemanaEjemplo` (220), `PasoPlanFragment` (700 con 1100 de retraso), `MainActivity` y `BarraNavegacion` (`config_shortAnimTime`, `DecelerateInterpolator`), y el deslizamiento entre pantallas en `res/anim/slide_*` y `tab_*`. DEC-039 ya dice que las de antes del alta siguen con lo suyo hasta que se toquen.
3. **Ni Material motion ni estilos Expressive.** No hay `MaterialSharedAxis`, `MaterialContainerTransform` ni `MaterialFadeThrough` (0 usos en `java/` y `res/`), ni Dynamic Color, ni estilos Expressive (Material 1.14.0 sin usar sus variantes). La transición entre pantallas es la propia de la app.
4. **Profi, todavía fuera de la app.** Se decidió (22-10-2026) que entre solo en el alta, las pantallas vacías, los logros y «Tu semana», pero no hay ningún recurso, ilustración, estilo ni texto suyo en `res/`; su material vive en el repositorio de marketing. Las pantallas vacías de arriba son justo donde aparecería. «Tu semana» es una pantalla nueva que se abre desde una tarjeta de los lunes en Inicio (GP-210, paso 6). La tarjeta «ESTA SEMANA» (GP-090) se queda como está y Profi no entra en ella.
5. **Sin escala de espaciado ni de radios declarada.** Los márgenes y radios se escriben en cada layout; hay 13 valores de radio y más de 10 de márgenes en uso (ver Layout y Shapes). Las medidas «del lienzo» viven en estilos sueltos (`estilos_estructura.xml`, `estilos_alta.xml`).
6. **Elevación incoherente (decidida, pendiente de GP-210, paso 2).** `Widget.GymProFit.Card` declara 1 dp, `Card.Estructura` 0 dp y 21 tarjetas lo fijan a 0 a mano; el comentario de GP-063 dice que el 1 dp es lo que despega la tarjeta en claro. La regla es «borde, no sombra»: `Card` pasa a 0 dp con borde en ese paso.
7. **Título de DEC-039 desfasado.** La tabla ya recoge los momentos 13 y 14 (del lienzo de nutrición, con `CIFRAS`) y `RESPIRA` (fila «— · Respira», sin número porque no es un momento del lienzo), y `MovimientoTest` la lee entera; pero el título sigue diciendo «doce momentos» y la tabla llega al 23.
8. **Alias de color heredados sin retirar** (`primary_light`, `golden`, `orange_dark`, `text_primary_dark`, `green_chip`, `red_chip`…), con valores que ya no coinciden en parte con los `gp_*`.
9. **Contenido de base de datos sin traducir** (DEC-020): 752 de 873 ejercicios con el nombre igual en ES y EN. Se ve en la pantalla de Entrenar.
10. **Títulos grandes que no crecen con la letra 1,3** en Inicio (`tvHola`, `tvHoyTitulo`, `tvSemanaZonas`, `tvInicial`): medido con el medidor (revisión de Inicio, GP-207). Puede ser un tope deliberado; no está documentado.
11. **`sp` escritos a mano que la regla prohíbe.** Quedan 23 `android:textSize="…sp"` literales en `res/layout` (por ejemplo el «R» del fundador, 20 sp, en `fragment_inicio.xml`).
12. **Borde discontinuo sin documentar.** La tarjeta del fundador (`drawable/bg_tarjeta_fundador.xml`), el botón discontinuo (`bg_boton_discontinuo.xml`) y «nueva rutina» (`bg_nueva_rutina.xml`) usan trazo discontinuo, una tercera forma de borde que «Components» no recoge y que el diseño de Inicio aprobado no tiene.
13. **Inicio con errores de carga.** Con la API devolviendo error, Inicio enseña el mismo mensaje en dos sitios, deja la tarjeta de nutrición sin cifra ni reintento y los puntos de macros sin texto; no usa `VistaEstado`. `cardNutricionHoy` es pulsable y no tiene nombre accesible (medidor de GP-209).
