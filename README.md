<div align="center">

# 💪 GymProFit

### Aplicación Android de gestión de entrenamientos y nutrición

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white)

*Trabajo de Fin de Grado — CFGS Desarrollo de Aplicaciones Multimedia (2º DAM)*
*Autor: Rubén Juan Candela*

</div>

---

## 📋 Tabla de contenidos

- [Descripción general](#-descripción-general)
- [Estructura del repositorio](#-estructura-del-repositorio)
- [App Android](#-app-android-appgymprofit)
- [API REST](#-api-rest-apigymprofit-api)
- [Base de datos](#-base-de-datos-db)
- [Tecnologías](#-tecnologías)
- [Requisitos previos](#-requisitos-previos)
- [Instalación y configuración](#-instalación-y-configuración)
- [Despliegue en producción](#-despliegue-en-producción)
- [Changelog](CHANGELOG.md)

---

## 📱 Descripción general

**GymProFit** es un proyecto full-stack compuesto por una aplicación Android nativa y una API REST propia. Permite a los usuarios registrarse, gestionar sus rutinas de entrenamiento, registrar sesiones, consultar un catálogo de ejercicios, hacer seguimiento de mediciones corporales, ver sus logros desbloqueados, llevar un registro nutricional diario y calcular sus necesidades nutricionales personalizadas según su perfil físico.

Incluye además **notificaciones push** (Firebase Cloud Messaging) con 11 recordatorios automáticos programados en el servidor, soporte **multiidioma completo (ES/EN)** — interfaz, push y catálogo de la BD — y autenticación con **refresh token** (renovación de sesión transparente). La API está **desplegada en producción** en Render + Aiven MySQL (ver [Despliegue en producción](#-despliegue-en-producción)).

El sistema distingue tres roles de usuario: **GUEST**, **USER** y **ADMIN**, con permisos diferenciados para cada operación de la API, comprobación de *ownership* en los recursos de usuario y rate-limiting en los endpoints de autenticación.

---

## 🗂️ Estructura del repositorio

```
GymProFit/
├── 📂 app/            # Aplicación Android (Android Studio - Java)
├── 📂 api/            # API REST backend (Spring Boot - Java)
├── 📂 db/             # Script SQL de referencia (el esquema real lo crean las migraciones Flyway)
└── 📂 documentacion/  # Guías: despliegue, entorno de desarrollo, notificaciones, auditoría y planes
```

---

## 📱 App Android (`app/GymProFit`)

Aplicación Android nativa desarrollada en Java con Android Studio. Consume la API REST mediante **Retrofit2 + OkHttp3 + Gson** con interfaces tipadas por dominio, guarda los tokens (access + refresh) cifrados en `EncryptedSharedPreferences` vía `PreferencesManager` y recibe **notificaciones push** de Firebase Cloud Messaging. Interfaz completa en **español e inglés**.

### Flujo de navegación

```
SplashActivity
     ↓
LoginActivity ←→ RegistroActivity
     ↓ (primer acceso)
Onboarding (1 → 2 → 3 → 4 → 5 → Resumen)
     ↓
HomeActivity (navegación inferior)
  ├── EjerciciosActivity → DetalleEjercicioActivity (vídeo + instrucciones)
  ├── RutinasActivity
  │     ├── CrearRutinaActivity → AnadirEjerciciosActivity → ResumenCrearRutinaActivity
  │     └── DetalleRutinaActivity → EditarRutinaActivity
  ├── NutricionActivity → ComidaActivity → AnadirAlimentoActivity / CrearAlimentoActivity
  └── PerfilActivity → EditarPerfilActivity
        ├── SesionesActivity → RegistrarSesionActivity → ResumenSesionActivity
        ├── MedicionesActivity → RegistrarMedicionActivity
        ├── LogrosActivity
        ├── AcercaDeActivity
        └── AdminActivity  (solo ROLE_ADMIN)
              ├── AdminUsuariosActivity
              ├── AdminRutinasActivity
              ├── AdminEjerciciosActivity
              └── AdminAlimentosActivity
```

---

### 📁 Estructura de la App

#### `app/src/main/java/es/pmdm/gymprofit/`

---

#### 🧩 `model/`

POJOs puros sin dependencias de red ni de Android.

| Clase | Descripción |
|---|---|
| `usuario/Usuario.java` | Perfil completo: id, username, email, peso, altura, edad, nivelExperiencia, objetivo, activo, rol |
| `ejercicio/Ejercicio.java` | Ejercicio con nombre, grupo muscular, dificultad, descripción, calorías e icono |
| `rutina/Rutina.java` | Rutina con nombre, nivel, descripción, duración y calorías |
| `sesion/SesionEntrenamiento.java` | Sesión con fechaInicio, fechaFin, duracionMinutos, caloriasQuemadas, notas, completada |
| `logro/Logro.java` | Logro con id, nombre, descripción, icono y tipo |
| `objetivo/ObjetivoPersonal.java` | Objetivo personal con progreso y fechas |
| `medicion/MedicionCorporal.java` | Medición corporal con peso, altura, IMC, grasa, músculo, perímetros |

---

#### 🌐 `network/`

Capa de red basada en **Retrofit2 + OkHttp3 + Gson**, con una interfaz tipada por dominio.

**`ApiClient.java`** — Singleton que construye el `Retrofit` (con la `BASE_URL` de `BuildConfig`) y cachea las interfaces vía `service(Clase.class)`. Su `OkHttpClient` lleva tres piezas: `AuthInterceptor` (inyecta `Authorization: Bearer <access>` en cada petición), `TokenAuthenticator` (ante un **401** renueva el access token con el refresh token y reintenta la petición de forma transparente) y el header `Accept-Language` con el idioma de la app (para recibir el catálogo localizado).

**Interfaces por dominio** — `AuthApi`, `UsuarioApi`, `RutinaApi`, `SesionApi`, `EjercicioApi`, `LogroApi`, `MedicionApi`, `ObjetivoApi`, `AlimentoApi`, `ComidaApi`, `AlimentoComidaApi`, `AdminApi` y `DeviceTokenApi`. Cada una declara sus endpoints con anotaciones Retrofit (`@GET`, `@POST`, `@PATCH`, `@Multipart`…) y trabaja directamente con los POJOs de `model/` (de)serializados por Gson. El contrato completo de endpoints está documentado en **Swagger** (`/api/swagger-ui.html`).

**`ApiCallback<T>` / `UiApiCallback<T>`** — Callback común sobre `enqueue` que unifica `onSuccess(T)` / `onError(code, msg)`. La variante `UiApiCallback` además oculta automáticamente el `LoadingDialog` y muestra el error con `UiFeedback`.

**`BooleanNumericAdapter`** — TypeAdapter de Gson para los campos `activo` que la API devuelve como `0/1`.

**`UtilREST.java`** — Reducido a la gestión de tokens de sesión (`setToken` / `getToken` / `clearToken`, access + refresh); la persistencia cifrada vive en `PreferencesManager`.

---

#### 🖼️ `ui/activities/`

| Activity | Descripción |
|---|---|
| `SplashActivity` | Carga inicial. Restaura la sesión (tokens cifrados en `EncryptedSharedPreferences`) y redirige a Home o Login |
| `LoginActivity` | Inicio de sesión. Guarda access + refresh token, id, username y rol; registra el token FCM del dispositivo. Detecta si onboarding ya fue completado |
| `RegistroActivity` | Registro de nuevo usuario |
| `Onboarding1–4Activity` | Flujo de configuración inicial (datos físicos, actividad, objetivo) |
| `Onboarding5Activity` | Selección de nivel de experiencia: PRINCIPIANTE, INTERMEDIO, AVANZADO, EXPERTO |
| `OnboardingResumenActivity` | Resumen nutricional y guardado. `PATCH /usuarios/{id}` con todos los datos del onboarding |
| `HomeActivity` | Pantalla principal con BottomNav. Estadísticas reales de la semana (entrenamientos, calorías, minutos) cargadas en `onResume`. Detecta JWT expirado → redirige a Login |
| `EjerciciosActivity` | Catálogo con filtro por grupo muscular y búsqueda |
| `RutinasActivity` | Listado de rutinas del usuario y predefinidas. Filtro por nivel |
| `CrearRutinaActivity` | Paso 1/3 crear rutina: nombre, descripción, nivel, duración |
| `AnadirEjerciciosActivity` | Paso 2/3: buscador + filtro dificultad, selección con series/reps via dialog |
| `ResumenCrearRutinaActivity` | Paso 3/3: revisión antes de guardar. POST /rutinas + POST /rutinas-ejercicios × N |
| `DetalleRutinaActivity` | Vista readonly de rutina. Botón editar visible solo si es propia |
| `EditarRutinaActivity` | PATCH nombre/desc/nivel/duración + añadir/eliminar ejercicios |
| `NutricionActivity` | Objetivos dinámicos (CalculadoraNutricional en onResume), barras de progreso consumido vs objetivo (rojo si supera), 5 cards de comida → ComidaActivity |
| `ComidaActivity` | Log diario de una comida: lista alimentos registrados, totales, FAB añadir |
| `AnadirAlimentoActivity` | Buscar alimento, seleccionar gramos, preview macros en tiempo real, añadir a comida |
| `CrearAlimentoActivity` | Crear alimento propio con macros por 100g |
| `AdminAlimentosActivity` | Gestión admin de alimentos: toggle activo, editar (solo ROLE_ADMIN) |
| `PerfilActivity` | Perfil con datos reales de la API. Config tema/idioma + cerrar sesión. Botón "Sobre GymProFit" al pie |
| `AcercaDeActivity` | Pantalla "Acerca de": logo adaptativo claro/oscuro, info extendida de la app (descripción, 6 features, tech stack) e info del desarrollador (bio, formación, 3 FCTs, email clickable). Botón "Compartir": abre el selector de compartir del sistema con el enlace de la app (sin permisos adicionales) |
| `EditarPerfilActivity` | Editar email, peso, altura, edad, nivel, objetivo. PATCH /usuarios/{id} |
| `SesionesActivity` | Historial de sesiones con opción de eliminar |
| `RegistrarSesionActivity` | Formulario para registrar sesión: rutina (spinner), calorías calculadas, cards de ejercicios con peso por ejercicio (`EjercicioPesoAdapter`), notas, valoración (RatingBar 1-5). POST /ejercicios-realizados por cada ejercicio al finalizar |
| `ResumenSesionActivity` | Detalle de sesión completada + estadísticas del usuario + logros desbloqueados |
| `MedicionesActivity` | Historial de mediciones corporales con opción de eliminar |
| `RegistrarMedicionActivity` | Formulario para añadir medición (peso obligatorio, resto opcionales) |
| `LogrosActivity` | Lista todos los logros. Desbloqueados resaltados. Dos llamadas paralelas con AtomicInteger |
| `AdminActivity` | Panel admin: 6 KPIs globales + acceso a gestión de usuarios, rutinas predefinidas y ejercicios (solo ROLE_ADMIN) |
| `AdminUsuariosActivity` | Lista usuarios con búsqueda, filtros estado/rol, toggle activo/inactivo y cambio de rol |
| `AdminRutinasActivity` | Lista rutinas predefinidas con filtros nivel/estado, toggle activa y edición completa vía `EditarRutinaAdminActivity` |
| `AdminEjerciciosActivity` | Lista catálogo de ejercicios con filtro estado, toggle activo y edición completa vía `EditarEjercicioAdminActivity` |
| `EditarRutinaAdminActivity` | Edición completa de rutina predefinida: nombre, descripción, nivel, duración, calorías, categoría, días semana |
| `EditarEjercicioAdminActivity` | Edición completa de ejercicio: nombre, descripción, grupo muscular, dificultad, calorías, equipo, instrucciones |

**`ui/adapters/`**

| Adapter | Descripción |
|---|---|
| `EjercicioAdapter` | Tarjeta de ejercicio con nombre, grupo muscular y calorías |
| `RutinaAdapter` | Rutina con nombre, nivel, duración y botón eliminar |
| `SesionAdapter` | Sesión con fecha, rutina asociada, duración y calorías |
| `MedicionAdapter` | Medición con peso, IMC y extras (grasa, músculo) |
| `LogroAdapter` | Logro con icono emoji, nombre, descripción y check si desbloqueado |
| `AdminUsuarioAdapter` | Usuario con username, email, chip rol y chip activo/inactivo. Popup contextual: toggle activo + cambiar rol |
| `AdminRutinaAdapter` | Rutina predefinida con nombre, nivel, estado y contador de ejercicios. Popup contextual: toggle activa + editar |
| `AdminEjercicioAdapter` | Ejercicio con nombre, grupo, dificultad y chip activo/inactivo. Popup contextual: toggle activo + editar |
| `AdminAlimentoAdapter` | Alimento con nombre, categoría, calorías, chip activo/inactivo. Popup contextual: toggle activo + editar |
| `AlimentoAdapter` | Ítem en buscador de AnadirAlimentoActivity: nombre, categoría, macros por 100g |
| `AlimentoComidaAdapter` | Ítem en ComidaActivity: nombre, gramos, calorías totales, macros |

---

#### 🛠️ `utils/`

| Clase | Descripción |
|---|---|
| `PreferencesManager` | Preferencias centralizadas. Los **tokens (access + refresh)** y la caché del **token FCM enviado** van cifrados en `EncryptedSharedPreferences`; el resto (usuarioId, username, rol, nivel, objetivo, sexo, actividad, calorías/macros/agua, onboarding, tema, idioma) en SharedPreferences normales. `cerrarSesion()` limpia tokens + id + username. `KEY_ONBOARDING` persiste entre sesiones; tracking adicional por usuario via `onboarding_done_<username>` |
| `CalculadoraNutricional` | BMR con Mifflin-St Jeor, factor de actividad y distribución de macros según objetivo |
| `ResultadoNutricional` | Modelo con calorías totales, proteínas, carbohidratos y grasas |
| `UIHelper` | Toasts personalizados (éxito, error, info), diálogos de confirmación con icono y traducción de enums (nivel, grupo muscular). Ancho diálogo = 90% pantalla |
| `NotificationHelper` | Notificaciones locales con 5 canales: sesión completada, medición guardada, rutina creada, logro desbloqueado y push FCM (`notificarPush`) |
| `LoadingDialog` | Spinner modal reutilizable (overlay) para cargas y submits, sin tocar los layouts |
| `UiFeedback` | Mapea errores HTTP a toasts localizados: `-1` → cold-start, `≥500` → servidor, `404` → estado vacío benigno (sin toast) |
| `PushTokenManager` | Registro del token FCM tras el login (con caché, incluye idioma), baja en logout y re-sync al cambiar idioma |
| `FechaUtils` | Formateo de fechas ISO para la vista (Sesiones, Mediciones) |
| `EjercicioNavHelper` | Centraliza el Intent hacia `DetalleEjercicioActivity` con todos sus extras |

---

#### 🔔 Notificaciones push (FCM)

| Clase | Descripción |
|---|---|
| `GymProFitApp` (Application) | Pre-crea el canal de notificaciones push al arrancar (necesario para que Android pinte las push en background) |
| `services/GymFirebaseMessagingService` | `onNewToken` → re-registra el token rotado por Firebase; `onMessageReceived` → muestra la push en foreground vía `NotificationHelper.notificarPush` |
| `network/DeviceTokenApi` | `POST`/`DELETE /notificaciones/token` (registro y baja del dispositivo) |

Detalle end-to-end del sistema (backend + Android) en [documentacion/NOTIFICACIONES.md](documentacion/NOTIFICACIONES.md).

---

#### ⚙️ Configuración

**`build.gradle`** — `BASE_URL` por **buildType**: `debug` la lee de `local.properties` (API local) y `release` apunta a producción (Render) con **minify + shrinkResources (R8)** y reglas ProGuard para Gson/Retrofit (APK ~5 MB). Dependencias principales: Retrofit2, OkHttp3, Gson, Firebase BoM (Messaging + Analytics) y security-crypto.

**`AndroidManifest.xml`** — Declara `GymProFitApp` como Application, todas las Activities, el servicio FCM y los permisos `INTERNET`, `POST_NOTIFICATIONS`, `CAMERA`, `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` (≤API 32).

**`local.properties`** *(no versionado)* — Contiene `sdk.dir` y `BASE_URL=http://10.0.2.2:8080/api/` (solo aplica al buildType `debug`).

**`google-services.json`** *(no versionado)* — Configuración del proyecto Firebase para FCM.

---

## 🌐 API REST (`api/gymprofit-api`)

Backend desarrollado con Spring Boot 3, Java 21, MariaDB (MySQL gestionado en producción), Flyway para migraciones, jOOQ para consultas avanzadas, MapStruct para mapeo de DTOs, Spring Security con JWT + refresh tokens, Firebase Admin SDK para notificaciones push y Swagger/OpenAPI.

### Arquitectura

```
Controller → Service → Repository (JPA / jOOQ) → MariaDB
```

---

### 📁 Estructura de la API

#### `config/`

| Clase | Descripción |
|---|---|
| `SecurityConfig` | CORS con lista blanca configurable, sesiones stateless, reglas por rol. `/auth/**` y Swagger públicos (Swagger deshabilitado en prod) |
| `FirebaseConfig` | Inicializa el Firebase Admin SDK una sola vez. Init *graceful*: sin credencial → push desactivadas y la API arranca igual (no rompe CI) |
| `FlywayConfig` | Migraciones de base de datos |
| `JooqConfig` | Consultas SQL tipadas |
| `SwaggerConfig` | OpenAPI en `/swagger-ui.html` |
| `security/JwtTokenProvider` | Genera y valida el access JWT (30 min). Configurable con `jwt.secret` y `jwt.expiration` |
| `security/JwtAuthenticationFilter` | Extrae y valida JWT en cada petición |
| `security/AuthRateLimitFilter` | Rate-limiting por IP en `/auth/**` (ventana fija 15 req/60s → 429 + `Retry-After`) |
| `security/SecurityUtils` | Comprobación de *ownership* (`checkOwnership`): un USER solo accede a sus propios recursos (protección anti-IDOR) |

---

#### `controller/`

| Controlador | Ruta base | Descripción |
|---|---|---|
| `AuthController` | `/auth` | Login, registro, guest, **refresh** y logout. Access JWT de 30 min + refresh opaco rotado/revocable |
| `UsuarioController` | `/usuarios` | CRUD completo. Solo ADMIN lista todos |
| `EjercicioController` | `/ejercicios` | CRUD. Filtros por grupo, dificultad y nombre. Escritura solo ADMIN |
| `RutinaController` | `/rutinas` | CRUD rutinas. Predefinidas y de usuario |
| `RutinaEjercicioController` | `/rutinas-ejercicios` | Relación rutina↔ejercicio |
| `SesionEntrenamientoController` | `/sesiones` | Registro de sesiones completadas |
| `EjercicioRealizadoController` | `/ejercicios-realizados` | Detalle ejercicios por sesión |
| `ProgresoEjercicioController` | `/progreso-ejercicios` | Mejor marca por ejercicio |
| `MedicionCorporalController` | `/mediciones-corporales` | Mediciones corporales. `GET /usuario/{id}/ordenadas` para historial |
| `ObjetivoPersonalController` | `/objetivos-personales` | Objetivos con progreso |
| `AlimentoController` | `/alimentos` | Catálogo nutricional |
| `ComidaController` | `/comidas` | Comidas diarias |
| `AlimentoComidaController` | `/alimentos-comida` | Alimentos por comida |
| `NotificacionController` | `/notificaciones` | Notificaciones in-app: inmediata → push al momento; programada → la envía el job al vencer |
| `DeviceTokenController` | `/notificaciones/token` | Registro/baja del token FCM del dispositivo (upsert idempotente, usuario del JWT, guarda el idioma) |
| `LogroController` | `/logros` | Catálogo de logros y logros por usuario |
| `AdminController` | `/admin` | Estadísticas globales (6 KPIs), lista usuarios con filtros jOOQ, toggle activo, cambiar rol, búsqueda rutinas predefinidas y ejercicios con filtros dinámicos |
| `EjercicioJooqController` | `/jooq/ejercicios` | Consultas avanzadas jOOQ |
| `UsuarioJooqController` | `/jooq/usuarios` | Consultas avanzadas jOOQ (solo ADMIN) |

---

#### `entity/`

| Entidad | Tabla |
|---|---|
| `Usuario` | `usuarios` — implementa `UserDetails`. `isEnabled()` devuelve `activo != null && activo` |
| `Role` | `roles` — ADMIN, USER, GUEST |
| `Ejercicio` | `ejercicios` |
| `Rutina` | `rutinas` |
| `RutinaEjercicio` | `rutina_ejercicios` |
| `SesionEntrenamiento` | `sesiones_entrenamiento` |
| `EjercicioRealizado` | `ejercicios_realizados` |
| `ProgresoEjercicio` | `progreso_ejercicios` |
| `MedicionCorporal` | `mediciones_corporales` |
| `ObjetivoPersonal` | `objetivos_personales` |
| `Alimento` | `alimentos` |
| `Comida` | `comidas` |
| `AlimentoComida` | `alimentos_comida` |
| `Notificacion` | `notificaciones` |
| `Logro` | `logros` |
| `UsuarioLogro` | `usuario_logros` |
| `RefreshToken` | `refresh_tokens` — refresh opacos con rotación y revocación (purga diaria con `RefreshTokenCleanupTask`) |
| `DeviceToken` | `device_tokens` — tokens FCM por dispositivo + idioma para push localizadas |

---

#### `enums/`

`GrupoMuscular`, `Dificultad`, `NivelExperiencia`, `TipoComida`, `TipoNotificacion`, `TipoObjetivo`, `RoleType`.

---

#### `exceptions/`

`ControllerExceptionHandler` (`@RestControllerAdvice`). Excepciones: `NotFoundEntityException` (404), `DuplicateEntityException` (409), `InvalidCredentialsException` (401), `InvalidDataException` (400), `UnauthorizedException` (403), `CreateEntityException` / `UpdateEntityException` / `DeleteEntityException` (500).

---

#### 🔔 Notificaciones push (FCM)

`PushNotificationService` envía las push a todos los dispositivos del usuario vía **Firebase Admin SDK** (tolerante a fallos, borra tokens muertos). Dos jobs `@Scheduled` completan el sistema: `NotificacionProgramadaTask` (cada 60 s envía las notificaciones programadas vencidas) y `RecordatorioNotificacionesTask`, con **11 recordatorios automáticos** (5 comidas, entrenar, inactividad, resumen semanal, logro próximo, medición mensual y objetivo por vencer — cron `Europe/Madrid`, solo usuarios con dispositivo registrado, anti-spam por título). Los textos se resuelven con `MessageSource` según `device_tokens.idioma` (push localizadas ES/EN). Detalle completo en [documentacion/NOTIFICACIONES.md](documentacion/NOTIFICACIONES.md).

---

#### `src/main/resources/`

| Archivo | Descripción |
|---|---|
| `application.properties` | Puerto 8080, context-path `/api`, perfil `dev` por defecto |
| `application-dev.properties` | BD local, logging DEBUG |
| `application-prod.properties` | Producción 12-factor: todo por variables de entorno (BD, JWT, Firebase, CORS) |
| `application-ci.properties` | Perfil para GitHub Actions (MariaDB efímera, rate-limit off) |
| `application-example.properties` | Plantilla para nuevos desarrolladores |
| `messages.properties` / `messages_en.properties` | Textos de las notificaciones push (ES/EN) resueltos con `MessageSource` |
| `logback-spring.xml` | Logs diarios en `logs/gymprofit_YYYY-MM-DD.log` en dev; solo consola en prod (FS efímero) |

> **Nota:** Las migraciones Flyway de `db/migration/` tienen prefijo `V` y **se ejecutan automáticamente al arrancar** la API (`ddl-auto=validate`): el esquema se crea y versiona solo, sin tocar la BD a mano.

---

#### `src/test/`

**230 tests**: unitarios de services (Mockito), de controllers con MockMvc y de integración end-to-end. Incluyen los tests de **ownership/IDOR** (`AbstractOwnershipTest`: `@SpringBootTest` sin mocks con usuarios reales owner/attacker — recurso ajeno → 403, propio → 200; 7 dominios), rate-limit (`AuthRateLimitFilterTest`), recordatorios push (`RecordatorioNotificacionesTaskTest`) y catálogo multiidioma (`CatalogoI18nTest`).

---

## 🗄️ Base de datos (`db/`)

MariaDB en `localhost:3308` en desarrollo (base de datos `gymprofit_db`); **MySQL gestionado (Aiven)** en producción. El esquema lo crean y versionan las migraciones **Flyway** al arrancar la API; `db/schema.sql` queda como referencia.

| Tabla | Descripción |
|---|---|
| `usuarios` | Perfil completo. `activo TINYINT(1)` — debe ser 1 para poder hacer login |
| `roles` / `usuario_roles` | Sistema de roles ADMIN, USER, GUEST |
| `ejercicios` | Catálogo con grupo muscular y dificultad como enum |
| `rutinas` | Flag `es_predefinida` para distinguir rutinas del sistema |
| `rutina_ejercicios` | Series, reps, peso recomendado y orden |
| `sesiones_entrenamiento` | fechaInicio, fechaFin, duracion, calorias |
| `ejercicios_realizados` | Detalle por sesión |
| `progreso_ejercicios` | Mejor marca histórica por ejercicio |
| `mediciones_corporales` | Peso, altura, IMC, grasa, músculo, perímetros |
| `objetivos_personales` | Valor actual vs meta, fechas y estado |
| `alimentos` / `comidas` / `alimentos_comida` | Módulo nutricional |
| `notificaciones` | Recordatorios y logros (con `push_enviada` para las programadas) |
| `logros` / `usuario_logros` | Sistema de logros con evaluación automática |
| `refresh_tokens` | Refresh tokens opacos con rotación y revocación |
| `device_tokens` | Tokens FCM por dispositivo + idioma para push localizadas |

> El catálogo (ejercicios, alimentos, logros, rutinas predefinidas) tiene columnas `*_en` para el multiidioma, resueltas en la API según el header `Accept-Language` (fallback a ES).

---

## 🛠️ Tecnologías

### App Android

| Tecnología | Uso |
|---|---|
| Java 11 | Lenguaje principal |
| Android SDK 36 (minSdk 24) | Plataforma objetivo |
| Retrofit2 + OkHttp3 + Gson | Cliente HTTP tipado (interceptor de token + authenticator de refresh) |
| Firebase (BoM 34.15.0) | Cloud Messaging (push) + Analytics |
| Material Design 3 | Componentes visuales y temas |
| EncryptedSharedPreferences (security-crypto) | Persistencia cifrada de tokens y datos de sesión |
| Gradle 8.13 | Build system (minify/R8 en release) |

### API REST

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.5.16 | Framework web |
| Spring Security | — | JWT + refresh tokens, roles, rate-limiting |
| Spring Data JPA | — | ORM y CRUD estándar |
| jOOQ | 3.19 | Consultas SQL complejas y tipadas |
| Flyway | 11.15 | Migraciones versionadas (automáticas al arrancar) |
| MapStruct | 1.6.3 | Mapeo DTO↔entidad (+ resolución i18n del catálogo) |
| Lombok | — | Reducción de boilerplate |
| JJWT | 0.13.0 | Generación y validación JWT |
| Firebase Admin SDK | 9.8.0 | Envío de notificaciones push (FCM) |
| SpringDoc OpenAPI | 2.8.15 | Documentación Swagger |
| MariaDB / MySQL | — | Base de datos relacional (MariaDB en dev, Aiven MySQL en prod) |
| JUnit 5 + Mockito | — | Tests unitarios e integración (230) |
| Maven | — | Gestión de dependencias |

---

## 📋 Requisitos previos

- **Android Studio** Hedgehog o superior
- **JDK 21** para la API y **JDK 11** para la app Android
- **MariaDB** 10.6+ (`localhost:3308`)
- **Maven** 3.8+ (o usar `./mvnw`)

---

## 🚀 Instalación y configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/ruubeenn13/GymProFit.git
cd GymProFit
```

### 2. Configurar la base de datos

Basta con crear la base de datos vacía — **las migraciones Flyway crean el esquema completo automáticamente al arrancar la API**:

```sql
CREATE DATABASE gymprofit_db CHARACTER SET utf8mb4;
```

> `db/schema.sql` se mantiene solo como referencia del esquema.

### 3. Configurar la API

```bash
cd api/gymprofit-api/src/main/resources
cp application-example.properties application-dev.properties
```

Edita `application-dev.properties` con tus credenciales de BD y clave JWT.

> **Opcional (push):** para probar las notificaciones push en dev, define la variable de entorno `FIREBASE_CREDENTIALS_PATH` apuntando al JSON de la service account de Firebase (no versionado). Sin ella la API arranca igual, con las push desactivadas.

### 4. Arrancar la API

```bash
cd api/gymprofit-api
./mvnw spring-boot:run
```

API disponible en `http://localhost:8080/api`. Swagger en `http://localhost:8080/api/swagger-ui.html`.

### 5. Configurar la App Android

Crea `app/GymProFit/local.properties`:

```properties
sdk.dir=C\:\\Users\\TuUsuario\\AppData\\Local\\Android\\Sdk
BASE_URL=http://10.0.2.2:8080/api/
```

> `10.0.2.2` es la IP del emulador para `localhost`. Con dispositivo físico, usar la IP local de la máquina. `BASE_URL` solo aplica al buildType `debug`; `release` apunta directamente a producción.

### 6. Ejecutar la App

Abre `app/GymProFit` en Android Studio, sincroniza Gradle y ejecuta en emulador o dispositivo.

---

## ☁️ Despliegue en producción

| Componente | Dónde |
|---|---|
| API | **Render** (Docker multi-stage, free tier) — `https://gymprofit-api.onrender.com/api` |
| Base de datos | **Aiven for MySQL** (always-free, TLS `verify-full` con CA) |
| App Android | buildTypes: `debug` → API local, `release` → producción con minify/R8 (APK ~5 MB) |
| CI | **GitHub Actions**: build + 230 tests contra MariaDB efímera en cada push/PR |
| Keep-alive | Cron cada 10 min a `/actuator/health` para que el free tier de Render no se duerma |

Guía completa paso a paso en [documentacion/DESPLIEGUE.md](documentacion/DESPLIEGUE.md).

---

## 📝 Changelog

El historial de cambios se ha movido a **[CHANGELOG.md](CHANGELOG.md)**.

---

<div align="center">

Desarrollado por **Rubén Juan Candela**
CFGS Desarrollo de Aplicaciones Multimedia · 2º DAM · 2026

</div>
