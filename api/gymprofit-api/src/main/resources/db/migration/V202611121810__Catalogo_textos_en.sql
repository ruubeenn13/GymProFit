-- ============================================================
-- GP-220: ningún ejercicio en el idioma equivocado
--
-- El inventario de producción (documentacion/estado/2026-11-02-gp216-lote3.md, repetido el
-- 12-11-2026) encontró 8 descripciones que salían en español con la app en inglés y 2
-- nombres que salían igual en los dos idiomas sin serlo: «Curl De Muñeca Con Barra» en
-- inglés y «Push-Up» en español. El id 10 tenía además, como descripción en inglés, la de
-- un press de banca. Los logros de producción ya tienen todos su inglés: aquí no va ninguno.
--
-- Las traducciones están escritas a mano, sin traductor automático, como en GP-217. Esta
-- migración no toca la de GP-217 (V202610221810) ni su CSV.
--
-- Los saltos de línea van con CHAR(10) y no dentro del literal: con core.autocrlf, un
-- literal de varias líneas llevaría \r en una base de desarrollo y no en producción.
--
-- Los ejercicios viejos de wger (inactivos, sin fed_id) van por su nombre exacto, que en
-- producción es único entre los que no tienen fed_id (ids de producción entre paréntesis): así
-- casan también en una base con otros ids, y el test puede aplicarla sobre filas sembradas.
-- Los de free-exercise-db, por fed_id. Las descripciones, solo si su inglés falta o es el
-- español. Solo hay UPDATE: aplicarla dos veces da lo mismo.
-- ============================================================

-- ── Los 2 nombres ────────────────────────────────────────────

UPDATE ejercicios SET
    nombre         = 'Curl de muñeca con barra',
    nombre_en      = 'Barbell Wrist Curl',
    descripcion_en = CONCAT_WS(CHAR(10),
        'An isolation exercise for the inner (front) side of the forearm. Besides building the forearm, it strengthens your grip, which carries over to heavy lifts such as the deadlift or pull-ups.',
        '- Starting position: hold a barbell with an underhand grip (palms facing up), hands about shoulder-width apart.',
        '- On the bench: kneel in front of a flat bench and rest the back of your forearms on it. Your wrists and hands should hang slightly past the edge so they can move through the full range.',
        '- Lowering: lower the bar under control by bending your wrists toward the floor. At the bottom, open your fingers slightly and let the bar roll down a little for a longer stretch.',
        '- Lifting: close your fingers first to secure the bar, then curl your wrists up quickly but under control, as high as they go. Squeeze your forearms hard at the top for one second.',
        '- Posture: keep your forearms pressed against the bench the whole time; only your wrists and fingers move.')
WHERE nombre = 'Curl De Muñeca Con Barra' AND fed_id IS NULL;  -- (10)

UPDATE ejercicios SET
    nombre    = 'Flexión',
    nombre_en = 'Push-Up'
WHERE nombre = 'Push-Up' AND fed_id IS NULL;  -- (297)

-- ── Las 8 descripciones ──────────────────────────────────────

UPDATE ejercicios SET descripcion_en = CONCAT_WS(CHAR(10),
        'How to do it:',
        '- Setup: rest a barbell on your upper back (traps), as you would for a squat. Stand tall with your feet hip-width apart and make sure you have a clear path ahead of you.',
        '- Execution: take a long step forward with one leg. Lower your hips under control until the knee of the back leg is just above the floor. The front leg should form a 90-degree angle.',
        '- Moving forward: instead of pushing back to the start, drive forward. Push through the heel of the front leg and bring the back leg through to start the next lunge with it straight away.',
        '- Keep going: keep alternating legs, "walking" forward with each lunge, smoothly but under control. Keep your torso upright and your abs braced so you don''t lose your balance.')
WHERE nombre = 'Zancadas Caminando con Barra' AND fed_id IS NULL  -- (38)
  AND (descripcion_en IS NULL OR TRIM(descripcion_en) = '' OR descripcion_en = descripcion);

UPDATE ejercicios SET descripcion_en = CONCAT_WS(CHAR(10),
        'This exercise is ideal for isolating the triceps of each arm and focusing on one side at a time.',
        'Description:',
        'Set the pulley high and use a single handle. Standing, keep your elbow tucked against your side; it acts as a hinge and stays fixed throughout the movement.',
        'Extend your arm fully downward until you feel a strong contraction in the triceps. Pause briefly and return slowly and under control to the starting position.')
