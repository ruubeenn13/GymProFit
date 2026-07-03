# Auditoría técnica GymProFit — 2026-07-02

Revisión exhaustiva del proyecto completo (API Spring Boot + app Android + build/infra) tras la entrega del TFG, con vistas a un lanzamiento real. Realizada sobre el código actual. Cada hallazgo lleva `fichero:línea`.

**Leyenda severidad:** 🔴 CRÍTICO · 🟠 ALTO · 🟡 MEDIO · 🟢 BAJO

---

## 0. Resumen ejecutivo

**Lo que ya está bien** (no tocar): `@ControllerAdvice` global completo, DTOs en todas las capas (ninguna entidad expuesta), MapStruct, BCrypt, JWT stateless, Flyway versionado y consistente, secretos reales fuera de git, 16 tests backend, jOOQ solo para queries complejas, adapters Android con ViewHolder correcto, red Android con timeouts y manejo de 401 centralizado.

**Lo que bloquea un lanzamiento** (por orden de gravedad):
1. 🔴 **Escalada a ADMIN sin autenticar** vía `/auth/register` (C1).
2. 🔴 **IDOR sistémico**: 40+ endpoints permiten a un USER leer/editar/borrar datos de CUALQUIER otro usuario (C2).
3. 🟠 Fugas de detalle interno (SQL/stacktrace) en respuestas de error.
4. 🟠 Android: token JWT sin cifrar + backup a la nube + HTTP en claro.
5. 🟠 Deuda técnica: cero paginación, relaciones EAGER, secreto JWT compartido dev/prod.

---

## 1. Seguridad — Backend

### 🔴 CRÍTICO

**C1 — Escalada de privilegios a ADMIN sin autenticación**
`AuthController.java:52-61` · `AuthService.java:80-84` · `RegisterDTO.java:42` · `IRoleRepository.java:20-21`
`/auth/register` es `permitAll` y `RegisterDTO` acepta `List<Integer> roles` del cliente. `register()` los usa directamente. Un atacante anónimo hace `POST /api/auth/register` con `"roles":[1]` y obtiene una cuenta ADMIN (RoleType.ADMIN = 1).
**Fix:** eliminar `roles` de `RegisterDTO`; forzar siempre `List.of(USER.getValue())` en el service, ignorando cualquier rol del cliente.

**C2 — IDOR sistémico (Broken Object Level Authorization)**
Causa raíz: **ningún controller consulta el principal autenticado; el `userId` sale SIEMPRE del cliente** (path `{id}`/`{usuarioId}`/`{comidaId}`/`{sesionId}` o campo del DTO). `SecurityConfig` filtra solo por rol, nunca por propiedad. Único service que valida propiedad: `RutinaService` (y solo en escrituras).
40+ endpoints afectados en 8 dominios. Un USER autenticado, cambiando el id, puede:
- **usuarios**: `UsuarioController.java:263 patchUsuario` → editar email/peso/altura/`activo` de otro (desactivar cuentas, secuestrar email); `:63 obtenerUsuario` leer cualquier perfil; `:274 subirFotoPerfil` sobrescribir foto ajena.
- **sesiones** (`SecurityConfig.java:119`): `SesionEntrenamientoController.java` `:45 findAll`, `:59 GET{id}`, `:87 PUT`, `:373 PATCH`, `:100 DELETE`, `:125 completar`, `:73 POST` (crea para otro), `:141..:355` historial de cualquiera.
- **mediciones-corporales** (`:121`): `MedicionCorporalController.java` `:45/:59/:87/:199/:100/:75/:125..:181` — peso/medidas de cualquiera.
- **objetivos-personales** (`:122`): `ObjetivoPersonalController.java` `:46/:60/:89/:293/:102/:231/:75/:127..:181`.
- **comidas** (`:125`): `ComidaController.java` `:48/:62/:92/:302/:107/:76/:132..:219`.
- **alimentos-comida** (`:126`): `AlimentoComidaController.java` `:43/:57/:85/:273/:98/:71/:123/:173/:197` — leer/vaciar la dieta de comidas ajenas.
- **progreso-ejercicios** (`:123`): `ProgresoEjercicioController.java` `:44/:58/:88/:329/:100/:74/:281/:304` — incl. borrado masivo ajeno.
- **notificaciones** (`:124`): `NotificacionController.java` `:45/:59/:88/:209/:317/:75/:220/:244` — spoofing y borrado masivo.
- **ejercicios-realizados** (`:120`): `EjercicioRealizadoController.java` `:44/:58/:86/:288/:99/:72/:235/:261`.
- **rutinas-ejercicios** (`:127`): `RutinaEjercicioController.java` `:58/:88/:294/:101/:74/:244/:268` — **bypass del guard de `RutinaService`**, permite editar rutinas predefinidas.
- **rutinas (lecturas)**: `RutinaController.java:59/:175/:273` — leer rutina privada ajena (las escrituras SÍ validan).

