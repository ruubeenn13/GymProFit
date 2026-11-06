# Lote 7 · pulido (GP-225, GP-229 y GP-240)

06-11-2026. Solo app; la API no cambia (producción sigue en `3200c0b`). Rama `gp-225-pulido`.

## Encargo

Cerrar los bajos de los lotes 3 a 5 de GP-216 y de GP-224 (GP-225), que la app diga bien las cosas con pocos datos (GP-229) y que no mezcle idiomas (GP-240). Fuera: los cortes de «Tus últimos entrenos» (GP-232) y los huecos n.º 14 de DESIGN.md.

## Commits

| Hash | Qué |
|---|---|
| `34273f3` | GP-240: idioma sin elegir, filtro de idiomas de la APK, `localeDeLaApp` en en-US/es-ES |
| `ae619dc` | GP-225 y GP-229: los bajos del encargo y las gráficas y Récords con pocos datos |
| `3b6020f`, `517cc2c`, `3ce385f` | Pasadas 1 a 3 del revisor en Nutrición, Añadir, la ficha, Comida y Estadísticas |
| `0db12bf`, `788b184` | Pasadas 1 y 2 en Entrenar, Tus datos, Programas, Registrar sesión y la ficha del ejercicio |
| `a88b066`, `5eb4c7f`, `0ff3764` | Pasadas 1 a 3 en Progreso, las listas y Medidas |
| `5283e96` | La release se cerraba al abrir Inicio (R8 quitaba `BurbujaProfi`), visto en producción |

## GP-240

- `app/build.gradle`: `androidResources { localeFilters += ["es", "en"] }` (AGP 9.4.1). La APK pasa de llevar las traducciones de AppCompat y Material a `'--_--' 'en' 'es'` (`aapt2 dump badging`).
- `utils/IdiomaApp`: sin idioma elegido, el primero de la lista del sistema que sea español o inglés; si no hay ninguno, español. Se aplica a cada pantalla (`ScaleUtils.wrap`), al contexto de las notificaciones (`IdiomaContexto`) y como idioma por defecto del proceso (Accept-Language y el calendario de Material), también tras cada cambio de configuración y al crear cada pantalla: con [francés] Android lo devolvía al del sistema después de `Application.onCreate` y el calendario salía en francés.
- Un idioma elegido fuera de la app (el idioma por app de Android 13 o `cmd locale`) cuenta como elegido: si no, la pantalla salía en inglés y la API en español (lo destapó el revisor).
- `FechaUtils.localeDeLaApp` = en-US o es-ES según `recursos_en_ingles`. Corregido el comentario de `values/idioma.xml`.

Comprobado sin idioma elegido, Inicio, Medidas y el calendario de Registrar sesión (meses y días):

| Sistema | API 36 | API 24 |
|---|---|---|
| [catalán, español] | español entero | español entero |
| [francés] | español entero (tras el arreglo por pantalla; antes, calendario en francés) | español entero |
| [francés, inglés] | inglés entero | inglés entero |
| [alemán, inglés] | inglés entero | inglés entero |

En API 36 la lista se pone con `settings put system system_locales` y un reinicio; en API 24, con un programa propio por `app_process` (root) que llama a `updatePersistentConfiguration`, como Ajustes (scratchpad, no versionado). Con idioma elegido no cambia nada (no se tocó ese camino).

## GP-229

- `PuntosGrafica`: 0 puntos (días) → el vacío; 1 → el punto con su valor encima y su fecha en el eje, sin tooltip ni cuadrícula, y la pista; 2 o más → la línea (recta entre puntos). En «Tu cuerpo», Medidas y la ficha del ejercicio.
- Medidas: «Aún no has apuntado esta medida.» o «Sin medidas en estas fechas. Prueba con «Todo».»; ya no «Aún no hay datos» con datos.
- Récords (`VacioRecords`): sin entrenos, «Tus récords salen cuando superas una marca tuya. El primer entreno pone tu punto de partida.»; con entrenos y sin récords, «Ya tienes tu punto de partida. El primer récord llega cuando superes una de tus marcas.»; si la lista de sesiones falla, el texto que vale en los dos casos.

## GP-225: cada bajo del encargo

Todos arreglados:

