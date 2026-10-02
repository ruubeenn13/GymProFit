# Nutrición nueva — el lienzo del 02-10-2026

Es el lienzo de `documentacion/diseno/2026-10-01-nutricion/` con lo que el propietario cambió el 02-10, al ver la 1.6.1 en su móvil. **Desde hoy la referencia es esta carpeta**; la del 01-10 se queda como estaba, como foto de lo que se aprobó ese día.

- `fuente/*.dc.html`: el lienzo, tablero a tablero, con los valores exactos: medidas en px = dp, colores, textos y animaciones. Se lee como especificación; no se ejecuta. Las marcas `{{ … }}` y el bloque `data-dc-script` son del editor del lienzo: guardan el estado de ejemplo y lo que cambia al tocar.
- `fuente/canvas.json`: el índice del lienzo, con el título y la posición de cada tablero.

## Qué cambia respecto al 01-10

Tres cosas, que son las decisiones 15, 16 y 17 de `Decisiones.dc.html`. Los demás tableros no cambian.

1. **Elegir la comida (tableros 4, 8 y 9).** La cabecera ya no dice «Añadir a [merienda ▾]» con la comida a tamaño de título. Dice «Añadir alimento» y, a la derecha, una etiqueta pequeña con la comida («Merienda ▾»). Tocarla abre una hoja con las cinco comidas, lo que lleva cada una y cuál toca ahora. Es la misma en Todo, en Favoritos y en Mis platos.
2. **Una comida (tableros 2 y 2b).** El resumen pasa a ser un anillo con el reparto de macros y cuánto pesa la comida en el día; cada alimento lleva su icono, su cantidad, sus kcal y su proteína; «Añadir alimento» y el escáner van fijos abajo, sin botón flotante. El tablero 2b es la comida sin apuntar.
3. **Crear un alimento (tablero 13, nuevo).** Los datos en el orden de la etiqueta del envase, por 100 g o por ración, con el aviso de si las calorías cuadran con los macros, cómo queda y cómo lo tomas. La misma pantalla edita un alimento propio.

## Los tableros

| N.º | Fichero | Qué es | Cambia |
|---|---|---|---|
| 1 | `Main` | Nutrición, hoy: un día de entreno, con el anillo, los macros, el agua y «¿Copiar la de ayer?» | — |
| 2 | `Comida` | Una comida: resumen con anillo, lista y borrar con deshacer | Sí |
| 2b | `ComidaVacia` | Una comida sin apuntar | Nuevo |
| 3 | `DiaCompleto` | El día completo | — |
| 4 | `Anadir` | Añadir: lo tuyo primero, con la etiqueta de la comida y su hoja | Cabecera |
| 5 | `Buscar` | Buscar en nuestra base, por grupos | — |
| 6 | `Escaner` | Escanear un código | — |
| 7 | `Ficha` | La ficha del alimento y la cantidad | — |
| 8 | `Favoritos` | Favoritos | Cabecera |
| 9 | `MisPlatos` | Mis platos y «Lo que te encaja» | Cabecera |
| 10 | `CrearPlato` | Crear un plato | — |
| 11 | `Entreno` | Al acabar el entreno | — |
| 12 | `Semana` | Tu semana de nutrición | — |
| 13 | `CrearAlimento` | Crear un alimento, como en la etiqueta | Nuevo |
| — | `Decisiones` | Lo aprobado el 01-10 (catorce decisiones) y el 02-10 (tres más) | Sí |
| — | `Movimiento` | Los momentos 13 a 22, que siguen a los doce de DEC-039 | — |
| — | `Tecnica` | Lo que se preveía por dentro antes de la 1.6.0 | — |

## Por lotes

Cada lote construye lo suyo y no dibuja nada que todavía no funcione.

- **1.6.0 y 1.6.1, hechas:** la API, y los tableros 4, 5, 6 y 7.
- **1.6.2:** lo del 02-10. La etiqueta y la hoja de las comidas (cabecera del tablero 4), los tableros 2 y 2b y el tablero 13.
- **1.6.3:** el tablero 8 y, de los tableros 1, 2b y 4, las pestañas, el «+» con la última cantidad, las comidas recientes, «¿Copiar la de ayer?» y «Lo que sueles».
- **1.6.4:** los tableros 9 y 10, y «Guardar como plato» del tablero 2.
- **1.6.5:** del tablero 1, el anillo, el agua, el día de entreno y los puntos de la semana; y los tableros 3, 11 y 12.

Lo que la 1.6.2 **no** dibuja aunque esté en sus tableros: del 2, «Guardar como plato» (1.6.4); del 2b, «¿Copiar la de ayer?» y «Lo que sueles merendar» (1.6.3); del 4, las pestañas, el «+» de cada fila, «Comidas recientes» y la barra de «Hecho» (1.6.3).

## Lo que el lienzo no dice

### La etiqueta de la comida y su hoja

- La etiqueta mide 36 de alto dentro de una zona de toque de 48, con borde de 1 y sin relleno: no compite con el título. Lleva el icono de la comida, su nombre y la flecha. Con letra grande baja a una segunda línea, debajo del título; no se recorta.
- La hoja enseña el día al que se añade («Hoy, jueves 1 de octubre»; otro día, su fecha). Cada fila: icono, nombre y, debajo, lo que ya lleva («450 kcal») o «Sin apuntar». «· ahora toca» solo en la comida que toca por la hora, y solo si el día es hoy.
- La elegida va con fondo, su icono en el acento suave y un check que salta. Tocar una fila elige y cierra; tocar fuera cierra sin cambiar nada.
- Es una elección de una entre cinco: para TalkBack, botones de opción, con «elegida» en la que lo está.

