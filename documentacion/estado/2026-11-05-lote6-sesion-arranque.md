# Lote 6 · sesión y arranque (GP-237, GP-235, GP-234, GP-233 y GP-219) — EN PAUSA

Pausado el 05-11-2026 a petición del propietario, sin cerrar. Lo retoma otra sesión.

## Encargo

Lote 6 · sesión y arranque: GP-237, GP-235, GP-234 y GP-233, más una prueba de GP-219. Solo app; la API no cambia.

OBJETIVO
Que un corte de red a mitad de uso no cierre la sesión, que perderla no dispare una llamada a /auth/refresh por petición, y que el tema y el idioma se fijen una vez al arrancar el proceso.

CONTEXTO (verificado en main, ccdcedc)
- GP-237: ApiClient.renovar devuelve null ante cualquier fallo de /auth/refresh (sin respuesta, tiempo agotado, 429, 5xx). TokenAuthenticator devuelve null, el 401 llega a ApiCallback.onResponse y UtilREST.notifyUnauthorized borra los tokens (memoria y almacén) y lleva a Entrar. Así se pierde un refresh válido por un corte de cobertura (el access dura 30 min) o por un 429 del cupo por IP. El arranque ya lo hace bien: DecisionArranque.clasificar solo cierra con un 401 (GP-223).
- GP-235: TokenAuthenticator lee el refresh fuera del cerrojo y dentro solo mira si otro hilo trajo un access nuevo. Si la renovación falla, cada petición en espera vuelve a llamar con el mismo refresh (6 en el 401 a mitad de uso de GP-223), y sin red cada una espera su propio tiempo agotado.
- GP-234: PreferencesManager.applyTheme() va en el onCreate de unas 20 pantallas, muchas después de super.onCreate (LoginActivity, SesionEnVivoActivity, RegistrarSesionActivity, ResumenSesionActivity…), y no en GymProFitApp.onCreate, donde ya va el idioma. Si una es la primera del proceso y el tema guardado no es el del sistema, se crea dos veces.
- GP-233: sin idioma guardado, BaseActivity.mostrarDialogoIdioma marca «Español» aunque la app salga en inglés.
- GP-219 dejó abierto que, por debajo de Android 13, el idioma vaya por el camino que perdía el tema (arreglado en 1d580ed con el override de ScaleUtils.wrap).

TAREA
1. GP-237 y GP-235, en un mismo cambio:
   - Solo un 401 de /auth/refresh cierra la sesión (con el aviso de cuenta desactivada si toca), con la clasificación de DecisionArranque compartida, no copiada. Sin respuesta, 429, 5xx o cuerpo ilegible: los tokens se quedan y la petición falla como sin respuesta (onFail(-1), el aviso de UiFeedback), nunca como 401; la siguiente acción vuelve a intentar.
   - Una llamada por intento: quien esperaba el cerrojo durante un intento usa su resultado (token nuevo, sesión cerrada o sin respuesta) sin volver a llamar. Un refresh rechazado se recuerda y quien llegue con él se rinde sin llamar; un fallo sin respuesta no pasa de su tanda.
   - DEC-059 en documentacion/PRODUCT-DECISIONS.md: solo un 401 de /auth/refresh cierra la sesión, al arrancar y a mitad de uso.
2. GP-234: el tema guardado se fija una vez en GymProFitApp.onCreate y se quitan las llamadas de cada pantalla, sin romper el override de ScaleUtils.wrap. Cambiar el tema en Ajustes sigue recreando.
3. GP-233: sin idioma guardado, el diálogo marca el idioma de los recursos con que se pinta la app (inglés si sale en inglés; si no, español).
4. GP-219, solo comprobar: en GymProFit_API_24, con el idioma de la app distinto del sistema, cambia la configuración del sistema con la app abierta (TalkBack, o el tamaño de letra si no hay TalkBack) y vuelve de otra pantalla. Si el idioma se pierde, va al override como el tema, con su test; si no, dilo.

RESTRICCIONES
- Sin dependencias nuevas salvo necesidad demostrada en el informe; mejor la decisión en una clase pura con la llamada inyectada, como DecisionArranque.
- La renovación del arranque no cambia de comportamiento.
- Pregunta antes de cualquier acción destructiva.

HECHO CUANDO
- Refresh sin respuesta, con 429 y con 5xx: nadie va a Entrar, los tokens siguen y, con la red de vuelta, la siguiente acción renueva y sigue. Refresh rechazado: Entrar una vez, como hoy.
- Con varias peticiones en espera, una llamada a /auth/refresh por intento, con la sesión viva, revocada y sin respuesta; con la viva, todas se reintentan con el token nuevo.
- Con la app en oscuro y el sistema en claro, y al revés, matar el proceso en Entrar, en la sesión en vivo y en Registrar sesión y volver crea cada pantalla una vez (contado en el log).
- Sin idioma elegido, con el sistema en inglés, en español y en francés, el diálogo marca el idioma que se ve.
- Tests nuevos vistos en rojo con el código de hoy.

