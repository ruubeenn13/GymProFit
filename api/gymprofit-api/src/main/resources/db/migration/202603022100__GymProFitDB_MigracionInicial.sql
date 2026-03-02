CREATE TABLE alimentos
(
    id             INT AUTO_INCREMENT   NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    categoria      VARCHAR(50) NULL,
    calorias       INT          NOT NULL,
    proteinas      INT NULL,
    carbohidratos  DECIMAL(5, 2) NULL,
    grasas         DECIMAL(5, 2) NULL,
    fibra          DECIMAL(5, 2) NULL,
    porcion_gramos INT NULL,
    descripcion    TEXT NULL,
    activo         TINYINT(1) DEFAULT 1 NULL,
    CONSTRAINT pk_alimentos PRIMARY KEY (id)
);

CREATE TABLE alimentos_comida
(
    id               INT AUTO_INCREMENT NOT NULL,
    cantidad_gramos  DECIMAL(6, 2) NOT NULL,
    calorias_totales INT NULL,
    comida_id        INT           NOT NULL,
    alimento_id      INT           NOT NULL,
    CONSTRAINT pk_alimentos_comida PRIMARY KEY (id)
);

CREATE TABLE comidas
(
    id                  INT AUTO_INCREMENT NOT NULL,
    fecha               datetime NULL,
    tipo_comida         VARCHAR(255) NOT NULL,
    total_calorias      INT NULL,
    total_proteinas     DECIMAL(5, 2) NULL,
    total_carbohidratos DECIMAL(5, 2) NULL,
    total_grasas        DECIMAL(5, 2) NULL,
    notas               TEXT NULL,
    usuario_id          INT          NOT NULL,
    CONSTRAINT pk_comidas PRIMARY KEY (id)
);

CREATE TABLE ejercicios
(
    id                INT AUTO_INCREMENT   NOT NULL,
    nombre            VARCHAR(100) NOT NULL,
    descripcion       TEXT NULL,
    grupo_muscular    VARCHAR(255) NOT NULL,
    dificultad        VARCHAR(255) NOT NULL,
    imagen_url        VARCHAR(255) NULL,
    instrucciones     TEXT NULL,
    calorias_quemadas INT NULL,
    equipo_necesario  VARCHAR(255) NULL,
    activo            TINYINT(1) DEFAULT 1 NULL,
    CONSTRAINT pk_ejercicios PRIMARY KEY (id)
);

CREATE TABLE ejercicios_realizados
(
    id                   INT AUTO_INCREMENT NOT NULL,
    series_completadas   INT NULL,
    repeticiones_rutinas INT NULL,
    peso_usado           DECIMAL(5, 2) NULL,
    tiempo_segundos      INT NULL,
    notas                TEXT NULL,
    sesion_id            INT NOT NULL,
    ejercicio_id         INT NOT NULL,
    CONSTRAINT pk_ejercicios_realizados PRIMARY KEY (id)
);

CREATE TABLE mediciones_corporales
(
    id             INT AUTO_INCREMENT NOT NULL,
    fecha          datetime NULL,
    peso           DECIMAL(5, 2) NOT NULL,
    altura         DECIMAL(3, 2) NULL,
    imc            DECIMAL(4, 2) NULL,
    grasa_corporal DECIMAL(4, 2) NULL,
    masa_muscular  DECIMAL(4, 2) NULL,
    cintura        DECIMAL(5, 2) NULL,
    pecho          DECIMAL(5, 2) NULL,
    brazos         DECIMAL(5, 2) NULL,
    piernas        DECIMAL(5, 2) NULL,
    notas          TEXT NULL,
    usuario_id     INT           NOT NULL,
    CONSTRAINT pk_mediciones_corporales PRIMARY KEY (id)
);

CREATE TABLE notificaciones
(
    id               INT AUTO_INCREMENT   NOT NULL,
    titulo           VARCHAR(100) NOT NULL,
    mensaje          TEXT         NOT NULL,
    tipo             VARCHAR(255) NOT NULL,
    fecha_creacion   datetime NULL,
    fecha_programada datetime NULL,
    leida            TINYINT(1) DEFAULT 0 NULL,
    usuario_id       INT          NOT NULL,
    CONSTRAINT pk_notificaciones PRIMARY KEY (id)
);

CREATE TABLE objetivos_personales
(
    id               INT AUTO_INCREMENT   NOT NULL,
    tipo_objetivo    VARCHAR(50)    NOT NULL,
    descripcion      TEXT           NOT NULL,
    valor_actual     DECIMAL(10, 2) NULL,
    valor_objetivo   DECIMAL(10, 2) NOT NULL,
    unidad           VARCHAR(20) NULL,
    fecha_inicio     date           NOT NULL,
    fecha_limite     date NULL,
    completado       TINYINT(1) DEFAULT 0 NULL,
    fecha_completado datetime NULL,
    usuario_id       INT            NOT NULL,
    CONSTRAINT pk_objetivos_personales PRIMARY KEY (id)
);

CREATE TABLE progreso_ejercicios
(
    id                    INT AUTO_INCREMENT NOT NULL,
    fecha                 datetime NULL,
    mejor_peso            DECIMAL(5, 2) NULL,
    mejor_repeticiones    DECIMAL(5, 2) NULL,
    mejor_tiempo_segundos INT NULL,
    notas                 TEXT NULL,
    usuario_id            INT NOT NULL,
    ejercicio_id          INT NOT NULL,
    CONSTRAINT pk_progreso_ejercicios PRIMARY KEY (id)
);

