-- ============================================================
-- Un solo programa abierto por usuario (GP-074, lote 1.2.1)
--
-- Con la API 1.2.0, seguir otro programa no cerraba el anterior. Solo pudo pasar
-- llamándola a mano (ninguna build la usa), pero si pasó: se queda el más reciente
-- (el id más alto: se crean en orden) y los demás se cierran, con sus rutinas
-- desactivadas, igual que al dejarlo. Las sesiones y los récords no se tocan.
--
-- La tabla derivada lleva GROUP BY para que MySQL la materialice y deje actualizar
-- la misma tabla de la que lee.
-- ============================================================
UPDATE programas_usuario pu
    JOIN (SELECT usuario_id, MAX(id) AS ultimo
          FROM programas_usuario
          WHERE fecha_fin IS NULL
          GROUP BY usuario_id) abiertos ON abiertos.usuario_id = pu.usuario_id
SET pu.fecha_fin = NOW()
WHERE pu.fecha_fin IS NULL
  AND pu.id <> abiertos.ultimo;

UPDATE rutinas r
    JOIN programas_usuario pu ON pu.id = r.programa_usuario_id
SET r.activa = 0
WHERE pu.fecha_fin IS NOT NULL
  AND r.activa = 1;
