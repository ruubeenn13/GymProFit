# 📝 Changelog — GymProFit

Historial de cambios del proyecto (API Spring Boot + app Android). Ver también el [README](README.md).

### 2026-07-06

| Hash | Descripción |
|---|---|
| `97fb114` | feat(android): **Crashlytics** — plugin 3.0.6 + dep por BoM; captura crashes/ANRs automaticamente, sube mapping R8 en release. Verificado: crash forzado capturado y enviado. Pendiente 1 clic: abrir Firebase Console → Crashlytics para activar el panel |
| `84ad460` | fix(api): **fotos de perfil en BD (BLOB)** — tabla `fotos_perfil` aparte (no arrastra el binario en queries de usuarios) + upsert con validacion (max 5MB) + lectura desde BD; eliminado el almacenamiento en disco (FS de Render efimero = fotos perdidas en cada redeploy). Android intacto. Test e2e 3 casos (bytes identicos/404/403 IDOR). **Verificado: la foto sobrevive al reinicio del backend y el avatar la muestra**. Suite 233/233 |
| `5fad8c7` | feat: **catalogo multiidioma** — columnas `_en` en ejercicios/alimentos/logros/rutinas + traducciones seed (31 filas) + resolucion por `Accept-Language` en los mappers (@AfterMapping, fallback ES) + header en `ApiClient` + enums (nivel/grupo muscular) traducidos en adapters y DetalleEjercicio via `UIHelper.traducirNivel/traducirGrupoMuscular`. **Verificado en emulador**: catalogo 100% EN ("Bench press/Intermediate"). Suite 230/230 |
| `17f6c27` | feat: **push multiidioma** — `device_tokens.idioma` + `messages.properties`(ES)/`messages_en.properties` + `MessageSource` en los recordatorios + `fallback-to-system-locale=false` + Android envia idioma al registrar token y re-sincroniza al cambiarlo. Test: push en ingles con token "en" |
| `4628585` | feat(api): **11 generadores de recordatorios push** (@Scheduled Europe/Madrid, solo usuarios con dispositivo): 5 comidas (8/11/14/17/21h si no registro esa comida), entrenar 18h (activos), inactividad 12h (anti-spam 4d), resumen semanal dom 20h, logro proximo 20:30 (anti-spam 7d), medicion mensual 10h (30d), objetivo por vencer 12:30 (3d). `crearSistema` sin SecurityContext + anti-spam por titulo. Test integracion 3 casos |
| `18ca80e` | fix(deploy): heap JVM al 60% del contenedor (Render 512MB; el default ~128MB se ahogaba arrancando con firebase-admin → timeout del health check en el primer deploy) |
| `15a1865` | feat(api): **push programadas** — job `NotificacionProgramadaTask` @Scheduled cada 60s: envia las notificaciones con `fecha_programada` vencida y `push_enviada=false` y las marca enviadas (columna nueva, DEFAULT TRUE para historicas). `save()`: inmediata=push al momento; programada=pendiente para el job. **Verificado en emulador**: programada a +1 min → push visible ~41s tras su hora. Suite 224/224 |
| `8274e30` | feat(android): **notificaciones push FCM — cliente**. `firebase-messaging` + `GymFirebaseMessagingService` (onNewToken/onMessageReceived) + `GymProFitApp` (Application nueva; pre-crea canal push "5" para push en background) + `PushTokenManager` (registro tras login con caché, baja en logout) + `DeviceTokenApi` + `NotificationHelper.notificarPush`. **PUSH END-TO-END VERIFICADA EN EMULADOR**: login→token registrado (200)→POST /notificaciones→backend envía vía Admin SDK→notificación visible en la barra con el logo de la app |
| `c3495b8` | feat(api): **notificaciones push FCM — BACKEND**. Migración `device_tokens` + entidad/DTO/repo/service (`DeviceTokenService`, upsert idempotente, usuario del JWT) + `DeviceTokenController` (`POST`/`DELETE /notificaciones/token`) + `FirebaseConfig` (init **graceful**: sin credencial → push desactivado, no rompe CI; carga por env var) + `PushNotificationService` (envío FCM, borra tokens muertos, tolerante a fallos) + enganche en `NotificacionService.save`. `firebase-admin 9.8.0`. Suite 224/224. Verificado runtime: backend con key real → "push ACTIVADAS". Falta Android |
| `6a5ab8c` | build(android): **pulido #8 — minify + shrinkResources + ProGuard/R8**. Release pasa de ~12.8 MB a ~5.1 MB (~60%). Reglas keep para Gson (`model.**` + enums), interfaces Retrofit (`network.**`), `BooleanNumericAdapter`, atributos Signature/Annotation + reglas Gson/Retrofit/OkHttp. **Verificado con el APK release minificado en emulador**: login + Rutinas renderiza todo (Gson OK bajo R8), sin crashes |
| `59d6f85` | fix(test): tests IDOR siembran su propio catálogo (ejercicio/alimento) — arregla 16 errores `IndexOutOfBounds` en CI (la MariaDB efímera solo trae el seed de Flyway, no el catálogo; en dev local sí existía) |
| `4ceea56` | test(api): **pulido #9 — tests e2e del 403 IDOR (ownership)** — cierra el hueco de la auditoría. La protección `SecurityUtils.checkOwnership` ya existía pero sin test e2e (los controller-tests mockean el service → nunca corría). Nueva infra `AbstractOwnershipTest` (@SpringBootTest sin mocks + @Transactional rollback; siembra owner/attacker USER reales; `runAs()` + `@WithUserDetails(setupBefore=TEST_EXECUTION)`). 7 clases / 29 casos (get/delete/lista ajena→403 + control positivo propio→200): Comida, MedicionCorporal, Notificacion, AlimentoComida, EjercicioRealizado, ProgresoEjercicio, RutinaEjercicio. Alimento excluido (su service no comprueba ownership en lectura, solo rol). **Suite 224/224 verde.** |
| `f047612` | feat(android): **pulido #5 batch 4 (FINAL) — cierra #5** — feedback carga/errores en las 7 pantallas admin (AdminActivity/Usuarios/Ejercicios/Rutinas/Alimentos + EditarEjercicioAdmin + EditarRutinaAdmin). Spinner en cargas y submits + `UiFeedback` en fallo. **Verificado en emulador con sesión ADMIN**: panel de stats, lista de ejercicios, toggle desactivar (DELETE 200)→reactivar (PUT 200) con spinner+recarga, sin crashes. **#5 COMPLETO**: helpers reutilizables + ~25 Activities (lectura/escritura/detalle/admin) verificadas end-to-end |
| `53a492f` | feat(android): **pulido #5 batch 3** — feedback carga/errores en pantallas de escritura y detalle (10 Activities): Registrar Sesión/Medición, Editar Rutina/Perfil, Crear/Añadir Alimento, Añadir Ejercicios, Comida, DetalleRutina + `MedicionesActivity.patchCampo` (edición inline; hueco detectado probando en emulador). Patrón: spinner en el submit + `UiFeedback.toastError` en fallo. **Verificado end-to-end**: crear medición (POST 200→éxito), editar Height inline (PATCH 200→180cm; y con backend caído → timeout 15s → spinner se oculta + toast, sin colgarse). Pendiente #5: pantallas admin |
| `19049ef` | feat(android): **pulido #5 batch 2** — feedback carga/errores en las 4 pantallas de lectura restantes (reutiliza helpers de batch 1). Sesiones/Logros/Mediciones: spinner + toast en fallo real (ya tenían `tvVacio`). Ejercicios: spinner + **empty state nuevo** (`tvEmpty` sobre RV) + toast + `actualizarEstadoVacio()` tras filtros. **Verificado end-to-end en emulador+API+DB**: Sesiones ("No sessions recorded"), Logros (catálogo), Mediciones ("No measurements recorded"), Ejercicios (lista + empty por búsqueda "Nothing here yet"), sin crashes. Pendiente #5: pantallas de escritura + Comida/DetalleRutina/admin |
| `701666f` | fix(android): `UiFeedback` ignora **404** — verificado en emulador que la API devuelve 404 (no 200+[]) en colecciones vacías (`/sesiones/usuario/{id}`, `/rutinas/usuario/{id}/activas`); mapearlo a toast "algo salió mal" avisaba de error a usuarios sin datos (Home lo disparaba). Ahora 404 = estado vacío benigno (sin toast), como 401. Solo -1 (cold-start) y ≥500 muestran toast. **Batch 1 probado end-to-end en emulador+API+DB reales**: Home, Rutinas (spinner "Loading…" + empty state "Nothing here yet" + toast al caer backend), Perfil, Nutrición, sin crashes |
| `b77551f` | feat(android): **pulido #5 batch 1** — feedback carga/errores. Helpers reutilizables `utils/LoadingDialog` (spinner modal overlay, `dialog_loading.xml`, sin tocar 30 layouts), `utils/UiFeedback.toastError(code,msg)` (mapea -1→cold-start/500→servidor/resto→genérico, strings ES+en), `network/UiApiCallback<T> extends ApiCallback` (auto hide+toast). Pantallas: Home (fallback "—"+toast), Rutinas (spinner+empty state `tvEmpty`+toast), Perfil (toast en fetch principal), Nutrición (404 vacío legítimo vs error real). Compila verde. Pendiente batch 2 + verificación emulador |

