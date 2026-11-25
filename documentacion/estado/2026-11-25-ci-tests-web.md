# Los tests de la web corren en el CI aunque la PR solo toque `web/` (25-11-2026)

Rama `ci-tests-web`, PR #137. Sin número GP: está pendiente de que el propietario le dé uno, si
hace falta.

## El fallo

Los `*WebTest` viven en el módulo Android (`app/GymProFit/app/src/test/.../ui/`) y solo los pasaba
`android-build-test`, que el job `cambios` arranca si cambia `app/` o `ci.yml`. Una PR que solo
tocaba `web/` pasaba el CI sin ellos. Estaba en `main`; lo apunta `web/PLAN-UN-DIA.md` (§0, 9).

## El arreglo

`.github/workflows/ci.yml`, commit `0a755920`:

- `cambios` da una salida más, `web`: `true` si cambia algo de `web/` o el propio `ci.yml` (y,
  como las demás, cuando no hay base con la que comparar).
- Job nuevo `web-test`: JDK 17, Gradle, el `google-services.json` de mentira (sin él no compila el
  módulo de los tests) y `:app:testDebugUnitTest --tests '*WebTest'`, sin `assembleDebug` ni lint.
  Unos 60 s. Saltado cuenta como superado, como los demás.

Se eligió un job aparte y no añadir `web/` a la condición de `android-build-test`: esa condición
arrastra también `app-release-r8` (emulador, unos 7 min) por un cambio de HTML.

## La prueba

PR #138, de la rama `ci-tests-web-prueba` contra `ci-tests-web`, para que el diff solo tocase
`web/`. Cerrada sin fusionar y con la rama borrada.

| Commit de la rama de prueba | `web-test` | `android-build-test` |
|---|---|---|
| `6c6bb51a`: quita el `clamp()` de un titular de `portada.css` | **falla**: 49 tests, 3 en rojo (`TitularesWebTest.todoTamanoFluidoVaAcotado`, `TitularesWebTest.losTitularesQueLlenanElAnchoSalenDeSuColumna`, `HojasVersionadasWebTest.cadaPaginaPideSusHojasConLaVersionDeSuContenido`) | saltado |
| `25e145a8`: revierte el anterior | saltado: el diff contra la base queda vacío | saltado |
| `e579f6fa`: un comentario HTML en `privacidad.html` | **pasa** (1 min) | saltado |

En la PR #137, que toca `ci.yml`, corren todos y pasan: `web-test`, `android-build-test`,
`app-release-r8`, `api-build-test`, `secretos`.

## Pendiente del propietario

- Fusionar la PR #137 si le parece bien.
- Si los checks requeridos de `main` se fijan a mano en GitHub, añadir `web-test`.