Iconos de las comidas (Material Symbols Rounded): desayuno `free_breakfast`, almuerzo `nutrition`, comida `restaurant`, merienda `cookie`, cena `dinner_dining`.

### Una comida

- El anillo reparte **las kcal de la comida** entre proteína, carbohidratos y grasa (4, 4 y 9 kcal por gramo), con un hueco de 3 entre tramos; dentro, las kcal. A su derecha, los gramos de cada macro y qué parte son **del objetivo del día**. Debajo, la comida frente a las kcal del día.
- Sin objetivo en el perfil (sin peso o altura), no hay nada con que comparar: se quitan la columna «de tu día» y la línea de las calorías de hoy, y quedan el anillo y los gramos.
- Cada fila: el icono de la categoría en un cuadro de 44, el nombre en una línea, la cantidad debajo («2 rebanadas (56 g)», «150 g»), y a la derecha las kcal y la proteína. Tocarla abre la ficha con «Actualizar», como en la 1.6.1.
- **Borrar.** En el lienzo la fila del arroz está a medio deslizar para enseñar el gesto y el fondo rojo con «Eliminar». En la app, deslizar la fila hasta el final la quita; no se queda a medias ni hay que tocar nada más. El aviso con «Deshacer» sale encima de la barra de abajo, no la tapa.
- Al quitar o deshacer, el anillo, los porcentajes y la barra del día pasan a su valor nuevo en 500 ms (curva estándar). Al entrar, el anillo se dibuja una vez y las filas suben en cascada.
- La barra de abajo es fija: «Añadir alimento» abre Añadir con esta comida ya elegida, y el botón redondo abre el escáner, también con esta comida.
- Vacía (2b): el icono de la comida, «Aún no has apuntado la merienda» y, si es la que toca por la hora y el día es hoy, «Es la que toca ahora». La barra de abajo, igual. Sin tarjeta de resumen.

Iconos de las categorías (Material Symbols Rounded), los mismos en las filas y en las fichas de categoría del tablero 13:

| Categoría | Icono |
|---|---|
| Carnes y aves | `kebab_dining` |
| Pescado y marisco | `set_meal` |
| Huevos | `egg_alt` |
| Lácteos | `breakfast_dining` |
| Legumbres | `grain` |
| Cereales y pan | `bakery_dining` |
| Frutas | `nutrition` |
| Verduras | `eco` |
| Frutos secos | `spa` |
| Aceites y grasas | `opacity` |
| Bebidas | `local_cafe` |
| Suplementos | `pill` |
| Snacks | `cookie` |
| Otro, o sin categoría | `restaurant` |

Si un tablero usa otro icono para un alimento (los del 01-10 ponen `set_meal` al pollo), manda esta tabla.

### Crear un alimento

- **El código**, si llega del escáner, arriba y sin poder cambiarlo.
- **Nombre**, obligatorio. **Marca**, opcional.
- **Categoría**: una fila de fichas que se desliza de lado, cada una con su icono. Ninguna viene elegida; sin elegir, el alimento se guarda como «Otro». En el lienzo sale elegida «Snacks» porque es el ejemplo.
- **Como en la etiqueta**: energía, grasas, hidratos de carbono, fibra y proteínas, en el orden del envase. Solo la energía es obligatoria, y 0 vale. El conmutador «100 g | 1 unidad» dice por cuánto son las cifras que se escriben; el segundo lado solo existe cuando abajo hay una ración con sus gramos, y lleva su nombre. Con «Bebidas», «100 ml».
- **El aviso**: con los tres macros escritos, compara la energía con 4·proteínas + 4·hidratos + 9·grasas. Si cuadra, la línea verde. Si no, una línea de aviso con lo que saldría («Con esos macros serían unas 430 kcal»), que **no impide guardar**: las etiquetas redondean, y la fibra y el alcohol cuentan aparte.
- **Así queda**: las cuatro cifras y el reparto, en vivo, por 100 g o por la ración.
- **¿Cómo lo tomas?**, opcional: el nombre de la ración, de una lista cerrada (unidad, ración, envase, rebanada), y sus gramos. Al lado, las kcal de una.
- «Solo lo ves tú».
- **Abajo**: «Guardar y añadir a la merienda» guarda y abre la ficha del alimento nuevo, con su ración si la tiene (o 100 g), para elegir la cantidad: no se apunta nada sin ver cuánto. «Solo guardar» guarda y vuelve. Al editar, un solo botón, «Guardar cambios».

## Cómo leerlo

Los datos son de ejemplo: Lucía, un jueves de entreno, a la hora de la merienda. Su objetivo es de 2.397 kcal, 136 g de proteína, 314 g de carbohidratos, 66 g de grasa y 2,2 L de agua. La comida del tablero 2 son 535 kcal, con 41 g de proteína, 59 g de carbohidratos y 13 g de grasa; sin el arroz, 275 kcal.

Los colores salen de la paleta de la app: si alguno no coincide con un token, **manda el token** (sus contrastes están medidos, GP-063). El tema claro sale de los mismos tokens.

Las `@keyframes` de cada tablero son la referencia de tiempos y curvas, y `Movimiento.dc.html` las reúne; en la app salen de `Movimiento` (DEC-039). Con «Quitar animaciones», cada momento salta a su estado final.

El tablero `Tecnica` se escribió antes de la 1.6.0. Lo que de verdad se decidió al construir está en DEC-040, DEC-041 y DEC-042, y manda sobre el tablero.
