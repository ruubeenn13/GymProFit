# Las tarjetas de rutinas, sesiones y programas (GP-232)

El propietario aprobó el diseño el 4 de noviembre de 2026 en un lienzo de claude.ai que no está en el repositorio. Sus 11 tableros están en `fuente/`, tal como salieron del lienzo: son `.dc.html` para leer como código (sin el motor del lienzo no se ven bien en el navegador). Lo que manda es esta especificación, la de la ficha de GP-232; los tableros, para medidas, colores y textos exactos. Lo construido está en `app/GymProFit/DESIGN.md` («Las tarjetas de sesión y de rutina»).

## Tableros

| Tablero | Qué enseña |
|---|---|
| `Main.dc.html` | Antes y después de cada superficie |
| `Barras.dc.html` | El componente de las barras: colores por tema, alto proporcional desde 0, esquinas y movimiento |
| `Historial-oscuro.dc.html`, `Historial-claro.dc.html` | El Historial completo por semanas |
| `Inicio-oscuro.dc.html` | El carrusel «Tus últimos entrenos» |
| `Progreso-oscuro.dc.html` | Las filas del Historial de Progreso |
| `Entrenar-oscuro.dc.html` | «Rutinas del programa» y «Mis rutinas» |
| `Elegir-oscuro.dc.html` | La hoja «¿Qué rutina hoy?» |
| `Programas-oscuro.dc.html` | El catálogo de programas con el recomendado |
| `Claro.dc.html` | Todas en claro |
| `Especificacion.dc.html` | La anatomía, las barras, el movimiento, los datos, los estados y los contrastes medidos |

## Pantallas

| Superficie | Dónde está | Pieza |
|---|---|---|
| Historial completo | Inicio › «Historial», o Progreso › «Ver todo» | `fragment_historial.xml`, `SesionAdapter` |
| Inicio · «Tus últimos entrenos» | Pestaña Inicio | `item_ultimo_entreno.xml` |
| Progreso · Historial | Pestaña Progreso | `item_fila_progreso.xml` |
| Entrenar · rutinas | Pestaña Entrenar | `item_rutina_fila.xml` |
| «¿Qué rutina hoy?» | Entrenar › «Empezar» | `item_elegir_rutina.xml` |
| Programas | Entrenar › «Programas» | `item_programa.xml` |

## Especificación

