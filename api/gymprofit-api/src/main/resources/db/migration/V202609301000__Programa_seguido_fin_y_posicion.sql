-- ============================================================
-- «Programa que sigue»: fin y posición inicial del ciclo (GP-074, lote 1.2.1)
--
-- Un usuario sigue un programa a la vez. Dejarlo, o seguir otro, le pone fecha_fin
-- (NULL mientras se sigue). posicion_inicial es por dónde empieza el ciclo: 1 al
-- seguir uno nuevo, y la que tocaba al cambiar el tiempo del mismo programa.
-- ============================================================
ALTER TABLE programas_usuario
    ADD COLUMN fecha_fin        DATETIME NULL,
    ADD COLUMN posicion_inicial INT      NOT NULL DEFAULT 1;
