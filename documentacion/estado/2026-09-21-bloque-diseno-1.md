# Bloque de diseño 1 — GP-059, GP-060 y GP-061

Informe de los tres primeros arreglos salidos de
[`AUDITORIA-DISENO-2026-09.md`](../AUDITORIA-DISENO-2026-09.md). Ejecutado el
2026-09-23 sobre el emulador `Medium_Phone_API_36.1` (1080 × 2400) con el
backend de desarrollo en `localhost:8080` y la app apuntando a `10.0.2.2:8080`
(distintivo `PRUEBAS · LOCAL` visible en todas las capturas).

Capturas en [`bloque-diseno-1/`](bloque-diseno-1/).

---

## GP-059 · La barra flotante tapa contenido en las cinco pestañas

Corresponde a **T-01** de la auditoría, y a **P-15** de la pestaña de Rutinas.

### Qué decía el hallazgo

Que la `FloatingNavBar` ocupa 86 dp (72 de alto + 14 de margen inferior), que los
cinco contenedores reservan como mucho 16 dp de `paddingBottom`, y que por tanto
queda contenido inalcanzable: «Registrar comida» en Home, la última tarjeta en
Rutinas, el último ejercicio en Ejercicios, «Comida» y «Merienda» en Nutrición y
«Mis logros» en Perfil.

### Qué se ha medido antes de tocar nada

**El solape no se reproduce en ejecución.** Se instaló el APK de depuración del
2026-09-21 17:59 —el mismo con el que se tomaron las capturas de la auditoría— y
se llevó cada pestaña **al final de su desplazamiento**:

| Pestaña | Último elemento con la lista al final | ¿Tapado? |
|---|---|---|
| Home | Tarjetas «Entrenos / Calorías / Minutos» | No |
| Rutinas | Tarjeta «HIIT Avanzado» | No |
| Nutrición | Fila «Cena» | No |
| Perfil | Botón «Eliminar cuenta» | No |

El motivo es que el `paddingBottom` de los layouts **no es el que manda**:
`BaseFragment.onViewCreated` ya buscaba el primer scroller vertical del árbol y
le sumaba una reserva en ejecución desde el commit `8f64cb7` (2026-07-09), el
mismo que trajo la barra liquid glass. La auditoría midió el XML, donde esa reserva no se ve,
y las capturas que cita están tomadas **a media lista**, no al final: lo que en
ellas se ve bajo el cristal es el `clipToPadding="false"` haciendo su trabajo
—dibujar el contenido difuminado mientras pasa por debajo—, no contenido
inalcanzable.

Corrección que esto obliga a hacer en la auditoría: **T-01 y P-15 están mal
medidos**. El defecto que sí existía es otro, y es el que el encargo señalaba.

### El defecto real: la reserva estaba escrita a mano

```java
int reserva = Math.round(96 * getResources().getDisplayMetrics().density);
```

Un 96 a pelo dentro de `BaseFragment`, sin relación declarada con los `72dp` y
`14dp` que `activity_main.xml` escribía también a pelo. Tres números sueltos que
describen la misma cosa: cambiar el alto de la barra dejaba la reserva corta —o
sobrada— sin que nada avisara. Es exactamente el problema que el encargo pedía
evitar, solo que repartido entre dos ficheros en lugar de cinco.

### Qué se ha hecho

Una sola fuente para las medidas de la barra, en `res/values/dimens.xml`:

```xml
<dimen name="nav_bar_height">72dp</dimen>
<dimen name="nav_bar_margin">14dp</dimen>
<dimen name="nav_bar_content_gap">12dp</dimen>
```

- `activity_main.xml` dibuja la barra con `@dimen/nav_bar_height` y
  `@dimen/nav_bar_margin`. Ya no hay ningún `dp` escrito a mano en la barra.
- `FloatingNavBar.espacioReservado(Context)` devuelve la suma de las tres: el
  alto, el margen y el aire que separa el último elemento del cristal. Es el
  único sitio donde se calcula ese hueco.
