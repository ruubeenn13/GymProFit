# Auditoría de diseño de la app Android — septiembre 2026

Auditoría de las pantallas de la app Android hecha el **21 de septiembre de 2026**
en el emulador `Medium_Phone_API_36.1` contra la API local, con el entorno aislado
de [ENTORNO-PRUEBAS.md](ENTORNO-PRUEBAS.md) (distintivo `PRUEBAS · LOCAL` visible
en todas las capturas).

**Este documento es un informe, no un plan.** No se ha tocado ni un layout. De
aquí salen tareas, pero las abre quien lleva el backlog, no esta auditoría.

A diferencia de [AUDITORIA-UX-2026-09.md](AUDITORIA-UX-2026-09.md), que juzgaba
estructura y flujo, aquí solo se recoge **lo que se puede medir**: un contador, una
línea de código, un ratio de contraste, un tamaño en dp. Si algo se ve bonito o feo
lo juzga quien mire las capturas.

Capturas en [`auditoria-diseno/`](auditoria-diseno/) — 60 archivos, nombre
`pantalla-idioma-tema.png`.

## Índice

1. [Alcance: qué se ha capturado y qué se ha recortado](#1-alcance-qué-se-ha-capturado-y-qué-se-ha-recortado)
2. [Cómo se han medido las cosas](#2-cómo-se-han-medido-las-cosas)
3. [Hallazgos por pantalla](#3-hallazgos-por-pantalla)
4. [Pantallas bloqueadas por GP-010, GP-012 y GP-016](#4-pantallas-bloqueadas-por-gp-010-gp-012-y-gp-016)
5. [Patrones transversales](#5-patrones-transversales)
6. [Resumen por severidad](#6-resumen-por-severidad)

---

## 1. Alcance: qué se ha capturado y qué se ha recortado

La app tiene **37 Activities y 6 Fragments**. El recorrido principal está en las
cuatro combinaciones de idioma y tema; el resto, en ES + oscuro.

**Datos usados.** Tres cuentas locales:

- `prueba` (id 1812), **sembrada** para esta auditoría: 5 sesiones, 3 comidas con
  5 alimentos, 4 mediciones corporales, 2 rutinas propias (una con 4 ejercicios) y
  1 logro desbloqueado.
- `vacia`, creada para esto y **sin ningún dato**, para los estados vacíos.
- `admin`, para el panel de administración.

**Capturado en las cuatro combinaciones** (ES/EN × claro/oscuro): Home, Rutinas,
Ejercicios, Nutrición y Perfil. 20 capturas.

**Capturado en ES + oscuro**: Splash, Login, Registro, Recuperar contraseña,
Onboarding 1 a 5 y resumen, Editar perfil, Sesiones, Resumen de sesión,
Mediciones, Logros, Acerca de, Eliminar cuenta, Detalle de rutina, Crear rutina,
Crear rutina con error de validación, Detalle de ejercicio, Comida, Añadir
alimento, Crear alimento, Estadísticas de nutrición, Registrar sesión, Panel de
administración y sus cuatro secciones.

**Capturado con la cuenta vacía**: las cinco pestañas, Sesiones, Mediciones y
Logros.

**Recortado, y por qué** — se dice en vez de omitirlo en silencio:

| Pantalla | Motivo |
|---|---|
| `AnadirEjerciciosActivity` | Solo se llega desde el asistente de crear rutina, y el asistente se atascó en la validación del paso 1 (ver P-38). Queda sin captura. |
| `ResumenCrearRutinaActivity` | Igual: está detrás de `AnadirEjercicios`. |
| `EditarRutinaActivity` | Requiere una rutina propia con ejercicios y varios pasos de formulario; se priorizó cubrir pantallas nunca vistas. |
| `EditarEjercicioAdminActivity`, `EditarRutinaAdminActivity` | Formularios de administración detrás de un menú contextual por fila. El inventario estático (T-02 a T-08) sí los cubre. |
| Eliminar cuenta en ES+claro y EN+oscuro | Se verificaron el 2026-09-21 al comprobar GP-008 y salieron correctas. Aquí quedan las dos combinaciones que faltaban. |

**Lo que no se ha podido capturar de verdad**: `SplashActivity`. Lo que se ve al
arrancar es la pantalla del sistema (`windowSplashScreen`), no el layout de la
activity. Eso es, en sí mismo, el hallazgo P-01.

---

## 2. Cómo se han medido las cosas

- **Emojis**: barrido de todos los `.xml` y `.java` de `app/src/main` con los
  rangos Unicode de emoji y símbolos.
- **Tipografía**: recuento de `android:textSize` en `res/layout/` frente a los
  estilos `Font.GymProFit.*` declarados en `values/themes.xml`.
- **Color**: búsqueda de literales `#rrggbb` en `res/layout/`, contra la regla
  escrita en la cabecera de `values/colors.xml`.
- **Espaciado**: extracción de los 1 005 valores de `layout_margin*` y `padding*`
  de todos los layouts y análisis de su distribución.
- **Contraste**: ratio WCAG 2.1 calculado sobre los tokens reales de
  `values/colors.xml` y `values-night/colors.xml`, y sobre el color efectivo
  cuando hay `android:alpha` (mezcla del color con el fondo).
- **Zonas pulsables**: recorrido del árbol XML de cada layout buscando elementos
  `clickable` o de clase botón con `layout_width`/`layout_height` por debajo de
  48 dp.
- **Estados**: inventario de `tvVacio`/`tvEmpty`, `ProgressBar` y `LoadingDialog`
  por pantalla.

---

## 3. Hallazgos por pantalla

Severidad: **Alta** rompe una decisión de producto vigente, incumple WCAG AA o
deja contenido inalcanzable · **Media** rompe el sistema visual o el idioma ·
**Baja** remate.

### 3.1 Splash — `SplashActivity`

Primera pantalla del arranque. Decide si va a Login o a Main.

Capturas: `splash-es-oscuro.png`

| # | Hallazgo | Evidencia | Severidad |
|---|---|---|---|
| P-01 | **El layout del splash no llega a verse.** Lo que aparece es la pantalla del sistema, con fondo claro, también cuando la app está en tema oscuro: al arrancar hay un destello claro antes del negro. | `splash-es-oscuro.png` (fondo claro con el tema en oscuro) frente a `activity_splash.xml:10` (`#141414`) | Media |
| P-02 | Los tres colores del layout del splash **no existen en la paleta**: `#141414` de fondo y `#C9963A` en texto e indicador. El dorado de la paleta es `#B58A2A` en claro y `#E8B84B` en oscuro. | `activity_splash.xml:10,32,43` | Media |

### 3.2 Login, Registro y Recuperar contraseña

Entrada a la app: acceso con invitado, alta con enlace a la política y
recuperación por código.

Capturas: `login-es-oscuro.png`, `registro-es-oscuro.png`,
`recuperar-password-es-oscuro.png`

| # | Hallazgo | Evidencia | Severidad |
|---|---|---|---|
| P-03 | El enlace «¿No tienes cuenta? Regístrate» tiene una zona pulsable de **59 px de alto** (`[297,2012][782,2071]`), unos 20 dp: menos de la mitad del mínimo. Es la única vía de alta de la app. | Volcado de accesibilidad de `LoginActivity`, nodo `tvNoTienesCuenta` | Alta |
| P-04 | Las tres pantallas usan `android:textSize` en lugar de los estilos `Font.GymProFit.*`: 7 casos en login, 5 en registro, 6 en recuperar. Ver T-02. | `activity_login.xml`, `activity_registro.xml`, `activity_recuperar_password.xml` | Media |

### 3.3 Onboarding (pasos 1 a 5 y resumen)

Cinco pasos que recogen datos, objetivo y nivel, y un resumen con el cálculo
nutricional.

Capturas: `onboarding1-es-oscuro.png` … `onboarding5-es-oscuro.png`,
`onboarding-resumen-es-oscuro.png`

| # | Hallazgo | Evidencia | Severidad |
|---|---|---|---|
| P-05 | **Los cuatro niveles de experiencia son emojis**, no iconos del sistema: 🌱 💪 🏆 ⚡. Es exactamente lo que se retiró de la pantalla de objetivos en `59ea6f4` («un emoji lo dibuja la fuente del sistema, cambia de un móvil a otro y no se puede teñir»). El paso anterior del mismo asistente ya usa `ic_objetivo_*`: **dos pantallas contiguas con dos sistemas de iconos distintos**. | `activity_onboarding5.xml:69,98,127,156` · `onboarding5-es-oscuro.png` | Alta |
| P-06 | **La gota del agua recomendada es un emoji** 💧 en el resumen. Mismo caso. | `activity_onboarding_resumen.xml:249` · `onboarding-resumen-es-oscuro.png` | Alta |
| P-07 | El asistente concentra el mayor número de desviaciones de la escala de espaciado: 23 en el resumen, 21 en el paso 5, 13 en el paso 1 y 13 en el paso 4. Ver T-04. | Recuento sobre `res/layout/` | Media |

### 3.4 Home

Saludo, mapa muscular de la semana y acciones rápidas.

Capturas: `home-es-oscuro.png`, `home-es-claro.png`, `home-en-oscuro.png`,
`home-en-claro.png`, `home-vacio-es-oscuro.png`

| # | Hallazgo | Evidencia | Severidad |
|---|---|---|---|
| P-08 | **La barra de navegación flotante tapa la última acción rápida.** «Registrar comida» ocupa `[237,2291][938,2337]` y la barra `[37,2111][1043,2300]`: el texto queda debajo. Pasa en las cinco pestañas (T-01), pero aquí deja **inalcanzable una acción primaria**. | `home-es-claro.png`, `home-vacio-es-oscuro.png` · volcado de `MainActivity` | Alta |
| P-09 | En tema claro la silueta del mapa muscular (`body_silhouette` `#D2D2D6`) sobre la tarjeta blanca da **1,51:1**. El dibujo que ocupa media pantalla casi no se ve. El mismo par en oscuro da 6,97:1. | `home-es-claro.png` frente a `home-es-oscuro.png` · `values/colors.xml`, `body_silhouette` | Media |
| P-10 | La tarjeta de racha usa `gp_gold` como color de texto: en tema claro da **3,17:1 sobre tarjeta y 2,80:1 sobre fondo**, por debajo de AA para texto normal. | `activity_home.xml:202,223` · cálculo WCAG | Alta |
| P-11 | Home no tiene estado de carga ni de error: solo caso feliz y un vacío bien resuelto (el rótulo pasa a «Tu cuerpo está por encender»). Si la API no responde, nada lo dice. | `activity_home.xml` sin `ProgressBar` ni contenedor de error · `HomeFragment.java` | Media |
| P-12 | 27 desviaciones de la escala de espaciado, el máximo de la app. Ver T-04. | Recuento sobre `activity_home.xml` | Media |

### 3.5 Rutinas

Lista de rutinas con filtro por nivel y acceso a crear una nueva.

Capturas: `rutinas-es-oscuro.png`, `rutinas-es-claro.png`,
`rutinas-en-oscuro.png`, `rutinas-en-claro.png`, `rutinas-vacio-es-oscuro.png`

| # | Hallazgo | Evidencia | Severidad |
|---|---|---|---|
| P-13 | **Cada tarjeta de rutina muestra calorías estimadas de entrenamiento**, que es lo que DEC-004 prohíbe. El valor lo calcula la API con `SUM(series × repeticiones × calorias_quemadas)`, literalmente la fórmula que la decisión declara sin fundamento fisiológico, y se pinta con una tilde de aproximación delante. Los propios números lo delatan: «Movilidad Activa · 35 min · **~2385 kcal**», más que la ingesta diaria completa del usuario. **DEC-004 solo reconoce la contradicción en `RegistrarSesionActivity`, y esta pantalla no entra en el alcance de GP-010** (Home y resumen de sesión): es un punto de exposición sin inventariar. | `RutinaAdapter.java:88` · `api/gymprofit-api/src/main/java/com/gymprofit/api/entity/Rutina.java:90-91` · `rutinas-vacio-es-oscuro.png` | Alta |
| P-14 | **La cuenta sin rutinas propias no ve un estado vacío: ve seis rutinas ajenas bajo el rótulo «Mis rutinas».** Son las predefinidas del catálogo. El estado vacío (`tvEmpty`) existe en el layout pero **nunca se alcanza**, porque la lista nunca llega vacía. | `rutinas-vacio-es-oscuro.png` (cuenta `vacia`, 0 rutinas propias en base de datos) · `activity_rutinas.xml` | Alta |
| P-15 | La última tarjeta de la lista queda medio tapada por la barra flotante: `paddingBottom="12dp"` frente a los 86 dp que ocupa la barra. Ver T-01. | `rutinas-vacio-es-oscuro.png` · `activity_rutinas.xml` | Alta |

### 3.6 Ejercicios

Catálogo de 873 ejercicios con buscador y filtro por grupo muscular, más la ficha
de cada uno.

Capturas: `ejercicios-es-oscuro.png`, `ejercicios-es-claro.png`,
`ejercicios-en-oscuro.png`, `ejercicios-en-claro.png`,
`ejercicios-vacio-es-oscuro.png`, `detalle-ejercicio-es-oscuro.png`

| # | Hallazgo | Evidencia | Severidad |
|---|---|---|---|
| P-16 | **752 de los 873 ejercicios activos (86 %) tienen el nombre en español idéntico al inglés**: no están traducidos. Con la app en español se lee «3/4 Sit-Up», «All Fours Quad Stretch», «Barbell Hack Squat». También 479 descripciones. Ver T-10. | `SELECT COUNT(*) … WHERE nombre = nombre_en` → 752 · `detalle-ejercicio-es-oscuro.png` | Alta |
| P-17 | Capitalización inconsistente dentro del propio catálogo: «AIR BIKE» en mayúsculas sostenidas junto a «Air Bike» y «Rodillo Ab». | Consulta a la tabla `ejercicios` | Baja |
| P-18 | **El marco de la imagen de demostración es blanco puro a fuego.** En tema oscuro es el único elemento `#FFFFFF` de la pantalla y se lee como un error de render. | `activity_detalle_ejercicio.xml:76,83` · `detalle-ejercicio-es-oscuro.png` | Media |
| P-19 | La ficha del ejercicio muestra **«Calorías · 5 kcal»**: otra estimación de gasto de entrenamiento, tampoco cubierta por GP-010. | `detalle-ejercicio-es-oscuro.png` · `activity_detalle_ejercicio.xml` | Alta |
| P-20 | La tarjeta del título ocupa una tarjeta completa para una sola línea de texto, mientras el resto de la pantalla agrupa cuatro datos por tarjeta. | `detalle-ejercicio-es-oscuro.png` | Baja |

### 3.7 Nutrición

Calorías y macros del día, acceso a estadísticas y las comidas de la jornada.

Capturas: `nutricion-es-oscuro.png`, `nutricion-es-claro.png`,
`nutricion-en-oscuro.png`, `nutricion-en-claro.png`,
`nutricion-vacio-es-oscuro.png`, `comida-es-oscuro.png`,
`anadir-alimento-es-oscuro.png`, `crear-alimento-es-oscuro.png`,
`estadisticas-nutricion-es-oscuro.png`

| # | Hallazgo | Evidencia | Severidad |
|---|---|---|---|
| P-21 | El estado vacío está **bien resuelto**: cada comida dice «Sin registrar» con su botón de añadir. Es el listón al que no llegan Sesiones ni Logros. Se anota como referencia, no como defecto. | `nutricion-vacio-es-oscuro.png` | — |
| P-22 | Las dos últimas comidas del día («Comida» y «Merienda») quedan tapadas por la barra flotante. `paddingBottom="6dp"`, el valor más bajo de las cinco pestañas. Ver T-01. | `nutricion-vacio-es-oscuro.png` · `activity_nutricion.xml` | Alta |
| P-23 | En tema claro, el macro de carbohidratos (`#C27C12`) sobre tarjeta da **3,39:1**: por debajo de AA para texto normal. Proteínas (4,88:1) y grasas (5,39:1) sí pasan, así que de los tres colores que codifican el mismo tipo de dato uno se lee peor que los otros dos. | Cálculo WCAG sobre `values/colors.xml` | Media |
| P-24 | Los botones de día anterior y siguiente miden 40 dp. Ver T-03. | `activity_nutricion.xml`, `btnDiaPrev`, `btnDiaNext` | Media |
| P-25 | El título «Seguimiento nutricional» llega hasta los 785 px de ancho y el menú de tres puntos empieza en 843: quedan **58 px de aire** entre el texto y el icono. | `nutricion-vacio-es-oscuro.png` | Baja |

### 3.8 Perfil

Datos del usuario, accesos a sus actividades, Acerca de y borrado de cuenta.

Capturas: `perfil-es-oscuro.png`, `perfil-es-claro.png`, `perfil-en-oscuro.png`,
`perfil-en-claro.png`, `perfil-vacio-es-oscuro.png`,
`editar-perfil-es-oscuro.png`, `acerca-de-es-oscuro.png`

| # | Hallazgo | Evidencia | Severidad |
|---|---|---|---|
| P-26 | «Mis logros» queda parcialmente tapado por la barra flotante. Ver T-01. | `perfil-es-oscuro.png` | Alta |
| P-27 | 23 desviaciones de la escala de espaciado y 23 usos de `android:textSize`: la segunda peor de la app en las dos medidas. Ver T-02 y T-04. | Recuento sobre `activity_perfil.xml` | Media |
| P-28 | La cabecera del perfil usa `btnMenuOpciones`, un `ImageView` de 40 dp, mientras las pantallas hijas a las que lleva usan `Toolbar` de Material. Ver T-05. | `activity_perfil.xml` | Media |

### 3.9 Sesiones y Mediciones

Historial de sesiones y de mediciones corporales, ambas colgando del perfil.

Capturas: `sesiones-es-oscuro.png`, `sesiones-vacio-es-oscuro.png`,
`mediciones-es-oscuro.png`, `mediciones-vacio-es-oscuro.png`

| # | Hallazgo | Evidencia | Severidad |
|---|---|---|---|
| P-29 | **El estado vacío de Sesiones es una línea de texto gris centrada en una pantalla negra**, sin icono ni acción, con el botón flotante de añadir a un lado y sin relación visual con el mensaje. Mediciones, en la misma app y a un toque de distancia, sí lleva icono de 64 dp más texto. Dos calidades de vacío en pantallas hermanas. | `sesiones-vacio-es-oscuro.png` frente a `mediciones-vacio-es-oscuro.png` · `activity_sesiones.xml` (solo `tvVacio`) frente a `activity_mediciones.xml` (`ic_peso` 64 dp + texto) | Media |
| P-30 | El botón de borrar de cada fila de sesión mide 36 dp. Ver T-03. | `item_sesion.xml`, `btnEliminarSesion` | Media |
| P-31 | La valoración de la sesión se pinta con el emoji ⭐ **dentro del propio string traducible**: `⭐ %d/5`. El símbolo viaja en el recurso de idioma, donde no pinta nada. | `values/strings.xml:261` · `values-en/strings.xml:272` | Media |

### 3.10 Logros

Lista de los seis logros del sistema, marcando los desbloqueados.

Capturas: `logros-es-oscuro.png`, `logros-vacio-es-oscuro.png`

| # | Hallazgo | Evidencia | Severidad |
|---|---|---|---|
| P-32 | **Los seis logros llevan el mismo emoji de trofeo.** El adaptador pinta `logro.getIcono()` y cae al literal `"🏆"` cuando viene vacío; la tabla `logros` **no tiene columna `icono`**, así que el caso «vacío» es el único que existe. El icono no distingue nada: es la misma decoración repetida seis veces. | `LogroAdapter.java:52` · `DESCRIBE logros` (sin columna `icono`) · `logros-es-oscuro.png` | Alta |
| P-33 | Los logros bloqueados se atenúan con `setAlpha(0.5f)` sobre toda la fila. El color efectivo de la descripción queda en **2,78:1 en oscuro y 2,23:1 en claro**: falla AA en los dos temas. La cabecera de `values/colors.xml` avisa justo de esto («el token que sustituye a los `android:alpha` sobre texto, que bajaban hasta 2,14:1»). | `LogroAdapter.java:63` · cálculo del color mezclado con el fondo | Alta |
| P-34 | Estado vacío de solo texto, igual que Sesiones. Ver P-29. | `activity_logros.xml` (`tvVacio` sin icono) | Media |

### 3.11 Eliminar cuenta y Acerca de

Pantalla de borrado definitivo (GP-008) y ficha de la app.

Capturas: `eliminar-cuenta-es-oscuro.png`, `eliminar-cuenta-en-claro.png`,
`acerca-de-es-oscuro.png`

| # | Hallazgo | Evidencia | Severidad |
|---|---|---|---|
| P-35 | En ES + claro la dirección `soporte@gymprofit.app` **se parte dentro del dominio**: la línea corta después de «gymprofit» y la siguiente empieza por «.app», que se lee como un espacio en medio del correo. En EN el mismo párrafo no se parte. | `eliminar-cuenta-en-claro.png` | Baja |
| P-36 | El icono del enlace de conservación lleva `android:alpha="0.55"`; su color efectivo en claro queda en **2,45:1**, por debajo del 3:1 que WCAG pide a un elemento gráfico informativo. | `activity_eliminar_cuenta.xml:254` · cálculo del color mezclado | Media |
| P-37 | Es de las pocas pantallas con `Toolbar` de Material y de las que mejor resuelven el contraste. Se anota como referencia del patrón de cabecera correcto. | `activity_eliminar_cuenta.xml:21` | — |

### 3.12 Rutinas: crear y detalle

Asistente de creación en varios pasos y ficha de la rutina.

Capturas: `crear-rutina-es-oscuro.png`, `crear-rutina-error-es-oscuro.png`,
`detalle-rutina-es-oscuro.png`

| # | Hallazgo | Evidencia | Severidad |
|---|---|---|---|
| P-38 | **El campo de nombre acepta texto duplicado sin avisar.** Al escribir en él dos veces quedó «RutinaRutina» y el formulario lo dio por bueno; el error que saltó fue el de descripción. Esto es lo que impidió llegar a `AnadirEjercicios` y capturar los pasos 2 y 3. | `crear-rutina-error-es-oscuro.png` | Media |
| P-39 | El mensaje «Este campo es obligatorio» aparece bajo la descripción sin que el campo quede marcado en rojo ni haya resumen arriba: hay que buscar dónde ha fallado. La pantalla de borrado de cuenta sí marca el campo (`tilPassword.setError`), así que el patrón bueno ya existe en la app. | `crear-rutina-error-es-oscuro.png` frente a `EliminarCuentaActivity.java:177` | Media |
| P-40 | El detalle de rutina repite las calorías estimadas de la lista. Ver P-13. | `detalle-rutina-es-oscuro.png` · `RutinasFragment.java:122` | Alta |

### 3.13 Panel de administración

Panel con estadísticas globales y cuatro secciones de gestión.

Capturas: `admin-panel-es-oscuro.png`, `admin-usuarios-es-oscuro.png`,
`admin-rutinas-es-oscuro.png`, `admin-ejercicios-es-oscuro.png`,
`admin-alimentos-es-oscuro.png`

| # | Hallazgo | Evidencia | Severidad |
|---|---|---|---|
| P-41 | **Rótulos inconsistentes en las cuatro secciones**: «Gestionar usuarios», «Gestionar rutinas», «Gestionar ejercicios» y «Gestión de alimentos». Tres con verbo, una con sustantivo. | `admin-panel-es-oscuro.png` | Baja |
| P-42 | De las seis tarjetas de estadística, **una está en naranja sólido sin criterio**: «Total usuarios». No es el dato más importante ni el único accionable; el color destaca algo que no lo pide. | `admin-panel-es-oscuro.png` | Baja |
| P-43 | Las cinco pantallas de administración usan cabecera manual con `btnBack` y `btnMenuOpciones` de 40 dp: **diez** de las zonas pulsables pequeñas de la app están aquí. Ver T-03 y T-05. | `activity_admin.xml`, `activity_admin_usuarios.xml`, `activity_admin_rutinas.xml`, `activity_admin_ejercicios.xml`, `activity_admin_alimentos.xml` | Media |
| P-44 | El botón de acciones de cada fila mide **32 dp**, el tamaño pulsable más pequeño de toda la app, y es el que abre el menú de editar y borrar. | `item_admin_alimento.xml`, `item_admin_ejercicio.xml`, `item_admin_rutina.xml`, `item_admin_usuario_v2.xml` | Alta |
| P-45 | Ninguna de las cuatro listas de administración tiene estado vacío. Ver T-06. | `activity_admin_*.xml`, sin `tvVacio` ni `tvEmpty` | Media |

---

## 4. Pantallas bloqueadas por GP-010, GP-012 y GP-016

Estas pantallas **cambian de función**, no solo de aspecto: GP-012 y GP-016
convierten el formulario retrospectivo en una sesión en vivo, y GP-010 retira las
calorías de Home y del resumen. Se inventarían y se capturan, pero **no se propone
rediseño**: hacerlo ahora sería hacerlo dos veces.

Capturas: `registrar-sesion-es-oscuro.png`, `resumen-sesion-es-oscuro.png`

| Pantalla | Estado | Qué queda anotado, sin propuesta |
|---|---|---|
| `RegistrarSesionActivity` | Bloqueada por GP-012 y GP-016 | Cabecera manual con `btnBack` de 40 dp. Lista de ejercicios sin estado vacío. Es donde DEC-004 sitúa la contradicción de calorías ya reconocida. |
| `ResumenSesionActivity` | Bloqueada por GP-010 | 27 usos de `android:textSize`, el máximo de la app. Muestra «Calorías · 0 kcal» y una tarjeta «Calorías quemadas · 0» cuando el valor llega nulo: pinta un cero como si fuera un dato medido. Lleva el emoji `✓` dentro del string traducible `resumen_completada` (`values/strings.xml:457`). |
| Bloque de entrenamiento de Home | Bloqueado por GP-010 | Los hallazgos de Home que **no** dependen de esto (P-08 barra flotante, P-09 silueta, P-10 dorado) siguen en pie. |

**Una salvedad que conviene no perder.** Las calorías de entrenamiento de la
**lista de rutinas** (P-13), del **detalle de rutina** (P-40) y de la **ficha de
ejercicio** (P-19) **no están cubiertas por GP-010**, que retira las de Home y las
del resumen de sesión. Son tres puntos de exposición distintos, alimentados por el
`@Formula` de `Rutina.java:90-91` y por `ejercicios.calorias_quemadas`. No se
marcan como bloqueados.

---

## 5. Patrones transversales

Los que se repiten valen más que los sueltos: se arreglan de una vez.

### T-01 · La barra de navegación flotante tapa el final de las cinco pestañas · **Alta**

La `FloatingNavBar` mide **72 dp de alto más 14 dp de margen inferior: 86 dp**.
Los cinco contenedores reservan como mucho **16 dp** de `paddingBottom`. Faltan
unos 70 dp en todas las pestañas, y lo que cae ahí no se puede leer ni tocar sin
adivinar que hay más abajo.

| Pantalla | `paddingBottom` | Qué queda tapado |
|---|---|---|
| Home | 10 dp / 16 dp | «Registrar comida», una acción primaria |
| Rutinas | 12 dp | La última tarjeta de rutina |
| Ejercicios | 12 dp | El último ejercicio de la lista |
| Nutrición | 6 dp | «Comida» y «Merienda» |
| Perfil | 12 dp | «Mis logros» |

Evidencia: `activity_main.xml:25,29` · los cinco layouts · `home-es-claro.png`,
`rutinas-vacio-es-oscuro.png`, `nutricion-vacio-es-oscuro.png`.

Rutinas y Ejercicios llevan `clipToPadding="false"`, que permite dibujar sobre el
padding pero **no reserva sitio**: no arregla el solape.

### T-02 · Los estilos tipográficos están definidos y no los usa nadie · **Media**

`values/themes.xml` declara **15 estilos** `Font.GymProFit.*` revestidos sobre la
escala M3 (`DisplayLarge` … `LabelSmall`), líneas 78 a 137.

**Ningún layout de la app usa uno solo de ellos.** En su lugar, **61 layouts**
aplican `android:textSize`, **347 veces** en total.

| Layout | Usos de `textSize` |
|---|---|
| `activity_resumen_sesion.xml` | 27 |
| `activity_perfil.xml` | 23 |
| `activity_mediciones.xml` | 22 |
| `activity_home.xml` | 21 |
| `activity_admin.xml` | 16 |
| `activity_onboarding_resumen.xml` | 15 |
| … 55 layouts más | |

La escala de `dimens.xml` es coherente en sí misma (11 tamaños, de 12 sp a 76 sp),
pero un tamaño suelto no es un estilo: no arrastra peso, interletrado ni altura de
línea, y por eso el mismo nivel jerárquico se pinta distinto según la pantalla.
**Lo positivo**: no hay ni un `textSize` con valor literal en `sp`; los 347 pasan
por `@dimen`.

### T-03 · 35 zonas pulsables por debajo de 48 dp · **Alta**

WCAG 2.1 (criterio 2.5.5) y Material piden 48 dp. Tres familias:

| Familia | Tamaño | Casos |
|---|---|---|
| `btnBack` y `btnMenuOpciones` de las cabeceras manuales | 40 dp | 29 |
| Botón de acciones de las filas de administración | 32 dp | 4 |
| Botón de borrar de fila (sesión, ejercicio seleccionado) | 36 dp | 2 |

A esto se suma `tvNoTienesCuenta` del login (P-03), que no declara alto y queda en
unos 20 dp efectivos.

Ninguna de las tres familias es un caso aislado: **las 35 salen de tres decisiones,
no de 35**.

### T-04 · Casi un tercio del espaciado se sale de la escala · **Media**

La escala **de facto** es la de Material, múltiplos de 4 dp: 16 dp (235 usos),
8 dp (163), 12 dp (125), 4 dp (55), 24 dp (52), 20 dp (44).

De los **1 005** valores de margen y padding de la app, **285 (28,4 %)** no son
múltiplos de 4:

| Valor | Usos |
|---|---|
| 14 dp | 87 |
| 10 dp | 64 |
| 6 dp | 61 |
| 5 dp | 32 |
| 2 dp | 25 |
| 18, 22, 26, 54 dp | 16 |

14 dp con 87 usos no es un descuido puntual: es una segunda escala informal
conviviendo con la buena. Los peores layouts: Home (27), resumen de onboarding
(23), onboarding 5 (21), Nutrición (18), Perfil (16).

Y un dato que explica el resto: **`dimens.xml` solo define tamaños de texto**. No
hay ni un token de espaciado, así que los 1 005 valores están escritos a mano, uno
a uno, en los layouts.

### T-05 · Dos patrones de cabecera, y tres pantallas con los dos · **Media**

| Patrón | Pantallas |
|---|---|
| `Toolbar` de Material | 11 |
| Cabecera manual (`ImageView` `btnBack` + `TextView` de título) | 18 |
| **Los dos en el mismo layout** | 3: `activity_anadir_alimento.xml`, `activity_comida.xml`, `activity_crear_alimento.xml` |

De aquí salen las 29 zonas pulsables de 40 dp de T-03: la cabecera manual no
hereda el tamaño táctil que la `Toolbar` trae de fábrica. Es el patrón que más
delata que la app se construyó por partes y en momentos distintos.

### T-06 · Los estados vacíos son tres sistemas distintos, y once listas no tienen ninguno · **Media**

Tres composiciones y dos nomenclaturas conviviendo:

| Patrón | Composición | Pantallas |
|---|---|---|
| `tvVacio` solo texto | Una línea gris centrada | Sesiones, Logros |
| `tvVacio` con icono | Icono de 64 dp más texto | Mediciones |
| `tvEmpty` con string genérico | `feedback_lista_vacia`, el mismo texto para cualquier lista | Ejercicios, Rutinas |

Y **once pantallas con `RecyclerView` no tienen estado vacío de ninguna clase**:
las cuatro de administración, `anadir_alimento`, `anadir_ejercicios`, `comida`,
`detalle_rutina`, `editar_rutina`, `registrar_sesion` y `resumen_crear_rutina`.

**Carga y error**: la carga es un `LoadingDialog` modal en 21 clases y un
`ProgressBar` en línea en 3 (`nutricion`, `onboarding_resumen`, `splash`) — dos
mecanismos para lo mismo. El error se resuelve **siempre** por toast: ninguna
pantalla tiene estado de error en el propio contenido, así que un fallo de red deja
una pantalla vacía y un aviso que se va solo a los tres segundos.

### T-07 · Emojis usados como icono: inventario completo · **Alta**

Diez sitios, en cinco grupos. El criterio a extender es el de `59ea6f4`:
`VectorDrawable` teñido por `tint`, el mismo archivo para claro y oscuro, como
`ic_objetivo_*`.

| Emoji | Dónde | Archivo |
|---|---|---|
| 🌱 💪 🏆 ⚡ | Niveles de experiencia del onboarding | `activity_onboarding5.xml:69,98,127,156` |
| 💧 | Agua recomendada del resumen de onboarding | `activity_onboarding_resumen.xml:249` |
| 🏆 | Icono de **todos** los logros (literal de respaldo que siempre acaba usándose) | `LogroAdapter.java:52` |
| ⭐ | Valoración de sesión, **dentro del string traducible** | `values/strings.xml:261` · `values-en/strings.xml:272` |
| ✓ | «Completada» del resumen de sesión, **dentro del string traducible** | `values/strings.xml:457` · `values-en/strings.xml:458` |

Los dos últimos son el caso peor: el símbolo viaja dentro del recurso de idioma, así
que al traducir hay que acordarse de arrastrarlo, y no se puede teñir ni alinear
con el resto de la fila.

**Lo ya correcto**: el paso 4 del onboarding usa `ic_objetivo_*`, y las cinco
pestañas, el panel de administración y las filas del perfil usan vectores teñidos.
El sistema existe y funciona; lo que falta es terminar de extenderlo.

### T-08 · Ocho colores escritos a fuego en cuatro layouts · **Media**

La cabecera de `values/colors.xml` lo dice sin ambigüedad: «los layouts no deben
hardcodear hex, siempre `@color/...` o `?attr/...`».

| Layout | Línea | Valor | Efecto |
|---|---|---|---|
| `activity_detalle_ejercicio.xml` | 76, 83 | `#FFFFFF` | Marco blanco puro en tema oscuro (P-18) |
| `activity_detalle_ejercicio.xml` | 96 | `#33000000` | Tinte negro fijo, invisible en oscuro |
| `activity_home.xml` | 469 | `#33FFFFFF` | Tinte blanco fijo, casi invisible en claro |
| `activity_splash.xml` | 10, 32, 43 | `#141414`, `#C9963A` ×2 | Colores fuera de paleta (P-02) |
| `dialog_loading.xml` | 24 | `#C9963A` | El indicador de carga **de toda la app** ignora el tema |

El último es el más caro: `dialog_loading` aparece en las 21 pantallas que usan
`LoadingDialog`, siempre con el mismo dorado fuera de paleta.

### T-09 · El tema claro es el que falla el contraste; el oscuro está bien · **Alta**

Calculados los pares reales de la paleta. **En oscuro, 21 de 22 pares pasan AA.**
En claro fallan seis:

| Par | Claro | Oscuro |
|---|---|---|
| Dorado sobre fondo (racha, logros) | **2,80:1** ✗ | 10,61:1 ✓ |
| Dorado sobre tarjeta | **3,17:1** ✗ | 9,05:1 ✓ |
| Macro de carbohidratos sobre tarjeta | **3,39:1** ✗ | 8,12:1 ✓ |
| Chip de éxito (texto sobre su contenedor) | **3,63:1** ✗ | 7,89:1 ✓ |
| Éxito sobre tarjeta | **4,18:1** ✗ | 9,58:1 ✓ |
| Borde de tarjeta sobre tarjeta | **1,66:1** ✗ | 2,06:1 ✗ |

El borde falla en los dos temas: WCAG pide 3:1 a un elemento de interfaz que
delimita. El propio `colors.xml` documenta ese 1,66:1 como mejora sobre el 1,15:1
anterior, así que es deuda consciente, no descuido.

A eso se suman los **104 `android:alpha` de los layouts** más el `setAlpha(0.5f)`
de `LogroAdapter`. Un `alpha` sobre texto no reduce solo la opacidad percibida:
mezcla el color con el fondo y tira el ratio abajo. Medido: un `alpha="0.6"` sobre
el texto secundario deja **2,71:1 en claro**, y el 0,5 de los logros bloqueados,
**2,23:1**. La cabecera de `colors.xml` ya avisa de esto, y aun así hay 104 usos.

**Nota**: `orange_dark` (`#FF8A3D`) daría 2,07:1 sobre el fondo claro, pero **no lo
usa nadie**: es un color muerto en `colors.xml`.

### T-10 · El contenido de la base de datos no está traducido · **Alta**

DEC-020 exige recursos en ES y EN. Las cadenas de la app **cumplen**: 569 strings
traducibles en cada idioma, sin ninguno suelto (las dos diferencias del recuento
bruto son las URLs marcadas `translatable="false"`).

Pero **el contenido que llena las pantallas no**:

- **752 de 873 ejercicios (86 %)** tienen `nombre` idéntico a `nombre_en`.
- **479 descripciones** idénticas entre los dos idiomas.
- La tabla `logros` sí está traducida: los seis tienen `nombre_en` y
  `descripcion_en` rellenos.

Con la app en español, la pestaña de Ejercicios —873 filas— y toda ficha de
ejercicio se leen en inglés. Es el contenido más visible de la app y el que menos
cumple la decisión.

---

## 6. Resumen por severidad

| Severidad | Hallazgos |
|---|---|
| **Alta** (21) | P-03, P-05, P-06, P-08, P-10, P-13, P-14, P-15, P-16, P-19, P-22, P-26, P-32, P-33, P-40, P-44 · T-01, T-03, T-07, T-09, T-10 |
| **Media** (26) | P-01, P-02, P-04, P-07, P-09, P-11, P-12, P-18, P-23, P-24, P-27, P-28, P-29, P-30, P-31, P-34, P-36, P-38, P-39, P-43, P-45 · T-02, T-04, T-05, T-06, T-08 |
| **Baja** (6) | P-17, P-20, P-25, P-35, P-41, P-42 |

**Dónde está el volumen.** Cinco arreglos transversales se llevan por delante la
mayor parte de la lista: reservar 86 dp bajo las cinco pestañas (T-01), subir a
48 dp las cabeceras manuales y los botones de fila (T-03 y T-05), sustituir los
diez emojis por vectores (T-07), cambiar los `android:alpha` sobre texto por
tokens de color (T-09) y unificar el estado vacío (T-06).

**Lo que está bien y conviene no romper.** El tema oscuro pasa AA en 21 de 22
pares. La paleta está documentada con sus ratios uno a uno. No hay ni un
`textSize` literal. Los vacíos de Nutrición y de Home están bien resueltos. Y el
criterio de iconos vectoriales de `59ea6f4` ya funciona donde se aplicó: no hay
que inventar un sistema, hay que terminar de extender el que existe.
