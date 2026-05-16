-- Cambia la columna altura de usuarios de DECIMAL(3,2) a DECIMAL(5,2)
-- para permitir almacenar la altura en cm (ej: 178.50) en lugar de metros (ej: 1.78)
ALTER TABLE usuarios MODIFY COLUMN altura DECIMAL(5, 2) NULL;
