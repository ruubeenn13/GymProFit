-- GP-182 (lote 1.6.4) · En un producto, la ración declarada va delante del envase si es
-- más pequeña que él (RacionesProducto). Los productos ya materializados tienen sus
-- raciones en el orden de la 1.6.1: «1 envase» en el puesto 1 y «1 ración» en el 2.
-- Se cambia solo el orden: las filas y sus ids se quedan como están, porque hay líneas
-- de comidas que las usan (alimentos_comida.racion_id).
--
-- Solo los productos de Open Food Facts con justo esas dos raciones del materializador.
-- Un multipack («1 unidad») no cambia: la unidad sigue primero.
--
-- (alimento_id, orden) es único: primero se apartan las dos filas a 1001 y 1002, y
-- después se devuelven a 1 y 2 ya cambiadas.
UPDATE alimento_raciones envase
    JOIN alimento_raciones racion
        ON racion.alimento_id = envase.alimento_id AND racion.orden = 2 AND racion.nombre = '1 ración'
    JOIN alimentos a ON a.id = envase.alimento_id
SET envase.orden = 1002,
    racion.orden = 1001
WHERE a.fuente = 'OFF'
  AND envase.orden = 1
  AND envase.nombre = '1 envase'
  AND envase.fuente LIKE 'Open Food Facts: envase%'
  AND racion.gramos < envase.gramos;

UPDATE alimento_raciones
SET orden = orden - 1000
WHERE orden IN (1001, 1002);
