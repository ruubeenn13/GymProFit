# Lote 11 — el tiempo por sesión y crear o editar una rutina

**Fecha:** 2026-11-08 · **Tareas:** GP-243, GP-244 · **PR:** #94 (API y tableros), #95 (app)

Diseño aprobado el 06-11: `documentacion/diseno/2026-11-06-rutinas-tiempo-y-crear/` (README y cuatro tableros).

## Qué se hizo

| Tarea | Lado | Commit | Qué |
|---|---|---|---|
| — | docs | `cc33827` | Los tableros y la especificación, en su carpeta. |
| GP-244 | API | `086c815` | `GET /ejercicios/buscar` con `grupoMuscular` repetido o con comas (con uno, como antes). `POST` y `PATCH /rutinas-ejercicios` aceptan `repeticionesMin`, `repeticionesMax` y `medida` opcionales. La tabla compartida de la duración. |
| GP-243 | app | `3af4a89` | La hoja «¿Cuánto tiempo tienes por sesión?»: «ahora», la frase, las rutinas en bloques con su línea y su lista, la regla y el aviso de rehacer. |
| GP-244 | app | `b5c68b9` | Crear en tres pasos (`CrearRutinaActivity`, `AnadirEjerciciosActivity`, `TuRutinaActivity`), editar en la misma pantalla del paso 3, la duración calculada (`DuracionRutina`), todo lo que dice la pantalla en `BorradorRutina`, y la rutina vieja sin ejercicios en Entrenar. Fuera `ResumenCrearRutinaActivity`, `EditarRutinaActivity` y sus layouts, como pide el README. |
| GP-243, GP-244 | app | `073e9ae`, `0e4375b`, `80fb180`, `c3f1f97` | Las tres pasadas del revisor (abajo). |

### La pauta como rango (decisión del propietario, 08-11)

El README pide «3 series de 8 a 12» de entrada y cambiar la pauta con «repeticiones o segundos», pero la API solo guardaba `repeticiones` en una rutina propia: el mapeador ignoraba el rango y la medida. Se preguntó y se amplió, de forma aditiva: con el rango, `repeticiones` = máximo (lo que leen las builds repartidas, como en las copias de `ProgramaService`); con solo `repeticiones` (una build vieja), mín = máx = ese número y la medida no cambia; rango a medias, fuera de 1 ≤ mín ≤ máx o medida desconocida, 400.

### Decisiones de texto que no fija el README

- La frase de la hoja: «cada rutina se queda en sus N básicos» cuando en todas lo que queda son básicos y salió algo; si no, «salen N ejercicios de tus rutinas», «los básicos pasan a N series» o «tus rutinas se quedan enteras».
- La frase del tiempo al crear: «te cabe un ejercicio más» si con uno más de los de por defecto (3 × 8–12, 90 s) sigue dentro; si no y no se pasa, «Justo en tus N minutos»; con 75+, por encima no se pasa («Unos 80 minutos, en tus 75 o más»).
- El nombre: las zonas del paso 1 en el orden de sus tarjetas; buscando entre todos, las de los ejercicios por series (hasta tres, sin cardio ni cuerpo completo); sin ninguna, «Mi rutina».
- Guardar va paso a paso (la rutina, las filas que ya no están, las que cambian, las nuevas) y apunta cada id: un reintento tras un corte no duplica nada. Un borrado que da 404 en un reintento cuenta como hecho.

## Tests (todos vistos en rojo)

- API: `BuscarEjerciciosPorZonasTest` (5; rojos los dos de varias zonas; aislamiento del catálogo según DEC-027: otro usuario y el invitado reciben lo mismo y nada de nadie); `PautaRutinaPropiaTest` (4, todos rojos: rango, segundos y vuelta, build vieja, 400); `ReglasProgramaTest.tabla_compartida_con_la_app` (rojo cambiando una cifra de la tabla). `mvnw verify`: 1306 en verde.
- App: `TiempoRutinasTest` (11: frase y línea sin recorte, solo extras, extras y series, 75+, bloques; rojo por mutación y, tras la pasada 1, 3 rojos con el espacio duro), `DuracionRutinaTest` (la tabla compartida), `BorradorRutinaTest` (9: nombre por zonas y buscando entre todos, cabe uno más, justo, te pasas, subir, bajar, quitar, pauta por defecto, no guardar sin ejercicios). Nueve mutaciones de `BorradorRutina` y `DuracionRutina`, cada una con su test en rojo. 1249 en verde; lint sin errores.

