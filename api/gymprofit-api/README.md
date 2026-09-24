<div align="center">

# GymProFit API

### REST Backend — Spring Boot 3.5.7

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.7-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)

*API REST del TFG GymProFit — CFGS 2º DAM*

</div>

---

## Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje |
| Spring Boot | 3.5.7 | Framework web |
| Spring Security | — | JWT + autorización por roles |
| Spring Data JPA | — | ORM y CRUD estándar |
| jOOQ | 3.19 | Consultas SQL complejas y tipadas |
| Flyway | 11.15 | Migraciones de esquema |
| MapStruct | 1.6.3 | Mapeo DTO ↔ entidad |
| Lombok | 1.18.38 | Reducción de boilerplate |
| JJWT | 0.13.0 | Generación y validación de tokens JWT |
| SpringDoc OpenAPI | 2.8.15 | Documentación Swagger |
| MariaDB | — | Base de datos relacional |
| JUnit 5 + Mockito | — | 329 tests unitarios, de controlador y de integración |
| Maven | — | Gestión de dependencias |

---

## Configuración rápida

### 1. Requisitos previos

- JDK 21
- MariaDB 10.6+ en `localhost:3308`
- Maven 3.8+ (o usar `./mvnw`)

### 2. Base de datos

```sql
CREATE DATABASE gymprofit_db CHARACTER SET utf8mb4;
```

Aplicar el schema inicial (los scripts Flyway deben ejecutarse manualmente — ver nota abajo):

```powershell
$sql = Get-Content "src\main\resources\db\migration\V202603022100__GymProFitDB_MigracionInicial.sql" -Raw
& "C:\Program Files\MariaDB 11.8\bin\mysql.exe" -h 127.0.0.1 -P 3308 -u root -p gymprofit_db -e $sql
```

Activar usuarios del seed si es necesario:

```sql
UPDATE usuarios SET activo = 1 WHERE activo IS NULL;
```

### 3. Configuración de propiedades

```bash
cp src/main/resources/application-example.properties src/main/resources/application-dev.properties
```

Editar `application-dev.properties`:

```properties
spring.datasource.url=jdbc:mariadb://localhost:3308/gymprofit_db
spring.datasource.username=root
spring.datasource.password=TU_PASSWORD
jwt.secret=TU_SECRET_BASE64
jwt.expiration=604800000
```

### 4. Arrancar

```bash
./mvnw spring-boot:run
```

