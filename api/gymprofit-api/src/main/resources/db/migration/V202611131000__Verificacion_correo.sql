-- ============================================================================
-- GP-045 · Verificación del correo (DEC-066).
--
-- Hasta ahora el alta creaba la cuenta sin comprobar el correo. Ahora se manda un
-- código de seis dígitos, pero la cuenta se usa entera sin verificar: la
-- verificación solo se pide para lo que depende del correo.
--
-- Aditiva: dos columnas en `usuarios` y una tabla nueva. Las cuentas que ya
-- existen quedan SIN verificar por el DEFAULT FALSE. La excepción de la decisión,
-- las cuentas de Google, no tiene filas que tocar: hoy no existe el acceso con
-- Google (GP-086), así que no hay ninguna.
-- ============================================================================

ALTER TABLE usuarios
    -- Si quien abrió la cuenta demostró que controla su correo.
    ADD COLUMN email_verificado    BOOLEAN  NOT NULL DEFAULT FALSE,
    -- Cuándo. Vuelve a NULL si el correo cambia, igual que la marca.
    ADD COLUMN email_verificado_en DATETIME NULL;

-- Mismo diseño que password_reset_codigos (V202609190900), y por lo mismo: una fila
-- por código para poder caducar, contar intentos y purgar sin tocar `usuarios`.
-- Tabla aparte y no una columna de «propósito» en aquella: emitir un código de
-- verificación invalida los vivos del usuario, y no debe matar uno de recuperación.
CREATE TABLE verificacion_correo_codigos
(
    id               INT AUTO_INCREMENT NOT NULL,

    usuario_id       INT                NOT NULL,

    -- La dirección a la que se mandó. El código verifica ESA dirección y ninguna
    -- otra: si el correo cambia entre medias, el código viejo no sirve.
    email            VARCHAR(100)       NOT NULL,

    -- Hash BCrypt de los seis dígitos. Nunca el código en claro.
    codigo_hash      VARCHAR(255)       NOT NULL,

    fecha_creacion   DATETIME           NOT NULL,
    fecha_expiracion DATETIME           NOT NULL,

    -- Un solo uso: se marca al consumirlo o al emitir otro.
    usado            BOOLEAN            NOT NULL DEFAULT FALSE,

    -- Intentos fallidos. Pasado el máximo, el código muere.
    intentos         INT                NOT NULL DEFAULT 0,

    CONSTRAINT pk_verificacion_correo_codigos PRIMARY KEY (id),
    CONSTRAINT fk_verificacion_correo_codigos_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_verificacion_correo_codigos_usuario
    ON verificacion_correo_codigos (usuario_id, fecha_expiracion);
