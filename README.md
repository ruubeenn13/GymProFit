<div align="center">

# 💪 GymProFit

### Aplicación Android de gestión de entrenamientos y nutrición

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white)
![Retrofit](https://img.shields.io/badge/Retrofit-48B983?style=for-the-badge&logo=square&logoColor=white)

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

---

## 📱 Descripción general

**GymProFit** es un proyecto full-stack compuesto por una aplicación Android nativa y una API REST propia. Permite a los usuarios registrarse, gestionar sus rutinas de entrenamiento, consultar un catálogo de ejercicios y calcular sus necesidades nutricionales personalizadas según su perfil físico (peso, altura, edad, nivel de experiencia y objetivo).

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

Aplicación Android nativa desarrollada en Java con Android Studio. Consume la API REST mediante Retrofit y almacena el token JWT en SharedPreferences.

### Flujo de navegación

```
SplashActivity
     ↓
Onboarding (1 → 2 → 3 → 4 → Resumen)
     ↓
LoginActivity ←→ RegistroActivity
     ↓
HomeActivity (navegación inferior)
  ├── EjerciciosActivity
  ├── RutinasActivity
  ├── NutricionActivity
  └── PerfilActivity
```

---

### 📁 Estructura de la App

#### `app/src/main/java/es/pmdm/gymprofit/`

---

#### 🧩 `model/`

Entidades de dominio locales de la app, independientes de la API.

| Clase | Descripción |
|---|---|
| `ejercicio/Ejercicio.java` | Modelo local de ejercicio con nombre, grupo muscular, dificultad, descripción, calorías e icono de recurso |
| `rutina/Rutina.java` | Modelo local de rutina con nombre, nivel, descripción, número de ejercicios, duración y calorías aproximadas |

> Estas clases son independientes de los DTOs de red — sirven para representar datos en la UI sin depender de la estructura de la API.

---

#### 🌐 `network/`

Toda la comunicación HTTP con la API REST.

**`ApiClient.java`** — Cliente HTTP singleton construido con OkHttp + Retrofit. Lee la `BASE_URL` desde `BuildConfig` (inyectada en tiempo de compilación desde `local.properties`). Gestiona el token JWT: al hacer login se almacena con `setToken()` y se inyecta automáticamente en la cabecera `Authorization: Bearer <token>` de cada petición. Al cerrar sesión se limpia con `clearToken()`. Incluye un interceptor de logging para depuración.

**`ApiService.java`** — Interfaz Retrofit que declara todos los endpoints de la API:

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `auth/login` | Inicio de sesión, devuelve JWT |
| `POST` | `auth/register` | Registro de nuevo usuario |
| `GET` | `usuarios/username/{username}` | Obtener usuario por nombre |
| `GET` | `usuarios/{id}` | Obtener usuario por ID |
| `PUT` | `usuarios/{id}` | Actualizar datos del usuario |
| `GET` | `ejercicios` | Listar todos los ejercicios |
| `GET` | `ejercicios/activos` | Listar ejercicios activos |
| `GET` | `ejercicios/grupo/{grupoMuscular}` | Filtrar por grupo muscular |
| `GET` | `ejercicios/nombre/{nombre}` | Buscar por nombre |
| `GET` | `ejercicios/{id}` | Obtener ejercicio por ID |
| `GET` | `rutinas/predefinidas` | Rutinas predefinidas del sistema |
| `GET` | `rutinas/predefinidas/nivel/{nivel}` | Rutinas predefinidas por nivel |
| `GET` | `rutinas/usuario/{usuarioId}` | Rutinas del usuario |
| `GET` | `rutinas/nivel/{nivel}` | Rutinas por nivel |
| `POST` | `rutinas` | Crear nueva rutina |
| `PUT` | `rutinas/{id}` | Modificar rutina |
| `DELETE` | `rutinas/{id}` | Eliminar rutina |

**`dto/`** — Objetos de transferencia de datos que mapean las respuestas JSON de la API:

| DTO | Campos principales |
|---|---|
| `LoginDTO` | `username`, `password` |
| `RegisterDTO` | `username`, `password`, `email`, `roles` |
| `TokenDTO` | `token`, `username`, `roles` — respuesta del login |
| `UsuarioDTO` | `id`, `username`, `email`, `peso`, `altura`, `edad`, `nivelExperiencia`, `objetivo`, `fechaRegistro`, `activo` |
| `EjercicioDTO` | `id`, `nombre`, `descripcion`, `grupoMuscular`, `dificultad`, `imagenUrl`, `instrucciones`, `caloriasQuemadas`, `equipoNecesario`, `activo` |
| `RutinaDTO` | `id`, `usuarioId`, `nombre`, `descripcion`, `duracionMinutos`, `nivel`, `esPredefinida`, `categoria`, `diasSemana`, `fechaCreacion`, `activa` |
| `RutinaCreateDTO` | `usuarioId`, `nombre`, `descripcion`, `duracionMinutos`, `nivel` — para crear rutinas nuevas |

---

#### 🖼️ `ui/activities/`

Pantallas de la aplicación. Cada Activity gestiona directamente sus llamadas a la API usando `ApiClient.getApiService()`.

| Activity | Descripción |
|---|---|
| `SplashActivity` | Pantalla de carga inicial. Comprueba si hay sesión activa en SharedPreferences para redirigir al Home o al Login |
| `Onboarding1Activity` | Primera pantalla de bienvenida — presentación de la app |
| `Onboarding2Activity` | Segunda pantalla — selección de objetivo (perder peso, ganar músculo, etc.) |
| `Onboarding3Activity` | Tercera pantalla — nivel de experiencia del usuario |
| `Onboarding4Activity` | Cuarta pantalla — datos físicos (peso, altura, edad) |
| `OnboardingResumenActivity` | Resumen del perfil configurado durante el onboarding. Realiza el registro en la API |
| `LoginActivity` | Inicio de sesión con username y contraseña. Llama a `auth/login`, guarda el JWT y navega al Home |
| `RegistroActivity` | Registro de nuevo usuario. Llama a `auth/register` con validación de campos |
| `HomeActivity` | Pantalla principal con navegación inferior (Bottom Navigation). Muestra resumen del perfil del usuario |
| `EjerciciosActivity` | Catálogo de ejercicios con RecyclerView. Permite filtrar por grupo muscular y buscar por nombre |
| `RutinasActivity` | Listado de rutinas del usuario y rutinas predefinidas. Permite eliminar rutinas propias |
| `CrearRutinaActivity` | Formulario para crear una nueva rutina personalizada |
| `NutricionActivity` | Calculadora nutricional. Calcula calorías diarias, macronutrientes y recomendaciones según el perfil del usuario |
| `PerfilActivity` | Visualización y edición del perfil: username, email, peso, altura, edad, nivel y objetivo. Incluye opción de cambiar idioma y cerrar sesión |

**`ui/adapters/`** — Adaptadores RecyclerView:

| Adapter | Descripción |
|---|---|
| `EjercicioAdapter.java` | Muestra cada ejercicio en una tarjeta con nombre, grupo muscular, dificultad y calorías |
| `RutinaAdapter.java` | Muestra cada rutina con nombre, nivel, duración y opción de eliminar |

---

#### 🛠️ `utils/`

Clases de utilidad reutilizables desde cualquier Activity.

| Clase | Descripción |
|---|---|
| `PreferencesManager.java` | Gestión centralizada de SharedPreferences. Guarda y recupera el token JWT, el ID de usuario, username, email, nivel, objetivo y datos físicos. Proporciona métodos para comprobar si hay sesión activa y para limpiarla al cerrar sesión |
| `CalculadoraNutricional.java` | Lógica de cálculo nutricional personalizada. Calcula el metabolismo basal (BMR) usando la fórmula de Harris-Benedict, aplica el factor de actividad según el nivel de experiencia del usuario y distribuye los macronutrientes (proteínas, carbohidratos, grasas) según el objetivo del usuario |
| `ResultadoNutricional.java` | Modelo de datos que encapsula el resultado del cálculo nutricional: calorías totales, proteínas, carbohidratos y grasas en gramos |
| `UIHelper.java` | Métodos helper para la interfaz: mostrar toasts personalizados, diálogos de confirmación y otros elementos visuales reutilizables |

---

#### 🎨 `res/`

Recursos de la aplicación.

| Carpeta | Contenido |
|---|---|
| `layout/` | Layouts XML de cada Activity y elementos adicionales: `activity_login.xml`, `activity_home.xml`, `activity_ejercicios.xml`, `activity_rutinas.xml`, `activity_crear_rutina.xml`, `activity_nutricion.xml`, `activity_perfil.xml`, `activity_registro.xml`, `activity_splash.xml`, `activity_onboarding1-4.xml`, `activity_onboarding_resumen.xml`, `item_ejercicio.xml`, `item_rutina.xml`, `dialog_custom.xml`, `dialog_idioma.xml`, `toast_custom.xml` |
| `drawable/` | 25+ iconos SVG vectoriales: navegación (`ic_home`, `ic_rutinas`, `ic_ejercicios`, `ic_nutricion`, `ic_perfil`), perfil (`ic_peso`, `ic_altura`, `ic_edad`, `ic_objetivo`, `ic_nivel`, `ic_email`), acciones (`ic_edit`, `ic_add`, `ic_search`, `ic_logout`, `ic_lock`, `ic_arrow_back`, `ic_check`, `ic_error`, `ic_info`, `ic_chevron_right`), decoración (`ic_sun`, `ic_moon`, `ic_language`, `ic_logo_gym`, `ic_calorias`, `ic_tiempo`), fondos (`bg_dialog.xml`, `bg_toast.xml`, `progress_bar_calorias.xml`) |
| `menu/` | `bottom_nav_menu.xml` — menú de navegación inferior con 4 destinos: Home, Ejercicios, Rutinas, Nutrición |
| `values/` | `strings.xml` (español), `colors.xml`, `themes.xml` |
| `values-en/` | `strings.xml` traducidos al inglés (internacionalización) |
| `values-night/` | `colors.xml` y `themes.xml` para el modo oscuro |
| `mipmap-*/` | Icono de la app en todas las densidades (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) |
| `color/` | `bottom_nav_color.xml` — selector de color para los ítems de la navegación inferior (activo/inactivo) |
| `xml/` | `backup_rules.xml` y `data_extraction_rules.xml` |

---

#### ⚙️ Configuración del módulo `app/`

**`build.gradle`** — Lee `BASE_URL` desde `local.properties` y la inyecta en `BuildConfig` para no hardcodear la URL en el código. Incluye la configuración de firma (`signingConfigs`) para generar APKs de release. Dependencias: Retrofit 2, OkHttp Logging Interceptor, Gson, Material Design, AppCompat y ConstraintLayout.

**`AndroidManifest.xml`** — Declara todas las Activities, permisos de internet y la configuración de seguridad de red.

**`local.properties`** *(no versionado)* — Contiene `sdk.dir` y `BASE_URL`. No se sube al repositorio por seguridad.

---

## 🌐 API REST (`api/gymprofit-api`)

Backend desarrollado con Spring Boot 3, Java 21, MariaDB, Flyway para migraciones, jOOQ para consultas avanzadas, MapStruct para mapeo de DTOs, Spring Security con JWT y documentación automática con Swagger/OpenAPI.

### Arquitectura de la API

```
Controller → Service → Repository (JPA / jOOQ) → MariaDB
```

Cada entidad sigue el patrón interfaz + implementación tanto en servicios como en repositorios.

---

### 📁 Estructura de la API

#### `src/main/java/com/gymprofit/api/`

---

#### ⚙️ `config/`

Configuración central de la aplicación.

| Clase | Descripción |
|---|---|
| `SecurityConfig.java` | Configuración de Spring Security. Define CORS abierto, sesiones stateless y reglas de autorización por rol: `/auth/**` y Swagger son públicos; GET de ejercicios, rutinas y alimentos son accesibles para GUEST+; escritura (POST/PUT/DELETE) en ejercicios y rutinas requiere ADMIN; gestión del perfil propio requiere USER o ADMIN |
| `FlywayConfig.java` | Configuración de Flyway para la gestión de migraciones de base de datos |
| `JooqConfig.java` | Configuración de jOOQ para consultas SQL tipadas y avanzadas |
| `SwaggerConfig.java` | Configuración de SpringDoc OpenAPI. Documentación interactiva disponible en `/swagger-ui.html` |
| `security/JwtTokenProvider.java` | Genera y valida tokens JWT. Configurable mediante `jwt.secret` y `jwt.expiration` |
| `security/JwtAuthenticationFilter.java` | Filtro que intercepta cada petición, extrae el token JWT de la cabecera `Authorization`, lo valida y establece el contexto de seguridad |
| `security/JwtEntryPoint.java` | Responde con 401 cuando una petición no autenticada accede a un recurso protegido |
| `security/JwtAccessDenied.java` | Responde con 403 cuando un usuario autenticado no tiene permisos suficientes |

---

#### 🎮 `controller/`

Controladores REST. Todos están documentados con anotaciones Swagger (`@Tag`, `@Operation`, `@ApiResponse`).

| Controlador | Ruta base | Descripción |
|---|---|---|
| `AuthController` | `/auth` | Login (`POST /login`) y registro (`POST /register`). Endpoints públicos sin autenticación |
| `UsuarioController` | `/api/usuarios` | CRUD completo. Búsqueda por username y email. Activación/desactivación. Verificación de existencia de username y email. Solo ADMIN puede listar todos los usuarios |
| `EjercicioController` | `/api/ejercicios` | CRUD completo. Filtros por grupo muscular, dificultad y nombre. Activación/desactivación y eliminación permanente. Escritura solo para ADMIN |
| `RutinaController` | `/api/rutinas` | CRUD de rutinas. Filtros por usuario, nivel, nombre y estado (activas/predefinidas). Activación/desactivación y eliminación permanente |
| `RutinaEjercicioController` | `/api/rutinas-ejercicios` | Gestión de la relación entre rutinas y ejercicios (qué ejercicios contiene cada rutina, con series, reps y orden) |
| `SesionEntrenamientoController` | `/api/sesiones` | Registro de sesiones de entrenamiento completadas por el usuario |
| `EjercicioRealizadoController` | `/api/ejercicios-realizados` | Registro de ejercicios realizados dentro de una sesión (series, repeticiones, peso, tiempo, notas) |
| `ProgresoEjercicioController` | `/api/progreso-ejercicios` | Seguimiento del progreso por ejercicio: mejor peso, mejor tiempo, número de repeticiones |
| `MedicionCorporalController` | `/api/mediciones-corporales` | Registro de mediciones corporales: peso, altura, IMC, grasa corporal, masa muscular, perímetros |
| `ObjetivoPersonalController` | `/api/objetivos-personales` | Gestión de objetivos personales con seguimiento de progreso y fecha límite |
| `AlimentoController` | `/api/alimentos` | CRUD del catálogo de alimentos con información nutricional completa |
| `ComidaController` | `/api/comidas` | Registro de comidas diarias por usuario (desayuno, almuerzo, comida, merienda, cena, snack) |
| `AlimentoComidaController` | `/api/alimentos-comida` | Relación entre alimentos y comidas con cantidad en gramos y calorías totales |
| `NotificacionController` | `/api/notificaciones` | Gestión de notificaciones del sistema (recordatorios, logros, objetivos, sistema) |
| `EjercicioJooqController` | `/api/jooq/ejercicios` | Consultas avanzadas de ejercicios usando jOOQ (accesible por GUEST, USER y ADMIN) |
| `UsuarioJooqController` | `/api/jooq/usuarios` | Consultas avanzadas de usuarios usando jOOQ (solo ADMIN) |

---

#### 📦 `dto/`

Objetos de transferencia de datos organizados por función.

**`auth/`** — DTOs de autenticación: `LoginDTO` (username + password), `RegisterDTO` (username + password + email), `TokenDTO` (token JWT + username + roles).

**`entity/`** — DTOs de entidades. Cada entidad tiene su propio paquete con variantes `DTO` (lectura), `CreateDTO` (creación) y en algunos casos `UpdateDTO` (actualización parcial):

`alimento`, `alimentocomida`, `comida`, `ejercicio`, `ejerciciorealizado`, `medicioncorporal`, `notificacion`, `objetivopersonal`, `progresoejercicio`, `rutina`, `rutinaejercicio`, `sesionentrenamiento`, `usuario`.

**`jooq/`** — DTOs específicos para las consultas jOOQ: `EjercicioJooqDTO` y `UsuarioJooqDTO` con campos optimizados para esas consultas.

---

#### 🗃️ `entity/`

Entidades JPA que mapean directamente con las tablas de la base de datos.

| Entidad | Tabla | Descripción |
|---|---|---|
| `Usuario` | `usuarios` | Usuario con datos de perfil, credenciales e implementación de `UserDetails` para Spring Security |
| `Role` | `roles` | Rol del sistema (ADMIN, USER, GUEST) |
| `Ejercicio` | `ejercicios` | Ejercicio del catálogo con grupo muscular, dificultad, instrucciones y calorías quemadas |
| `Rutina` | `rutinas` | Rutina de entrenamiento con flag `esPredefinida` para distinguir las del sistema de las del usuario |
| `RutinaEjercicio` | `rutina_ejercicios` | Relación muchos a muchos entre rutinas y ejercicios con series, repeticiones, peso recomendado y orden |
| `SesionEntrenamiento` | `sesiones_entrenamiento` | Sesión completada con fecha, duración real y calorías quemadas |
| `EjercicioRealizado` | `ejercicios_realizados` | Registro detallado de un ejercicio en una sesión (series, reps, peso, tiempo, notas) |
| `ProgresoEjercicio` | `progreso_ejercicios` | Mejor marca histórica del usuario en cada ejercicio |
| `MedicionCorporal` | `mediciones_corporales` | Medición con IMC, grasa corporal, masa muscular y perímetros corporales |
| `ObjetivoPersonal` | `objetivos_personales` | Objetivo con valor actual, valor meta, unidad, fechas de inicio/límite y estado de completado |
| `Alimento` | `alimentos` | Alimento del catálogo con macronutrientes (calorías, proteínas, carbohidratos, grasas, fibra) por porción |
| `Comida` | `comidas` | Registro de comida diaria con totales nutricionales calculados |
| `AlimentoComida` | `alimentos_comida` | Alimento dentro de una comida con cantidad en gramos y calorías totales |
| `Notificacion` | `notificaciones` | Notificación con tipo, título, mensaje, fecha programada y estado de lectura |

---

#### 🔢 `enums/`

Enumeraciones que tipifican los valores posibles de los campos.

| Enum | Valores |
|---|---|
| `GrupoMuscular` | `PECHO`, `ESPALDA`, `PIERNAS`, `HOMBROS`, `BRAZOS`, `ABDOMEN`, `CARDIO`, `FULLBODY` |
| `Dificultad` | `PRINCIPIANTE`, `INTERMEDIO`, `AVANZADO` |
| `Nivel` | `PRINCIPIANTE`, `INTERMEDIO`, `AVANZADO`, `EXPERTO` |
| `NivelExperiencia` | `PRINCIPIANTE`, `INTERMEDIO`, `AVANZADO`, `EXPERTO` |
| `TipoComida` | `DESAYUNO`, `ALMUERZO`, `COMIDA`, `MERIENDA`, `CENA`, `SNACK` |
| `TipoNotificacion` | `RECORDATORIO`, `LOGRO`, `OBJETIVO`, `SISTEMA` |
| `TipoObjetivo` | `PERDER_PESO`, `GANAR_MASA_MUSCULAR`, `MEJORAR_RESISTENCIA`, `MEJORAR_FLEXIBILIDAD`, `MEJORAR_FUERZA`, `MANTENER_PESO`, `REDUCIR_GRASA_CORPORAL`, `MEJORAR_VELOCIDAD`, `AUMENTAR_CALORIAS`, `MEJORAR_MOVILIDAD`, `COMPLETAR_RETO`, `OTRO` |
| `RoleType` | `ADMIN`, `USER`, `GUEST` |

---

#### 🔧 `exceptions/`

Sistema de manejo de excepciones centralizado.

**`ControllerExceptionHandler.java`** — Manejador global (`@RestControllerAdvice`). Captura todas las excepciones personalizadas y devuelve respuestas JSON estructuradas con el código HTTP apropiado.

| Excepción | HTTP | Descripción |
|---|---|---|
| `NotFoundEntityException` | 404 | Entidad no encontrada en la base de datos |
| `DuplicateEntityException` | 409 | Entidad duplicada (username o email ya existe) |
| `InvalidCredentialsException` | 401 | Credenciales incorrectas en el login |
| `InvalidDataException` | 400 | Datos de entrada inválidos o vacíos |
| `UnauthorizedException` | 403 | Sin permisos para la operación |
| `CreateEntityException` | 500 | Error al crear la entidad |
| `UpdateEntityException` | 500 | Error al actualizar la entidad |
| `DeleteEntityException` | 500 | Error al eliminar la entidad |
| `ObjetivoAlreadyCompletedException` | 400 | El objetivo ya estaba marcado como completado |
| `SesionNotCompletedException` | 400 | La sesión de entrenamiento no está completada |
| `ErrorGenericoException` | 500 | Error genérico del servidor |

---

#### 🗺️ `mappers/`

Interfaces MapStruct para la conversión automática entre entidades JPA y DTOs. MapStruct genera las implementaciones en tiempo de compilación, eliminando código boilerplate de conversión manual.

Mappers disponibles: `AlimentoMapper`, `AlimentoComidaMapper`, `ComidaMapper`, `EjercicioMapper`, `EjercicioRealizadoMapper`, `MedicionCorporalMapper`, `NotificacionMapper`, `ObjetivoPersonalMapper`, `ProgresoEjercicioMapper`, `RutinaMapper`, `RutinaEjercicioMapper`, `SesionEntrenamientoMapper`, `UsuarioMapper`.

---

#### 🗄️ `repository/`

Capa de acceso a datos con dos tecnologías diferenciadas según la complejidad de la consulta.

**`jpa/`** — Repositorios Spring Data JPA (interfaces que extienden `JpaRepository`). Para operaciones CRUD estándar y consultas derivadas del nombre del método: `IEjercicioRepository`, `IRutinaRepository`, `IUsuarioRepository`, `IRoleRepository`, `ISesionEntrenamientoRepository`, `IEjercicioRealizadoRepository`, `IProgresoEjercicioRepository`, `IMedicionCorporalRepository`, `IObjetivoPersonalRepository`, `IAlimentoRepository`, `IComidaRepository`, `IAlimentoComidaRepository`, `INotificacionRepository`.

**`jooq/`** — Repositorios jOOQ para consultas SQL complejas y tipadas, generadas automáticamente a partir del esquema de base de datos. Se usan cuando JPA no puede expresar la consulta de forma eficiente: `EjercicioJooqRepository` / `IEjercicioJooqRepository` y `UsuarioJooqRepository` / `IUsuarioJooqRepository`.

---

#### 🔬 `service/`

Capa de lógica de negocio. Cada servicio implementa su interfaz correspondiente. Los servicios gestionan la validación de datos, el mapeo DTO↔entidad mediante MapStruct, el llamado al repositorio y el lanzamiento de excepciones personalizadas.

Servicios: `AuthService` (login y registro con BCrypt), `UsuarioService` (CRUD + `UserDetailsService` para Spring Security), `EjercicioService`, `RutinaService`, `RutinaEjercicioService`, `SesionEntrenamientoService`, `EjercicioRealizadoService`, `ProgresoEjercicioService`, `MedicionCorporalService`, `ObjetivoPersonalService`, `AlimentoService`, `ComidaService`, `AlimentoComidaService`, `NotificacionService`.

---

#### 📝 `src/main/resources/`

| Archivo | Descripción |
|---|---|
| `application.properties` | Configuración principal. Activa el perfil `dev` por defecto, expone la API en el puerto 8080 con context-path `/api`, configura Flyway y Swagger |
| `application-dev.properties` | Configuración del entorno de desarrollo (conexión a BD local, nivel de logging DEBUG) |
| `application-prod.properties` | Configuración del entorno de producción |
| `application-example.properties` | Plantilla de configuración para nuevos desarrolladores con todos los parámetros necesarios y valores de ejemplo |
| `logback-spring.xml` | Configuración de logging. Genera archivos de log diarios en `logs/` con el patrón `gymprofit_YYYY-MM-DD.log` |

**`db/migration/`** — Migraciones Flyway versionadas en orden cronológico:

| Migración | Descripción |
|---|---|
| `202603022100__GymProFitDB_MigracionInicial.sql` | Creación de las 13 tablas principales del sistema |
| `202603231235__Cambios_Entidades.sql` | Ajustes en la estructura de entidades existentes |
| `202603251830__Fix_mejor_repeticiones_tipo.sql` | Corrección del tipo de dato de repeticiones en la tabla de progreso |
| `202604051853__Add_Auth_Roles.sql` | Añade las tablas `roles` y `usuario_roles` para el sistema de autenticación por roles |

---

#### 🧪 `src/test/`

Suite de tests con JUnit 5, Mockito y Spring Security Test.

**Tests de controladores** (`controller/`): `AuthControllerTest`, `EjercicioControllerTest`, `RutinaControllerTest`.

**Tests de servicios** (`service/`): `AuthServiceTest`, `EjercicioServiceTest`, `RutinaServiceTest`, `SesionEntrenamientoServiceTest`, `UsuarioServiceTest`.

---

## 🗄️ Base de datos (`db/`)

**`schema.sql`** — Script SQL completo para la creación inicial de la base de datos `GymProFitDB` en MariaDB/MySQL. Define todas las tablas del sistema con sus relaciones, índices y restricciones de integridad referencial.

### Esquema de tablas

| Tabla | Descripción |
|---|---|
| `usuarios` | Usuarios con datos de perfil (peso, altura, edad, nivel, objetivo), credenciales e índices en `username` y `email` |
| `roles` | Roles del sistema: ADMIN, USER, GUEST |
| `usuario_roles` | Tabla de unión muchos a muchos entre usuarios y roles |
| `ejercicios` | Catálogo de ejercicios con grupo muscular (enum), dificultad (enum), instrucciones, calorías e índices |
| `rutinas` | Rutinas con flag `es_predefinida` para distinguir las del sistema de las creadas por usuarios |
| `rutina_ejercicios` | Relación ejercicios-rutinas con series, repeticiones, peso recomendado, tiempo de descanso y orden |
| `sesiones_entrenamiento` | Sesiones completadas con fecha de inicio/fin, duración real y calorías quemadas |
| `ejercicios_realizados` | Detalle de cada ejercicio en una sesión: series, repeticiones, peso, tiempo y notas |
| `progreso_ejercicios` | Mejor marca histórica del usuario en cada ejercicio (mejor peso, mejor tiempo, mejor repeticiones) |
| `mediciones_corporales` | Evolución de medidas: peso, altura, IMC, grasa corporal, masa muscular y perímetros (cintura, pecho, brazos, piernas) |
| `objetivos_personales` | Objetivos con valor actual, valor meta, unidad, fechas de inicio/límite y timestamp de completado |
| `alimentos` | Catálogo nutricional: calorías, proteínas, carbohidratos, grasas y fibra por porción en gramos |
| `comidas` | Registros de comidas diarias por usuario con totales nutricionales calculados |
| `alimentos_comida` | Alimentos incluidos en cada comida con cantidad en gramos y calorías calculadas |
| `notificaciones` | Notificaciones con tipo (enum), título, mensaje, fecha de creación, fecha programada y estado de lectura |

---

## 🛠️ Tecnologías

### App Android

| Tecnología | Uso |
|---|---|
| Java 11 | Lenguaje principal |
| Android SDK 36 (min 24) | Plataforma objetivo |
| Retrofit 2 | Cliente HTTP para la API REST |
| OkHttp + Logging Interceptor | Gestión de peticiones y logs de red |
| Gson | Serialización/deserialización JSON |
| Material Design 3 | Componentes visuales y temas |
| SharedPreferences | Persistencia local del token JWT y datos de sesión |
| Gradle 8.13 | Sistema de construcción |

### API REST

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.5.7 | Framework web |
| Spring Security | — | Autenticación JWT y autorización por roles |
| Spring Data JPA | — | Acceso a datos ORM |
| jOOQ | 3.19 | Consultas SQL avanzadas y tipadas |
| Flyway | 11.15 | Migraciones de base de datos versionadas |
| MapStruct | 1.6.3 | Mapeo automático DTO↔entidad |
| Lombok | 1.18.38 | Reducción de código boilerplate |
| JJWT | 0.13.0 | Generación y validación de tokens JWT |
| SpringDoc OpenAPI | 2.8.15 | Documentación Swagger automática |
| MariaDB | — | Base de datos relacional |
| JUnit 5 + Mockito | — | Testing unitario e integración |
| Maven | — | Gestión de dependencias y build |

---

## 📋 Requisitos previos

- **Android Studio** Hedgehog o superior
- **JDK 21** para la API y **JDK 11** para la app Android
- **MariaDB** 10.6+ o MySQL 8+
- **Maven** 3.8+ (o usar el wrapper `./mvnw` incluido en la API)

---

## 🚀 Instalación y configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/ruubeenn13/TFG-GymProFit.git
cd TFG-GymProFit
```

### 2. Configurar la base de datos

```sql
mysql -u root -p < db/schema.sql
```

O deja que Flyway cree las tablas automáticamente al arrancar la API (recomendado).

### 3. Configurar la API

```bash
cd api/gymprofit-api/src/main/resources
cp application-example.properties application-dev.properties
```

Edita `application-dev.properties` con tus credenciales de base de datos y tu clave JWT secreta.

### 4. Arrancar la API

```bash
cd api/gymprofit-api
./mvnw spring-boot:run
```

La API estará disponible en `http://localhost:8080/api`.
La documentación Swagger en `http://localhost:8080/api/swagger-ui.html`.

### 5. Configurar la App Android

Crea el archivo `local.properties` en `app/GymProFit/` (si no existe ya):

```properties
sdk.dir=C\:\\Users\\TuUsuario\\AppData\\Local\\Android\\Sdk
BASE_URL=http://10.0.2.2:8080/api/
```

> `10.0.2.2` es la IP que el emulador de Android usa para acceder a `localhost` de tu máquina. Si usas un dispositivo físico, usa la IP local de tu PC en la red.

### 6. Ejecutar la App

Abre el proyecto `app/GymProFit` en Android Studio, sincroniza Gradle (**File → Sync Project with Gradle Files**) y ejecuta en un emulador o dispositivo físico.

---

<div align="center">

Desarrollado con ❤️ por **Rubén Juan Candela**
CFGS Desarrollo de Aplicaciones Multimedia · 2º DAM · 2026

</div>
