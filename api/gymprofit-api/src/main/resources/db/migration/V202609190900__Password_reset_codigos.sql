-- ============================================================================
-- Recuperación de contraseña por código de un solo uso.
--
-- Hasta ahora no existía: quien olvidaba la contraseña perdía la cuenta, sin
-- más salida que crearse otra. Es un bloqueante para publicar en Play Store.
--
-- El código son 6 dígitos que viajan al correo del usuario, y aquí NO se guarda
-- el código sino su hash BCrypt, por el mismo motivo por el que no se guarda la
-- contraseña: quien lea la tabla no debe poder entrar en ninguna cuenta.
--
-- Se guarda una fila por solicitud, en vez de una columna en `usuarios`, para
-- poder caducar, contar intentos y auditar sin tocar la tabla principal.
-- ============================================================================

CREATE TABLE password_reset_codigos
(
    id               INT AUTO_INCREMENT NOT NULL,

    -- Cuenta para la que se pidió el código.
    usuario_id       INT                NOT NULL,

    -- Hash BCrypt de los 6 dígitos. Nunca el código en claro.
    codigo_hash      VARCHAR(255)       NOT NULL,

    fecha_creacion   DATETIME           NOT NULL,

    -- Vida corta a propósito: un código de recuperación que dura horas es una
    -- contraseña alternativa sentada en una bandeja de entrada.
    fecha_expiracion DATETIME           NOT NULL,

    -- Un código sirve UNA vez. Al consumirlo se marca, no se borra, para que un
    -- reintento con el mismo código falle de forma explícita.
    usado            BOOLEAN            NOT NULL DEFAULT FALSE,

    -- Intentos fallidos de verificación. Pasado el máximo el código muere: si no,
    -- seis dígitos se adivinan a fuerza bruta en un rato.
    intentos         INT                NOT NULL DEFAULT 0,

    CONSTRAINT pk_password_reset_codigos PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_codigos_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

-- Se consulta siempre "el último código vivo de este usuario".
CREATE INDEX idx_password_reset_codigos_usuario ON password_reset_codigos (usuario_id, fecha_expiracion);
