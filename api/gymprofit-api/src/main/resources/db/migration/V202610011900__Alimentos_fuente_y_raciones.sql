-- GP-127 · Cada alimento dice de dónde sale y si está revisado, y tiene sus raciones.
--
-- fuente:        CIQUAL y USDA (básicos), OFF (Open Food Facts); NULL, hecho a mano
--                (catálogo de ADMIN o alimento de un usuario).
-- codigo_origen: el código del alimento en su fuente. Con la fuente, identifica la fila:
--                es la clave con la que la carga de básicos es idempotente.
-- revisado:      1 en los básicos, curados uno a uno; 0 en el resto.
ALTER TABLE alimentos
    ADD COLUMN fuente        VARCHAR(16) NULL,
    ADD COLUMN codigo_origen VARCHAR(32) NULL,
    ADD COLUMN revisado      TINYINT(1)  NOT NULL DEFAULT 0,
    ADD CONSTRAINT uq_alimentos_fuente_codigo UNIQUE (fuente, codigo_origen);

-- Los productos ya importados por código de barras son de Open Food Facts.
UPDATE alimentos
SET fuente = 'OFF', codigo_origen = barcode
WHERE barcode IS NOT NULL AND usuario_id IS NULL;

-- Raciones: «1 unidad mediana (118 g)». El nombre no lleva los gramos: los pone el cliente.
-- fuente dice de dónde sale el peso, para poder revisarlo.
CREATE TABLE alimento_raciones (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    alimento_id INT          NOT NULL,
    nombre      VARCHAR(60)  NOT NULL,
    nombre_en   VARCHAR(60)  NOT NULL,
    gramos      DECIMAL(6,1) NOT NULL,
    fuente      VARCHAR(255) NOT NULL,
    orden       INT          NOT NULL,
    CONSTRAINT fk_alimento_raciones_alimento FOREIGN KEY (alimento_id)
        REFERENCES alimentos (id) ON DELETE CASCADE,
    CONSTRAINT uq_alimento_raciones_orden UNIQUE (alimento_id, orden)
);