### 2026-07-04

| Hash | Descripción |
|---|---|
| `a64fc0d` | docs: plan turnkey de UX feedback carga/errores Android (`PLAN-UX-FEEDBACK.md`) — backlog #5, a ejecutar la siguiente sesión (helpers `LoadingDialog`/`UiFeedback`, empty states, sin tocar 30 layouts) |
| `d2742ff` | refactor(android): **cierra Retrofit etapa 2** — última ruta con AsyncTask+HttpURLConnection migrada a interfaz tipada (`UsuarioApi.descargarFoto` → `Call<ResponseBody>`, foto de perfil por `enqueue`+`ApiCallback`, token por interceptor). Toda la red de la app pasa ya por Retrofit tipado |
| `2c1f502` | feat(api): pulido #5/#6 — `AuthRateLimitFilter` (ventana fija por IP, 15 req/60s sobre `/auth/login\|register\|guest\|refresh`, 429 + `Retry-After`, `X-Forwarded-For` de Render, antes del filtro JWT; off en ci/dev para no romper los `@SpringBootTest`) + `RefreshTokenCleanupTask` (`@Scheduled` diario 04:00 que purga refresh tokens revocados/expirados; nueva query + `@EnableScheduling`) |
| `d68df98` | test(api): `AuthRateLimitFilter` — 3 tests aislados (sin BD): 15 pasan y la 16ª → 429, `enabled=false` no limita, IPs con contadores independientes. Verificación runtime del rate-limit |
| `2315130` | chore(android): scaffolding Firebase (FCM push) sin implementar — plugin `google-services` + `firebase-bom` 34.15.0 + `firebase-analytics` (`google-services.json` gitignored) + `PLAN-FIREBASE-PUSH.md` (plan end-to-end; bloqueante = service-account key) |

