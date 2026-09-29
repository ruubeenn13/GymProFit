-- ============================================================
-- Series por tiempo (GP-125, lote 1.2.0)
--
-- La plancha y la plancha lateral se miden en segundos. Una serie por tiempo guarda
-- sus segundos aquí; repeticiones sigue siendo NOT NULL y va a 0, para no romper a
-- las builds que ya leen series. NULL en todas las series de antes y en las de
-- repeticiones.
-- ============================================================
ALTER TABLE series_realizadas
    ADD COLUMN segundos INT NULL;
