-- ============================================================
-- Ejercicios: equipamiento como lista cerrada y marca de nombre revisado (GP-085).
--
-- equipamiento: un valor de Equipamiento (enum). equipo_necesario, el texto libre de
-- la importación, se queda como está: lo sigue leyendo la app.
-- nombre_revisado: alguien ha mirado el nombre en español. La web de administración
-- filtra por él para traducir el catálogo (DEC-020, T-10 de la auditoría de diseño).
--
-- Las columnas van en esta migración y el relleno en la siguiente, que es la que se
-- puede volver a ejecutar sobre filas preparadas en el test.
-- ============================================================
ALTER TABLE ejercicios
    ADD COLUMN equipamiento VARCHAR(20) NOT NULL DEFAULT 'OTRO',
    ADD COLUMN nombre_revisado TINYINT(1) NOT NULL DEFAULT 0;
