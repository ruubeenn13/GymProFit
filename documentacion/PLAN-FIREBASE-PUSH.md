# Plan — Notificaciones Push (Firebase Cloud Messaging)

Estado: **BACKEND ✅ HECHO (2026-07-06)** · Android PENDIENTE.
Alcance decidido: **end-to-end completo** (Android recibe/muestra + backend envía push real vía Firebase Admin SDK).

## ✅ BACKEND HECHO (2026-07-06)
Service-account key aportada por el usuario (gitignoreada, raíz del repo). Implementado:
migración `V202607061200__Add_device_tokens.sql` + entidad `DeviceToken` + `DeviceTokenCreateDTO`
+ `IDeviceTokenRepository` + `IDeviceTokenService`/`DeviceTokenService` (upsert idempotente por token,
usuario del JWT) + `DeviceTokenController` (`POST`/`DELETE /notificaciones/token`) + `config/FirebaseConfig`
(init graceful: sin credencial → push desactivado, NO rompe CI; carga por env `FIREBASE_CREDENTIALS_JSON`
prod / `FIREBASE_CREDENTIALS_PATH` local) + `PushNotificationService` (envío FCM, borra tokens muertos,
tolerante a fallos) + enganche en `NotificacionService.save` (push inmediato si no es programada).
Suite 224/224 verde. Verificado en runtime: backend arranca con la key real → "push ACTIVADAS".
**Falta Android + verificación end-to-end (recibir push en el emulador).** El job @Scheduled para
notificaciones programadas queda como follow-up (ahora solo envío inmediato).

## Config Firebase (ya hecho / verificado)
- Proyecto: `GymProFit` · ID `gymprofit-app` · nº `416139974211`.
- App Android `es.pmdm.gymprofit` · appId `1:416139974211:android:7ff523d38e6799625f66f6`.
- `app/google-services.json` ✅ presente y coincide (nº proyecto, appId, paquete).
- SHA vacío: **irrelevante** para FCM push (solo hace falta para Auth/Dynamic Links).
- `AndroidManifest.xml`: `POST_NOTIFICATIONS` ✅ ya declarado.
- `build.gradle` (raíz + app): plugin `com.google.gms.google-services` ✅ + `firebase-bom:34.15.0` + `firebase-analytics` ✅.

## Lo que FALTA

### Android (cliente FCM)
1. **Dep messaging** — en `app/build.gradle`, añadir `implementation 'com.google.firebase:firebase-messaging'` (ahora solo está `firebase-analytics`).
2. **`FirebaseMessagingService`** — clase nueva (p.ej. `es.pmdm.gymprofit.services.GymFirebaseMessagingService`):
   - `onNewToken(String token)` → registrar token en backend (llamada Retrofit vía nueva `NotificacionApi`/`DeviceTokenApi`).
   - `onMessageReceived(RemoteMessage)` → construir y mostrar `Notification` en el canal (foreground; en background FCM la pinta solo si viene `notification` payload).
3. **Declarar service en manifest** con `<intent-filter>` `com.google.firebase.MESSAGING_EVENT`. Opcional: meta-data `default_notification_channel_id` + icono.
4. **Canal de notificación** (Android 8+) — crear `NotificationChannel` en `Application.onCreate()` o en `BaseActivity` (revisar si ya hay clase `Application` propia; si no, crear una y declararla en manifest).
5. **Permiso runtime `POST_NOTIFICATIONS`** (Android 13+) — pedir con `ActivityResultLauncher` en `HomeActivity.onCreate` (o `SplashActivity`).
6. **Registrar token** — al login/arranque: `FirebaseMessaging.getInstance().getToken()` → POST al backend. Guardar en `PreferencesManager` para no re-enviar si no cambió. Borrar/invalidar token en logout.
7. **Strings** — textos del canal + permiso en `values/strings.xml` y `values-en/strings.xml` (nada hardcodeado).

