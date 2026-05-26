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
│   ├── objetivo/      # ObjetivoPersonal.java
│   └── alimento/      # Alimento.java, Comida.java, AlimentoComida.java
├── network/
│   ├── UtilREST.java          # Motor HTTP
│   ├── UtilJSONParser.java    # Parser JSON
│   └── API.java               # Fachada de endpoints
├── ui/
│   ├── activities/
│   │   ├── BaseActivity.java  # Clase base: menú opciones (tema, idioma, contáctanos, logout) vía dialogs
│   │   └── ...                # Una Activity concreta por pantalla
│   └── adapters/              # RecyclerView.Adapter por cada lista
└── utils/
    ├── PreferencesManager.java
    ├── EjercicioNavHelper.java    # Construye y lanza el Intent a DetalleEjercicioActivity
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
  ├── EjerciciosActivity → DetalleEjercicioActivity (vídeo + instrucciones)
  ├── RutinasActivity
  │     ├── CrearRutinaActivity → AnadirEjerciciosActivity → ResumenCrearRutinaActivity
  │     └── DetalleRutinaActivity → EditarRutinaActivity
  │           └── (click ejercicio) → DetalleEjercicioActivity
  ├── NutricionActivity → ComidaActivity → AnadirAlimentoActivity → CrearAlimentoActivity
  └── PerfilActivity → EditarPerfilActivity
        ├── SesionesActivity → RegistrarSesionActivity → ResumenSesionActivity
        ├── MedicionesActivity → RegistrarMedicionActivity
        ├── LogrosActivity
        ├── AcercaDeActivity
        └── AdminActivity  (solo ROLE_ADMIN)
              └── AdminAlimentosActivity
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
| `HomeActivity` | Saludo contextual, fecha locale-aware. Estadísticas reales de la semana actual (entrenamientos, calorías, minutos) cargadas desde API. Detecta JWT expirado (401) → redirige a Login |
| `EjerciciosActivity` | Catálogo con buscador y filtro por grupo muscular |
| `RutinasActivity` | Listado rutinas del usuario + predefinidas. Filtro por nivel. Long-press contextual: admin en predefinidas (Editar→`EditarRutinaAdminActivity` + Desactivar/Activar) o en propias (Editar + Eliminar); usuario solo en propias (Editar + Eliminar) |
| `CrearRutinaActivity` | Paso 1/3: nombre, descripción, nivel, duración |
| `AnadirEjerciciosActivity` | Paso 2/3: buscador + filtro dificultad, selección con series/reps |
| `ResumenCrearRutinaActivity` | Paso 3/3: revisión + POST /rutinas + POST /rutinas-ejercicios × N |
| `DetalleRutinaActivity` | Vista readonly. Botón editar visible solo si es rutina propia |
| `EditarRutinaActivity` | PATCH nombre/desc/nivel/duración + añadir/eliminar ejercicios |
| `NutricionActivity` | Rediseñada: objetivos dinámicos en `onResume` (CalculadoraNutricional con datos frescos del perfil), barras de progreso consumido vs objetivo (rojo si supera), 5 cards de comida (DESAYUNO..CENA) → ComidaActivity |
| `ComidaActivity` | Log diario de una comida: header con totales (calorías/macros), RecyclerView AlimentoComidaAdapter, FAB → AnadirAlimentoActivity, long-press → popup contextual anclado (editar cantidad / desactivar / eliminar) |
| `AnadirAlimentoActivity` | SearchView local sobre alimentos activos; click → dialog gramos + preview macros en tiempo real → POST /alimentos-comida (crea comida primero si no existe); long-press en alimento propio → popup contextual anclado (editar/eliminar); botón "Crear alimento" |
| `CrearAlimentoActivity` | Formulario alimento propio (nombre, categoría Spinner, calorías, proteínas, carbos, grasas). POST /alimentos con usuarioId. setResult al caller |
| `AdminAlimentosActivity` | Gestión admin de alimentos: búsqueda, filtros categoría/estado, toggle activo, editar via dialog. Acceso desde AdminActivity (solo ROLE_ADMIN) |
| `PerfilActivity` | Datos reales de la API + resumen de última medición corporal (peso/altura). Hereda de `BaseActivity`. Botón "Sobre GymProFit" al pie |
| `AcercaDeActivity` | Pantalla "Acerca de": logo adaptativo claro/oscuro (`@drawable/logo` + `drawable-night/`), info extendida de la app (descripción, 6 features, tech stack) e info del desarrollador (bio, formación, 3 FCTs, email clickable `ACTION_SENDTO`). Botón "Compartir": pide permiso `READ_CONTACTS` en runtime vía `ActivityResultLauncher`; si se concede abre selector de contactos (`ACTION_PICK Phone.CONTENT_URI`); extrae número via `ContentResolver` y lanza `ACTION_SENDTO smsto:` con el texto pre-rellenado. Extiende `AppCompatActivity`, aplica tema/idioma manualmente |
| `EditarPerfilActivity` | PATCH /usuarios/{id}. Campos vacíos → null en BD |
| `SesionesActivity` | Historial de sesiones, eliminar |
| `RegistrarSesionActivity` | Crear sesión: spinner rutinas, calorías calculadas, cards de ejercicios con campo de peso por ejercicio (RecyclerView+`EjercicioPesoAdapter`), RatingBar 1-5 |
| `ResumenSesionActivity` | Detalle sesión + 6 stats de usuario + logros desbloqueados |
| `MedicionesActivity` | Vista detallada de la última medición corporal (peso, altura, grasa, músculo, perímetros). FAB para registrar nueva |
| `RegistrarMedicionActivity` | POST /mediciones-corporales |
| `LogrosActivity` | Todos los logros con desbloqueados resaltados |
| `AdminActivity` | Panel admin: estadísticas globales (6 KPIs) + acceso a gestión de usuarios, rutinas y ejercicios (solo ROLE_ADMIN) |
| `AdminUsuariosActivity` | Gestión de usuarios: buscar por username, filtrar por estado/rol, toggle activo/inactivo, cambiar rol (solo ROLE_ADMIN) |
| `AdminRutinasActivity` | Gestión de rutinas predefinidas: buscar, filtrar por nivel/estado, toggle activa, editar todos los campos vía `EditarRutinaAdminActivity` (solo ROLE_ADMIN) |
| `AdminEjerciciosActivity` | Gestión del catálogo de ejercicios: buscar, filtrar por estado, toggle activo, editar todos los campos vía `EditarEjercicioAdminActivity` (solo ROLE_ADMIN) |
| `EditarRutinaAdminActivity` | Edición completa de rutina predefinida: nombre, descripción, nivel (Spinner), duración, calorías, categoría, días semana. PATCH /rutinas/{id} (solo ROLE_ADMIN) |
| `EditarEjercicioAdminActivity` | Edición completa de ejercicio: nombre, descripción, grupo muscular (Spinner), dificultad (Spinner), calorías, equipo necesario, instrucciones. PATCH /ejercicios/{id} (solo ROLE_ADMIN) |
| `DetalleEjercicioActivity` | Detalle de ejercicio: reproducción automática de vídeo local (`res/raw/video_<id>.mp4`), stats (músculo, nivel, calorías, equipamiento), descripción e instrucciones |

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
// PopupWindow anclado al elemento que lo dispara:
UIHelper.mostrarMenuAnclado(ctx, anchorView, titulo_nullable, List<UIHelper.MenuAction>)
// MenuAction(iconRes, label, destructive, action) — destructive=true → colorError + separador
```

Diálogos: ancho = 90% de la pantalla. Icono papelera: `@drawable/ic_delete` (color `?attr/colorError`).
`mostrarMenuAnclado`: PopupWindow con fondo redondeado `colorSurface` (12dp), elevación 8dp, alineado al borde derecho del anchor. Items destructivos precedidos de separador y en `colorError`.

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
POST   ejercicios-realizados
GET    mediciones-corporales/usuario/{id}/ordenadas
POST   mediciones-corporales
DELETE mediciones-corporales/{id}
GET    logros
GET    logros/usuario/{usuarioId}
GET    admin/usuarios?page=&size=&activo=&rol=&username=
GET    admin/estadisticas-globales
PATCH  admin/usuarios/{id}/toggle-activo
PATCH  admin/usuarios/{id}/rol?nuevoRol=
GET    admin/rutinas/predefinidas/busqueda?nombre=&nivel=&categoria=&activa=
GET    admin/ejercicios/busqueda?nombre=&grupoMuscular=&dificultad=&activo=
PATCH  rutinas/{id}
DELETE rutinas/{id}          (desactivar)
PUT    rutinas/{id}/activar
PATCH  ejercicios/{id}
DELETE ejercicios/{id}       (desactivar)
PUT    ejercicios/{id}/activar
```

