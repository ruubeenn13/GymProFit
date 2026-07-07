-- ============================================================
-- V202607071000__Catalogo_apis_externas.sql
-- Migración del catálogo a fuentes externas (roadmap lanzamiento fase 1.2):
--   · alimentos  → Open Food Facts (búsqueda en vivo + import bajo demanda)
--   · ejercicios → wger (import masivo vía endpoint admin)
-- Cambios:
--   1. alimentos: columnas barcode (código de barras OFF, clave de upsert)
--      y marca (fabricante), con índice único sobre barcode (NULL permitido
--      múltiple en MySQL/MariaDB → los alimentos personalizados no chocan).
--   2. ejercicios: columna wger_id (id del ejercicio en wger, clave de
--      upsert idempotente del import) con índice único.
--   3. Purga del catálogo seed antiguo (10+10 de demo, poco precisos):
--      se BORRAN las filas globales no referenciadas por otras tablas y se
--      DESACTIVAN (activo=0) las que sí tienen referencias (FK de comidas,
--      rutinas, sesiones o progreso) para no romper integridad ni histórico.
--      En prod el catálogo está vacío → los DELETE/UPDATE son no-op seguros.
-- ============================================================

-- ── 1. ALIMENTOS: origen externo ─────────────────────────────────────────────
ALTER TABLE alimentos
    ADD COLUMN barcode VARCHAR(32)  NULL,
    ADD COLUMN marca   VARCHAR(100) NULL;

CREATE UNIQUE INDEX uq_alimentos_barcode ON alimentos (barcode);

-- ── 2. EJERCICIOS: origen externo ─────────────────────────────────────────────
ALTER TABLE ejercicios
    ADD COLUMN wger_id INT NULL;

CREATE UNIQUE INDEX uq_ejercicios_wger_id ON ejercicios (wger_id);

-- Fix del typo histórico del enum GrupoMuscular: ABOMEN → ABDOMEN (el enum
-- Java y el generado de jOOQ se renombran en el mismo commit).
UPDATE ejercicios SET grupo_muscular = 'ABDOMEN' WHERE grupo_muscular = 'ABOMEN';

-- ── 3. PURGA DEL SEED ANTIGUO ─────────────────────────────────────────────────
-- Alimentos globales (usuario_id NULL) sin líneas de comida asociadas → fuera.
DELETE FROM alimentos
WHERE usuario_id IS NULL
  AND id NOT IN (SELECT DISTINCT alimento_id FROM alimentos_comida);

-- Alimentos globales referenciados por comidas → baja lógica (histórico intacto).
UPDATE alimentos
SET activo = 0
WHERE usuario_id IS NULL
  AND id IN (SELECT DISTINCT alimento_id FROM alimentos_comida);

-- Ejercicios sin referencias (rutinas, sesiones, progreso) → fuera.
DELETE FROM ejercicios
WHERE id NOT IN (SELECT DISTINCT ejercicio_id FROM rutina_ejercicio)
  AND id NOT IN (SELECT DISTINCT ejercicio_id FROM ejercicios_realizados)
  AND id NOT IN (SELECT DISTINCT ejercicio_id FROM progreso_ejercicios);

-- Ejercicios referenciados → baja lógica (las rutinas/sesiones antiguas siguen consultables).
UPDATE ejercicios
SET activo = 0
WHERE id IN (SELECT DISTINCT ejercicio_id FROM rutina_ejercicio)
   OR id IN (SELECT DISTINCT ejercicio_id FROM ejercicios_realizados)
   OR id IN (SELECT DISTINCT ejercicio_id FROM progreso_ejercicios);
