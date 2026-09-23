-- ============================================================================
-- Clave de idempotencia del guardado de una sesión.
--
-- EL PROBLEMA QUE RESUELVE
-- -----------------------
-- Guardar una sesión deja de ser «crea la sesión y luego, con suerte, sus
-- ejercicios» y pasa a ser una sola operación atómica. Eso obliga a que la app
-- pueda REINTENTAR cuando falla, y un reintento sin protección duplica
-- entrenamientos: con mala red la petición puede llegar al servidor, guardarse
-- entera, y perderse solo la respuesta. El usuario no ve confirmación, vuelve a
-- pulsar, y acaba con dos sesiones idénticas que además cuentan doble para la
-- racha y los logros.
--
-- La protección no puede vivir en el cliente: el cliente es justamente quien no
-- sabe si la primera llegó.
--
-- CÓMO
-- ----
-- El cliente genera una clave única por INTENTO DE GUARDADO (no por pulsación:
-- el reintento reusa la misma) y la manda con la sesión. El servidor la guarda
-- con restricción de unicidad; si le vuelve a llegar la misma, devuelve la
-- sesión que ya existe en vez de crear otra.
--
-- POR QUÉ UNA COLUMNA Y NO UNA TABLA APARTE
-- -----------------------------------------
-- Una tabla de claves obligaría a mantener sincronizadas dos filas y a decidir
-- qué hacer con las claves huérfanas. Aquí la clave vive en la propia sesión que
-- creó: si la sesión se borra, la clave se va con ella, que es exactamente lo
-- que se quiere —borrar una sesión y volver a guardarla es una operación nueva—.
--
-- POR QUÉ LA UNICIDAD ES (usuario_id, clave) Y NO SOLO LA CLAVE
-- ------------------------------------------------------------
-- Para que la búsqueda de una clave repetida no pueda devolver NUNCA la sesión
-- de otro usuario. Con un UUID la colisión entre dos personas es teórica, pero
-- «teórica» no es una garantía de aislamiento: con el par, es imposible por
-- construcción. Además dos usuarios pueden reutilizar la misma clave sin
-- estorbarse.
--
-- Las sesiones ya guardadas se quedan con la clave a NULL, y un índice único
-- admite tantos NULL como haga falta: no colisionan entre ellas ni bloquean
-- nada.
-- ============================================================================

ALTER TABLE sesiones_entrenamiento
    ADD COLUMN idempotencia_clave VARCHAR(64) NULL AFTER valoracion;

-- El índice hace las dos cosas: impide el duplicado y es por el que se busca
-- cuando llega un reintento.
CREATE UNIQUE INDEX uq_sesion_idempotencia
    ON sesiones_entrenamiento (usuario_id, idempotencia_clave);
