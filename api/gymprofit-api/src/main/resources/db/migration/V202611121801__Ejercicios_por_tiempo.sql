-- ============================================================
-- GP-130: los ejercicios del catálogo que se miden en segundos
--
-- Los que se hacen manteniendo una posición: la plancha y los demás isométricos. La lista
-- se revisó contra el nombre y la descripción de producción el 12-11-2026. Los dudosos
-- (estiramientos, liberación con rodillo, paseos con carga, cardio, Superman, Bird Dog,
-- Dead bug, limpiaparabrisas isométrico, cruz de hierro) se quedan en repeticiones y van al
-- informe del lote 19: la medida se puede cambiar en la pauta de cada rutina.
--
-- Los del catálogo de free-exercise-db se marcan por fed_id, que es su identificador
-- estable y el mismo en todas las bases (el id numérico cambia de una a otra). Los viejos
-- de wger, inactivos y sin fed_id, por id y nombre a la vez: si en otra base ese id es otro
-- ejercicio, no se toca. Solo hay UPDATE: aplicarla dos veces da lo mismo.
-- ============================================================

-- Activos, por fed_id (id de producción entre paréntesis).
UPDATE ejercicios SET medida = 'SEGUNDOS'
WHERE fed_id IN (
    'Plank',                                    -- (837) Plancha
    'Side_Bridge',                              -- (960) Plancha lateral
    'One_Handed_Hang',                          -- (813) Suspensión en barra a una mano
    'Crucifix',                                 -- (484) Crucifijo: aguantar los brazos en cruz
    'Plate_Pinch',                              -- (838) Pinza con discos
    'Stomach_Vacuum',                           -- (1091) Vacío abdominal
    'Isometric_Chest_Squeezes',                 -- (658) Presión isométrica de pecho
    'Isometric_Neck_Exercise_-_Front_And_Back', -- (659) Isométrico de cuello, delante y detrás
    'Isometric_Neck_Exercise_-_Sides'           -- (660) Isométrico de cuello, laterales
);

-- Inactivos de wger, por id y nombre (pueden seguir en el historial de alguien).
UPDATE ejercicios SET medida = 'SEGUNDOS' WHERE id = 266 AND nombre = 'Plancha de antebrazo';
UPDATE ejercicios SET medida = 'SEGUNDOS' WHERE id = 15  AND nombre = 'Sostenimiento Lateral Isométrico';
UPDATE ejercicios SET medida = 'SEGUNDOS' WHERE id = 54  AND nombre = 'Hollow hold';
UPDATE ejercicios SET medida = 'SEGUNDOS' WHERE id = 169 AND nombre = 'Sentadilla isometrica al fallo';
UPDATE ejercicios SET medida = 'SEGUNDOS' WHERE id = 24  AND nombre = 'Suspensión en Romos';
