CREATE TABLE roles
(
    id     INT AUTO_INCREMENT NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id)
);

CREATE TABLE usuario_roles
(
    role_id    INT NOT NULL,
    usuario_id INT NOT NULL
);

ALTER TABLE roles
    ADD CONSTRAINT uc_roles_nombre UNIQUE (nombre);

ALTER TABLE usuario_roles
    ADD CONSTRAINT fk_usurol_on_role FOREIGN KEY (role_id) REFERENCES roles (id);

ALTER TABLE usuario_roles
    ADD CONSTRAINT fk_usurol_on_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);