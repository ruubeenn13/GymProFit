# GP-279 · El repositorio, listo para quien llega desde redes

**Fecha:** 2026-11-19 · **Rama:** `gp-279-repo-publico` · **PR:** #129 · No toca el código de la app ni de la API.

## Commits

| Hash | Qué |
|---|---|
| `1b1094e5` | ci: job `secretos` con gitleaks 8.30.1 (binario fijado con SHA-256) sobre el árbol y los commits nuevos; `.gitleaks.toml` con tres excepciones revisadas. |
| `a7a9c411` | docs(decisiones): DEC-067 y `LICENSE` sin la mención académica. |
| `720ae129` | docs(repositorio): README, GIF, cifras, CONTRIBUTING, SECURITY, plantillas e imagen para redes. |

## Lo que ya existía

- **README** con portada, insignias de CI, versión y API, mermaid y «Arrancarlo en local»: se conserva todo salvo la cabecera. La portada SVG (`portada-*.svg`) decía «v1.6.5 · en desarrollo» y sale de la cabecera; los ficheros se quedan en el repositorio, sin uso.
- **LICENSE** ya era «todos los derechos reservados». Solo cambia el propósito («portfolio y evaluación académica del TFG», que contradice DEC-001) por «se puede ver, no reutilizar sin permiso», y el contacto.
- **CI** en verde en `main` (último run del 15-11, `63e3f837`). Dependabot y sus etiquetas, sin tocar.
- Etiqueta **`good first issue`**: existía; se le cambia la descripción.
- **Etiquetas `vX.Y.Z`**: la última es `v1.8.0`. No había ninguna release.

## Lo hecho

1. **GIF** (`documentacion/img/readme/demo.gif`): 320 px, 12 fps, 10,8 s, 1,82 MB. Emulador `Pixel_8_Capturas_34`, API local, variante `verificacion` sin distintivo (en local, sin commitear y quitada al acabar, como dice `scripts/README.md`), oscuro, español y barra de demostración. Recorrido: Inicio → Nutrición → Progreso → Entrenar, sin teclado. Para que hoy tuviera datos se **resembró `@dani.entrena`** en la BD local (borrado por `DELETE /usuarios/me` y `sembrar_demo.py` con la misma clave): los datos de la siembra del 13-11 acababan ese día y Nutrición y «Kilos movidos» salían a cero.
2. **README**: GIF, una línea, insignias (CI, licencia, versión, API), enlaces a `gymprofit.app` y `/beta`, «Probarlo» (beta para usuarios; los comandos de CLAUDE.md para desarrollar) y una línea de licencia. Puesto al día con lo medido en `main`: más de 1.100 tests de la API (1.128 `@Test`), más de 1.300 de la app (1.385), más de 270 endpoints (272 mapeos), 36 tablas y 64 migraciones (antes 1.147, 518, 250+, 30 y 51); el diario nuevo ya está hecho; la web también sirve la portada y la beta.
3. **DEC-067**: todos los derechos reservados, sin PR de código hasta un CLA, seguridad por privado.
4. **Temas y descripción** con `gh`: android, java, spring-boot, fitness, gym-tracker, nutrition, mariadb, jooq, workout-tracker y flyway; descripción de una línea y `https://gymprofit.app` como web.
5. **Imagen para redes** (`documentacion/marca/social-preview.png`, 1280×640): fondo `#0B0C0E`, Barlow y Barlow Condensed de `web/fuentes`, logo único y dos capturas reales de la demo (Inicio y Nutrición). **No se puede subir por API**: GitHub no la expone ni en REST ni en GraphQL. Queda para el propietario en Settings → General → Social preview.
6. **CI**: insignia en el README; job `secretos` nuevo. Se comprobó que falla: con una clave aleatoria plantada en `application-dev.properties` del árbol, gitleaks sale con 1; sin ella, con 0.
7. **Escaneo de secretos**: historial completo (1.151 commits) y árbol actual con gitleaks 8.30.1. **Ninguna credencial de producción.** El detalle va solo en el resumen al propietario. Sin reescribir el historial.
8. **CONTRIBUTING.md** y **SECURITY.md** (`gymprofit.soporte@gmail.com`).
9. **Plantillas**: `fallo.yml`, `propuesta.yml`, `config.yml` (sin incidencias en blanco; enlaces a SECURITY, Discussions y la beta) y `PULL_REQUEST_TEMPLATE.md` con el aviso del CLA. **Discussions activadas**: GitHub crea las categorías por defecto, entre ellas Q&A, Ideas y Announcements; la API no permite crear ni renombrar categorías.
10. **Etiquetas**: `beta` creada; `good first issue` con su descripción nueva.
11. **Release** `v1.8.0` con las notas del CHANGELOG (los ocho cambios de la sección del 22-10 entre `v1.7.0` y `v1.8.0`). Sin versión ni etiqueta nueva (DEC-056).
12. **Enlaces**: lychee 0.24.2 sobre README, CONTRIBUTING, SECURITY, LICENSE y las plantillas. Antes de fusionar, fallaban solo los que dependen de la propia fusión (SECURITY en `main`) o de Discussions, ya activadas. `ciqual.anses.fr` devolvía 503 «Maintenance en cours» (comprobado también con un navegador): el enlace es el correcto.

## Decisiones tomadas

- El GIF sustituye a la portada SVG en la cabecera, no se pone debajo: dos imágenes de cabecera empujaban «Probarlo» fuera de la primera pantalla, y la portada mostraba una versión vieja.
- La imagen para redes dice «apúntate a la beta», no «beta abierta»: la beta aún no se ha abierto.
- SECURITY no promete un plazo fijo de respuesta («lo antes posible, normalmente en pocos días»).
- Las cifras del README pasan a mínimos («más de 1.100») para que no se queden viejas con cada test.
- El job `secretos` no mira el historial viejo, solo el árbol y los commits nuevos: el historial ya está revisado y es público.

## Pendiente del propietario

- Subir `documentacion/marca/social-preview.png` en Settings → General → Social preview.
- Si se quieren las categorías en español («Preguntas», «Anuncios»), renombrarlas en la web. Ojo: si cambia el *slug*, los enlaces de CONTRIBUTING y de `config.yml` a `categories/q-a` e `ideas` dejan de valer.
- Hacer requerido el check `secretos` en la protección de `main`, si se quiere (no se ha tocado la rama protegida).
- Show HN y redes, al abrir la beta.
