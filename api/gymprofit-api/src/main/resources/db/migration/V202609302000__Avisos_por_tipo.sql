-- GP-112 (lote 1.5.0): avisos por tipo.
-- Tres interruptores por cuenta para los recordatorios que genera el servidor:
--   entrenar  → el de inactividad (el de las 18:00 se retira: saltaba también los días
--               de descanso de un programa).
--   comidas   → los cinco de comidas. Apagado de serie: son cinco avisos al día y solo
--               sirven a quien registra lo que come.
--   progreso  → resumen semanal, logro próximo, medición mensual y objetivo por vencer.
-- El DEFAULT de ADD COLUMN rellena también las cuentas que ya existen, así que los
-- valores de serie valen para todas: quien recibía los de comidas deja de recibirlos
-- hasta que los encienda (la 1.4.0 no tiene dónde).
ALTER TABLE usuarios
    ADD COLUMN avisos_entrenar TINYINT(1) NOT NULL DEFAULT 1,
    ADD COLUMN avisos_comidas  TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN avisos_progreso TINYINT(1) NOT NULL DEFAULT 1;
