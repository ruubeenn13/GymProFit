# GP-285, lote 1 de Liquid Glass: sistema visual, cristal vivo, barra flotante e Inicio (2026-11-26)

Rama `gp-285-liquid-glass-lote-1` (PR #144). El propietario aprobó el diseño con cuatro correcciones; están hechas en el **remate**, al final de este informe, que es donde están las cifras de rendimiento que valen y el estado final. Sin versión ni etiqueta (DEC-056). Sin cambios de API ni de contratos.

Referencia: `documentacion/diseno/2026-11-25-liquid-glass/` (dirección A, aprobada el 25-11-2026). Decisión: DEC-070. Sistema: `app/GymProFit/DESIGN.md`, «Liquid Glass».

## Commits

| Hash | Commit |
|---|---|
| `8eac5e27` | docs(documentacion): referencia de diseño de Liquid Glass, dirección A |
| `6b1c2486` | feat(android): cristal de Liquid Glass: estático, vivo en tres niveles y fondo ambiental |
| `8501a872` | feat(android): barra flotante de cristal e Inicio en Liquid Glass |
| `14944cdb` | docs(decisiones,documentacion): DEC-070, Liquid Glass con componente propio y contraste medido |

Más el commit de este informe y del `CHANGELOG.md`.

## Qué se entrega

- **Sistema visual** (`values/estilos_cristal.xml`, tokens `gp_ambiente_*`, `gp_cristal_*`, `gp_vivo_*`, `gp_lente*`, `gp_mas_*` en los dos temas): fondo ambiental (`FondoAmbiental`), cristal estático (`Cristal`, `Widget.GymProFit.Card.Cristal` y sus tres variantes), selección «lente», botón principal y secundario, chip. `Widget.GymProFit.Card` no se toca.
- **Cristal vivo** (`ui/cristal/`): `LienzoCristal` guarda en un `RenderNode` lo que hay detrás y `CristalVivo` lo redibuja desenfocado en su hueco. Tres niveles, decididos en `NivelCristal.elegir(api, altoContraste, techo)`: 1, API 33+, desenfoque y refracción (AGSL, `res/raw/cristal_refraccion.agsl`); 2, API 31–32, desenfoque; 3, API 30 o menos, texto de alto contraste o techo de rendimiento, superficie al 98 %.
- **Barra flotante**: cápsula de cristal vivo de 64 dp a 16 de los lados y 26 del borde de abajo más el hueco del sistema, con el «+» separado (círculo de 64). El contenido de las cuatro pestañas pasa por debajo (`MainActivity.aplicarHuecoInferior`). Se conserva todo lo que hacía: nombres, rol y posición para TalkBack, tooltips, el «+» que gira a «×», el velo y la hoja que no tapan la barra, y la barra de la sesión encima, ahora también cristal vivo. La hoja del «+» es cristal vivo.
- **Inicio**: las mismas secciones, textos y lógica en el sistema nuevo. «Hoy toca» en la tarjeta héroe; «Tu día» en tres tiles con barra; progreso, músculos, últimos entrenos, primeros pasos y comida que toca en cristal.
- **Tema claro**: diseñado con las mismas reglas (cristal blanco esmerilado sobre fondo cálido con brillos suaves). **Pendiente de tu aprobación con las capturas.**

## El componente: propio frente a BlurView

| | Propio | BlurView 4.0.0 (Dimezis) |
|---|---|---|
| Licencia | código de la app | Apache-2.0, leída en el `LICENSE` y el README de su repositorio (`gh api repos/Dimezis/BlurView`): compatible con una app cerrada, con aviso en «Licencias de terceros» |
| Mantenimiento | el nuestro | activo: 4.0.0 del 25-10-2026, último commit del mismo día, 11 incidencias abiertas |
| Distribución | — | JitPack (ya en `settings.gradle` por MPAndroidChart) |
| Tamaño | — | +27.856 bytes de dex y una textura de 6.386 bytes (medido descomprimiendo los dos APK; la diferencia de 4,5 MB entre dos APK debug que vi a media tarea era del empaquetado incremental, no de la librería) |
| Nivel 2 | `RenderNode` + `RenderEffect` | lo mismo (`BlurTarget` + `RenderNodeBlurController`) |
| Nivel 1 | efecto encadenado en el mismo nodo | no lo trae; su nodo es privado, así que el shader iría sobre la vista entera, hijos incluidos |
| Nivel 3 | superficie opaca | en API 30 o menos desenfoca con OpenGL (29–30) o RenderScript: habría que apagarlo |
| Saturación 180 % | sí | no |

Coste por fotograma, medido con un prototipo (la cápsula de la barra con `BlurView`, mismo radio de desenfoque; no está en el repositorio) y el mismo guion: ver la tabla de abajo. **Sin diferencia medible.** Se elige el propio: BlurView solo cubriría el nivel 2 y lo demás habría que escribirlo igual; no hay necesidad que justifique la dependencia.

## Rendimiento (DEC-021)

**Guion fijo** (`medir.py` en el scratchpad de la sesión): `dumpsys gfxinfo com.gymprofit.app reset`; cuatro veces (tres `adb shell input swipe` hacia arriba y tres hacia abajo, del 71 % al 29 % del alto, 300 ms cada uno y 450 ms de pausa); `dumpsys gfxinfo`. Unos 1.090 fotogramas por pasada en Inicio, tema oscuro, variante debug, cuenta con datos.

**Protocolo que vale** (el tercero que probé; los dos primeros no): la app **compilada entera** tras instalarla (`cmd package compile -m speed -f`), el emulador con 3 GB y la GPU del equipo, y las variantes **intercaladas** (una pasada de cada una por ronda, empezando cada ronda por una distinta). Con eso las pasadas se repiten al fotograma.

| Emulador | Antes (`main`) | Después | Rondas |
|---|---|---|---|
| API 36 (nivel 1) | 0,73 % lentos · p50 18 · p90 19 · p95 19 · p99 19 ms | **0,82 %** (0,73–0,82) · p50 18 · p90 19 · p95 19 · p99 19 ms | 3 |
| API 32 (nivel 2) | 0,73 % · p50 17 · p90 18 · p95 18 · p99 19 ms | **1,2 %** (1,2 en las cuatro) · p50 17 · p90 18 · p95 19 · p99 19 ms | 4 |
| API 24 (nivel 3) | 0,0 % · p50 5 · p90 5 · p95 5 · p99 6 ms | **0,0 %** · p50 5 · p90 5 · p95 5 · p99 6 ms | 4 |

En fotogramas: API 36, 8 lentos de 1.090 antes y 9 después; API 32, 8 de 1.090 antes y 13 de 1.081 después.

Por niveles y frente a BlurView, en el mismo emulador y la misma tanda:

| | Base | Nivel 1 | Nivel 2 | Nivel 3 | BlurView |
|---|---|---|---|---|---|
| API 36 (3 rondas) | 0,73 % | 0,82 % (0,73–0,82) | 0,82 % (0,73–0,82) | 0,73 % (0,73–0,82) | 0,73 % (0,73–1,1) |
| API 32 (3 rondas) | 0,73 % | — | 1,11 % (1,11–1,3) | 1,3 % (1,11–1,3) | 1,11 % (1,11–1,2) |

Los percentiles son los mismos en todas las columnas (API 36: p50 18, p90 18–19, p95 19, p99 19 ms; API 32: p50 17, p90 18, p95 18–19, p99 19 ms).

**Lectura.** Ningún nivel empeora de forma apreciable: en API 36 la diferencia es un fotograma de cada mil. En API 32 hay cinco fotogramas lentos más por pasada (+0,47 puntos), pero **los hay igual en el nivel 3, que no desenfoca**: vienen del rediseño (fondo ambiental y tarjetas translúcidas), no del cristal vivo. **Ningún dispositivo baja de nivel de salida.** Para un móvil real que sí lo necesite está el vigía.

**El vigía (`VigiaCristal`, `RendimientoCristal`).** Mide el dibujo de cada fotograma (órdenes + GPU, `FrameMetrics`) y baja un nivel tras tres ventanas seguidas de 120 fotogramas con más del 25 % por encima de 33,4 ms. Los números salen de una tanda con registro en API 36: la duración total de `FrameMetrics` no sirve (incluye esperas de búfer que `gfxinfo` descuenta: con la superficie opaca había ventanas con 87 de 120 fotogramas por encima de 33 ms), y el dibujo pasó de 33,4 ms en 0 a 7 de cada 120, igual en los tres niveles (media de 15 a 23 ms en todos). La primera versión medía el total y **bajó sola del nivel 2 al 3** en una pasada: se corrigió antes de medir nada más.

**Lo que no valió, para que no se repita.** (1) Pasadas en bloque sin compilar: recién instalada, la app corre interpretada y una misma variante dio del 0,8 % al 37 %. (2) El emulador con 2 GB se quedó sin memoria a los diez minutos (310 MB en swap) y todo empeoraba con el tiempo, fuera cual fuera la variante. (3) Mientras otra sesión pasaba tests y auditorías de Lighthouse en el mismo equipo, hasta la base daba un 25 %. Esas tandas están descartadas; las primeras que hice en API 36 (3,0 % la base y 4,0 % el nivel 1, sin compilar) apuntaban en la misma dirección con más ruido.

## Legibilidad

`ContrasteCristalTest` recorre el fondo ambiental entero en seis tamaños de pantalla (la misma cuenta que pinta `FondoAmbiental`), compone cada capa de cristal con sus tres tonos y mide cada par de texto sobre el peor punto, en los dos temas y en los tres niveles. Con los valores de la maqueta falla (visto en rojo: secundario 3,19:1, acento 2,87:1, etiqueta activa de la barra 2,42:1); los cambios, con su cifra, están en la tabla «Ajustes al implementar» del README de la referencia. Peor par de cada grupo, en `app/GymProFit/DESIGN.md`.

«La última vez, hace 3 días» va en un chip de cristal con el texto principal (5,48:1 en el peor punto).

## Tests

**1.389 tests unitarios** (`testDebugUnitTest`), **30 nuevos, todos vistos en rojo**. `lintDebug`: 0 errores.

| Clase | Tests | Cómo se vieron en rojo |
|---|---|---|
| `NivelCristalTest` | 7: API 33 y 36 → nivel 1; 31 y 32 → 2; 24 y 30 → 3; el alto contraste fuerza el 3 en las seis versiones; el techo baja y nunca sube; `unoMenos`; `deNumero` | 5 con la clase en esqueleto (devolvía siempre opaco); los otros 2 (alto contraste y API ≤ 30), quitando su línea |
| `RendimientoCristalTest` | 5: scroll fluido, por debajo del umbral, tirón suelto, tirón sostenido, límite exacto | los 5 con el esqueleto; el del límite, además, cambiando `>` por `>=` |
| `ContrasteCristalTest` | 7: texto sobre el fondo, sobre el cristal estático, gráficas y pistas, la barra en los tres niveles, el «+», la cuenta de WCAG, el brillo al 70 % | 3 con los valores de la maqueta; gráficas con el blanco al 20 % y la pista al 14 %; el «+» con otro icono; la cuenta invirtiendo el alfa; el brillo con 0,80 (y hubo que reescribirlo: comparaba la constante consigo misma) |
| `CristalTest` | 6: medidas del diseño; el cristal vivo fuera de su lienzo; el efecto no se crea al dibujar; la tarjeta de siempre no cambia; Inicio entero en cristal; DESIGN.md recoge el sistema | cada uno quitando lo que prueba (26 dp → 24; la hoja dentro del lienzo; `prepararEfecto()` en `onDraw`; la tarjeta transparente; una tarjeta opaca en Inicio; sin el encabezado en DESIGN.md) |
| `LetraGrandeTest` | +1: la etiqueta de la barra no crece más de lo que cabe | en rojo de verdad: con letra 0,85 el código la subía a 1,0. Arreglado |
| `TextoBarraSesionTest` | 4: parte por el primer separador y lo quita; ninguna línea empieza ni acaba en «·»; con un solo separador; sin separador no se toca | 3 con la función en esqueleto (devolvía el texto tal cual); el cuarto, quitando la guarda de «sin separador» |

Tests existentes que el rediseño cambia: `CimientosTest` (el «+» es un círculo de 32 dp de radio, no de 26; «Hoy toca» ya no es la tarjeta protagonista) y `TarjetaEstadosTest` (el anillo de foco de la pestaña va en el color del texto de la barra).

**Instrumentado** (`BarraNavegacionRecorteTest`, 3 tests): el «+» se ve entero; la barra mide lo del diseño (64 · 16 · 26 + hueco · «+» de 64 a 10 dp · pestañas de 48 dp o más a 360 dp de ancho); el nivel se puede leer en esta versión. En verde en API 36 y API 32, y en API 32 también con `high_text_contrast_enabled` a 1 (nivel opaco). Con el código final, 3 de 3 en **API 36, API 32 y API 24**.

Los cuatro `*WebTest` que fallan en local no son de este cambio (ver «Fuera del lote»).

## Revisor de UI

Dos pasadas del revisor (`revisor-ui-android`) en Inicio, la barra, la hoja del «+» y la barra de la sesión, en API 36 (nivel 1) y las cuatro combinaciones de siempre: ES · claro · 1,0 — EN · oscuro · 1,0 — ES · oscuro · 1,3 — EN · claro · 1,3.

**Primera pasada: 1 alto, 3 medios, 7 bajos.**

| | Qué | Arreglo |
|---|---|---|
| A1 | En claro, con la hoja abierta, el cristal salía gris: secundario de la hoja a 4,21–4,46:1 y pestaña activa a 3,73:1 | velo del «+» al 35 % en claro (`gp_velo_cristal`), tinte del cristal vivo al 80–70 %, acento `#8A2E00`; el test de contraste cuenta ya el velo |
| M1 | Con letra 1,3, «Empezar» quedaba bajo la barra | tarjeta héroe 36 dp más baja |
| M2 | Dos manchas en las puntas de las pistas a 0 % | `BarraProgresoCristal` |
| M3 | La barra de la sesión dejaba el reloj solo o un «·» colgando | parte por el primer separador (`TextoBarraSesion`) |
| B1 | Carrusel de «Tus últimos entrenos» cortado a 20 dp del borde | a sangre (sin recorte en la columna ni en la sección) |
| B2 | Asa de la hoja invisible (1,03:1 en claro) | en el texto secundario del cristal |
| B3 | Barras del sistema más claras que la pantalla con el velo | resuelto en la de estado por el velo más suave; queda la de los tres botones (abajo) |
| B7 | Tipografía de la maqueta no aplicada | es de toda la app: fila en «Ajustes al implementar» del README |

**Segunda pasada (mismas pantallas y combinaciones): 0 altos, 1 medio, bajos.** A1, M1, M2 y B2 resueltos, con medida: secundario de la hoja en claro 5,31–6,77:1, pestaña activa sobre su lente 5,50–5,85:1, «Empezar» entero con letra 1,3 (acaba en y=2034 px y la barra empieza en 2038), pistas uniformes, asa a 5,77–6,95:1 en claro y 9,63–10,72:1 en oscuro. Zonas pulsables: ninguna por debajo de 48 dp. Todos los controles con nombre, rol y estado.

El medio nuevo y lo que quedaba a medias, arreglado después y **comprobado a mano con medida**, como manda la regla cuando la última pasada solo deja medios:

| | Qué | Arreglo | Medida a mano |
|---|---|---|---|
| Medio | En oscuro, la etiqueta de la pestaña activa bajaba a 4,15–4,47:1 cuando el círculo dorado del trofeo de «Tu progreso» pasaba por detrás | tinte del cristal vivo en oscuro al 72–62 % (era 55–40 %) y lente al 7 % (era 10 %) | barrido de Inicio de arriba abajo en pasos de 110 px (34 capturas por combinación) tomando, en cada una, el píxel más claro del fondo a los dos lados de la etiqueta, dentro de la lente: peor punto **5,45:1** en oscuro (ES y EN, `#FFB27A` sobre `#4D4430`, con el trofeo detrás) y **6,24:1 · 6,35:1** en claro (ES · EN). Con la lente al 10 % daba 4,87–4,95:1 |
| B1 | El carrusel seguía cortado (lo recortaba su sección) | `clipChildren` fuera en la columna y en la sección | captura: la segunda tarjeta llega al borde de la pantalla |
| M3 (resto) | El «·» colgaba al principio de la segunda línea | el separador se quita al partir | captura en EN, claro y oscuro: «Workout in progress» / «Upper A · 0:06» |

**Sin arreglar, todos bajos:**

- **El halo naranja del botón principal no se pinta.** El estilo lo declara (`gp_boton_halo`, elevación) pero bajo «Empezar» no hay tinte (medido: el mismo color a 4 y a 30 px del borde). Probé a darle elevación de vista y a cambiar el animador de estados: sigue sin salir, y lo retiré para no dejar cambios sin efecto comprobado. DESIGN.md dice que hoy no se pinta.
- **M1 queda justo con letra 1,3 en español**: 1,5 dp de aire entre «Empezar» y la barra, y depende del contenido (una línea más de texto por encima lo taparía).
- **B3, resto**: en claro, con el velo, la franja de los tres botones del sistema es más clara que la pantalla (2,0–2,2:1 de escalón).
- B4 (contenido que se ve tras los botones del sistema), B5 (viudas en algún texto) y B6 (dos paradas de TalkBack en una tarjeta, que ya estaban en `main`).
- **Estimado, no medido**: la pestaña activa con una barra llena de la gráfica justo detrás. La demo tiene vacías las semanas que caen ahí. Con el tinte y la lente nuevos calculo unos 5:1; el revisor estimaba 3,6:1 con los de antes.

El revisor solo midió el nivel 1 (API 36). Los niveles 2 y 3 se comprueban con `ContrasteCristalTest` (peor par de cada nivel en DESIGN.md) y con las capturas de API 32 y API 24.

## Comprobaciones a mano

- **Los tres niveles, en los tres emuladores**, ES y EN, claro y oscuro: capturas en el escritorio, carpeta `GP-285 lote 1`, subcarpetas `API 36`, `API 32` y `API 24` (20 por emulador: Inicio arriba, con scroll y el contenido bajo la barra, la hoja del «+», la barra de la sesión y la barra de la sesión con scroll). Hechas con el código final.
- **«Quitar animaciones»** (las tres escalas a 0, API 36, release): la hoja del «+» abre y cierra, el «+» pasa a «×» y vuelve, el scroll bajo la barra va bien. Sin errores en logcat.
- **Texto de alto contraste**: `BarraNavegacionRecorteTest` en API 32 con `high_text_contrast_enabled` a 1 (nivel opaco), en una tanda anterior a los últimos arreglos de color.
- **TalkBack de verdad, no**: lo comprobado es el árbol de accesibilidad (nombres, rol, estado y posición de cada control, en el test instrumentado y en las dos pasadas del revisor), no una sesión con TalkBack encendido.
- **El rendimiento se midió antes de los arreglos del revisor.** Lo que cambió después no toca cómo se dibuja el cristal: colores, una vista de barra propia (un rectángulo redondeado), el corte de un texto y dos `clipChildren`. No lo he vuelto a medir.

## Producción

APK de **release** (R8, firma de release; `app-release.apk`, 6.607.724 bytes, compilado con el código final) instalado en el emulador de API 36, contra `https://api.gymprofit.app` (commit en producción: `20d234d6`; el lote no toca la API).

- Cuenta de prueba `prueba-gp285-a@gymprofit.app`, creada por la API (201).
- Entrar → cuestionario del alta (cuatro pasos) → avisos → **Inicio** de una cuenta nueva (sin entrenos, con «Primeros pasos»): tarjeta héroe, «Tu día» a cero, barra flotante y scroll bajo ella.
- Hoja del «+» (el «+» pasa a «×», el velo no tapa la barra), «Empezar entrenamiento» → sesión en vivo → atrás → **barra de la sesión** sobre la navegación, y la hoja otra vez con «Volver a la sesión».
- Lo mismo con «Quitar animaciones».
- Sin errores de la app en logcat.
- **Cuenta borrada** (`DELETE /usuarios/me`, 200) y comprobado: el login devuelve **401**. La clave se eliminó entonces. La release se desinstaló del emulador.

## Decisiones tomadas

1. **DEC-070** y no 069: la 069 está tomada en la rama de «Un día», sin fusionar.
2. Componente propio (arriba).
3. Cinco valores de la maqueta cambian porque chocan con las reglas de legibilidad: brillo naranja 55 % → 26 %, lente 14 % → 7 % con `#FFB27A`, tinte de la barra en oscuro 55–40 % → 72–62 %, pista de las barras a surco oscuro, barras apagadas de la gráfica 20 % → 45 %. La escala de letra no cambia (sección de 20 sp en Barlow SemiBold, título de 34, cifra de 26): es de toda la app.
4. El cristal estático va sin sombra (es translúcido y se vería a través); solo la lleva el cristal vivo.
5. Se conserva el carrusel de ejercicios de «Hoy toca» (la maqueta lo cambia por chips, que quitarían la pauta y la última marca) y los textos de «Tu día» («2.180 · te quedan», no «1.420 · de 2.450»): el encargo pide no cambiar textos ni lógica. En «Tu día» los aros y los vasos pasan a barras; los vasos se siguen diciendo a TalkBack.
6. La hoja del «+» ya no sube desde debajo de la barra (flota y se vería pasar por detrás): sube 36 dp y aparece.
7. El nivel 3 es opaco al 98 % (al 92–94 % el texto de detrás se entreveía).
8. La barra de la sesión, si no cabe en una línea, parte por su primer separador y lo quita (`TextoBarraSesion`); lo que oye TalkBack no cambia.
9. En claro, el velo del «+» sobre el lienzo de cristal es más suave (`gp_velo_cristal`, negro 35 %; en oscuro, el 62 % de siempre) y el tinte del cristal vivo sube al 80–70 %: con el velo de siempre la hoja y la barra salían grises.
10. Las barras de progreso sobre cristal son una vista propia (`BarraProgresoCristal`), no el indicador de Material, que con pista translúcida dejaba dos manchas en las puntas.
11. La tarjeta héroe se recorta 36 dp (márgenes, chip y bloque del icono de cada ejercicio de 84 a 72 dp) para que «Empezar» no quede bajo la barra con letra 1,3.
12. La referencia estaba en `documentacion/diseno/diseno-liquid-glass-2026-11-25/2026-11-25-liquid-glass/`; se movió a la ruta del encargo.

## Fuera del lote, encontrado por el camino

- **Los tests instrumentados de `main` no compilan**: `FilaSerieRotulosTest` usa `R.layout.item_serie`, borrado en GP-241, y el CI no pasa `connectedAndroidTest`. No lo he tocado. Para lanzar el mío lo aparté en local y lo devolví.
- **Cuatro `*WebTest` fallan en mi máquina** (`BetaWebTest`, dos de `HojasVersionadasWebTest`, `LogoUnicoWebTest`) porque en el árbol quedan `web/dist`, `web/.astro` y `web/node_modules` de la rama de «Un día», que `main` no ignora. No son de este cambio y no los he tocado.
- **Una release vieja en el AVD de API 36.** El emulador tenía instalada una release (apunta a producción). Mi `adb install -r` del debug falló por la firma y no lo vi; tecleé la cuenta demo **local** (`dani.entrena`) y pulsé «Entrar» en esa release. No apareció error ni rastro de petición, pero no puedo descartar que saliera un intento de login a producción con esa cuenta. Desinstalé la release. Ahora las utilidades comprueban la variante antes de teclear.
- Una sesión en curso en el emulador hace que `uiautomator dump` devuelva volcados viejos (el reloj de su barra cambia cada segundo). Apuntado en la nota del emulador del vault.

## Tareas del propietario

1. **Aprobar el rediseño con las capturas** del escritorio (`GP-285 lote 1`), y en particular **el tema claro**, que es diseño mío sobre las mismas reglas. Hasta entonces la rama no se fusiona.
2. Decidir qué se hace con `FilaSerieRotulosTest` (no compila en `main` desde GP-241).
3. Si quieres descartarlo del todo: mirar en los registros de Render si hubo un intento de login de `dani.entrena` el 26-11 (el de la release vieja del emulador).
4. Lo pendiente de antes sigue igual (Render y UptimeRobot a liveness, etc.).

## Dudas

- **El halo del botón principal**: ¿se quiere de verdad (habría que pintarlo a mano, la sombra de Android no lo da) o se quita de la referencia?
- **Margen de «Empezar» con letra 1,3**: 1,5 dp. Si se quiere holgura, la tarjeta héroe tiene que perder algo más (por ejemplo, una línea del subtítulo).
- **La escala de letra de la maqueta** (títulos en Barlow Condensed más grandes) no se ha aplicado: sería para toda la app, en su propio lote.
- **CI**: la rama se sube con este commit; el resultado va en el resumen al propietario.

---

# Remate del lote 1 y GP-286 (2026-11-26, tarde)

El propietario aprobó el diseño con cuatro correcciones vistas en las capturas, y pidió aparte GP-286. Esta sección sustituye, donde se contradigan, a lo de arriba: las cifras de rendimiento, los bajos «sin arreglar» y el estado de la rama. Se fusiona en `main` por la PR #144; el hash de la fusión va en el resumen al propietario. Sin versión ni etiqueta (DEC-056). Solo app: no hay despliegue de API.

## Commits

| Hash | Commit |
|---|---|
| `617f348a` | fix(android,documentacion): el contenido se desvanece antes de las barras del sistema y «Hoy toca» no se parte ni se pega a la barra (GP-285) |
| `c240ee97` | test(android): FilaSerieRotulosTest mira los rótulos de la pantalla que hay, y los tests instrumentados vuelven a compilar (GP-286) |
| `f46a0aaf` | fix(android): con gestos, el hueco del asa del sistema también va macizo (GP-285) |
| `20e7dfa1` | ci(android): la instantánea del AVD del humo de la release se rehace con el emulador de ahora |
| `8125fc88` | test(android): los instrumentados nuevos valen también en Android 7 (GP-285, GP-286) |
| `944a604e` | fix(android): en Android 7 la sombra de la barra y de la hoja ya no sale cortada en recto (GP-285) |
| `dd0f1335` | ci(android): el humo de la release guarda el logcat desde el principio y no se queda colgado |
| `843cfd2b` | fix(android): los bordes del sistema se pintan con una imagen estirada, no con un degradado de ocho paradas (GP-285) |

Más el commit de este informe y del `CHANGELOG.md`.

## Los cuatro fallos, cómo quedan

**1. Bordes del sistema.** Dos vistas en `activity_main` (`bordeSuperior`, `bordeInferior`), dentro del lienzo y debajo del velo, con el fondo base macizo en la franja de cada barra del sistema y en degradado hacia el contenido (`ui/cristal/BordesSistema`, que solo hace cuentas, y `BordeSistema`, que pinta). Iguales en los tres niveles del cristal.

| | Macizo | Degradado |
|---|---|---|
| Arriba | la barra de estado entera | 24 dp por debajo (`borde_sistema_desvanecido`) |
| Abajo, con botones | la franja de los botones entera | hasta media cápsula de la barra (26 + 32 dp) |
| Abajo, con gestos | el hueco del asa entero | corto: 26 dp, hasta el borde de la cápsula |

Medido por el revisor de UI: en API 36 «100 kg × 12» pasa de 5,19:1 junto a la cápsula a 1,00:1 en la franja de los botones; bajo el reloj, de 12,4:1 a 1,00:1 en 24 dp, sin corte seco. En API 24 el «2.180◁» no se reproduce: nada pasa por detrás de los iconos, y ◁ ● ■ dan 15,9:1 (oscuro) y 5,76:1 (claro) contra su franja.

- **En API < 29 la barra de navegación lleva fondo propio**, y en las demás también: es el tramo macizo del borde, que va en el contenido. La ventana sigue transparente para que el velo lo cubra. La excepción es Android 7 en claro, donde los botones son siempre blancos y la ventana les pone detrás `gp_velo` (ya lo hacía el lote 1).
- **El contraste forzado del sistema se apaga desde Android 10** (`setNavigationBarContrastEnforced(false)`): era lo que pintaba la franja más clara por encima del velo.

**4. El velo del «+» cubre esa franja con el mismo tono.** Con la hoja abierta en claro: franja `#9E9D9A`, pantalla justo encima `#9E9D9A` (escalón 1,000–1,010:1 en API 36 y API 32; antes 2,0–2,2:1). En Android 7 en claro la franja es más oscura que la pantalla velada (4,07:1), nunca más clara: es la excepción de los botones blancos.

**2. «Hoy toca».** La etiqueta de la última vez sale de la fila de las siluetas: va debajo, a todo el ancho de la tarjeta, con `maxLines="1"`. El subtítulo sigue junto a las siluetas mientras quepa ahí en dos líneas; si necesita más, baja a todo el ancho (`ui/inicio/CabeceraHoyToca`).

| Emulador | Letra | Etiqueta | Subtítulo |
|---|---|---|---|
| API 36 y API 32 (411 dp) | 1,0 | 1 línea (25,5 dp) | 2 líneas, junto a las siluetas |
| API 36 y API 32 | 1,3 | 1 línea (30,5–30,9 dp) | 2 líneas, a todo el ancho |
| API 24 (392 dp) | 1,0 y 1,3 | 1 línea | 2 líneas, a todo el ancho |

Igual en ES y EN. A 360 dp de ancho con letra 1,0 (probado cambiando la densidad del emulador) también queda en dos líneas, debajo; antes eran tres junto a las siluetas.

**3. Margen de la barra flotante.** Token nuevo `barra_margen_boton` (16 dp), que sustituye a `barra_aire_contenido`: lo último de cada pestaña acaba a esa distancia del pie, y el botón de «Hoy toca», con el scroll arriba, también. Si queda más cerca o a medias debajo y acortando hasta 24 dp el bloque del icono de cada ejercicio se arregla, la tarjeta cede justo lo que falta (`ui/inicio/EncajeHeroe`); si no llega ni con eso, no se toca.

| Emulador | Letra 1,0 (ES · EN) | Letra 1,3 (ES · EN) |
|---|---|---|
| API 36 | 155,4 · 135,2 dp | **17,5 · 17,5 dp** (era 1,5) |
| API 32 | 187,4 · 167,2 dp | 21,7 · 21,7 dp |
| API 24 | 56,4 · 36,4 dp | el botón queda detrás de la barra (ver «Bajos abiertos») |

El encaje se vio trabajar: con letra 1,36 en API 36 cede 10 px y deja 16,0 dp; a 360 dp de ancho con letra 1,0, cede 10 dp y deja 16,0. Al final del scroll, lo último queda a 40 dp de la cápsula en los tres.

**El halo del botón principal** sale del estilo (`Widget.GymProFit.BotonPildora.Expresivo.Cristal`), de los colores (`gp_boton_halo`), de `dimens.xml`, de DESIGN.md y del README de la referencia. DEC-070 no lo citaba. Las maquetas `.dc.html` lo conservan en su CSS; el README, que manda, dice que no se pinta.

## Lo que salió por el camino

- **Con gestos, el asa del sistema tachaba el texto** (revisor, API 32, medio): el borde corto no tenía tramo macizo y a la altura del asa el texto seguía a 2,0:1 en claro. El hueco de gestos va ahora macizo; el asa da ≥ 3,85:1 siempre (5,52 en claro, 16,86 en oscuro).
- **En Android 7 la sombra del cristal vivo salía cortada en recto** (revisor, API 24, medio; venía del lote 1): dos orejas grises en las esquinas de la cápsula y de la hoja, con escalones de 19–39 niveles. Antes de Android 8 el recorte «todo menos la forma» parte de la caja de la vista; ahí ya no se recorta, porque el cristal es opaco y tapa lo de debajo. Mayor escalón ahora: 3 niveles en claro. Dentro de la cápsula y de la hoja la diferencia media es de 0,25 niveles.
- **El degradado de los bordes tiraba el emulador del CI.** La primera versión era un `LinearGradient` de siete u ocho paradas con tramado. Con ella, el humo de la release moría al pintarse Inicio: el emulador entero desaparecía un segundo después de entrar, sin un error de la app. En local no se reproduce, ni con Android 14, gestos, inglés, cuenta nueva y la misma release con R8: en el CI el emulador pinta con SwiftShader sobre Linux. Ahora la rampa es una imagen de 1 px de ancho que se estira; con ese cambio el job pasó a la primera. Queda como hipótesis confirmada por un run (ver «Dudas»).
- **El job `app-release-r8` ya fallaba a ratos sin este código** (ramas `privacidad-proveedores` y dependabot, el 25 y el 26-11: «no aparece contenedorAlta»). La instantánea del AVD de la caché no valía para el emulador de los runners (37.3.10) y arrancaba en frío en todos los runs. Se renovó la clave de la caché (`-v4`); el run siguiente la creó y el de después la cargó en 2 s. El guion de humo guarda además el logcat desde el principio y pone tope de tiempo a sus órdenes de adb: antes, con el emulador colgado, se quedaba 16 minutos parado y no subía nada.

## GP-286

`FilaSerieRotulosTest` inflaba `item_serie`, borrado en GP-241. Lo que protegía (GP-133: los rótulos de las columnas se leen enteros con letra grande) existe ahora como una fila de textos en `item_apuntar_ejercicio`: el test mira esa. **Todo `connectedAndroidTest` compila y corre.**

- A letra 1,0 y 1,3, en español y en inglés: pasa en API 36, 32 y 24.
- **A letra 2,0 no pasa, y arreglarlo es tocar producción**: «SERIE» mide 163 px en una columna de 126 y «ANTERIOR» / «PREVIOUS» 281 / 279 en 231 (móvil de 1080 px a 420 ppp): se parten por la mitad de la palabra. No he tocado el layout: ese caso está apartado con `@Ignore` y su porqué, pendiente de decisión.
- `CamposSerie`, la vista que hacía eso en la pantalla vieja, sigue en el código y ya no la usa ningún layout.
- En Android 7 fallan dos tests de `CampoContrasenaTest` que no son de este cambio (el nodo de accesibilidad lleva también la pista: «Secreta1234., Contraseña»). Nadie los había pasado ahí porque la batería no compilaba. En API 36 y 32 pasan.

## Tests

**1.405 tests unitarios**, 16 nuevos, todos vistos en rojo. Fallan los cuatro `*WebTest` de siempre por los restos de «Un día» en `web/`, que no son de este cambio. `lintDebug`: 0 errores.

| Clase | Tests | Cómo se vieron en rojo |
|---|---|---|
| `BordesSistemaTest` | 6: los bordes existen en los tres niveles; con gestos queda corto y el hueco del asa es macizo; la rampa va de transparente al fondo base; sin contraste del sistema la barra lleva fondo propio; el velo los cubre y el cristal los ve; el margen de la barra sale del token | los 6 con las clases en esqueleto; el del asa y el de la rampa, otra vez al cambiar cada uno |
| `EncajeHeroeTest` | 5: con sitio no cede; justo de sitio cede lo que falta; si no llega no se toca; con lo ya cedido da lo mismo; la última vez no se parte | 3 con el esqueleto; los otros 2, devolviendo siempre lo que falta |
| `CabeceraHoyTocaTest` | 4: hasta dos líneas va junto a las siluetas; con tres o más, debajo; sin siluetas no se mueve; el texto de debajo va fuera de la fila | 3 cambiando la regla (`>=`, sin mirar las siluetas, sin la guarda); el de tres líneas, con el tope en 3 |
| `CristalTest` | +1: la sombra no se recorta donde el recorte la corta; y `DESIGN_recoge_liquid_glass` pide ahora los bordes, el token y el encaje | el nuevo, con el recorte por diferencia aún en el código; el de DESIGN.md, antes de escribirlo |

**Instrumentados** (`connectedDebugAndroidTest`, la batería entera): 30 tests.

| | API 36 | API 32 | API 24 |
|---|---|---|---|
| Pasan | 29 | 29 | 27 |
| Apartado (`@Ignore`, GP-286 a letra 2,0) | 1 | 1 | 1 |
| Fallan | 0 | 0 | 2 (`CampoContrasenaTest`, ajenos) |

Nuevo: `BordeSistemaPintadoTest` (3), que pinta el borde en un mapa de bits y mira lo que sale en cada versión; visto en rojo invirtiendo el sentido de la rampa. `FilaSerieRotulosTest`, visto en rojo estrechando la columna.

## Revisor de UI

Inicio y la barra (con la hoja del «+»), las ocho combinaciones (ES/EN × claro/oscuro × letra 1,0/1,3) en cada emulador.

| Pasada | Altos | Medios | Qué |
|---|---|---|---|
| API 36 (nivel 1, botones) | 0 | 0 | 4 bajos; uno era una cifra de DESIGN.md, corregida |
| API 32 (nivel 2, gestos), 1.ª | 0 | 1 | el asa de gestos tachaba texto: arreglado |
| API 32, 2.ª | 0 | 0 | el medio, resuelto y medido |
| API 24 (nivel 3, botones), 1.ª | 0 | 1 | la sombra cortada en recto: arreglada |
| API 24, 2.ª | 0 | 0 | el medio, resuelto y medido |

Zonas pulsables: ninguna por debajo de 48 dp. Contraste: ningún texto por debajo de 4,5:1 (peor par, 5,06:1). Pestaña activa con el trofeo dorado detrás: 5,54–5,60:1 (API 36), 5,00:1 en oscuro y 6,23:1 en claro (API 32).

**Lo que el revisor no vio con el código final.** Sus pasadas son anteriores al cambio del degradado por la imagen estirada (y la de API 36, también al del asa, que con botones no cambia nada). La curva es la misma, ahora exacta. Comprobado a mano después, con el código final, en las capturas de entrega: franja de botones, hueco del asa y barra de estado con variación 0 por fila en API 36, 32 y 24, claro y oscuro, con y sin velo; y `BordeSistemaPintadoTest` en verde en los tres.

## Fotogramas lentos (DEC-021)

Mismo guion que en el lote 1 (`medir.py`: 24 deslizamientos, unos 1.090 fotogramas por pasada en Inicio, oscuro, debug compilada entera, emulador con 3 GB y la GPU del equipo), tres rondas intercaladas de tres variantes: `main`, el lote 1 tal como se entregó y el código final.

| Emulador | `main` | Lote 1 entregado | Final |
|---|---|---|---|
| API 36 (nivel 1) | 0,73 % (8 de 1.093) | 1,10 % (1,10–1,19; 12–13) | **1,28 %** (1,19–1,28; 13–14 de 1.091) |
| API 32 (nivel 2) | 0,74 % (8 de 1.086) | 1,51 % (16 de 1.061, las tres) | **1,51 %** (1,51–2,06; 16, 16 y 22) |
| API 24 (nivel 3) | 0,0 % (0 de 1.285) | 0,0 % | **0,0 %** (0 de 1.299) |

Percentiles, iguales en las tres variantes: API 36, p50 17–18 · p90 18–19 · p95 19 · p99 19–20 ms; API 32, p50 17 · p90 18 · p95 18–19 · p99 19 ms (21 en la ronda de 2,06 %); API 24, p50 5 · p90 5 · p95 5–6 · p99 5–7 ms.

**Lectura, y una corrección al informe de arriba.** El remate no añade fotogramas lentos que se puedan medir: uno o dos de cada 1.090 en API 36, ninguno en API 32 ni en API 24. Pero **el lote 1 costaba más de lo que dije**: lo medí antes de los arreglos del revisor y di 0,82 % en API 36 y 1,2 % en API 32; tal como se entregó son 1,10 % y 1,51 %. Frente a `main`, el rediseño entero cuesta 5–6 fotogramas lentos de cada 1.090 en API 36 y 8 en API 32, todos del tipo «órdenes de dibujo lentas», con los percentiles sin moverse. Ningún dispositivo baja de nivel.

Una tanda anterior en API 36, con el degradado de la GPU, dio 1,28 % (1,19–1,47): la imagen estirada no cambia la cifra.

## Producción

Release (R8, firma de release, 6.608.164 bytes, compilada con `843cfd2b`) en el emulador de API 36, contra `https://api.gymprofit.app` (commit en producción: `20d234d6`; el lote no toca la API).

- Cuenta de prueba `prueba-gp285-c@gymprofit.app`, creada por la API (201).
- Entrar → cuestionario del alta → avisos → Inicio de una cuenta nueva: bordes del sistema, «Hoy toca» con la etiqueta en una línea («Todavía no la has hecho»), barra flotante y scroll bajo ella.
- Hoja del «+» → «Empezar entrenamiento» → sesión en vivo → atrás → barra de la sesión → la hoja otra vez → las cuatro pestañas.
- Sin errores de la app en logcat.
- **Cuenta borrada** (`DELETE /usuarios/me`, 200) y comprobado: el login devuelve **401**. La clave se eliminó entonces y la release se desinstaló.

Antes, con el código del primer commit, se había hecho la misma pasada con `prueba-gp285-b`, también borrada (401).

## Capturas

Escritorio, carpeta `GP-285 lote 1 remate`, subcarpetas `API 36`, `API 32` y `API 24`: 24 por emulador (oscuro y claro × ES y EN × letra 1,0 y 1,3: Inicio arriba, el scroll con los bordes del sistema y la hoja del «+» con el velo). Hechas con el código final.

## Decisiones tomadas

1. Los bordes van en el contenido y no en el color de la ventana, también en API < 29: así el velo los cubre. Por eso se apaga el contraste forzado del sistema.
2. Con gestos el degradado queda corto (26 dp), pero el hueco del asa es macizo: sin eso el asa tachaba el texto.
3. El token del margen es uno solo (`barra_margen_boton`) para el final del scroll y para el botón de «Hoy toca» en reposo; `barra_aire_contenido` desaparece.
4. La tarjeta cede como mucho 24 dp de sus iconos. Si no basta, no se toca: forzar más quitaría contenido.
5. El subtítulo sigue la misma regla que la etiqueta (baja a todo el ancho si junto a las siluetas pasa de dos líneas), aunque el encargo solo pedía dos líneas a letra 1,0: a 360 dp de ancho no se cumplía de otro modo.
6. Los bordes se pintan con una imagen estirada, no con un degradado de la GPU.
7. Dos commits de `ci` en la rama, fuera de lo pedido: sin ellos el CI no podía ponerse en verde ni decir por qué fallaba.
8. GP-286: el test se reorienta a la pantalla que existe; el caso de letra 2,0 se aparta en vez de tocar producción.

## Bajos abiertos

- **En Android 7 con letra 1,3 «Empezar» queda detrás de la barra** (392 × 851 dp: 71 dp por debajo de su borde superior; harían falta 87 y se ceden 24). Cumple la regla escrita; la cápsula es opaca y por debajo asoman 6–7 dp de naranja. Se llega con un desplazamiento, con la tarjeta entera o con el «+».
- La tarjeta «Hoy toca» queda tangente a la barra con letra 1,3 (API 36 y 32); «Tu día» asoma cortado bajo la cápsula en reposo.
- Con gestos, el degradado corto corta el texto por la mitad más que desvanecerlo (baja de 7:1 a 1,5:1 en 30 px).
- La sombra del cristal vivo mide unos 13 dp de rampa, no los 26 dp de difuminado que dice DESIGN.md (igual en todas las versiones; se aprobó así en capturas).
- El cristal opaco al 98 % deja entrever lo de detrás (hasta 5 niveles).
- «HOY TOCA · DÍA 1 DE 4» parte y deja el «4» solo con letra 1,3 en español en Android 7; «Último récord · hace 6 días» parte mal.
- Con el velo en claro, el reloj del sistema baja a 3,89:1 (transitorio, texto del sistema).
- El gris de las siluetas en claro (1,47:1 contra la tarjeta; componente anterior).
- Sin comprobar: TalkBack encendido de verdad, la sombra de la barra de la sesión en Android 7 y el movimiento de la hoja.

## Tareas del propietario

1. **GP-286, letra 2,0**: decidir si se arregla el layout de «Apuntar entreno» («Serie» y «Anterior» no caben en su columna) y, al hacerlo, quitar el `@Ignore`. Y si se borra `CamposSerie`, que ya no se usa.
2. `CampoContrasenaTest` falla en Android 7 en dos tests: mirar si es del test o de la app.
3. Lo pendiente de antes sigue igual (Render y UptimeRobot a liveness, el posible intento de login de `dani.entrena` en producción).

## Dudas

- **La causa de la caída del emulador del CI está confirmada por un solo run** (falló tres veces en Inicio con el degradado, pasó a la primera con la imagen). Si `app-release-r8` vuelve a morir al pintar Inicio, el logcat ya queda guardado en el artefacto `humo-release`.
- **«Empezar» detrás de la barra en móviles bajos con letra grande**: si se quiere el botón principal siempre visible en la primera pantalla, hay que cambiar la regla (por ejemplo, quitar una línea a los nombres del carrusel al ceder).
