# GP-236: cerrar todas las sesiones alcanza también a los access tokens (2026-11-05)

Solo API; la app no se toca. Rama `gp-236-version-sesion`. Sin versión ni etiqueta (DEC-056). La decisión es DEC-058, registrada en el remate una vez confirmado el despliegue.

## Commits

| Hash | Qué |
|---|---|
| `fa84d41` | fix(api): cerrar todas las sesiones alcanza también a los access tokens |

## Qué se endurece

- **Versión de sesión por usuario** (`usuarios.version_sesion`, empieza en 0), sellada en cada access token (claim `ver`) y en cada refresh (`refresh_tokens.version_sesion`).
- **Cambiar o restablecer la contraseña, y desactivar la cuenta, cierran todas las sesiones**, también los access tokens ya emitidos: el filtro JWT responde 401 a uno de una versión anterior, en rutas protegidas y públicas, como con una cuenta desactivada (GP-083). `/auth/refresh` tampoco renueva uno de una versión anterior.
- **Un solo sitio sube la versión**: `RefreshTokenService.cerrarTodasLasSesiones`, que además revoca los refresh. Lo usan los cinco que antes solo revocaban.
- **La rotación del refresh es atómica**, y lo que emite hereda la versión del refresh usado.
- **Compatibilidad**: lo que no lleva versión cuenta como 0; desplegar no cierra la sesión a nadie. La migración solo añade dos columnas con valor por defecto (un `ALTER` por tabla), así que la versión anterior arranca igual con `ddl-auto=validate`.
- **Para la app** es el mismo 401 que el de un token caducado: va a Entrar con el aviso.

## Decisiones

- **Versión y no hora del cambio**: `iat` va en segundos y la app vuelve a entrar justo después de cambiar la contraseña, a menudo en el mismo segundo.
- **MariaDB (local y CI) frente a MySQL (producción)**: con `innodb_snapshot_isolation`, la revocación condicional de un refresh que cambió tras leerlo da error 1020 en vez de 0 filas. `rotar` contesta 401 en los dos casos.
- **Rotar dos veces el mismo refresh a la vez** da 401 a la segunda. La app no lo hace: el autenticador y la renovación del arranque comparten cerrojo.
- Mensaje nuevo `error.sesion.cerrada` (ES y EN) para el 401 del filtro.

## Tests

`VersionSesionTest` (6 casos, contexto entero, JWT de verdad, sin `@Transactional`) y `AuthServiceTest` ajustado. Contra `main` sin el arreglo, 4 de los 5 primeros casos en rojo. Después, cada pieza quitada por separado deja al menos un caso en rojo: el corte del filtro, el sello al entrar, la revocación condicional, la comprobación de versión del refresh, la herencia de versión del access renovado y la subida de versión. Suite de la API: 1272 en verde.

## Entrega

- **PR #79**, con el CI en verde, fusionado en `main` en `3200c0b`. Rama borrada.
- **Despliegue**: `/actuator/info` de producción da `3200c0b` (confirmado también por el propietario).
- **Comprobación en producción**, con una cuenta de prueba creada para ello:
  - Dos sesiones de la misma cuenta; la A cambia la contraseña. La B recibe 401 al momento en una ruta protegida, en una pública y en su refresh. La A vuelve a entrar con la contraseña nueva y recibe 200.
  - Con el release 1.8.0 abierto en Inicio, la contraseña se cambia desde otra sesión: la siguiente petición lleva a la app a Entrar con el aviso de sesión cerrada y el usuario ya escrito.
  - La cuenta se borra y su login da 401; sus claves se borran después.
- **Clientes que rotan el refresh**: la app no rota en paralelo (el autenticador y la renovación del arranque comparten cerrojo); el bot tampoco, comprobado por el propietario fuera de este repositorio (DEC-058).
