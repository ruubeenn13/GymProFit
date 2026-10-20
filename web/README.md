# web — sitio público de GymProFit

Sitio estático de `gymprofit.app`: portada, política de privacidad, términos de uso,
la página de eliminación de cuenta y la de la beta (`/beta`, `/beta/revisa`, `/beta/dentro` y `/beta/entrar`). HTML y CSS a mano, sin build, sin dependencias y sin
JavaScript. Lo que hay en esta carpeta es exactamente lo que se sirve.

```
index.html · privacidad.html · terminos.html · eliminar-cuenta.html · estilo.css
beta.html · beta/revisa.html · beta/dentro.html · beta.css   (la beta, GP-201)
beta/entrar.html   el enlace de los correos de «beta abierta» (GP-202)
_headers  la caché de Cloudflare Pages (ver «Caché y versiones»)
_redirects  la redirección de /beta/entrar, comentada hasta el día de apertura
fuentes/  seis .woff2: Barlow 400 / 500 / 600 y Barlow Condensed 500 / 600 / 700
img/      logos claro y oscuro (256 y 512, webp) + favicons y apple-touch-icon
```

## Dónde vive

Desplegado en **Cloudflare Pages**, proyecto **`gymprofit`**, por **subida
directa**: no está conectado a este repositorio, así que un push aquí **no
despliega nada**. El dominio `gymprofit.app` apunta a ese proyecto.

## Cómo se despliega a mano

1. Si ha cambiado alguna hoja, poner su versión nueva en las páginas, desde la
   raíz del repositorio: `python scripts/versionar-hojas-web.py`. Con
   `--comprobar` solo dice si falta; el test `HojasVersionadasWebTest` también
   falla si una hoja cambia y su versión no.
2. Comprimir el **contenido** de `web/` menos este README, no la carpeta. El zip
   que se sube es eso: lo que hay aquí, salvo `README.md`. `_headers` y
   `_redirects` van dentro.
3. Proyecto `gymprofit` en Cloudflare Pages → pestaña **Implementaciones** → subir
   el zip.

Lo del contenido no es manía: el zip se descomprime en la raíz del sitio, así que
si dentro va una carpeta `web/`, todo queda colgando de `/web/...` y las rutas
absolutas del HTML —`/estilo.css`, `/fuentes/...`, `/img/...`— apuntan a ficheros
que no existen. El sitio se ve sin estilos y sin tipografía.

## Caché y versiones

Un navegador que ya había visitado `/beta` mezcló la página nueva con el
`beta.css` de su caché (GP-201). Para que no vuelva a pasar:

- **Las hojas van versionadas por su contenido**: cada página pide
  `/beta.css?v=<hash>` o `/estilo.css?v=<hash>`, con los 8 primeros caracteres
  del SHA-256 de la hoja (saltos de línea normalizados a `\n`, para que salga
  igual en Windows y en el CI). La URL cambia solo cuando cambia la hoja. La
  versión la pone `scripts/versionar-hojas-web.py`; nunca a mano.
- **`_headers`**: el HTML, con `Cache-Control: no-cache` (se revalida siempre,
  y es lo que dice qué versión pedir); `estilo.css` y `beta.css`, un año e
  `immutable`. Las fuentes y las imágenes **no** van versionadas, así que se
  quedan con la revalidación de Pages: si un día se versionan, pueden pasar a
  `immutable`.
- **Ninguna regla `/*`**: si dos reglas ponen `Cache-Control` a la misma ruta,
  Pages une sus valores con una coma. Por eso cada página tiene su regla. Una
  página nueva necesita la suya, o se queda con la caché por defecto de Pages.

## Rutas limpias

Los enlaces internos son `/privacidad`, `/terminos` y `/eliminar-cuenta`, sin `.html`: Pages
resuelve la ruta limpia al fichero correspondiente. Esas dos URL públicas son
**requisito de Google Play** —política de privacidad y vía de eliminación de
cuenta—, así que si alguna vez se cambia de alojamiento hay que comprobar que el
nuevo también las resuelve, o los enlaces declarados en la ficha se rompen.

## La beta

`/beta` es la página a la que llega quien viene de TikTok o Instagram. Lleva su
propio **formulario**, que se envía por POST normal al endpoint del formulario de
Brevo, sin JavaScript (DEC-050). Brevo pide confirmar el correo: tras enviar
redirige a `/beta/revisa` y tras confirmar, a `/beta/dentro`. Las dos
redirecciones se configuran en Brevo, no aquí.

- **Los campos son los de Brevo**: `EMAIL`, `OPT_IN` (`value="1"`),
  `email_address_check` (trampa para bots, vacía y oculta para todos),
  `locale=es` y `html_type=simple`. Si la action o un nombre cambian en Brevo, se
  cambian en `beta.html` y en `BetaWebTest`, que los fija.