### 2026-07-03

| Hash | Descripción |
|---|---|
| `e14ecdb` | fix(android): pulido #1/#4 — cold-start de Render (readTimeout 60s + callTimeout 60s, evita SocketTimeout en la 1ª petición tras dormir) + quitado permiso `READ_CONTACTS` (el picker del sistema no lo requiere) |
| `2ceaa70` | fix(api): pulido #2/#3 — 400 (no 500) en requests mal formadas (3 `@ExceptionHandler`) + logs solo a consola en perfil prod (FS efímero) |
| `0c881d6` | ci: keep-alive — cron cada 10 min que pinguea `/api/actuator/health` para que Render (free) no se duerma (~730h/mes < 750h gratis) |
| `4d76c00` | refactor(android): Retrofit etapa 2 **F8b limpieza final** — borrados `API.java` y `UtilJSONParser` (~375 líneas de parseo manual), `UtilREST` reducido a solo tokens, `RawApi` fuera de `ApiClient`. **Migración a Retrofit tipado COMPLETA** (toda la red por interfaces por dominio + Gson) |
| `886f01a` | refactor(android): Retrofit etapa 2 F8a — barrido de 9 llamadas cross-domain residuales a las interfaces tipadas (Sesiones→Rutinas/Logros, Perfil→Medición, Detalle/EditarRutina→Ejercicios) |
| `e3b1980` | refactor(android): Retrofit etapa 2 F7.5 — dominio Usuario/Perfil tipado (`UsuarioApi`: getPorId/estadisticas/patch/subirFoto @Multipart); Perfil/EditarPerfil/Onboarding + restos de usuario en Mediciones/ResumenSesión. Validado contra API real |
| `10702d3` | refactor(android): Retrofit etapa 2 F7 — Auth tipado (`AuthApi`/`UsuarioApi`, POJO `TokenResponse` con `rolPrincipal()`); login/registro/logout migrados manteniendo el guardado de sesión idéntico (UtilREST + prefs). Validado contra API real |
| `a6f079a` | ci: no disparar CI en commits solo-documentación (`paths-ignore` en push) — evita runs "canceled" cuando el commit de changelog seguía al de código |
| `a4c9b89` | refactor(android): Retrofit etapa 2 F6 — dominio Admin tipado (`AdminApi`, 7 pantallas); paginación (lista plana), búsquedas jOOQ, POJO `EstadisticasGlobales`, `BooleanNumericAdapter` para `activo` Byte→boolean. Validado contra API real como ADMIN |
| `07a1dde` | refactor(android): Retrofit etapa 2 F5 — dominio Nutrición tipado (`AlimentoApi`/`ComidaApi`/`AlimentoComidaApi`); comidas + alimentos + alimentos-comida, sin `@SerializedName` (claves JSON = campos POJO) |
| `6d70516` | refactor(android): Retrofit etapa 2 F4 — dominio Rutinas + rutinas-ejercicios tipado (`RutinaApi`, POJO `RutinaEjercicio`, `@SerializedName("esPredefinida")`); callback-hell de 2 niveles aplanado |
| `83f1548` | refactor(android): Retrofit etapa 2 F3 — dominio Sesiones tipado (`SesionApi`) + `utils/FechaUtils` (el formateo de fechas pasa a la vista; arregla la regresión de fecha ISO cruda en Mediciones/Sesiones) |
| `957f9a5` | refactor(android): Retrofit etapa 2 F2 — dominios Logros y Ejercicios tipados (`LogroApi`/`EjercicioApi`, POJO `UsuarioLogro`, `@SerializedName("caloriasQuemadas")`) |
| `593d260` | refactor(android): Retrofit etapa 2 F0+F1 — infra tipada con Gson (`ApiClient.service()`, `ApiCallback<T>`, cuerpos parciales `Map`) + piloto Mediciones migrado |
| `0020301` | test(api): +56 tests unitarios de nutrición y mediciones (Alimento/AlimentoComida/MedicionCorporal/Comida); suite 192/192 verde |
| `2c9a09f` | feat(android): `BASE_URL` por buildType — debug→API local (`10.0.2.2:8080`), release→Render (prod) |
| `eae7647` | docs: **API en producción** — desplegada y verificada en `https://gymprofit-api.onrender.com` |
| `a7742b0` | fix(db): PK compuesta en `usuario_roles` (Aiven MySQL exige PK) + CA de Aiven empaquetado para TLS `verify-full` |
| `1ca2e7c` | feat(deploy): Actuator (`/actuator/health`) + `application-prod.properties` 12-factor (env vars) + Dockerfile multi-stage + `render.yaml` (Render + Aiven MySQL) |
| `861d4f4` | fix(db): `alimentos.proteinas` INT→DECIMAL(5,2) — drift esquema↔entidad que tumbaba Flyway en una BD construida desde cero (CI/Aiven) |
| `df9ec8c` | ci: `LOG_DIR` de logback overridable (la ruta del server AWS no existe en el runner) |
| `b086813` | ci: fuentes jOOQ versionadas en `src/generated/jooq` + perfil `ci` (un clone/CI compilan offline sin BD) |
| `5f7fada` | ci: workflow de GitHub Actions (build+test de la API con MariaDB efímera) — gate del auto-merge de Dependabot vía ruleset |