VERIFICACIÓN
- Local, emulador contra la API local con jwt.expiration corto: renovación a mitad de uso sin respuesta y con 5xx (con una ayuda solo de depuración si hace falta), sesión viva y revocada, contando las llamadas a /auth/refresh en el log de la API.
- Producción, release con una cuenta de prueba: uso normal de Inicio, Nutrición y Entrenar y, tras cambiar la contraseña desde otra sesión (como en GP-236), Entrar con el aviso tras una sola llamada a /auth/refresh (contada en logcat).
- GP-234 y GP-233, en API 36 y en API 24.

INFORME breve: commits, tests (cuáles vistos en rojo), comprobación en producción, decisiones, lo de GP-219 y dudas o bloqueos. El largo, en documentacion/estado/.

## Dónde se queda

**Rama** `gp-237-sesion-arranque`, subida, sin PR. Commits sobre `main` (`ccdcedc`): `3c87a14` (GP-234) y el de este informe. **Stash** `lote-6-pausa` (`git stash list`; se aplica sobre esta rama con `git stash pop`): todo lo de GP-237, GP-235 y GP-233, y los apaños de depuración. La API no se ha tocado.

### GP-234 — hecho, falta API 24

- `3c87a14`: `GymProFitApp.aplicarTemaGuardado()` (antes del idioma) fija `setDefaultNightMode(PreferencesManager.temaGuardado(this))`; fuera `applyTheme()` y sus 21 llamadas (y los `PreferencesManager` locales e imports que quedaban sin uso). Cambiar el tema en Ajustes (`PerfilActivity.elegirTema` y el diálogo de `BaseActivity`) pasa por `PreferencesManager.cambiarTema`: guarda y llama a `setDefaultNightMode`, y AppCompat recrea; se quitó el `recreate()` explícito. `ScaleUtils.wrap` no cambia.
- Test `TemaUnaVezTest` (2), visto en rojo contra `main` (los 2 fallan). Suite unitaria 1049 en verde, `assembleDebug` y `lintDebug` bien.
- Medido en `Medium_Phone_API_36.1` (debug contra la API local), contando `wm_on_create_called` en `logcat -b events` tras matar el proceso con la app detrás y volver por el lanzador:
  - App oscura, sistema claro, APK de `main`: Entrar 2, sesión en vivo 2, Registrar sesión 2.
  - Igual con el cambio: Entrar 1 (3 de 3), sesión en vivo 1 (6 de 6), Registrar sesión 1 (3 de 3).
  - App clara, sistema oscuro, con el cambio: sesión en vivo 1 (2 de 2), Registrar sesión 1 (2 de 2). **Falta Entrar** en esta combinación.
  - Cambiar a claro en Ajustes: Perfil y la `MainActivity` de debajo se destruyen y se crean una vez cada una, y se ven en claro.
- **Falta**: Entrar con app clara y sistema oscuro, y las dos combinaciones en `GymProFit_API_24`.

### GP-237 y GP-235 — a medias (en el stash)

Ficheros: `network/TurnoRenovacion.java` (nuevo, puro), `network/SesionCerradaException.java` (nuevo), `network/ApiClient.java`, `network/ApiCallback.java`, tests `network/TurnoRenovacionTest.java` (11) y `network/TokenAuthenticatorTest.java` (6).

