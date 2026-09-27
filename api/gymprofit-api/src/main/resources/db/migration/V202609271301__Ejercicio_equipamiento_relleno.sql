-- ============================================================
-- Relleno de ejercicios.equipamiento y ejercicios.nombre_revisado (GP-085).
--
-- equipamiento sale de equipo_necesario (y de su versión inglesa, por si acaso). Un
-- ejercicio con varios aparatos se queda con el primero de este orden: barra,
-- mancuernas, kettlebell, polea, máquina, banda, peso corporal; lo que no casa con
-- ninguno (banco suelto, balón medicinal, fitball, rodillo) es OTRO. La barra de
-- dominadas no es una barra: se reescribe antes para que cuente como peso corporal.
-- Polea va antes que máquina porque la importación llama «Cable machine» a la polea.
--
-- nombre_revisado: true donde el nombre en español ya es distinto del inglés (con la
-- colación de la tabla: sin distinguir mayúsculas ni tildes), o donde no hay inglés
-- con el que compararlo. <=> es la igualdad que trata NULL como un valor.
-- ============================================================
-- El texto con que se compara es, en todas las ramas, este (repetido porque MySQL no
-- deja reutilizar un alias en el SET ni leer la misma tabla en un derivado del UPDATE):
--   REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas')
UPDATE ejercicios
SET equipamiento = CASE
        WHEN REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%barra%' OR REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%barbell%'
            THEN 'BARRA'
        WHEN REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%mancuerna%' OR REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%dumbbell%'
            THEN 'MANCUERNAS'
        WHEN REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%kettlebell%'
            THEN 'KETTLEBELL'
        WHEN REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%polea%' OR REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%cable%'
            THEN 'POLEA'
        WHEN REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%quina%' OR REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%machine%' OR REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%cinta de correr%'
            THEN 'MAQUINA'
        WHEN REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%banda%' OR REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%band%'
            THEN 'BANDA'
        WHEN REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%sin equipo%' OR REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%bodyweight%' OR REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%no equipment%' OR REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%dominadas%' OR REPLACE(REPLACE(REPLACE(LOWER(CONCAT_WS(' | ', equipo_necesario, equipo_necesario_en)), 'barra de dominadas', 'dominadas'), 'barra dominadas', 'dominadas'), 'pull-up bar', 'dominadas') LIKE '%esterilla%'
            THEN 'PESO_CORPORAL'
        ELSE 'OTRO'
    END;

UPDATE ejercicios SET nombre_revisado = NOT (nombre <=> nombre_en);
