# Bloque de diseño 2b — GP-084, retoque de GP-079, GP-080, GP-082 y GP-081

Cinco tareas en el orden pedido. La última, GP-081, **acaba en una parada**: el
fondo del icono de la app lo decide el propietario. Aquí están las dos versiones.

Ejecutado el 2026-09-24. Emulador `Medium_Phone_API_36.1` contra la API local.
Todo se comprobó en ejecución. Láminas en [`bloque-diseno-2b/`](bloque-diseno-2b/):
una fila por estado y una columna por combinación ES/EN × claro/oscuro. Las
capturas originales se quedan fuera del repositorio por tamaño.

| Commit | Qué |
|---|---|
| `170c7f2` | GP-084 · el tema oscuro deja de tener otra tipografía |
| `3c528ea` | GP-079 · el logro conseguido va en dorado, no en naranja |
| `3ef0e23` | GP-080 · un solo sistema de iconos en toda la app |
| `d936bf1` | GP-082 · pantalla de licencias de terceros |
| `9157b6b` | GP-081 · **en la rama `gp-081-icono-pendiente`, no en `main`**: espera la decisión del fondo |

Nada de este bloque tocó la API. Tests de la app: 40 en verde. `IconosTest` es
nuevo, y `LogroMedallonTest` se ajustó al dorado.

---

## GP-084 · El tema oscuro tenía otra tipografía

`values-night/themes.xml` redefinía el tema entero, 45 atributos, y se había
separado del claro. Seguía poniendo **Barlow Condensed como fuente de todo el
texto**, que `ad0eaf0` quitó del claro a propósito, y **no tenía
`windowAnimationStyle`**.

Queda **una sola definición del tema**, en `values/themes.xml`. Lo único que no
era un color, `windowLightStatusBar`, pasa a un `bool` con qualifier
(`values/bools.xml` y `values-night/bools.xml`). Se borra
`values-night/themes.xml`.

Tres pantallas con texto de cuerpo (ficha de ejercicio, perfil y buscador de
alimentos):

![Ficha de ejercicio](bloque-diseno-2b/gp084-detalle-ejercicio.jpg)
![Perfil](bloque-diseno-2b/gp084-perfil.jpg)
![Añadir alimento](bloque-diseno-2b/gp084-anadir.jpg)

**Transiciones.** Con las animaciones del sistema a ×10 se capturó el paso a
mitad de abrir una ficha. Orden de la lámina: antes claro, antes oscuro, después
claro, después oscuro. Antes, en claro la pantalla anterior asomaba por la
izquierda (el deslizamiento de la app) y en oscuro no (la animación por defecto
del sistema). Después, los dos temas enseñan el mismo fotograma.

![Transición a mitad](bloque-diseno-2b/gp084-transicion.jpg)

---

## GP-079 · El logro conseguido va en dorado

DEC-018 reserva el dorado para récords y logros. El medallón conseguido pasa a un
contenedor dorado nuevo, `gp_gold_container`, con el glifo en `gp_gold` y
«Conseguido · fecha» en `gp_gold_text`. La barra de progreso sigue en naranja: lo
que está en curso es marca, lo conseguido es oro.

| Glifo contra contenedor | Claro | Oscuro |
|---|---|---|
| `gp_gold` / `gp_gold_container` | **3,10:1** (`#AE8528` / `#FFF4D9`) | 7,21:1 (`#E8B84B` / `#3A2E12`) |

Fijado en `LogroMedallonTest`. En claro el contenedor es muy pálido **a la
fuerza**: `gp_gold` tiene que dar 3:1 también sobre el fondo de la app (GP-063), y
con ese dorado no cabe un contenedor más saturado que siga dando 3:1.

![Logros: conseguido en dorado](bloque-diseno-2b/gp079-logro-dorado.jpg)

---

## GP-080 · Un solo sistema de iconos

Todo icono de interfaz es **Material Symbols Rounded, peso 400**, importado sin
redibujar de `@material-symbols/svg-400@0.47.5` (carpeta `rounded`, variantes
`-fill`). El método es el de GP-079: `<group android:translateY="960">` para el
`viewBox 0 -960 960 960`, y el nombre `ic_ms_<símbolo>`. **El relleno es el
estado**: contorno en reposo y relleno en lo activo (la pestaña de la barra de
navegación y el objetivo elegido en el onboarding).

