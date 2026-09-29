-- ============================================================
-- Sexo y nivel de actividad del usuario (GP-111).
-- Hasta ahora solo vivían en el móvil; quien reinstalaba tenía las calorías calculadas
-- como hombre moderado. Opcionales: NULL = el usuario no lo ha dicho (las cuentas que
-- ya existen, hasta que la app los suba). Valores de los enums Sexo y NivelActividad.
-- ============================================================
ALTER TABLE usuarios
    ADD COLUMN sexo VARCHAR(10) NULL,
    ADD COLUMN nivel_actividad VARCHAR(20) NULL;
