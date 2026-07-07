-- ============================================================
-- V202607071400__Ejercicio_musculo_primario.sql
-- Precisión del catálogo de ejercicios: músculo primario REAL (de
-- free-exercise-db: 'adductors', 'quadriceps'...) traducido ES/EN, en
-- lugar del grupo grueso de wger. El grupo_muscular (enum del chip de
-- filtro) se sigue usando pero ahora se DERIVA del músculo primario, más
-- fino que la categoría de wger. Se rellena al reimportar el catálogo.
-- ============================================================

ALTER TABLE ejercicios
    ADD COLUMN musculo_primario    VARCHAR(60) NULL,
    ADD COLUMN musculo_primario_en VARCHAR(60) NULL;
