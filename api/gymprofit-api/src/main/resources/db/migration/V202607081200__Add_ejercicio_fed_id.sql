-- fed_id: identificador estable de free-exercise-db (el "id" slug del dataset).
-- Pasa a ser la clave de upsert del catálogo al usar free-exercise-db como fuente
-- BASE (~800 ejercicios, todos con demostración de 2 fotogramas). Nullable + único
-- (MySQL/MariaDB permite varios NULL en índice único, para las filas viejas de wger).
ALTER TABLE ejercicios ADD COLUMN fed_id VARCHAR(120) NULL;
ALTER TABLE ejercicios ADD CONSTRAINT ux_ejercicios_fed_id UNIQUE (fed_id);
