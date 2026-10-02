-- Lote 1.6.1 · Reportar un alimento: los avisos que revisa el administrador.
--
-- Sin quién: ni usuario ni texto libre, para que un aviso no sea un dato personal (no
-- entra en la política de privacidad ni en el borrado de la cuenta). El freno por
-- cuenta, para que una sola persona no infle un aviso, vive en memoria (FrenoAvisos).
--
-- Un aviso es de un alimento del catálogo, o del código de un producto de productos_off
-- que aún no se ha materializado. `clave` los junta en una columna («a:123» o
-- «c:8410…»), y la clave única (clave, motivo, abierto) deja un solo aviso ABIERTO por
-- alimento y motivo: al resolverlo, `abierto` pasa a NULL, que no choca con nada, y el
-- siguiente aviso abre otro.
CREATE TABLE avisos_alimento (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    alimento_id INT          NULL,
    barcode     VARCHAR(32)  NULL,
    clave       VARCHAR(40)  NOT NULL,
    motivo      VARCHAR(20)  NOT NULL,
    veces       INT          NOT NULL DEFAULT 1,
    abierto     TINYINT(1)   NULL DEFAULT 1,
    creado      DATETIME     NOT NULL,
    actualizado DATETIME     NOT NULL,
    resuelto    DATETIME     NULL,
    CONSTRAINT fk_avisos_alimento_alimento FOREIGN KEY (alimento_id)
        REFERENCES alimentos (id) ON DELETE CASCADE,
    CONSTRAINT uq_avisos_alimento_abierto UNIQUE (clave, motivo, abierto)
);

CREATE INDEX idx_avisos_alimento_abierto ON avisos_alimento (abierto, veces);