---

## Trampas conocidas

| Problema | Solución |
|---|---|
| `AsyncTask` deprecado | El profesor lo exige (UD06). No sustituir |
| Token se pierde al matar el proceso | `SplashActivity` lo restaura con `UtilREST.setToken(token)` |
| PATCH no soportado nativamente en Android | Workaround con reflexión Java en `UtilREST` |
| `optString()` devuelve `"null"` literal cuando el campo JSON es null | Resuelto en origen: `UtilJSONParser.safeStr()` usa `isNull()` antes de `optString()`. No filtrar en UI |
| `peso` debe enviarse como Double, no String | `Double.parseDouble(str.replace(",", "."))` |
| Tema/idioma deben aplicarse antes de `setContentView` | Orden en `onCreate`: `applyTheme()` → `aplicarIdioma()` → `setContentView()`. `BaseActivity` lo gestiona; las subclases solo llaman `super.onCreate()` primero |
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

## Changelog

### 2026-05-26 — Compartir vía SMS con selector de contactos

- **AcercaDeActivity — compartir**: botón "Compartir app" reemplaza `ACTION_SEND` genérico. Flujo: pide permiso `READ_CONTACTS` en runtime (`ActivityResultLauncher<String>`); si denegado muestra Toast; si concedido abre selector de contactos (`ACTION_PICK` sobre `Phone.CONTENT_URI`); al seleccionar extrae número via `ContentResolver.query`; lanza `ACTION_SENDTO smsto:<numero>` con texto pre-rellenado (`acerca_compartir_texto`). `AndroidManifest`: añadido `uses-permission READ_CONTACTS`.