**Fix general:** crear helper de propiedad (espejo de `RutinaService.getCurrentUser()`/`checkOwnership()` en `RutinaService.java:32-34,248-258`). En escrituras por id: cargar entidad y exigir `entidad.getUsuario().getId().equals(principal.getId()) || isAdmin`. En creates: derivar `usuarioId` del token, nunca del body. Convertir los `findAll` de datos personales en solo-ADMIN.

### 🟠 ALTO
(Todo el bloque IDOR listado arriba es severidad ALTO salvo C1/C2 raíz.)

### 🟡 MEDIO
- **CORS wildcard** `SecurityConfig.java:67` `setAllowedOrigins("*")` en `/**` + `@CrossOrigin("*")` en los 16 controllers → lista blanca de orígenes; quitar anotaciones redundantes.
- **Secreto JWT compartido dev/prod** `application-dev.properties:33` = `application-prod.properties:29`; su Base64 decodifica a frase predecible → secreto aleatorio distinto por entorno, por variable de entorno.
- **JWT 7 días sin refresh ni revocación** `JwtTokenProvider.java:34` + `jwt.expiration=604800000` → access corto (15-60 min) + refresh token.
- **Fuga de info en errores** `ControllerExceptionHandler.java:50,63,76,152,165,198` devuelven `ex.getCause().getMessage()` (JDBC/Hibernate/SQL) al cliente → loguear la causa, no devolverla.
- **Enumeración de usuarios** `AuthService.java:72-77` mensajes distintos username vs email → mensaje genérico único.
- **Login con credenciales malas devuelve 500 en vez de 401** `AuthService.login:47` lanza `BadCredentialsException` sin handler → cae en el `Exception` genérico. `InvalidCredentialsException` (handler 401) está definida pero **nunca se lanza** (código muerto) → añadir `@ExceptionHandler(AuthenticationException.class)` → 401.

### 🟢 BAJO
- Swagger `permitAll` `SecurityConfig.java:91` expone el mapa de la API a anónimos → restringir/deshabilitar en prod.
- `SecurityConfig.java:112` regla `PUT /usuarios/{id}` inerte (no existe handler) → config muerta.
- `application.properties:2` default `dev` → si un deploy no fuerza `prod`, arranca con config de desarrollo.
- jOOQ sin inyección SQL (bind params correctos). Punto frágil menor: `.valueOf(x.toUpperCase())` en `UsuarioJooqRepository.java:89,133` y `AuthService.java:92` → `IllegalArgumentException` → 500.

---

## 2. Calidad de código — Backend

### 🟠 ALTO
- **Relaciones JPA EAGER en todas las entidades** → N+1 / cargas masivas: `Comida.java:47`, `Notificacion.java:43`, `MedicionCorporal.java:57`, `AlimentoComida.java:29,33`, `ProgresoEjercicio.java:39,43`, `ObjetivoPersonal.java:54`, `Rutina.java:52`, `RutinaEjercicio.java:41,45`, `SesionEntrenamiento.java:45`, `EjercicioRealizado.java:42`, `UsuarioLogro.java:27`, `Usuario.java:66`. **Fix:** `LAZY` + `@EntityGraph`/`JOIN FETCH` donde haga falta.
- **Cero paginación**: todos los repos extienden `CrudRepository` (no `JpaRepository`). Todos los `findAll`/`findByX` devuelven `List` completa (14 controllers). **Fix:** `JpaRepository<E,Integer>` + `Pageable`.
- **Manejo de errores incoherente**: los DELETE/activar/permanente usan `try/catch(Exception)+Map` que traga la excepción y devuelve 500, saltándose el `@ControllerAdvice` (un `NotFound` en delete devuelve 500 en vez de 404). Afecta a los 14 controllers de entidad (`EjercicioController:100,119,143`, `AlimentoController:101,125,149`, `UsuarioController:104,128,152`, etc.). **Fix:** quitar try/catch, dejar fluir las `*EntityException`.

