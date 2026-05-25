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
| JUnit 5 + Mockito | — | 132 tests unitarios e integración |
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
$sql = Get-Content "src\main\resources\db\migration\202603022100__GymProFitDB_MigracionInicial.sql" -Raw
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

### AUTH — `/auth`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| POST | `/auth/login` | No | Login. Body: `{username, password}` → `TokenDTO` |
| POST | `/auth/register` | No | Registro. Body: `{username, password, email}` → 201 |
| POST | `/auth/guest` | No | Login como invitado → `TokenDTO` con ROLE_GUEST |

### USUARIOS — `/usuarios`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| GET | `/usuarios/{id}` | USER/ADMIN | Usuario por ID |
| GET | `/usuarios/username/{u}` | USER/ADMIN | Usuario por username |
| GET | `/usuarios/{id}/estadisticas` | USER/ADMIN | Estadísticas jOOQ |
| PUT | `/usuarios` | USER/ADMIN | Actualizar completo (id en body) |
| PATCH | `/usuarios/{id}` | USER/ADMIN | Actualización parcial |
| GET | `/usuarios` | ADMIN | Todos los usuarios |
| DELETE | `/usuarios/{id}` | ADMIN | Soft delete |
| DELETE | `/usuarios/{id}/permanente` | ADMIN | Eliminar permanentemente |

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
| PATCH | `/sesiones/{id}` | USER+ | Actualización parcial |
| DELETE | `/sesiones/{id}` | USER+ | Eliminar |

La respuesta de `POST /sesiones` incluye el campo `nuevosLogros: ["Nombre logro", ...]` (solo si se desbloquearon logros nuevos).

### LOGROS — `/logros`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| GET | `/logros` | GUEST+ | Todos los logros |
| GET | `/logros/usuario/{usuarioId}` | GUEST+ | Logros del usuario |
| POST | `/logros` | ADMIN | Crear logro |
| PUT | `/logros/{id}` | ADMIN | Actualizar logro |

### MEDICIONES — `/mediciones-corporales`

| Método | URL | Auth | Descripción |
|---|---|---|---|
| GET | `/mediciones-corporales/usuario/{id}/ordenadas` | USER+ | Historial del usuario |
| POST | `/mediciones-corporales` | USER+ | Registrar medición |
| DELETE | `/mediciones-corporales/{id}` | USER+ | Eliminar |

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
| GET | `/admin/usuarios` | ADMIN | Usuarios con filtros dinámicos (jOOQ) |
| GET | `/admin/estadisticas-globales` | ADMIN | Estadísticas globales (jOOQ) |

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

**JWT:** Header `Authorization: Bearer <token>`. Expiración: 7 días. `401` en token expirado/inválido, `403` en rol insuficiente.

**Usuarios pre-seeded** (creados por `DataInitializer`):
- `admin` / `Admin1234` → ROLE_ADMIN
- `guest` / `guest` → ROLE_GUEST

---

## Sistema de logros

`LogroService.evaluarLogros(usuarioId)` se llama automáticamente al crear/completar una sesión. Devuelve `List<String>` con los nombres de logros recién desbloqueados (vacía si ninguno).

| TipoLogro | Condición |
|---|---|
| `PRIMERA_SESION` | ≥ 1 sesión completada |
| `CONSTANCIA` | ≥ 7 sesiones completadas |
| `DEDICADO` | ≥ 30 sesiones completadas |
| `CENTENARIO` | ≥ 100 ejercicios realizados |
| `OBJETIVO_CUMPLIDO` | ≥ 1 objetivo personal completado |
| `MAQUINA` | ≥ 10 objetivos completados |

Los logros son permanentes: eliminar una sesión no revoca el logro obtenido.

---

## Base de datos

MariaDB en `localhost:3308`, BD `gymprofit_db`.

> **IMPORTANTE — Flyway:** Los archivos de migración en `db/migration/` no tienen prefijo `V` → Flyway **no los ejecuta automáticamente**. El esquema se creó manualmente. Para aplicar nuevas migraciones, ejecutar el SQL directamente en MariaDB.

**Nombrado de nuevas migraciones:** `YYYYMMDDHHmm__Descripcion_Snake.sql` (hora España/Madrid).

Migraciones aplicadas:
```
202603022100__GymProFitDB_MigracionInicial.sql
202603231235__Cambios_Entidades.sql
202603251830__Fix_mejor_repeticiones_tipo.sql
202604051853__Add_Auth_Roles.sql
202605141912__Migrate_Objetivo_To_Enum.sql
202605161102__Seed_Roles.sql
202605161135__Logros.sql
202605251000__Fix_altura_mediciones_corporales.sql   ← DECIMAL(3,2)→DECIMAL(5,2) en altura
```

---

## Tests

**132 tests — BUILD SUCCESS**

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
| `GrupoMuscular` | `PECHO`, `ESPALDA`, `PIERNAS`, `HOMBROS`, `BRAZOS`, `ABOMEN`, `CARDIO`, `FULLBODY` |
| `Dificultad` | `PRINCIPIANTE`, `INTERMEDIO`, `AVANZADO` |
| `TipoObjetivo` | `PERDER_PESO`, `GANAR_MASA_MUSCULAR`, `MANTENER_PESO`, `MEJORAR_RESISTENCIA`, `MEJORAR_FUERZA`, `REDUCIR_GRASA_CORPORAL`, `MEJORAR_FLEXIBILIDAD`, `MEJORAR_VELOCIDAD`, `AUMENTAR_CALORIAS`, `MEJORAR_MOVILIDAD`, `COMPLETAR_RETO`, `OTRO` |
| `TipoLogro` | `PRIMERA_SESION`, `CONSTANCIA`, `DEDICADO`, `CENTENARIO`, `OBJETIVO_CUMPLIDO`, `MAQUINA` |

---

<div align="center">

Desarrollado por **Rubén Juan Candela** · CFGS 2º DAM · 2026

</div>
