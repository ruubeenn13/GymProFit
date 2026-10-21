# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Gente que entrena en serio en el gimnasio (o en casa con mancuernas o peso corporal) y que llega a `gymprofit.app` desde un vídeo de TikTok o Instagram, casi siempre en el móvil y en el navegador integrado de esas apps. Su trabajo en la web: entender en segundos qué hace la app y apuntarse a la beta. Una parte llega después desde los correos de la beta (`/beta/revisa`, `/beta/dentro`, `/beta/entrar`) y otra, desde Google Play, a las páginas legales.

## Product Purpose

GymProFit es una app Android que junta en un sitio el entreno, la nutrición y el progreso: apunta las series, las comidas y el peso, y dice qué toca hoy. La web existe para enseñar eso y llevar a la lista de espera de la beta. Éxito: que quien llega desde un vídeo se apunte y confirme el correo.

## Positioning

Entreno y dieta en la misma app, con el detalle de una app de entreno (lo de la última vez en cada serie, el descanso que avisa con la pantalla apagada) y el de una de nutrición (buscar o escanear, lo que queda hoy, copiar la comida de ayer). «Entrenas en serio. Comes a ojo.»

## Operating Context

- Beta cerrada de Google Play, solo Android y gratis. Se entra en tres pasos: apuntarse en `/beta` (formulario propio enviado a Brevo, doble confirmación), unirse al Grupo de Google de probadores y, el día de apertura, aceptar la prueba en Play.
- Sitio estático en Cloudflare Pages, subido a mano. Cero peticiones a terceros al cargar.

## Capabilities and Constraints

- Cada función que nombra la web existe en `main`; si desaparece de la app, se quita de la web.
- `/privacidad`, `/terminos` y `/eliminar-cuenta` son requisito de Google Play: sus URL no cambian.
- El bloque `▼ APERTURA DE LA BETA` de `/beta/entrar` es el único cambio para abrir la beta (GP-202, DEC-052).
- JavaScript propio solo como mejora progresiva (DEC-053): sin JS todo se ve y funciona.

## Brand Commitments

- Nombre GYMPROFIT, logos de `web/img`, fondo oscuro `#0B0C0E`, naranja de la app `#FF6A00`; Barlow y Barlow Condensed servidas desde el dominio.
- Paleta de los vídeos en la beta (`#E8710A`, `#CBA135`, `#F5F1EA`).
- Tono directo, en tuteo, sin épica de gimnasio.
- El propietario no quiere: aspecto de plantilla SaaS, movimiento de feria, estética de gimnasio agresiva (negro y neón, «no pain no gain»).

## Evidence on Hand

- Cuatro capturas reales de la app con la cuenta demo local (`web/img/capturas/`: inicio, entreno, nutrición, progreso), regenerables con `scripts/sembrar_demo.py`.
- No hay: cifras de usuarios, testimonios, valoraciones, prensa ni precios. No se inventan.

## Product Principles

1. Enseñar la app real antes que contarla.
2. Nada que comprometa el producto sin estar decidido: ni cifras, ni promesas, ni plazos.
3. Privacidad de verdad: nada sale de la página hasta que el visitante lo pide.
4. Rápida en un móvil con 4G y dentro del navegador de TikTok.

## Accessibility & Inclusion

WCAG AA. Con `prefers-reduced-motion` no hay movimiento. Todo usable con teclado y sin JavaScript.
