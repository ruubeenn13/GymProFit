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
- [Changelog](CHANGELOG.md)

---

## 📱 Descripción general

**GymProFit** es un proyecto full-stack compuesto por una aplicación Android nativa y una API REST propia. Permite a los usuarios registrarse, gestionar sus rutinas de entrenamiento, registrar sesiones, consultar un catálogo de ejercicios, hacer seguimiento de mediciones corporales, ver sus logros desbloqueados y calcular sus necesidades nutricionales personalizadas según su perfil físico.

El sistema distingue tres roles de usuario: **GUEST**, **USER** y **ADMIN**, con permisos diferenciados para cada operación de la API.

---

## 🗂️ Estructura del repositorio

```
TFG-GymProFit/
├── 📂 app/          # Aplicación Android (Android Studio - Java)
├── 📂 api/          # API REST backend (Spring Boot - Java)
└── 📂 db/           # Script SQL de creación de la base de datos
```

---

## 📱 App Android (`app/GymProFit`)

Aplicación Android nativa desarrollada en Java con Android Studio. Consume la API REST mediante `HttpURLConnection` + `AsyncTask` + `org.json` (arquitectura 4 capas UD06) y almacena el token JWT en SharedPreferences vía `PreferencesManager`.

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
  ├── NutricionActivity
  └── PerfilActivity → EditarPerfilActivity
        ├── SesionesActivity → RegistrarSesionActivity → ResumenSesionActivity
        ├── MedicionesActivity → RegistrarMedicionActivity
        ├── LogrosActivity
        ├── AcercaDeActivity
        └── AdminActivity  (solo ROLE_ADMIN)
              ├── AdminUsuariosActivity
              ├── AdminRutinasActivity
              └── AdminEjerciciosActivity
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

Arquitectura 4 capas obligatoria (UD06).

**`UtilREST.java`** — Capa 2. `HttpURLConnection` + `AsyncTask` (deprecado pero requerido por el temario). Gestiona el token JWT estático (`setToken` / `clearToken` / `getToken`). Inyecta `Authorization: Bearer <token>` en cada petición. PATCH forzado vía reflexión Java. Loggea cada petición con `Log.d("GymProFit", método + url + statusCode)`.

**`UtilJSONParser.java`** — Capa 3. Parseo con `org.json`. Incluye el helper `parseFecha()` que maneja tanto el formato array de Jackson `[2024,5,17,10,30,0]` como string ISO `"2024-05-17T10:30:00"`. Métodos: `parseToken`, `parseTokenUsername`, `parseTokenRol` (extrae `roles[0]` del `TokenDTO` y normaliza a `ROLE_X`), `parseUsuario`, `parseUsuarioList`, `parseEjercicio`, `parseEjercicioList`, `parseRutina`, `parseRutinaList`, `parseSesion`, `parseSesionList`, `parseLogro`, `parseLogroList`, `parseLogrosDesbloqueados`, `parseMedicion`, `parseMedicionList`, `parseObjetivo`, `parseObjetivoList`.

**`API.java`** — Capa 4. Fachada estática con todos los endpoints agrupados por sección: AUTH, USUARIOS, EJERCICIOS, RUTINAS, SESIONES, LOGROS, OBJETIVOS, MEDICIONES, ADMIN.

