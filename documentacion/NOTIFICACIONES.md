# Sistema de notificaciones — GymProFit

Estado: **✅ COMPLETO y verificado end-to-end en emulador** (2026-07-06). Push real recibida (inmediata, programada y recordatorios), multiidioma ES/EN.

## Arquitectura (visión general)

```
                    ┌──────────────────────── BACKEND (Spring Boot) ────────────────────────┐
App Android         │                                                                        │
  login ──────────► │ POST /notificaciones/token  → device_tokens (token FCM + idioma)       │
                    │                                                                        │
  POST /notificaciones (inmediata) → NotificacionService.save ──► push al momento            │
  POST /notificaciones (fechaProgramada) → BD con push_enviada=false                         │
                    │                                    ▲                                   │
                    │  NotificacionProgramadaTask (@Scheduled 60s) ── envía vencidas ────────┤
                    │  RecordatorioNotificacionesTask (11 crons) ── genera recordatorios ────┤
                    │                                    │                                   │
                    │  PushNotificationService ── Firebase Admin SDK ──► FCM (Google)        │
                    └────────────────────────────────────┼───────────────────────────────────┘
                                                         ▼
  GymFirebaseMessagingService ◄── push ── (foreground: la app la pinta; background: Android)
```

## Componentes backend

| Componente | Qué hace |
|---|---|
| `config/FirebaseConfig` | Inicializa el Admin SDK **una vez**. Init *graceful*: sin credencial → push desactivado y la app arranca normal (CI no rompe). Credencial por env: `FIREBASE_CREDENTIALS_JSON` (prod, contenido del JSON) o `FIREBASE_CREDENTIALS_PATH` (local, ruta al fichero gitignoreado). |
| `PushNotificationService.enviarA(usuarioId, titulo, cuerpo)` | Envía la push a **todos** los dispositivos del usuario. Tolerante a fallos (nunca lanza); borra de BD los tokens muertos (`UNREGISTERED`/`INVALID_ARGUMENT`). No-op si Firebase no está inicializado. |
| `DeviceTokenController` + `DeviceTokenService` | `POST /notificaciones/token` (registro, **upsert idempotente** por token; el usuario sale del JWT, nunca del body) y `DELETE /notificaciones/token` (logout). Guarda también el **idioma** del dispositivo. |
| `NotificacionService.save` | CRUD normal (usuario autenticado). Inmediata → push al momento (`push_enviada=true`); con `fechaProgramada` → queda pendiente (`push_enviada=false`). |
| `NotificacionService.crearSistema` | Variante **sin SecurityUtils** para los jobs `@Scheduled` (no tienen SecurityContext). Persiste in-app + push inmediata. |
| `NotificacionProgramadaTask` | Cada 60 s: envía las notificaciones con `fecha_programada` vencida y `push_enviada=false`, y las marca enviadas (sin duplicados; la BD es la cola — sobrevive reinicios). |
| `RecordatorioNotificacionesTask` | Los 11 generadores automáticos (tabla abajo). |

## Los 11 recordatorios automáticos

Todos con `zone="Europe/Madrid"` (el servidor corre en UTC) y **solo para usuarios con dispositivo registrado** (`findDistinctUsuarioIds`). La condición se evalúa **en el momento del envío** (no se pre-programa): si registras la cena a las 20:50, a las 21:00 no llega nada.

| # | Recordatorio | Cuándo | Condición | Anti-spam |
|---|---|---|---|---|
| 1-5 | Comidas (desayuno/almuerzo/comida/merienda/cena) | 8:00 / 11:00 / 14:00 / 17:00 / 21:00 | No registró esa comida hoy | — (cron 1/día) |
| 6 | Entrenar (genérico, sin nombre de rutina) | 18:00 | No entrenó hoy **y** sí en los últimos 3 días (activos) | — |
| 7 | Inactividad | 12:00 | ≥3 días sin sesión (o nunca) | 4 días |
| 8 | Resumen semanal | domingo 20:00 | ≥1 sesión esta semana (nº sesiones, min, kcal) | — |
| 9 | Logro próximo | 20:30 | A 1 sesión de CONSTANCIA (7) o DEDICADO (30)* | 7 días |
| 10 | Medición mensual | 10:00 | Última medición hace ≥30 días | 30 días |
| 11 | Objetivo por vencer | 12:30 | Objetivo sin completar con fecha límite ≤3 días | 3 días |

