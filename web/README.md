# web — sitio público de GymProFit

Sitio estático de `gymprofit.app`: portada, política de privacidad, términos de uso,
la página de eliminación de cuenta, la de la beta (`/beta`, `/beta/revisa`, `/beta/dentro` y `/beta/entrar`) y la 404. La portada lleva
JavaScript propio como mejora progresiva (DEC-053, ver «El movimiento de la portada»);
el resto de páginas, ninguno.

**En las ramas de «Un día» (DEC-069, aceptada el 26-11-2026) la web se construye con
Astro**: lo que se sirve es `web/dist/`, no `web/`. Desde la fase 4, las páginas
secundarias (privacidad, términos, eliminar cuenta, la beta y sus tres páginas, y la 404)
salen de `src/pages/` en el mundo de «Un día» (ver «Las páginas secundarias»). La
portada del dorsal sigue tal cual en `public/` hasta la fase 3, y Astro la copia a `dist`
sin tocarla; la prueba de «Un día» vive en `/un-dia` (`noindex`).

```
npm ci && npm run build      # en web/: escribe dist/ (Node ≥ 22.12; la telemetría de Astro va apagada)
python scripts/servir-web.py # desde la raíz: sirve dist como Pages, con brotli; --red para el móvil
```

`package.json` · `package-lock.json` · `astro.config.mjs` · `herramientas/astro.mjs` (el
lanzador que apaga la telemetría) · `src/pages/` (`un-dia.astro`, las legales, `beta/`,
`404.astro`) · `src/componentes/app/` (la interfaz de la app rehecha) ·
`src/componentes/dia/` (el reloj, la luz) · `src/componentes/comunes/` (el armazón y el
pie de las páginas secundarias) · `src/estilos/` ·
`src/movimiento/` (`inicio.js`, la escena, `depurar.js` del panel `?debug=motion`) ·
`src/datos/dia-ejemplo.json` (lo escribe `scripts/dia-ejemplo-web.py` desde la demo
local). Los tests de la web leen `dist`: hay que construirla antes de `gradlew test`.
`pruebas/antes/` guarda las páginas secundarias tal como eran en `public/` antes de la
fase 4: son la referencia de `TextoIgualWebTest` y no van en el zip.

Las páginas que salen de `src/pages/` (fase 4):

```
privacidad · terminos · eliminar-cuenta   con lectura.css (y mundo.css, lo común)
beta · beta/revisa · beta/dentro          con beta.css (la beta, GP-201)
beta/entrar   los pasos para entrar, enlace de los correos de «beta abierta» (GP-202)
404           lo que Pages sirve, con estado 404, en cualquier ruta que no existe (GP-205)
```

Lo que queda en `public/`:

```
index.html · portada.css   la portada de producto (GP-203), hasta la fase 3
js/       portada.js y, en js/vendor/, GSAP, ScrollTrigger y Lenis con su licencia (DEC-053)
_headers  la caché de Cloudflare Pages (ver «Caché y versiones»)
DISENO.md · PRODUCT.md · .impeccable/   el diseño de GP-205: no van en el zip
PLAN-UN-DIA.md   el plan del rediseño «Un día» (DEC-069): no va en el zip
fuentes/  seis .woff2: Barlow 400 / 500 / 600 y Barlow Condensed 500 / 600 / 700
img/      logo.svg (el logo único), favicon.png y apple-touch-icon.png (DEC-057), logo-correo.png
          (la G sola y transparente de los correos, GP-266) + las texturas
img/capturas/  las cuatro capturas de la portada, en webp a 360 y 720 de ancho
```

## Dónde vive

Desplegado en **Cloudflare Pages**, proyecto **`gymprofit`**, por **subida
directa**: no está conectado a este repositorio, así que un push aquí **no
despliega nada**. El dominio `gymprofit.app` apunta a ese proyecto.

## Cómo se despliega a mano

1. Si ha cambiado `portada.css` o un script de la portada, poner su versión nueva en
   la página, desde la raíz del repositorio: `python scripts/versionar-hojas-web.py`
   (versiona `public/`, donde ya solo queda la portada del dorsal; lo que construye
   Astro lleva el hash en el nombre y no lo necesita). Con `--comprobar` solo dice si falta; el test
   `HojasVersionadasWebTest` también falla si una hoja cambia y su versión no.
2. En `main` (sin Astro): comprimir el **contenido** de `web/`, no la carpeta,
   **sin** `README.md`, `DISENO.md`, `PRODUCT.md`, `PLAN-UN-DIA.md` ni `.impeccable/`:
   son documentación del diseño, no páginas. `_headers` y `404.html` van dentro.
   Con Astro: `npm ci && npm run build` y comprimir el **contenido de `web/dist/`**,
   donde la documentación ya no está.
3. Proyecto `gymprofit` en Cloudflare Pages → pestaña **Implementaciones** → subir
   el zip.