### 2026-07-02

| Hash | Descripción |
|---|---|
| `436ebbc` | chore(build): mover OWASP dependency-check a un perfil `security-scan` (no romper los builds offline) |
| `3b8e2ed` | refactor(android): migrar el motor de red a Retrofit + OkHttp (Fase 3, etapa 1) — nuevo `ApiClient` con interceptor de token y `Authenticator` que renueva el JWT en un 401 de forma transparente; `UtilREST` pasa a delegar en él manteniendo su fachada (API.java y las Activities NO cambian). Fuera AsyncTask/HttpURLConnection y el hack de reflexión para PATCH. Verificado en emulador (login + GET autenticado + refresh) |
| `5cd740a` | refactor(api): respuestas de conteo/existencia con DTOs tipados — nuevos `CountDTO`/`ExistsDTO` sustituyen los `Map<String,Object>` crudos en 23 endpoints (contrato Swagger claro). Android no consume estos endpoints |
| `f34cd83` | refactor(api): más sueltos de la auditoría — `IllegalArgumentException` (enum inválido en filtros) ahora devuelve 400 en vez de 500; `AdminController` ya no llama a repos jOOQ directamente (pasa por los services de dominio); Swagger deshabilitado en producción (springdoc off) |
| `981284c` | ci(dependabot): auto-merge de PRs de Dependabot (aprobar + auto-merge squash de patch/minor; los major quedan a revisión manual) |
| `1659d29` | chore(security): añadir OWASP `dependency-check-maven` (escaneo de CVEs bajo demanda, `failBuildOnCVSS=8`) y mejorar `dependabot.yml` (grouped updates minor/patch + labels) |
| `4429b89` | chore(deps): subir dependencias vulnerables — `spring-boot-starter-parent` 3.5.7→3.5.16 (parchea transitivas: jackson, logback, tomcat…), lombok 1.18.38→1.18.46, y quitar el override de `spring-boot-starter-validation` (hereda del parent). Añadido `.github/dependabot.yml` para actualizaciones automáticas. 136 tests verdes |
| `b069bf2` | chore(security): sueltos de la auditoría — CORS con lista blanca configurable (fuera el wildcard `*` y los 16 `@CrossOrigin("*")`), `jwt.secret` por variable de entorno (`${JWT_SECRET}` en prod, literal en dev), y compilador Maven a Java 21 (coherente con `java.version`) |
| `506480e` | feat(android): endurecer seguridad (auditoría Fase 3) — token/refresh cifrados con `EncryptedSharedPreferences`, `network-security-config` que prohíbe HTTP en claro salvo hosts de desarrollo, exclusión del almacén cifrado del backup, y fix del leak de Activity en la descarga de avatar. Verificado en emulador |
| `407891e` | feat(android): integración de refresh token — guarda access+refresh, renueva el access de forma transparente en un 401 y reintenta la petición, y revoca el refresh en el logout. APK compila |
| `5627814` | feat(api): refresh tokens (access JWT corto de 30 min + refresh opaco persistido de 30 días, con rotación y revocación). Nuevos `POST /auth/refresh` y `POST /auth/logout`; login/guest devuelven `refreshToken`. Verificado end-to-end |
| `08f2d6f` | perf(api): relaciones `@ManyToOne` a LAZY (14) + `@Transactional(readOnly=true)` a nivel de service para el mapeo seguro; roles de Usuario se mantienen EAGER por seguridad (Spring Security) |
| `d2c25a2` | chore(api): silenciar warnings de MapStruct con `@Mapping(ignore)` explícitos |
| `9e9af10` | fix(api): bugs menores — `/notificaciones/ordenadas` ahora ordena, count de alimentos-comida en clave correcta, mensaje de tipo de objetivo con concatenación real, ruta `/progreso-ejercicios` coherente, `@Schema` de comidas corregido |
| `bc22945` | fix(api): quitar `try/catch(Exception)+Map` de los 13 controllers (31 bloques) — las excepciones propagan al `@ControllerAdvice` y devuelven el código correcto (404/400/403) en vez de 500 |
| `53edb64` | test(api): mockear `SecurityUtils` en tests de service (arreglo tras Fase 1 C2); suite 132/132 verde |
| `2fd1fdc` | refactor(api): migrar los 16 repositorios de `CrudRepository` a `JpaRepository` y eliminar los 15 casts `(List<X>)` en los services (auditoría Fase 2, base para paginación) |
| `544838f` | chore(security): externalizar credenciales del pom (jOOQ codegen → propiedades Maven) y re-versionar `pom.xml` (auditoría Fase 1, ítem 4) |
| `2c27e96` | docs(comentarios): documentados 125 XML de Android (layouts, values, menu, drawable, mipmap, manifest) + logback con cabecera y comentarios de sección, sin tocar contenido |
| `932cc9d` | docs(comentarios): documentados los 280 archivos .java (API + Android) con cabecera de bloque por archivo + comentarios inline por método/campo, sin tocar código |
| `8ba39a7` | fix(security): auditoría Fase 1 — cierre de escalada ADMIN (C1) e IDOR sistémico (C2) vía SecurityUtils, no fuga de causa en errores, 401 en credenciales, null-check en UsuarioService.patch |

