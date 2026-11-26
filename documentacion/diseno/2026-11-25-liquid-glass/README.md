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
- **Botón principal:** `#FF6A00` con texto `#1A0E00`, brillo superior blanco 45 % y halo naranja 45 %. Secundario: cristal estático.
- **«+»:** círculo de 64 dp, degradado radial `#FFB27A` → `#FF6A00`, icono `#1A0E00`.
- **Barra:** cápsula flotante de 64 dp a 16 dp de los lados y 26 dp del borde inferior (más el hueco de gestos), con el «+» separado a su derecha. El contenido pasa por debajo.
- **Texto:** principal `#F2F2F3`; secundario `#C9CDD6`; acento `#FFB27A`/`#FF8A3D`. Títulos y cifras en Barlow Condensed 700–800; texto en Barlow.
- **Récord:** fila con degradado dorado `rgba(232,184,75,.28)` → `.08` y brillo `rgba(255,236,190,.4)`; dorado `#E8B84B`.

## Reglas de legibilidad (obligatorias)

1. Ningún brillo ambiental intenso detrás de texto pequeño (< 18 sp). En la maqueta de Inicio, «La última vez, hace 3 días» queda ≈ 3:1 sobre el brillo naranja: es un fallo de la maqueta, no la referencia.
2. Cada par texto/superficie pasa 4,5:1 (3:1 en texto ≥ 24 sp o gráficos) medido sobre el **punto más claro** de lo que puede haber detrás (fondo + brillo + cristal compuestos).
3. Las cifras y textos clave nunca dependen del desenfoque para leerse: tienen que pasar también en el nivel 3.

## Tema claro

No hay maqueta. Se diseña con los mismos principios (cristal blanco esmerilado sobre fondo claro con brillos cálidos suaves) y las mismas reglas de contraste, y se enseña en capturas para aprobación.
