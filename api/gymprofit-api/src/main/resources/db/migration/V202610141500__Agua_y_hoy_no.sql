-- Lote 1.6.8 · El agua y «Hoy no» (GP-166 y GP-162, decisiones 10 y 11 del lienzo).
--
-- El agua: los mililitros de un día, uno por cuenta y día. La app suma y resta vasos y
-- manda el total, así que repetir la misma petición deja lo mismo.
CREATE TABLE agua_diaria (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  INT      NOT NULL,
    fecha       DATE     NOT NULL,
    mililitros  INT      NOT NULL,
    actualizado DATETIME NOT NULL,
    CONSTRAINT fk_agua_diaria_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT uq_agua_diaria_usuario_fecha UNIQUE (usuario_id, fecha)
);

-- «Hoy no»: una comida que ese día no se hace, y que cuenta para el día completo. Una
-- marca por cuenta, día y comida; quitarla borra la fila. Va aparte de `comidas` porque
-- una comida marcada no tiene por qué existir, y crearla vacía la haría aparecer en el
-- historial y en los atajos que leen las comidas.
CREATE TABLE comidas_hoy_no (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  INT         NOT NULL,
    fecha       DATE        NOT NULL,
    tipo_comida VARCHAR(20) NOT NULL,
    creado      DATETIME    NOT NULL,
    CONSTRAINT fk_comidas_hoy_no_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT uq_comidas_hoy_no_usuario_fecha_tipo UNIQUE (usuario_id, fecha, tipo_comida)
);
