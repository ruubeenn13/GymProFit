# Despliegue en la nube — GymProFit

> **Contexto:** no se dispone de servidor propio con Docker, así que **se descarta Docker Compose** como método de despliegue de producción. En su lugar se usa una plataforma **PaaS** (la app se despliega desde el repositorio Git) + una **base de datos MySQL gestionada gratuita**. Investigación de opciones a fecha 2026-07.

## Resumen de la decisión

| Capa | Elección recomendada | Por qué |
|---|---|---|
| **Base de datos** | **Aiven for MySQL** (always-free) | Gestionada, 1 GB almacenamiento / 1 GB RAM, backups automáticos, **sin tarjeta** y **sin caducidad**. MySQL es compatible con el stack actual (driver MariaDB, Flyway, jOOQ). |
| **API (compute)** | **Koyeb** free *o* **Render** free | Despliegan Spring Boot desde Git sin servidor propio. |
| **CI** | **GitHub Actions** | Build + tests en cada push (no requiere infra). |
| **Health check** | **Spring Boot Actuator** | Endpoint `/actuator/health` para que la PaaS sepa si la app está viva. |

## Opciones de base de datos (el punto crítico)

El proyecto usa **MySQL/MariaDB**; las PaaS gratuitas (Render, Koyeb) **no ofrecen MySQL gestionado gratis** (Render solo Postgres, Koyeb solo compute), así que la BD va aparte.

- **Aiven for MySQL — always-free** *(recomendada)*: 1 GB RAM / 1 CPU / 1 GB almacenamiento, backups, sin tarjeta, sin límite de tiempo. Suficiente para el volumen del TFG.
- **TiDB Cloud Serverless**: compatible con el protocolo MySQL, free tier más generoso (varios GB). Buena si se necesita más espacio; revisar diferencias de dialecto.
- **Railway MySQL**: existe, pero el plan gratis da ~$1/mes de crédito → **no aguanta 24/7**. Descartada para producción continua.
- **PlanetScale**: eliminó su hobby/free tier (2024) → descartada.

## Opciones de compute (API)

- **Koyeb** free: 1 servicio web, 512 MB RAM, 0.1 vCPU, 2 GB SSD. **Scale-to-zero tras 1 h** sin tráfico (cold start al despertar). Despliega desde Git o Dockerfile/buildpack Java.
- **Render** free: despliega web service desde Git sin tarjeta; **spin-down** tras inactividad (~1 min para volver). Su BD gratis es Postgres (no sirve aquí → usar Aiven).
- **fly.io**: **ya NO tiene free tier para cuentas nuevas** (pay-as-you-go, tarjeta obligatoria). Solo cuentas antiguas conservan el legacy. Descartada como opción gratuita.

> Limitación común del compute gratis: **cold start** (~1 min) tras inactividad. Aceptable para un TFG/demo; para producción real se pasaría a un plan de pago o VM.

## Estado 2026-07-03 — DESPLEGADO Y LIVE ✅

API pública: **https://gymprofit-api.onrender.com** (Render Web Service, Docker, Frankfurt, Free) → Aiven MySQL (TLS verify-full). Verificado en producción: `/api/actuator/health` 200 UP, register 201 + login 200 con JWT. Auto-deploy desde `main`. Swagger OFF en prod (a propósito; verlo en local en `http://localhost:8080/api/swagger-ui.html`). Free duerme a ~15 min → cold start ~30-40s la 1ª request. Pendiente: `BASE_URL` de Android a la URL de Render para builds de release.

## Variables de entorno en Render

Estado a 2026-09-19. En el perfil `prod` **ninguna de estas ocho tiene valor por defecto**: el
placeholder de `application-prod.properties` va pelado (`${VARIABLE}`) a propósito, así que si
falta cualquiera de ellas el contexto de Spring no levanta y **el servicio no arranca**. Es
deliberado: una configuración a medias que arranca es peor que un arranque que falla, porque el
fallo se descubre en producción y en silencio.

| Variable | Grupo | Si falta |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Datasource | **No arranca** |
| `SPRING_DATASOURCE_USERNAME` | Datasource | **No arranca** |
| `SPRING_DATASOURCE_PASSWORD` | Datasource | **No arranca** |
| `JWT_SECRET` | Firma de tokens | **No arranca** |
| `MAIL_HOST` | Correo (Brevo SMTP) | **No arranca** |
| `MAIL_USERNAME` | Correo (Brevo SMTP) | **No arranca** |
| `MAIL_PASSWORD` | Correo (Brevo SMTP) | **No arranca** |
| `MAIL_FROM` | Correo (Brevo SMTP) | **No arranca** |
| `ADMIN_PASSWORD` | Semilla de administración | Arranca, pero **sin crear cuenta de administración** |

Por grupos:

- **Datasource y `JWT_SECRET`**: sin ellas no hay ni base de datos ni forma de firmar tokens.
- **Correo**: las cuatro son obligatorias desde que la recuperación de contraseña existe. Antes
  llevaban default vacío y la consecuencia era peor que no arrancar: Spring no creaba
  `JavaMailSender`, el código de recuperación acababa escrito en el log y
  `POST /auth/forgot-password` respondía 200 sin haber enviado nada. La única vía de recuperar
  una cuenta fallaba de forma indistinguible del éxito.
- **`ADMIN_PASSWORD`**: es la excepción, la única opcional. Sin ella la API funciona con
  normalidad pero `DataInitializer` **no crea ninguna cuenta con rol ADMIN** y lo deja anotado en
  el log de arranque. Es intencionado: una API sin panel de administración sigue sirviendo a sus
  usuarios, mientras que un ADMIN con contraseña conocida es acceso total al sistema. No tiene
  valor por defecto porque una contraseña de administrador en el repositorio es una cuenta de
  administración pública.