Lo del contenido no es manía: el zip se descomprime en la raíz del sitio, así que
si dentro va una carpeta `web/`, todo queda colgando de `/web/...` y las rutas
absolutas del HTML —`/_astro/...`, `/fuentes/...`, `/img/...`— apuntan a ficheros
que no existen. El sitio se ve sin estilos y sin tipografía.

## Vista previa para el móvil (nunca producción)

TikTok no abre `http://` ni IP de la red local, así que para medir «Un día» en su navegador hace
falta https. Se sube una **vista previa** al mismo proyecto, en una rama que no es la de producción.
La sube el propietario con su cuenta; nadie más inicia sesión ni sube nada.

Sin instalar nada global: `npx wrangler@4.168.0` (comprobado el 25-11-2026 con `--help`).

1. Construir: `cd web && npm ci && npm run build`.
2. **Comprobar la rama de producción**: Pages publica en producción lo que se sube a la rama de
   producción del proyecto, y en vista previa lo de cualquier otra. En un proyecto de subida directa
   esa rama se fija al crearlo. `npx wrangler@4.168.0 pages deployment list --project-name=gymprofit --environment=production`
   enseña la rama de cada despliegue de producción: si alguno fuera `un-dia-prueba`, **no se sigue**.
3. Subir: `npx wrangler@4.168.0 pages deploy dist --project-name=gymprofit --branch=un-dia-prueba --commit-dirty=true`
   (desde `web/`; pide `npx wrangler@4.168.0 login` la primera vez).
4. Queda en `https://un-dia-prueba.gymprofit.pages.dev` (alias de la rama, siempre el último
   despliegue) y en una URL con hash por despliegue. Comprobar que es vista previa y no se indexa:
   `curl -sI https://un-dia-prueba.gymprofit.pages.dev/un-dia | grep -i x-robots-tag` → `noindex`
   (Pages lo pone en toda vista previa, y `_headers` y la etiqueta `robots` de la página, también).
5. `gymprofit.app` no cambia: compruébalo con `curl -sI https://gymprofit.app/un-dia` (404 mientras
   «Un día» no esté en producción).

## Caché y versiones

Un navegador que ya había visitado `/beta` mezcló la página nueva con el
`beta.css` de su caché (GP-201). Para que no vuelva a pasar:

- **Las hojas y los scripts van versionados por su contenido.** Lo que construye
  Astro lleva el hash en el nombre (`/_astro/beta.<hash>.css`), y las hojas pequeñas
  van dentro del propio HTML (`inlineStylesheets: 'auto'`), que se revalida siempre.
  La portada del dorsal pide `/portada.css?v=<hash>`, con los 8 primeros caracteres
  del SHA-256 de la hoja (saltos de línea normalizados a `\n`, para que salga
  igual en Windows y en el CI). La URL cambia solo cuando cambia la hoja. Sus `.js`
  igual, también los de la lista `data-libs` que carga `portada.js`. Esa
  versión la pone `scripts/versionar-hojas-web.py`; nunca a mano.
- **`_headers`**: el HTML, con `Cache-Control: no-cache` (se revalida siempre,
  y es lo que dice qué versión pedir); `/_astro/*`, `portada.css` y
  todo `/js/*`, un año e `immutable`. `estilo.css` y `beta.css` ya no existen (fase 4),
  ni sus reglas. Las fuentes y las imágenes **no** van versionadas, así que se
  quedan con la revalidación de Pages: si un día se versionan, pueden pasar a
  `immutable`.
- **El logo y el favicon tienen nombres sin tema ni tamaño**: `img/logo.svg`,
  `img/favicon.png` e `img/apple-touch-icon.png`. Como se revalidan, un logo
  regenerado con el mismo nombre llega solo; el nombre cambia solo si cambia lo
  que el fichero es (no si se regenera), porque los navegadores guardan el favicon
  aparte y no siempre lo vuelven a pedir con el mismo `href`. Así se hizo en GP-231,
  cuando los dos favicons por tema pasaron a uno solo.
- **Ninguna regla `/*`**: si dos reglas ponen `Cache-Control` a la misma ruta,
  Pages une sus valores con una coma. Por eso cada página tiene su regla. Una
  página nueva necesita la suya, o se queda con la caché por defecto de Pages.

## La 404

Cloudflare Pages sirve `404.html`, con estado **404**, en cualquier ruta que no
exista. Sale de `src/pages/404.astro`: «la hora que no existe», las 25:61 en el reloj de
cabecera y en grande, siempre en carbón, sin ningún JavaScript y con rutas absolutas en
todo lo que carga (`PaginaNoEncontradaWebTest`). Sin ese fichero, Pages trata el sitio como una aplicación de una página y
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
  cambian en `src/pages/beta.astro` y en `BetaWebTest`, que los fija.
- **Solo Android.** La beta se instala desde Google Play; la página lo dice junto
  al formulario y no pregunta el móvil.
