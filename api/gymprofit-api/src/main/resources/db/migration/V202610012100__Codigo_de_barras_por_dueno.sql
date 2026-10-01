-- GP-160 · El código de barras es único por dueño, no en toda la tabla (DEC-040).
--
-- En la 1.6.1, «Créalo» guardará el código de un producto que no existe en un alimento
-- TUYO. Ese código tiene que poder convivir con el de un producto del catálogo que llegue
-- después, y con el mismo código en el alimento de otra persona: cada uno ve el suyo.
--   · Alimentos de un usuario: (barcode, usuario_id) único.
--   · Catálogo (usuario_id NULL): MySQL y MariaDB dejan repetir NULL en una clave única,
--     así que aquí la unicidad de los productos la pone (fuente, codigo_origen), que ya
--     existe (V202610011900); y para el catálogo hecho a mano, el servicio.
-- No se hace con una columna generada IFNULL(usuario_id, 0): MySQL no la permite sobre
-- una columna cuya clave ajena borra con SET NULL, que es el caso de usuario_id.
ALTER TABLE alimentos DROP INDEX uq_alimentos_barcode;
ALTER TABLE alimentos ADD CONSTRAINT uq_alimentos_barcode_usuario UNIQUE (barcode, usuario_id);