### 2026-05-28

| Hash | Descripción |
|---|---|
| *(pendiente)* | feat(android): Onboarding5Activity para nivel de experiencia, fix persistencia onboarding, fix PUT→PATCH en OnboardingResumen, EXPERTO en spinner EditarPerfil |

### 2026-05-26

| Hash | Descripción |
|---|---|
| `2153cdf` | feat(android): compartir app vía SMS — permiso `READ_CONTACTS` en runtime, selector de contactos, SMS pre-rellenado con link de la app |
| `c502be0` | feat(android): AcercaDeActivity — pantalla "Acerca de" con logo adaptativo tema, info extendida app y desarrollador, email clickable |
| `49f8c68` | feat(android+api): nutrición completa — NutricionActivity macros dinámicos, ComidaActivity log diario, AnadirAlimentoActivity, CrearAlimentoActivity, AdminAlimentosActivity, BottomSheet menus (UIHelper.mostrarBottomMenu reemplaza todos los PopupMenu), API: AlimentoComidaDTO enriquecido con macros totales, admin alimentos jOOQ |
| `0dc0fb6` | feat(android): ComidaActivity, AlimentoComidaAdapter y layouts nutrición |
| `e65a817` | feat(android): parsers JSON para Alimento, Comida y AlimentoComida |
| `05691d7` | feat(android): modelos Alimento/Comida/AlimentoComida y 13 métodos API nutrición |
| `b0536c5` | feat(android): guardar peso/altura/edad en prefs y recalcular macros al editar perfil |
| `067f401` | feat(api): jOOQ búsqueda admin alimentos, fix activarAlimento, permisos alimentos |
| *(anterior)* | feat(android): menú "Contáctanos", rediseño card entrenamientos, fix bottom nav |
| `43bd0d2` | feat: añadir archivos restantes del anterior commit |
| `13ef397` | feat(android+api): foto de perfil (galería+cámara, FileProvider, multipart), fix BottomNav dark mode |

