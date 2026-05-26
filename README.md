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
- [Changelog](#-changelog)

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
Onboarding (1 → 2 → 3 → 4 → Resumen)
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
| `actualizarUsuario` | `PUT usuarios` | Actualizar perfil (id en body) |
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
| `Onboarding1–4Activity` | Flujo de configuración inicial (objetivo, nivel, datos físicos) |
| `OnboardingResumenActivity` | Resumen y guardado. Llama a `PUT /usuarios` con todos los datos |
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
| `PreferencesManager` | SharedPreferences centralizado. Campos: token, usuarioId, username, rol, nivel, objetivo, sexo, actividad, calorías/macros/agua, onboarding, tema, idioma. `cerrarSesion()` limpia token + id + username + onboarding |
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

---

<div align="center">

Desarrollado por **Rubén Juan Candela**
CFGS Desarrollo de Aplicaciones Multimedia · 2º DAM · 2026

</div>