- **Rutas.** `dist/beta.html` sirve `/beta`, y `beta/revisa.html` y `beta/dentro.html`
  sirven sus rutas: Pages resuelve por fichero, así que la carpeta `beta/` no le
  quita la ruta a `beta.html`. No hace falta `beta/index.html`, que daría `/beta/`
  con barra (`build.format: 'file'`; `RutasWebTest`).
- **Su hoja es `src/estilos/beta.css`, no `lectura.css`.** La paleta es la de los vídeos
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
- **El formulario es una tarjeta de la app** (radio 24) desde la fase 4: deja el dorsal,
  la tela y el troquel de GP-205. No se gira ni se anima, y no puede crecer, para que
  a 375 × 667 siga entero en la primera pantalla.

## El día que abre la beta

Los correos de Brevo de «beta abierta» enlazan a `/beta/entrar`, y esa URL no
cambia nunca: las plantillas no se tocan (GP-202). Es una página de tres pasos
(DEC-052, que sustituye a DEC-051): unirse al Grupo de Google de probadores,
aceptar la prueba en Google Play e instalar. Una prueba cerrada solo admite las
cuentas de su lista, y esa lista es el grupo, así que el grupo se ofrece ya en
`/beta/dentro`. Hasta el día de apertura el paso 2 se ve apagado, con «Disponible
el día de apertura». Abrir es cambiar un solo bloque de `src/pages/beta/entrar.astro`
(Astro conserva los comentarios en `dist/beta/entrar.html`), el que
va entre `▼ APERTURA DE LA BETA` y `▲ FIN DE LA APERTURA DE LA BETA`:

1. Dentro de ese bloque, quitar las marcas `<!--` y `-->` que envuelven el paso con
   el botón «Aceptar la prueba», y borrar el paso apagado de debajo. Antes,
   comprobar que su enlace es el de unión que da Play Console.
2. Pasar `BetaWebTest`. Falla si el bloque queda a medias (los dos pasos a la vista o
   ninguno) y si el enlace de Play aparece fuera del bloque o en otra página.
3. Hacer el zip y subirlo como en «Cómo se despliega a mano».
4. Comprobar en `https://gymprofit.app/beta/entrar` que el botón está y lleva a la
   prueba. Si la prueba se cierra, se deshace igual: vuelve el paso apagado.

## Las páginas secundarias

Desde la fase 4 de «Un día» (`PLAN-UN-DIA.md` §7), privacidad, términos, eliminar
cuenta, la beta y sus tres páginas, y la 404 viven en el mundo nuevo sin escenas ni
GSAP. Todas comparten `src/componentes/comunes/Pagina.astro` y `src/estilos/mundo.css`:

- **El texto no cambia al rediseñar.** Privacidad, términos y eliminar cuenta son textos
  legales que Google Play tiene declarados. `TextoIgualWebTest` compara el texto y los
  enlaces de cada página con su copia de `pruebas/antes/`: cambiar un texto a propósito
  es cambiar también esa copia, en el mismo commit.
- **El reloj de cabecera es la marca.** Cada página marca su hora: las legales, las 7:00;
  la beta, las 19:00; la 404, las 25:61. Es decorativo (`aria-hidden`).
- **La luz, quieta.** Las lamas de la portada, una capa pintada una vez, arriba y
  apagándose hacia abajo. Su opacidad, por tema, sale de medir el contraste de cada texto
  sobre el punto más claro de una banda: `LuzQuietaWebTest` falla si un texto baja de
  4,5:1. Las legales y la beta, en claro y oscuro; la 404, siempre en carbón.
- **Transición entre páginas** con View Transitions entre documentos: solo
  `@view-transition` en CSS, sin JavaScript ni `ClientRouter`, con el reloj como elemento
  compartido (la hora sale por arriba y entra la nueva) y nada con movimiento reducido.
  La portada del dorsal no la pide, así que entre ella y estas páginas se navega como
  siempre; `TransicionesWebTest` lo fija.

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
- **Ningún móvil se ve vacío.** Cada captura lleva de fondo su reserva: ella misma
  a 20 × 44 px, borrosa, en línea en `portada.css` (con su color medio debajo). Si
  cambia una captura, se regenera con `python scripts/reservas-capturas-web.py`
  (`PortadaWebTest` falla si falta una). Además, `portada.js` pide y decodifica cada
  captura un viewport antes de que entre en pantalla, también con movimiento
  reducido; sin JavaScript queda el `loading="lazy"` del navegador.
- **Las líneas-guía** llevan en `--x` y `--y` dónde acaba el dato en la captura, en
  fracciones (1080 × 2400). Si cambia una captura, se vuelven a colocar. Debajo de
  1200 px no caben y se ve, en su lugar, una etiqueta bajo el móvil.

## Los colores son los de la app

Salvo la beta (arriba), los tokens de `:root` en `src/estilos/lectura.css` y en `portada.css` son **los mismos valores** que
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