- `BaseFragment` lo llama en lugar del 96. Se marca el scroller con
  `R.id.tag_hueco_barra_reservado` para no sumar la reserva dos veces si la
  vista se reutiliza.

Cambiar `nav_bar_height` mueve ahora la barra **y** el hueco de las cinco
pestañas a la vez, que era el requisito.

La reserva pasa de 96 dp a 98 dp (72 + 14 + 12). La diferencia es irrelevante en
pantalla; lo que cambia es de dónde sale el número.

### Por qué no se ha tocado `clipToPadding`

Porque no es el arreglo y tampoco estorba: el sitio lo reserva el `padding`, y
`clipToPadding="false"` es lo que deja que el contenido siga viéndose difuminado
bajo el cristal mientras se desplaza, que es el efecto buscado. Se ha dejado
escrito en el comentario de `BaseFragment` para que no se confunda otra vez.

### Verificación

APK reconstruido e instalado. Las cinco pestañas llevadas al final del
desplazamiento, con la cuenta `admin` (tiene las seis rutinas predefinidas y las
cuatro filas del perfil, incluido el panel de administración):

| Pestaña | Captura | Último elemento, entero y pulsable |
|---|---|---|
| Home | `gp059-home.png` | Fila «Entrenos / Calorías / Minutos» |
| Rutinas | `gp059-rutinas.png` | Tarjeta «HIIT Avanzado» |
| Ejercicios | `gp059-ejercicios.png` | «Zottman Preacher Curl», último de los 873 |
| Nutrición | `gp059-nutricion.png` | Fila «Cena» |
| Perfil | `gp059-perfil.png` | Botón «Eliminar cuenta» |

Ejercicios se recorrió entero hasta el final real de la lista (350 gestos de
desplazamiento, de «3/4 Sit-Up» a «Zottman Preacher Curl»), no hasta un punto
intermedio.

La reserva es geométrica: no depende del idioma ni del tema, así que no hay
cuatro combinaciones que comprobar aquí.

---

## GP-060 · «Mis rutinas» enseña seis rutinas ajenas a quien no tiene ninguna

Corresponde a **P-14** de la auditoría.

### Qué pasaba

`cargarRutinas()` pedía **siempre** las predefinidas y, encima, añadía las del
usuario. Con una cuenta recién creada la lista traía seis rutinas del catálogo
bajo el rótulo «Mis rutinas», que no son suyas, y el `tvEmpty` del layout era
inalcanzable porque la lista nunca llegaba vacía.

### Qué se ha hecho

Las predefinidas dejan de mezclarse con las propias:

1. Se piden **primero las propias**. Si hay alguna, la pantalla muestra solo esas
   —que es lo único que «Mis rutinas» puede prometer.
2. Si no hay ninguna, se piden las predefinidas y se muestran **dentro del estado
   vacío**: un bloque con «Aún no tienes rutinas propias», la invitación a crear
   una, y el rótulo **«Rutinas para empezar»** que atribuye las tarjetas de debajo
   a GymProFit y no al usuario.
3. El bloque es un ítem del `ConcatAdapter` (`RutinasVacioHeaderAdapter`,
   `item_rutinas_vacio.xml`), entre la tarjeta «+ Nueva rutina» y el listado, para
   que se desplace con la lista en lugar de flotar encima.

Un invitado (`usuarioId == -1`) entra directo por el camino 2: no tiene rutinas
propias que pedir, pero sigue viendo por dónde se empieza.

### El 404 que estaba escondido debajo

Al probarlo, la cuenta `vacia` seguía sin ver nada. El motivo no estaba en la
pantalla: **la API responde `404` a una lista vacía**, no `200` con `[]`.

```
GET /api/rutinas/usuario/5080/activas  →  404
```

