# Auditoría UX y plan de rediseño — septiembre 2026

Auditoría de las 42 pantallas de la app más el coste de render del shell, hecha
el **18 de septiembre de 2026** tras dos meses sin tocar el proyecto (último
commit: 20 de julio). Cinco revisiones en paralelo —entrada y alta, home y
perfil, entrenamiento, nutrición y administración, y dirección comercial— más una
medición del coste de render de la barra de navegación.

Versión navegable y filtrable: <https://claude.ai/artifact/8QMrQ8xhHsY4XWzjAfdZwc>

Este documento es la referencia de trabajo: **consultarlo antes de tocar
cualquier pantalla**. Todo el trabajo se hace en el entorno aislado descrito en
[ENTORNO-PRUEBAS.md](ENTORNO-PRUEBAS.md).

## Índice

1. [Bloqueantes](#1-bloqueantes)
2. [Decisiones de diseño tomadas](#2-decisiones-de-diseño-tomadas)
3. [Sistema visual](#3-sistema-visual)
4. [Reestructuraciones de pantalla](#4-reestructuraciones-de-pantalla)
5. [Hallazgos por zona](#5-hallazgos-por-zona)
6. [Rendimiento y deuda técnica](#6-rendimiento-y-deuda-técnica)
7. [Plan de trabajo en 6 fases](#7-plan-de-trabajo-en-6-fases)
8. [Ficha de Play Store](#8-ficha-de-play-store)

---

## 1. Bloqueantes

Seis cosas que no son opinión de diseño: cierran la app, la dejan mal colocada en
pantalla o rompen el ciclo del producto en silencio. **Las seis comprobadas línea
a línea en el repositorio.**

### 1.1 La app se cierra al crear una rutina

`Integer.parseInt` sin protección sobre un campo numérico sin `maxLength`. Once
dígitos en "duración" lanzan `NumberFormatException`.

- `CrearRutinaActivity.java:88` · `EditarRutinaActivity.java:288`
- `AnadirEjerciciosActivity.java:231` (series y repeticiones)
- `activity_crear_rutina.xml:136` · `dialog_series_reps.xml:30,50` (sin `maxLength`)

**Arreglo:** `maxLength` en el XML, parseo defensivo y rangos (5-300 min, 1-10
series, 1-50 repeticiones).

### 1.2 La coma decimal cierra la app

El teclado español ofrece coma. `Double.parseDouble("1,75")` y
`new BigDecimal("75,5")` revientan. En mediciones el mensaje que sale es **"Error
de conexión con el servidor"**, así que el usuario cree que no tiene internet.

- `Onboarding3Activity.java:61` (altura)
- `MedicionesActivity.java:438,451,506` · `RegistrarMedicionActivity.java:174`
- El patrón correcto **ya existe** en `EditarPerfilActivity.java:175`

**Arreglo:** normalizar la coma antes de parsear y, si aun así falla, mensaje
real ("introduce un número válido").

### 1.3 Edge-to-edge sin gestionar con targetSdk 36

Cero coincidencias de `setOnApplyWindowInsetsListener`, `EdgeToEdge.enable()` o
`fitsSystemWindows` en todo el proyecto. En Android 15+ el edge-to-edge es
obligatorio: cabeceras bajo la barra de estado y botones bajo la de navegación,
**en las 42 pantallas**.

- `build.gradle:15-20` — compileSdk 36, minSdk 24, targetSdk 36
- `Onboarding4Activity.java:9-13` ya importa `EdgeToEdge`, `Insets` y
  `WindowInsetsCompat` sin usarlos: el andamiaje quedó a medias

**Arreglo:** aplicar los insets de `systemBars()` en una `BaseActivity` común.
Ya hay un ejemplo funcionando en `GymProFitApp.marcarPantallaComoPruebas()`.

### 1.4 Se puede quedar sin onboarding para siempre

El splash solo comprueba si hay sesión, nunca si el onboarding se completó. Quien
cierre la app a mitad entra directo sin peso, altura, edad ni objetivo, y no
vuelve a ver el onboarding jamás: los datos viajaban en extras del Intent.

- `SplashActivity.java:84-88`
- `OnboardingResumenActivity.java:65-71` — además inventa valores por defecto
  (25 años, hombre, 70 kg, 170 cm) y presenta el plan como "basado en tus datos"

**Arreglo:** comprobar también el flag de onboarding y persistir cada paso al
avanzar, en vez de arrastrarlo en extras.

### 1.5 El ciclo del producto se rompe en el caso por defecto

Registrar sesión arranca con "Sin rutina asociada" preseleccionado. Guardar así
crea una sesión sin ejercicios, no genera progreso, y la gráfica de progresión
con el récord en dorado —lo mejor construido de la app— no aparece nunca.

- `RegistrarSesionActivity.java:229-231`
- `DetalleRutinaActivity` no tiene ningún botón para entrenar la rutina

**Arreglo:** preseleccionar la última rutina usada y añadir "Entrenar esta
rutina" como acción primaria del detalle.

### 1.6 La app se presenta como un trabajo de clase

"Sobre GymProFit" es el currículum del desarrollador: instituto, dos
titulaciones, tres prácticas con horas y el stack técnico. La descripción empieza
diciendo que es un Trabajo Final de Ciclo.

- `strings.xml:539` (descripción con "Trabajo Final de Ciclo del CFGS DAM")
- `strings.xml:551,553,555` (rol, biografía y formación del autor)
- `strings.xml:571` — teléfono **falso** `+34 600 000 000`, y pulsarlo abre el marcador
- `strings.xml:570,574` — web y enlace de compartir a un hosting gratuito, apuntando al **login**
- `activity_detalle_ejercicio.xml:103` — "Vídeo próximamente" en la mejor pantalla de la app
- `activity_acerca_de.xml:242-609` — 368 de 618 líneas son el CV

Los otros tres strings `*_proximamente` (`registro_proximamente`,
`nutricion_proximamente`, `perfil_editar_proximamente`) están **muertos**: no los
referencia nadie. Se borran.

---

## 2. Decisiones de diseño tomadas

Decididas por el usuario el 18 de septiembre de 2026.

### Barra de navegación → dirección "Ember"

Se retira la lente con refracción AGSL. El motivo medido: `FloatingNavBar.java:370-388`
crea un objeto `RenderEffect` nuevo y llama a `setRenderEffect` **en cada frame**
de arrastre, viaje y rebote, lo que fuerza composición offscreen por hardware.
(No hay animadores vivos en reposo: los tres se cancelan bien, así que no drena
batería quieta. El coste es por gesto.)

| | Especificación |
|---|---|
| Cápsula | 68 dp de alto, radio 34 dp, inset lateral 14 dp, margen inferior = inset del sistema + 10 dp |
| Fondo | `#151619` sólido, borde superior 1 dp blanco al 8% |
| Activo | halo `RadialGradient` de 56 dp, naranja 30% → transparente, pintado en el Canvas |
| Movimiento | el centro del halo viaja con spring 320 / 0.75; el radio late 56 → 68 → 56 dp en 220 ms |
| Iconos | activo 26 dp al 100%, inactivos 22 dp al 45%; etiqueta siempre visible, 10 sp |
| Punto | 4 dp naranja, 6 dp bajo el icono activo |
| Coste | un quad extra por frame. Sin shader, sin blur, sin capa offscreen |
| Compatible | API 21+ al 100% (hoy el efecto completo exige API 33) |

**Condiciones del usuario:** sin emojis; iconos vectoriales monocromos que tomen
el color del tema y se lean bien en claro y en oscuro.

**Se conserva** el gesto de arrastre con inercia y gravedad magnética: era CPU
barata, lo caro era el shader.

**Se pierde** frente a la barra actual: la magnificación, la refracción y la
aberración cromática. Ninguna app de referencia (Nike Training Club, Strava,
Hevy) usa lente ni refracción: todas marcan el activo con **color y peso**.

### Objetivos del onboarding: de 10 a 4

Decidido el 18 de septiembre de 2026. Hoy el onboarding ofrece **10 objetivos** y
el enum de la API tiene **12**, con ramas de cálculo que se solapan (flexibilidad
y movilidad comparten rama) y dos opciones que el usuario lee como lo mismo
("perder peso" y "reducir grasa").

Se quedan **cuatro**, los que de verdad cambian el plan de calorías y macros:

| Se muestra | Explicación (en resultado, no en mecanismo) | Se guarda como |
|---|---|---|
| Perder grasa | Comerás algo menos de lo que gastas | `PERDER_PESO` |
| Ganar músculo | Comerás algo más y con más proteína | `GANAR_MASA_MUSCULAR` |
| Mantenerme | Las mismas calorías que gastas | `MANTENER_PESO` |
| Ganar fuerza | Más proteína, con una subida moderada | `MEJORAR_FUERZA` |

"Perder peso" y "Reducir grasa" **se fusionan en "Perder grasa"**, que es lo que
la gente quiere decir, y se guarda como `PERDER_PESO`.

Se retiran de la app **y de la API**: `MEJORAR_RESISTENCIA`,
`MEJORAR_FLEXIBILIDAD`, `MEJORAR_VELOCIDAD`, `AUMENTAR_CALORIAS`,
`MEJORAR_MOVILIDAD`, `REDUCIR_GRASA_CORPORAL`, `COMPLETAR_RETO` y `OTRO`. Exige
migración Flyway que reasigne los valores antiguos antes de estrechar el enum, y
limpiar la rama correspondiente de `CalculadoraNutricional` en la app.

**Iconos: línea fina monocroma.** Vectores de trazo (estilo Material Symbols
outlined), 28 dp, trazo 2 dp, **sin emojis**. El color lo pone el tema por tint:
`?attr/colorOnSurfaceVariant` en reposo y el naranja de marca en el seleccionado,
así se leen igual en claro y en oscuro. La tarjeta seleccionada se marca además
con `android:checkable` y su icono de comprobación, que es lo que hace que el
lector de pantalla anuncie el estado (hoy la selección solo se distingue por el
color del borde).

### Naranja unificado en los dos temas

Hoy `gp_primary` es teal `#0F766E` en claro y naranja `#FF6A00` en oscuro
(`values/colors.xml:15` frente a `values-night/colors.xml`). La app cambia de
identidad al cambiar de tema, y en claro el acento pelea con el logo, que es
naranja en ambos.

- **Texto e iconos sobre blanco:** `#C24800` (contraste AA)
- **Rellenos:** `#FF6A00`
- **Presupuesto:** un solo elemento naranja saturado por pantalla; el resto,
  naranja al 12-18% como fondo de chip o al 35% como borde
- Texto secundario: blanco al 60%, **nunca** naranja apagado

---

## 3. Sistema visual

Reglas transversales. Se aplican una vez en estilos y revisten toda la app.

### Espaciado

Escala base 4: **4 / 8 / 12 / 16 / 24 / 32 / 48 / 64**. Nada fuera de escala.
Padding lateral de pantalla siempre **20 dp**. Separación entre bloques 24 dp;
dentro de un bloque, 8 o 12 dp. Nunca dos separaciones distintas para la misma
relación jerárquica.

### Superficies

Tres niveles y ni uno más: fondo `#121316`, tarjeta `#1A1B1F`, elevada `#212227`.
La separación se hace **por valor**, no por borde. Borde 1 dp blanco al 8% solo
cuando dos superficies del mismo nivel se tocan.

### Tipografía

Barlow Condensed en toda la app. **Tracking negativo al crecer, positivo al
encoger** — un tracking global es un error.

| Rol | Tamaño | Peso | Tracking | Interlineado |
|---|---|---|---|---|
| Dato XXL | 56 sp | 700 | −0.02em | 1.0 |
| Título de sección | 20 sp | 600 | +0.01em | — (mayúsculas) |
| Cuerpo | 15 sp | 400 | 0 | 1.45 |
| Etiqueta | 11 sp | 600 | +0.06em | — (mayúsculas) |
| Número en línea | 17 sp | 600 | — | tabular |

Esto elimina la necesidad del parche actual de escala global del 18%
(`ScaleUtils.FONT_SCALE`), que existe porque 365 `textSize` están fijados a mano
en los layouts.

**Peso antes que tamaño:** para destacar dentro de un bloque, subir de 400 a 600
antes de subir de tamaño.

### Movimiento

Entradas 180 ms; **salidas 140 ms — siempre más rápidas que la entrada**.
Movimiento en pantalla con spring damping 0.85. Rebote (damping 0.7) solo después
de un gesto con inercia. Nunca `alpha` desde 0 con `scale` desde 0: mínimo
`scale 0.94`.

### Dónde no va nada

Regla de restricción. **No hay efecto, animación ni naranja en:**

- Texto de contenido. El naranja no es color de párrafo.
- Filas de lista y RecyclerView: sin animación de entrada, sin stagger, sin
  elevación por scroll. Se ven cientos de veces al día.
- Números en vivo durante un entrenamiento: sin cuenta ascendente, sin brillo.
- Estados de error: rojo funcional, cero decoración.
- Formularios: solo cambio de borde a 120 ms al enfocar.
- Detrás de contenido denso (listas de alimentos, tablas): **nunca** blur ni
  transparencia. Fondo sólido siempre.

**Regla madre:** el naranja marca *dónde estás* y *qué puedes tocar ahora*. Si un
elemento naranja no responde al dedo ni indica posición, está mal puesto.

---

## 4. Reestructuraciones de pantalla

Cambios de forma, no retoques. Cada uno sustituye un recorrido por otro más corto.

### 4.1 Crear rutina: de tres pantallas a una

**Hoy:** Crear (nombre, descripción y duración obligatorias) → Añadir ejercicios
→ Resumen, *y el guardado real ocurre en la tercera*
(`ResumenCrearRutinaActivity.guardarRutina():180`). "Cancelar" ocupa la mitad de
la barra inferior, tiene el mismo tamaño que "Guardar" y destruye tres pantallas
de trabajo sin preguntar. Dos toques rápidos crean la rutina dos veces.

**Nuevo:** una sola pantalla con nombre arriba (único campo obligatorio), chips de
nivel, lista de ejercicios con series y repeticiones editables tocando la fila,
botón "Añadir ejercicio" que abre el buscador como hoja inferior (no Activity), y
barra fija "Guardar rutina".

Duración calculada de las series y editable; descripción opcional autogenerada
("N ejercicios · M min"). Desaparecen `ResumenCrearRutinaActivity` y el botón
destructivo.

### 4.2 Registrar sesión: de formulario a "repetir y ajustar"

**Hoy:** unas 33 interacciones para seis ejercicios, 28 de ellas de teclado. La
duración se teclea aunque la rutina ya la tenga. Cada peso se escribe desde cero
aunque el histórico esté en la API (`ProgresoEjercicioApi.getHistorial`). La
fecha es siempre hoy y no se puede cambiar.

**Nuevo:**
- Entrada desde "Entrenar esta rutina" en el detalle de la rutina
- Fecha editable (hoy por defecto) + duración precargada de la rutina
- Un ejercicio por fila con **el peso de la última vez ya escrito** y stepper
  −2,5 / +2,5, más distintivo de récord si supera el máximo
- "¿Cómo fue?" en tres chips, en campo propio (hoy se concatena como texto dentro
  de `notas`)

Baja a unas 6 interacciones al repetir una rutina conocida, y garantiza que
siempre se generen registros de progreso.

### 4.3 Home: de resumen semanal a "qué hago hoy"

**Hoy:** saludo, racha decorativa, tres contadores semanales sin referencia y tres
botones. Nada de hoy. El objetivo de calorías está calculado y guardado en local
(`PreferencesManager.getCaloriasDiarias`) y no se muestra. La acción principal
dice "Iniciar entrenamiento" y abre el **historial**. Ninguna de las cinco
gráficas de la app vive aquí.

**Nuevo, en este orden:**
1. Silueta muscular de la semana (ver 4.6)
2. Hoy: calorías consumidas sobre objetivo y "¿has entrenado hoy?", un solo botón grande
3. Racha pulsable, con aviso "entrena hoy o pierdes tu racha de 5 días" cuando la última sesión fue ayer
4. Esta semana: los tres números, cada uno con comparación ("3 de 4 previstos", "+12% que la semana pasada")

### 4.4 Perfil partido en dos, y los ajustes a la vista

**Hoy:** 681 líneas, 10 niveles de anidamiento y 14 `layout_weight` que fuerzan
doble medida. Mezcla identidad, historial, datos de salud, gamificación y el panel
de administración en la misma tarjeta. Los ajustes reales (tema, idioma, cerrar
sesión) están escondidos tras un icono de tres puntos sin descripción, que es la
única vía para salir de la cuenta.

**Nuevo:** (a) identidad; (b) **mis datos de salud** —peso, altura, edad, sexo,
actividad, nivel, objetivo, todos editables, incluidos sexo y actividad, hoy
inalcanzables tras el onboarding—; (c) mi progreso (sesiones, mediciones,
logros); (d) ajustes, rescatados del icono de tres puntos; (e) admin; (f) sobre
la app.

Técnicamente: raíz `ConstraintLayout` + un `<include>` de fila reutilizable. Baja
de 10 niveles a 4 y mata los 14 `layout_weight`.

### 4.5 Añadir alimento: recientes, raciones y sin cerrarse

**Hoy:** solo gramos —el modelo `Alimento` no tiene ningún campo de ración—, y
tras añadir un alimento la pantalla se cierra, así que una comida de tres cosas
obliga a reabrirla tres veces. La primera vez que se entra en "Desayuno" de cada
día la pantalla aparece **completamente en blanco** (`ComidaActivity.java:100-102`).

**Nuevo:** fila de recientes (un toque = registrado con su última cantidad),
resultados con "+" directo, hoja inferior con nombre del alimento, chips
`½ / 1 / 2 raciones` y gramos plegado, y al añadir un "Añadido · Deshacer" que
deja seguir buscando.

De seis interacciones por alimento a dos. **Es el único cambio de esta lista que
toca la base de datos**: necesita `gramosPorRacion` en el modelo, con su
migración Flyway.

### 4.6 La silueta muscular (el gancho comercial)

De todo lo que ya existe, es el activo más vendible: funciona el día 1 sin
historial, se entiende sin leer, y ninguna competidora (Fitia, MyFitnessPal,
Hevy, Strava) lo pone en portada — todas enseñan una retícula de texto y números.

Hoy **no es un mapa**: `UIHelper.cuerpoMuscular()` mapea los 16 `ic_body_*` a un
icono de 40 dp dentro de una fila de lista y a un dato del detalle.

**Qué construir:** silueta frontal y dorsal a lo ancho en la parte superior de
Home, con los músculos entrenados los últimos 7 días teñidos de `#FF6A00` por
intensidad (número de series que los tocaron) y los no tocados en gris carbón.
Encima, una frase: "ESTA SEMANA · 4 grupos · te falta pierna".

Un usuario nuevo lo ve gris entero, y eso ya es la llamada a la acción más fuerte
que puede tener la app: un cuerpo vacío que quiere llenar.

### 4.7 Reubicaciones de datos entre pantallas

| Elemento | De | A | Por qué |
|---|---|---|---|
| Silueta muscular agregada | no existe (icono en `item_ejercicio`) | cabecera de Home | es el gancho; hoy es decoración de lista |
| Gráfica de peso | Mediciones (3 toques) | tarjeta en Home, 30 días | la gráfica más universal, enterrada |
| Récord en dorado | Detalle de ejercicio | tarjeta en Home que abre el detalle | no se ve nunca si no buscas un ejercicio concreto |
| Anillo de calorías del día | Nutrición | Home, junto al cuerpo | el dato más consultado exige un toque |
| Mejor racha, total de sesiones y minutos | Resumen de sesión | Perfil, como cabecera | son credenciales del usuario, no resumen de un entreno |
| Bloque "Información" (nombre, email, edad…) | Perfil, media pantalla | dentro de Editar perfil | es un formulario en modo lectura; hunde lo que sí vende |
| Imagen real del ejercicio | solo el detalle | `item_ejercicio` | 873 filas con foto convierten la lista en escaparate |

---

## 5. Hallazgos por zona

97 hallazgos con archivo y línea. Los marcados como CRÍTICO bloquean o confunden
a un usuario no técnico; ALTO se ve amateur o genera dudas.

### Entrada y alta

Splash, login, registro, onboarding 1-5 y resumen. 18 hallazgos: 6 crítico · 9 alto · 3 medio.

| Gravedad | Qué está mal | Qué hacer | Dónde |
|---|---|---|---|
| CRÍTICO | El campo de login pide “Usuario” pero en el registro se dan usuario y correo. La API solo autentica por usuario: quien pruebe su correo recibe “usuario o contraseña incorrectos” sin entender por qué. | Cambiar a “Nombre de usuario” con ayuda: “el que elegiste al crear la cuenta, no tu correo”. Mejor: aceptar ambos en el endpoint. | `activity_login.xml:81 · strings.xml:14` |
| CRÍTICO | No existe “¿Olvidaste tu contraseña?” en ninguna parte de la app. Para un usuario no técnico, olvidarla equivale a perder la cuenta y desinstalar. | Enlace bajo el botón de entrar, conectado al flujo OTP por correo que ya está diseñado. | `activity_login.xml:173-180` |
| CRÍTICO | La app valida contraseñas de 6 caracteres; la API exige 8 con mayúscula, minúscula, dígito y símbolo. El usuario pasa la validación local y recibe un “Error al crear la cuenta” opaco. | Misma regla en cliente, ayuda permanente bajo el campo y los cuatro requisitos marcándose en verde al escribir. | `RegistroActivity.java:95-99 · RegisterDTO.java:31-34` |
| CRÍTICO | Todo fallo de login se traduce a “usuario o contraseña incorrectos”, incluido el 429 del limitador. Un usuario bloqueado reintenta y alarga su propio bloqueo. | Distinguir sin conexión, bloqueo temporal, credenciales y caída del servidor, cada uno con su instrucción. | `LoginActivity.java:171-173` |
| CRÍTICO | Los cuatro niveles de actividad (sedentario, ligero, moderado, activo) no se definen en ninguna parte, y son el multiplicador que decide todo el objetivo calórico. | Cuatro tarjetas con descripción concreta de días de ejercicio a la semana, sin preselección. | `activity_onboarding3.xml:106-174` |
| CRÍTICO | Peso y altura solo se validan como “no vacío”. Quien escriba la altura en metros obtiene un metabolismo basal de ~586 kcal y un plan entero de basura, sin que nada avise. | Rangos 100-250 cm y 30-300 kg con error en el campo, y sugerencia al detectar un valor menor que 3. | `Onboarding3Activity.java:49-52` |
| ALTO | Si falta un dato, el resumen lo inventa en silencio: 25 años, hombre, 70 kg, 170 cm. Y luego presenta el plan como “basado en tus datos”. | No inventar: volver al paso que falta, o declarar el supuesto con enlace para corregirlo. | `OnboardingResumenActivity.java:65-71` |
| ALTO | Las tres barras de macros del resumen están fijas a cero y nadie las actualiza: se lee “Proteínas 150 g” con la barra vacía, como si la pantalla estuviera rota. | Rellenarlas con el reparto real o eliminarlas. Una barra decorativa a cero es peor que ninguna. | `activity_onboarding_resumen.xml:173-233` |
| ALTO | El indicador de progreso del paso 1 tiene cuatro segmentos y el de los pasos 2 a 5 tiene cinco, cuando hay seis pantallas reales. El progreso salta y miente. | Un solo componente de seis posiciones con texto explícito “Paso 2 de 6”. | `activity_onboarding1.xml:88-99 · activity_onboarding2.xml:181-185` |
| ALTO | “Saltar” no avisa de nada: el usuario pierde el plan nutricional sin saberlo y aterriza en un Home con ceros. Además su área táctil ronda los 36 dp. | Botón de 48 dp y diálogo: “sin estos datos no podemos calcular tus calorías”. | `activity_onboarding1.xml:16-28` |
| ALTO | Los objetivos se describen con mecanismo, no con resultado: “déficit calórico controlado”, “superávit calórico + proteína alta”. | “Perder peso: comerás algo menos de lo que gastas”. Reescribir los diez en ese registro. | `strings.xml:426-434` |
| ALTO | Ningún botón primario se deshabilita durante la llamada de red. En conexión lenta no pasa nada visible, el usuario pulsa varias veces y acaba topando con el limitador. | Deshabilitar con indicador de carga y rehabilitar al responder. | `LoginActivity.java:84-94 · RegistroActivity.java:72-75` |
| ALTO | Ningún campo declara autofillHints ni imeOptions: el gestor de contraseñas no ofrece rellenar ni guardar, y el teclado no encadena los campos. | Pistas de autorrelleno y cadena “siguiente” hasta un “hecho” que dispare el botón. | `activity_login.xml:88-114 · activity_registro.xml:81-162` |
| ALTO | El borde de tarjeta seleccionada se fija en píxeles, no en dp: al tocar una tarjeta todos los bordes adelgazan y la elegida casi no destaca. | Convertir a dp o usar el estado “checked” de la tarjeta Material, que además se anuncia solo. | `Onboarding4Activity.java:168-176 · Onboarding5Activity.java:116-126` |
| ALTO | La selección de objetivo y nivel solo se comunica por color de borde: sin lector de pantalla y sin distinguir color, no hay forma de saber qué se ha elegido. | Tarjetas marcables con icono de selección; el estado se anuncia solo. | `activity_onboarding4.xml:80-140` |
| MEDIO | El saludo concatena a mano y produce “¡Bienvenido a / ruben!”; si el nombre viene vacío la frase queda cortada. | Un solo texto con marcador de posición y respaldo sin nombre. | `Onboarding1Activity.java:41 · strings.xml:206` |
| MEDIO | El splash tarda 1,5 segundos fijos aunque la comprobación sea instantánea, y usa colores fijos: en tema claro hay un destello negro antes del login. | Bajar la espera y usar los colores del tema o la pantalla de arranque del sistema. | `SplashActivity.java:30 · activity_splash.xml:10,32` |
| MEDIO | El sexo viene preseleccionado como hombre y cualquier otro estado cae en hombre. Cambia el metabolismo basal en unas 160 kcal al día. | No preseleccionar, exigir elección y explicar por qué se pregunta. | `Onboarding2Activity.java:79` |

### Home, perfil y progreso

Home, perfil, editar perfil, mediciones, logros, acerca de y el shell de navegación. 27 hallazgos: 11 crítico · 11 alto · 5 medio.

| Gravedad | Qué está mal | Qué hacer | Dónde |
|---|---|---|---|
| CRÍTICO | La acción principal de Home dice “Iniciar entrenamiento” y abre el historial de sesiones ya hechas. La promesa no se cumple: no existe ningún flujo de empezar ahora. | O renombrar a “Registrar entrenamiento” y abrir el formulario, o construir la sesión en curso. | `HomeFragment.java:186 · activity_home.xml:375` |
| CRÍTICO | La racha es el elemento más grande de Home, no es pulsable y no se explica. Nadie sabe qué la sube ni que hoy la pierde si no entrena. | Tarjeta pulsable con explicación e histórico, y aviso explícito cuando la última sesión fue ayer. | `activity_home.xml:93-182` |
| CRÍTICO | Un número grande llamado solo “Calorías” en una app con pestaña de nutrición: nadie sabe si son comidas o quemadas. Además el mismo icono sirve para racha y para calorías. | “Calorías quemadas”, icono propio, y menos peso visual por ser un dato que teclea el propio usuario. | `activity_home.xml:278` |
| CRÍTICO | Home no muestra el objetivo de calorías del día pese a estar calculado y guardado en local. La pregunta “¿qué hago hoy?” no se responde en ninguna parte. | Bloque “Hoy” arriba con consumidas sobre objetivo. Coste de red cero: el dato ya está en preferencias. | `activity_home.xml (completo)` |
| CRÍTICO | Guardar una medición no confirma nada: la pantalla parpadea y vuelve igual. El usuario no sabe si se guardó. | Confirmación explícita, como ya hace la pantalla de alta de medición. | `MedicionesActivity.java:483-484` |
| CRÍTICO | El guardado por día es invisible: la cabecera dice “Última medición: 12/07” y al tocar el peso el usuario cree que corrige ese registro, pero se crea uno nuevo con fecha de hoy clonando el resto. | Cabecera “Hoy, 18 sep” con selector de fecha, y marcar como arrastrados los valores que vienen de otro día. | `MedicionesActivity.java:441-449` |
| CRÍTICO | El estado vacío de mediciones dice “pulsa + para añadir” y no hay ningún +: el botón flotante se eliminó y el real dice “Registrar medición”. La instrucción es falsa. | “Añade tu peso para empezar a ver tu evolución”. | `activity_mediciones.xml:83` |
| CRÍTICO | Sexo y nivel de actividad solo se fijan en el onboarding y no se pueden cambiar nunca, pese a decidir el objetivo de calorías. Además viven solo en el dispositivo, no en el servidor. | Llevarlos a editar perfil y al servidor: son datos de salud, no preferencias del móvil. | `EditarPerfilActivity.java:207-209` |
| CRÍTICO | Los campos de peso y altura de editar perfil no dicen la unidad, y la altura no admite decimales: quien escriba 1,75 no puede y quien escriba 175 acierta por suerte. | Sufijos kg y cm en el campo, como ya hace el diálogo de mediciones. | `activity_editar_perfil.xml:82,102,113` |
| CRÍTICO | Los logros bloqueados solo se atenúan: no se ve el progreso. “Completa 7 sesiones” sin decir que vas por 3 no motiva a nadie. | Barra “3 de 7” por logro. Requiere que la API devuelva objetivo y conteo actual. | `LogroAdapter.java:57-63 · Logro.java:19-21` |
| CRÍTICO | La navegación principal no existe para el lector de pantalla: las cinco celdas no son pulsables, no tienen descripción y no anuncian cuál está seleccionada. | Celdas pulsables reales con descripción y estado de pestaña. | `FloatingNavBar.java:190-215` |
| ALTO | Los tres números de “esta semana” no tienen objetivo ni comparación: 3 / 840 / 120 no dice si va bien o mal. | Referencia en cada cifra: “3 de 4 previstos”, “+12% que la semana pasada”. | `activity_home.xml:219-323` |
| ALTO | El icono de tres puntos no tiene descripción y es la única vía para tema, idioma, contacto y cerrar sesión. | Descripción y, mejor, bajar los ajustes a filas visibles en Perfil, que es donde todo el mundo los busca. | `activity_home.xml:79 · activity_perfil.xml:54` |
| ALTO | La edición directa de mediciones solo se señaliza con un chevron de 16 dp al 40% de opacidad: las filas se leen como datos, no como campos editables. | Lápiz a plena opacidad en las que tienen valor y estilo de acción en las vacías. | `activity_mediciones.xml:250-304` |
| ALTO | La API devuelve el IMC calculado y la app no lo muestra en ninguna parte: es justo el único indicador que un usuario no técnico sabe interpretar. | Bajo el peso, con su franja: “22,4 — peso saludable”. | `MedicionCorporal.java:54` |
| ALTO | Los perímetros no explican cómo se miden ni qué es normal. “Cintura” no dice dónde medir y nadie sabe su porcentaje de grasa sin báscula especial. | Ayuda por fila: “mide a la altura del ombligo, sin apretar”. | `strings.xml:322-327` |
| ALTO | Existen descripciones escritas y traducidas para cada objetivo y cada nivel, y el desplegable no las usa: el usuario elige entre diez objetivos a ciegas. | Lista de dos líneas con título y descripción. El contenido ya está hecho. | `EditarPerfilActivity.java:83-110 · strings.xml:426-459` |
| ALTO | El botón de guardar de editar perfil está al final del scroll: con el teclado abierto queda tapado y hay que cerrarlo para guardar. | Botón fijo al pie, fuera del scroll, y cadena de teclado entre campos. | `activity_editar_perfil.xml:194-199` |
| ALTO | La pantalla de logros no tiene contador global ni separa conseguidos de pendientes, y se descarta la fecha en que se logró cada uno, que sí está en la base de datos. | Cabecera “2 de 6”, dos secciones y “conseguido el 12 jul” en cada uno. | `LogrosActivity.java:115-133 · UsuarioLogro.java:10-23` |
| ALTO | Las cuatro acciones de contacto miden unos 28 dp de alto con iconos de 16 dp, separadas por 6 dp: la zona más difícil de acertar de la app. | Filas de 56 dp a ancho completo con separador, como el resto de la app. | `activity_acerca_de.xml:481-600` |
| ALTO | Una Activity completa de alta de medición y su layout no se lanzan desde ningún sitio, junto a un adapter que nadie instancia: unas 650 líneas muertas. Se llevan por delante una notificación que ya no se dispara nunca. | Borrarlas, o recuperar el alta completa desde mediciones (hoy solo se puede dar de alta el peso). | `RegistrarMedicionActivity · MedicionAdapter · item_medicion.xml` |
| ALTO | Las cinco pestañas viven siempre en memoria y dos de ellas recargan red cada vez que vuelven a primer plano: cambiar de pestaña cuesta peticiones innecesarias. | Caché con marca de tiempo y recarga solo cuando el dato esté rancio o haya cambiado algo. | `MainActivity.java:56 · HomeFragment.java:69-73 · NutricionFragment.java:101-105` |
| MEDIO | Si falla la red, los tres contadores de Home muestran un guion sin ninguna explicación en pantalla: solo un aviso que se va solo. | Estado de error en la propia tarjeta con “tocar para reintentar”. | `HomeFragment.java:144-146` |
| MEDIO | Las filas de peso y altura del perfil están ocultas por XML pero el código las sigue rellenando. | Borrar filas y código, o mostrarlas. | `activity_perfil.xml:285,322 · PerfilFragment.java:173-176` |
| MEDIO | El diálogo de edición de mediciones no enfoca el campo ni abre el teclado, y no valida rangos: se puede guardar 750 kg. | Foco y teclado al abrir, y rango por campo con mensaje claro. | `InputDialog.java:44-63` |
| MEDIO | El fondo de la ventana y el del contenido pintan el mismo color, uno encima del otro, y encima se apilan tarjetas y círculos: tres o cuatro capas de repintado en las mismas zonas. | Quitar el fondo del contenido y dejar solo el de la ventana. | `activity_main.xml:11-14 · activity_home.xml:13` |
| MEDIO | La demostración del ejercicio relanza una carga de imagen cada 700 ms de forma indefinida mientras la pantalla esté abierta. | Dos imágenes ya decodificadas alternando, o un GIF/WebP animado. Se cancela bien al cerrar, eso está correcto. | `DetalleEjercicioActivity.java:184-198` |

### Entrenamiento

Rutinas, crear y editar rutina, catálogo de ejercicios, detalle y sesiones. 24 hallazgos: 9 crítico · 11 alto · 4 medio.

| Gravedad | Qué está mal | Qué hacer | Dónde |
|---|---|---|---|
| CRÍTICO | En la pantalla donde se eligen los ejercicios no se ve la lista de lo seleccionado: el único indicio es que el botón pasa de “Continuar (0)” a “(1)”. Si vuelves a pulsar uno ya añadido, sale un aviso de error. | Fila de chips con lo añadido y marca de “añadido” en cada resultado. | `AnadirEjerciciosActivity.java:115-144,216` |
| CRÍTICO | Sobre 873 ejercicios, los únicos filtros de esa pantalla son los tres niveles de dificultad. El filtro por músculo existe, pero está en la otra pantalla. | Chips musculares en primera fila, dificultad en la segunda, y un chip “sin material”. | `activity_anadir_ejercicios.xml:65-102` |
| CRÍTICO | “Continuar (0)” está activo con cero ejercicios y guarda una rutina vacía sin avisar. | Botón deshabilitado con el texto “añade al menos un ejercicio”. | `AnadirEjerciciosActivity.java:142,293-297` |
| CRÍTICO | “Cancelar” y “Guardar rutina” ocupan la mitad cada uno. Cancelar destruye tres pantallas de trabajo sin ningún diálogo: punto de no retorno invisible. | Dejar solo guardar a ancho completo; si se conserva cancelar, como texto en la barra superior y con confirmación. | `ResumenCrearRutinaActivity.java:107-110` |
| CRÍTICO | El botón de guardar rutina no se deshabilita ni muestra carga: dos toques crean dos rutinas duplicadas. | Indicador de carga y botón deshabilitado al entrar en el guardado. | `ResumenCrearRutinaActivity.java:189-203` |
| CRÍTICO | Editar, eliminar y activar una rutina solo existen en pulsación larga, sin ningún icono. Un usuario no técnico no lo descubre jamás. | Icono de tres puntos de 48 dp en cada fila, con el mismo menú. | `RutinasFragment.java:97 · RutinaAdapter.java:95-98` |
| CRÍTICO | La fecha de la sesión es siempre hoy y no se puede cambiar: quien apunta el entreno al día siguiente falsea su propia racha. | Selector de fecha con hoy por defecto. | `RegistrarSesionActivity.java:234-236` |
| CRÍTICO | “Series” y “Repeticiones” aparecen desnudos en el diálogo. Alguien que no ha pisado un gimnasio no sabe qué son ni qué poner. | Ayuda bajo cada campo: “cuántas veces repites el bloque; si no lo sabes, deja 3”. | `strings.xml:484-485 · dialog_series_reps.xml:19,39` |
| CRÍTICO | El campo dice “Peso (kg)” sin aclarar de qué peso habla: ¿la barra entera, una mancuerna, las placas? Ambigüedad que corrompe todo el histórico. | “Peso por mancuerna o barra completa” y ayuda para el peso corporal. | `strings.xml:256 · item_ejercicio_peso.xml:47` |
| ALTO | Registrar un entreno de seis ejercicios cuesta unas 33 interacciones, 28 de teclado. La duración no se precarga de la rutina aunque esté disponible, y cada peso se teclea desde cero. | Precargar duración y peso anterior, stepper de ±2,5 kg y botón “repetir última sesión”. | `RegistrarSesionActivity.java:83,218-222` |
| ALTO | La gráfica de progresión se oculta entera con menos de dos registros: el usuario no sabe que existe, así que no tiene motivo para registrar la segunda sesión. | Mostrar siempre la tarjeta, en estado “registra este ejercicio dos veces y verás tu progreso”. | `DetalleEjercicioActivity.java:124-127` |
| ALTO | “PR” y “PROGRESIÓN · PESO MÁX.” son jerga, y el récord solo se explica si tocas exactamente ese punto de la gráfica. | “Tu progreso · peso máximo”, etiqueta permanente “tu récord · 60 kg” y leyenda. | `strings.xml:303-304 · DetalleEjercicioActivity.java:143-156` |
| ALTO | Al guardar la rutina, los ejercicios se envían en peticiones paralelas y el fallo se ignora: si falla la mitad, igualmente dice “guardada correctamente” y queda incompleta en silencio. | Envío agrupado o secuencial, y avisar “se guardaron 4 de 6 ejercicios”. | `ResumenCrearRutinaActivity.java:219-236` |
| ALTO | Los pesos de la sesión se envían justo antes de cerrar la pantalla y el fallo se ignora: pueden perderse sin que nadie se entere, y son el único dato que alimenta la progresión. | Enviar antes de cerrar, con indicador, y reportar el fallo. | `RegistrarSesionActivity.java:283,300-318` |
| ALTO | La descripción y la duración son obligatorias en el primer paso: hay que redactar un texto y adivinar los minutos de una rutina que aún no existe. | Descripción opcional autogenerada y duración calculada en el resumen, editable. | `CrearRutinaActivity.java:81-82` |
| ALTO | En el resumen y en la edición se puede borrar un ejercicio pero no cambiar sus series o repeticiones: para corregir un 3×10 hay que eliminarlo, volver atrás, buscarlo y añadirlo otra vez. | Tocar la fila abre el diálogo precargado. | `ResumenCrearRutinaActivity.java:137-148` |
| ALTO | El músculo y el equipo se pintan crudos desde la API, en inglés: el usuario lee “quadriceps” y “barbell”, mientras el grupo sí se traduce. | Traducir ambos, como ya se hace con nivel y grupo muscular. | `DetalleEjercicioActivity.java:217-228` |
| ALTO | Al abrir una rutina se descarga el catálogo entero de 873 ejercicios solo para resolver los nombres de cinco u ocho filas. Si falla, las filas dicen “Ejercicio 412”. | Que la API devuelva el nombre en la relación, como ya hace para registrar sesión, y borrar la llamada al catálogo. | `DetalleRutinaActivity.java:162-208` |
| ALTO | Primer día en sesiones: pantalla en blanco con “no hay sesiones registradas” y un botón flotante en la esquina. Cero instrucción. | Estado vacío con ilustración y botón grande “registrar mi primer entreno”. | `activity_sesiones.xml:51-60 · strings.xml:239` |
| ALTO | El estado vacío de rutinas está forzado a oculto siempre: si falla la red solo se ve la tarjeta de crear y un aviso efímero, sin explicar por qué no hay rutinas. | Estado de error con reintento, y separar “recomendadas” de “mis rutinas”. | `RutinasFragment.java:179-182` |
| MEDIO | La valoración de la sesión se concatena como texto dentro de las notas y se pinta cruda en el resumen. Se ve a apáño. | Campo propio en la API y estrellas dibujadas en el resumen. | `RegistrarSesionActivity.java:240-244` |
| MEDIO | Tres pantallas encadenadas sin ningún indicador de progreso. | “Paso 1 de 3” en cada barra superior. | `activity_crear_rutina.xml:22 · activity_anadir_ejercicios.xml:21` |
| MEDIO | Unidades y textos concatenados en código (“ ejercicios”, “ min”, “Ejercicio ” + número), que no existen en la traducción inglesa. | Plurales y formatos en recursos, en ambos idiomas. | `RutinaAdapter.java:86-88 · DetalleRutinaActivity.java:113` |
| MEDIO | Botones de eliminar de 36 dp sin descripción, y cinco controles de navegación a 40 dp igualmente sin descripción. | 48 dp reales y descripción en cada uno. | `item_sesion.xml:54-64 · activity_rutinas.xml:44-53` |

### Nutrición

Diario, comida, añadir y crear alimento, estadísticas. 17 hallazgos: 4 crítico · 9 alto · 4 medio.

| Gravedad | Qué está mal | Qué hacer | Dónde |
|---|---|---|---|
| CRÍTICO | La primera vez que se entra en una comida del día, la pantalla aparece completamente en blanco: no se carga nada y el estado vacío tampoco se muestra. Solo hay un botón flotante. | Mostrar el vacío con botón ancho “añadir alimento”; el flotante no basta como única llamada. | `ComidaActivity.java:100-102 · activity_comida.xml:128` |
| CRÍTICO | El diálogo de cantidad no dice qué alimento estás añadiendo: un campo “Cantidad (g)” flotando, justo después de una espera por la importación. | Nombre del alimento como título, con marca y calorías por 100 g debajo. | `AnadirAlimentoActivity.java:394-396` |
| CRÍTICO | Solo se aceptan gramos: no hay ningún concepto de ración ni unidad en el modelo. Nadie pesa un vaso de leche ni media manzana. | Chips de media, una y dos raciones sobre un campo de gramos plegado. Requiere campo nuevo y migración. | `dialog_gramos.xml:13-27 · Alimento.java:15-32` |
| CRÍTICO | Desactivar un ejercicio, una rutina o un alimento del catálogo global es un solo toque sin confirmación. Afecta a todos los usuarios y a las comidas ya registradas. | Confirmación con el nombre del elemento. Los textos ya están escritos en el proyecto y no se usan. | `AdminEjerciciosActivity.java:142 · AdminRutinasActivity.java:156 · AdminAlimentosActivity.java:195` |
| ALTO | Los tres errores de validación de cantidad muestran como mensaje el propio texto de ayuda: sale un aviso que dice “Cantidad (g)”. | Mensajes reales (“escribe cuánto has comido”) y validación en el campo. | `AnadirAlimentoActivity.java:399,406,410` |
| ALTO | El buscador de alimentos no tiene estado inicial ni estado sin resultados: la lista se queda en blanco sin explicar nada. | Sin búsqueda, “tus alimentos recientes”; sin resultados, “no encontramos X” con botón para crearlo. | `activity_anadir_alimento.xml` |
| ALTO | Los títulos dicen “Calorías de hoy” y “Comidas de hoy” y no cambian al navegar a días pasados: viendo el martes pasado la pantalla afirma que son las de hoy. | Actualizar ambos al cambiar de día: “calorías del mar 14”. | `activity_nutricion.xml:130,381` |
| ALTO | Los macros se muestran como cifra desnuda en gramos, sin objetivo al lado. El único aviso visual aparece cuando te pasas: ir corto o ir bien se ven igual. | “128 / 150 g” más una palabra de estado por macro, con el mismo umbral que el mapa de calor. | `NutricionFragment.java:232-239` |
| ALTO | Bajo la barra de calorías solo pone “Objetivo diario”, sin decir si vas bien ni de dónde sale. Y como no es editable, quien no se reconozca en el número no tiene salida. | “Te quedan 640 kcal”, más un icono que explique el cálculo y enlace a perfil. | `activity_nutricion.xml:177-184` |
| ALTO | “Adherencia” es jerga clínica y es un porcentaje sin denominador. Además duplica otro indicador de la misma pantalla. | “Días en tu objetivo”, “3 de 5 días”, y reducir de cuatro indicadores a tres con lectura. | `EstadisticasNutricionActivity.java:176 · strings.xml:309-312` |
| ALTO | Editar y eliminar un alimento propio solo existe en pulsación larga, sin ninguna señal, mientras el resto de la app usa un icono explícito. | Icono de acciones en la fila cuando el alimento es propio. | `AnadirAlimentoActivity.java:109-118` |
| ALTO | Desde una pantalla de uso diario, un administrador puede borrar un alimento del catálogo global, y el diálogo no distingue “de mi lista” de “de toda la app”. | Sacar el borrado global de aquí; que viva solo en el panel de administración. | `AnadirAlimentoActivity.java:175-180` |
| ALTO | Las categorías de alimento solo existen en español: en inglés el desplegable muestra “Carnes y aves”, “Lácteos”. Además el código compara contra el texto visible en español. | Duplicar el recurso en inglés y comparar contra un código estable, no contra la etiqueta. | `arrays.xml:27-39 · CrearAlimentoActivity.java:76` |
| MEDIO | El objetivo por defecto está fijado a 2000 kcal: si el perfil está incompleto, el usuario ve un número inventado presentado con la misma autoridad que uno calculado. | “Completa tu perfil para calcular tu objetivo” con enlace, en vez de una cifra falsa. | `NutricionFragment.java:49` |
| MEDIO | El umbral del ±15% que decide qué días cuentan como “en objetivo” y colorea el mapa de calor no se explica en ninguna parte. | Una línea bajo el título del mapa, y celdas pulsables que lleven al día. | `EstadisticasNutricionActivity.java:165,281` |
| MEDIO | Los totales y la vista previa de macros están escritos a mano en español dentro del código, mientras otra pantalla sí usa recursos. Y una división sin proteger imprime NaN si la cantidad es cero. | Reutilizar el recurso existente en ambos sitios y proteger el divisor. | `ComidaActivity.java:206-209,261-266` |
| MEDIO | El botón de día siguiente desaparece al llegar a hoy: el usuario ve un hueco y no entiende por qué. Ambos chevrones miden 40 dp sin descripción. | Dejarlo deshabilitado en vez de invisible, a 48 dp y con descripción. | `activity_nutricion.xml:76-108 · NutricionFragment.java:122` |

### Administración

Panel y las cuatro listas de gestión. 11 hallazgos: 5 alto · 6 medio.

| Gravedad | Qué está mal | Qué hacer | Dónde |
|---|---|---|---|
| ALTO | El buscador lanza una petición en cada tecla, y cada una abre un diálogo de carga modal: escribir cinco letras son cinco peticiones y cinco parpadeos que roban el foco del teclado. | Retardo de 400 ms, como ya se hace en añadir alimento, e indicador en línea en vez de modal. | `AdminUsuariosActivity.java:104-117` |
| ALTO | Las cuatro listas piden una página fija de 100 sin paginación ni aviso del corte: con 101 usuarios el administrador cree que solo hay 100. | Scroll infinito con el listener que ya existe, más contador en la cabecera. | `AdminUsuariosActivity.java:124 · AdminEjerciciosActivity.java:122` |
| ALTO | Un mismo grupo de filtros mezcla dos ejes distintos (estado y rol) y elegir uno borra el otro en silencio: no se puede ver “administradores inactivos”. | Dos grupos con etiqueta, combinables, y un botón de limpiar. | `AdminUsuariosActivity.java:79-98` |
| ALTO | Ninguna lista de administración tiene estado vacío: filtrar sin resultados da una pantalla en blanco indistinguible de un error de red. | Vacío por lista que distinga “no hay nada” de “no hay nada con estos filtros”. | `los cuatro activity_admin_*.xml` |
| ALTO | El panel usa el mismo tema, color y estilo que la app de usuario, y se entra desde una fila más del perfil: nada avisa de que se están manipulando datos globales. | Banda permanente “modo administrador · los cambios afectan a todos los usuarios”. Es la señal más barata y la que más error evita. | `activity_admin.xml · PerfilFragment.java:334-339` |
| MEDIO | Los desplegables de administración muestran los valores crudos en mayúsculas (“FULLBODY”, “PRINCIPIANTE”) mientras la lista de al lado sí los traduce. | Etiquetas traducidas en el desplegable, enviando el valor interno al guardar. | `EditarEjercicioAdminActivity.java:33-36` |
| MEDIO | Ninguna pantalla de edición detecta cambios sin guardar: el botón de volver cierra directamente y se pierde todo lo escrito. | Comparar estado y confirmar “¿descartar cambios?” al salir. | `EditarEjercicioAdminActivity.java:46 · EditarRutinaAdminActivity.java:42` |
| MEDIO | El diálogo de edición omite del guardado todo campo vacío: quien borre las proteínas para ponerlas a cero no cambia nada y nadie se lo dice. | Interpretar vacío como cero explícito o bloquear con error en el campo. | `AdminAlimentosActivity.java:244-277` |
| MEDIO | “Días semana” es texto libre cuyo formato solo se explica en la ayuda: cualquier error tipográfico entra en la base de datos sin validar. | Siete chips de día que se serializan al guardar. | `EditarRutinaAdminActivity.java:108-109` |
| MEDIO | El diálogo de rol se construye en código con textos fijos, identificadores mágicos y márgenes en píxeles crudos, distintos según la densidad de pantalla. | Layout propio con dp, y una frase de consecuencia: “un administrador puede borrar contenido de toda la app”. | `AdminUsuariosActivity.java:182-188` |
| MEDIO | Los botones de acciones de las filas miden 32 dp, siendo el único control interactivo de cada fila, y sin descripción. | 48 dp de área táctil y descripción con el nombre del elemento. | `item_admin_alimento.xml:51-60 · item_admin_usuario_v2.xml:87-97` |

---

## 6. Rendimiento y deuda técnica

Medido sobre el código, no estimado.

| Asunto | Dato | Dónde |
|---|---|---|
| Coste dominante de la barra | `RenderEffect` nuevo + `setRenderEffect` **por frame** en arrastre, viaje y rebote | `FloatingNavBar.java:370-388` |
| Animadores en reposo | ninguno: los tres se cancelan bien. El coste es por gesto, no en reposo | `FloatingNavBar.java:240-252,333-340,415-429` |
| `findViewById` | **491 en 59 ficheros**. No hay ViewBinding ni DataBinding | todo el proyecto |
| `textSize` fijados a mano | **365 en 60 ficheros** | `res/layout` |
| Jerarquías profundas | perfil 10 niveles, mediciones y home 9 | `activity_perfil.xml`, `activity_mediciones.xml`, `activity_home.xml` |
| Doble medida | `layout_weight` anidado: perfil 14, resumen de sesión 13, onboarding4 13, admin 12 | idem |
| Overdraw | ventana y contenido pintan el mismo color apilado, más tarjetas y círculos encima | `activity_main.xml:11-14`, `activity_home.xml:13` |
| Red por pestaña | las 5 pestañas viven siempre (`offscreenPageLimit(4)`) y dos recargan en cada `onResume` | `MainActivity.java:56`, `HomeFragment.java:69-73`, `NutricionFragment.java:101-105` |
| Bucle de imágenes | `Glide.load()` relanzado cada 700 ms indefinidamente (sí se cancela al destruir) | `DetalleEjercicioActivity.java:184-198` |
| Catálogo completo por rutina | se descargan 873 ejercicios para resolver 5-8 nombres | `DetalleRutinaActivity.java:162-176` |

### Código muerto confirmado

Unas 650 líneas. Nada lo lanza ni lo instancia:

- `RegistrarMedicionActivity` + `activity_registrar_medicion.xml` + su entrada en
  el manifest. Se lleva por delante `NotificationHelper.notificarMedicionGuardada`,
  que ya no se dispara nunca.
- `MedicionAdapter` + `item_medicion.xml` (era el único sitio que pintaba el IMC).
- `item_admin_usuario.xml` (el adapter infla la v2).
- 9 strings huérfanos con sus traducciones: `home_resumen`, `home_entrenamientos`,
  `home_esta_semana`, `home_entrenamientos_semana`, `perfil_configuracion`,
  `perfil_editar_proximamente`, `mediciones_eliminar`,
  `mediciones_confirmar_eliminar`, `grafica_rango_7`.

---

## 7. Plan de trabajo en 6 fases

El criterio del orden no es la gravedad aislada, sino que cada fase apoye a la
siguiente: primero deja de romperse, luego el ciclo del producto funciona, luego
se ve caro, y solo entonces se enseña.

### Fase 1 — Dejar de romperse (1-2 días)

- Los dos crashes: `maxLength` y parseo defensivo en duración, series y
  repeticiones; normalizar la coma decimal en altura y mediciones
- Insets del sistema en una `BaseActivity` común
- El splash comprueba también el onboarding; cada paso se guarda al avanzar
- Fuera el disfraz de proyecto: currículum, mención al Trabajo Final, teléfono
  falso, enlace al hosting gratuito y "Vídeo próximamente"
- Confirmación antes de desactivar ejercicios, rutinas y alimentos del catálogo
  global (los textos ya están escritos y nadie los usa)
- Borrar el código muerto

### Fase 2 — Que el ciclo funcione (3-5 días)

- "Entrenar esta rutina" en el detalle, con la rutina ya seleccionada
- Registrar sesión con precarga (peso anterior, stepper, duración, fecha editable)
- Menú de tres puntos visible en cada rutina y cada alimento propio
- Errores en el campo que falla, no en un aviso genérico: registro, login
  (distinguir sin conexión, bloqueo por intentos y credenciales), crear rutina, admin
- Aviso de la política de contraseña antes de enviar, con los cuatro requisitos
  marcándose al escribir
- Filtro por músculo en la pantalla donde se eligen ejercicios

### Fase 3 — Que se vea caro (4-6 días)

- Barra Ember y retirada del shader por frame
- Naranja unificado en ambos temas, con presupuesto de un acento por pantalla
- Tres niveles de superficie y escala de espaciado base 4, con 20 dp de margen lateral
- Tipografía con ritmo (tracking por tamaño); elimina el parche de escala global
- Objetivos táctiles a 48 dp y descripciones en los controles
- Los cuatro `Spinner` viejos a campos Material

### Fase 4 — El gancho comercial (5-8 días)

- La silueta muscular en Home
- Home reordenado a "hoy"
- El récord en dorado sube a Home como tarjeta propia
- Foto real en las filas del catálogo
- Resumen tras entrenar con jerarquía de celebración: un número grande, no seis iguales

### Fase 5 — Que lo entienda cualquiera (4-6 días)

- Lenguaje de estado en nutrición: "te quedan 640 kcal", "128 / 150 g · vas bien"
- Raciones además de gramos, con recientes y sin cerrar la pantalla al añadir
- Fuera la jerga: adherencia, PR, core, déficit y superávit calórico, series y
  repeticiones sin explicar, niveles de actividad sin definir
- Perfil partido y ajustes rescatados del icono de tres puntos
- Mediciones con fecha explícita, confirmación al guardar, IMC interpretado y
  ayuda de cómo medir cada perímetro
- Estados vacíos que enseñan (hoy uno manda pulsar un botón que se eliminó)

### Fase 6 — Onboarding y tienda (4-6 días)

- Onboarding refundido en un solo flujo con estado guardado: objetivo primero,
  datos y medidas fusionados, nivel, y el resumen nutricional fijado en Home en
  vez de mostrarse y tirarse
- Las seis capturas de la ficha
- Legal: privacidad, términos y declaración de datos. **Bloquea publicar**

### Aplazado a propósito

- **ViewBinding** (491 `findViewById`) y profundidad de layouts: deuda real, pero
  migrarlos ahora congela semanas sin que el usuario note nada. Se hacen pantalla
  por pantalla, aprovechando que las fases 3 y 5 ya reescriben esos archivos.
- **Modo de entreno en vivo** (serie a serie, cronómetro): es la ausencia que más
  delata a la app frente a Hevy, pero cambia el modelo de datos y no tiene
  sentido construirlo sobre un flujo de registro que todavía cuesta 33 toques.
- **Estética del panel de admin**: solo necesita la fase 1 (confirmaciones,
  debounce en los buscadores y un banner de contexto). No sale en ninguna captura.

---

## 8. Ficha de Play Store

Las seis capturas, en orden. Las marcadas requieren construir algo antes.

1. **Home con el cuerpo encendido**, medio pintado (un cuerpo lleno no genera
   deseo). *No existe: es la fase 4.*
2. **Detalle de ejercicio** con los dos fotogramas alternándose, silueta del
   músculo y progresión con el récord dorado. *Existe; hay que borrar antes el
   "Vídeo próximamente".*
3. **Catálogo** con buscador activo, chips de filtro y el contador "873
   ejercicios" visible. *El contador no se muestra hoy en ningún sitio: 873 es un
   número de venta y está escondido.*
4. **Nutrición, día lleno**: calorías, macros y las cinco comidas con alimentos
   reales. *Sale bien tal cual.*
5. **Mapa de calor de adherencia + barras de calorías**. *Es la captura más
   "producto serio" que ya está montada.*
6. **Resumen post-sesión** con racha, minutos, calorías y un logro desbloqueado.
   *Existe, pero necesita jerarquía de celebración.*

### Identidad

MyFitnessPal y Fitia venden listas de comida. Hevy vende tablas de series. Strava
vende mapas y gente. Las cuatro enseñan en portada una retícula de texto y
números.

GymProFit debe enseñar **una figura humana naranja sobre carbón**: es lo único
que en una miniatura de Play a 100 px de ancho se reconoce como forma y no como
texto. Lo demás —la Barlow Condensed, los números XXL, el récord dorado— sostiene
ese gesto pero no lo sustituye. Si en la primera captura hay un anillo de
calorías, somos Fitia en oscuro.
