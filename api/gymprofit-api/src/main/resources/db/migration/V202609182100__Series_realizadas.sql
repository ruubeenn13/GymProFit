-- ============================================================================
-- Registro POR SERIE de un ejercicio dentro de una sesión.
--
-- Hasta ahora `ejercicios_realizados` guardaba UNA fila por ejercicio con un
-- solo `peso_usado`. Si alguien hacía 4x8 subiendo carga (60, 65, 70, 70 kg) no
-- había dónde meterlo: tenía que elegir un número y mentir.
--
-- Eso deja sin base el producto entero. Sin dato por serie no hay progresión de
-- carga, no hay récords personales, no hay volumen levantado y las gráficas de
-- progreso no pueden decir la verdad. Es lo que separa a esta app de Hevy o
-- Strong, que existen justamente para eso.
--
-- Se añade una tabla HIJA en vez de tocar la existente: las sesiones ya
-- guardadas siguen siendo válidas y simplemente no tienen series detalladas.
-- `ejercicios_realizados.peso_usado` y `series_completadas` se conservan como
-- resumen, y a partir de ahora los calcula el servidor desde las series.
-- ============================================================================

CREATE TABLE series_realizadas (
    id                     INT AUTO_INCREMENT PRIMARY KEY,

    -- Ejercicio de la sesión al que pertenece esta serie.
    ejercicio_realizado_id INT NOT NULL,

    -- Orden dentro del ejercicio, empezando en 1. Es lo que el usuario ve como
    -- "serie 1, serie 2…", así que se guarda y no se deduce del id.
    numero                 INT NOT NULL,

    -- Repeticiones REALES de esta serie, que no tienen por qué coincidir con las
    -- que pedía la rutina: fallar la última serie es información, no un error.
    repeticiones           INT NOT NULL,

    -- Peso de esta serie. Nulo en ejercicios de peso corporal.
    peso                   DECIMAL(5,2) NULL,

    -- Si el usuario llegó a marcarla. Una serie planificada y no completada
    -- también es un dato: distingue "no la hice" de "no la apunté".
    completada             BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_serie_ejercicio_realizado
        FOREIGN KEY (ejercicio_realizado_id)
        REFERENCES ejercicios_realizados (id)
        ON DELETE CASCADE,

    -- Dos series con el mismo número dentro del mismo ejercicio no tienen
    -- sentido, y además protege de un doble envío.
    CONSTRAINT uq_serie_por_ejercicio UNIQUE (ejercicio_realizado_id, numero)
);

-- El acceso natural es "dame las series de este ejercicio, en orden".
CREATE INDEX idx_series_ejercicio ON series_realizadas (ejercicio_realizado_id, numero);
