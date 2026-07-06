# PLAN — Feedback de carga y errores en Android (backlog #5)

**Prioridad:** 🔴 la más alta del backlog de pulido (por delante de #9 tests IDOR y #8 minify).
**Estado:** ✅ **COMPLETADO (2026-07-06)** — implementado en 4 batches (commits `b77551f`, `701666f`, `19049ef`, `53a492f`, `f047612`) y verificado end-to-end en emulador. ~25 Activities cubiertas (lectura, escritura, detalle y admin). Cambio sobre el plan: `UiFeedback` también silencia **404** (la API devuelve 404 en colecciones vacías, no 200+[]; se descubrió probando en emulador).
**Fecha plan:** 2026-07-04.

## Problema
- **Cold-start de Render (free)**: la 1ª petición tras dormir tarda ~60s (timeout ya subido en Batch 1, commit `e14ecdb`). Durante esa espera las pantallas de lectura salen **en blanco** → parece que la app está rota.
- **`onFail` solo loguea** (`ApiCallback` default): un error de red = pantalla muda, sin aviso al usuario. 30 overrides de `onFail` repartidos, la mayoría vacíos o con `Log`.
- **Listas vacías sin estado**: RecyclerViews sin datos se ven en blanco, sin "no hay nada aún".

Para un **producto real en lanzamiento** esto es dealbreaker.

## Alcance (medido)
- **~30 Activities** hacen red (`.enqueue`). Solo **3 layouts** tienen `ProgressBar` hoy (`activity_nutricion`, `activity_onboarding_resumen`, `activity_splash`).
- Herencia mixta: **15** extienden `BaseActivity`, **~23** extienden `AppCompatActivity` directo → **NO se puede centralizar por herencia**. `BaseActivity` no tiene helpers de loading/error.
- **31 bloques `new ApiCallback`**, **30 `onFail` override**.

## Estrategia — helpers reutilizables, sin tocar 30 layouts

### 1. Spinner de carga: `utils/LoadingDialog` (NUEVO)
- Diálogo modal translúcido con un `ProgressBar` centrado (layout `dialog_loading.xml`). Cancelable=false.
- API estática: `LoadingDialog.show(Activity)` / `LoadingDialog.hide()`. Una instancia por Activity (guardar en un `WeakHashMap<Activity,Dialog>` o campo).
- **No toca los layouts de cada pantalla** (es un overlay). Se muestra antes del `enqueue` y se oculta en `onOk`/`onFail`.
- Alternativa para el cold-start: primer intento muestra texto "Activando el servidor, puede tardar unos segundos…" (mensaje distinto en `code == -1`/timeout).

### 2. Mapeo de errores: `utils/UiFeedback` (NUEVO)
- `UiFeedback.toastError(Context, int code, String msg)`:
  - `code == -1` (timeout/red caída) → string `error_cold_start` ("Activando el servidor, reinténtalo en unos segundos").
  - `code == 401` → ya lo gestiona `ApiCallback` (notifyUnauthorized); no duplicar.
  - `code >= 500` → `error_servidor`.
  - resto → `error_generico`.
- Strings nuevos en `strings.xml` (ES) + `values-en/strings.xml`.

### 3. (Opcional) `network/UiApiCallback<T> extends ApiCallback<T>` (NUEVO)
- Constructor `UiApiCallback(Activity, boolean autoLoading)`.
- Auto-oculta el `LoadingDialog` y llama `UiFeedback.toastError` en `onFail` por defecto.
- Las pantallas que quieran feedback estándar cambian `new ApiCallback<>` → `new UiApiCallback<>(this)` y se ahorran repetir el toast/hide. Las que ya manejan error a mano siguen con `ApiCallback`.

### 4. Empty states (SÍ toca layouts de listas)
- Pantallas con `RecyclerView`: añadir un `TextView tvEmpty` (oculto por defecto) con mensaje "sin datos aún".
- En el `onOk`: `tvEmpty.setVisibility(lista.isEmpty() ? VISIBLE : GONE)`.
- Scope medio: ~8-10 layouts de lista.

## Orden de aplicación (pantallas de LECTURA primero, mayor impacto)
1. `HomeActivity`, `RutinasActivity`, `PerfilActivity`, `NutricionActivity` (arranque + más visitadas).
2. `SesionesActivity`, `MedicionesActivity`, `LogrosActivity`, `EjerciciosActivity`.
3. `ComidaActivity`, `DetalleRutinaActivity`, y pantallas admin (`Admin*Activity`).
4. Pantallas de escritura (`Registrar*`, `Editar*`, `Crear*`, `Anadir*`): spinner en el submit + toast de error/éxito.

## Anti-patrones a respetar (CLAUDE.md Android)
- Nada de Fragments/ViewModel/Room/Compose. Solo Activities + helpers estáticos.
- Strings SIEMPRE en `strings.xml` + `values-en`. Nunca hardcodear el mensaje.
- Red siempre `enqueue`. El `LoadingDialog` es solo UI, no bloquea hilo.
- IDs con prefijo: `pb` (ProgressBar), `tv` (TextView `tvEmpty`).
- Guard `isFinishing()/isDestroyed()` antes de tocar la vista en callbacks (patrón ya usado).

## Verificación
- `./gradlew :app:compileDebugJavaWithJavac` (JDK 21 en `C:\Users\ruben\.jdks\ms-21.0.11`).
- Prueba manual en emulador contra API local dev (`10.0.2.2:8080`): forzar cold-start (o cortar red) y ver spinner + toast; lista vacía → empty state.
- Commit por lotes de pantallas → siempre verde. Changelog + memoria por commit.

## Después de #5
- **#9** tests HTTP de ownership (403 IDOR) en 8 controllers (la protección YA existe, faltan los e2e).
- **#8** `minifyEnabled true` + `shrinkResources` + reglas ProGuard (keep `es.pmdm.gymprofit.model.**` para Gson + Retrofit/OkHttp).
- Firebase push (Hilo B): sigue bloqueado por la service-account key.