- Hoja de seguir programa sin el lavanda (todas las `BottomSheetDialog`, `ThemeOverlay.GymProFit.Hoja`).
- «DÍAS REGISTRADOS» a 1,3: las dos tarjetas de cada fila a la misma altura (`AltoIgual`). Donut: los gramos en la leyenda.
- Separadores a 70 dp; insignia con `gp_revisado`; buscador con `buscador_alto`/`buscador_radio` (56 dp, también la Biblioteca); «Lo que te encaja» con Profi y 17 sp; «+» con rol de botón; bocadillo con cola y borde (`BurbujaProfi`); iconos teñidos por categoría; corazón a 18 dp; cifras con «gramos»; sin capa de foco en ningún componente de Material ni en el día de la tira.
- «Principiante»; «Mucha» ya no mueve el selector (`AltoReservado`); los 4 dp laterales de la pista responden; cabecera a 56 dp (`cabecera_alto`; el 64 era el `actionBarSize` de Material 3, sin motivo medido).
- Apoyo del récord igual (`FilaRecord`); chip «—» con diferencia 0; chevrones en `gp_stroke`; línea azul en Medidas; plancha igual; foco con teclado al pasar a una pestaña desde un enlace (con toque o con la barra ya iba al contenido: medido); listas a 20 dp; el tooltip no tapa su punto; `spRutina` con nombre «Rutina: …» (con TalkBack encendido el nodo es `Spinner` con esa descripción; la voz no se pudo grabar).
- Inglés de EE. UU. («Protein», «program», «liters», «licenses», «centimeters», «Fat»…); «3,061 kilocalories left.»; Proteínas en frases.

## Revisor de UI

| Grupo | Pasada | Altos | Medios | Bajos |
|---|---|---|---|---|
| Nutrición | 1 | 1 | 4 | 9 |
| Nutrición | 2 | 1 (regresión propia: días a 47 dp) | 2 | 1 |
| Nutrición | 3 | 0 | 3 (+1 visto una vez) | 1 → medios arreglados y medidos a mano |
| Entrenar | 1 | 3 | 6 | 4 |
| Entrenar | 2 | 2 | 4 | 6 |
| Entrenar | 3 | 0 | 0 | 5 |
| Progreso | 1 | 0 | 3 | 6 |
| Progreso | 2 | 1 | 1 | 3 |
| Progreso | 3 | 0 | 3 | 1 → medios arreglados y medidos a mano |

Medidas a mano de los últimos medios:

- **Nutrición:**
  - el anillo del mensaje de Profi y de «Ahora no» queda a 4,0 dp del botón (ES 1,0 y EN 1,3);
  - la unidad de la ficha, a 12 dp de «CANTIDAD»;
  - las ideas en EN 1,3 ocupan dos líneas, con el detalle entero.
- **Progreso:**
  - Cintura con 30 días a 1,3 ya no tiene cuadrícula y su cifra va en la condensada (captura `p3_cintura13_crop.png`);
  - las dos descripciones dicen sus cifras: «Peso máximo, la mejor marca de cada día: de 82,5 kg el 18 sept a 85 kg el 28 sept».

## Producción

Release firmada contra `api.gymprofit.app` y la cuenta nueva `prueba-l7-b2fb0f`, creada por la API.

- **Con 0:** «Tu cuerpo» con su vacío; Récords con el texto sin entrenos; la ficha de Sentadilla con barra dice «Todavía no has hecho este ejercicio…».
- **Con 1 peso y 1 entreno de ayer:**
  - «Tu cuerpo» y Medidas, el punto de 80,4 kg con «Apunta otro peso para ver cómo cambias»;
  - la ficha, el punto de 65 kg con «La próxima vez verás tu progreso»;
  - Récords, «Ya tienes tu punto de partida…».
- **Con 2 pesos y 2 entrenos:**
  - los dos pesos caen el mismo día, porque la API pone la fecha del servidor y no se puede fechar un pesaje. Siguen siendo un punto (el último, 79,9 kg), como dice la regla. La línea de dos días de peso se comprobó en local con `prueba` (78,5 → 79,2 kg);
  - la ficha dibuja la línea de 65 a 70 kg y sale el primer récord.
- **Fallo encontrado y arreglado:** la release se cerraba al abrir Inicio, porque R8 quitaba `BurbujaProfi` (`5283e96`). La debug no reduce el código, por eso no lo vieron ni los tests ni el revisor.
- **Borrado:** la cuenta se borró por la API (`DELETE /usuarios/me`, 200). Después, el login y el token dan 401. Los ficheros de la clave y del token se borraron después de comprobarlo.

## Tests