### Icono viejo → símbolo nuevo

| Viejo | Estilo viejo (según el enunciado y el trazo del XML) | Nuevo | Dónde |
|---|---|---|---|
| `ic_add` | relleno clásico | `add` | FAB y botones de añadir |
| `ic_agua` | a mano, trazo 1,9 | `water_drop` | agua del resumen del onboarding |
| `ic_altura` | relleno clásico | `height` | perfil, onboarding 3 |
| `ic_arrow_back` | relleno clásico | `arrow_back` | 27 pantallas |
| `ic_calorias` | relleno clásico | `local_fire_department` | racha de inicio |
| `ic_check` | relleno clásico | `check` | selecciones, series, diálogos |
| `ic_check` en «Mis logros» | — | **`trophy`** | el check no decía «logros» |
| `ic_chevron_down` / `_up` | a mano, trazo 1,9 | `keyboard_arrow_down` / `_up` | mediciones |
| `ic_chevron_right` | relleno clásico | `chevron_right` | filas navegables |
| `ic_close` | relleno clásico | `close` | quitar ejercicio |
| `ic_delete` | MDI | `delete` | borrar |
| `ic_edad` | relleno clásico | `cake` | perfil, onboarding 2 |
| `ic_edit` | relleno clásico | `edit` | editar |
| `ic_ejercicios` | relleno clásico | `fitness_center` (+ `_fill`) | barra, inicio, rutinas |
| `ic_email` | relleno clásico | `mail` | perfil, registro, contacto |
| `ic_error` | relleno clásico | `error` | diálogos de error |
| `ic_home` | relleno clásico | `home` (+ `_fill`) | barra |
| `ic_info` | relleno clásico | `info` | diálogos, ficha |
| `ic_language` | relleno clásico | `language` | idioma |
| `ic_lock` | relleno clásico | `lock` | contraseñas |
| `ic_logo_gym` | relleno clásico: el glifo `fitness_center` | **`info`** | botón «Sobre GymProFit» |
| `ic_logout` | relleno clásico | `logout` | cerrar sesión |
| `ic_moon` / `ic_sun` | relleno clásico | `dark_mode` / `light_mode` | tema |
| `ic_more_vert` | relleno clásico | `more_vert` | menús |
| `ic_nivel` | relleno clásico: barras | **`stairs`** | perfil, ficha de ejercicio |
| `ic_nutricion` | relleno clásico | `restaurant` (+ `_fill`) | barra, inicio |
| `ic_objetivo` | relleno clásico | `target` | perfil, resumen |
| `ic_objetivo_perder_grasa` | a mano, trazo 2,0 | `trending_down` (+ `_fill`) | onboarding 4 |
| `ic_objetivo_ganar_musculo` | a mano, trazo 2,0 | `trending_up` (+ `_fill`) | onboarding 4 |
| `ic_objetivo_fuerza` | a mano, trazo 2,0 | `bolt` (+ `_fill`) | onboarding 4 |
| `ic_objetivo_mantener` | a mano, trazo 2,0 | `balance` (+ `_fill`) | onboarding 4 |
| `ic_palette` | relleno clásico | `palette` | tema |
| `ic_perfil` | relleno clásico | `person` (+ `_fill`) | barra, perfil |
| `ic_peso` | relleno clásico | `monitor_weight` | perfil, mediciones |
| `ic_rutinas` | relleno clásico | `assignment` (+ `_fill`) | barra, inicio |
| `ic_search` | relleno clásico | `search` | buscadores |
| `ic_share` | relleno clásico | `share` | compartir |
| `ic_stats` | relleno clásico | `bar_chart` | historial de nutrición |
| `ic_tiempo` | relleno clásico | `schedule` | duraciones |
| `ic_trofeo` | relleno clásico | `trophy` | tarjeta de récord |
| `ic_visibility_off` | relleno clásico | `visibility_off` | ocultar / desactivar |
| `ic_web` | relleno clásico | **`open_in_new`** | enlaces a la web (privacidad) |
| `ic_phone` | relleno clásico | **borrado** | no lo usaba nadie |

