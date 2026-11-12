# Prueba de carga de la API (GP-221)

Prueba reproducible con [k6](https://k6.io) del recorrido de un usuario típico, contra la API **en local** con el perfil de producción y los límites de Render free. **Nunca contra producción**: `recorrido.js` se niega si `BASE_URL` no es local y `correr.sh` si la base no lo es.

## Qué hace cada usuario virtual

En bucle, con 1 a 3 s de pausa entre pantallas, las mismas llamadas que la app:

1. **Entra**: `POST /auth/login` y `GET /usuarios/username/{u}`.
2. **Inicio**: las diez llamadas de `InicioFragment.cargarTodo()` (rutinas activas, sesiones, programa seguido, volumen muscular y semanal, historial, récords y un `GET /ejercicios/{id}` por récord, comidas de hoy, `/diario`).
3. **Guarda una sesión**: `POST /sesiones/completa` con un ejercicio de tres series y el resumen que abre la app después (`GET /sesiones/{id}` y `/sesiones/{id}/resumen`).
4. **Diario de hoy**: comidas del día y `/diario`.
5. **Busca un alimento**: `GET /alimentos/buscar?q=…`.

Cada cuenta va con su propia IP en `X-Forwarded-For`, como llegan los usuarios reales tras el proxy de Render: el límite de peticiones es por IP.

## Cómo se corre

Requisitos: Docker y la MariaDB local de desarrollo (`localhost:3308`, `gymprofit_db`).

```sh
# 1. La imagen de la API, con el mismo Dockerfile que Render
docker build -t gymprofit-api:carga api/gymprofit-api

# 2. Una pasada por nivel de usuarios simultáneos (3 min cada uno)
DB_PASSWORD='<la de la base local>' sh scripts/carga/correr.sh 5 10 20 40
```

`correr.sh` levanta el contenedor con **512 MB y 0,1 de CPU** (Render free) y el `ENTRYPOINT` del Dockerfile, así que la JVM lleva los mismos topes que en producción (heap de 160 MB, etc.) y Hikari y Tomcat los de `application-prod.properties` (5 conexiones, 20 hilos). Los secretos (JWT, importación) se generan al vuelo y solo valen para ese contenedor; Brevo lleva una clave falsa, y nada del recorrido manda correo.

Variables: `DURACION` (3m), `CPUS` (0.1), `EJERCICIO_ID` (860, uno del catálogo local), `DB_URL`, `DB_USER`.

El arranque con 0,1 de CPU tarda unos 150 s en local con el equipo tranquilo (en Render, 185-207 s), y pasa de 7 minutos si el equipo está ocupado: el script espera hasta 15. Si tarda mucho más de 150 s, las cifras de esa pasada no valen.

## Qué deja

En `scripts/carga/resultados/<fecha>/` (no se versiona):

- `resumen.txt`: una línea por nivel con p95, p99, mediana, errores y peticiones por segundo.
- `vus-N.json`: el resumen completo de k6 de cada nivel.
- `docker-stats.csv`: CPU, memoria e hilos del contenedor cada pocos segundos.
- `api.log`: el log de la API (busca `Connection is not available` para el pool, `Memoria:` para la memoria).

Los umbrales del guion son los de GP-221: p95 por debajo de 800 ms y 0 % de errores.

## Cuentas

`setup()` crea una cuenta `carga_<pasada>_<n>` por usuario virtual y `teardown()` las borra con `DELETE /usuarios/me`, que se lleva todo lo suyo. Si una pasada se corta a medias, quedan en la base local: se ven con `SELECT username FROM usuarios WHERE username LIKE 'carga\_%'` y se borran igual, entrando con `Carga1234.`.

Resultados de la primera medición: `documentacion/estado/2026-11-12-gp221-escala-sin-coste.md`.