### 2026-05-26 — AcercaDeActivity

- **AcercaDeActivity**: nueva pantalla accesible desde `PerfilActivity` (botón "Sobre GymProFit" al pie). Header con `@drawable/logo` adaptativo claro/oscuro (via `drawable-night/`). Card app: descripción extendida del proyecto, 6 funcionalidades con `ic_check`, tech stack. Card desarrollador: nombre, rol, centro, bio, formación (2 entradas), 3 FCTs, email clickable (`ACTION_SENDTO`), stack técnico. Strings bilingüe ES+EN (~30 entradas). Cumple requisito módulo Multimedia: "Pantalla de información sobre la aplicación".

### 2026-05-26 — Nutrición completa, BottomSheet menus, admin alimentos
- **NutricionActivity rediseño**: objetivos calculados en `onResume` con datos frescos del perfil (`CalculadoraNutricional`). Barras de progreso consumido vs objetivo: si se supera cualquier macro → `colorError` (rojo estilo Fitia). 5 cards de comida (DESAYUNO, ALMUERZO, COMIDA, MERIENDA, CENA) → `ComidaActivity`
- **ComidaActivity**: log diario de una comida. Header con totales calorías/macros. RecyclerView `AlimentoComidaAdapter`. FAB → `AnadirAlimentoActivity`. Long-press → BottomSheet contextual (editar cantidad / desactivar admin / eliminar)
- **AnadirAlimentoActivity**: SearchView local sobre alimentos activos. Click → dialog gramos + preview macros en tiempo real → `POST /alimentos-comida` (crea comida antes si no existe para ese tipo+día). Long-press en alimento propio → BottomSheet editar/eliminar/desactivar. Botón "Crear alimento" → `CrearAlimentoActivity`
- **CrearAlimentoActivity**: formulario alimento propio (nombre, categoría Spinner, calorías, proteínas, carbos, grasas). `POST /alimentos` con `usuarioId`. `setResult(RESULT_OK)` al caller
- **AdminAlimentosActivity**: lista alimentos activos+inactivos. SearchView + filtros categoría/estado. BottomSheet: toggle activo + editar (dialog). Acceso desde `AdminActivity` (nuevo botón)
- **BottomSheet menus → PopupWindow anclado**: `UIHelper.mostrarMenuAnclado(ctx, anchorView, title, actions)` — los menús contextuales salen anclados al elemento que los dispara (3 puntitos y long-press). PopupWindow con fondo redondeado `colorSurface` 12dp, elevación 8dp, alineado derecha. 8 archivos afectados: `BaseActivity`, `AnadirAlimentoActivity`, `ComidaActivity`, `RutinasActivity`, `AdminAlimentoAdapter`, `AdminUsuarioAdapter`, `AdminRutinaAdapter`, `AdminEjercicioAdapter`
- **API.java**: 13 nuevos métodos para nutrición (alimentos, comidas, alimentos-comida)
- **UtilJSONParser.java**: parsers para Alimento, Comida, AlimentoComida

