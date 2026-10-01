-- GP-164 · Productos que se venden en España, de la exportación de Open Food Facts.
--
-- Tabla propia, que dice su origen en el nombre: los datos de Open Food Facts (ODbL)
-- se copian tal cual y no se modifican. La llena un workflow semanal por
-- POST /importacion/productos (DEC-041); la API nunca descarga la exportación.
--
-- Un producto no es un alimento hasta que alguien lo elige: entonces se materializa en
-- `alimentos` (DEC-032), y reimportar ya no toca esa fila, porque hay comidas que la usan.
-- Todos los valores son por 100 g.
CREATE TABLE productos_off (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    codigo        VARCHAR(32)  NOT NULL,
    nombre        VARCHAR(200) NOT NULL,
    marca         VARCHAR(100) NULL,
    kcal          DECIMAL(5,1) NOT NULL,
    proteinas     DECIMAL(5,2) NOT NULL,
    carbohidratos DECIMAL(5,2) NOT NULL,
    grasas        DECIMAL(5,2) NOT NULL,
    fibra         DECIMAL(5,2) NULL,
    racion_gramos DECIMAL(6,1) NULL,
    racion_texto  VARCHAR(60)  NULL,
    envase        VARCHAR(60)  NULL,
    escaneos      INT          NOT NULL DEFAULT 0,
    actualizado   DATETIME     NOT NULL,
    CONSTRAINT uq_productos_off_codigo UNIQUE (codigo)
);
