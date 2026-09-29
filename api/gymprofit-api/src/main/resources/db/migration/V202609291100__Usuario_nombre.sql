-- ============================================================
-- Nombre para mostrar del usuario (GP-116).
-- Opcional y aparte del de usuario, que sigue siendo el de entrar. La API lo guarda
-- recortado, de 1 a 40 caracteres; NULL = sin nombre, y la app usa el de usuario.
-- ============================================================
ALTER TABLE usuarios ADD COLUMN nombre VARCHAR(40) NULL;
