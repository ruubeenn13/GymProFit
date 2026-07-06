-- ============================================================
-- V202607061500__Add_catalogo_i18n.sql
-- Internacionalización del catálogo (ejercicios, alimentos, logros y
-- rutinas predefinidas): añade columnas *_en NULLABLE con la traducción
-- al inglés de los textos ES existentes. La API resuelve el idioma por
-- request (header Accept-Language) en los mappers; si la columna EN es
-- NULL se sirve el texto ES (fallback).
-- Los UPDATE van por id + texto/tipo original: si la fila no existe o no
-- coincide (BD vacía, CI, datos modificados) son no-op seguros.
-- ============================================================

-- ── EJERCICIOS: columnas EN (mismos tipos que las columnas ES) ───────────────
ALTER TABLE ejercicios
    ADD COLUMN nombre_en           VARCHAR(100) NULL,
    ADD COLUMN descripcion_en      TEXT         NULL,
    ADD COLUMN instrucciones_en    TEXT         NULL,
    ADD COLUMN equipo_necesario_en VARCHAR(255) NULL;

-- ── ALIMENTOS: columnas EN ────────────────────────────────────────────────────
ALTER TABLE alimentos
    ADD COLUMN nombre_en      VARCHAR(100) NULL,
    ADD COLUMN categoria_en   VARCHAR(50)  NULL,
    ADD COLUMN descripcion_en TEXT         NULL;

-- ── LOGROS: columnas EN ───────────────────────────────────────────────────────
ALTER TABLE logros
    ADD COLUMN nombre_en      VARCHAR(100) NULL,
    ADD COLUMN descripcion_en TEXT         NULL;

-- ── RUTINAS: columnas EN ──────────────────────────────────────────────────────
ALTER TABLE rutinas
    ADD COLUMN nombre_en      VARCHAR(100) NULL,
    ADD COLUMN descripcion_en TEXT         NULL,
    ADD COLUMN categoria_en   VARCHAR(50)  NULL;

-- ── TRADUCCIONES: EJERCICIOS del seed (ids 1-10) ──────────────────────────────
-- El filtro extra por nombre evita traducir filas distintas si los ids no
-- corresponden al seed original.
UPDATE ejercicios SET
    nombre_en           = 'Bench press',
    descripcion_en      = 'Compound barbell chest exercise',
    equipo_necesario_en = 'Barbell and bench',
    instrucciones_en    = '1. Lie on your back on a flat bench with your feet flat on the floor.
2. Grip the bar with both hands slightly wider than shoulder width.
3. Unrack the bar and position it over your lower chest.
4. Lower the bar slowly and under control until it lightly touches your chest.
5. Press the bar back up explosively until your elbows are fully extended.
6. Keep your shoulder blades retracted, your core braced and your feet planted throughout the movement.'
WHERE id = 1 AND nombre = 'Press de banca';

UPDATE ejercicios SET
    nombre_en           = 'Squat',
    descripcion_en      = 'Fundamental exercise for quadriceps and glutes',
    equipo_necesario_en = 'Barbell'
WHERE id = 2 AND nombre = 'Sentadilla';

UPDATE ejercicios SET
    nombre_en           = 'Deadlift',
    descripcion_en      = 'Barbell posterior chain exercise',
    equipo_necesario_en = 'Barbell'
WHERE id = 3 AND nombre = 'Peso muerto';

UPDATE ejercicios SET
    nombre_en           = 'Pull-ups',
    descripcion_en      = 'Bodyweight pull on a fixed bar',
    equipo_necesario_en = 'Pull-up bar'
WHERE id = 4 AND nombre = 'Dominadas';

UPDATE ejercicios SET
    nombre_en           = 'Overhead press',
    descripcion_en      = 'Standing barbell shoulder press',
    equipo_necesario_en = 'Barbell'
WHERE id = 5 AND nombre = 'Press militar';

UPDATE ejercicios SET
    nombre_en           = 'Biceps curl',
    descripcion_en      = 'Elbow flexion with dumbbells',
    equipo_necesario_en = 'Dumbbells'
WHERE id = 6 AND nombre = 'Curl de bíceps';

UPDATE ejercicios SET
    nombre_en           = 'Triceps pushdown',
    descripcion_en      = 'Elbow extension on a high cable pulley',
    equipo_necesario_en = 'Cable machine'
WHERE id = 7 AND nombre = 'Tríceps en polea';

UPDATE ejercicios SET
    nombre_en           = 'Plank',
    descripcion_en      = 'Isometric core exercise',
    equipo_necesario_en = 'No equipment'
WHERE id = 8 AND nombre = 'Plancha';

UPDATE ejercicios SET
    nombre_en           = 'Treadmill running',
    descripcion_en      = 'Moderate-pace treadmill cardio',
    equipo_necesario_en = 'Treadmill'
WHERE id = 9 AND nombre = 'Carrera en cinta';

UPDATE ejercicios SET
    nombre_en           = 'Burpees',
    descripcion_en      = 'High-intensity full-body exercise',
    equipo_necesario_en = 'No equipment'
WHERE id = 10 AND nombre = 'Burpees';

-- ── TRADUCCIONES: ALIMENTOS del seed (ids 1-10) ───────────────────────────────
-- categoria_en traduce las categorías vigentes tras V202605262000
-- (Carnes y aves, Huevos, Cereales y pan, Aceites y grasas...).
UPDATE alimentos SET
    nombre_en      = 'Chicken breast',
    categoria_en   = 'Meat & poultry',
    descripcion_en = 'Grilled chicken breast'
