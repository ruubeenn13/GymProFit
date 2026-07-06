-- ============================================================
-- V202607061200__Add_device_tokens.sql
-- Tabla de tokens de dispositivo (FCM) para notificaciones push.
-- Un usuario puede tener varios dispositivos; cada uno registra su token FCM.
-- El token es único (si se reasigna a otro usuario, se actualiza usuario_id).
-- ============================================================
CREATE TABLE device_tokens
(
    id                  INT AUTO_INCREMENT NOT NULL,
    token               VARCHAR(255)       NOT NULL,
    usuario_id          INT                NOT NULL,
    plataforma          VARCHAR(20)        NOT NULL DEFAULT 'ANDROID',
    fecha_registro      DATETIME           NOT NULL,
    fecha_actualizacion DATETIME           NOT NULL,
    CONSTRAINT pk_device_tokens PRIMARY KEY (id),
    CONSTRAINT uq_device_tokens_token UNIQUE (token),
    CONSTRAINT fk_device_tokens_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);