### Backend (registro token + envío)
Orden Spring Boot: Migración → Entidad → DTO → Mapper → Repository → Service → Controller → SecurityConfig.
1. **Migración Flyway** — nueva tabla `device_tokens` (multi-dispositivo por usuario). Nombre sugerido `V2026MMDDHHmm__Add_device_tokens.sql` (última actual: `V202607030700`). Columnas: `id` PK, `usuario_id` FK→`usuarios`, `token` VARCHAR unique, `plataforma` (ENUM/VARCHAR, default ANDROID), `fecha_registro`, `fecha_actualizacion`. Índice único en `token`.
2. **Entidad** `DeviceToken` (Lombok `@Getter @Setter @AllArgsConstructor @NoArgsConstructor`, `@ManyToOne` LAZY a `Usuario`).
3. **DTO** `DeviceTokenCreateDTO` (solo `token` + `plataforma`; el usuario sale del JWT, NO del body → evitar IDOR).
4. **Mapper** MapStruct `DeviceTokenMapper`.
5. **Repository** `IDeviceTokenRepository extends JpaRepository` — `findByToken`, `findByUsuarioId`, `deleteByToken`. Marcar `@RepositoryRestResource(exported=false)` (context-path /api ya expone repos; ver [[project_tasks_api]]).
6. **Service** `IDeviceTokenService`/`DeviceTokenService` — `registrar(usuarioId, dto)` upsert idempotente (si token existe, reasignar a usuario+refrescar fecha).
7. **Controller** `DeviceTokenController` (o ampliar `NotificacionController`) — `POST /notificaciones/token` (registrar) y opcional `DELETE /notificaciones/token` (logout). `@RestController` + `ResponseEntity`, sin `@RequestMapping("/api")` (context-path). Swagger `@Operation`/`@ApiResponse`.
8. **SecurityConfig** — `POST /notificaciones/token` requiere autenticado (usuario logueado, no guest público — decidir si guest puede). El resto de `/notificaciones/**` ya protegido.
9. **Firebase Admin SDK**:
   - Dep Maven en `pom.xml`: `com.google.firebase:firebase-admin` (verificar última versión vía context7 al implementar).
   - **Service-account key** (JSON, secreto — NO es google-services.json): Firebase Console → Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada. **⚠️ Pedírsela al usuario.**
   - Cargar credencial desde **variable de entorno** (contenido del JSON en base64 o path), NO commitear. En Render → env var (p.ej. `FIREBASE_CREDENTIALS_JSON`). Local dev → path a fichero fuera de git + `.gitignore`.
   - `@Configuration` `FirebaseConfig` → inicializa `FirebaseApp` una vez con la credencial (guard si ya inicializada).
10. **Servicio de envío** `PushNotificationService` — `enviarA(usuarioId, titulo, cuerpo, data)`: busca tokens del usuario, construye `Message`/`MulticastMessage`, envía con `FirebaseMessaging.getInstance().send(...)`. Manejar `UNREGISTERED`/`INVALID_ARGUMENT` → borrar token muerto de `device_tokens`.
11. **Enganche** — en `NotificacionService`, al crear/programar una `Notificacion`, llamar a `PushNotificationService.enviarA(...)`. Para `fecha_programada` futura: usar el `@Scheduled` ya disponible (`@EnableScheduling` ya activado por la tarea de purga [[project_backlog_pulido]]) → job que barre notifs programadas vencidas y las envía.

## Bloqueante para la siguiente sesión
- **Generar y aportar la service-account key** (JSON) desde Firebase Console → Cuentas de servicio. Sin ella no hay envío real; el resto (Android + registro token + endpoints) se puede montar sin ella.

## Notas
- Cambios ya en working tree sin commitear (Batch 2 backend independiente de esto): rate-limit `/auth/**` (#7) + purga refresh tokens (#6). Firebase analytics/plugin en build.gradle también sin commitear. Decidir si commitear Batch 2 aparte antes de empezar push.
