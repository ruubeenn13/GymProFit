-- GP-236 · Versión de sesión por usuario.
--
-- Cerrar todas las sesiones de una cuenta sube su versión, y cada access token y cada
-- refresh lleva sellada la versión con la que se emitió: los de una versión anterior
-- dejan de valer.
--
-- Empieza en 0, que es también lo que vale un token emitido antes de esta migración:
-- desplegar no cierra la sesión a nadie. Solo añade columnas con valor por defecto, así
-- que la versión anterior de la API sigue arrancando con ddl-auto=validate si se vuelve
-- atrás.
ALTER TABLE usuarios ADD COLUMN version_sesion INT NOT NULL DEFAULT 0;

ALTER TABLE refresh_tokens ADD COLUMN version_sesion INT NOT NULL DEFAULT 0;
