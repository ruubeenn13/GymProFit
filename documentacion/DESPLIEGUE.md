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

## Preparación de código (detalle)

Compute elegido: **Render** (más simple para empezar; auto-deploy desde Git, soporta monorepo con Root Directory y Docker). BD: **Aiven for MySQL** (ya con cuenta).

Artefactos añadidos al repo (verificados en local: 136 tests verdes + `/api/actuator/health` → `200 {"status":"UP"}`):
- **Actuator**: `spring-boot-starter-actuator`; en `application.properties` solo `health` expuesto, `show-details=never`; `/actuator/health` público en `SecurityConfig`.
- **`application-prod.properties` versionado y 12-factor**: datasource + `JWT_SECRET` por variables de entorno (cero secretos en git); `server.port=${PORT:8080}`; dialecto Hibernate autodetectado (MySQL de Aiven); Swagger off. Se quitó del `.gitignore` (dev sigue ignorado).
- **`Dockerfile`** multi-stage (JDK21 build → JRE21 run), `LOG_DIR=/tmp/logs`, `SPRING_PROFILES_ACTIVE=prod`, arranca por `$PORT`. `.dockerignore` incluido.
- **`render.yaml`** (Blueprint en raíz): servicio web docker, `rootDir: api/gymprofit-api`, `healthCheckPath: /api/actuator/health`, env vars de secretos marcadas `sync:false`.

Pendiente (clics de cuenta, con guía): crear MySQL en Aiven → copiar credenciales → conectar repo en Render (Blueprint) → pegar env vars → verificar health público → apuntar Android `BASE_URL`.

## Pasos de migración pendientes (cuando se despliegue)

1. **BD**: crear instancia Aiven for MySQL; volcar el esquema con Flyway (las migraciones `V*.sql` corren solas al arrancar).
2. **Compatibilidad MySQL vs MariaDB**: el driver `org.mariadb.jdbc` conecta contra MySQL, pero conviene:
   - Revisar el **dialecto de jOOQ** (codegen: `MariaDBDatabase` → `MySQLDatabase` si la BD destino es MySQL).
   - Revisar SQL específico de MariaDB en las migraciones (tipos, `BOOLEAN`/`TINYINT`, etc.).
3. **Secretos por entorno**: `jwt.secret`, credenciales de BD y `BASE_URL` de Android como **variables de entorno** de la PaaS (no en `application-prod.properties`). Ver también el punto de "secreto JWT distinto por entorno" de la auditoría.
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