### 🟡 MEDIO
- Respuestas `Map<String,Object>` crudas en endpoints `count/*`/`exists/*`/mensaje (sin contrato Swagger) → DTOs `CountDTO`/`ExistsDTO`/`MensajeDTO`.
- `@Transactional` de `jakarta` en `UsuarioService.java:18` (el resto usa el de Spring) → unificar en Spring.
- Cast `(List<X>) repo.findAll()` en 15 services → desaparece con `JpaRepository`.
- Mapeo manual campo a campo en `modify()`/`patch()` de casi todos los services (incoherente: `UsuarioService.modify:160` sí usa mapper) → MapStruct `@MappingTarget` + `nullValuePropertyMappingStrategy=IGNORE`.
- Validación/lógica en controllers: `ComidaController:307`, `ObjetivoPersonalController:205-211`, `AlimentoController:188-192` (catálogo hardcodeado) → mover a service/enum.
- Salto de capa: `AdminController:34-36,99,115,130` llama repos jOOQ directo → pasar por service.
- `@Transactional` faltante en varias escrituras (`modify`/`save`) → añadir a toda mutación; `readOnly=true` en lecturas.

### 🟢 BAJO (bugs reales)
- `NotificacionController:130-139` `/ordenadas` llama al mismo método que el no ordenado → **no ordena**.
- `AlimentoComidaController:245` `respuesta.put("comidaId", count)` → mete el count en la clave equivocada.
- `MedicionCorporalController:109` typo `"mensjae"`.
- `ObjetivoPersonalController:208` string con `\"` literales.
- **`UsuarioService.patch:228-230`** asigna `peso`/`altura`/`edad` **sin comprobar null** → un PATCH parcial que los omita los pone a null (destruye datos). **Bug a corregir ya.**
- Swagger incorrecto: `ComidaController:42` (schema AlimentoDTO), `SesionEntrenamientoController:229`.
- Ruta inconsistente `ProgresoEjercicioController:197` (`/progreso-ejercicio` singular).
- Sin Javadoc en services/mappers (lo exige el propio CLAUDE.md).

---

## 3. Android (`es.pmdm.gymprofit`)

### 🟠 ALTO
- **HTTP en claro global** `AndroidManifest.xml:14` `usesCleartextTraffic="true"` → el token Bearer viaja sin cifrar. Restringir a `network-security-config` con dominio dev; HTTPS en el resto.
- **Backup del token a la nube** `AndroidManifest.xml:13` `allowBackup="true"` con reglas vacías → JWT y sesión en SharedPreferences se copian vía adb/nube en claro. Excluir `GymProFitPrefs` del backup o `allowBackup="false"`.
- **Token sin cifrar** `PreferencesManager.java:47` → `EncryptedSharedPreferences` (androidx.security-crypto).
- **AsyncTask con leak de Activity** `PerfilActivity.java:239-268` (descarga avatar) toca la view tras destruir la Activity → migrar a OkHttp/Glide con check `isDestroyed()`.