### 2026-05-25

| Hash | Descripción |
|---|---|
| `28341e8` | feat(android): pesos por serie en RegistrarSesion, stats reales en Home, admin edición completa, menú long-press contextual rutinas |
| `2e9b741` | feat(android): DetalleEjercicioActivity con vídeo local y stats card |
| `1e4fb60` | feat(admin): panel de administración completo en Android y fixes de API |
| `bf49bbe` | fix(android): cargar solo rutinas activas del usuario tras eliminar |
| `961f804` | feat(android): logo PNG dark/light en splash/login/registro, fix BottomNav sin animación, fix diálogo idioma login |
| `e2d8c0c` | feat(android): BaseActivity con menú tema/idioma, fix altura mediciones, mejoras UI |

### 2026-05-22

| Hash | Descripción |
|---|---|
| *(pendiente)* | feat: notificaciones (4 canales), cálculo automático calorías sesión, fix logros duplicados, ic_delete, btnGuardar fijo al fondo |
| *(pendiente)* | feat(android): ResumenSesionActivity; fix logros: evaluarLogros retorna nuevos logros; RatingBar valoración; calorías calculadas desde ejercicios |
| *(pendiente)* | fix: fecha_fin NOT NULL en SesionEntrenamientoService; completada leída del DTO; evaluarLogros en save() |

### 2026-05-21

| Hash | Descripción |
|---|---|
| `3555fe5` | feat(android): flujo crear rutina (3 pasos), detalle y edición de rutina |
| `a92cefb` | feat: añadir EditarPerfilActivity con PATCH /usuarios/{id}; fix mapper y service |
| `b436ff9` | fix: corregir filtros de ejercicios y rutinas; ampliar seed con rutinas por nivel |
| `94110ce` | fix(security): devolver 401 en JWT expirado/inválido en lugar de 500 |
| `7f2ce1d` | feat: añadir pantallas Sesiones, Mediciones, Logros y Admin; fix onboarding repetido en login |

### 2026-05-17

| Hash | Descripción |
|---|---|
| *(archivado)* | fix: saltar onboarding en login si usuario ya tiene datos; activar usuarios con activo=NULL; añadir logging a UtilREST y PerfilActivity |

