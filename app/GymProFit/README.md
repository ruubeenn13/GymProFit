<div align="center">

# GymProFit — App Android

### Aplicación nativa de gestión de entrenamiento

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Material Design](https://img.shields.io/badge/Material_Design_3-757575?style=for-the-badge&logo=material-design&logoColor=white)

*App Android del TFG GymProFit — CFGS 2º DAM*

</div>

---

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java 11 | Lenguaje |
| Android SDK 36 (minSdk 24) | Plataforma objetivo |
| HttpURLConnection + AsyncTask | Cliente HTTP (arquitectura UD06 requerida por el profesor) |
| org.json | Parseo JSON nativo Android — sin dependencias externas |
| Material Design 3 | Componentes visuales y temas (claro/oscuro) |
| SharedPreferences | Persistencia local del token JWT y configuración |
| Gradle 8.13 | Build system |

---

## Configuración rápida

### 1. Requisitos previos

- Android Studio Hedgehog o superior
- JDK 11
- API en ejecución en `localhost:8080` (ver `api/gymprofit-api/README.md`)

### 2. Crear `local.properties`

En la raíz del módulo (`app/GymProFit/local.properties`):

```properties
sdk.dir=C\:\\Users\\TuUsuario\\AppData\\Local\\Android\\Sdk
BASE_URL=http://10.0.2.2:8080/api/
```

> `10.0.2.2` es la IP del host desde el emulador Android. Con dispositivo físico, usar la IP local de la máquina (p.ej. `192.168.1.x`).

### 3. Ejecutar

Abrir `app/GymProFit` en Android Studio → sincronizar Gradle → Run en emulador API 24+.

---

## Arquitectura

Sin Fragments, sin ViewModel, sin Room, sin Compose. Solo Activities puras.

### Arquitectura de red — 4 capas (UD06)

```
┌─────────────────────────────────────────┐
│  Capa 1: model/                          │
│  POJOs puros, sin dependencias externas  │
├─────────────────────────────────────────┤
│  Capa 2: network/UtilREST.java          │
│  HttpURLConnection + AsyncTask           │
│  Token JWT estático. PATCH vía reflexión │
├─────────────────────────────────────────┤
│  Capa 3: network/UtilJSONParser.java    │
│  org.json nativo. parseXxx + parseXxxList│
├─────────────────────────────────────────┤
│  Capa 4: network/API.java               │
│  Fachada estática. Todos los endpoints   │
└─────────────────────────────────────────┘
```

### Estructura de paquetes

```
es.pmdm.gymprofit/
├── model/
│   ├── usuario/       # Usuario.java
│   ├── ejercicio/     # Ejercicio.java
│   ├── rutina/        # Rutina.java, RutinaEjercicio.java
│   ├── sesion/        # SesionEntrenamiento.java
│   ├── logro/         # Logro.java, UsuarioLogro.java
│   ├── medicion/      # MedicionCorporal.java
│   └── objetivo/      # ObjetivoPersonal.java
├── network/
│   ├── UtilREST.java          # Motor HTTP
│   ├── UtilJSONParser.java    # Parser JSON
│   └── API.java               # Fachada de endpoints
├── ui/
│   ├── activities/            # Una Activity por pantalla
│   └── adapters/              # RecyclerView.Adapter por cada lista
└── utils/
    ├── PreferencesManager.java
    ├── CalculadoraNutricional.java
    ├── ResultadoNutricional.java
    ├── UIHelper.java
    └── NotificationHelper.java
```

---

## Flujo de navegación

```
SplashActivity
     ↓
LoginActivity ←→ RegistroActivity
     ↓ (primer acceso)
Onboarding (1 → 2 → 3 → 4 → Resumen)
     ↓
HomeActivity (navegación inferior)
  ├── EjerciciosActivity
  ├── RutinasActivity
  │     ├── CrearRutinaActivity → AnadirEjerciciosActivity → ResumenCrearRutinaActivity
  │     └── DetalleRutinaActivity → EditarRutinaActivity
  ├── NutricionActivity
  └── PerfilActivity → EditarPerfilActivity
        ├── SesionesActivity → RegistrarSesionActivity → ResumenSesionActivity
        ├── MedicionesActivity → RegistrarMedicionActivity
        ├── LogrosActivity
        └── AdminActivity  (solo ROLE_ADMIN)
```

---

## Pantallas

| Activity | Descripción |
|---|---|
| `SplashActivity` | Launcher. Restaura token JWT → Home o Login |
| `LoginActivity` | POST /auth/login. Guarda token, id, username, rol |
| `RegistroActivity` | POST /auth/register |
| `Onboarding1–4Activity` | Configuración inicial: objetivo, nivel, datos físicos |
| `OnboardingResumenActivity` | Cálculo nutricional + PUT /usuarios |
| `HomeActivity` | Saludo contextual, fecha locale-aware. Detecta JWT expirado (401) → redirige a Login |
| `EjerciciosActivity` | Catálogo con buscador y filtro por grupo muscular |
| `RutinasActivity` | Listado rutinas del usuario + predefinidas. Filtro por nivel |
| `CrearRutinaActivity` | Paso 1/3: nombre, descripción, nivel, duración |
| `AnadirEjerciciosActivity` | Paso 2/3: buscador + filtro dificultad, selección con series/reps |
| `ResumenCrearRutinaActivity` | Paso 3/3: revisión + POST /rutinas + POST /rutinas-ejercicios × N |
| `DetalleRutinaActivity` | Vista readonly. Botón editar visible solo si es rutina propia |
| `EditarRutinaActivity` | PATCH nombre/desc/nivel/duración + añadir/eliminar ejercicios |
| `NutricionActivity` | Calculadora macros y agua (resultado del onboarding) |
| `PerfilActivity` | Datos reales de la API, cambio tema/idioma, logout |
| `EditarPerfilActivity` | PATCH /usuarios/{id}. Campos vacíos → null en BD |
| `SesionesActivity` | Historial de sesiones, eliminar |
| `RegistrarSesionActivity` | Crear sesión: spinner rutinas, calorías calculadas automáticamente, RatingBar 1-5 |
| `ResumenSesionActivity` | Detalle sesión + 6 stats de usuario + logros desbloqueados |
| `MedicionesActivity` | Historial mediciones corporales, eliminar |
| `RegistrarMedicionActivity` | POST /mediciones-corporales |
| `LogrosActivity` | Todos los logros con desbloqueados resaltados |
| `AdminActivity` | Estadísticas globales + lista usuarios (solo ROLE_ADMIN) |

---

## Utilidades

### `PreferencesManager`

Encapsula todo acceso a SharedPreferences. Nunca acceder directamente desde una Activity.

```java
prefs.getToken()              // JWT Bearer token
prefs.getUsuarioId()          // int id del usuario logado
prefs.getUsername()           // String username
prefs.getRol()                // "ROLE_USER" / "ROLE_ADMIN" / "ROLE_GUEST"
prefs.isAdmin()               // true si rol == "ROLE_ADMIN"
prefs.isOnboardingCompletado()
prefs.haySesion()             // true si hay token no vacío
prefs.applyTheme()            // aplicar antes de setContentView en todo onCreate
prefs.cerrarSesion()          // limpia token + id + username (no elimina tema ni idioma)
```

### `UIHelper`

```java
UIHelper.mostrarToastExito(ctx, msg)
UIHelper.mostrarToastError(ctx, msg)
UIHelper.mostrarToastInfo(ctx, msg)
UIHelper.mostrarDialogo(ctx, titulo, msg, runnable)
UIHelper.mostrarDialogoConIcono(ctx, titulo, msg, R.drawable.ic_delete, runnable)
```

Diálogos: ancho = 90% de la pantalla. Icono papelera: `@drawable/ic_delete` (color `?attr/colorError`).

### `NotificationHelper`

```java
NotificationHelper.notificarSesionCompletada(ctx, duracionMinutos, calorias)
NotificationHelper.notificarMedicionGuardada(ctx)
NotificationHelper.notificarRutinaCreada(ctx, nombreRutina)
NotificationHelper.notificarLogrosDesbloqueados(ctx, List<String> nombres)
```

4 canales (API 26+). Permiso `POST_NOTIFICATIONS` solicitado en runtime en `HomeActivity` (API 33+).

### `CalculadoraNutricional`

Mifflin-St Jeor con factor de actividad y distribución de macros por objetivo. Devuelve `ResultadoNutricional` (calorías, proteínas, carbos, grasas, agua).

---

## Endpoints usados desde Android

Base URL: `BuildConfig.BASE_URL` desde `local.properties`.

```
POST   auth/login                                          → token + username
POST   auth/register
GET    usuarios/{id}
PATCH  usuarios/{id}
GET    usuarios/{id}/estadisticas
GET    ejercicios/activos
GET    ejercicios/grupo/{grupoMuscular}
GET    rutinas/predefinidas
GET    rutinas/usuario/{usuarioId}
POST   rutinas
PATCH  rutinas/{id}
DELETE rutinas/{id}
GET    rutinas-ejercicios/rutina/{rutinaId}
POST   rutinas-ejercicios
DELETE rutinas-ejercicios/rutina/{rId}/ejercicio/{eId}
GET    sesiones/usuario/{usuarioId}
GET    sesiones/{id}
POST   sesiones
DELETE sesiones/{id}
GET    mediciones-corporales/usuario/{id}/ordenadas
POST   mediciones-corporales
DELETE mediciones-corporales/{id}
GET    logros
GET    logros/usuario/{usuarioId}
GET    admin/usuarios
GET    admin/estadisticas-globales
```

---

## Trampas conocidas

| Problema | Solución |
|---|---|
| `AsyncTask` deprecado | El profesor lo exige (UD06). No sustituir |
| Token se pierde al matar el proceso | `SplashActivity` lo restaura con `UtilREST.setToken(token)` |
| PATCH no soportado nativamente en Android | Workaround con reflexión Java en `UtilREST` |
| `optString()` devuelve `"null"` literal cuando el campo JSON es null | Filtrar siempre: `!"null".equals(s)` |
| `peso` debe enviarse como Double, no String | `Double.parseDouble(str.replace(",", "."))` |
| Tema/idioma deben aplicarse antes de `setContentView` | Orden en `onCreate`: `applyTheme()` → `aplicarIdioma()` → `setContentView()` |
| Filtros de enum en adapters usan `equalsIgnoreCase` | La API devuelve enums en UPPERCASE |
| `fecha_fin` NOT NULL en sesiones | La API calcula `fechaInicio + duracionMinutos` si no se envía |

---

## Dependencias (`build.gradle`)

Sin Retrofit, OkHttp ni Gson. Solo dependencias Android estándar:

```groovy
implementation libs.appcompat
implementation libs.material
implementation libs.activity
implementation libs.constraintlayout
```

`org.json` es nativo de Android — no requiere dependencia adicional.

---

<div align="center">

Desarrollado por **Rubén Juan Candela** · CFGS 2º DAM · 2026

</div>
