-- Cambia altura de DECIMAL(3,2) a DECIMAL(5,2) para permitir valores en cm (ej: 185.00)
ALTER TABLE mediciones_corporales MODIFY COLUMN altura DECIMAL(5, 2) NULL;
