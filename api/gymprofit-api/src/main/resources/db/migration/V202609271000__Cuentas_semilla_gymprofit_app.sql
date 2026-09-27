-- ============================================================
-- GP-106 · Las cuentas semilla pasan al dominio del producto.
--
-- admin y guest se crearon con correos de gymprofit.com, un dominio que no es del
-- proyecto. Pasan a gymprofit.app.
--
-- Solo se cambia la fila cuyo correo sigue siendo EXACTAMENTE el de .com (comparación binaria:
-- la colación de la columna no distingue mayúsculas). Si el propietario ya le puso
-- otro correo a la cuenta, se respeta.
--
-- Y solo si la dirección de destino está libre: la columna es UNIQUE, y un choque
-- aquí no fallaría la migración sin más, tumbaría el arranque de producción. La
-- tabla derivada con DISTINCT se materializa, que es lo que permite a MySQL leer
-- usuarios dentro de un UPDATE sobre usuarios (sin ella, error 1093).
-- ============================================================

UPDATE usuarios
SET email = 'admin@gymprofit.app'
WHERE email = CAST('admin@gymprofit.com' AS BINARY)
  AND NOT EXISTS (SELECT 1
                  FROM (SELECT DISTINCT email FROM usuarios WHERE email = 'admin@gymprofit.app') AS ocupado);

UPDATE usuarios
SET email = 'guest@gymprofit.app'
WHERE email = CAST('guest@gymprofit.com' AS BINARY)
  AND NOT EXISTS (SELECT 1
                  FROM (SELECT DISTINCT email FROM usuarios WHERE email = 'guest@gymprofit.app') AS ocupado);
