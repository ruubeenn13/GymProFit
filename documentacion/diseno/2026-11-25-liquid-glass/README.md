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

El desenfoque de la maqueta (`blur(22px)`) es la desviación (σ); en Android el «radio» de `RenderEffect` que da esa σ es (σ − 0,5) / 0,57735.
