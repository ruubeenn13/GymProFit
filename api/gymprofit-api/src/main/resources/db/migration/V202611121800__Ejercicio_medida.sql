-- ============================================================
-- GP-130: en qué se mide un ejercicio del catálogo
--
-- Hasta ahora la medida (repeticiones o segundos) solo vivía en la pauta de una rutina
-- (rutinas_ejercicios.medida, GP-125/GP-244). Un ejercicio suelto no la tenía, así que en
-- una sesión libre la plancha pedía repeticiones la primera vez. Con la columna aquí, la app
-- la toma al añadir el ejercicio a una rutina o a una sesión, y la API la copia a la pauta
-- cuando una build vieja añade un ejercicio sin decir la medida.
--
-- Todo empieza en REPETICIONES. Los que van en segundos los marca la migración siguiente
-- (V202611121801), aparte para que un test pueda volver a aplicarla.
-- ============================================================

ALTER TABLE ejercicios
    ADD COLUMN medida VARCHAR(15) NOT NULL DEFAULT 'REPETICIONES';
