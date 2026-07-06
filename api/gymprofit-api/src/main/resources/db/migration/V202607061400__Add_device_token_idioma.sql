-- ============================================================
-- V202607061400__Add_device_token_idioma.sql
-- Añade la columna idioma a device_tokens para internacionalizar las
-- notificaciones push generadas por el servidor. Cada dispositivo registra
-- el idioma de su usuario (código ISO de 2 letras, p.ej. 'es', 'en').
-- Por defecto 'es' (español), también para las filas ya existentes.
-- ============================================================
ALTER TABLE device_tokens
    ADD COLUMN idioma VARCHAR(5) NOT NULL DEFAULT 'es';
