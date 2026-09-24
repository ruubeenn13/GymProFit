# Bloque de diseño 2c — estado al cortar la sesión (2026-09-24)

Encargo: GP-089, GP-062, GP-071 y GP-088, en ese orden, un commit por tarea.
La sesión se cortó a mitad. Este informe dice qué está hecho, qué está a medias
y qué falta. **No es el informe final.**

## Estado

| Tarea | Estado | Dónde |
|---|---|---|
| Regla de seguridad en `CLAUDE.md` | Hecha | `9ca07d3` |
| GP-089 tipografía | Código en `main`; verificación a medias | `3de9436` |
| GP-062 cabecera y zonas pulsables | Sin empezar; cifra verificada en ejecución | — |
| GP-071 códigos de API a la vista | Sin empezar; barrido iniciado | — |
| GP-088 récords | API escrita, **sin compilar**, en rama aparte | `wip/gp-088-records` (`d06b372`) |

## GP-089 · Una sola tipografía

**Hecho.** Barlow 400/500/600 (`res/font/barlow*.ttf`, `web/fuentes/barlow-*.woff2`,
de `google/fonts`, la misma fuente que la condensada ya empaquetada). La escala M3
del tema apunta a `dimens.xml`, `text_eyebrow` pasa a 13 sp, y las gráficas
(`ChartStyler.textoSp`) y la barra de navegación dejan de fijar tamaños por debajo de 13.
Fuera las tres fuentes del sistema que quedaban en layouts (`monospace` de la
licencia, `sans-serif` y `sans-serif-medium` de los menús). La licencia nombra
Barlow y Barlow Condensed. Corregido el comentario de ad0eaf0: la familia sí tiene 400.

**Hallazgo al verificar.** Con `android:fontFamily` en el tema, todos los titulares
perdían la condensada: un `fontFamily` de tema se aplica como atributo de la vista y
gana al `textAppearance`. Se ve en las primeras capturas de «después». La fuente por
defecto va ahora en `android:textAppearanceSmall`, y `TipografiaTest` impide que vuelva.

**Medido en el árbol de accesibilidad** (servicio `MedidorAccesibilidad`, solo en
`debug`): antes, 21 textos distintos por debajo de 13 sp en las pantallas en ES claro
(la barra de navegación a 12,5, las etiquetas a 12,2, chips de 12). Después, en las
nueve primeras pantallas medidas, el único texto por debajo de 13 es el distintivo
`PRUEBAS · LOCAL`, que solo existe en `debug`.

**Web** (medido con Playwright, 390 px): antes, cuerpo en `-apple-system`; después,
solo Barlow y Barlow Condensed en las tres páginas, mínimo 14 px. La etiqueta
`.estado` pedía la condensada en 400, que no se sirve; va en 500. **Lista para
subirla.**

**Falta:**
- Capturas de «después» de todas las pantallas en ES/EN × claro/oscuro: la tanda se
  cortó. Las de «antes» están completas (120).
- Cinco pantallas con la letra del sistema a 1,3.
- Comprobar diálogos, snackbars y desplegables abiertos, que no salen en el recorrido.
- Los *toast* los pinta el sistema desde Android 12 y no se les puede cambiar la
  fuente: es la única fuente del sistema que quedará a la vista.

## GP-062 · Cabecera y zonas pulsables

**Verificado en ejecución antes de tocar** (la cifra se sostiene): botones de
cabecera a 40×40 dp en Logros, Estadísticas de nutrición e Inicio; el enlace de
registro del login mide 227×23 dp. Además `btnBack` no tiene nombre accesible en
varias pantallas. Las tres pantallas «con las dos cabeceras»
(`activity_anadir_alimento`, `activity_comida`, `activity_crear_alimento`) **solo
tienen la manual**: «Toolbar» era un comentario del XML. Registrar sesión tiene 14
zonas por debajo de 48 dp y Sesiones, 7.

## GP-071 · Hallazgos del barrido, sin arreglar

- `RutinaAdapter`: «N ejercicios» y «min» a fuego; `NutricionFragment`: «/ N kcal»,
  «N kcal»; `DetalleRutinaActivity` y `ResumenCrearRutinaActivity`: «min»;
  `AlimentoAdapter` y `AdminAlimentoAdapter`: «kcal/100g»; `MedicionesActivity`:
  «kg»/«cm»; `RutinasFragment.NIVEL_TODOS = "Todos"`; `AdminUsuariosActivity`:
  «USER»/«ADMIN»; `AdminUsuarioAdapter` enseña el rol sin traducir; los del encargo.
- La fecha de Inicio salió en inglés con la app en español en una de las tandas
  («Thursday, September 24»); en otra, bien. Sospecha: se formatea con
  `Locale.getDefault()` y no con el idioma de la app. Sin confirmar.

## GP-088 · Récords

**Decisión, medida:** `progreso_ejercicios` desaparece y los récords se calculan al
pedirlos. Todas las series de un usuario sintético con 450 sesiones y 10 800 series
se leen en 11,4 ms (`ANALYZE`); guardarlos obligaría a invalidarlos al borrar una
sesión, editar una serie o borrar un ejercicio de una sesión. Usuario sintético en la
BD local: `carga88` / `Carga1234.`.

**Escrito en la rama, sin compilar:** `CalculadoraRecords` (lógica pura; su test, 12
casos, **sí pasó**), `RecordService`, `RecordController` (`GET /records?desde=`,
`GET /records/ejercicio/{id}/progresion`), campos `recordsBatidos` y
`primerasMarcas` en el guardado completo, rutas viejas `historial` y
`record-destacado` leyendo de las series, migración `V202609242200` que retira la
tabla, `Musculos` (la normalización de músculo, sacada de `SesionEntrenamientoService`),
`RecordsTest` de integración y los tests que tocaban la tabla ajustados.

**Falta:** compilar y `mvnw verify`, regenerar o limpiar las clases jOOQ de la tabla,
toda la parte Android (inicio, resumen, pantalla Récords, gráfica, zonas en un solo
sitio) y las capturas.

## Visto y no arreglado

- Si Android mata el proceso y restaura una pantalla que no es Splash, el token no se
  carga y la app cierra la sesión (la inicialización de `UtilREST` solo está en
  `SplashActivity`). Se vio al abrir pantallas por adb; es el mismo camino que la
  restauración tras muerte del proceso. Sin reproducir por esa vía.
- `DetalleRutinaActivity` abierta desde el trampolín enseña «Ejercicio 1…4»; falta ver
  si pasa también entrando desde Rutinas.