| Método | Endpoint | Descripción |
|---|---|---|
| `login` | `POST auth/login` | Inicio de sesión |
| `register` | `POST auth/register` | Registro |
| `getUsuarioPorUsername` | `GET usuarios/username/{u}` | Perfil por username |
| `getUsuarioPorId` | `GET usuarios/{id}` | Perfil por ID |
| `actualizarUsuario` | `PUT usuarios` | Actualizar perfil completo (id en body) — solo para ADMIN |
| `patchUsuario` | `PATCH usuarios/{id}` | Actualización parcial — usado en onboarding y edición de perfil |
| `getRutinasDeUsuario` | `GET rutinas/usuario/{id}` | Rutinas del usuario |
| `crearRutina` | `POST rutinas` | Nueva rutina |
| `eliminarRutina` | `DELETE rutinas/{id}` | Eliminar rutina |
| `getEjercicios` | `GET ejercicios` | Catálogo completo |
| `getSesionesByUsuario` | `GET sesiones/usuario/{id}` | Sesiones del usuario |
| `crearSesion` | `POST sesiones` | Nueva sesión |
| `eliminarSesion` | `DELETE sesiones/{id}` | Eliminar sesión |
| `getLogros` | `GET logros` | Todos los logros |
| `getLogrosDeUsuario` | `GET logros/usuario/{id}` | Logros desbloqueados |
| `getMedicionesDeUsuario` | `GET mediciones-corporales/usuario/{id}/ordenadas` | Historial de mediciones |
| `crearMedicion` | `POST mediciones-corporales` | Nueva medición |
| `eliminarMedicion` | `DELETE mediciones-corporales/{id}` | Eliminar medición |
| `getAdminEstadisticas` | `GET admin/estadisticas-globales` | 6 KPIs globales (ADMIN) |
| `getAdminUsuariosFiltrados` | `GET admin/usuarios?page&size&activo&rol&username` | Lista usuarios con filtros (ADMIN) |
| `adminToggleActivoUsuario` | `PATCH admin/usuarios/{id}/toggle-activo` | Activar/desactivar usuario (ADMIN) |
| `adminCambiarRolUsuario` | `PATCH admin/usuarios/{id}/rol?nuevoRol=` | Cambiar rol (ADMIN) |
| `adminBuscarRutinasPredefinidas` | `GET admin/rutinas/predefinidas/busqueda` | Buscar rutinas predefinidas con filtros (ADMIN) |
| `adminBuscarEjercicios` | `GET admin/ejercicios/busqueda` | Buscar ejercicios con filtros (ADMIN) |
| `adminActivarRutina` / `adminDesactivarRutina` | `PUT/DELETE rutinas/{id}/activar` | Toggle activa rutina (ADMIN) |
| `adminActivarEjercicio` / `adminDesactivarEjercicio` | `PUT/DELETE ejercicios/{id}/activar` | Toggle activo ejercicio (ADMIN) |
| `adminEditarRutina` | `PATCH rutinas/{id}` | Editar todos los campos de rutina predefinida (ADMIN) |
| `adminEditarEjercicio` | `PATCH ejercicios/{id}` | Editar todos los campos de ejercicio (ADMIN) |
| `crearEjercicioRealizado` | `POST ejercicios-realizados` | Registrar ejercicio realizado en sesión |

---

#### 🖼️ `ui/activities/`

| Activity | Descripción |
|---|---|
| `SplashActivity` | Carga inicial. Restaura token JWT desde SharedPreferences y redirige a Home o Login |
| `LoginActivity` | Inicio de sesión. Guarda token, id, username y rol. Detecta si onboarding ya fue completado |
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
| `AcercaDeActivity` | Pantalla "Acerca de": logo adaptativo claro/oscuro, info extendida de la app (descripción, 6 features, tech stack) e info del desarrollador (bio, formación, 3 FCTs, email clickable). Botón "Compartir": pide permiso `READ_CONTACTS` en runtime, abre selector de contactos y lanza SMS pre-rellenado con el enlace de la app |
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
| `PreferencesManager` | SharedPreferences centralizado. Campos: token, usuarioId, username, rol, nivel, objetivo, sexo, actividad, calorías/macros/agua, onboarding, tema, idioma. `cerrarSesion()` limpia token + id + username. `KEY_ONBOARDING` persiste entre sesiones; tracking adicional por usuario via `onboarding_done_<username>` |
| `CalculadoraNutricional` | BMR con Mifflin-St Jeor, factor de actividad y distribución de macros según objetivo |
| `ResultadoNutricional` | Modelo con calorías totales, proteínas, carbohidratos y grasas |
| `UIHelper` | Toasts personalizados (éxito, error, info) y diálogos de confirmación con icono. Ancho diálogo = 90% pantalla |
| `NotificationHelper` | Notificaciones con 4 canales: sesión completada, medición guardada, rutina creada, logro desbloqueado (InboxStyle expandible) |

---

#### ⚙️ Configuración

**`build.gradle`** — Lee `BASE_URL` desde `local.properties` e inyecta en `BuildConfig`. No hay dependencias Retrofit/OkHttp/Gson — solo `org.json` nativo de Android.

**`AndroidManifest.xml`** — Declara todas las Activities y los permisos `INTERNET`, `POST_NOTIFICATIONS`, `CAMERA`, `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` (≤API 32), `READ_CONTACTS`.

**`local.properties`** *(no versionado)* — Contiene `sdk.dir` y `BASE_URL=http://10.0.2.2:8080/api/`.

---

## 🌐 API REST (`api/gymprofit-api`)

Backend desarrollado con Spring Boot 3, Java 21, MariaDB, Flyway para migraciones, jOOQ para consultas avanzadas, MapStruct para mapeo de DTOs, Spring Security con JWT y Swagger/OpenAPI.

### Arquitectura

```
Controller → Service → Repository (JPA / jOOQ) → MariaDB
```

---

### 📁 Estructura de la API

#### `config/`

| Clase | Descripción |
|---|---|
| `SecurityConfig` | CORS abierto, sesiones stateless, reglas por rol. `/auth/**` y Swagger públicos |
| `FlywayConfig` | Migraciones de base de datos |
| `JooqConfig` | Consultas SQL tipadas |
| `SwaggerConfig` | OpenAPI en `/swagger-ui.html` |
| `security/JwtTokenProvider` | Genera y valida JWT. Configurable con `jwt.secret` y `jwt.expiration` |
| `security/JwtAuthenticationFilter` | Extrae y valida JWT en cada petición |

