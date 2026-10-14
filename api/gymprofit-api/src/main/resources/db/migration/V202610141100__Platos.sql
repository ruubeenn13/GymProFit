-- ============================================================
-- Lote 1.6.6 (A2): los platos propios (GP-165, DEC-047)
--
-- Un plato es un alimento del usuario con ingredientes: así sale solo en la búsqueda,
-- en recientes, en favoritos, en «Lo que sueles» y al copiar una comida.
--   · plato_raciones: cuántas raciones salen (1 a 99); NULL en lo que no es un plato.
--   · plato_peso_gramos: el peso del plato hecho, opcional. Sin él, los valores del
--     alimento son los de una ración; con él, por 100 g.
-- Sus raciones son filas de alimento_raciones: la de «sin peso», de 100 g por dentro y
-- que no cambia nunca, y la de «con peso», con sus gramos de verdad. AlimentoService
-- las distingue por su fuente (Platos.FUENTE_SIN_PESO, Platos.FUENTE_CON_PESO).
--
-- Los ingredientes no se pueden quedar sin su alimento: la clave ajena no borra en
-- cascada, y borrar del todo un alimento que está en un plato es un 409.
-- ============================================================

ALTER TABLE alimentos
    ADD COLUMN plato_raciones    INT          NULL,
    ADD COLUMN plato_peso_gramos DECIMAL(7,1) NULL;

CREATE TABLE plato_ingredientes (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    plato_id        INT          NOT NULL,
    alimento_id     INT          NOT NULL,
    cantidad_gramos DECIMAL(6,2) NOT NULL,
    racion_id       INT          NULL,
    raciones        DECIMAL(5,2) NULL,
    orden           INT          NOT NULL,
    CONSTRAINT fk_plato_ingredientes_plato FOREIGN KEY (plato_id)
        REFERENCES alimentos (id) ON DELETE CASCADE,
    CONSTRAINT fk_plato_ingredientes_alimento FOREIGN KEY (alimento_id)
        REFERENCES alimentos (id),
    CONSTRAINT fk_plato_ingredientes_racion FOREIGN KEY (racion_id)
        REFERENCES alimento_raciones (id) ON DELETE SET NULL
);

CREATE INDEX ix_plato_ingredientes_plato ON plato_ingredientes (plato_id, orden);
