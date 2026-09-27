---
name: GymProFit Admin
description: La mesa de trabajo del propietario de GymProFit, en oscuro.
colors:
  fondo: "#0B0C0E"
  lateral: "#101216"
  tarjeta: "#16191E"
  campo: "#0F1114"
  nota: "#1B1E24"
  seleccion: "#252A31"
  borde: "#252A31"
  borde-suave: "#1E2228"
  borde-control: "#4A505B"
  texto: "#F2F2F3"
  texto-etiqueta: "#D3D8DF"
  texto-secundario: "#C9CFD8"
  texto-apagado: "#A1A9B6"
  texto-tenue: "#7C8594"
  acento: "#FF6A00"
  acento-sobre: "#1A0E00"
  acento-fondo: "#3A2110"
  acento-claro: "#FFB27A"
  acento-hover: "#FF7A1A"
  enlace: "#FF8A3D"
  ok: "#7FD69B"
  ok-fondo: "#13301F"
  aviso: "#E8B84B"
  aviso-fondo: "#3A2E12"
  peligro: "#F9A8A8"
  peligro-borde: "#7A3434"
  peligro-fondo: "#2A1616"
  barra-alta: "#6B3A17"
  barra-sesion: "#2F4E78"
  barra-sesion-hoy: "#6BA6F5"
typography:
  display:
    fontFamily: "Barlow Condensed, Barlow, system-ui, sans-serif"
    fontSize: "36px"
    fontWeight: 700
    lineHeight: 1
  headline:
    fontFamily: "Barlow Condensed, Barlow, system-ui, sans-serif"
    fontSize: "32px"
    fontWeight: 700
  title:
    fontFamily: "Barlow Condensed, Barlow, system-ui, sans-serif"
    fontSize: "22px"
    fontWeight: 700
  body:
    fontFamily: "Barlow, system-ui, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.4
  label:
    fontFamily: "Barlow, system-ui, sans-serif"
    fontSize: "13px"
    fontWeight: 600
rounded:
  control: "12px"
  tarjeta: "18px"
  pildora: "22px"
spacing:
  s1: "4px"
  s2: "8px"
  s3: "12px"
  s4: "16px"
  s6: "24px"
components:
  boton-principal:
    backgroundColor: "{colors.acento}"
    textColor: "{colors.acento-sobre}"
    rounded: "{rounded.pildora}"
    height: "46px"
    padding: "0 20px"
  boton:
    backgroundColor: "transparent"
    textColor: "{colors.texto}"
    rounded: "{rounded.pildora}"
    height: "46px"
    padding: "0 20px"
  campo:
    backgroundColor: "{colors.campo}"
    textColor: "{colors.texto}"
    rounded: "{rounded.control}"
    height: "46px"
    padding: "0 12px"
  tarjeta:
    backgroundColor: "{colors.tarjeta}"
    rounded: "{rounded.tarjeta}"
    padding: "16px"
  etiqueta-ok:
    backgroundColor: "{colors.ok-fondo}"
    textColor: "{colors.ok}"
    height: "26px"
---

# Design System: GymProFit Admin

<!-- Fuente normativa: admin/src/estilos/tokens.css. Si un valor cambia allí, cambia aquí. -->

## Overview

**Creative North Star: "La mesa de trabajo"**

Una herramienta de una sola persona para trabajar rápido: el catálogo, las cuentas y el estado del
producto, sin nada que distraiga. Es la app de GymProFit en oscuro llevada a una pantalla grande,
con la misma paleta, las mismas fuentes y los mismos iconos. La densidad importa más que el aire,
y el color se reserva para lo que pide acción o dice un estado.

**Key Characteristics:**
- Oscuro siempre; no hay tema claro.
- Un único acento, el naranja de la marca, para la acción principal, la selección y lo pendiente.
- Profundidad por capas de tono (fondo → lateral → tarjeta), sin sombras.
- Titulares en Barlow Condensed; todo lo demás en Barlow.

## Colors

Neutros casi negros ligeramente fríos, un naranja de marca y estados apagados sobre fondos oscuros
de su mismo tono.

### Primary
- **Naranja GymProFit** (acento): el botón principal, la barra de la semana actual, el campo
  pendiente de revisar y el foco. Sobre él, texto casi negro (acento-sobre).
- **Naranja enlace** (enlace): enlaces y acciones de texto, e iconos de la sección activa.
- **Brasa** (acento-fondo) con **melocotón** (acento-claro): fondo y texto de lo elegido (sección
  activa, chip pulsado, insignia de admin).

