-- ============================================================================
-- La valoración de una sesión pasa a ser un DATO, no una línea de texto.
--
-- Hasta ahora la app formateaba la puntuación de las estrellas con un recurso
-- de idioma y la metía como PRIMERA LÍNEA de `notas`, delante de lo que hubiera
-- escrito el usuario. Tres problemas, y ninguno es cosmético:
--
--   · No se puede consultar. «Media de valoración», «sesiones de 5 estrellas» o
--     cualquier gráfica de sensación exigen parsear texto libre.
--   · Se queda congelada en el idioma del momento. Una sesión guardada en
--     español dice «Valoración: 3/5» para siempre, aunque la app se ponga en
--     inglés; y las de antes dicen «⭐ 3/5», que ya ni siquiera es el formato.
--   · La app escribe dentro del texto del usuario. Sus notas no son suyas.
--
-- Se añade la columna y se RECUPERA lo ya guardado: se extrae la valoración de
-- las notas, se escribe en la columna y se quita esa línea dejando intacto todo
-- lo demás.
--
-- QUÉ FORMATOS SE RECONOCEN, Y POR QUÉ ESOS
-- -----------------------------------------
-- Salen del historial de git de `sesiones_valoracion_fmt` en values/ y
-- values-en/, no de suponer. Han existido exactamente tres literales:
--
--   ⭐ %d/5          ES y EN, desde 6ca53c3 (2026-05-22) hasta 5e0dee3 (2026-09-23)
--   Valoración: %d/5 ES, desde 40c686b (2026-09-23)
--   Rating: %d/5     EN, desde 40c686b (2026-09-23)
--
-- El emoji salió de las cadenas en 40c686b (GP-061) y el texto se hizo
-- explícito porque esto se guarda, no se lee en pantalla.
--
-- POR QUÉ LA COINCIDENCIA ES TAN ESTRICTA
-- ---------------------------------------
-- `RegistrarSesionActivity` construye la nota como `valoracion + "\n" + notas`,
-- así que el patrón SIEMPRE ocupa la primera línea entera. Aquí se exige justo
-- eso: que la nota empiece por el literal exacto y que lo siguiente sea el final
-- de la nota o un salto de línea. Un «me salió un 4/5» escrito por el usuario en
-- mitad de una frase NO se toca, y tampoco «4/5» al principio de una línea si no
-- lleva delante uno de los tres prefijos.
--
-- Y se compara con `utf8mb4_bin` a propósito: la colación por defecto de la base
-- ignora mayúsculas Y acentos, así que sin esto «valoracion: 3/5» escrito a mano
-- por el usuario casaría y le borraríamos su línea.
--
-- MEDIDO CONTRA LA BASE LOCAL ANTES DE DEJARLA LISTA (2026-09-23)
-- --------------------------------------------------------------
--   8 sesiones en total, las 8 con notas.
--   3 casan con «⭐ N/5», 2 con «Valoración: N/5», 0 con «Rating: N/5».
--   5 sesiones reciben valoración; 5 notas se modifican y las 5 quedan vacías
--   (la valoración era su única línea), así que pasan a NULL.
--   3 notas de siembra («Sesion de siembra») no se tocan.
--   0 notas con texto del usuario detrás de la valoración.
--
-- Una valoración de 0 estrellas NO se reconoce: la columna admite 1..5 y un
-- «⭐ 0/5» no es una valoración válida, así que esa línea se queda donde está en
-- vez de inventar un valor. En la base local no hay ninguna.
--
-- Y ensayada también sobre casos escritos a mano, porque en la base local no hay
-- ninguna nota con texto del usuario detrás de la valoración y esa rama se
-- quedaría sin comprobar ([salto] es un salto de línea real):
--
--   ⭐ 3/5                                    → 3, notas NULL
--   ⭐ 4/5 [salto] Buenas sensaciones          → 4, notas «Buenas sensaciones»
--   Valoración: 5/5 [salto] linea1 [salto] linea2 → 5, notas con las DOS líneas
--   Rating: 1/5 [salto] felt weak today         → 1, notas «felt weak today»
--   me salió un 4/5, muy bien                 → no casa
--   Valoracion: 3/5   (sin tilde)             → no casa
--   valoración: 3/5   (minúscula)             → no casa
--   ⭐ 0/5  /  ⭐ 6/5                        → no casan
--   ⭐ 3/5 y encima lloviendo                 → no casa (no ocupa la línea entera)
--   Hoy flojo [salto] Valoración: 2/5         → no casa (no es la PRIMERA línea)
-- ============================================================================

-- INT y no TINYINT aunque quepa de sobra: el campo de la entidad es Integer y
-- `ddl-auto=validate` rechaza el arranque si los tipos no casan. El rango real lo
-- pone el CHECK de abajo, que es donde tiene que estar.
ALTER TABLE sesiones_entrenamiento
    ADD COLUMN valoracion INT NULL AFTER duracion_minutos;

ALTER TABLE sesiones_entrenamiento
    ADD CONSTRAINT chk_sesion_valoracion
        CHECK (valoracion IS NULL OR valoracion BETWEEN 1 AND 5);

-- Un solo UPDATE: la columna y las notas se tocan a la vez, de modo que no puede
-- quedar una sesión con la valoración extraída y la línea todavía en las notas.
-- `IFNULL` porque REGEXP_SUBSTR devuelve NULL en MySQL y '' en MariaDB cuando no
-- hay coincidencia, y esta migración corre en las dos.
UPDATE sesiones_entrenamiento
SET
    valoracion = CAST(
        SUBSTRING(
            REGEXP_SUBSTR(notas COLLATE utf8mb4_bin, '^(⭐|Valoración:|Rating:) [1-5]/5'),
            -3, 1
        ) AS UNSIGNED
    ),
    notas = CASE
        WHEN CHAR_LENGTH(notas) = CHAR_LENGTH(
                REGEXP_SUBSTR(notas COLLATE utf8mb4_bin, '^(⭐|Valoración:|Rating:) [1-5]/5'))
            THEN NULL
        ELSE SUBSTRING(
                notas,
                CHAR_LENGTH(REGEXP_SUBSTR(notas COLLATE utf8mb4_bin, '^(⭐|Valoración:|Rating:) [1-5]/5')) + 2
            )
    END
WHERE
    IFNULL(REGEXP_SUBSTR(notas COLLATE utf8mb4_bin, '^(⭐|Valoración:|Rating:) [1-5]/5'), '') <> ''
    AND (
        -- La valoración era la nota entera…
        CHAR_LENGTH(notas) = CHAR_LENGTH(
            REGEXP_SUBSTR(notas COLLATE utf8mb4_bin, '^(⭐|Valoración:|Rating:) [1-5]/5'))
        -- …o venía seguida de un salto de línea y del texto del usuario.
        OR SUBSTRING(
               notas,
               CHAR_LENGTH(REGEXP_SUBSTR(notas COLLATE utf8mb4_bin, '^(⭐|Valoración:|Rating:) [1-5]/5')) + 1,
               1
           ) = CHAR(10 USING utf8mb4)
    );