- 1141 unitarios en verde (1074 antes del lote; 67 nuevos). `lintDebug` sin errores (638 avisos).
- **Vistos en rojo antes del arreglo, sobre una versión que reproducía el comportamiento viejo:** `IdiomaAppTest` (7 de 9), `TextosInglesTest` (4 de 4), `PuntosGraficaTest`, `VacioRecordsTest`, `ChartMarkerTest` (dos veces: el punto y el área de datos), `TintesCategoriaTest`, `SelectorPildoraToqueTest`, `FilaRecordTest`, `DiferenciaVezTest`, `DiaConEstaComidaTest`, `PlanPrevioTest` (cifras guardadas) y `CifraEjeTest`.
- **Vistos en rojo contra el código de `HEAD`, sin Gradle:** `PulidoTest` (21 reglas de recursos y código, cada pasada contra el commit anterior), `EstadisticasYFormulariosTest.elDonutDiceLosGramosEnLaLeyenda` y `DrawablesConClaseTest`. El script compila el test solo y lo corre sobre `git archive HEAD`.
- Se ajustó `TemaUnaVezTest`: `Drawable.applyTheme` no es fijar el tema.

## Bajos que se quedan, y por qué

Ninguno es del encargo; los dio el revisor en pantallas tocadas. La regla de CLAUDE.md pide cerrar los medios.

- **Fuera de este lote, para otro de pulido:**
  - ideas de Comida con icono de 44 dp y su anillo pegado al icono;
  - el subtítulo y el orden de la frase de «Lo que te encaja» frente al tablero 25;
  - la adherencia por color en pantalla y «Off» en EN;
  - la casilla del icono de la ficha sin tinte;
  - la gráfica de calorías sin el día del dato;
  - separación desigual entre chips marcados y sin marcar;
  - poco aire sobre el título de la hoja de seguir programa;
  - «Main lifts at 2» / «sets» partido en EN 1,3;
  - «Full body» / «B» partido en el bocadillo a 1,3;
  - «Proteína. Proteína: …» repetido en «Tu día» de Inicio;
  - eje Y en pasos de 0,8;
  - sesiones del mismo día sin hora;
  - menú del desplegable más estrecho que el campo;
  - «Siguiente» y «Guardar sesión» en Barlow Condensed;
  - «Duración» sin unidad en Crear rutina;
  - «Reintentar» de Récords con el contorno de Material;
  - filas de Medidas a 56 dp;
  - anillo de Medidas sobre el borde de la tarjeta;
  - cifras de Medidas en naranja;
  - «Kilos moved»;
  - papeleras iguales con varias sesiones del mismo día y rutina;
  - el trofeo con círculo en Progreso y sin él en la lista;
  - el eje Y distinto según la métrica con un punto.
- **Chips de las pantallas de administración** con el estilo de Material: no son pantallas de este lote.

## Decisiones

- **Colores por categoría:**
  - carne, pescado, huevos, lácteos y suplementos, azul de proteína;
  - legumbres, cereales, frutas y snacks, ámbar;
  - frutos secos y aceites, violeta;
  - verduras, verde;
  - bebidas, azul del agua.
  
  Todos pasan 3:1 sobre el círculo.
- **Línea azul en Medidas en las tres métricas,** no solo en peso.
- **El dorado de Progreso sigue a la lista:** solo el récord más reciente. El tablero 16 dibuja todos dorados, pero el mismo récord no puede ir celebrado en una vista y apagado en la otra.
- **Hoja de seguir programa con un alto mínimo del 85 %:** a toda la altura se pegaba arriba.
- **Capa de foco:** se quita en todo Material sobrescribiendo sus opacidades, en lugar de estilo a estilo.
- **«Atrás» pasa a «Volver»** en las seis pantallas que lo decían.
- **Recursos retirados:** se quitaron `gp_texto_sobre_macro` y `progreso_records_vacio`. Queda sin uso `ic_ms_star_shine_fill`: no se borró, por preguntar antes de borrar.

## Entorno

- **Emulador API 36:**
  - se colgó dos veces con el medidor (cae `system_server` y deja de responder `uiautomator`);
  - el arranque desde la instantánea no lo arreglaba; un arranque en frío (`-no-snapshot-load`) sí.
- **Emulador API 24:**
  - tuvo que arrancar en frío;
  - tenía una release con otra firma y se desinstaló.
- **Medidor de accesibilidad:** con él encendido, el idioma por defecto del proceso vuelve al del sistema, y ahora eso llega también a la API (GP-219b). El revisor midió con él solo sobre pantallas ya abiertas.
- **API local:** parada al acabar. El emulador queda con la release, sin datos.

## Dudas

- **`ic_ms_star_shine_fill`:** se queda sin uso. ¿Se borra?
- **Dorado de los récords recientes:** el tablero 16 lo pinta en todos; aquí se dejó solo el más reciente. ¿Vale así?
