# GP-258 — sin sesión guardada, «Tu sesión ha terminado» no sale nunca

Rama `gp-258-aviso-sin-sesion`, PR #104. Solo app: la API no cambia, así que Render no despliega.

## Commits

| Commit | Qué |
|---|---|
| `46dfbf9` test(android): la prueba de humo falla si sale «Tu sesión ha terminado» sin sesión | `ci/humo-release.sh` busca el aviso en el arranque desde el icono, con la app abierta sin la categoría LAUNCHER y tras una contraseña mal escrita. Va antes del arreglo para verla fallar. |
| `2512439` fix(android): sin sesión guardada no sale «Tu sesión ha terminado» | Las tres reglas de abajo, `SinSesionTest` y la precisión en DEC-059. |

## La causa

Encontrada, con el `api.log` y el logcat del run en rojo **43200335278** (el del commit `46dfbf9`, sin arreglo), que la reprodujo en el primer paso:

1. 10:32:49 el humo abre la app como el icono. `MainActivity` monta `PantallaArranque`, que decide.
2. 10:32:50 el emulador recién arrancado cambia la configuración: `InputReader: Reconfiguring input devices, changes=KEYBOARD_LAYOUTS` y `onSubscriptionsChanged` (la SIM). Android relanza `MainActivity` (`finishDrawing of relaunch` a las 10:32:51.8).
3. La recreada trae `savedInstanceState`, así que `PantallaArranque.esArranque` es falso y `MainActivity.java:153` (antes del arreglo) iba a `montarPestanas`: Inicio sin sesión.
4. Inicio pide `GET /ejercicios/activos` y `GET /sesiones/historial` sin token (`api.log`), la API contesta 401.
5. `ApiCallback.java:48` (antes del arreglo) llamaba a `UtilREST.notifyUnauthorized` con cualquier 401, y el oyente de `BaseActivity.java:71` abre Entrar con `EXTRA_SESION_CERRADA`.

Los dos runs del principio (43105641695 y 43113670636) cuadran con el mismo camino, según su logcat (no tenían `api.log`): el primero abrió `MainActivity` sin `ACTION_MAIN`/`LAUNCHER` (el humo de entonces aún no lo ponía), y el segundo tuvo el mismo relanzamiento por los dispositivos de entrada a las 22:47:20. Lo que dispara la recreación es del entorno; el fallo es de la app. En un móvil de verdad la dispara cualquier cambio de configuración en esos segundos (girar, la SIM, un teclado, el tamaño de letra) o abrir `MainActivity` desde algo que no es el icono.

Al revisar los caminos salieron dos más del mismo tipo:

- **La contraseña mal escrita en Entrar**: `POST /auth/login` da 401 y ese 401 pasaba por el mismo `notifyUnauthorized`. El primero tras instalar reabría Entrar con el aviso.
- **`POST /auth/change-password` salía sin token**: `ApiClient.esEndpointAuth` metía toda `/auth/` en el grupo sin token, y la API la pide autenticada. Cambiar la contraseña desde Ajustes daba 401 y cerraba la sesión. Lo vio `SinSesionTest.cambiar_la_contrasena_lleva_el_token` en rojo.

Revisado y descartado: el registro del token de FCM (`PushTokenManager.registrar` ya sale si no hay `usuarioId`, y `cerrarSesion` lo borra; `onNewToken` pasa por ahí), los receptores de `envivo/` (no llaman a la API), `GymProFitApp` (no pide nada), y lo que sale justo tras Entrar o el alta (`LoginActivity` y `GuardaPlanActivity` guardan los tokens antes de la siguiente petición). Las pantallas sin sesión usan solo `/auth/*`, `/programas/recomendado`, `/programas/{c}/vista-previa` y `/actuator/health`.

## El arreglo, por regla

1. **Sin sesión, `MainActivity` decide siempre por el arranque** (`DecisionArranque.pasaPorElArranque`): venga del icono, de otra intención o recreada. Con sesión, como antes.
2. **Un 401 solo cierra la sesión si la petición salió con un access token** (`ApiCallback.llevabaSesion`, mira la última petición, la del reintento si el autenticador renovó). El resto llega a la pantalla como `onFail(401)`.
3. **Una petición que pide sesión no sale sin access ni refresh**: el `AuthInterceptor` lanza `SinSesionException` y `ApiCallback` da `onFail(401)` sin aviso. Con solo el refresh sí sale, porque es una sesión y el autenticador la renueva. Las rutas sin sesión, en `ApiClient.esRutaSinSesion`, son las públicas de `SecurityConfig` que usa la app; `/auth/change-password` ya no está entre ellas y lleva su token.

DEC-059 no cambia para una sesión real: la petición con token que da 401, o el 401 de `/auth/refresh`, cierran igual (`un_401_a_una_peticion_con_token_sigue_cerrando_la_sesion` y los de `TokenAuthenticatorTest`). La precisión queda apuntada en DEC-059.

## Tests

- `SinSesionTest`, 7 tests. Antes del arreglo, 4 en rojo: `un_401_a_una_peticion_sin_token_no_avisa_de_sesion_terminada` («expected:<0> but was:<1>»), `sin_sesion_una_peticion_que_la_necesita_no_sale` («usuarios/3: salió sin token»), `cambiar_la_contrasena_lleva_el_token` («expected:<Bearer access> but was:<null>») y `sin_sesion_main_activity_siempre_decide_por_el_arranque`. Los otros 3 protegen lo que no debe cambiar (DEC-059, rutas públicas, sesión con solo el refresh).
- Suite de la app: 1282 tests en verde; `assembleDebug` y `lintDebug` sin fallos.
- Humo de la release: run **43200335278** en rojo sin el arreglo («Humo de la release: «Tu sesión ha terminado» sin sesión guardada (arranque desde el icono)»); run **43201522408** con el arreglo, en verde: pasa los tres pasos nuevos y el recorrido entero.

## Comprobación en producción

Release normal (`assembleRelease`, 1.8.0, API de producción) en `Medium_Phone_API_36.1`:

- Instalación limpia y abierta como el icono: Bienvenida, sin aviso.
- Cerrada y abierta sin la categoría LAUNCHER: Bienvenida, sin aviso.
- Abierta como el icono con un cambio de tamaño de letra a los 0,4 s (recreación mientras decide): Bienvenida, sin aviso.
- Cuenta de prueba `prueba258c4b7b1` creada por `POST /auth/register` (201). En Entrar, primero con una clave mala: se queda en Entrar, sin aviso. Después con la buena: entra (va al cuestionario de cuenta existente, porque se creó sin perfil).
- Borrada con `DELETE /usuarios/me` (200); el login ya da 401. Fichero de la clave borrado después. App desinstalada del emulador.

## Dudas y límites

- La lista de rutas sin sesión copia `SecurityConfig`. Una ruta pública nueva que la app pida sin sesión y no esté en `esRutaSinSesion` falla en el móvil con `onFail(401)`; se ve en cuanto se prueba sin cuenta, pero hay que acordarse de añadirla.
- El cambio de contraseña desde Ajustes estaba roto en todas las builds repartidas hasta ahora. No es de seguridad (fallaba cerrando la sesión), pero quien lo haya probado habrá visto Entrar con el aviso.
