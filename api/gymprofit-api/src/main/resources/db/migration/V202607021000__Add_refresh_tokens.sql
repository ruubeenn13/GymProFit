-- ============================================================
-- V202607021000__Add_refresh_tokens.sql
-- Tabla de refresh tokens (opacos, persistidos y revocables) para el flujo
-- de renovación de sesión: el access token JWT es corto y se renueva con
-- un refresh token de larga duración que puede rotarse y revocarse (logout).
-- ============================================================
CREATE TABLE refresh_tokens
(
    id               INT AUTO_INCREMENT NOT NULL,
    token            VARCHAR(255)       NOT NULL,
    usuario_id       INT                NOT NULL,
    fecha_expiracion DATETIME           NOT NULL,
    revocado         BOOLEAN            NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);