### 2026-05-16

| Hash | Descripción |
|---|---|
| `0e26f83` | **feat:** Migrar capa de red de Retrofit a arquitectura 4 capas (UD06) — `UtilREST` + `UtilJSONParser` + `API.java`. Añadir pantallas Sesiones, Mediciones, Logros y Admin. Eliminar Retrofit/OkHttp/Gson |
| `00c1be2` | **fix:** Corregir tipo de columna `altura` en tabla `usuarios`: `DECIMAL(3,2)` → `DECIMAL(5,2)` para admitir valores en cm (ej: 178.00) |
| `a63a6b7` | **feat:** Añadir script SQL de datos de ejemplo (`seed_datos_ejemplo.sql`) |
| `a429207` | **test:** Añadir suite completa de 132 tests y corregir tests existentes |
| `0a063fa` | **feat:** Añadir Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, etc.) a todos los DTOs de entrada |
| `74db20a` | **feat:** Endpoints admin con filtros dinámicos jOOQ (`GET /admin/usuarios`) y estadísticas globales (`GET /admin/estadisticas-globales`) |
| `23e1f7b` | **feat:** Implementar sistema de logros con evaluación automática tras cada sesión. Entidades `Logro` y `UsuarioLogro`, `LogroService` con listeners de dominio |
| `70d4103` | **feat:** Endpoint `GET /usuarios/{id}/estadisticas` con jOOQ (sesiones totales, calorías, duración media) |
| `1bd3495` | **feat:** Implementar sistema de roles (ADMIN, USER, GUEST) con `DataInitializer` y `SecurityConfig` siguiendo el patrón de clase |

### 2026-05-14

| Hash | Descripción |
|---|---|
| `791f863` | **fix:** Corregir routing con Spring Data REST, validación de enums en PATCH y contexto de seguridad |
| `d1d5cb3` | **feat:** Añadir endpoints PATCH a los 13 controladores de entidades con DTOs específicos |
| `4fd168e` | **feat:** Migrar campo `objetivo` de `String` a enum `TipoObjetivo`. Fix permisos de rutinas en SecurityConfig |
| `347fa86` | **feat:** Migrar campo `objetivo` de `Usuario` a `TipoObjetivo` enum (entidad + DTOs + mapper) |

### 2026-05-05

| Hash | Descripción |
|---|---|
| `eec1f48` | **docs:** Crear README.md inicial del proyecto |
| `75b5f52` | **feat:** Añadir pantallas de onboarding (4 pasos + resumen), pantalla de registro, signing config y .gitignore |
| `449287b` | **feat:** Añadir `application-example.properties` como plantilla de configuración |
| `f6ec758` | **fix:** Corregir ruta de Swagger UI con context-path `/api` y permitir acceso público en Security/JWT |

### 2026-04-13

| Hash | Descripción |
|---|---|
| `ae17963` | **feat:** Conectar la API al proyecto Android con Retrofit + interceptor JWT |
| `3ce0427` | **feat:** Rediseñar `SplashActivity` con verificación de sesión y animación de fade-in |
| `03f38d2` | **feat:** Rediseñar `PerfilActivity` con datos del usuario, items clickables y sección de configuración |

### 2026-04-12

| Hash | Descripción |
|---|---|
| `1c7afae` | **feat:** Mejorar `NutricionActivity`, añadir `UIHelper` con toasts y diálogos personalizados |
| `5cf1545` | **feat:** Añadir `RutinasActivity` con RecyclerView y `CrearRutinaActivity` |

### 2026-04-10

| Hash | Descripción |
|---|---|
| `4cdd493` | **feat:** Mejorar diseño visual general y añadir `EjerciciosActivity` con filtros |

### 2026-04-08

| Hash | Descripción |
|---|---|
| `820cba0` | **fix:** Corregir typos en controllers jOOQ |
| `d9e6c09` | **feat:** Implementar jOOQ con consultas avanzadas para ejercicios y usuarios |
| `dd9c3d8` | **test:** Tests de integración de `RutinaController` |
| `c396aaf` | **test:** Tests de integración de `EjercicioController` |
| `9b8d789` | **test:** Tests de integración de `AuthController` |
| `654e31f` | **fix:** Corregir `SesionEntrenamientoServiceTest` |
| `b93b588` | **fix:** Corregir typos en `SesionEntrenamientoService` |
| `e7ff1f4` | **test:** Tests unitarios de `SesionEntrenamientoService` |