1. **Cifras y barras.** En cada tarjeta de sesión y de rutina, los kilos (Σ peso × reps, el volumen de GP-011) en condensada 700 a 26 sp, y a su lado las barras de sus últimas veces con esa rutina, hasta 6, de la más antigua a esta, que va a la derecha. La de esta sesión (en una rutina, la última vez), `gp_primary`; la que batió un récord, `gp_gold` (si esta lo batió, va en dorado); las demás, `gp_stroke`. Empiezan en 0. Ancho, hueco y alto: 10, 6 y 36 dp en tarjeta; 7, 4 y 28 en el carrusel de Inicio; 5, 3 y 24 en las filas. Esquinas como `BarrasKilosView`: 8 dp arriba y 4 abajo, sin pasar de media barra. Decorativas para TalkBack.
2. **Todo en frase**, sin «·», en las líneas de estas tarjetas, en ES y EN: «Ayer, 18:05», «3 % más que la anterior», «58 minutos, 18 series y 1 récord», «Pecho, espalda y brazos» («Chest, back, and arms», con la coma de serie). Las zonas salen de las cadenas de `Zonas`, de más a menos series. El resto de la app no cambia.
3. **Sesión** (Historial, Inicio y Progreso): la rutina es el título (condensada 600 a 20 sp; sin rutina, «Entrenamiento libre») y el cuándo va arriba a la derecha; debajo, las zonas, los kilos con sus barras, la comparación con la anterior de la misma rutina (en verde si sube; neutra si baja; «Igual que la anterior» a 0 %; la primera vez, «Primera vez con esta rutina», sin porcentaje) y minutos, series y récords en frase, el récord en dorado. Entrenamiento libre: solo cifras, sin barras ni comparación. Sin kilos, la cascada del punto 13. Inicio: una sola línea destacada, el récord antes que la comparación, y fuera «Sin récords». Progreso: filas compactas con las barras pequeñas a la derecha.
4. **Historial completo por semanas**, con su cabecera («ESTA SEMANA» y «2 entrenos y 22.050 kg»; sin kilos, «3 entrenos») y la fecha como «Martes 3, 18:05» («Tuesday 3, 6:05 PM»: tampoco en inglés lleva el mes, que da la cabecera). `VistaEstado` para carga, error y vacío, dentro de la sección y sin toast (es la parte del Historial de GP-064).
5. **Sin papelera**: deslizar a la izquierda para borrar, con «Deshacer» durante 5 s (`QuitarConDeshacer`, como en Comida: el borrado se manda cuando se va el aviso), la acción de TalkBack con las cadenas de GP-227 y borrar desde el resumen.
6. **Rutinas de Entrenar**: cuándo toca, arriba a la derecha («Hoy» en naranja, «Siguiente», «En 2 entrenos»); zonas; ejercicios y minutos; los kilos de la última vez con sus barras, y «La última vez, hace 6 días», con sus récords si los hubo. Sin hacer todavía: «Sin hacer todavía» en el sitio de los kilos, sin barras. Si la última vez fue a 0 kg, la cascada del punto 13. En «Mis rutinas», ▶ para empezar. La hoja «¿Qué rutina hoy?»: la que elige `HoyToca` primero, con «Toca hoy» justo detrás del nombre; zonas, «La última vez, …», barras pequeñas (si la última vez tuvo kilos) y ▶ en cada fila.
7. **Programas**: el recomendado, en tarjeta protagonista (`gp_hero`); cada programa con la tira de su ciclo (`Siglas` sobre `Programa.semana`), el nivel con su punto (`NivelColor`; sobre la protagonista, el del tema oscuro en los dos temas) y «Lo sigues», justo detrás del nombre, en el que sigues. Dos rutinas distintas de un ciclo nunca comparten siglas («Push» PS y «Pull» PL). Sin barras.
8. **Movimiento**, una sola vez al entrar la tarjeta: las barras crecen desde abajo una tras otra (`BARRA` y `CASCADA_ESCALON`), los kilos cuentan (`Movimiento.contar` en `CIFRAS`) y la barra dorada brilla una vez (`BRILLO`). Al tocar, `Transiciones.abrirTarjeta`; borrar y deshacer, con `SALE` y `RECOLOCA`. Ninguna duración nueva; quieto con «Quitar animaciones» y sin repetirse al desplazar.
9. **Letra 1,3**: si las barras no caben junto a los kilos, bajan debajo; ningún texto se corta, tampoco el título de «Tus últimos entrenos» (pasa aquí desde GP-225).
10. **Ningún color nuevo.** Contrastes del lienzo sobre la tarjeta, claro y oscuro: barra de esta sesión 5,63 y 5,81; de récord 3,40 y 9,05; las demás 3,41 y 3,30; subida en verde 5,25 y 9,58; récord en texto 5,09 y 9,05; neutro 6,68 y 7,05.
11. **TalkBack**: cada tarjeta se lee de una vez («Pierna A, ayer a las 18:05. Pierna y abdomen. 13.640 kilos, 3 % más que la anterior. 58 minutos, 18 series y 1 récord. Tus 6 últimas veces con esta rutina, de 11.020 a 13.640 kilos») y borrar es una acción de la tarjeta («Borrar Pierna A del 3 de noviembre»).
12. **Fuera**: la papelera y su diálogo, el ✓ verde igual en todas las sesiones de Progreso, la pesa igual en todas las rutinas, «Sin récords» por defecto en Inicio, `item_rutina.xml` y `RutinaAdapter`.
13. **Sin kilos, en cascada** (decisión del propietario del 7 de noviembre: con los programas de peso corporal es un tipo de usuario entero, no un caso raro). Ninguna superficie enseña «0 kg» ni «0 series»; una sola función pura (`CifraTarjeta`) decide la cifra en las cinco que la tienen. Con kilos, como arriba. Con 0 kg y series, «3 series» en la misma condensada, sin barras ni comparación, y debajo solo minutos y récords (Progreso: «29 sept, 3 series y 1 récord»; Inicio: la línea destacada solo si hubo récord). Sin series, el aviso secundario: «Sin ejercicios apuntados» si no hay ejercicios y «Sin series hechas» («No sets done») si los hay sin ninguna serie, con los minutos debajo. Una rutina cuya última vez fue a 0 kg enseña las series de esa vez, sin barras. Una barra de 0 kg dentro de una rutina con kilos se queda en su filo de 4 dp. TalkBack dice «3 series», nunca «0 kilos». «Kilos movidos» de Progreso queda fuera (GP-249).

## API

Aditiva: las rutas de antes siguen igual.

- `GET /sesiones/historial`, con el usuario del token, paginado por semanas: hasta 8 semanas con sesiones por página, una semana nunca se parte y el cursor `siguiente` (pasado como `?antes=`) lleva a la siguiente. Por semana: su lunes en la hora local, entrenos y kilos. Por sesión: rutina, inicio, minutos, kilos, series, ejercicios, récords batidos ese día (`CalculadoraRecords`, sin primeras marcas), kilos de la anterior de la misma rutina, series por músculo (`Musculos.normalizar`) y sus barras: kilos, series y récord de sus últimas 6 sesiones de la misma rutina, hasta ella, aunque caigan en páginas anteriores. Un número fijo de consultas por petición, ninguna por sesión.
- En `GET /rutinas/usuario/{id}/activas`, `musculos` y `ultimas` (fecha, kilos, series y récord de sus 6 últimas sesiones; `series` desde el 7 de noviembre, para la cascada del punto 13), con consultas agrupadas y solo en esa ruta.
- Inicio y Progreso pasan al endpoint nuevo y se quitan sus peticiones por tarjeta. Los récords de una sesión se cuentan igual en todas partes.
