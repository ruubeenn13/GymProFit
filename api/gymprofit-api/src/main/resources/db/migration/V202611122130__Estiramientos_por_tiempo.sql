-- ============================================================
-- GP-262: los estiramientos estáticos y la liberación con rodillo se miden en segundos
--
-- El lote 19 (V202611121801) dejó los estiramientos en repeticiones como dudosos. Decisión
-- del propietario (12-11-2026): los que se hacen manteniendo una postura y la liberación
-- miofascial con rodillo pasan a SEGUNDOS. Se quedan en repeticiones los que se hacen
-- moviéndose, y Superman, Bird Dog y Dead bug (no se nombran aquí, así que no se tocan):
--   Chair_Leg_Extended_Stretch (447), Dynamic_Back_Stretch (549), Dynamic_Chest_Stretch
--   (550), Groin_and_Back_Stretch (604), Iron_Crosses_stretch (657),
--   Round_The_World_Shoulder_Stretch (908) y Spinal_Stretch (1026).
-- Tampoco «Rodillo de muñeca» (Wrist_Roller), que es de fuerza, ni los círculos.
--
-- Cada uno se clasificó leyendo su descripción del catálogo (catalogo/ejercicios-es.csv):
-- «mantén de 10 a 30 segundos» es estático; «repite de 5 a 10 veces», «alterna» o «gira
-- unas 3 veces», dinámico. La lista es la de producción del 12-11-2026 (id entre paréntesis).
--
-- Solo UPDATE por fed_id, la clave estable de free-exercise-db (el id cambia entre bases).
-- Aplicarla dos veces da lo mismo. La pauta de una rutina ya guardada no cambia: la medida
-- del ejercicio es lo que se propone al añadirlo.
-- ============================================================

