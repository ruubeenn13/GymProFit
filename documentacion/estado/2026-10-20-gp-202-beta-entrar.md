# GP-202 (parte web) · /beta/entrar — 2026-10-20

Rama `gp-202-beta-entrar`. Solo web y tests: sin API ni versión de app.

## Qué se ha hecho

- **`web/beta/entrar.html`**: la ruta de los correos de «beta abierta» existe ya. Titular «Abre muy pronto.», «La beta todavía no está abierta. Te avisaremos por correo en cuanto puedas entrar.» y botón «Volver a la beta» (`/beta`). Mismo marco, sello, pie y hoja (`beta.css?v=be26edd3`, sin cambios en la hoja) que `/beta/dentro`; el sello lleva un reloj. `noindex`. Los `--k` del titular medidos en el navegador (3,56 y 3,06), con el mismo método que da los de `/beta/dentro`. Comprobada a 375 × 667 en claro y en oscuro: sin desbordar y con el botón en el primer pantallazo.
- **`web/_redirects`**, nuevo: `/beta/entrar https://play.google.com/apps/testing/com.gymprofit.app 302`, **comentada**.
- **Caché**: la regla `/beta/*` de `_headers` ya cubre `/beta/entrar` con `no-cache`; no hacía falta otra.
- **`web/README.md`**: sección «El día que abre la beta», en cuatro pasos.
- **DEC-051** registrada.

## Prioridad de `_redirects` sobre ficheros

Documentación oficial de Cloudflare Pages (Configuration → Redirects): «Redirects are always followed, regardless of whether or not an asset matches the incoming request.» Por eso la página puede quedarse y la regla, comentada, gana el día que se descomente. Código por defecto 302; `#` es comentario.

## Tests

`BetaWebTest` (14, antes 11) y `HojasVersionadasWebTest` (2), en verde.

- `laBetaNoLlevaJavaScriptNiCargaNadaDeFuera` incluye ya `beta/entrar.html`.
- Nuevos: `entrarDiceQueAbreProntoYVuelveALaBeta` (noindex, texto, botón a `/beta`), `elEnlaceDeLaPruebaCerradaSoloEstaEnRedirects` (recorre todo `web/` salvo binarios) y `laRedireccionDeEntrarEstaListaEnUnaLinea` (la regla está una vez, comentada o no, y no hay otra activa).
- Vistos en rojo a propósito: con el enlace de Play en la página, sin `noindex` y con una regla extra en `_redirects`, fallaron los tres nuevos; restaurados después.

## Zip

`C:\Users\ruben\Desktop\gymprofit-web-gp-202.zip` (25 ficheros, 415 379 bytes): el de GP-201 más `_redirects` y `beta/entrar.html`, rutas con `/`, sin `README.md`.

## Pendiente

- **Subirlo a Cloudflare Pages** (propietario) y después la comprobación con `curl -I` de `/beta/entrar`: 200, `noindex` en el HTML, `Cache-Control: no-cache`, y `beta.css?v=be26edd3` con `public, max-age=31536000, immutable`.
- **El enlace de unión**: el de `_redirects` sigue el formato de Play (`/apps/testing/<applicationId>`); confirmarlo en Play Console antes de descomentar.
