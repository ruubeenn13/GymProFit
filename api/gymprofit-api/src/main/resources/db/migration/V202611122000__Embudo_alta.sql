-- GP-019 · El embudo del alta: cuánta gente llega a cada pantalla, por día.
--
-- Solo un recuento por día de Madrid y paso. A propósito no hay nada más: ni cuenta, ni
-- IP, ni móvil, ni hora. Una fila no dice de quién es porque nadie lo sabe: la app
-- avisa sin token y la API solo suma uno a la fila del día (DEC-064). Si alguna vez hace
-- falta una columna más, es otra decisión, no una migración.
CREATE TABLE alta_pasos (
    dia   DATE         NOT NULL,
    paso  VARCHAR(16)  NOT NULL,
    total INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (dia, paso)
);
