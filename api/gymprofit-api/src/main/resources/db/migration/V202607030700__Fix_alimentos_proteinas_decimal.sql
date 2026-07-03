-- Corrige el tipo de alimentos.proteinas: la migración inicial la creó como INT
-- pero la entidad Alimento la mapea como BigDecimal(5,2) → ddl-auto=validate falla
-- en una BD construida desde cero (CI). En BDs donde ya sea DECIMAL (drift manual)
-- este MODIFY es un no-op.
ALTER TABLE alimentos MODIFY proteinas DECIMAL(5, 2) NULL;
