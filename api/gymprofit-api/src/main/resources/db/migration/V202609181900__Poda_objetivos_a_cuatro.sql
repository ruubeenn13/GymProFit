-- ============================================================================
-- Poda de objetivos: de 12 valores a 4.
--
-- El enum TipoObjetivo tenia 12 valores, de los que la app solo ofrecia 10 y
-- ninguno de los seis "no caloricos" (resistencia, flexibilidad, velocidad,
-- movilidad, reto y otro) cambiaba de verdad el plan nutricional: todos acababan
-- en calorias de mantenimiento con pequenos retoques de macros. Pedir al usuario
-- que elija entre diez tarjetas para llegar a cuatro resultados distintos es
-- ruido en la pantalla que mas condiciona el resto de la app.
--
-- Se quedan los cuatro que SI cambian el calculo:
--   PERDER_PESO          deficit
--   GANAR_MASA_MUSCULAR  superavit alto
--   MANTENER_PESO        mantenimiento
--   MEJORAR_FUERZA       superavit moderado con proteina muy alta
--
-- Los datos existentes NO se pierden: se reasignan al objetivo equivalente
-- ANTES de tocar la columna.
--
-- OJO, deriva de esquema detectada al preparar esta migracion:
--   desarrollo tenia objetivos_personales.tipo_objetivo como ENUM(12 valores),
--   pero PRODUCCION lo tiene como VARCHAR(50). Se converge en VARCHAR(50), que
--   es lo que ya hay en produccion, lo que tiene usuarios.objetivo y lo que
--   mapea @Enumerated(EnumType.STRING) en la entidad. La restriccion de valores
--   vive en Java (TipoObjetivo), que es por donde pasa toda escritura.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Reasignacion en usuarios.objetivo.
--
--    REDUCIR_GRASA_CORPORAL -> PERDER_PESO: son el mismo objetivo contado de dos
--      maneras, y tener los dos obligaba al usuario a adivinar la diferencia.
--    AUMENTAR_CALORIAS -> GANAR_MASA_MUSCULAR: ambos son superavit calorico.
--    El resto no expresa intencion calorica -> MANTENER_PESO.
-- ---------------------------------------------------------------------------
UPDATE usuarios
SET objetivo = 'PERDER_PESO'
WHERE objetivo = 'REDUCIR_GRASA_CORPORAL';

UPDATE usuarios
SET objetivo = 'GANAR_MASA_MUSCULAR'
WHERE objetivo = 'AUMENTAR_CALORIAS';

UPDATE usuarios
SET objetivo = 'MANTENER_PESO'
WHERE objetivo IN ('MEJORAR_RESISTENCIA', 'MEJORAR_FLEXIBILIDAD', 'MEJORAR_VELOCIDAD',
                   'MEJORAR_MOVILIDAD', 'COMPLETAR_RETO', 'OTRO');

-- ---------------------------------------------------------------------------
-- 2. La columna de objetivos_personales pasa a VARCHAR(50) en los dos entornos.
--    En produccion ya lo es, asi que alli el ALTER no cambia nada; en desarrollo
--    deja de ser un ENUM de MySQL y se acaba la deriva.
--
--    Va ANTES de reasignar los valores a proposito: mientras siga siendo un ENUM
--    no se puede escribir en esa columna un valor que no este en su lista, y el
--    UPDATE de abajo necesita poder hacerlo.
-- ---------------------------------------------------------------------------
ALTER TABLE objetivos_personales
    MODIFY COLUMN tipo_objetivo VARCHAR(50) NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. Misma reasignacion en objetivos_personales.tipo_objetivo.
-- ---------------------------------------------------------------------------
UPDATE objetivos_personales
SET tipo_objetivo = 'PERDER_PESO'
WHERE tipo_objetivo = 'REDUCIR_GRASA_CORPORAL';

UPDATE objetivos_personales
SET tipo_objetivo = 'GANAR_MASA_MUSCULAR'
WHERE tipo_objetivo = 'AUMENTAR_CALORIAS';

UPDATE objetivos_personales
SET tipo_objetivo = 'MANTENER_PESO'
WHERE tipo_objetivo IN ('MEJORAR_RESISTENCIA', 'MEJORAR_FLEXIBILIDAD', 'MEJORAR_VELOCIDAD',
                        'MEJORAR_MOVILIDAD', 'COMPLETAR_RETO', 'OTRO');
