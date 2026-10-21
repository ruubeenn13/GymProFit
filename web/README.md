# web — sitio público de GymProFit

Sitio estático de `gymprofit.app`: portada, política de privacidad, términos de uso,
la página de eliminación de cuenta, la de la beta (`/beta`, `/beta/revisa`, `/beta/dentro` y `/beta/entrar`) y la 404. HTML y CSS a mano, sin build. La portada lleva
JavaScript propio como mejora progresiva (DEC-053, ver «El movimiento de la portada»);
el resto de páginas, ninguno. Lo que hay en esta carpeta es lo que se sirve, salvo
lo que se queda fuera del zip (abajo).

```
index.html · portada.css   la portada de producto (GP-203)
privacidad.html · terminos.html · eliminar-cuenta.html · estilo.css
beta.html · beta/revisa.html · beta/dentro.html · beta.css   (la beta, GP-201)
beta/entrar.html   los pasos para entrar, enlace de los correos de «beta abierta» (GP-202)
404.html  lo que Pages sirve, con estado 404, en cualquier ruta que no existe (GP-205)
js/       portada.js y, en js/vendor/, GSAP, ScrollTrigger y Lenis con su licencia (DEC-053)
_headers  la caché de Cloudflare Pages (ver «Caché y versiones»)
DISENO.md · PRODUCT.md · .impeccable/   el diseño de GP-205: no van en el zip
fuentes/  seis .woff2: Barlow 400 / 500 / 600 y Barlow Condensed 500 / 600 / 700
img/      logos claro y oscuro (256 y 512, webp) + favicons y apple-touch-icon
img/capturas/  las cuatro capturas de la portada, en webp a 360 y 720 de ancho
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
2. Comprimir el **contenido** de `web/`, no la carpeta, **sin** `README.md`,
   `DISENO.md`, `PRODUCT.md` ni `.impeccable/`: son documentación del diseño, no
   páginas. `_headers` y `404.html` van dentro.
3. Proyecto `gymprofit` en Cloudflare Pages → pestaña **Implementaciones** → subir
   el zip.

Lo del contenido no es manía: el zip se descomprime en la raíz del sitio, así que
si dentro va una carpeta `web/`, todo queda colgando de `/web/...` y las rutas
absolutas del HTML —`/estilo.css`, `/fuentes/...`, `/img/...`— apuntan a ficheros
que no existen. El sitio se ve sin estilos y sin tipografía.

## Caché y versiones

Un navegador que ya había visitado `/beta` mezcló la página nueva con el
`beta.css` de su caché (GP-201). Para que no vuelva a pasar:

- **Las hojas y los scripts van versionados por su contenido**: cada página pide
  `/beta.css?v=<hash>`, `/estilo.css?v=<hash>` o `/portada.css?v=<hash>`, con los 8 primeros caracteres
  del SHA-256 de la hoja (saltos de línea normalizados a `\n`, para que salga
  igual en Windows y en el CI). La URL cambia solo cuando cambia la hoja. Los `.js`
  igual, también los de la lista `data-libs` que carga `portada.js`. La
  versión la pone `scripts/versionar-hojas-web.py`; nunca a mano.
- **`_headers`**: el HTML, con `Cache-Control: no-cache` (se revalida siempre,
  y es lo que dice qué versión pedir); `estilo.css`, `beta.css`, `portada.css` y
  todo `/js/*`, un año e `immutable`. Las fuentes y las imágenes **no** van versionadas, así que se
  quedan con la revalidación de Pages: si un día se versionan, pueden pasar a
  `immutable`.
- **Ninguna regla `/*`**: si dos reglas ponen `Cache-Control` a la misma ruta,
  Pages une sus valores con una coma. Por eso cada página tiene su regla. Una
  página nueva necesita la suya, o se queda con la caché por defecto de Pages.

## La 404

Cloudflare Pages sirve `404.html`, con estado **404**, en cualquier ruta que no
exista. Sin ese fichero, Pages trata el sitio como una aplicación de una página y
responde a cualquier ruta con la portada y un **200**, que los buscadores indexan
como páginas duplicadas. `scripts/servir-web.py` hace lo mismo en local.

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
  Desde GP-205 usa las curvas de la portada, sin rebotes, y nada se mueve sin parar
  (fuera el pulso del botón y la luz que respiraba).
- **El formulario es el dorsal** (GP-205): el único panel claro, con la banda, los
  ojales y el troquel de la portada, en el crema de los vídeos. No se gira ni se
  anima, y no puede crecer: la banda ocupa el relleno de arriba y el troquel va en
  el hueco de encima del botón, para que a 375 × 667 siga entero en la primera
  pantalla.

## El día que abre la beta

Los correos de Brevo de «beta abierta» enlazan a `/beta/entrar`, y esa URL no
cambia nunca: las plantillas no se tocan (GP-202). Es una página de tres pasos
(DEC-052, que sustituye a DEC-051): unirse al Grupo de Google de probadores,
aceptar la prueba en Google Play e instalar. Una prueba cerrada solo admite las
cuentas de su lista, y esa lista es el grupo, así que el grupo se ofrece ya en
`/beta/dentro`. Hasta el día de apertura el paso 2 se ve apagado, con «Disponible
el día de apertura». Abrir es cambiar un solo bloque de `beta/entrar.html`, el que
va entre `▼ APERTURA DE LA BETA` y `▲ FIN DE LA APERTURA DE LA BETA`:

1. Dentro de ese bloque, quitar las marcas `<!--` y `-->` que envuelven el paso con
   el botón «Aceptar la prueba», y borrar el paso apagado de debajo. Antes,
   comprobar que su enlace es el de unión que da Play Console.
2. Pasar `BetaWebTest`. Falla si el bloque queda a medias (los dos pasos a la vista o
   ninguno) y si el enlace de Play aparece fuera del bloque o en otra página.
3. Hacer el zip y subirlo como en «Cómo se despliega a mano».
4. Comprobar en `https://gymprofit.app/beta/entrar` que el botón está y lleva a la
   prueba. Si la prueba se cierra, se deshace igual: vuelve el paso apagado.

## La portada

`/` es la página de producto (GP-203): qué hace la app y cómo se entra en la beta.
Usa su propia hoja, `portada.css`, con los colores de la app, porque enseña la app.

- **Las capturas son de la app real**, con la cuenta demo del entorno local
  (`scripts/README.md` explica cómo se regeneran). En `img/capturas/`, en webp a 360 y
  720 de ancho, con `width` y `height` para que no salten al cargar. Solo la del hero
  carga sin esperar; las demás llevan `loading="lazy"`.
- **Cada función que nombra existe en `main`**: si una desaparece de la app, se quita
  de aquí. Sin cifras, testimonios, valoraciones ni logos de prensa, y sin redes por
  ahora.
- **Los titulares que llenan el ancho** (el del hero y «Entrenas en serio. Comes a
  ojo.») llevan `--k` por línea, como la beta: si cambia el texto, se vuelve a medir.
- `PortadaWebTest` fija el orden de las secciones, el único `h1`, las capturas, las
  preguntas, que no carga nada de fuera, que sus scripts son propios y el
  presupuesto de JavaScript.

## El movimiento de la portada

La dirección de diseño (el dorsal, la tela, el móvil fijo) está en `DISENO.md`; la
regla, en DEC-053.

- **Mejora progresiva.** Sin JavaScript la página está entera y quieta. La entrada
  del hero es CSS; lo que va atado al scroll lo hace `js/portada.js`, que carga
  GSAP, ScrollTrigger y Lenis desde `js/vendor/` **después del evento `load`** (antes
  retrasaban el LCP) y añade `.movimiento` a `<html>`.
- **Con movimiento reducido no se descarga nada**: `portada.js` sale antes de pedir
  las librerías, y las animaciones CSS viven dentro de
  `@media (prefers-reduced-motion: no-preference)`.
- **`portada.js` no cambia la altura de la página.** El layout de escritorio es el
  mismo con JavaScript que sin él: los móviles de cada pilar guardan su sitio con
  `visibility: hidden` y el móvil fijo los sustituye. Si el script cambiara alturas
  al arrancar, un ancla (`/#pilar-progreso`) o el scroll que el navegador restaura
  al recargar caerían en otro sitio.
- **Presupuesto**: todo el JavaScript de la portada, ≤ 60 KB comprimido
  (`PortadaWebTest`). Actualizar una librería es copiar su `.min.js` con su aviso de
  licencia en `js/vendor/` y volver a versionar.
- **Las líneas-guía** llevan en `--x` y `--y` dónde acaba el dato en la captura, en
  fracciones (1080 × 2400). Si cambia una captura, se vuelven a colocar. Debajo de
  1200 px no caben y se ve, en su lugar, una etiqueta bajo el móvil.

## Los colores son los de la app

Salvo la beta (arriba), los tokens de `:root` en `estilo.css` y en `portada.css` son **los mismos valores** que
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
cifras y etiquetas, y Barlow en todo el texto. Desde GP-205 van **recortados a los
caracteres de la web** (Latin-1, la puntuación general, € y ™), con el kerning y
las métricas intactos: pesan un 40 % menos (de ~39 a ~23 KB cada una) y eso bajaba
el LCP de la portada en un 4G lento. Para regenerarlos hace falta `fonttools` con
`brotli`, una vez por fichero:

```
pyftsubset barlow_regular.ttf --flavor=woff2 --layout-features='*'   --unicodes="U+0020-007E,U+00A0-00FF,U+0131,U+0152-0153,U+2010-2027,U+2030,U+2039-203A,U+20AC,U+2122,U+2212"   --output-file=barlow-400.woff2
```

Si un texto nuevo lleva un carácter fuera de ese rango (una flecha, un símbolo),
se añade al rango y se regeneran las seis; si no, ese carácter sale con otra fuente.

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
