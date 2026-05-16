CREATE TABLE logros
(
    id          INT AUTO_INCREMENT NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    descripcion TEXT         NOT NULL,
    tipo        VARCHAR(30)  NOT NULL,
    CONSTRAINT pk_logros PRIMARY KEY (id)
);

CREATE TABLE usuario_logros
(
    id             INT AUTO_INCREMENT NOT NULL,
    usuario_id     INT      NOT NULL,
    logro_id       INT      NOT NULL,
    fecha_obtenido DATETIME NOT NULL,
    CONSTRAINT pk_usuario_logros PRIMARY KEY (id)
);

ALTER TABLE usuario_logros
    ADD CONSTRAINT uc_usuario_logro UNIQUE (usuario_id, logro_id);

ALTER TABLE usuario_logros
    ADD CONSTRAINT fk_usulog_on_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);

ALTER TABLE usuario_logros
    ADD CONSTRAINT fk_usulog_on_logro FOREIGN KEY (logro_id) REFERENCES logros (id);

INSERT INTO logros (nombre, descripcion, tipo) VALUES
    ('Primera sesión',   'Completa tu primera sesión de entrenamiento', 'PRIMERA_SESION'),
    ('Constancia',       'Completa 7 sesiones de entrenamiento',        'CONSTANCIA'),
    ('Dedicado',         'Completa 30 sesiones de entrenamiento',       'DEDICADO'),
    ('Centenario',       'Realiza 100 ejercicios en total',             'CENTENARIO'),
    ('Objetivo cumplido','Completa tu primer objetivo personal',        'OBJETIVO_CUMPLIDO'),
    ('Máquina',          'Completa 10 objetivos personales',            'MAQUINA');
