# Lote 10 — series por semana, nombres en el otro idioma y el aro de la sesión en vivo

**Fecha:** 2026-11-08 · **Tareas:** GP-249, GP-245, GP-251, GP-252 · **PR:** #91 y #93 (API), #92 (app)

## Qué se hizo

| Tarea | Lado | Commit | Qué |
|---|---|---|---|
| GP-249 | API | `fa1770c` | `series` por semana en `GET /sesiones/usuario/{id}/volumen-semanal`, con `SERIES_DEL_EJERCICIO` (la regla del resumen y del Historial); una consulta agrupada más, ninguna por semana. Aditivo. |
| GP-249 | docs | `2a546a2` | La regla en DEC-060. |
| GP-249 | app | `8e06b69` | Sin kilos en el período, Progreso e Inicio pasan a «Series por semana» (`MedidaSemanal`): barras, cifra de esta semana y porcentaje con la regla de los kilos. Con kilos, igual que antes. Sin kilos ni series, Progreso lo dice e Inicio esconde el bloque. |
| GP-245 | API | `88b0032`, `30e49be` | Una copia de plantilla sin cambiar da su nombre, descripción, categoría y notas de ejercicio en el idioma de la petición (`TextosRutina`); cambiada, la suya. Rutinas, `/rutinas-ejercicios`, programa seguido, sesión, Historial y diario. Sin columnas nuevas. |
| GP-251 | app | `0a958fb`, `cf5f76b` | Tres causas, las tres de la app (abajo). |
| GP-252 | app | `b421a05`, `8155905` | Sin hacer, el aro; hecha, el círculo relleno con borde y check (`MarcaSerie`), en la sesión en vivo y en Apuntar entreno. TalkBack dice el estado y el botón se nombra por lo que hace. |

`ProgramasTest.idioma_de_las_copias` fijaba lo de antes (una copia en inglés se leía en inglés también en español) y se puso al día: es justo lo que GP-245 cambia.

## GP-251: las causas

La API ya elegía `nombre_en` en todas las rutas que dan nombres de ejercicio y de alimento: se comprobó en local y en producción (873 ejercicios activos, todos traducidos). Las tres causas eran de la app:

1. **Detalle de rutina («Press de banca» junto a «Bench press»)**: el catálogo de ejercicios se guardaba en memoria sin su idioma (`CatalogoEjercicios`). Cambiar de idioma no reinicia el proceso, así que los ejercicios activos salían del catálogo pedido antes en español y los desactivados, que llegan con la rutina, en inglés. Ahora se guarda con el idioma (`CachePorIdioma`).
2. **Resumen («Frente a la última vez») y «Para recuperar»**: `Accept-Language` salía de `Locale.getDefault()`, que Android vuelve a poner en el del sistema cuando algo del propio proceso aplica su configuración. Reproducido con el medidor de accesibilidad del revisor (un servicio del mismo proceso): la pantalla en inglés y la API contestando en español. Con TalkBack real no pasa, pero la cabecera no puede depender de eso: ahora es el idioma de la app (`IdiomaApp.peticiones`: el elegido, el de la app en Android o el del sistema entre los dos).
3. **Sesión en vivo abierta al cambiar de idioma** (pasada 2 del revisor): la sesión guarda sus textos y no sabía en qué idioma llegaron. Ahora guarda el idioma y, si cambia, los vuelve a pedir (`LogicaSesion.traducir`).

**Traducciones que faltan en el catálogo** (no se hacen en este lote):
- **Ejercicios**: 1 de los 1171 de producción, «Curl De Muñeca Con Barra» (id 10, desactivado, de la importación antigua de wger). Otros 12 tienen el mismo nombre en los dos idiomas porque es inglés en los dos («Superman», «Good Morning», «Pallof Press»…): no faltan.
- **Alimentos de Open Food Facts**: todos van solo en español por origen (`productos_off` no tiene columna en inglés; 198.224 en la base local). En la base local, los 8 materializados en `alimentos` no tienen `nombre_en`. En producción no se pudo contar sin acceso a la base; por la API no se pueden listar.
- Lo escrito por el usuario (sus platos y alimentos) no se traduce, por decisión del lote.

## Tests (todos vistos en rojo)