### 🟡 MEDIO
- **AsyncTask deprecado (API 30)**: solo 3 instancias (`UtilREST.java:46,130` + `PerfilActivity:239`) pero canalizan toda la red. Migrar a **Retrofit + OkHttp + Gson** elimina `UtilREST` entero (~218 líneas) y ~90% de `UtilJSONParser` (~375 líneas): ~600 líneas netas, más el hack de reflexión-PATCH (`UtilREST.java:68-71`), el parseo en UI thread, y la duplicación de token. Alto retorno.
- Parseo JSON en UI thread (`UtilREST` onPostExecute → onSuccess).
- Fuga en logcat `UtilREST.java:90,181` loguea URLs de admin con username/filtros → envolver en `BuildConfig.DEBUG`.
- Permiso peligroso `READ_CONTACTS` (`AndroidManifest.xml:10` / `AcercaDeActivity:67-86`) innecesario → usar la URI del picker.
- Bug latente `API.java:54-56` `actualizarUsuario(id,...)` ignora el `id` (PUT a `usuarios` sin id).
- Duplicación del patrón request+parse+runOnUiThread en ~20 Activities → con Retrofit, Callback genérico + `Resource<T>`.

### 🟢 BAJO
- `runOnUiThread` redundante en cada callback (onPostExecute ya es UI thread).
- `catch(Exception ignored){}` en `PerfilActivity:166,290`, `UtilJSONParser:372` → loguear en debug.
- Token como `static` mutable en `UtilREST:19-35` → interceptor que lo lea de `PreferencesManager`.
- Sin ViewBinding (`build.gradle:64-65`) → findViewById repetido.

---

## 4. Build / Config / Infraestructura / Git

### 🟠 ALTO
- **`db/.idea/dataSources.xml:18` TRACKEADO en git** expone IP+puerto de prod AWS → `git rm --cached db/.idea/dataSources.xml` + ignorar `db/.idea/`.
- **Credenciales AWS en claro en `pom.xml:187-189`** (bloque jOOQ codegen: IP, user, pass) → externalizar a `${db.url}/${db.user}/${db.pass}` desde `settings.xml`/env.
- **Mismo `jwt.secret` dev y prod** (ver seguridad MEDIO) → distinto por entorno vía env.

### 🟡 MEDIO
- **`pom.xml` entero fuera de git** (efecto colateral de meter secretos) → CI imposible; un clone no compila. Sacar secretos y volver a versionar.
- Falta `spring-boot-starter-actuator` (sin health-check para AWS).
- Sin Testcontainers / tests de integración con BD real.
- **Sin tests de nutrición** (Alimento/AlimentoComida), MedicionCorporal ni Comida.
- `app/build.gradle:43` `minifyEnabled false` en release → `true` + `shrinkResources true`.
- Sin `.github/workflows`, `Dockerfile` ni `docker-compose`.
- **Incoherencia Java** `pom.xml:139-140` compila a 17 pero declara `java.version=21` → unificar.

### 🟢 BAJO
- `spring-boot-starter-validation` con versión fija `3.5.6` ≠ parent `3.5.7` → quitar `<version>`.
- `.idea` trackeados pese al ignore (añadidos antes) → `git rm -r --cached app/GymProFit/.idea db/.idea`.
- `uploads/fotos-perfil/4.jpg` TRACKEADO (foto de runtime, privacidad) → `git rm --cached` + ignorar `uploads/`.
- `libs.versions.toml:10-11` retrofit/okhttp declarados pero no usados → limpiar (o aprovechar en la migración Android).

---

## 5. Roadmap de remediación (orden recomendado)

**Fase 1 — Seguridad bloqueante (antes de exponer nada):**
1. ✅ C1: quitar `roles` de `RegisterDTO`, forzar USER.
2. ✅ C2: helper de ownership (`SecurityUtils`) + derivar userId del JWT en los 12 services; `findAll` → solo ADMIN.
3. ✅ Dejar de devolver `ex.getCause()` en errores; handler 401 (`AuthenticationException`) para credenciales.
4. ✅ `git rm --cached` de `uploads/` + `.idea` + `dataSources.xml`; secretos AWS fuera del pom (credenciales del codegen jOOQ → propiedades Maven `${jooq.db.*}` con default local + override `-D`/settings.xml); `pom.xml` re-versionado y `pom.example.xml` eliminado (redundante).

> **Estado 2026-07-02:** **Fase 1 COMPLETA** (ítems 1-4 + bug `UsuarioService.patch`). Compila con JDK 21. Fases 2-4 sin empezar.