Es deliberado —`RutinaController#obtenerRutinasActivasPorUsuario` lanza
`NotFoundEntityException` cuando la consulta no devuelve filas— y el contrato no
se toca desde aquí (CLAUDE.md: hay builds repartidas fuera de Play que lo
consumen). Así que **la pantalla lo traduce**: en estas dos llamadas un `404` no
es un fallo, es el caso vacío, y no genera aviso al usuario. Cualquier otro
código sí se avisa, y entonces la lista se deja vacía en vez de enseñar
predefinidas que podrían estar tapando las del usuario.

Esto explica también por qué el código anterior mezclaba: pedía las predefinidas
primero justamente porque las propias «fallaban» cuando no había ninguna.

### `tvEmpty` deja de ser código muerto

Ahora es alcanzable, y dice dos cosas distintas según el caso:

- Filtro de nivel sin resultados → «No hay rutinas de este nivel»
  (`rutinas_vacio_filtro`).
- Lista vacía sin filtro (solo ocurre tras un fallo de red) → «No hay nada aún»
  (`feedback_lista_vacia`).

### Consecuencia que conviene tener presente

Una cuenta **con** rutinas propias ya no ve las predefinidas en esta pantalla.
Es lo que se sigue de la decisión —las predefinidas son sugerencia de arranque—,
y para el administrador no hay pérdida porque las gestiona desde el panel de
administración. Si algún día se quiere un acceso permanente al catálogo de
rutinas, es una sección aparte, no un mezclado bajo «Mis rutinas».

### Verificación

| Caso | Captura | Resultado |
|---|---|---|
| Cuenta `vacia`, ES, oscuro | `gp060-vacia-es-oscuro.png` | Estado vacío + «RUTINAS PARA EMPEZAR» sobre las seis predefinidas |
| Cuenta `vacia`, EN, claro | `gp060-vacia-en-claro.png` | Igual, traducido y con el tema claro |
| Cuenta `prueba` (2 rutinas propias), ES, oscuro | `gp060-prueba-es-oscuro.png` | Solo sus dos rutinas; ni bloque vacío ni predefinidas |
| Filtro «Avanzado» sin resultados, ES, oscuro | `gp060-filtro-es-oscuro.png` | `tvEmpty` visible con «No hay rutinas de este nivel» |

Cadenas nuevas en `values/strings.xml` y `values-en/strings.xml`. Los dos títulos
del bloque llevan `accessibilityHeading="true"`.

---

## GP-061 · Terminar el sistema de iconos

Corresponde a **T-07** de la auditoría.

### La pregunta que había que responder antes: ¿tienen código estable los logros?

**Sí, y no hace falta migración.** La tabla `logros` no tiene columna de icono,
pero sí tiene `tipo`, un `TipoLogro` (enum de la API) con un valor distinto por
logro y **presente en `LogroDTO`**:

| `tipo` | Logro |
|---|---|
| `PRIMERA_SESION` | Primera sesión |
| `CONSTANCIA` | Constancia (7 sesiones) |
| `DEDICADO` | Dedicado (30 sesiones) |
| `CENTENARIO` | Centenario (100 ejercicios) |
| `OBJETIVO_CUMPLIDO` | Objetivo cumplido |
| `MAQUINA` | Máquina (10 objetivos) |

Es estable (es un enum, no un autoincremento) y llega al cliente, así que el mapa
vive en la app y la base de datos no se toca.

De paso aparece la causa real del trofeo repetido: `LogroAdapter` leía
`logro.getIcono()`, y **ese campo no existe en `LogroDTO`** —ni en la tabla—, así
que siempre era `null` y el literal de respaldo acababa siendo el icono de los
seis. No era un icono por defecto mal elegido: era el único camino que había.

### Qué se ha hecho

**Diez iconos nuevos, todos `VectorDrawable` de 24 teñidos por `tint`, un archivo
para los dos temas**, extendiendo el criterio de `59ea6f4` (`ic_objetivo_*`).