- API: `VolumenSemanalTest` (+4: peso corporal, con kilos y mezcla, límites y aislamiento, sesión sin detalle por serie); `NombreRutinaCopiaIdiomaTest` (8: seguido en inglés leído en español y al revés, programa seguido, sesión/Historial/diario, renombrada, propia sin plantilla, Historial del catálogo, notas). Rojo por falta del campo o del código. `verify`: 1296 en verde.
- App, rojo por mutación: `MedidaSemanalTest` (sin kilos, algunos, todos, nada, API vieja), `CachePorIdiomaTest`, `IdiomaPeticionesTest`, `MarcaSerieTest` (aro, aro de la que toca, círculo hecho, estado y nombre), `LogicaSesionTest` (+3: volumen a peso corporal, traducir, traducción incompleta). `test` y `lintDebug`: 1227 tests en verde.

## Revisor de UI

Progreso, Inicio, Entrenar, sesión en vivo y resumen, en las cuatro combinaciones.
- **Pasada 1**: 1 bloqueante (el aro de la serie que toca salía relleno: el tinte de fondo pinta el óvalo entero), 1 alto (notas de los ejercicios de la copia en el idioma en que se siguió; arreglado en la API con la regla de GP-245, `30e49be`), 2 medios («Volumen 0 kg» en la cabecera a peso corporal; el círculo hecho sin contraste en claro, 1,14:1) y bajos.
- **Pasada 2**: sin bloqueantes ni altos. 2 medios arreglados y medidos a mano: la sesión abierta pasa al idioma nuevo (antes «Para empezar A», «Sentadilla sin peso» y las notas en español con la app en inglés; después «Getting started A», «Bodyweight Squat», «Once every set reaches 20…»); «Primera vez» en neutro (#A1A9B6 sobre #1B1E24, antes `colorPrimary`). También el punto doble de TalkBack («peso.. Primera vez»).
- Medidas de la pasada 2: borde de la hecha 5,25:1 en claro y 9,58:1 en oscuro; aro gris 3,41:1 / 3,30:1; aro de la que toca 5,63:1 / 5,81:1; botones de 48×48.

Bajos que se quedan (no son del lote, o son decisión): «Volumen 0 kg» antes de marcar la primera serie (al empezar el contador va de cero, también con kilos); «series esta semana» parte en dos líneas a 1,3; «Getting started / A» se corta en la tarjeta de Inicio en inglés; músculos en tres formatos según la pantalla; alturas distintas en «Por tu cuenta»; «0 récords» en dorado en Progreso; «SERIE» y «ANTERIOR» casi se tocan a 1,3; el «·» que lee TalkBack en la cifra de Progreso; la línea de comida de «Para recuperar» en plural con un solo alimento.

## Producción

API de #91 desplegada a las 12:05 (`92c9ce9`) y de #93 a las 13:01 (`37b1f8e`), comprobadas en `/actuator/info` y por la API con cuentas de prueba nuevas (`series` por semana; programa seguido en inglés leído en español; renombrada que se queda; notas en los dos idiomas).

Con la release de la rama y cuentas nuevas:
- Peso corporal (5 sesiones a 0 kg): Inicio dice «Series por semana» y «+100 % en 5 semanas»; Progreso, «4 series esta semana · el 33 % de la pasada». Ningún «0 kg» ni «Kilos movidos» en ninguno de los dos.
- Programa seguido en inglés (por la API con `Accept-Language: en`, que es lo que manda la app en inglés) y la app en español: «Para empezar A» en Entrenar, Inicio, el Historial y las hojas «¿Qué rutina hoy?» y «¿Qué entrenaste?»; la renombrada («Mi rutina B») se queda.
- App en inglés: la rutina propia con ejercicios del catálogo («Romanian Deadlift from Deficit», «Seated Biceps»), con el catálogo cargado antes en español en el mismo proceso, y su resumen con «Compared with last time» en inglés. Con kilos, Inicio sigue diciendo «Kilos por semana».

Cuentas borradas: `prueba251a` (la del recuento del catálogo), `prueba10a` y `prueba10c`, DELETE 200 y login 401 en las tres.

## Pendiente para el propietario

- `envivo_marcar` (ES y EN) quedó sin uso: el botón usa ahora las cadenas de Apuntar entreno. No se ha borrado sin preguntar.
- En la base local quedan la cuenta `peso249` y dos sesiones nuevas en `prueba` (13723 y 13724), de las pruebas.
