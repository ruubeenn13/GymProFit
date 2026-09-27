-- ============================================================
-- Último acceso de cada cuenta (GP-085, web de administración).
-- Se pone al entrar y al renovar el token; NULL = no ha entrado desde que existe la
-- columna. Hora del reloj del servidor, igual que fecha_registro.
-- ============================================================
ALTER TABLE usuarios ADD COLUMN ultimo_acceso DATETIME NULL;
