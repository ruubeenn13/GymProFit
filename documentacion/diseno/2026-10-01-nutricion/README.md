# Nutrición nueva — GP-162, con GP-160, GP-165 y GP-166

Diseño aprobado por el propietario el 01-10-2026, con sus catorce decisiones (`Decisiones.dc.html`).

- `fuente/*.dc.html`: el lienzo, tablero a tablero, con los valores exactos: medidas en px = dp, colores, textos y animaciones. Se lee como especificación; no se ejecuta. Las marcas `{{ … }}` y el bloque `data-dc-script` son del editor del lienzo: guardan el estado de ejemplo y lo que cambia al tocar (qué se añade, la cantidad de la ficha, el código leído).
- `fuente/canvas.json`: el índice del lienzo, con el título y la posición de cada tablero.

## Los tableros

| N.º | Fichero | Qué es |
|---|---|---|
| 1 | `Main` | Nutrición, hoy: un día de entreno, con el anillo, los macros, el agua y «¿Copiar la de ayer?» |
| 2 | `Comida` | Una comida, con borrar y deshacer |
| 3 | `DiaCompleto` | El día completo |
| 4 | `Anadir` | Añadir: lo tuyo primero |
| 5 | `Buscar` | Buscar en nuestra base, por grupos |
| 6 | `Escaner` | Escanear un código |
| 7 | `Ficha` | La ficha del alimento y la cantidad |
| 8 | `Favoritos` | Favoritos |
| 9 | `MisPlatos` | Mis platos y «Lo que te encaja» |
| 10 | `CrearPlato` | Crear un plato |
| 11 | `Entreno` | Al acabar el entreno |
| 12 | `Semana` | Tu semana de nutrición |
| — | `Decisiones` | Lo aprobado el 01-10 y las catorce decisiones |
| — | `Movimiento` | Los momentos 13 a 22, que siguen a los doce de DEC-039 |
| — | `Tecnica` | Lo que se preveía por dentro antes de la 1.6.0 |

## Por lotes

Cada lote construye lo suyo y no dibuja nada que todavía no funcione.

- **1.6.0, hecha:** la API. Básicos, productos de España, búsqueda en casa y código de barras.
- **1.6.1:** los tableros 4, 5, 6 y 7. Sin las pestañas, sin el «+» de añadir en un toque, sin «Comidas recientes» y sin el corazón, que llegan con sus lotes.
- **1.6.2:** el tablero 8 y, de los tableros 1, 2 y 4, el «+» con la última cantidad, las comidas recientes, «¿Copiar la de ayer?» y borrar con deshacer.
- **1.6.3:** los tableros 9 y 10.
- **1.6.4:** del tablero 1, el anillo, el agua, el día de entreno y los puntos de la semana; y los tableros 3, 11 y 12.

## Cómo leerlo

Los datos son de ejemplo: Lucía, un jueves de entreno, a la hora de la merienda. Su objetivo es de 2.397 kcal, 136 g de proteína, 314 g de carbohidratos, 66 g de grasa y 2,2 L de agua, y lleva 1.236 kcal y 82 g de proteína. El yogur de la ficha tiene 60 kcal y 10 g de proteína por 100 g, en un envase de 200 g.

Los colores salen de la paleta de la app: si alguno no coincide con un token, **manda el token** (sus contrastes están medidos, GP-063). El tema claro sale de los mismos tokens. Son nuevos el azul del agua (`#56C2D6`) y la insignia azul de «datos revisados», que en el lienzo usa el azul de la proteína.

Las `@keyframes` de cada tablero son la referencia exacta de tiempos y curvas, y `Movimiento.dc.html` las reúne. Con «Quitar animaciones», cada momento salta a su estado final.

El tablero `Tecnica` se escribió antes de la 1.6.0. Lo que de verdad se decidió al construirla está en DEC-040 y DEC-041, y manda sobre el tablero.

Lo que se ve en el lienzo y no existe: nada. El modo «Foto» del escáner no se dibuja; va después del lanzamiento (GP-161).
