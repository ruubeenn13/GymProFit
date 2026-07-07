-- ============================================================
-- V202607071030__Fix_grupo_muscular_varchar.sql
-- La columna ejercicios.grupo_muscular es un ENUM de MySQL en las BD
-- antiguas (dev) que NO incluye el literal corregido 'ABDOMEN' → el import
-- de wger fallaba con "Data truncated". Se normaliza a VARCHAR(20) (JPA usa
-- @Enumerated(STRING), no necesita ENUM de BD) y se corrige el typo
-- histórico ABOMEN → ABDOMEN en los datos. En BD creadas solo por Flyway
-- (prod) la columna ya es VARCHAR y el MODIFY es un no-op seguro.
-- ============================================================

ALTER TABLE ejercicios MODIFY grupo_muscular VARCHAR(20) NOT NULL;

UPDATE ejercicios SET grupo_muscular = 'ABDOMEN' WHERE grupo_muscular = 'ABOMEN';