Iconos por defecto de los componentes, donde el componente deja cambiarlos:

| Componente | Antes | Ahora |
|---|---|---|
| Ojo de la contraseña (`TextInputLayout`, 5 campos) | relleno clásico | `sel_ojo_password`: `visibility` / `visibility_off` |
| Aviso de error de los campos (59 `TextInputLayout`) | relleno clásico | `error` |
| Flecha de los `Spinner` (8) | AppCompat | `keyboard_arrow_down`, por `Widget.GymProFit.Spinner` |
| Check de los chips de filtro | Material Components | `check`, en `Widget.GymProFit.Chip` |
| Lupa y aspa del buscador de administración | AppCompat | `search` y `close`, por `Widget.GymProFit.SearchView` |

**Decisiones que había que justificar:**

- **`ic_nivel` → `stairs`.** Tenía que decir «nivel», no «señal» ni «estadística».
  El viejo era un gráfico de barras, que dice «estadística». `stairs` es una
  escalera: niveles que se suben de uno en uno, de principiante a experto. Se
  descartaron `military_tech` y `workspace_premium` porque una medalla ya es el
  lenguaje de los logros (y el nivel no se gana, se declara), y `trending_up`
  porque ya es «ganar músculo» en el paso anterior.
- **`ic_logo_gym` → `info`.** No era el logo: era el glifo `fitness_center`
  puesto en el botón «Sobre GymProFit». Lo que representa ahí es «información de
  la app», y se le quita ese nombre.
- **`ic_web` → `open_in_new`** y no `public` o `language`: el globo ya es el
  idioma, y lo que hace el enlace es abrir fuera.
- **`expand_more` / `expand_less`** no existen con ese nombre en el paquete de
  npm: son alias de `keyboard_arrow_down` / `keyboard_arrow_up`, el mismo glifo.
  Se importan por su nombre canónico.
- **Color por defecto.** Cada símbolo hereda el color por defecto del icono que
  sustituye (`colorError` la papelera, `colorPrimary` el chevron…), así que un uso
  sin tinte se ve del mismo color que antes. Donde había tinte, manda el tinte.

**Borrados:** los 43 iconos viejos, `ic_phone` y los **cinco menús de `res/menu/`**,
que tampoco se inflaban (los menús se construyen en código con `UIHelper`) y solo
servían para referenciar iconos viejos.

**Excepciones que quedan**, todas decididas:

- Fuera de alcance por tu decisión: las banderas `ic_flag_*`, las ilustraciones
  `ic_body_*`, `ic_musculo_*` (que `SiluetaMuscularView` carga por nombre) e
  `ic_silueta_*`, el logo `logo.png` y el medidor `ic_nivel_*` de GP-078.
- **Las estrellas del `RatingBar`** (3 usos) siguen siendo las de AppCompat. El
  componente reparte una imagen por estrella solo si es un bitmap: con un vector
  estira una sola estrella a todo el ancho. Cambiarlas pide un control propio o
  rasterizar el símbolo, y se deja para cuando se decida.

`IconosTest` falla si aparece un `ic_*` fuera de la familia, sin contar las
excepciones decididas, o un `ic_ms_*` que no lleve la marca de importación.

**contentDescription.** Ninguno cambió: la migración solo tocó recursos
gráficos. El nombre accesible del ojo de la contraseña es el de Material, igual
que antes.

Detalle ampliado (barra de navegación, ficha, acciones rápidas y filas del perfil):

![Detalle de iconos](bloque-diseno-2b/gp080-detalle-iconos.jpg)

Ojo de la contraseña (oculta y a la vista) y aviso de error de un campo:

![Componentes](bloque-diseno-2b/gp080-componentes.jpg)

Pantallas, antes y después:

![Inicio](bloque-diseno-2b/gp080-inicio.jpg)
![Inicio, abajo](bloque-diseno-2b/gp080-inicio-abajo.jpg)
![Rutinas](bloque-diseno-2b/gp080-rutinas.jpg)
![Detalle de rutina](bloque-diseno-2b/gp080-detalle-rutina.jpg)
![Ejercicios](bloque-diseno-2b/gp080-ejercicios.jpg)
![Ficha de ejercicio](bloque-diseno-2b/gp080-detalle-ejercicio.jpg)
![Nutrición](bloque-diseno-2b/gp080-nutricion.jpg)
![Menú contextual](bloque-diseno-2b/gp080-menu-contextual.jpg)
![Añadir alimento](bloque-diseno-2b/gp080-anadir.jpg)
![Perfil](bloque-diseno-2b/gp080-perfil.jpg)
![Menú de opciones](bloque-diseno-2b/gp080-menu-opciones.jpg)
![Perfil, abajo](bloque-diseno-2b/gp080-perfil-abajo.jpg)
![Mediciones](bloque-diseno-2b/gp080-mediciones.jpg)
![Acerca de](bloque-diseno-2b/gp080-acerca-de.jpg)
![Eliminar cuenta](bloque-diseno-2b/gp080-eliminar-cuenta.jpg)
![Registrar sesión](bloque-diseno-2b/gp080-registrar-sesion.jpg)
![Login](bloque-diseno-2b/gp080-login.jpg)
![Registro](bloque-diseno-2b/gp080-registro.jpg)
![Onboarding 2](bloque-diseno-2b/gp080-onboarding2.jpg)
![Onboarding 3](bloque-diseno-2b/gp080-onboarding3.jpg)
![Onboarding 4](bloque-diseno-2b/gp080-onboarding4.jpg)
![Resumen del onboarding](bloque-diseno-2b/gp080-onboarding-resumen.jpg)

Administración (sin rediseñar, solo para que no quede ningún icono viejo):

![Panel](bloque-diseno-2b/gp080-admin.jpg)
![Alimentos](bloque-diseno-2b/gp080-admin-alimentos.jpg)
![Ejercicios](bloque-diseno-2b/gp080-admin-ejercicios.jpg)
![Editar ejercicio](bloque-diseno-2b/gp080-admin-editar-ejercicio.jpg)
![Editar rutina](bloque-diseno-2b/gp080-admin-editar-rutina.jpg)

---

## GP-082 · Pantalla de licencias de terceros

- **`play-services-oss-licenses`** genera la lista desde las dependencias de la
  compilación: **167 entradas en release**, entre ellas Retrofit, OkHttp, Gson,
  Material Components, Glide, Firebase y AndroidX. Se abre desde Acerca de, en una
  tarjeta nueva, con título propio en ES y EN.
- **Se fija la 17.3.0 y no la última.** Desde la 17.4.0 arrastra Material 1.13,
  que saca `colorPrimary` y compañía de `material.R.attr` y no compila. Subir
  Material no puede ser un efecto secundario de una pantalla de licencias.
- **A mano, con su texto completo**, lo que el plugin no ve: **Material
  Symbols** (Apache 2.0) y **Barlow** (SIL OFL 1.1, texto del repositorio del
  proyecto), que viajan como recursos, y **MPAndroidChart** (Apache 2.0), que
  viene de JitPack con un POM sin licencia declarada. **Sin esta comprobación
  habría faltado**: no está en las 167.
- Las dos pantallas del plugin necesitan ActionBar, que el tema de la app no
  tiene. Van con `Theme.GymProFit.ConBarra` y la barra en `colorSurface` (la
  primera captura la sacó en el lila por defecto de Material 3).
- El texto legal se desplaza en horizontal: viene cortado a 80 columnas, y al
  reflotar se partía en líneas sueltas.
- Para las dependencias de Maven, el detalle del plugin muestra el **enlace** a la
  licencia, no su texto: es lo que guarda el plugin. Para Apache 2.0, el texto
  completo está además en la entrada manual.

**Cómo se verificó la lista real.** En variantes depurables el plugin solo pone
una entrada, «Debug License Info». Se compiló una variante temporal **no
depurable**, con la URL local y la telemetría apagada como en `debug`. No se ha
commiteado. En esa variante no funciona `run-as`, así que el tema se cambió desde
el menú de la app.

![Acerca de: la tarjeta nueva](bloque-diseno-2b/gp082-acerca-de.jpg)
![Lista, detalle y textos](bloque-diseno-2b/gp082-licencias.jpg)

---

