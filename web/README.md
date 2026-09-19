# web — sitio público de GymProFit

Sitio estático de `gymprofit.app`: portada, política de privacidad y la página de
eliminación de cuenta. HTML y CSS a mano, sin build, sin dependencias y sin
JavaScript. Lo que hay en esta carpeta es exactamente lo que se sirve.

```
index.html · privacidad.html · eliminar-cuenta.html · estilo.css
fuentes/  tres .woff2 (Barlow Condensed 500 / 600 / 700)
img/      logos claro y oscuro (256 y 512, webp) + favicons y apple-touch-icon
```

## Dónde vive

Desplegado en **Cloudflare Pages**, proyecto **`gymprofit`**, por **subida
directa**: no está conectado a este repositorio, así que un push aquí **no
despliega nada**. El dominio `gymprofit.app` apunta a ese proyecto.

## Cómo se despliega a mano

1. Comprimir el **contenido** de `web/`, no la carpeta.
2. Proyecto `gymprofit` en Cloudflare Pages → pestaña **Implementaciones** → subir
   el zip.

Lo del contenido no es manía: el zip se descomprime en la raíz del sitio, así que
si dentro va una carpeta `web/`, todo queda colgando de `/web/...` y las rutas
absolutas del HTML —`/estilo.css`, `/fuentes/...`, `/img/...`— apuntan a ficheros
que no existen. El sitio se ve sin estilos y sin tipografía.

## Rutas limpias

Los enlaces internos son `/privacidad` y `/eliminar-cuenta`, sin `.html`: Pages
resuelve la ruta limpia al fichero correspondiente. Esas dos URL públicas son
**requisito de Google Play** —política de privacidad y vía de eliminación de
cuenta—, así que si alguna vez se cambia de alojamiento hay que comprobar que el
nuevo también las resuelve, o los enlaces declarados en la ficha se rompen.

## Los colores son los de la app

Los tokens de `:root` en `estilo.css` son **los mismos valores** que
`app/GymProFit/app/src/main/res/values/colors.xml` (tema claro) y
`values-night/colors.xml` (tema oscuro). Están copiados a mano porque no hay
forma razonable de compartirlos entre un `colors.xml` y una hoja de estilos:
**si cambian allí, hay que cambiarlos aquí**, o la web y la app dejan de ser la
misma marca.

## Las fuentes se sirven desde este dominio

Los tres `.woff2` se generaron desde los `.ttf` de
`app/GymProFit/app/src/main/res/font/` (`barlow_condensed_medium`, `_semibold` y
`_bold`), por lo mismo: la web y la app tienen que verse iguales.

Se sirven **desde el propio dominio a propósito**, y no desde Google Fonts. Pedir
una fuente a un tercero le entrega la IP del visitante; hacerlo justo en la página
que explica cómo se tratan sus datos sería contradecirla en la primera línea. El
resto de la tipografía es la del sistema, que no se descarga de ningún sitio.

## Pendiente y bloqueante

El bloque **«1. Responsable del tratamiento»** de `privacidad.html` está sin
rellenar (`[PENDIENTE DE COMPLETAR]`): faltan nombre o razón social, NIF si
aplica y dirección postal. El RGPD exige identificar al responsable, así que
**esto bloquea la publicación en Play**: una política de privacidad sin
responsable identificado no cumple, y es de las cosas que se revisan.
