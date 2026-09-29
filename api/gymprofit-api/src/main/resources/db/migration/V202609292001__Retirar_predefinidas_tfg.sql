-- ============================================================
-- Se retiran las seis rutinas predefinidas del TFG (catálogo v1, decisión 6 del punto 8)
--
-- Las sustituyen los programas del catálogo. Al aplicarse, las únicas filas con
-- es_predefinida = 1 son esas seis («Pecho y Tríceps», «Full Body», «Movilidad Activa»,
-- «Espalda y Bíceps», «Powerlifting Base», «HIIT Avanzado»), y solo en las bases de
-- desarrollo: producción no tenía ninguna (comprobado el 2026-09-29 con una cuenta de
-- prueba, GET /rutinas/predefinidas daba []). Las plantillas nuevas no son predefinidas.
--
-- Dos casos, porque sesiones_entrenamiento.rutina_id apunta a rutinas con FK RESTRICT:
--   · sin sesiones que la usen: se borra, con sus ejercicios;
--   · con sesiones: se queda la fila para no romper el historial, pero deja de ser
--     predefinida y se desactiva. Sin usuario y sin ser predefinida ni plantilla, ningún
--     usuario la ve por ninguna ruta (RutinaService.canView).
-- ============================================================

DELETE re
FROM rutina_ejercicio re
         JOIN rutinas r ON r.id = re.rutina_id
WHERE r.es_predefinida = 1
  AND NOT EXISTS (SELECT 1 FROM sesiones_entrenamiento s WHERE s.rutina_id = r.id);

DELETE r
FROM rutinas r
WHERE r.es_predefinida = 1
  AND NOT EXISTS (SELECT 1 FROM sesiones_entrenamiento s WHERE s.rutina_id = r.id);

UPDATE rutinas
SET es_predefinida = 0,
    activa         = 0
WHERE es_predefinida = 1;
