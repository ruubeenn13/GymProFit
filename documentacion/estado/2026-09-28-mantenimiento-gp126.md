# Mantenimiento — el CI de la app (GP-126), el despliegue de la web y una regla nueva

2026-09-28. Rama `mantenimiento-gp126`, sacada de `main` (`9228bbd`).

| Commit | Tarea | Qué |
|---|---|---|
| `a727147` | GP-126 | CI · job `android-build-test`; la firma de release, solo con `keystore.properties` |
| `0bbc699` | — | `admin/README.md` · el despliegue, tal y como se publicó |
| `a375837` | — | `CLAUDE.md` · nada fuera de este repositorio sin que el propietario lo pida |

## 1 · GP-126, la app en el CI

`android-build-test` sigue el patrón de los otros dos jobs. El job `cambios` gana la salida `app`:
`true` si cambia algo en `app/` o el propio `ci.yml`, y también ante la duda (sin base con la que
comparar). Si no toca, el job se salta, y un job saltado cuenta como superado: no cuelga el gate.
Hace `sh ./gradlew --no-daemon assembleDebug test` con JDK 17 (el mínimo de AGP 8.13) y la caché
de `gradle/actions/setup-gradle`.

**Por qué un clon limpio no compilaba.** `signingConfigs.release` hacía `file(null)` si faltaba
`keystore.properties`, y eso rompía la configuración del proyecto entero, debug incluido. Ahora:

- La firma de release solo se configura si hay `keystore.properties`.
- Sin él, **empaquetar** el release (`packageRelease`, `bundleRelease`…) falla al empezar, con un
  mensaje que remite a «Compilar y firmar» en `app/GymProFit/README.md`. Antes, con la firma
  vacía, AGP habría sacado un APK sin firmar sin decir nada. Compilar y probar el release
  (`testReleaseUnitTest`, que `test` ejecuta) sí funciona: no firma.
- `local.properties` ya no hacía falta: el `BASE_URL` por defecto es el del emulador.
- El `google-services.json` de verdad no se versiona: el CI escribe uno de mentira con
  `package_name` `com.gymprofit.app`. Ningún secreto.

**Probado en un clon limpio** de la rama (`git clone`, sin `keystore.properties`,
`local.properties` ni `google-services.json`), con JDK 17 como en el CI:

| Paso | Resultado |
|---|---|
| Con el `build.gradle` de `main` | Rojo al evaluar el proyecto: `Cannot convert 'null' to File` |
| Con el nuevo, sin `google-services.json` | Configura; falla solo `processDebugGoogleServices`: falta el fichero |
| El paso del CI (el JSON de mentira, sacado del propio `ci.yml`) + `assembleDebug test` | `BUILD SUCCESSFUL` en 56 s; **144 tests** de debug y 144 de release, 0 fallos; `app-debug.apk` |
| `assembleRelease` sin `keystore.properties` | Falla al empezar con el mensaje nuevo |
| `assembleRelease --dry-run` en la copia con firma | Planifica `packageRelease` sin error |

El primer run de verdad es el de la fusión en `main` (ver abajo).

**Para marcarlo obligatorio** en GitHub (Settings → Branches → la regla de `main` → *Require
status checks*): el check se llama **`android-build-test`**. Conviene: el auto-merge de Dependabot
se apoya en los checks requeridos, y sin él un PR de Gradle patch/minor se fusionaría aunque
rompiera la app. No he podido ver desde aquí qué checks están marcados hoy.

## 2 · El README de la web

La sección de despliegue de `admin/README.md` se rehízo con lo que pasó al publicarla: Access
primero (y el código por correo, que hay que añadir como proveedor de identidad), el Worker con
*Import a repository*, qué hacer si falla la conexión con GitHub, el directorio raíz que el
formulario no tiene y se corrige después en Settings → Build, lo que salió mal en el primer
despliegue, cómo se ve uno bueno y cómo dar de alta o de baja a otro administrador.

## 3 · La regla nueva

`CLAUDE.md`, en *Prohibiciones duras*: nada fuera de este repositorio (otros repositorios, notas,
la configuración de la máquina) sin que el propietario lo pida.

## Visto y no tocado

- **La memoria de Claude sigue diciendo** que el mapa de arquitectura del bot se actualice al
  cambiar la app (`feedback_mapa_arquitectura` en el vault). Choca con la regla nueva, pero
  corregirla es tocar notas de fuera: no lo he hecho sin preguntar.