- API: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`

---

## Arquitectura

```
Controller → Service → Repository (JPA / jOOQ) → MariaDB
```

```
src/main/java/com/gymprofit/api/
├── config/
│   ├── security/          # JwtAuthFilter, JwtProvider, JwtEntryPoint, JwtAccessDenied
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   └── FlywayConfig.java / JooqConfig.java
├── controller/            # Un controller por entidad
├── dto/
│   ├── auth/              # LoginDTO, RegisterDTO, TokenDTO
│   └── entity/{entidad}/  # XxxDTO, XxxCreateDTO, XxxPatchDTO
├── entity/                # Entidades JPA
├── enums/                 # Todos los enums del dominio
├── exceptions/            # ControllerExceptionHandler + excepciones custom
├── mappers/               # Interfaces MapStruct
├── repository/
│   ├── jpa/               # IXxxRepository extends JpaRepository
│   └── jooq/              # Repositorios jOOQ para queries complejas
└── service/{entidad}/     # IXxxService + XxxService
```

---

## Endpoints

Todos van bajo el context-path `/api`. Los `@RequestMapping` de los controllers **no incluyen** `/api`.

> **Falta en esta tabla el módulo de nutrición** (`/alimentos`, `/comidas`,
> `/alimentos-comida`), que llegó después. Manda el código; Swagger en local lo
> lista entero. Dos reglas de ese módulo que no se ven en la ruta y conviene
> saber: el **catálogo** de alimentos (los de `usuario_id` NULL) **solo lo
> escribe un ADMIN**, y los alimentos con dueño solo su dueño; el import de Open
> Food Facts por código de barras sí crea catálogo, porque son productos reales.

### AUTH — `/auth`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| POST | `/auth/login` | No | Login. Body: `{username, password}` → `TokenDTO` |
| POST | `/auth/register` | No | Registro. Body: `{username, password, email}` → 201 |
| POST | `/auth/guest` | No | Login como invitado → `TokenDTO` con ROLE_GUEST |
| POST | `/auth/refresh` | No | Renueva el access token con el refresh opaco, que se rota |
| POST | `/auth/logout` | No | Revoca el refresh token recibido |
| POST | `/auth/change-password` | USER/ADMIN | Cambia la contraseña. El username sale del token, nunca del body. Revoca todas las sesiones |
| POST | `/auth/forgot-password` | No | Pide un código de 6 dígitos por correo. Responde **siempre lo mismo**, exista la cuenta o no |
| POST | `/auth/reset-password` | No | Canjea el código por una contraseña nueva. 15 min de vida, 5 intentos, un solo uso. Revoca todas las sesiones |

### USUARIOS — `/usuarios`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| GET | `/usuarios/{id}` | USER/ADMIN | Usuario por ID |
| GET | `/usuarios/username/{u}` | USER/ADMIN | Usuario por username |
| GET | `/usuarios/{id}/estadisticas` | USER/ADMIN | Estadísticas jOOQ |
| PUT | `/usuarios` | ADMIN | Actualizar completo (id en body) — SecurityConfig: solo ADMIN vía catch-all `/usuarios/**` |
| PATCH | `/usuarios/{id}` | USER/ADMIN | Actualización parcial del perfil. **No** cambia `activo` (lo ignora) ni el correo: un `email` distinto del actual da 400 |
| PUT | `/usuarios/me/email` | USER/ADMIN | Cambia el correo propio. Body: `{email, password}` — reautentica. 400 formato, 403 contraseña, 409 en uso. El usuario sale del token. En el cupo estricto del rate limit. El correo nuevo aún no se verifica (GP-045) |
| GET | `/usuarios` | ADMIN | Todos los usuarios |
| DELETE | `/usuarios/{id}` | ADMIN | Soft delete |
| DELETE | `/usuarios/{id}/permanente` | ADMIN | Eliminar permanentemente |
| POST | `/usuarios/{id}/foto` | USER/ADMIN | Subir foto de perfil (multipart/form-data, campo `foto`). Se guarda en la tabla `fotos_perfil`, no en disco |
| GET | `/usuarios/{id}/foto` | USER/ADMIN | Obtener foto de perfil (bytes JPEG). 404 si no existe |
| DELETE | `/usuarios/me` | USER | **Borra la cuenta y todos sus datos, sin vuelta atrás.** Body: `{password}` — se reautentica antes de tocar nada. El usuario sale del token, nunca de la ruta. En el cupo estricto del rate limit |

### EJERCICIOS — `/ejercicios`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| GET | `/ejercicios/activos` | GUEST+ | Solo activos |
| GET | `/ejercicios/{id}` | GUEST+ | Por ID |
| GET | `/ejercicios/grupo/{grupoMuscular}` | GUEST+ | Por grupo muscular |
| GET | `/ejercicios/nombre/{nombre}` | GUEST+ | Búsqueda por nombre |
| POST | `/ejercicios` | ADMIN | Crear |
| PATCH | `/ejercicios/{id}` | ADMIN | Actualización parcial |
| DELETE | `/ejercicios/{id}` | ADMIN | Desactivar |

### RUTINAS — `/rutinas`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| GET | `/rutinas/predefinidas` | GUEST+ | Predefinidas del sistema |
| GET | `/rutinas/predefinidas/nivel/{nivel}` | GUEST+ | Predefinidas por nivel |
| GET | `/rutinas/{id}` | GUEST+ | Por ID |
| GET | `/rutinas/usuario/{usuarioId}` | USER+ | Del usuario |
| POST | `/rutinas` | USER+ | Crear rutina |
| PATCH | `/rutinas/{id}` | USER+ | Actualización parcial |
| DELETE | `/rutinas/{id}` | USER+ | Desactivar |

### RUTINAS-EJERCICIOS — `/rutinas-ejercicios`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| GET | `/rutinas-ejercicios/rutina/{rutinaId}` | USER+ | Ejercicios de una rutina |
| POST | `/rutinas-ejercicios` | USER+ | Asociar ejercicio (campo `orden` obligatorio) |
| DELETE | `/rutinas-ejercicios/rutina/{rId}/ejercicio/{eId}` | USER+ | Desasociar |

> **`orden` es NOT NULL en BD.** Bean Validation lo permite null, la BD lo rechaza con error 1048. Siempre enviar `"orden": i+1`.

### SESIONES — `/sesiones`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| GET | `/sesiones/{id}` | USER+ | Por ID |
| GET | `/sesiones/usuario/{usuarioId}` | USER+ | Del usuario |
| POST | `/sesiones` | USER+ | Crear sesión (evalúa logros si `completada=true`) |
| POST | `/sesiones/completa` | USER+ | Crear sesión **con sus ejercicios y series** en una transacción. Idempotente |
| PATCH | `/sesiones/{id}` | USER+ | Actualización parcial |
| DELETE | `/sesiones/{id}` | USER+ | Eliminar |

La respuesta de `POST /sesiones` incluye el campo `nuevosLogros: ["Nombre logro", ...]` (solo si se desbloquearon logros nuevos).

**`POST /sesiones/completa` es el camino que usa la app** desde GP-006. Guarda la sesión entera —sesión, ejercicios realizados y series— **o no guarda nada**: antes eran un POST de sesión más uno por ejercicio, y un fallo en medio dejaba la sesión a medias.

> **`claveIdempotencia` es obligatoria.** El cliente genera una por intento de guardado y la **reutiliza al reintentar**: si la misma clave llega dos veces, el servidor devuelve la sesión que ya creó en vez de crear otra. Así un fallo de red que sí llegó al servidor no acaba en dos entrenamientos. Única por `(usuario_id, clave)`, máximo 64 caracteres.

```json
{
  "claveIdempotencia": "8f02abfa-…",
  "rutinaId": 12,
  "fechaInicio": "2026-09-23T12:54:00",
  "duracionMinutos": 55,
  "valoracion": 3,
  "notas": "Buenas sensaciones",
  "completada": true,
  "ejercicios": [
    {
      "ejercicioId": 340,
      "repeticionesReales": 10,
      "series": [{"numero": 1, "repeticiones": 10, "peso": 60.0}]
    }
  ]
}
```

`usuarioId` **no se manda**: sale del token (DEC-013). `rutinaId` puede faltar —entrenamiento libre— y la lista de ejercicios puede venir vacía. `valoracion` es 1..5 (ver abajo).

En cada serie, `numero` (de 1 a 20) y `repeticiones` (de 0 a 100) son obligatorios; `peso` es opcional, de 0 a 500 kg. Sin `numero` la respuesta es un 400.

`POST /sesiones` **se mantiene igual** para las builds repartidas fuera de Play, que no conocen la ruta nueva.

> **`valoracion` es un campo**, no una línea de texto. Hasta GP-070 la app formateaba las estrellas y las metía como primera línea de `notas`; la migración `V202609231430` las extrajo a su columna. `INT NULL`, con `CHECK` de 1 a 5. No valorar es un caso normal.

### LOGROS — `/logros`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| GET | `/logros` | GUEST+ | Todos los logros |
| GET | `/logros/progreso` | USER+ | Catálogo con el estado del usuario del token: conseguido y cuándo, o métrica, umbral y progreso. Sin id en la ruta |
| GET | `/logros/usuario/{usuarioId}` | USER+ | Logros obtenidos por el usuario (solo el propio, salvo ADMIN). La app ya no lo usa |
| POST | `/logros` | ADMIN | Crear logro |
| PUT | `/logros/{id}` | ADMIN | Actualizar logro |

### MEDICIONES — `/mediciones-corporales`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| GET | `/mediciones-corporales/usuario/{id}/ordenadas` | USER+ | Historial del usuario |
| POST | `/mediciones-corporales` | USER+ | Registrar medición |
| PATCH | `/mediciones-corporales/{id}` | USER+ | Actualización parcial de un campo |
| DELETE | `/mediciones-corporales/{id}` | USER+ | Eliminar |

### EJERCICIOS REALIZADOS — `/ejercicios-realizados`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| POST | `/ejercicios-realizados` | USER+ | Registrar ejercicio realizado en una sesión (sesionId, ejercicioId, seriesCompletadas, repeticionesReales, pesoUsado opcional) |

### OBJETIVOS — `/objetivos-personales`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| GET | `/objetivos-personales/usuario/{usuarioId}` | USER+ | Del usuario |
| POST | `/objetivos-personales` | USER+ | Crear objetivo |
| PATCH | `/objetivos-personales/{id}` | USER+ | Actualización parcial |
| DELETE | `/objetivos-personales/{id}` | USER+ | Eliminar |

### ADMIN — `/admin`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| GET | `/admin/usuarios?activo=&rol=&username=&page=&size=` | ADMIN | Usuarios con filtros dinámicos (jOOQ). `rol` acepta `USER`, `ADMIN` o con prefijo `ROLE_` |
| GET | `/admin/estadisticas-globales` | ADMIN | Estadísticas globales: totalUsuarios, usuariosActivos, totalSesiones, sesionesHoy, rutinasPredefinidas, ejerciciosActivos |
| PATCH | `/admin/usuarios/{id}/toggle-activo` | ADMIN | Activar/desactivar usuario. Desactivar revoca todos sus refresh tokens; y el token o el refresh de una cuenta desactivada reciben 401 con `cause: CUENTA_DESACTIVADA` en cualquier ruta |
| PATCH | `/admin/usuarios/{id}/rol?nuevoRol=` | ADMIN | Cambiar rol del usuario (`ROLE_USER` o `ROLE_ADMIN`) |
| GET | `/admin/rutinas/predefinidas/busqueda?nombre=&nivel=&categoria=&activa=` | ADMIN | Buscar rutinas predefinidas con filtros dinámicos (jOOQ) |
| GET | `/admin/ejercicios/busqueda?nombre=&grupoMuscular=&dificultad=&activo=` | ADMIN | Buscar ejercicios del catálogo con filtros dinámicos (jOOQ) |
| GET | `/admin/alimentos/busqueda?nombre=&categoria=&activo=` | ADMIN | Buscar alimentos con filtros dinámicos (jOOQ) |

---

## Seguridad y roles

| Endpoint | GUEST | USER | ADMIN |
|---|---|---|---|
| GET ejercicios, rutinas, logros | ✅ | ✅ | ✅ |
| POST/PUT/DELETE ejercicios | ❌ | ❌ | ✅ |
| CRUD rutinas propias | ❌ | ✅ | ✅ |
| CRUD sesiones, mediciones, objetivos | ❌ | ✅ | ✅ |
| GET/PATCH propio usuario | ❌ | ✅ | ✅ |
| /admin/**, /usuarios (todos) | ❌ | ❌ | ✅ |
| PATCH /rutinas/{id}, DELETE /rutinas/{id} | ❌ | ✅ (propias) | ✅ |
| PUT /ejercicios/{id}/activar, PATCH /ejercicios/{id} | ❌ | ❌ | ✅ |

**JWT:** Header `Authorization: Bearer <token>`. Expiración: 7 días. `401` en token expirado/inválido, `403` en rol insuficiente.

**Usuarios pre-seeded** (creados por `DataInitializer`):
- `admin` / `Admin1234` → ROLE_ADMIN
- `guest` / `guest` → ROLE_GUEST

---

## Sistema de logros

`LogroService.evaluarLogros(usuarioId)` se llama automáticamente al crear/completar una sesión. Devuelve `List<String>` con los nombres de logros recién desbloqueados (vacía si ninguno).

La métrica y el umbral de cada logro viven en el enum `TipoLogro`, y es el único sitio: de ahí leen la concesión (`evaluarLogros`), el progreso (`GET /logros/progreso`) y el aviso de «logro próximo». Si cambia un umbral, cambia también la descripción del logro en la tabla `logros`, que repite el número en prosa.

| TipoLogro | Métrica | Umbral |
|---|---|---|
| `PRIMERA_SESION` | sesiones completadas | 1 |
| `CONSTANCIA` | sesiones completadas | 7 |
| `DEDICADO` | sesiones completadas | 30 |
| `CENTENARIO` | ejercicios realizados | 100 |
| `OBJETIVO_CUMPLIDO` | objetivos completados | 1 |
| `MAQUINA` | objetivos completados | 10 |

Los logros son permanentes: eliminar una sesión no revoca el logro obtenido.

---

## Base de datos

MariaDB en `localhost:3308`, BD `gymprofit_db`.

> **IMPORTANTE — Flyway:** Los archivos de migración en `db/migration/` no tienen prefijo `V` → Flyway **no los ejecuta automáticamente**. El esquema se creó manualmente. Para aplicar nuevas migraciones, ejecutar el SQL directamente en MariaDB.

**Nombrado de nuevas migraciones:** `YYYYMMDDHHmm__Descripcion_Snake.sql` (hora España/Madrid).

Migraciones aplicadas:
```
V202603022100__GymProFitDB_MigracionInicial.sql
V202603231235__Cambios_Entidades.sql
V202603251830__Fix_mejor_repeticiones_tipo.sql
V202604051853__Add_Auth_Roles.sql
V202605141912__Migrate_Objetivo_To_Enum.sql
V202605161102__Seed_Roles.sql
V202605161135__Logros.sql
V202605251000__Fix_altura_mediciones_corporales.sql   ← DECIMAL(3,2)→DECIMAL(5,2) en altura
V202605251830__Add_instrucciones_press_banca.sql      ← UPDATE ejercicios SET instrucciones WHERE id=1
V202605261000__Add_foto_perfil_usuarios.sql          ← columna foto_perfil VARCHAR(255) en usuarios (prefijo V → Flyway la ejecuta)
```

---

## Tests

**329 tests — BUILD SUCCESS**

```
controller/: AuthControllerTest, EjercicioControllerTest, RutinaControllerTest,
             SesionEntrenamientoControllerTest, ObjetivoPersonalControllerTest,
             LogroControllerTest, UsuarioControllerTest, AdminControllerTest
service/:    AuthServiceTest, EjercicioServiceTest, RutinaServiceTest,
             SesionEntrenamientoServiceTest, UsuarioServiceTest,
             LogroServiceTest, ObjetivoPersonalServiceTest
```

```bash
./mvnw test
```

---

## Enums

| Enum | Valores |
|---|---|
| `NivelExperiencia` | `PRINCIPIANTE`, `INTERMEDIO`, `AVANZADO`, `EXPERTO` |
| `GrupoMuscular` | `PECHO`, `ESPALDA`, `PIERNAS`, `HOMBROS`, `BRAZOS`, `ABDOMEN`, `CARDIO`, `FULLBODY` |
| `Dificultad` | `PRINCIPIANTE`, `INTERMEDIO`, `AVANZADO` |
| `TipoObjetivo` | `PERDER_PESO`, `GANAR_MASA_MUSCULAR`, `MANTENER_PESO`, `MEJORAR_RESISTENCIA`, `MEJORAR_FUERZA`, `REDUCIR_GRASA_CORPORAL`, `MEJORAR_FLEXIBILIDAD`, `MEJORAR_VELOCIDAD`, `AUMENTAR_CALORIAS`, `MEJORAR_MOVILIDAD`, `COMPLETAR_RETO`, `OTRO` |
| `TipoLogro` | `PRIMERA_SESION`, `CONSTANCIA`, `DEDICADO`, `CENTENARIO`, `OBJETIVO_CUMPLIDO`, `MAQUINA` |

---

## Changelog

### 2026-05-28
- **Fix SecurityConfig documentado**: `PUT /usuarios` solo accesible para ADMIN (catch-all `/usuarios/**`). `PATCH /usuarios/{id}` es el endpoint correcto para USER. Android actualizado: `OnboardingResumenActivity` y `EditarPerfilActivity` usan `PATCH /usuarios/{id}`

### 2026-05-26
- **Nutrición — AlimentoComidaDTO enriquecido**: añadidos `nombreAlimento`, `categoriaAlimento`, `proteinasTotales`, `carbohidratosTotales`, `grasasTotales` calculados en mapper (macro × cantidadGramos / 100). Fix `AlimentoComidaService`. Fix `activarAlimento()` (bug llamaba `deleteById`). `GET /admin/alimentos/busqueda?nombre=&categoria=&activo=` con filtros dinámicos jOOQ. Migración `V202605262000__Update_categorias_alimentos.sql`
- **Foto de perfil**: `POST /usuarios/{id}/foto` (multipart) + `GET /usuarios/{id}/foto` (bytes JPEG). Archivos en `./uploads/fotos-perfil/{id}.jpg`. Columna `foto_perfil VARCHAR(255)` en `usuarios` vía migración `V202605261000__Add_foto_perfil_usuarios.sql`. `FlywayConfig` añade `FlywayMigrationInitializer` para garantizar que Flyway corre antes que Hibernate

---

<div align="center">

Desarrollado por **Rubén Juan Candela** · CFGS 2º DAM · 2026

</div>