### 2026-05-26 — Foto de perfil, fix BottomNav dark mode
- **Foto de perfil en PerfilActivity**: avatar clickable (`FrameLayout` 72dp + `ShapeableImageView` 64dp circular + badge edición). Launchers: galería (`GetContent`, sin permisos) + cámara (`TakePicture`, solicita `CAMERA`). `FileProvider` en manifest para URI de cámara. `UtilREST.uploadMultipart()` + `API.uploadFotoPerfil()` — `POST /usuarios/{id}/foto`. Carga foto en `onResume` con `GET /usuarios/{id}/foto`
- **Fix BottomNav dark mode**: `values-night/themes.xml` no tenía `bottomNavigationStyle` → iconos perdían estilo en modo oscuro. Añadido al tema night

### 2026-05-26 — Contáctanos, rediseño card entrenamientos, fix bottom nav
- **Contáctanos en menú**: `BaseActivity` añade ítem `menu_contactanos` (ic_email) en el popup de opciones. Click abre `Intent.createChooser` con `ACTION_SENDTO` + `mailto:` → email chooser precargado con destinatario, asunto y cuerpo fijos. Strings bilingües (ES/EN) en `menu_contactanos`, `email_contacto_asunto`, `email_contacto_cuerpo`
- **Card entrenamientos Home**: fondo cambiado a `colorSurface`, número `colorPrimary` 44sp bold, icono `ic_ejercicios` 32dp dentro de `FrameLayout` 60dp circular (`bg_circle.xml` + `backgroundTint="?attr/colorPrimary"` + `app:tint="?attr/colorOnPrimary"`). Label unificado: `home_entrenamientos_semana` ("Entrenamientos esta semana") en una sola línea encima del número
- **Bottom nav**: `design_bottom_navigation_height=56dp` (nuevo `dimens.xml`) + `android:minHeight=56dp` en estilo + `itemPaddingTop=10dp` / `itemPaddingBottom=6dp` → acerca iconos a labels (~4dp gap)

### 2026-05-25 — Pesos por serie, stats home, admin edición completa, menú long-press
- **Pesos por serie en RegistrarSesion**: al seleccionar rutina aparece card con `EjercicioPesoAdapter` (RecyclerView+TextWatcher); al finalizar sesión hace `POST /ejercicios-realizados` por cada ejercicio con `pesoUsado` opcional
- **Stats reales en Home**: `onResume` llama a `GET /sesiones/usuario/{id}`, filtra por semana actual (lunes–domingo) y muestra entrenamientos, calorías y minutos reales. Card principal cambiado de `colorPrimary` a `colorPrimaryContainer`
- **Admin edición completa**: `EditarRutinaAdminActivity` (nivel Spinner + duración/calorías/categoría/diasSemana) y `EditarEjercicioAdminActivity` (grupoMuscular+dificultad Spinners + calorías/equipo/instrucciones) reemplazan los diálogos parciales previos
- **Long-press menú corregido**: `RutinaAdapter.setUserContext(isAdmin, userId)` habilita long-press para admin en cualquier rutina o para el propietario en las suyas; menú contextual diferenciado: predefinidas → Editar+Desactivar/Activar, propias → Editar+Eliminar; `ic_visibility_off.xml` nuevo drawable