## Revisor de UI

Hoja del tiempo, tarjeta de una rutina vieja sin ejercicios, los tres pasos de crear y editar, en las cuatro combinaciones.
- **Pasada 1**: ningún bloqueante. 2 altos: la zona elegida salía teñida de dorado (la tarjeta marcada de Material pinta `colorSecondary`, que es el dorado; en claro el texto bajaba a 4,49:1) y las píldoras de zona se marcaban como casilla. 2 medios: el menú anclado se fundía con las filas en oscuro (ahora lleva el borde `gp_stroke`, como el desplegable) y la barra del tiempo quedaba suelta sin ejercicios. Bajos: espacio duro en la frase, «min» y «ahora» en Barlow, «unos 0 minutos», temblor recortado, radio de la descripción y dos frases en inglés.
- **Pasada 2**: 2 altos, los dos de las píldoras (el `Chip` anunciaba «no seleccionado» aunque no se pudiera marcar y no dejaba cambiar su fuente): ahora son `MaterialButton`. 2 medios: la barra del paso 2 con 0 ejercicios y «For beginner.». 3 bajos de espaciado y borde.
- **Pasada 3**: sin bloqueantes, altos ni medios. Los 2 bajos, arreglados y medidos a mano por volcado: tarjeta sin ejercicios con 8,4 dp arriba y 8,0 abajo; a letra 1,3, 12 dp entre la frase de zonas y la del nivel.
- Sin comprobar: el movimiento con «Quitar animaciones», el arrastre con el asa y TalkBack de oído (las acciones de la fila están registradas y el ⋮ ofrece las mismas).

## Producción

API desplegada (`/actuator/info` → `aed534e`) y comprobada con `prueba-244-a@gymprofit.app`: una zona, 84 ejercicios; pecho y hombros, 219; una zona que no existe, 400; la fila guarda de 8 a 12 con `repeticiones` 12; una build vieja con `repeticiones` 10 deja 10 a 10; rango al revés, 400.

Con la release y esa cuenta: seguir «Cuerpo completo» a 60 y pasar a 30 (la hoja decía 25, 30 y 25 min; la API, lo mismo, 3 ejercicios cada una); crear «Pecho y hombros» por zonas y «Pierna, hombros y brazos» buscando entre todos, ordenadas con subir y bajar (sin arrastrar) y guardadas con los 25 minutos que decía la pantalla y el orden elegido; editar la segunda (una serie más, quitar un ejercicio): 20 minutos y dos filas; salir a medias avisa en el paso 2, en el aspa del paso 3 y al editar, y «Descartar» cierra el asistente entero. La cuenta se borró y el login da 401.

## Para el propietario

- Recursos que quedan sin uso tras el lote y no se han borrado sin preguntar: `EjercicioSeleccionadoAdapter`, `item_ejercicio_seleccionado`, `dialog_series_reps`, `item_crear_lista`, `view_paso_crear`, y las cadenas de la hoja vieja y de las pantallas que se fueron (`seguir_sin`, `seguir_completa`, `seguir_basicos`, `crear_rutina_*`, `anadir_ejercicios_*`, `resumen_crear_rutina_*`, `editar_rutina_*` salvo `editar_rutina_guardado`, `error_duracion_invalida`, `nivel_y_duracion`, `selector_grupo_nivel`, `envivo_anadido`…).
- Fuera del lote, vistos por el revisor: el título del aviso de salir dice «lo que has apuntado» también al crear una rutina; la cabecera del menú anclado va en mayúsculas y se corta a 1,3; el paso 2 en inglés sigue el orden del nombre en español (la API ordena por `nombre`), y el catálogo tiene datos raros («Bodyweight Flyes … with an EZ bar»); «Más programas» y la cabecera del programa de Entrenar siguen con «·».
- Local: rutinas 123452 y 123453 (esta, creada para el punto 10) en la cuenta `prueba`.