> **`MAIL_FROM` no basta con que esté definida.** El formato es `Nombre <direccion@dominio>`, y la
> dirección **tiene que estar verificada en el proveedor SMTP** (en Brevo: *Senders*) o pertenecer
> a un dominio autenticado. Con cualquier otra, Brevo rechaza el envío aunque el resto de la
> configuración sea correcta; el `catch` de `EmailService` no propaga el fallo —para no delatar qué
> cuentas existen— así que el endpoint seguiría respondiendo 200 y el correo no saldría.

---

## Preparación de código (detalle)

Compute elegido: **Render** (más simple para empezar; auto-deploy desde Git, soporta monorepo con Root Directory y Docker). BD: **Aiven for MySQL** (ya con cuenta).

Artefactos añadidos al repo (verificados en local: 136 tests verdes + `/api/actuator/health` → `200 {"status":"UP"}`):
- **Actuator**: `spring-boot-starter-actuator`; en `application.properties` solo `health` expuesto, `show-details=never`; `/actuator/health` público en `SecurityConfig`.
- **`application-prod.properties` versionado y 12-factor**: todos los secretos por variables de entorno (cero secretos en git); `server.port=${PORT:8080}`; dialecto Hibernate autodetectado (MySQL de Aiven); Swagger off. Se quitó del `.gitignore` (dev sigue ignorado). Ver abajo la lista completa de variables.
- **`Dockerfile`** multi-stage (JDK21 build → JRE21 run), `LOG_DIR=/tmp/logs`, `SPRING_PROFILES_ACTIVE=prod`, arranca por `$PORT`. `.dockerignore` incluido.
- **`render.yaml`** (Blueprint en raíz): servicio web docker, `rootDir: api/gymprofit-api`, `healthCheckPath: /api/actuator/health`, env vars de secretos marcadas `sync:false`.

Pendiente (clics de cuenta, con guía): crear MySQL en Aiven → copiar credenciales → conectar repo en Render (Blueprint) → pegar env vars → verificar health público → apuntar Android `BASE_URL`.

## Actualización 2026-07-06 — Firebase push + memoria JVM

- **Env var nueva en Render: `FIREBASE_CREDENTIALS_JSON`** = contenido completo del JSON de la service-account key de Firebase (Consola → Cuentas de servicio → Generar clave privada). Es un SECRETO: nunca en git (gitignoreado `*firebase-adminsdk*.json`). Sin ella la API arranca igual con el push desactivado (`FirebaseConfig` es graceful — clave para CI). En local se usa `FIREBASE_CREDENTIALS_PATH` (ruta al fichero). Verificación: el log de arranque debe decir `Firebase Admin SDK inicializado: notificaciones push ACTIVADAS`.
- **Heap JVM**: el `Dockerfile` arranca con `-XX:MaxRAMPercentage=60.0`. En la instancia free de Render (512 MB) el default de la JVM (~25% ≈ 128 MB) se quedó corto al añadir `firebase-admin` (grpc/netty): el primer deploy agotó el timeout del health check ("no open ports detected") con arranques de ~180 s. Con el 60% (~300 MB) el arranque tiene margen.
- **Zona horaria**: los recordatorios push usan `zone="Europe/Madrid"` en los `@Scheduled` (el contenedor corre en UTC). Ver `documentacion/NOTIFICACIONES.md`.

## Pasos de migración pendientes (cuando se despliegue)

1. **BD**: crear instancia Aiven for MySQL; volcar el esquema con Flyway (las migraciones `V*.sql` corren solas al arrancar).
2. **Compatibilidad MySQL vs MariaDB**: el driver `org.mariadb.jdbc` conecta contra MySQL, pero conviene:
   - Revisar el **dialecto de jOOQ** (codegen: `MariaDBDatabase` → `MySQLDatabase` si la BD destino es MySQL).
   - Revisar SQL específico de MariaDB en las migraciones (tipos, `BOOLEAN`/`TINYINT`, etc.).
3. **Secretos por entorno**: todos los secretos y el `BASE_URL` de Android como **variables de entorno** de la PaaS, nunca con valor literal en `application-prod.properties`. La lista vigente está en la sección "Variables de entorno en Render" de este documento.
4. **Actuator**: añadir `spring-boot-starter-actuator` y exponer `/actuator/health` (la PaaS lo usa como health check).
5. **CI**: workflow de GitHub Actions que compile y pase los tests; opcionalmente, deploy automático al hacer merge a `main`.
6. **Android**: apuntar `BASE_URL` (en `local.properties` / `BuildConfig`) a la URL pública de la API con **HTTPS**.

## Fuentes (2026-07)

- [Aiven — Always-Free MySQL](https://aiven.io/free-mysql-database)
- [Aiven for MySQL free tier (docs)](https://aiven.io/docs/products/mysql/concepts/mysql-free-tier)
- [Render — Platforms with a real free tier (2026)](https://render.com/articles/platforms-with-a-real-free-tier-for-developers-in-2026)
- [Koyeb free tier 2026](https://www.srvrlss.io/provider/koyeb/)
- [Railway pricing 2026](https://www.srvrlss.io/provider/railway/)
- [Deploy Spring Boot en Railway (guía)](https://docs.railway.com/guides/spring-boot)
- [Fly.io — Use a MySQL Database](https://fly.io/docs/app-guides/mysql-on-fly/)