### 2026-05-25 — DetalleEjercicioActivity + rediseño lista ejercicios en rutinas
- **DetalleEjercicioActivity**: nueva pantalla con vídeo autoplay (`VideoView` + `MediaController` + `setOnPreparedListener`), stats card (músculo/nivel/calorías/equipamiento), descripción e instrucciones. Vídeos en `res/raw/video_<id>.mp4`
- **EjercicioNavHelper**: utilidad compartida que construye el Intent con todos los extras del ejercicio; usada desde `EjerciciosActivity` y `DetalleRutinaActivity`
- **item_ejercicio_seleccionado**: rediseñado como `MaterialCardView` idéntico a `item_ejercicio`, con chip `X × Y series/reps`, chips de dificultad y calorías, chevron o botón eliminar según contexto
- **EjercicioSeleccionadoAdapter**: 3 constructores para mantener compatibilidad; nuevo `clickListener` para abrir `DetalleEjercicioActivity` desde `DetalleRutinaActivity`
- **Fix `UtilJSONParser`**: `safeStr()` con `isNull()` elimina el bug de `optString()` devolviendo `"null"` literal para todos los modelos

### 2026-05-25 — Panel admin completo
- **AdminUsuariosActivity**: búsqueda por username, filtros activo/inactivo/rol, toggle activo con diálogo de confirmación, cambio de rol con RadioGroup — `MaterialAlertDialogBuilder`
- **AdminRutinasActivity**: filtros nivel/activa, búsqueda por nombre, toggle activa, diálogo editar nombre/descripción
- **AdminEjerciciosActivity**: filtros activo, búsqueda por nombre, toggle activo, diálogo editar
- **AdminRutinaAdapter / AdminEjercicioAdapter**: chips activo/inactivo con colores adaptativos, PopupMenu contextual
- **Fix login**: rol guardado desde `TokenDTO.roles[]` (via `parseTokenRol`) en lugar de `UsuarioDTO` que no devuelve el campo rol — `obtenerUsuario` ya no sobrescribe el rol guardado
- **UI dark mode**: `values-night/colors.xml` con colores de chip (#1B5E20/#7F0000 + texto adaptativo), `colorOnSurfaceVariant` en textos secundarios, stroke 1dp en cards, `MaterialAlertDialogBuilder` en todos los diálogos admin

### 2026-05-25 — Mejoras visuales anteriores
- **Logo PNG con soporte dark/light**: reemplazado `ic_logo_gym` por `drawable/logo.png` + `drawable-night/logo.png` en `SplashActivity`, `LoginActivity` y `RegistroActivity`
- **Fix transición BottomNav**: `overridePendingTransition(0,0)` aplicado antes y después de `finish()` en los 5 activities principales para suprimir completamente la animación al cambiar de pestaña
- **Fix flash entre activities**: `android:windowBackground` explícito en `themes.xml` y `values-night/themes.xml` para evitar destello del fondo del sistema durante la transición
- **Fix ancho diálogo idioma en Login**: el diálogo de selección de idioma en `LoginActivity` ahora usa el 88% del ancho de pantalla, igual que los del resto de la app
- **BottomNav sin animación de indicador**: estilo global `Widget.GymProFit.BottomNavigationView` en el tema que elimina el pill animado (`itemActiveIndicatorStyle=@null`), aumenta el icono a 27dp y reduce el padding vertical a 6dp

---

<div align="center">

Desarrollado por **Rubén Juan Candela** · CFGS 2º DAM · 2026

</div>