WHERE id = 1 AND nombre = 'Pechuga de pollo';

UPDATE alimentos SET
    nombre_en      = 'White rice',
    categoria_en   = 'Cereals & bread',
    descripcion_en = 'Cooked white rice'
WHERE id = 2 AND nombre = 'Arroz blanco';

UPDATE alimentos SET
    nombre_en      = 'Whole egg',
    categoria_en   = 'Eggs',
    descripcion_en = 'Whole boiled egg'
WHERE id = 3 AND nombre = 'Huevo entero';

UPDATE alimentos SET
    nombre_en      = 'Oats',
    categoria_en   = 'Cereals & bread',
    descripcion_en = 'Rolled oats'
WHERE id = 4 AND nombre = 'Avena';

UPDATE alimentos SET
    nombre_en      = 'Banana',
    categoria_en   = 'Fruits',
    descripcion_en = 'Ripe banana'
WHERE id = 5 AND nombre = 'Plátano';

UPDATE alimentos SET
    nombre_en      = 'Skimmed milk',
    categoria_en   = 'Dairy',
    descripcion_en = '0% fat skimmed milk'
WHERE id = 6 AND nombre = 'Leche desnatada';

UPDATE alimentos SET
    nombre_en      = 'Turkey breast',
    categoria_en   = 'Meat & poultry',
    descripcion_en = 'Cooked turkey breast'
WHERE id = 7 AND nombre = 'Pechuga de pavo';

UPDATE alimentos SET
    nombre_en      = 'Almonds',
    categoria_en   = 'Nuts',
    descripcion_en = 'Raw unsalted almonds'
WHERE id = 8 AND nombre = 'Almendras';

UPDATE alimentos SET
    nombre_en      = 'Broccoli',
    categoria_en   = 'Vegetables',
    descripcion_en = 'Steamed broccoli'
WHERE id = 9 AND nombre = 'Brócoli';

UPDATE alimentos SET
    nombre_en      = 'Olive oil',
    categoria_en   = 'Fats',
    descripcion_en = 'Extra virgin olive oil'
WHERE id = 10 AND nombre = 'Aceite de oliva';

-- ── TRADUCCIONES: LOGROS (ids 1-6, sembrados por V202605161135) ───────────────
-- Filtro por tipo (único por logro) además del id para máxima robustez.
UPDATE logros SET
    nombre_en      = 'First session',
    descripcion_en = 'Complete your first training session'
WHERE id = 1 AND tipo = 'PRIMERA_SESION';

UPDATE logros SET
    nombre_en      = 'Consistency',
    descripcion_en = 'Complete 7 training sessions'
WHERE id = 2 AND tipo = 'CONSTANCIA';

UPDATE logros SET
    nombre_en      = 'Dedicated',
    descripcion_en = 'Complete 30 training sessions'
WHERE id = 3 AND tipo = 'DEDICADO';

UPDATE logros SET
    nombre_en      = 'Centurion',
    descripcion_en = 'Perform 100 exercises in total'
WHERE id = 4 AND tipo = 'CENTENARIO';

UPDATE logros SET
    nombre_en      = 'Goal achieved',
    descripcion_en = 'Complete your first personal goal'
WHERE id = 5 AND tipo = 'OBJETIVO_CUMPLIDO';

UPDATE logros SET
    nombre_en      = 'Machine',
    descripcion_en = 'Complete 10 personal goals'
WHERE id = 6 AND tipo = 'MAQUINA';

-- ── TRADUCCIONES: RUTINAS PREDEFINIDAS del seed (ids 1, 2, 5, 6, 7, 8) ────────
-- Solo las predefinidas del sistema (es_predefinida = 1); las rutinas de
-- usuario (ids 3-4 del seed) no se traducen.
UPDATE rutinas SET
    nombre_en      = 'Chest & Triceps',
    descripcion_en = 'Classic push routine for chest and triceps',
    categoria_en   = 'Strength'
WHERE id = 1 AND es_predefinida = 1 AND nombre = 'Pecho y Tríceps';

UPDATE rutinas SET
    nombre_en      = 'Full Body',
    descripcion_en = 'Balanced full-body workout',
    categoria_en   = 'General'
WHERE id = 2 AND es_predefinida = 1 AND nombre = 'Full Body';

UPDATE rutinas SET
    nombre_en      = 'Active Mobility',
    descripcion_en = 'Light cardio and bodyweight exercises to get started',
    categoria_en   = 'Cardio'
WHERE id = 5 AND es_predefinida = 1 AND nombre = 'Movilidad Activa';

UPDATE rutinas SET
    nombre_en      = 'Back & Biceps',
    descripcion_en = 'Pull work with emphasis on back and biceps',
    categoria_en   = 'Strength'
WHERE id = 6 AND es_predefinida = 1 AND nombre = 'Espalda y Bíceps';

UPDATE rutinas SET
    nombre_en      = 'Powerlifting Basics',
    descripcion_en = 'The three basic lifts with maximal loads',
    categoria_en   = 'Strength'
WHERE id = 7 AND es_predefinida = 1 AND nombre = 'Powerlifting Base';

UPDATE rutinas SET
    nombre_en      = 'Advanced HIIT',
    descripcion_en = 'High intensity with minimal rest between sets',
    categoria_en   = 'Cardio'
WHERE id = 8 AND es_predefinida = 1 AND nombre = 'HIIT Avanzado';
