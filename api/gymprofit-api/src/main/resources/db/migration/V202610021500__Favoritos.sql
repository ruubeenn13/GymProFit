-- Lote 1.6.3 · Favoritos y su propuesta (GP-162, decisiones 2 y 6 del lienzo).
--
-- Un favorito es de una cuenta y de un alimento que esa cuenta puede ver (del catálogo
-- o suyo); eso lo comprueba la API al marcarlo. Borrar la cuenta o el alimento se lo
-- lleva: las dos claves ajenas borran en cascada (y el borrado de la cuenta lo hace
-- además a mano, para dejarlo en su log).
CREATE TABLE favoritos (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  INT      NOT NULL,
    alimento_id INT      NOT NULL,
    creado      DATETIME NOT NULL,
    CONSTRAINT fk_favoritos_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_favoritos_alimento FOREIGN KEY (alimento_id)
        REFERENCES alimentos (id) ON DELETE CASCADE,
    CONSTRAINT uq_favoritos_usuario_alimento UNIQUE (usuario_id, alimento_id)
);

-- Lo que ya no se propone a una cuenta como favorito, nunca más: lo que rechazó
-- (RECHAZADA) y lo que alguna vez fue su favorito (FAVORITO), aunque luego lo quitara.
-- La propuesta sale una sola vez por alimento.
CREATE TABLE propuestas_favorito (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  INT         NOT NULL,
    alimento_id INT         NOT NULL,
    motivo      VARCHAR(10) NOT NULL,
    creado      DATETIME    NOT NULL,
    CONSTRAINT fk_propuestas_favorito_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_propuestas_favorito_alimento FOREIGN KEY (alimento_id)
        REFERENCES alimentos (id) ON DELETE CASCADE,
    CONSTRAINT uq_propuestas_favorito_usuario_alimento UNIQUE (usuario_id, alimento_id)
);