-- Estiramientos que se mantienen (62).
UPDATE ejercicios SET medida = 'SEGUNDOS'
WHERE fed_id IN (
    'Adductor_Groin',                          -- (304) Estiramiento de aductores con compañero
    'All_Fours_Quad_Stretch',                  -- (307) Estiramiento de cuádriceps a cuatro patas
    'Ankle_On_The_Knee',                       -- (320) Estiramiento con el tobillo sobre la rodilla
    'Behind_Head_Chest_Stretch',               -- (368) Estiramiento de pecho con las manos tras la nuca
    'Calf_Stretch_Elbows_Against_Wall',        -- (437) Estiramiento de gemelos con los codos contra la pared
    'Calf_Stretch_Hands_Against_Wall',         -- (438) Estiramiento de gemelos con las manos contra la pared
    'Cat_Stretch',                             -- (443) Estiramiento del gato
    'Chair_Lower_Back_Stretch',                -- (448) Estiramiento lumbar en silla
    'Chair_Upper_Body_Stretch',                -- (450) Estiramiento del tren superior en silla
    'Chest_And_Front_Of_Shoulder_Stretch',     -- (451) Estiramiento de pecho y parte frontal del hombro
    'Chest_Stretch_on_Stability_Ball',         -- (456) Estiramiento de pecho con fitball
    'Chin_To_Chest_Stretch',                   -- (459) Estiramiento de barbilla al pecho
    'Dancers_Stretch',                         -- (489) Estiramiento del bailarín
    'Hamstring_Stretch',                       -- (610) Estiramiento de femorales
    'IT_Band_and_Glute_Stretch',               -- (631) Estiramiento de banda iliotibial y glúteo
    'Intermediate_Groin_Stretch',              -- (651) Estiramiento intermedio de ingles
    'Intermediate_Hip_Flexor_and_Quad_Stretch',-- (652) Estiramiento intermedio de flexores de la cadera y cuádriceps
    'Kneeling_Forearm_Stretch',                -- (693) Estiramiento de antebrazos de rodillas
    'Kneeling_Hip_Flexor',                     -- (695) Estiramiento de flexores de la cadera de rodillas
    'Leg-Up_Hamstring_Stretch',                -- (707) Estiramiento de isquiotibiales con pierna elevada
    'Lying_Bent_Leg_Groin',                    -- (733) Estiramiento de aductores tumbado con compañero, piernas flexionadas
    'Lying_Glute',                             -- (743) Estiramiento de glúteo tumbado con compañero
    'Lying_Hamstring',                         -- (744) Estiramiento de femorales tumbado con compañero
    'Lying_Prone_Quadriceps',                  -- (749) Estiramiento de cuádriceps boca abajo con compañero
    'Middle_Back_Stretch',                     -- (763) Estiramiento de espalda media de pie
    'On-Your-Back_Quad_Stretch',               -- (779) Estiramiento de cuádriceps tumbado boca arriba
    'On_Your_Side_Quad_Stretch',               -- (780) Estiramiento de cuádriceps tumbado de lado
    'One_Arm_Against_Wall',                    -- (804) Estiramiento de dorsal con un brazo contra la pared
    'Overhead_Lat',                            -- (819) Estiramiento de dorsal con compañero por encima de la cabeza
    'Overhead_Stretch',                        -- (822) Estiramiento de brazos por encima de la cabeza
    'Overhead_Triceps',                        -- (823) Estiramiento de tríceps con compañero por encima de la cabeza
    'Peroneals_Stretch',                       -- (833) Estiramiento de peroneos
    'Posterior_Tibialis_Stretch',              -- (844) Estiramiento de tibial posterior
    'Quad_Stretch',                            -- (869) Estiramiento de cuádriceps
    'Runners_Stretch',                         -- (910) Estiramiento del corredor
    'Seated_Biceps',                           -- (923) Estiramiento de bíceps sentado con compañero
    'Seated_Calf_Stretch',                     -- (927) Estiramiento de gemelos sentado
    'Seated_Floor_Hamstring_Stretch',          -- (935) Estiramiento de femorales sentado en el suelo
    'Seated_Front_Deltoid',                    -- (936) Estiramiento de deltoides anterior sentado con compañero
    'Seated_Glute',                            -- (937) Estiramiento de glúteos sentado con compañero
    'Seated_Hamstring',                        -- (939) Estiramiento de femorales sentado con compañero
    'Seated_Hamstring_and_Calf_Stretch',       -- (940) Estiramiento de femorales y gemelos sentado
    'Seated_Overhead_Stretch',                 -- (947) Estiramiento lateral sentado por encima de la cabeza
    'Shoulder_Stretch',                        -- (958) Estiramiento de hombro
    'Side-Lying_Floor_Stretch',                -- (959) Estiramiento lateral tumbado de lado en el suelo
    'Side_Lying_Groin_Stretch',                -- (966) Estiramiento de ingle tumbado de lado
    'Side_Neck_Stretch',                       -- (967) Estiramiento lateral de cuello
    'Side_Wrist_Pull',                         -- (970) Estiramiento de brazo cruzado tirando de la muñeca
    'Standing_Biceps_Stretch',                 -- (1045) Estiramiento de bíceps de pie
    'Standing_Elevated_Quad_Stretch',          -- (1058) Estiramiento de cuádriceps de pie con pie elevado
    'Standing_Gastrocnemius_Calf_Stretch',     -- (1060) Estiramiento de gemelos de pie
    'Standing_Hamstring_and_Calf_Stretch',     -- (1061) Estiramiento de femorales y gemelos de pie
    'Standing_Hip_Flexors',                    -- (1063) Estiramiento de flexores de la cadera de pie
    'Standing_Lateral_Stretch',                -- (1065) Estiramiento lateral de pie
    'Standing_Soleus_And_Achilles_Stretch',    -- (1081) Estiramiento de sóleo y tendón de Aquiles de pie
    'The_Straddle',                            -- (1112) Estiramiento de piernas abiertas sentado
    'Tricep_Side_Stretch',                     -- (1121) Estiramiento lateral de tríceps
    'Triceps_Stretch',                         -- (1126) Estiramiento de tríceps
    'Upper_Back-Leg_Grab',                     -- (1134) Estiramiento de espalda alta abrazando las piernas
    'Upper_Back_Stretch',                      -- (1135) Estiramiento de espalda alta
    'Upward_Stretch',                          -- (1139) Estiramiento hacia arriba
    'Worlds_Greatest_Stretch'                  -- (1164) Estiramiento más completo del mundo
);

-- Liberación miofascial con rodillo (14).
UPDATE ejercicios SET medida = 'SEGUNDOS'
WHERE fed_id IN (
    'Adductor',             -- (303) Liberación miofascial de aductores con rodillo
    'Anterior_Tibialis-SMR',-- (321) Liberación miofascial del tibial anterior con rodillo
    'Brachialis-SMR',       -- (403) Liberación miofascial de braquial con rodillo
    'Calves-SMR',           -- (439) Liberación miofascial de gemelos con rodillo
    'Foot-SMR',             -- (577) Liberación miofascial del pie con rodillo
    'Hamstring-SMR',        -- (609) Liberación miofascial de femorales con rodillo
    'Iliotibial_Tract-SMR', -- (632) Liberación miofascial de la banda iliotibial con rodillo
    'Latissimus_Dorsi-SMR', -- (705) Liberación miofascial de dorsales con rodillo
    'Lower_Back-SMR',       -- (729) Liberación miofascial lumbar con rodillo
    'Neck-SMR',             -- (774) Liberación miofascial de cuello con rodillo
    'Peroneals-SMR',        -- (832) Liberación miofascial de peroneos con rodillo
    'Piriformis-SMR',       -- (836) Liberación miofascial de piriforme con rodillo
    'Quadriceps-SMR',       -- (870) Liberación miofascial de cuádriceps con rodillo
    'Rhomboids-SMR'         -- (895) Liberación miofascial de romboides con rodillo
);
