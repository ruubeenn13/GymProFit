-- Lote 1.6.1 · La ración elegida se guarda en la línea de la comida.
--
-- Los gramos (cantidad_gramos) siguen siendo lo que cuenta para las calorías y los
-- macros. La ración y cuántas solo dicen cómo lo eligió el usuario, para que la app
-- enseñe «2 rebanadas (56 g)» y abra la ficha con esa unidad. Las dos son opcionales:
-- las líneas de antes y las que se apuntan en gramos no llevan ninguna.
-- Si se borra la ración (el alimento cambia sus raciones), la línea se queda en gramos.
ALTER TABLE alimentos_comida
    ADD COLUMN racion_id INT NULL,
    ADD COLUMN raciones DECIMAL(5,2) NULL,
    ADD CONSTRAINT fk_alimentos_comida_racion FOREIGN KEY (racion_id)
        REFERENCES alimento_raciones (id) ON DELETE SET NULL;