CREATE TABLE rutina_ejercicio
(
    id               INT AUTO_INCREMENT NOT NULL,
    series           INT DEFAULT 3  NOT NULL,
    repeticiones     INT DEFAULT 10 NOT NULL,
    peso_recomendado DECIMAL(5, 2) NULL,
    tiempo_descanso  INT NULL,
    orden            INT DEFAULT 1  NOT NULL,
    notas            TEXT NULL,
    rutina_id        INT            NOT NULL,
    ejercicio_id     INT            NOT NULL,
    CONSTRAINT pk_rutina_ejercicio PRIMARY KEY (id)
);

CREATE TABLE rutinas
(
    id               INT AUTO_INCREMENT   NOT NULL,
    nombre           VARCHAR(100) NOT NULL,
    descripcion      TEXT NULL,
    duracion_minutos INT NULL,
    nivel            VARCHAR(255) NOT NULL,
    es_predefinida   TINYINT(1) DEFAULT 0 NULL,
    categoria        VARCHAR(50) NULL,
    dias_semana      VARCHAR(100) NULL,
    fecha_creacion   datetime NULL,
    activa           TINYINT(1) DEFAULT 1 NULL,
    usuario_id       INT NULL,
    CONSTRAINT pk_rutinas PRIMARY KEY (id)
);

CREATE TABLE sesiones_entrenamiento
(
    id                INT AUTO_INCREMENT   NOT NULL,
    fecha_inicio      datetime NOT NULL,
    fecha_fin         datetime NOT NULL,
    duracion_minutos  INT NULL,
    calorias_quemadas INT NULL,
    notas             TEXT NULL,
    completada        TINYINT(1) DEFAULT 0 NULL,
    usuario_id        INT      NOT NULL,
    rutina_id         INT NULL,
    CONSTRAINT pk_sesiones_entrenamiento PRIMARY KEY (id)
);

CREATE TABLE usuarios
(
    id                INT AUTO_INCREMENT   NOT NULL,
    username          VARCHAR(50)  NOT NULL,
    password          VARCHAR(255) NOT NULL,
    email             VARCHAR(100) NOT NULL,
    peso              DECIMAL(5, 2) NULL,
    altura            DECIMAL(3, 2) NULL,
    edad              INT NULL,
    nivel_experiencia VARCHAR(255) NULL,
    objetivo          VARCHAR(100) NULL,
    fecha_registro    datetime NULL,
    activo            TINYINT(1) DEFAULT 1 NULL,
    CONSTRAINT pk_usuarios PRIMARY KEY (id)
);

ALTER TABLE usuarios
    ADD CONSTRAINT uc_usuarios_email UNIQUE (email);

ALTER TABLE usuarios
    ADD CONSTRAINT uc_usuarios_username UNIQUE (username);

ALTER TABLE alimentos_comida
    ADD CONSTRAINT FK_ALIMENTOS_COMIDA_ON_ALIMENTO FOREIGN KEY (alimento_id) REFERENCES alimentos (id);

ALTER TABLE alimentos_comida
    ADD CONSTRAINT FK_ALIMENTOS_COMIDA_ON_COMIDA FOREIGN KEY (comida_id) REFERENCES comidas (id);

ALTER TABLE comidas
    ADD CONSTRAINT FK_COMIDAS_ON_USUARIO FOREIGN KEY (usuario_id) REFERENCES usuarios (id);

ALTER TABLE ejercicios_realizados
    ADD CONSTRAINT FK_EJERCICIOS_REALIZADOS_ON_EJERCICIO FOREIGN KEY (ejercicio_id) REFERENCES ejercicios (id);

ALTER TABLE ejercicios_realizados
    ADD CONSTRAINT FK_EJERCICIOS_REALIZADOS_ON_SESION FOREIGN KEY (sesion_id) REFERENCES sesiones_entrenamiento (id);

ALTER TABLE mediciones_corporales
    ADD CONSTRAINT FK_MEDICIONES_CORPORALES_ON_USUARIO FOREIGN KEY (usuario_id) REFERENCES usuarios (id);

ALTER TABLE notificaciones
    ADD CONSTRAINT FK_NOTIFICACIONES_ON_USUARIO FOREIGN KEY (usuario_id) REFERENCES usuarios (id);

ALTER TABLE objetivos_personales
    ADD CONSTRAINT FK_OBJETIVOS_PERSONALES_ON_USUARIO FOREIGN KEY (usuario_id) REFERENCES usuarios (id);

ALTER TABLE progreso_ejercicios
    ADD CONSTRAINT FK_PROGRESO_EJERCICIOS_ON_EJERCICIO FOREIGN KEY (ejercicio_id) REFERENCES ejercicios (id);

ALTER TABLE progreso_ejercicios
    ADD CONSTRAINT FK_PROGRESO_EJERCICIOS_ON_USUARIO FOREIGN KEY (usuario_id) REFERENCES usuarios (id);

ALTER TABLE rutinas
    ADD CONSTRAINT FK_RUTINAS_ON_USUARIO FOREIGN KEY (usuario_id) REFERENCES usuarios (id);

ALTER TABLE rutina_ejercicio
    ADD CONSTRAINT FK_RUTINA_EJERCICIO_ON_EJERCICIO FOREIGN KEY (ejercicio_id) REFERENCES ejercicios (id);

ALTER TABLE rutina_ejercicio
    ADD CONSTRAINT FK_RUTINA_EJERCICIO_ON_RUTINA FOREIGN KEY (rutina_id) REFERENCES rutinas (id);

ALTER TABLE sesiones_entrenamiento
    ADD CONSTRAINT FK_SESIONES_ENTRENAMIENTO_ON_RUTINA FOREIGN KEY (rutina_id) REFERENCES rutinas (id);

ALTER TABLE sesiones_entrenamiento
    ADD CONSTRAINT FK_SESIONES_ENTRENAMIENTO_ON_USUARIO FOREIGN KEY (usuario_id) REFERENCES usuarios (id);