## GP-081 · El icono de la app y el de las notificaciones — PARADO

> **Cerrado después, el mismo día:** el propietario eligió la **A**. Regenerado con el
> script, fusionado en `main` como `7ac8e8a` y borrada la rama.

**Está hecho y verificado, salvo el fondo, que te toca a ti.** Vive en la rama
**`gp-081-icono-pendiente`** (`9157b6b`, versión B), fuera de `main`.

**Qué hay:**

- Icono adaptativo con el logo real **dentro de la zona segura de 66 dp sobre
  108**. El logo se coloca por su **círculo mínimo**, no por su caja: la G es casi
  redonda, y por la caja quedaría más pequeño de lo necesario. Ocupa 64 dp de 66,
  con 1 dp de margen.
- Capa monocroma con la silueta del logo, para el icono temático.
- Icono pequeño de notificación: la misma silueta, blanca sobre transparente, en
  las cuatro notificaciones de `NotificationHelper` y con el color de marca.
- **Hallazgo:** la push de la API lleva bloque `notification`. Con la app en
  segundo plano la pinta el sistema, sin pasar por `NotificationHelper`, así que
  hacía falta también `default_notification_icon` en el manifiesto. Sin él,
  Android usa el icono de la app, que en la barra de estado es un borrón. Se
  resolvió en la app: la API no se tocó.
- La plantilla de Android Studio (el robot verde sobre `#3DDC84` y los `.webp`)
  se borra.
- `scripts/generar-icono-app.py` genera todo desde `logo.png`: el adaptativo, el
  monocromo, los heredados de API 24-25, el pequeño de notificación y el PNG de
  512 para Play.

**Las dos versiones.** A: fondo `#0B0C0E` con el logo de la variante oscura (G
dorada, trazo naranja). B: fondo blanco con el logo de la variante clara (G
naranja, trazo dorado).

Cajón de aplicaciones: antes, A y B. Antes, el launcher metía el PNG en un
círculo blanco de compatibilidad y el logo quedaba pequeño.

![Cajón: antes, A y B](bloque-diseno-2b/gp081-cajon-antes-A-B.jpg)

**Las cinco máscaras del launcher y el icono temático**, desde la pantalla de
personalización del propio launcher. Fila de arriba A, abajo B. Columnas:
círculo, cuadrado, trébol, galleta, arco y temático. **Ninguna recorta el logo**,
y el temático enseña la silueta. El aro que rodea el icono es el resalte del
launcher para «app sugerida», no parte del icono.

![Máscaras, A y B](bloque-diseno-2b/gp081-mascaras-A-B.jpg)

**Las cuatro notificaciones en la barra de estado**, antes (izquierda) y después
(derecha): sesión completada con logro, push con la app en segundo plano, push con
la app abierta y rutina creada. Antes salía un icono genérico de cuadros
apilados, porque Android no acepta como icono pequeño los de sistema en color.
Para probar la push se arrancó un momento la API local con la credencial de
servicio que existe en local: solo envía a los tokens de la base local, que son
del emulador. Después se volvió a arrancar sin ella.

![Barra de estado](bloque-diseno-2b/gp081-barra-estado.jpg)

**Icono de 512 × 512 para Play**, de la misma fuente:
[A, oscuro](bloque-diseno-2b/gp081-play-A-oscuro.png) ·
[B, blanco](bloque-diseno-2b/gp081-play-B-blanco.png).

**Para cerrar GP-081 hace falta:** elegir A o B. Con B, la rama se fusiona tal
cual. Con A, se ejecuta el script con el logo oscuro y `#0B0C0E`, se cambia
`ic_launcher_fondo` y se fusiona. Queda por verificar el icono heredado de API
24-25 en un emulador de esa versión: no lo hay.

---

## Visto de paso y no arreglado

- «Has conseguido 1 logro(s)» en la notificación de logros: plural a mano con
  «(s)», contra DEC-020.
- «PROTEÍNAS» se sigue partiendo en el resumen del onboarding. Ahora pasa en los
  dos temas, porque los dos usan ya la fuente de cuerpo del sistema.
- El chip seleccionado de los filtros sigue siendo lila de Material por defecto
  (ya apuntado en el 2a).