\* Umbrales hardcodeados: en BD la tabla `logros` no guarda el umbral (vive en el switch de `LogroService`).

**Anti-spam**: `existsByUsuarioIdAndTituloAndFechaCreacionAfter` — no se repite una notificación con el mismo título dentro de su ventana. Edge case aceptado: si el usuario cambia de idioma, el título cambia y puede recibir un duplicado puntual.

Los generadores 6 y 7 se complementan sin duplicarse: 6 solo avisa a usuarios activos; a partir de 3 días parados entra 7.

**Descartado a propósito**: notificación de "récord personal" — el servidor no calcula récords (`mejorPeso` llega ya calculado del cliente); inventar la lógica excedía el alcance.

## Multiidioma (ES/EN, escalable)

- El **idioma viaja con el device token** (`device_tokens.idioma`): la app lo envía al registrar el token y lo re-sincroniza al cambiar idioma (`BaseActivity.resincronizarIdiomaPush()` invalida la caché y re-registra).
- El backend resuelve los textos con **Spring MessageSource**: `messages.properties` (ES, default) + `messages_en.properties`, claves `notif.*`. `spring.messages.fallback-to-system-locale=false` (sin esto, un servidor con locale EN serviría inglés al pedir ES).
- **Añadir un idioma** = crear `messages_xx.properties` (backend) + `values-xx/strings.xml` (Android). Sin tocar código.
- El **catálogo en BD** (ejercicios/alimentos/logros/rutinas predefinidas) se localiza con columnas `*_en` y resolución por header `Accept-Language` en los mappers (fallback a ES si no hay traducción). La app manda el header desde `ApiClient`.

## Componentes Android

| Componente | Qué hace |
|---|---|
| `GymProFitApp` (Application) | Pre-crea el canal de notificaciones push (id `"5"`) al arrancar: en background Android pinta la push él solo en el canal del meta-data del manifest, que debe existir de antemano. |
| `services/GymFirebaseMessagingService` | `onNewToken` → re-registra (rotación de Firebase); `onMessageReceived` → muestra la push en foreground (`NotificationHelper.notificarPush`). |
| `utils/PushTokenManager` | `registrar()`: obtiene el token FCM y lo envía si hay sesión y el token cambió (caché `fcm_token_enviado` en prefs); incluye el idioma. `eliminar()`: baja en logout. Todo best-effort/silencioso. |
| `network/DeviceTokenApi` | `POST`/`DELETE notificaciones/token`. |
| Hooks | Registro tras login (`LoginActivity.obtenerUsuario`); baja en logout (`BaseActivity.confirmarCerrarSesion`); re-sync al cambiar idioma. |

## Operación

- **Prod (Render)**: env var `FIREBASE_CREDENTIALS_JSON`. Verificar en logs: `Firebase Admin SDK inicializado: notificaciones push ACTIVADAS`.
- **Dev local**: `FIREBASE_CREDENTIALS_PATH` apuntando al JSON de la raíz del repo (gitignoreado).
- **CI**: sin credencial → push desactivado automáticamente, suite verde.
- El keep-alive (cron cada 10 min) mantiene despierto el free tier de Render → los crons corren puntuales (retraso máx ~1 min en programadas).

## Cómo añadir un recordatorio nuevo

1. Clave `notif.nuevo.titulo`/`notif.nuevo.mensaje` en `messages.properties` **y** `messages_en.properties`.
2. Método `@Scheduled(cron="...", zone="Europe/Madrid")` en `RecordatorioNotificacionesTask`: iterar `findDistinctUsuarioIds()`, evaluar condición, anti-spam si es recurrente, `notificacionService.crearSistema(...)` con textos resueltos vía `MessageSource` + `localeDe(usuarioId)`.
3. Test en `RecordatorioNotificacionesTaskTest` (invocar el método directamente, sin esperar al cron).
