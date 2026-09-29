-- ============================================================
-- Programas y plantillas de rutina (GP-074, lote 1.2.0)
--
-- Un programa es un plan de varios días (catálogo v1, documentacion/CATALOGO-PLANTILLAS.md):
-- su semana es una secuencia ordenada de rutinas, que puede repetir alguna (el de 6 días
-- lleva Empuje y Tirón dos veces). Las rutinas de la semana son PLANTILLAS: sin usuario,
-- con código estable y marcadas con es_plantilla. No son predefinidas: /rutinas/predefinidas
-- alimenta el carrusel de las builds 1.1.x repartidas, y esas no deben verlas.
--
-- Quien sigue un programa se lleva una copia suya de cada rutina distinta, enlazada a la
-- plantilla de la que sale y a su fila de programas_usuario.
--
-- Todo es aditivo: las columnas nuevas admiten NULL o tienen valor por defecto, así que
-- lo que escriben las builds viejas sigue siendo válido.
-- ============================================================

CREATE TABLE programas
(
    id             INT AUTO_INCREMENT NOT NULL,
    codigo         VARCHAR(20)  NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    nombre_en      VARCHAR(100) NULL,
    descripcion    TEXT NULL,
    descripcion_en TEXT NULL,
    nivel          VARCHAR(20)  NOT NULL,
    equipamiento   VARCHAR(20)  NOT NULL,
    dias_min       INT          NOT NULL,
    dias_max       INT          NOT NULL,
    activo         TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT pk_programas PRIMARY KEY (id),
    CONSTRAINT uk_programas_codigo UNIQUE (codigo)
);

-- La semana: una fila por día, en orden. Una rutina puede salir dos veces en la misma
-- semana, por eso la clave natural es (programa, posición) y no (programa, rutina).
CREATE TABLE programa_rutina
(
    id          INT AUTO_INCREMENT NOT NULL,
    programa_id INT NOT NULL,
    rutina_id   INT NOT NULL,
    posicion    INT NOT NULL,
    CONSTRAINT pk_programa_rutina PRIMARY KEY (id),
    CONSTRAINT uk_programa_rutina_posicion UNIQUE (programa_id, posicion),
    CONSTRAINT fk_programa_rutina_programa FOREIGN KEY (programa_id) REFERENCES programas (id),
    CONSTRAINT fk_programa_rutina_rutina FOREIGN KEY (rutina_id) REFERENCES rutinas (id)
);

-- «Programa que sigue» un usuario. Seguirlo otra vez crea otra fila y otras copias.
CREATE TABLE programas_usuario
(
    id           INT AUTO_INCREMENT NOT NULL,
    usuario_id   INT      NOT NULL,
    programa_id  INT      NOT NULL,
    minutos      INT      NOT NULL,
    fecha_inicio DATETIME NOT NULL,
    CONSTRAINT pk_programas_usuario PRIMARY KEY (id),
    CONSTRAINT fk_programas_usuario_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_programas_usuario_programa FOREIGN KEY (programa_id) REFERENCES programas (id)
);

CREATE INDEX idx_programas_usuario_usuario ON programas_usuario (usuario_id);

-- Plantillas y copias. codigo solo lo llevan las plantillas (UNIQUE admite varios NULL).
ALTER TABLE rutinas
    ADD COLUMN codigo             VARCHAR(20) NULL,
    ADD COLUMN es_plantilla       TINYINT(1)  NOT NULL DEFAULT 0,
    ADD COLUMN plantilla_id       INT         NULL,
    ADD COLUMN programa_usuario_id INT        NULL,
    ADD CONSTRAINT uk_rutinas_codigo UNIQUE (codigo),
    ADD CONSTRAINT fk_rutinas_plantilla FOREIGN KEY (plantilla_id) REFERENCES rutinas (id),
    ADD CONSTRAINT fk_rutinas_programa_usuario FOREIGN KEY (programa_usuario_id) REFERENCES programas_usuario (id);

-- Cada ejercicio de una rutina, opcionales: rango, medida (GP-125), básico o extra, por
-- lado y la nota en inglés. repeticiones sigue siendo NOT NULL y lleva el máximo del
-- rango, para que una build vieja enseñe algo con sentido.
ALTER TABLE rutina_ejercicio
    ADD COLUMN repeticiones_min INT         NULL,
    ADD COLUMN repeticiones_max INT         NULL,
    ADD COLUMN medida           VARCHAR(15) NULL,
    ADD COLUMN tipo             VARCHAR(10) NULL,
    ADD COLUMN por_lado         VARCHAR(10) NULL,
    ADD COLUMN notas_en         TEXT        NULL;