### Neutral
- **Fondo** (fondo), **lateral** (lateral), **tarjeta** (tarjeta), **campo** (campo), **nota**
  (nota) y **selección** (seleccion): las capas, de la más profunda a la más cercana.
- **Borde** (borde), **borde suave** (borde-suave) y **borde de control** (borde-control): divisiones
  de tarjeta, filas de tabla y controles.
- Cinco tonos de texto, de **texto** a **texto tenue**, para jerarquía sin tamaño.

### Estados
- **Verde** (ok / ok-fondo), **ámbar** (aviso / aviso-fondo) y **rojo claro** (peligro,
  peligro-borde, peligro-fondo): siempre texto claro sobre fondo oscuro del mismo tono.

### Named Rules
**La regla del acento único.** El naranja marca la acción principal, lo elegido y lo pendiente. No
decora.

## Typography

**Display Font:** Barlow Condensed (con Barlow y system-ui)
**Body Font:** Barlow (con system-ui)

**Character:** Condensada y firme para títulos y cifras; Barlow limpia para leer y escribir.

### Hierarchy
- **Display** (700, 36px, 1): cifras del Resumen.
- **Headline** (700, 32px): título de cada pantalla.
- **Title** (700, 22px): títulos de tarjeta y de gráfica.
- **Body** (400, 15px, 1.4): tablas, fichas y formularios. En el móvil, nunca por debajo de 14px y
  los campos a 16px.
- **Label** (600, 13px): etiquetas de campo y cabeceras de tabla en escritorio.

## Layout

Barra lateral a la izquierda y contenido a todo el ancho. Espaciado de 4 en 4 (8, 12, 16, 24),
margen de página de 24px en escritorio y 16px en el móvil.

- **Desde 1280px:** barra lateral de 232px con nombre e icono.
- **De 768 a 1279px:** barra de iconos de 72px; el nombre aparece al pasar el ratón o con el foco.
- **Por debajo de 768px:** título arriba, cuatro pestañas abajo como en la app, tablas como lista
  de tarjetas y fichas y editores a pantalla completa.
- Tablas con filas de 48px, 25 por página y cabecera fija al desplazar.
- Ejercicios y Alimentos: lista y editor lado a lado solo desde 1440px.
- Nunca hay scroll horizontal.

## Elevation & Depth

Plano. La profundidad sale de las capas de tono y de los bordes de 1px; no hay sombras salvo el
anillo de foco (3px de fondo y 2px de melocotón). Los diálogos y el panel de la ficha se separan
con un velo negro al 60%.

## Shapes

Esquinas suaves y consistentes: 12px en campos y notas, 18px en tarjetas y diálogos, y píldora
completa en botones, filtros, chips y etiquetas.

## Components

### Buttons
- **Shape:** píldora (altura 46px).
- **Primary:** fondo naranja, texto casi negro, 700.
- **Secundario:** transparente con borde de control, texto claro, 600.
- **Peligro:** borde rojo oscuro y texto rojo claro; fondo rojo muy oscuro al pasar.
- **Hover / Focus:** fondo de nota (o naranja más claro en el principal); foco con el anillo.

### Chips
- **Style:** píldora de 40px con borde de control; la zona pulsable llega a 44px.
- **State:** pulsado, sin borde, fondo brasa y texto melocotón.

### Cards / Containers
- **Corner Style:** 18px.
- **Background:** tarjeta.
- **Shadow Strategy:** ninguna.
- **Border:** 1px de borde.
- **Internal Padding:** 16px (20px en tarjetas grandes de escritorio).

### Inputs / Fields
- **Style:** fondo de campo, borde de control, 12px de radio, 46px de alto.
- **Focus:** borde de 2px naranja.
- **Error:** borde de 2px rojo claro y el mensaje debajo. Pendiente de revisar: borde naranja.

### Navigation
- Enlace de 44px, icono de contorno y texto secundario; activo, fondo brasa, texto claro 600 e icono
  relleno en naranja enlace.

## Do's and Don'ts

### Do:
- **Do** sacar todo color y medida de tokens.css.
- **Do** poner las acciones justo después del contenido que afectan.
- **Do** mantener 44px de zona táctil y el anillo de foco en todo lo alcanzable.

### Don't:
- **Don't** anunciar pantallas o funciones que aún no existen.
- **Don't** añadir un tema claro ni otro acento.
- **Don't** usar sombras para separar capas.