---

#### `controller/`

| Controlador | Ruta base | Descripción |
|---|---|---|
| `AuthController` | `/auth` | Login y registro. Endpoints públicos |
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
| `NotificacionController` | `/notificaciones` | Notificaciones del sistema |
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

---

#### `enums/`

`GrupoMuscular`, `Dificultad`, `NivelExperiencia`, `TipoComida`, `TipoNotificacion`, `TipoObjetivo`, `RoleType`.

---

#### `exceptions/`

`ControllerExceptionHandler` (`@RestControllerAdvice`). Excepciones: `NotFoundEntityException` (404), `DuplicateEntityException` (409), `InvalidCredentialsException` (401), `InvalidDataException` (400), `UnauthorizedException` (403), `CreateEntityException` / `UpdateEntityException` / `DeleteEntityException` (500).

---

#### `src/main/resources/`

| Archivo | Descripción |
|---|---|
| `application.properties` | Puerto 8080, context-path `/api`, perfil `dev` por defecto |
| `application-dev.properties` | BD local, logging DEBUG |
| `application-prod.properties` | Entorno de producción |
| `application-example.properties` | Plantilla para nuevos desarrolladores |
| `logback-spring.xml` | Logs diarios en `logs/gymprofit_YYYY-MM-DD.log` |

> **Nota:** Los archivos de migración Flyway en `db/migration/` no tienen prefijo `V` → Flyway **no los ejecuta automáticamente**. Cualquier cambio de esquema se aplica con SQL directo en MariaDB.

---

#### `src/test/`

132 tests. Controladores: `AuthControllerTest`, `EjercicioControllerTest`, `RutinaControllerTest`. Servicios: `AuthServiceTest`, `EjercicioServiceTest`, `RutinaServiceTest`, `SesionEntrenamientoServiceTest`, `UsuarioServiceTest`.

---

## 🗄️ Base de datos (`db/`)

MariaDB en `localhost:3308`, base de datos `gymprofit_db`.

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
| `notificaciones` | Recordatorios y logros |
| `logros` / `usuario_logros` | Sistema de logros con evaluación automática |

---

## 🛠️ Tecnologías

### App Android

| Tecnología | Uso |
|---|---|
| Java 11 | Lenguaje principal |
| Android SDK 36 (minSdk 24) | Plataforma objetivo |
| HttpURLConnection + AsyncTask | Cliente HTTP (UD06) |
| org.json | Parseo JSON nativo |
| Material Design 3 | Componentes visuales y temas |
| SharedPreferences | Persistencia local del token JWT y datos de sesión |
| Gradle 8.13 | Build system |

### API REST

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.5.7 | Framework web |
| Spring Security | — | JWT y autorización por roles |
| Spring Data JPA | — | ORM y CRUD estándar |
| jOOQ | 3.19 | Consultas SQL complejas y tipadas |
| Flyway | 11.15 | Migraciones versionadas |
| MapStruct | 1.6.3 | Mapeo DTO↔entidad |
| Lombok | 1.18.38 | Reducción de boilerplate |
| JJWT | 0.13.0 | Generación y validación JWT |
| SpringDoc OpenAPI | 2.8.15 | Documentación Swagger |
| MariaDB | — | Base de datos relacional |
| JUnit 5 + Mockito | — | Tests unitarios e integración |
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
git clone https://github.com/ruubeenn13/TFG-GymProFit.git
cd TFG-GymProFit
```

### 2. Configurar la base de datos

```sql
-- Crear la base de datos
CREATE DATABASE gymprofit_db CHARACTER SET utf8mb4;
-- Ejecutar el schema inicial
mysql -u root -p gymprofit_db < db/schema.sql
-- Activar usuarios del seed si fuera necesario
UPDATE usuarios SET activo = 1 WHERE activo IS NULL;
```

### 3. Configurar la API

```bash
cd api/gymprofit-api/src/main/resources
cp application-example.properties application-dev.properties
```

Edita `application-dev.properties` con tus credenciales de BD y clave JWT.

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

> `10.0.2.2` es la IP del emulador para `localhost`. Con dispositivo físico, usar la IP local de la máquina.

### 6. Ejecutar la App

Abre `app/GymProFit` en Android Studio, sincroniza Gradle y ejecuta en emulador o dispositivo.

---

## 📝 Changelog

El historial de cambios se ha movido a **[CHANGELOG.md](CHANGELOG.md)**.

---

<div align="center">

Desarrollado por **Rubén Juan Candela**
CFGS Desarrollo de Aplicaciones Multimedia · 2º DAM · 2026

</div>