**Fase 2 — Robustez backend:** ✅ COMPLETA
5. ✅ Bug `UsuarioService.patch` (null-check peso/altura/edad).
6. ◑ Migrar repos a `JpaRepository` (hecho, quita casts) + `Pageable` (**NO expuesto en endpoints**: rompería Android; pendiente como paso aditivo si se quiere paginación).
7. ✅ Relaciones `LAZY` + `@Transactional(readOnly=true)` a nivel de service (roles de Usuario se quedan EAGER por Spring Security).
8. ✅ Quitar try/catch+Map de controllers (delegan en `@ControllerAdvice`).
9. ✅ Secreto JWT por entorno + **refresh token** (opaco, persistido, rotado/revocable; API+Android verificado end-to-end).
   + Extras Fase 2: warnings MapStruct silenciados, bugs 🟢, CORS whitelist, Java 21, deps vulnerables subidas + OWASP + Dependabot (auto-merge).

**Fase 3 — Android:**
10. ✅ Cifrar token (`EncryptedSharedPreferences`) + quitar cleartext (`network-security-config`) + excluir el almacén del backup + fix leak `PerfilActivity`.
11. ◑ Migrar red a Retrofit+OkHttp+Gson: **ETAPA 1 (motor) HECHA** — `ApiClient` (Retrofit+OkHttp) + `Authenticator` de refresh; `UtilREST` delega manteniendo la fachada; fuera AsyncTask. **PENDIENTE etapa 2 (tipada):** Gson + modelos, eliminar `UtilJSONParser`, Activities a `Callback<T>`.

**Fase 4 — Lanzamiento:** ⬜ SIN EMPEZAR
12. ~~Docker Compose~~ **descartado.** Despliegue **PaaS** (Koyeb/Render free) + **Aiven for MySQL** (gratis) + **CI (GitHub Actions build+test)** + **Actuator**. Ver `documentacion/DESPLIEGUE.md`.
13. Tests de nutrición/mediciones. `minifyEnabled true`.
14. HTTPS + dominio (URL pública de la PaaS).

---

## 6. Estado fin de jornada 2026-07-02 · PENDIENTE para mañana

**Hecho hoy:** Fase 1 ✅ · Fase 2 ✅ · Fase 3 (seguridad ✅ + Retrofit motor ✅) · sueltos MEDIO ✅ · deps/Dependabot/OWASP ✅. Todo en `origin/main`, 136 tests verdes, verificado en emulador.

**Pendiente (orden sugerido para mañana):**
1. **CI — GitHub Actions** (`.github/workflows/ci.yml`): ✅ CREADO. Build + tests (`mvnw verify`, JDK 21) en cada push a `main`/PR, con MariaDB 11 efímera (service container) y datasource sobrescrito por env (perfil `dev`, jwt.secret literal intacto). Job `api-build-test`. `mvnw` invocado con `sh` (el fichero está en modo 644, no ejecutable). **PENDIENTE (manual en web, una vez):** Ruleset en `main` → *Require a pull request* (0 approvals) + *Require status checks* = `api-build-test` + añadir el usuario a **bypass list** (para seguir pusheando directo). Sin eso el auto-merge de Dependabot no queda gateado.
2. **Retrofit etapa 2 (tipada)** — Gson + modelos, eliminar `UtilJSONParser`, migrar las 31 Activities a `Callback<T>`. Bloque grande y arriesgado; verificar pantalla a pantalla en emulador.
3. **Fase 4 resto:** Actuator (`/actuator/health`) + tests de nutrición/mediciones + `minifyEnabled true` + despliegue PaaS (Aiven MySQL + Koyeb/Render) + HTTPS.

**Sueltos NO abordados (decisión tomada):** enumeración en `register` (se deja por UX) y MapStruct `@MappingTarget` en modify/patch (skip: riesgo>valor).

**Recordatorios de entorno:** compilar API con `JAVA_HOME=C:\Users\ruben\.jdks\ms-21.0.10` y `./mvnw -o compile/test` (offline OK; OWASP en perfil `security-scan`). BD local MariaDB en `localhost:3308`. Android: `./gradlew :app:assembleDebug`, emulador AVD `Medium_Phone_API_36.1`, `BASE_URL=10.0.2.2:8080`. En dev el `jwt.secret` debe ser LITERAL (no `${JWT_SECRET:...}`).