WHERE nombre = 'Extensión de Tríceps a una Mano en Polea' AND fed_id IS NULL  -- (217)
  AND (descripcion_en IS NULL OR TRIM(descripcion_en) = '' OR descripcion_en = descripcion);

UPDATE ejercicios SET descripcion_en =
        'Adjust the machine''s lever to your height and sit with your back against the back pad. Place your ankles on the pads, your thighs under the hold-down pad just above the knees, and your hands on the handles. Breathing in, bend your knees. Keep your back still the whole time: only your lower legs move, as your knees bend. Hold the contracted position for one second. Slowly return to the starting position. Breathe out at the end of the movement.'
WHERE nombre = 'Curl femoral sentado' AND fed_id IS NULL  -- (249)
  AND (descripcion_en IS NULL OR TRIM(descripcion_en) = '' OR descripcion_en = descripcion);

UPDATE ejercicios SET descripcion_en =
        'Sit on the machine with your feet shoulder-width apart. Keep your back upright and your arms bent to take the bar at shoulder height, then press the bar as high as you can until your arms are straight. Lower it slowly and repeat.'
WHERE nombre = 'Press de hombro con maquina' AND fed_id IS NULL  -- (271)
  AND (descripcion_en IS NULL OR TRIM(descripcion_en) = '' OR descripcion_en = descripcion);

UPDATE ejercicios SET descripcion_en = CONCAT_WS(CHAR(10),
        'This variation is great for isolating each quadriceps on its own, which helps correct strength imbalances and improves the mind-muscle connection.',
        'Description:',
        'Sit on the leg extension machine and set the roller over the ankle of one leg only. Extend that leg until it is completely straight, squeezing the quadriceps hard at the top of the movement.',
        'Lower the weight slowly and under control. Finish all the reps with one leg before switching to the other.')
WHERE nombre = 'Extensión de Cuádriceps a una Pierna' AND fed_id IS NULL  -- (298)
  AND (descripcion_en IS NULL OR TRIM(descripcion_en) = '' OR descripcion_en = descripcion);

UPDATE ejercicios SET descripcion_en = CONCAT_WS(CHAR(10),
        '1. Stand with your feet shoulder-width apart and a light dumbbell in each hand at your sides.',
        '2. Raise your straight arms out to the sides up to shoulder height, so your body forms a cross.',
        '3. Hold the position for one or two seconds and lower slowly.',
        '4. Use very little weight, don''t go above shoulder height and don''t swing your torso.')
WHERE fed_id = 'Iron_Cross'  -- (656)
  AND (descripcion_en IS NULL OR TRIM(descripcion_en) = '' OR descripcion_en = descripcion);

UPDATE ejercicios SET descripcion_en = CONCAT_WS(CHAR(10),
        '1. Stand with your feet a little wider than your shoulders and a kettlebell on the floor in front of you. Push your hips back with a straight back and grab the handle with one hand.',
        '2. Swing the kettlebell back between your legs, then drive your hips forward hard to send it up to chest height, with your arm straight.',
        '3. Let it fall and guide it back between your legs, hips back. Do all the reps and switch hands.',
        '4. The power comes from your hips, not your arm; keep your back straight and don''t round your lower back.')
WHERE fed_id = 'One-Arm_Kettlebell_Swings'  -- (796)
  AND (descripcion_en IS NULL OR TRIM(descripcion_en) = '' OR descripcion_en = descripcion);

UPDATE ejercicios SET descripcion_en = CONCAT_WS(CHAR(10),
        '1. Lie on your side on the floor, legs together and straight, with your top hand behind your head.',
        '2. Lift your torso and legs at the same time, bringing your elbow toward your knee and squeezing your obliques.',
        '3. Lower slowly without fully resting and repeat. Do all the reps and switch sides.',
        '4. Don''t pull on your neck with your hand: the movement comes from your abs.')
WHERE fed_id = 'Side_Jackknife'  -- (962)
  AND (descripcion_en IS NULL OR TRIM(descripcion_en) = '' OR descripcion_en = descripcion);
