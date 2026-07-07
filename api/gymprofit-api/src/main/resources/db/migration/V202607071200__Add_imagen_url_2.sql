-- ============================================================
-- V202607071200__Add_imagen_url_2.sql
-- Segundo fotograma de la demostración del ejercicio (free-exercise-db
-- publica 2 posiciones por ejercicio): la app alterna imagen_url ↔
-- imagen_url_2 para animar el "monigote" haciendo el ejercicio.
-- NULL = solo imagen estática (o ninguna).
-- ============================================================

ALTER TABLE ejercicios
    ADD COLUMN imagen_url_2 VARCHAR(255) NULL;