| Dónde | Antes | Ahora |
|---|---|---|
| Onboarding paso 5, cuatro niveles | 🌱 💪 🏆 ⚡ | `ic_nivel_principiante/intermedio/avanzado/experto` |
| Resumen del onboarding, agua | 💧 | `ic_agua` |
| Los seis logros | el mismo 🏆 | `ic_logro_*`, uno por `tipo`, más `ic_logro` de respaldo |
| `sesiones_valoracion_fmt` | «⭐ %d/5» | «Valoración: %d/5» |
| `resumen_completada` | «✓ Completada» | «Completada» + `ic_check` como `drawableStart` |

**Los cuatro niveles son un medidor, no cuatro dibujos.** Cuatro barras
ascendentes y cambia cuántas van rellenas. Lo que la pantalla pregunta es una
**posición en una escala**, y con cuatro metáforas sueltas (brote, brazo, trofeo,
rayo) no se puede comparar una tarjeta con la de al lado. Además el rayo ya es
`ic_objetivo_fuerza` **en el paso anterior**: repetirlo aquí confundiría dos
preguntas seguidas.

**El de «Objetivo cumplido» se rehízo.** El primer intento era la diana con una
marca de verificación encima, y a 34 dp los dos trazos se cruzaban con los aros y
se leía como un garabato. Es ahora una diana con un dardo clavado, que entra desde
fuera y no pisa ningún aro.

### Dos símbolos más, fuera del inventario de los diez

`medicion_mas_detalles` y `medicion_menos_detalles` llevaban **▾ y ▴ dentro de la
cadena traducible**, en ES y en EN. No son emojis y por eso no estaban en T-07,
pero son exactamente el caso que el encargo llamaba el peor: un símbolo que viaja
en el fichero de idioma, que no se tiñe con el tema y que hay que acordarse de
arrastrar a cada traducción. Se han sacado a `ic_chevron_down` / `ic_chevron_up`,
que `MedicionesActivity` cambia al plegar y desplegar.

**Es alcance añadido por decisión propia**, y se señala por si no interesa: son
dos cadenas y dos vectores, revertibles por separado.

### Lo que NO se ha tocado y por qué

- Las banderas del diálogo de idioma ya eran vectores (`ic_flag_es`,
  `ic_flag_en`). Parecen emojis en la captura y no lo son.
- Los caracteres `─` `→` `≥` que quedan en el repositorio están **solo en
  comentarios** de código y de recursos. No se ven en pantalla.

### Verificación

| Pantalla | Captura | Qué se comprueba |
|---|---|---|
| Onboarding paso 4 (ES, oscuro) | `gp061-onboarding4-es-oscuro.png` | El sistema que se extiende, con su rayo |
| Onboarding paso 5 (ES, oscuro) | `gp061-onboarding5-es-oscuro.png` | Los cuatro medidores, 1 a 4 barras |
| Resumen del onboarding (ES, oscuro) | `gp061-resumen-onb-es-oscuro.png` | La gota, teñida de color de marca |
| Logros (ES, oscuro) | `gp061-logros-es-oscuro.png` | Los seis distintos |
| Logros (EN, claro) | `gp061-logros-en-claro.png` | Los seis distintos, tema claro |
| Resumen de sesión (ES, oscuro) | `gp061-resumen-sesion-es-oscuro.png` | «Valoración: 3/5» en las notas, «Completada» con vector, y el logro recién desbloqueado con SU icono |
| Mediciones (ES, oscuro) | `gp061-mediciones-es-oscuro.png` | Galón hacia arriba al desplegar |
| Mediciones (ES, claro) | `gp061-mediciones-es-claro.png` | El mismo galón teñido en claro |

Un barrido final del árbol de `res/` y `java/` no deja **ningún emoji en layout,
código ni cadena**: los únicos caracteres fuera de ASCII que quedan son de
comentarios.

### Rastro en la base de datos de pruebas

Para recorrer el onboarding hizo falta una cuenta nueva: **`iconos61` /
`Iconos1234.`** (email `iconos61@local.test`), nivel Experto, objetivo ganar
músculo. Queda en la base local del 3308; bórrala si estorba. La cuenta `prueba`
tiene **una sesión de 45 min más**, la que se registró para ver el badge
«Completada».
