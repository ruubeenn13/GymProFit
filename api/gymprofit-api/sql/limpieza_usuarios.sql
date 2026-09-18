-- ============================================================================
-- limpieza_usuarios.sql — Borra usuarios y TODO lo que cuelga de ellos.
--
-- Pensado para dejar la base limpia de cuentas de prueba antes de publicar.
-- Las claves ajenas hacia `usuarios` son casi todas RESTRICT, así que el orden
-- de borrado importa: primero los nietos, luego los hijos y al final el usuario.
-- (device_tokens, fotos_perfil y refresh_tokens son CASCADE y se van solos;
-- `alimentos` es SET NULL, así que los alimentos creados por un usuario borrado
-- se quedan en el catálogo sin dueño, que es lo que queremos.)
--
-- CÓMO USARLO
--   1. Edita la lista de usuarios A CONSERVAR de abajo.
--   2. Haz una copia de seguridad ANTES (ver el bloque de comentarios al final).
--   3. Ejecuta el script entero. Va dentro de una transacción: si algo falla,
--      no se borra nada.
--   4. Revisa el recuento final que imprime y confirma con COMMIT.
--
-- Este script NO toca el catálogo (ejercicios, alimentos, logros) ni el esquema.
-- ============================================================================

START TRANSACTION;

-- ---------------------------------------------------------------------------
-- Usuarios que SOBREVIVEN. Todo lo demás se borra.
-- ---------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS conservar;
CREATE TEMPORARY TABLE conservar (username VARCHAR(255) PRIMARY KEY);

-- ---> EDITA ESTA LISTA SEGÚN EL ENTORNO <---
--
-- PRODUCCIÓN: admin (si se borra, DataInitializer lo recrea al reiniciar con la
--             contraseña de fábrica Admin1234), guest (necesario para "entrar
--             como invitado") y la cuenta de servicio del bot de Discord.
INSERT INTO conservar (username) VALUES ('admin'), ('guest'), ('gymprobot');
--
-- DESARROLLO: solo la cuenta de pruebas.
-- INSERT INTO conservar (username) VALUES ('prueba');

-- Identificadores a borrar, calculados una sola vez.
DROP TEMPORARY TABLE IF EXISTS borrar;
CREATE TEMPORARY TABLE borrar (id BIGINT PRIMARY KEY);
INSERT INTO borrar (id)
SELECT u.id FROM usuarios u
WHERE u.username NOT IN (SELECT username FROM conservar);

-- Qué se va a borrar, para verlo antes de confirmar.
SELECT COUNT(*) AS usuarios_a_borrar FROM borrar;
SELECT u.id, u.username, u.email FROM usuarios u JOIN borrar b ON b.id = u.id ORDER BY u.id;

-- ---------------------------------------------------------------------------
-- Nietos: dependen de comidas, sesiones y rutinas del usuario.
-- ---------------------------------------------------------------------------
DELETE ac FROM alimentos_comida ac
    JOIN comidas c ON c.id = ac.comida_id
    JOIN borrar b ON b.id = c.usuario_id;

DELETE er FROM ejercicios_realizados er
    JOIN sesiones_entrenamiento s ON s.id = er.sesion_id
    JOIN borrar b ON b.id = s.usuario_id;

DELETE re FROM rutina_ejercicio re
    JOIN rutinas r ON r.id = re.rutina_id
    JOIN borrar b ON b.id = r.usuario_id;

-- ---------------------------------------------------------------------------
-- Hijos directos del usuario.
-- ---------------------------------------------------------------------------
DELETE c  FROM comidas c                JOIN borrar b ON b.id = c.usuario_id;
DELETE s  FROM sesiones_entrenamiento s JOIN borrar b ON b.id = s.usuario_id;
DELETE r  FROM rutinas r                JOIN borrar b ON b.id = r.usuario_id;
DELETE pe FROM progreso_ejercicios pe   JOIN borrar b ON b.id = pe.usuario_id;
DELETE m  FROM mediciones_corporales m  JOIN borrar b ON b.id = m.usuario_id;
DELETE o  FROM objetivos_personales o   JOIN borrar b ON b.id = o.usuario_id;
DELETE n  FROM notificaciones n         JOIN borrar b ON b.id = n.usuario_id;
DELETE ul FROM usuario_logros ul        JOIN borrar b ON b.id = ul.usuario_id;
DELETE ur FROM usuario_roles ur         JOIN borrar b ON b.id = ur.usuario_id;

-- ---------------------------------------------------------------------------
-- El usuario. Arrastra en cascada device_tokens, fotos_perfil y refresh_tokens.
-- ---------------------------------------------------------------------------
DELETE u FROM usuarios u JOIN borrar b ON b.id = u.id;

-- ---------------------------------------------------------------------------
-- Comprobación: quién queda y con cuántos datos.
-- ---------------------------------------------------------------------------
SELECT u.id, u.username, u.email,
       (SELECT COUNT(*) FROM rutinas r WHERE r.usuario_id = u.id)                AS rutinas,
       (SELECT COUNT(*) FROM sesiones_entrenamiento s WHERE s.usuario_id = u.id) AS sesiones,
       (SELECT COUNT(*) FROM comidas c WHERE c.usuario_id = u.id)                AS comidas,
       (SELECT COUNT(*) FROM mediciones_corporales m WHERE m.usuario_id = u.id)  AS mediciones
FROM usuarios u ORDER BY u.id;

-- Huérfanos que no deberían existir tras la limpieza (todo debe dar 0).
-- Ojo: las rutinas del sistema (es_predefinida = 1) tienen usuario_id NULL a
-- propósito y NO son huérfanas; por eso se excluyen los nulos.
SELECT
    (SELECT COUNT(*) FROM comidas c                LEFT JOIN usuarios u ON u.id = c.usuario_id  WHERE u.id IS NULL AND c.usuario_id IS NOT NULL) AS comidas_huerfanas,
    (SELECT COUNT(*) FROM sesiones_entrenamiento s LEFT JOIN usuarios u ON u.id = s.usuario_id  WHERE u.id IS NULL AND s.usuario_id IS NOT NULL) AS sesiones_huerfanas,
    (SELECT COUNT(*) FROM rutinas r                LEFT JOIN usuarios u ON u.id = r.usuario_id  WHERE u.id IS NULL AND r.usuario_id IS NOT NULL) AS rutinas_huerfanas,
    (SELECT COUNT(*) FROM mediciones_corporales m  LEFT JOIN usuarios u ON u.id = m.usuario_id  WHERE u.id IS NULL AND m.usuario_id IS NOT NULL) AS mediciones_huerfanas;

-- Las rutinas predefinidas deben seguir ahí (el catálogo no se toca).
SELECT COUNT(*) AS rutinas_predefinidas FROM rutinas WHERE usuario_id IS NULL;

-- Revisa los recuentos de arriba y confirma a mano:
--   COMMIT;    -- para aplicar
--   ROLLBACK;  -- para deshacer y no borrar nada

-- ============================================================================
-- COPIA DE SEGURIDAD PREVIA (obligatoria en producción)
--
--   mariadb-dump -h <host> -P <puerto> -u <usuario> -p --ssl \
--       --single-transaction --routines --add-drop-table <base> \
--       > copia_antes_de_limpieza.sql
--
-- Restaurar si algo sale mal:
--   mariadb -h <host> -P <puerto> -u <usuario> -p --ssl <base> \
--       < copia_antes_de_limpieza.sql
-- ============================================================================
