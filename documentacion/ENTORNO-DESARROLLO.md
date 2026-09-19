# Entorno de desarrollo — GymProFit

> Guía para levantar **todo el proyecto en local**, sin depender del laboratorio AWS.
> A partir de la entrega del TFG, **el entorno por defecto es `dev` (local)**. AWS queda como opción histórica documentada al final.

> **Aislamiento de producción:** este documento explica cómo levantar cada pieza.
> Por qué un build de desarrollo **no puede** escribir en producción aunque se
> quiera (guarda en Gradle, guarda en arranque y distintivo en pantalla), más el
> usuario de pruebas y el reset de datos, está en
> [ENTORNO-PRUEBAS.md](ENTORNO-PRUEBAS.md).

---

## TL;DR — arrancar en local

1. **MariaDB** corriendo en `localhost:3308`, base de datos `gymprofit_db` (usuario `root`, pass `12345`).
2. **API**: arrancar `GymProFitApiApplication`. Perfil `dev` ya activo por defecto → escucha en `http://localhost:8080/api`. `application-dev.properties` necesita `app.seed.admin.password` (ver abajo) o no habrá usuario administrador.
3. **Android**: emulador. `BASE_URL=http://10.0.2.2:8080/api/` (ya configurado en `local.properties`). Run.
4. **(Opcional) Push FCM en dev**: exportar `FIREBASE_CREDENTIALS_PATH` con la ruta al JSON de la service-account key (raíz del repo, gitignoreado) antes de arrancar la API. Sin la variable, la API arranca igual con el push desactivado. Ver `documentacion/NOTIFICACIONES.md`.

Con eso funciona sin AWS.

---

## Mapa de entornos

| Componente        | dev (local, POR DEFECTO)                     | prod (AWS, histórico)                     |
|-------------------|----------------------------------------------|-------------------------------------------|
| Perfil Spring     | `dev` (`spring.profiles.active=dev`)         | `prod`                                    |
| Puerto API        | `8080` (default Spring)                       | `5000`                                    |
| BD                | `jdbc:mariadb://localhost:3308/gymprofit_db` | `jdbc:mariadb://3.228.158.124:3306/...`   |
| Android `BASE_URL`| `http://10.0.2.2:8080/api/`                  | `http://35.170.9.0/api/` (EC2)            |
| Fotos perfil      | `./uploads/fotos-perfil`                      | `/home/ubuntu/uploads/fotos-perfil`       |

`10.0.2.2` = alias que el **emulador Android** usa para llegar al `localhost` del PC anfitrión.

---

## API (Spring Boot)

- El perfil activo se fija en `src/main/resources/application.properties`:
  ```properties
  spring.profiles.active=dev
  ```
- `application-dev.properties` → BD local `localhost:3308`, logs DEBUG, `ddl-auto=validate`, y la contraseña del administrador semilla (ver abajo).

### Requisito: `app.seed.admin.password` en `application-dev.properties`
La contraseña del usuario `admin` ya **no está en el código**: `DataInitializer` la lee de esa
propiedad. Si no está definida, o viene vacía, el arranque deja este aviso en el log y
**no crea ningún usuario con rol ADMIN**, de modo que el panel de administración queda
inaccesible en desarrollo:

```
No se crea el usuario 'admin': la propiedad app.seed.admin.password está vacía.
```

Añade la línea a tu `application-dev.properties` (que está gitignoreado, por eso puede llevar el
valor literal):

```properties
app.seed.admin.password=Admin1234
```

En `prod` esa misma propiedad se mapea a la variable de entorno `ADMIN_PASSWORD` y **sin default**:
una contraseña de administrador en el repositorio es una cuenta de administración pública. La
cuenta `guest` no necesita configuración: se crea siempre, con una contraseña aleatoria que se
descarta, porque `POST /auth/guest` emite el token sin comprobar credenciales.

> Si ya tenías un `admin` en tu BD local, añadir la propiedad no le cambia la contraseña:
> `DataInitializer` no toca los usuarios que ya existen.

### El correo NO hace falta en dev ni en ci
En `prod` las dos variables de correo (`BREVO_API_KEY` y `MAIL_FROM`) son obligatorias y sin ellas
la API no arranca, pero en dev y ci se pueden dejar sin configurar: sin clave de API,
`EmailService` entrega el código de recuperación en un fichero de **`target/mail-outbox/`**
(configurable con `app.mail.outbox.dir`) y en el log solo deja la ruta. Así se puede probar el
flujo entero de `/auth/forgot-password` sin cuenta de correo.

El transporte es la **API HTTP de Brevo** (`POST https://api.brevo.com/v3/smtp/email`), no su
SMTP: Render bloquea los puertos 25, 465 y 587 en los servicios del plan gratuito, así que por
SMTP no salía nada desde producción. Si quieres probar envíos de verdad en local, añade a tu
`application-dev.properties`:

```properties
app.mail.brevo.api-key=xkeysib-<tu_clave>
app.mail.from=GymProFit <tu_remitente_verificado@dominio>
```

> **La clave de API NO es la clave SMTP.** Son credenciales distintas y se generan en pestañas
> distintas del panel: la de API está en *SMTP & API > **API Keys*** y empieza por `xkeysib-`; la
> de SMTP está en *SMTP & API > SMTP*. Poner la de SMTP aquí da 401 en cada envío.

Las variables `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME` y `MAIL_PASSWORD` **ya no las lee nadie**:
`spring-boot-starter-mail` salió del `pom.xml`.

El código **no se escribe en el log en ningún perfil**, ni completo ni a trozos: durante sus 15
minutos de validez equivale a la contraseña de la cuenta, y un log se agrega, se rota a servicios
de terceros y se conserva mucho más tiempo que el propio código. El fichero, en cambio, vive en el
directorio de build y desaparece con un `mvn clean`.
- `application-prod.properties` → BD AWS. **No hace falta tocarlo**; solo se usa si se arranca con `--spring.profiles.active=prod`.
- Migraciones Flyway en `src/main/resources/db/migration/` con formato `V<timestamp>__descripcion.sql` (SÍ se usan, versionadas). `ddl-auto=validate`: si el esquema no coincide con las entidades, la app **no arranca** → los cambios de esquema van SIEMPRE por una nueva migración Flyway, nunca a mano.

### Requisito: MariaDB local en 3308
El puerto es 3308 (no el 3306 por defecto). Asegúrate de que tu instancia local escucha ahí, o ajusta la URL en `application-dev.properties`.

### Compilar / regenerar jOOQ (sin secretos en el pom)
Desde la entrega, `pom.xml` está **versionado y sin credenciales AWS**. La conexión del codegen de jOOQ sale de propiedades Maven con **default local** (no secreto):
```
jooq.db.url=jdbc:mariadb://localhost:3308/gymprofit_db · jooq.db.user=root · jooq.db.password=12345
jooq.codegen.skip=true   (por defecto NO regenera; usa target/generated-sources/jooq)
```
- **Compilar** (normal, no regenera jOOQ): `mvn compile` (con JDK 17+). No requiere BD.
- **Regenerar clases jOOQ** contra la BD local: `mvn org.jooq:jooq-codegen-maven:generate -Djooq.codegen.skip=false` (MariaDB en 3308 encendida).
- **Regenerar contra AWS u otra BD** sin tocar el pom: añadir `-Djooq.db.url=... -Djooq.db.user=... -Djooq.db.password=...` (o definir esas props en un perfil de `~/.m2/settings.xml`).

> Nota: `application-dev.properties` / `application-prod.properties` siguen fuera de git (contienen `jwt.secret` y credenciales de BD).

---

## Android

- `BASE_URL` **no está hardcodeada**: se inyecta como `BuildConfig.BASE_URL` desde `local.properties` (fichero fuera de control de versiones):
  ```properties
  # app/GymProFit/local.properties
  BASE_URL=http://10.0.2.2:8080/api/
  ```
- Si `local.properties` no define `BASE_URL`, el `build.gradle` usa por defecto `http://10.0.2.2:8080/api/` (ver `app/build.gradle`).

### Emulador vs dispositivo físico
| Escenario                 | Valor de `BASE_URL`                         |
|---------------------------|---------------------------------------------|
| Emulador Android          | `http://10.0.2.2:8080/api/`                 |
| Móvil físico (misma WiFi) | `http://<IP-LAN-del-PC>:8080/api/` (ej. `http://192.168.1.50:8080/api/`) |

Para móvil físico: obtener la IP del PC (`ipconfig`), ponerla en `BASE_URL`, y permitir el puerto 8080 en el firewall de Windows. Ambos dispositivos en la misma red.

> Nota: la API sirve HTTP en claro. Android bloquea cleartext por defecto en release; en dev ya está permitido para estas IPs locales. Si añades una IP nueva y falla la conexión, revisar `network_security_config.xml`.

---

## Volver a AWS (si algún día se necesita)

1. API: arrancar con perfil prod → `java -jar ... --spring.profiles.active=prod` (o cambiar `spring.profiles.active=prod` en `application.properties`).
2. Android: en `local.properties` → `BASE_URL=http://35.170.9.0/api/` (o la IP/dominio del EC2 vigente).
3. Requiere el laboratorio AWS encendido (EC2 + MariaDB). Por eso, para trabajo diario, **usar dev**.

---

## Cambios aplicados al pasar a dev (2026-07-02)
- `app/GymProFit/local.properties`: `BASE_URL` de `http://35.170.9.0/api/` (EC2) → `http://10.0.2.2:8080/api/` (API local).
- API ya tenía `spring.profiles.active=dev` por defecto → sin cambios.
