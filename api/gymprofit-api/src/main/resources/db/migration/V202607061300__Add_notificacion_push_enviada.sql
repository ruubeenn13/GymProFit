-- ============================================================
-- V202607061300__Add_notificacion_push_enviada.sql
-- Flag de control del envío push: el job programado envía las notificaciones
-- con fecha_programada vencida y push_enviada=FALSE, y las marca a TRUE para
-- no duplicar envíos. DEFAULT TRUE: las notificaciones históricas se consideran
-- ya gestionadas (no deben re-enviarse retroactivamente al desplegar esto).
-- ============================================================
ALTER TABLE notificaciones
    ADD COLUMN push_enviada TINYINT(1) NOT NULL DEFAULT 1;
