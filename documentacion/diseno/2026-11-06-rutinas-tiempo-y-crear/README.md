# Tiempo por sesión y crear una rutina (GP-243 y GP-244)

El propietario aprobó estos diseños el 6 de noviembre de 2026 en el lienzo «Rutinas: tiempo, crear y elegir» de claude.ai, que no está en el repositorio: la opción 1 de «¿Cuánto tiempo tienes por sesión?» («Bloques») y la mezcla de las opciones A y B para crear una rutina. Sus tableros están en `fuente/`, tal como salieron del lienzo: son `.dc.html` para leer como código. Manda esta especificación; los tableros, para medidas, colores y textos exactos. Solo están en oscuro y en español: el claro y el inglés salen de `app/GymProFit/DESIGN.md`. Lo que dibujan y la API no tiene no se inventa: va a «Huecos».

## Tableros

| Tablero | Qué enseña |
|---|---|
| `Tiempo-1.dc.html` | La hoja «¿Cuánto tiempo tienes por sesión?», al pasar de 60 a 30 min |
| `Mezcla-1.dc.html` | Crear, paso 1: qué y cuánto |
| `Mezcla-2.dc.html` | Crear, paso 2: eliges los ejercicios |
| `Mezcla-3.dc.html` | Crear, paso 3: tu rutina, que también sirve para editarla |

## Pantallas

| Superficie | Pieza de hoy |
|---|---|
| Tiempo por sesión | `SeguirProgramaHoja`, `dialog_seguir_programa.xml`, `item_vista_previa_rutina.xml` |
| Crear, paso 1 | `CrearRutinaActivity` |
| Crear, paso 2 | `AnadirEjerciciosActivity` |
| Crear, paso 3, y editar | `ResumenCrearRutinaActivity` y `EditarRutinaActivity`, que pasan a ser una sola |

## Especificación

### Tiempo por sesión (GP-243)

1. **La hoja** lleva arriba el programa («Cuerpo completo, tu programa») y el título. El selector de cuatro tiempos (30, 45, 60 y 75+) dice «ahora» bajo el que tiene, y al elegir otro la hoja se rehace con la vista previa de ese tiempo (`VistaPrevia`, sin cambios de API).
2. **Una frase** resume lo que cambia en las rutinas con ese tiempo, en una función pura con su test: «Con 30 minutos, cada rutina se queda en sus 3 básicos.» en el tablero.
3. **Cada rutina, en su tarjeta**: el nombre, los minutos y el botón de desplegar; un bloque por ejercicio, lleno y con una raya por serie si se queda, vacío y discontinuo si sale (primero los que se quedan, en su orden, y luego los que salen); y una línea: «3 de 7 ejercicios. Los básicos, a 2 series.» o, si no cambia, «Los 7 ejercicios, enteros.». Desplegada, la lista: el check, el nombre y la pauta («2 × 6–10») si se queda; el aspa, el nombre tachado y «fuera» si sale. Para TalkBack, una frase por rutina, y los bloques son decorativos.
4. **Debajo**, la regla del recorte: «Con menos tiempo salen primero los extras, desde el final, y luego series de los básicos.».
5. **El aviso de rehacer**, en su tarjeta con el icono: «Se rehacen tus 3 rutinas» y «Pierdes los cambios que les hiciste. Tus sesiones y récords se quedan.», y el botón «Cambiar a 30 min». Los mismos casos que hoy: seguir por primera vez, «Cambiar a», «Rehacer las rutinas», «Ya sigues otro», cargando y error con reintento.

### Crear una rutina (GP-244)