- `TurnoRenovacion`: la llamada y los tokens inyectados, la clasificación con `DecisionArranque.clasificar`. Solo un 401 de `/auth/refresh` da `CERRADA` (con cuenta desactivada si el cuerpo lo dice); sin respuesta, 429, 5xx o un 2xx sin los dos tokens dan `SIN_RESPUESTA` y no tocan los tokens. Un refresh rechazado se recuerda (`rechazado`) y quien llega con él se rinde sin llamar. La tanda se decide por la **hora de envío** de la petición (`Response.sentRequestAtMillis` contra la hora en que acabó el último intento): una enviada antes de que acabe un intento usa su resultado; una enviada después (la siguiente acción) vuelve a llamar.
- `ApiClient.TokenAuthenticator` usa `TURNO`: `RENOVADA` repite con el token nuevo; `SIN_REFRESH` y el segundo 401 seguido devuelven null como antes; `CERRADA` cierra la respuesta y lanza `SesionCerradaException` (aviso de cuenta desactivada si lo dice el refresh o el cuerpo del 401 original); `SIN_RESPUESTA` cierra la respuesta y lanza `IOException`, que llega a `ApiCallback.onFailure` como `onFail(-1)`. `ApiCallback.onFailure` trata `SesionCerradaException` como el 401 de siempre (`notifyUnauthorized` una vez + `onFail(401)`). La renovación del arranque solo cambia de cerrojo (`synchronized (TURNO)` en vez de `TokenAuthenticator.class`), mismo comportamiento.
- Tests vistos en rojo por mutación (el código de hoy no tiene la clase): sin la regla de la tanda fallan los dos de tanda; sin recordar el rechazado, el suyo; con sin respuesta como cierre, 5; con el autenticador devolviendo null sin respuesta (lo de hoy), el de 429/5xx; sin la rama de `SesionCerradaException` en `ApiCallback`, el suyo. Sin la tanda y sin el recuerdo a la vez, falla también el de la sesión revocada.
- Medido en el emulador (API 36, debug, access de 60 s, proxy delante de la API):
  - Primera versión (tanda por llegada del 401): sin respuesta, 9 peticiones fallan como sin red y 3 llamadas; 503, 9 fallidas y 4 llamadas. Nadie fue a Entrar y con la red de vuelta 1 llamada renovó y siguió.
  - Versión del stash (tanda por envío), sin ver logcat porque el emulador se cerró: 503 → 4 llamadas (1 al abrir Perfil, 3 al volver a Inicio en 83 ms); 429 → 3; 500 → 3; tras cada una, con la red, 1 llamada renovó. **La regla por hora de envío no juntó las 3 de la vuelta a Inicio**: sin investigar si esas peticiones salen en cadena (una tras el fallo de otra), lo que las haría acciones distintas, o si algo del cálculo falla.
- **Falta**: aclarar lo de arriba (logcat con la hora de envío de cada 401); sesión viva con varias en espera (una llamada) y revocada (cambiar la contraseña por curl: Entrar una vez tras 1 llamada); sin red de verdad (`svc wifi disable`); DEC-059 en `PRODUCT-DECISIONS.md`; CHANGELOG; producción con release y cuenta de prueba.

### GP-233 — a medias (en el stash)

Ficheros: `res/values/idioma.xml` y `res/values-en/idioma.xml` (bool `recursos_en_ingles`), `PreferencesManager.idiomaMarcado(guardado, recursosEnIngles)`, `BaseActivity.mostrarDialogoIdioma` y `PerfilActivity.esIngles` (la hoja de Ajustes tenía el mismo fallo: leía la primera configuración, «fr» con el sistema en francés e inglés, y la app sale en inglés). Test `IdiomaMarcadoTest` (3), visto en rojo con la lógica de hoy (falla el de «sin elegir y en inglés»).
**Falta**: comprobar en el emulador sin idioma elegido con el sistema en inglés, español y francés, en API 36 y 24.

### GP-219 — sin empezar

### Apaños de depuración (en el stash, `scripts/depuracion-lote6/`, no van al repositorio)

- `proxy.py`: proxy en 8080 hacia la API en 8081; todo pasa salvo `POST /api/auth/refresh`, que hace lo que diga `modo.txt` (junto al script): `pasar`, `503`/`500`/`429`/`502`, `colgar` (70 s), `cortar`. Cada llamada va a `refresh.log`. La API se arranca así, sin tocar ningún fichero de configuración: `sh ./mvnw -q spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --jwt.expiration=60000"`. La app de depuración sigue en `10.0.2.2:8080`.
- `tanda.sh <modo> [espera]`: espera a que caduque el access (65 s), pone el modo, abre Perfil y vuelve a Inicio, y cuenta las llamadas del proxy y las líneas de la app en logcat.
- `matar_y_volver.sh`: con la app delante, la manda atrás, la mata (`am kill` y, si sigue, `run-as … kill -9`), la abre por el lanzador y cuenta `wm_on_create_called` por pantalla cuando se reanuda. En el emulador cargado una creación tardó 19 s: el script espera a la reanudación.
- `toca.py "texto"`: pulsa el nodo que contiene ese texto o descripción (uiautomator). `entrar.sh`: entra con la cuenta local `gp237a`; lee la clave de `clave_local.txt`, que no está en el stash (se crea otra cuenta o se restablece con forgot-password: el código cae en `api/gymprofit-api/target/mail-outbox`).

### Estado del entorno

- Cuenta local `gp237a` (base 3308) con una sesión en vivo «Entrenamiento libre» empezada; producción no se ha tocado.
- `Medium_Phone_API_36.1`: queda instalada la debug del lote con esa cuenta (antes había el release de `f17027e` sin sesión) y el sistema en claro. API local, proxy y emulador parados.
