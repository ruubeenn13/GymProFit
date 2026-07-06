-- ============================================================
-- V202607061600__Add_fotos_perfil_blob.sql
-- Persistencia de las fotos de perfil en BD (BLOB): el filesystem de Render es
-- EFÍMERO y las fotos guardadas en disco se perdían en cada redeploy. Tabla
-- aparte (no columna en usuarios) para que las queries de usuarios (login,
-- listados admin...) no arrastren el BLOB: solo los endpoints de foto la leen.
-- LONGBLOB: es el tipo que Hibernate espera para @Lob byte[] (ddl-auto=validate).
-- ============================================================
CREATE TABLE fotos_perfil
(
    usuario_id          INT          NOT NULL,
    datos               LONGBLOB     NOT NULL,
    content_type        VARCHAR(50)  NOT NULL DEFAULT 'image/jpeg',
    fecha_actualizacion DATETIME     NOT NULL,
    CONSTRAINT pk_fotos_perfil PRIMARY KEY (usuario_id),
    CONSTRAINT fk_fotos_perfil_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);