6. **Paso 1, qué y cuánto** («Nueva rutina», con el aspa para cerrar): «¿Qué quieres entrenar?», con las seis zonas en tarjetas de dos en dos (Pecho, Espalda, Pierna, Hombros, Brazos y Abdomen, los `GrupoMuscular` de la API, cada una con dos ejemplos), una o varias; «¿Cuánto tiempo tienes?», con el selector de 30, 45, 60 y 75+ min (de entrada, el de su programa si sigue uno y, si no, 45); y «Prefiero buscar entre todos», que salta al paso 2 sin zonas. Cardio y cuerpo completo no son zonas de este paso: sus ejercicios se encuentran buscando entre todos. El pie dice lo elegido («Pecho y hombros, 45 minutos.») y «Elegir ejercicios», que pide al menos una zona.
7. **Paso 2, eliges los ejercicios** («Elige ejercicios»): el buscador del catálogo (`GET /ejercicios/buscar`) filtrado por esas zonas, con una píldora por zona que se quita y «+ Zona» para añadir otra. Hoy `grupoMuscular` admite un solo valor: pasa a admitir varios, de forma aditiva (con uno, como ahora). Cada fila, con el nombre, la zona y el equipo («Pecho, con barra») y el botón de añadir, que pasa a check. Fuera el filtro de dificultad. El pie: cuántos llevas, «Unos 35 de tus 45 min» con su barra, y «Listo». Cada ejercicio entra con 3 series de 8 a 12 y 90 s de descanso (las pautas del tablero son de ejemplo) y se cambia en el paso 3.
8. **Paso 3, tu rutina** («Nueva rutina», o «Editar rutina» al editar):
   - el nombre en grande, propuesto por las zonas («Pecho y hombros»; buscando entre todos, por las zonas de los ejercicios elegidos, con `Zonas`) y la nota «Sale de las zonas que elegiste; cámbialo si quieres.»;
   - la tarjeta con ejercicios, series y minutos («35, más o menos»), la barra del tiempo con su frase («35 de tus 45 minutos: te cabe un ejercicio más.», «Justo en tus 45 minutos.» o «Te pasas unos 10 minutos de tus 45.»), las zonas en una frase y el nivel del perfil («Para nivel intermedio, el tuyo.») con «Cambiar». Al editar no hay tiempo elegido: la tarjeta dice los minutos, sin barra;
   - la lista de ejercicios con su pauta, el asa para arrastrar y ⋮ con cambiar la pauta (series, repeticiones o segundos, y descanso), subir, bajar y quitar: así se ordena sin arrastrar, también con TalkBack;
   - «Añadir ejercicios», que vuelve al paso 2 con lo elegido; «Añadir una descripción», plegada;
   - el pie: «4 ejercicios y unos 35 minutos.» y «Guardar rutina» («Guardar cambios» al editar). Sin ejercicios no se guarda, y el pie lo dice («Añade al menos un ejercicio.»).

   Editar abre esta pantalla con la rutina cargada; `EditarRutinaActivity` y `ResumenCrearRutinaActivity` se van. Salir con algo elegido avisa (GP-098). `EditarRutinaAdminActivity` no se toca.
9. **La duración se calcula, no se teclea.** La fórmula es la de `ReglasPrograma.centesimas`, en la API, con su redondeo a 5 min. La app lleva una copia pura para el cálculo en vivo, y la API y la app pasan la misma tabla de casos, en un fichero del repositorio que leen los tests de los dos, para que no se separen. Al guardar, la app manda `duracionMinutos`; el contrato no cambia.
10. **Una rutina vieja sin ejercicios**: su tarjeta de Entrenar dice «Sin ejercicios todavía» y abre el paso 3 para añadirlos.
11. **En todas estas pantallas**: todo en frase y sin «·», en ES y EN; claro y oscuro; letra 1,3 sin cortes; contrastes medidos (4,5:1 en texto y 3:1 en gráficos); TalkBack con nombre en cada control, y las zonas y los tiempos con su estado; el movimiento con los tokens de DESIGN.md y quieto con «Quitar animaciones». Ningún color nuevo sin pasar por DESIGN.md.

## Bajos que tienen que desaparecer

Los dio el revisor en el lote 7 en estas pantallas: el poco aire sobre el título de la hoja de seguir programa, «Main lifts at 2» / «sets» partido en inglés a 1,3, «Siguiente» en Barlow Condensed y «Duración» sin unidad en Crear rutina. Y el del lote 9: los «·» de Añadir ejercicios.

## Huecos

Ninguno conocido al escribir esto.