- **Solo Android.** La beta se instala desde Google Play; la página lo dice junto
  al formulario y no pregunta el móvil.
- **Rutas.** `beta.html` sirve `/beta`, y `beta/revisa.html` y `beta/dentro.html`
  sirven sus rutas: Pages resuelve por fichero, así que la carpeta `beta/` no le
  quita la ruta a `beta.html`. No hace falta `beta/index.html`, que daría `/beta/`
  con barra.
- **Su hoja es `beta.css`, no `estilo.css`.** La paleta es la de los vídeos
  (`#E8710A`, `#CBA135`, `#F5F1EA` sobre `#0B0C0E`), no la de la app: es lo que
  reconoce quien llega desde un vídeo. Las fuentes son los mismos ficheros.
- **El titular llena el ancho** línea a línea: cada `<span>` lleva `--k`, su ancho
  en em medido en el navegador. Si cambia el texto, hay que volver a medirlo.
- **El primer pantallazo a 375 × 667 es logo, titular, una línea y la tarjeta
  del formulario**, entera. Va justo: cualquier línea de más en esa zona la empuja
  fuera. El iPhone se nombra una sola vez, debajo; ningún texto se repite, y
  `BetaWebTest` lo comprueba frase a frase.
- **Todo se ve en 0,6 s.** Cada entrada dura 400 ms como mucho, el escalonado
  acaba antes de 600 ms y ninguna parte de opacidad 0, así que el contenido está a
  la vista aunque la animación tarde en arrancar. El formulario no se anima. Lo que
  respira, pulsa o celebra empieza después.
- **El movimiento es solo CSS** y vive entero dentro de
  `@media (prefers-reduced-motion: no-preference)`. Fuera de ese bloque la página
  está ya en su estado final, así que con movimiento reducido se ve quieta y completa.

## El día que abre la beta

Los correos de Brevo de «beta abierta» enlazan a `/beta/entrar`, y esa URL no
cambia nunca: las plantillas no se tocan (GP-202, DEC-051). Hasta el día de
apertura sirve `beta/entrar.html` («abre muy pronto», con vuelta a `/beta`). Una
regla de `_redirects` **gana al fichero de la misma ruta** —«Redirects are always
followed, regardless of whether or not an asset matches the incoming request»,
documentación de Cloudflare Pages—, así que abrir es cambiar una línea:

1. En `web/_redirects`, quitar el `#` de la línea de `/beta/entrar` y comprobar
   que su enlace es el de unión de la prueba cerrada que da Play Console. Se queda
   en **302**: un 301 lo guarda el navegador para siempre.
2. Pasar `BetaWebTest`: el enlace de la prueba cerrada solo puede estar en
   `_redirects`, y esa regla es la única activa.
3. Hacer el zip y subirlo como en «Cómo se despliega a mano». `beta/entrar.html`
   se queda: si la prueba se cierra, basta con volver a poner el `#`.
4. Comprobar: `curl -I https://gymprofit.app/beta/entrar` da `302` y
   `location:` con el enlace de Play.

## Los colores son los de la app

Salvo la beta (arriba), los tokens de `:root` en `estilo.css` son **los mismos valores** que
`app/GymProFit/app/src/main/res/values/colors.xml` (tema claro) y
`values-night/colors.xml` (tema oscuro). Están copiados a mano porque no hay
forma razonable de compartirlos entre un `colors.xml` y una hoja de estilos:
**si cambian allí, hay que cambiarlos aquí**, o la web y la app dejan de ser la
misma marca.

## Las fuentes se sirven desde este dominio

Los seis `.woff2` se generaron desde los `.ttf` de
`app/GymProFit/app/src/main/res/font/` (`barlow_regular`, `_medium` y `_semibold`;
`barlow_condensed_medium`, `_semibold` y `_bold`), por lo mismo: la web y la app
tienen que verse iguales. Una familia, dos anchos: la condensada en titulares,
cifras y etiquetas, y Barlow en todo el texto. Para regenerarlos hace falta
`fonttools` con `brotli`: `TTFont(ttf); f.flavor = "woff2"; f.save(woff2)`.

Se sirven **desde el propio dominio a propósito**, y no desde Google Fonts. Pedir
una fuente a un tercero le entrega la IP del visitante; hacerlo justo en la página
que explica cómo se tratan sus datos sería contradecirla en la primera línea. No
queda ninguna fuente del sistema a la vista: el `sans-serif` de la pila solo
aparece si un fichero no llega a cargar.

## Revisar si la app se monetiza

El responsable del tratamiento está identificado con nombre y con el correo
`privacidad@gymprofit.app` como vía de contacto, **sin dirección postal**: la app
es gratuita y sin actividad económica.

Eso cambia el día que se monetice. Al pasar a ser comerciante en la UE, Play
**publica la dirección postal de todas formas**, así que entonces hay que volver
sobre este bloque y decidir si se pone aquí también, en lugar de facilitarla solo
a petición.
