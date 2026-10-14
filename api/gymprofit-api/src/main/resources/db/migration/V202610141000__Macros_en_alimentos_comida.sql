-- ============================================================
-- Lote 1.6.6 (A1): cada línea de una comida guarda sus macros
--
-- Hasta ahora una línea guardaba sus gramos y sus kcal, pero la proteína, los
-- carbohidratos y la grasa se calculaban al leerla con los valores que tuviera el
-- alimento ese día: corregir un alimento propio cambiaba los macros de días pasados y
-- no sus kcal. Desde aquí se calculan al escribir la línea y se leen tal cual.
--
-- Las columnas nacen a 0; las rellena la migración siguiente
-- (V202610141001__Macros_en_alimentos_comida_rellenar.sql), que se puede volver a
-- ejecutar sin cambiar nada y por eso va aparte.
--
-- Los totales de la comida pasan de DECIMAL(5,2) a DECIMAL(8,2), como las líneas: un
-- plato apuntado por raciones puede sumar más de 999,99 g de un macro en una comida, y
-- con (5,2) eso era un error de la base.
-- ============================================================

ALTER TABLE alimentos_comida
    ADD COLUMN proteinas_totales     DECIMAL(8,2) NOT NULL DEFAULT 0,
    ADD COLUMN carbohidratos_totales DECIMAL(8,2) NOT NULL DEFAULT 0,
    ADD COLUMN grasas_totales        DECIMAL(8,2) NOT NULL DEFAULT 0;

ALTER TABLE comidas
    MODIFY total_proteinas     DECIMAL(8,2) NULL,
    MODIFY total_carbohidratos DECIMAL(8,2) NULL,
    MODIFY total_grasas        DECIMAL(8,2) NULL;