### 2026-04-06

| Hash | Descripción |
|---|---|
| `70bfbf9` | **test:** Tests unitarios de `RutinaService` |
| `5144b09` | **test:** Tests unitarios de `EjercicioService` |
| `37706ea` | **test:** Tests unitarios de `UsuarioService` |
| `6ca99c2` | **test:** Tests unitarios de `AuthService` |

### 2026-04-05

| Hash | Descripción |
|---|---|
| `2235570` | **feat:** Implementar autenticación JWT con roles y filtro de seguridad |
| `7e8eaa3` | **feat:** Añadir entidad `Role` y adaptar `Usuario` para implementar `UserDetails` |
| `fbb333e` | **fix:** Corrección de typos y errores en controllers |
| `a249ec9` | **feat:** Módulo `Notificacion` completo (entidad, DTO, repositorio, servicio, controller) |

### 2026-04-02

| Hash | Descripción |
|---|---|
| `7bce1ce` | **feat/fix:** Módulo `ProgresoEjercicio` completo + fix mapper `MedicionCorporal` |
| `d802eee` | **feat/fix:** Módulo `MedicionCorporal` completo + fix `FetchType` en relaciones JPA + fix logs |

### 2026-03-27

| Hash | Descripción |
|---|---|
| `4bac430` | **feat/fix:** Módulo `ObjetivoPersonal` completo + enum `TipoObjetivo` + fix configuración Logback |

### 2026-03-26

| Hash | Descripción |
|---|---|
| `1a9ad13` | **feat/fix:** Módulo `RutinaEjercicio` completo + fix logs |

### 2026-03-25

| Hash | Descripción |
|---|---|
| `5fea0f7` | **feat/fix:** Módulo `EjercicioRealizado` completo + correcciones generales |
| `5c87db8` | **feat:** Ampliar módulos `Alimento` y `Comida` con métodos count y estadísticas |

### 2026-03-23

| Hash | Descripción |
|---|---|
| `0d024b5` | **feat:** Implementar módulo `SesionEntrenamiento` completo con CRUD y validaciones |

### 2026-03-20

| Hash | Descripción |
|---|---|
| `19f97e9` | **feat:** Implementar módulo `AlimentoComida` completo con CRUD y validaciones |

### 2026-03-18

| Hash | Descripción |
|---|---|
| `2a467c6` | **feat:** Implementar módulo `Comida` completo con CRUD y validaciones |

### 2026-03-16

| Hash | Descripción |
|---|---|
| `ff36ed2` | **feat:** Implementar módulo `Alimento` completo con CRUD y validaciones |

### 2026-03-05

| Hash | Descripción |
|---|---|
| `f926ac2` | **feat:** Implementar módulo `Rutina` completo con CRUD y validaciones |

### 2026-03-04

| Hash | Descripción |
|---|---|
| `0995235` | **feat:** Configurar sistema de logs con Logback |
| `ea2e8f9` | **feat:** Módulo `Ejercicio` completo y funcional |

### 2026-03-03

| Hash | Descripción |
|---|---|
| `be19780` | **feat:** Implementar módulo `Usuario` completo con excepciones personalizadas |
| `c38fb5f` | **feat:** Crear DTOs para transferencia de datos de todas las entidades |

### 2026-03-02

| Hash | Descripción |
|---|---|
| `2b8fbb8` | **feat:** Crear repositorios JPA para las 13 entidades |

### 2026-02-27

| Hash | Descripción |
|---|---|
| `63902b7` | **feat:** Configuración inicial del proyecto Spring Boot y modelo de datos completo |
| `069c2b4` | **feat:** Commit inicial — proyecto Spring Boot generado |

### 2026-02-20

| Hash | Descripción |
|---|---|
| `5db17ef` | **feat:** Implementar navegación completa y sistema de preferencias (Android) |

### 2026-02-19

| Hash | Descripción |
|---|---|
| `aae7262` | **feat:** Implementar Splash, Login y selector de tema/idioma (Android) |
| `a964ee5` | **feat:** Commit inicial de la app Android |
| `6c76145` | **feat:** Estructura inicial del TFG: carpetas db, api y app |

