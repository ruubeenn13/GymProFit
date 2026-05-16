-- =============================================================================
-- GymProFit — Limpieza + seed de datos de ejemplo
-- Uso: ejecutar este script completo contra la BD de desarrollo.
-- Después arrancar la API: DataInitializer creará admin (contraseña: Admin1234)
-- y guest (contraseña: guest) automáticamente si no existen.
-- Los 3 usuarios de ejemplo usan contraseña: Demo1234
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ── LIMPIEZA ─────────────────────────────────────────────────────────────────
TRUNCATE TABLE usuario_logros;
TRUNCATE TABLE usuario_roles;
TRUNCATE TABLE ejercicios_realizados;
TRUNCATE TABLE alimentos_comida;
TRUNCATE TABLE progreso_ejercicios;
TRUNCATE TABLE rutina_ejercicio;
TRUNCATE TABLE sesiones_entrenamiento;
TRUNCATE TABLE notificaciones;
TRUNCATE TABLE mediciones_corporales;
TRUNCATE TABLE objetivos_personales;
TRUNCATE TABLE comidas;
TRUNCATE TABLE rutinas;
TRUNCATE TABLE usuarios;
TRUNCATE TABLE ejercicios;
TRUNCATE TABLE alimentos;
TRUNCATE TABLE logros;
TRUNCATE TABLE roles;

-- ── ROLES ─────────────────────────────────────────────────────────────────────
INSERT INTO roles (id, nombre) VALUES
(1, 'ADMIN'),
(2, 'USER'),
(3, 'GUEST');

-- ── LOGROS ────────────────────────────────────────────────────────────────────
INSERT INTO logros (nombre, descripcion, tipo) VALUES
('Primera sesión',    'Completa tu primera sesión de entrenamiento', 'PRIMERA_SESION'),
('Constancia',        'Completa 7 sesiones de entrenamiento',        'CONSTANCIA'),
('Dedicado',          'Completa 30 sesiones de entrenamiento',       'DEDICADO'),
('Centenario',        'Realiza 100 ejercicios en total',             'CENTENARIO'),
('Objetivo cumplido', 'Completa tu primer objetivo personal',        'OBJETIVO_CUMPLIDO'),
('Máquina',           'Completa 10 objetivos personales',            'MAQUINA');

-- ── EJERCICIOS ────────────────────────────────────────────────────────────────
INSERT INTO ejercicios (id, nombre, descripcion, grupo_muscular, dificultad, calorias_quemadas, equipo_necesario, activo) VALUES
(1,  'Press de banca',    'Ejercicio compuesto para pecho con barra',        'PECHO',    'INTERMEDIO',    8, 'Barra y banco',   1),
(2,  'Sentadilla',        'Ejercicio fundamental para cuádriceps y glúteos', 'PIERNAS',  'INTERMEDIO',   10, 'Barra',           1),
(3,  'Peso muerto',       'Ejercicio de cadena posterior con barra',         'ESPALDA',  'AVANZADO',     12, 'Barra',           1),
(4,  'Dominadas',         'Jalón en barra fija con peso corporal',           'ESPALDA',  'INTERMEDIO',    8, 'Barra dominadas', 1),
(5,  'Press militar',     'Press de hombros con barra en pie',               'HOMBROS',  'INTERMEDIO',    7, 'Barra',           1),
(6,  'Curl de bíceps',    'Flexión de codo con mancuernas',                  'BRAZOS',   'PRINCIPIANTE',  4, 'Mancuernas',      1),
(7,  'Tríceps en polea',  'Extensión de codo en polea alta',                 'BRAZOS',   'PRINCIPIANTE',  4, 'Polea',           1),
(8,  'Plancha',           'Ejercicio isométrico de core',                    'ABOMEN',   'PRINCIPIANTE',  3, 'Sin equipo',      1),
(9,  'Carrera en cinta',  'Cardio en cinta a ritmo moderado',                'CARDIO',   'PRINCIPIANTE', 10, 'Cinta de correr', 1),
(10, 'Burpees',           'Ejercicio de cuerpo completo de alta intensidad', 'FULLBODY', 'INTERMEDIO',   15, 'Sin equipo',      1);

-- ── ALIMENTOS ─────────────────────────────────────────────────────────────────
INSERT INTO alimentos (id, nombre, categoria, calorias, proteinas, carbohidratos, grasas, fibra, porcion_gramos, descripcion, activo) VALUES
(1,  'Pechuga de pollo', 'Proteínas',     165, 31,   0.00,   3.60,  0.00, 100, 'Pechuga de pollo a la plancha',  1),
(2,  'Arroz blanco',     'Carbohidratos', 130,  3,  28.00,   0.30,  0.40, 100, 'Arroz blanco cocido',            1),
(3,  'Huevo entero',     'Proteínas',     155, 13,   1.10,  11.00,  0.00, 100, 'Huevo cocido completo',          1),
(4,  'Avena',            'Cereales',      389, 17,  66.00,   7.00, 11.00, 100, 'Avena en copos',                 1),
(5,  'Plátano',          'Frutas',         89,  1,  23.00,   0.30,  2.60, 120, 'Plátano maduro',                 1),
(6,  'Leche desnatada',  'Lácteos',        35,  4,   5.00,   0.10,  0.00, 100, 'Leche desnatada 0%',             1),
(7,  'Pechuga de pavo',  'Proteínas',     135, 30,   0.00,   1.00,  0.00, 100, 'Pechuga de pavo cocida',         1),
(8,  'Almendras',        'Frutos secos',  579, 21,   7.00,  50.00, 12.00,  30, 'Almendras crudas sin sal',       1),
(9,  'Brócoli',          'Verduras',       34,  3,   7.00,   0.40,  2.60, 100, 'Brócoli al vapor',               1),
(10, 'Aceite de oliva',  'Grasas',        884,  0,   0.00, 100.00,  0.00,  15, 'Aceite de oliva virgen extra',   1);

-- ── USUARIOS ──────────────────────────────────────────────────────────────────
-- Contraseña de los 3 usuarios de ejemplo: Demo1234 (hash BCrypt coste 10)
-- admin y guest los crea DataInitializer al arrancar la API (IDs 4 y 5)
INSERT INTO usuarios (id, username, password, email, peso, altura, edad, nivel_experiencia, objetivo, fecha_registro, activo) VALUES
(1, 'carlos_garcia', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'carlos@gymprofit.com', 80.00, 1.78, 28, 'INTERMEDIO',   'GANAR_MASA_MUSCULAR', '2025-01-15 09:00:00', 1),
(2, 'maria_lopez',   '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'maria@gymprofit.com',  65.00, 1.65, 25, 'PRINCIPIANTE', 'PERDER_PESO',          '2025-02-01 10:30:00', 1),
(3, 'javier_ruiz',   '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'javier@gymprofit.com', 90.00, 1.82, 32, 'AVANZADO',     'MEJORAR_FUERZA',       '2025-01-01 08:00:00', 1);

-- ── USUARIO_ROLES ─────────────────────────────────────────────────────────────
INSERT INTO usuario_roles (usuario_id, role_id) VALUES
(1, 2),
(2, 2),
(3, 2);

-- ── RUTINAS ───────────────────────────────────────────────────────────────────
INSERT INTO rutinas (id, nombre, descripcion, duracion_minutos, nivel, es_predefinida, categoria, dias_semana, fecha_creacion, activa, usuario_id) VALUES
(1, 'Pecho y Tríceps', 'Rutina clásica de empuje para pecho y tríceps',   60, 'INTERMEDIO',   1, 'Fuerza',  'LUNES,JUEVES',            '2025-01-01 00:00:00', 1, NULL),
(2, 'Full Body',       'Entrenamiento de cuerpo completo equilibrado',     50, 'PRINCIPIANTE', 1, 'General', 'MARTES,VIERNES',          '2025-01-01 00:00:00', 1, NULL),
(3, 'Fuerza Pesada',   'Rutina personalizada de fuerza con cargas altas',  75, 'AVANZADO',     0, 'Fuerza',  'LUNES,MIERCOLES,VIERNES', '2025-02-10 00:00:00', 1, 3),
(4, 'Cardio y Core',   'Cardio combinado con trabajo de core',             45, 'PRINCIPIANTE', 0, 'Cardio',  'MARTES,JUEVES,SABADO',    '2025-02-15 00:00:00', 1, 2);

-- ── RUTINA_EJERCICIO ──────────────────────────────────────────────────────────
INSERT INTO rutina_ejercicio (id, series, repeticiones, peso_recomendado, tiempo_descanso, orden, rutina_id, ejercicio_id) VALUES
-- Rutina 1: Pecho y Tríceps
(1,  4, 10, 80.00, 90, 1, 1, 1),
(2,  3, 12, 25.00, 60, 2, 1, 7),
-- Rutina 2: Full Body
(3,  3, 12, 60.00, 90, 1, 2, 2),
(4,  4, 60,  NULL, 45, 2, 2, 8),
(5,  3, 15,  NULL, 60, 3, 2, 10),
-- Rutina 3: Fuerza Pesada
(6,  5,  5, 120.00, 120, 1, 3, 3),
(7,  5,  5, 100.00, 120, 2, 3, 2),
(8,  4,  8,  90.00,  90, 3, 3, 1),
-- Rutina 4: Cardio y Core
(9,  1, 30,  NULL, 30, 1, 4, 9),
(10, 4, 60,  NULL, 30, 2, 4, 8);

-- ── SESIONES_ENTRENAMIENTO ────────────────────────────────────────────────────
INSERT INTO sesiones_entrenamiento (id, fecha_inicio, fecha_fin, duracion_minutos, calorias_quemadas, notas, completada, usuario_id, rutina_id) VALUES
-- Carlos
(1, '2025-03-01 10:00:00', '2025-03-01 11:05:00', 65, 380, 'Buena sesión, subida de peso en press', 1, 1, 1),
(2, '2025-03-05 10:00:00', '2025-03-05 11:15:00', 75, 420, NULL,                                    1, 1, 3),
(3, '2025-03-10 10:00:00', '2025-03-10 11:20:00', 80, 450, 'Nuevo récord en peso muerto',           1, 1, 3),
-- María
(4, '2025-03-02 09:00:00', '2025-03-02 09:50:00', 50, 280, 'Primera sesión completada',             1, 2, 2),
(5, '2025-03-04 09:00:00', '2025-03-04 09:45:00', 45, 260, NULL,                                    1, 2, 4),
(6, '2025-03-07 09:00:00', '2025-03-07 09:50:00', 50, 290, 'Mejorando ritmo en cardio',             1, 2, 4),
-- Javier
(7, '2025-03-01 18:00:00', '2025-03-01 19:20:00', 80, 500, NULL,                                    1, 3, 3),
(8, '2025-03-03 18:00:00', '2025-03-03 19:25:00', 85, 520, 'PR en sentadilla',                      1, 3, 3),
(9, '2025-03-06 18:00:00', '2025-03-06 19:15:00', 75, 480, NULL,                                    1, 3, 3);

-- ── EJERCICIOS_REALIZADOS ─────────────────────────────────────────────────────
INSERT INTO ejercicios_realizados (id, series_completadas, repeticiones_rutinas, peso_usado, tiempo_segundos, notas, sesion_id, ejercicio_id) VALUES
-- Sesión 1 - Carlos (Pecho y Tríceps)
(1,  4, 10,  80.00, NULL, NULL,             1, 1),
(2,  3, 12,  25.00, NULL, NULL,             1, 7),
-- Sesión 2 - Carlos (Fuerza Pesada)
(3,  5,  5, 120.00, NULL, NULL,             2, 3),
(4,  5,  5, 100.00, NULL, NULL,             2, 2),
(5,  4,  8,  90.00, NULL, NULL,             2, 1),
-- Sesión 3 - Carlos (Fuerza Pesada)
(6,  5,  5, 125.00, NULL, 'Nuevo récord',   3, 3),
(7,  5,  5, 102.50, NULL, NULL,             3, 2),
(8,  4,  8,  92.50, NULL, NULL,             3, 1),
-- Sesión 4 - María (Full Body)
(9,  3, 12,  40.00, NULL, NULL,             4, 2),
(10, 4, NULL, NULL,   60, NULL,             4, 8),
(11, 3, 15,   NULL, NULL, NULL,             4, 10),
-- Sesión 5 - María (Cardio y Core)
(12, 1, NULL, NULL, 1800, NULL,             5, 9),
(13, 4, NULL, NULL,   45, NULL,             5, 8),
-- Sesión 6 - María (Cardio y Core)
(14, 1, NULL, NULL, 1900, 'Mejor tiempo',   6, 9),
(15, 4, NULL, NULL,   50, NULL,             6, 8),
-- Sesión 7 - Javier (Fuerza Pesada)
(16, 5,  5, 150.00, NULL, NULL,             7, 3),
(17, 5,  5, 130.00, NULL, NULL,             7, 2),
(18, 4,  8, 110.00, NULL, NULL,             7, 1),
-- Sesión 8 - Javier (Fuerza Pesada)
(19, 5,  5, 150.00, NULL, NULL,             8, 3),
(20, 5,  5, 132.50, NULL, 'PR sentadilla',  8, 2),
(21, 4,  8, 112.50, NULL, NULL,             8, 1),
-- Sesión 9 - Javier (Fuerza Pesada)
(22, 5,  5, 152.50, NULL, NULL,             9, 3),
(23, 5,  5, 132.50, NULL, NULL,             9, 2),
(24, 4,  8, 112.50, NULL, NULL,             9, 1);

-- ── PROGRESO_EJERCICIOS ───────────────────────────────────────────────────────
INSERT INTO progreso_ejercicios (id, fecha, mejor_peso, mejor_repeticiones, mejor_tiempo_segundos, notas, usuario_id, ejercicio_id) VALUES
(1, '2025-03-10 00:00:00', 125.00,  5, NULL, 'PR histórico',  1, 3),
(2, '2025-03-10 00:00:00',  92.50,  8, NULL, NULL,            1, 1),
(3, '2025-03-04 00:00:00',  40.00, 12, NULL, NULL,            2, 2),
(4, '2025-03-07 00:00:00',   NULL, NULL, 1900, 'Mejor tiempo', 2, 9),
(5, '2025-03-08 00:00:00', 152.50,  5, NULL, 'PR total',      3, 3),
(6, '2025-03-08 00:00:00', 132.50,  5, NULL, 'PR sentadilla', 3, 2);

-- ── MEDICIONES_CORPORALES ─────────────────────────────────────────────────────
INSERT INTO mediciones_corporales (id, fecha, peso, altura, imc, grasa_corporal, masa_muscular, cintura, pecho, brazos, piernas, notas, usuario_id) VALUES
-- Carlos (evolución 3 meses)
(1, '2025-01-15 08:00:00', 82.00, 1.78, 25.90, 18.00, 38.00, 86.00, 104.00, 36.00, 56.00, 'Medición inicial', 1),
(2, '2025-02-15 08:00:00', 81.00, 1.78, 25.56, 17.00, 39.00, 85.00, 105.00, 37.00, 57.00, NULL,               1),
(3, '2025-03-15 08:00:00', 80.00, 1.78, 25.25, 16.00, 40.00, 84.00, 106.00, 38.00, 58.00, 'Buena evolución',  1),
-- María (evolución 2 meses)
(4, '2025-02-01 08:00:00', 68.00, 1.65, 24.98, 28.00, 26.00, 78.00, 90.00, 28.00, 52.00, 'Medición inicial', 2),
(5, '2025-03-01 08:00:00', 67.00, 1.65, 24.61, 27.00, 26.50, 77.00, 90.00, 28.00, 52.00, NULL,               2),
-- Javier (evolución 2 meses)
(6, '2025-01-01 08:00:00', 92.00, 1.82, 27.76, 14.00, 46.00, 88.00, 112.00, 42.00, 64.00, 'Medición inicial', 3),
(7, '2025-03-01 08:00:00', 90.00, 1.82, 27.17, 13.00, 47.00, 87.00, 113.00, 43.00, 65.00, NULL,               3);

-- ── COMIDAS ───────────────────────────────────────────────────────────────────
INSERT INTO comidas (id, fecha, tipo_comida, total_calorias, total_proteinas, total_carbohidratos, total_grasas, notas, usuario_id) VALUES
-- Carlos
(1, '2025-03-01 08:00:00', 'DESAYUNO', 450, 35.00, 50.00, 10.00, NULL,          1),
(2, '2025-03-01 13:00:00', 'COMIDA',   650, 55.00, 60.00, 12.00, 'Post-entreno', 1),
(3, '2025-03-01 20:00:00', 'CENA',     500, 45.00, 30.00, 15.00, NULL,           1),
-- María
(4, '2025-03-02 08:30:00', 'DESAYUNO', 300, 20.00, 40.00,  8.00, NULL,                1),
(5, '2025-03-02 13:30:00', 'COMIDA',   450, 35.00, 45.00, 10.00, 'Comida equilibrada', 2),
-- Javier
(6, '2025-03-01 08:00:00', 'DESAYUNO', 600, 45.00, 70.00, 15.00, NULL,                         3),
(7, '2025-03-01 13:00:00', 'COMIDA',   800, 65.00, 80.00, 18.00, 'Post-entreno voluminizador',  3);

-- ── ALIMENTOS_COMIDA ──────────────────────────────────────────────────────────
INSERT INTO alimentos_comida (id, cantidad_gramos, calorias_totales, comida_id, alimento_id) VALUES
-- Desayuno Carlos
(1,   80.00, 311, 1, 4),   -- Avena 80g
(2,  250.00,  88, 1, 6),   -- Leche desnatada 250ml
(3,  200.00, 310, 1, 3),   -- 2 huevos (200g)
-- Comida Carlos
(4,  200.00, 330, 2, 1),   -- Pollo 200g
(5,  150.00, 195, 2, 2),   -- Arroz 150g
(6,  200.00,  68, 2, 9),   -- Brócoli 200g
-- Cena Carlos
(7,  200.00, 270, 3, 7),   -- Pavo 200g
(8,  100.00, 130, 3, 2),   -- Arroz 100g
-- Desayuno María
(9,   60.00, 233, 4, 4),   -- Avena 60g
(10, 200.00,  70, 4, 6),   -- Leche 200ml
-- Comida María
(11, 150.00, 248, 5, 1),   -- Pollo 150g
(12, 100.00, 130, 5, 2),   -- Arroz 100g
-- Desayuno Javier
(13, 120.00, 467, 6, 4),   -- Avena 120g
(14, 300.00, 105, 6, 6),   -- Leche 300ml
(15, 200.00, 310, 6, 3),   -- Huevo 200g
-- Comida Javier
(16, 300.00, 495, 7, 1),   -- Pollo 300g
(17, 200.00, 260, 7, 2),   -- Arroz 200g
(18,  30.00, 174, 7, 8);   -- Almendras 30g

-- ── OBJETIVOS_PERSONALES ──────────────────────────────────────────────────────
INSERT INTO objetivos_personales (id, tipo_objetivo, descripcion, valor_actual, valor_objetivo, unidad, fecha_inicio, fecha_limite, completado, fecha_completado, usuario_id) VALUES
(1, 'GANAR_MASA_MUSCULAR', 'Ganar 5 kg de masa muscular',         2.00,   5.00, 'kg',       '2025-01-15', '2025-07-15', 0, NULL, 1),
(2, 'MEJORAR_FUERZA',      'Alcanzar 100 kg en press de banca',  92.50, 100.00, 'kg',       '2025-01-15', '2025-06-15', 0, NULL, 1),
(3, 'PERDER_PESO',         'Bajar 5 kg de peso corporal',         1.00,   5.00, 'kg',       '2025-02-01', '2025-06-01', 0, NULL, 2),
(4, 'MEJORAR_RESISTENCIA', 'Correr 5 km sin parar',               2.00,   5.00, 'km',       '2025-02-01', '2025-05-01', 0, NULL, 2),
(5, 'MEJORAR_FUERZA',      'Alcanzar 160 kg en peso muerto',    152.50, 160.00, 'kg',       '2025-01-01', '2025-04-01', 0, NULL, 3),
(6, 'COMPLETAR_RETO',      'Completar 30 sesiones de entreno',    9.00,  30.00, 'sesiones', '2025-01-01', '2025-04-30', 0, NULL, 3);

-- ── NOTIFICACIONES ────────────────────────────────────────────────────────────
INSERT INTO notificaciones (id, titulo, mensaje, tipo, fecha_creacion, fecha_programada, leida, usuario_id) VALUES
(1, '¡Logro desbloqueado!',   'Has completado tu primera sesión',              'LOGRO',        '2025-03-01 11:05:00', NULL,                  1, 1),
(2, 'Recordatorio',           'No olvides tu sesión de hoy a las 10:00',       'RECORDATORIO', '2025-03-05 08:00:00', '2025-03-05 09:30:00', 0, 1),
(3, '¡Logro desbloqueado!',   'Has completado tu primera sesión',              'LOGRO',        '2025-03-02 09:50:00', NULL,                  1, 2),
(4, 'Recordatorio de sesión', 'Tu próxima sesión es mañana a las 09:00',       'RECORDATORIO', '2025-03-03 20:00:00', '2025-03-04 08:30:00', 0, 2),
(5, '¡Logro desbloqueado!',   'Has completado tu primera sesión',              'LOGRO',        '2025-03-01 19:20:00', NULL,                  1, 3),
(6, 'Objetivo cerca',         'Estás al 95% de tu objetivo de peso muerto',    'OBJETIVO',     '2025-03-08 00:00:00', NULL,                  0, 3);

-- ── USUARIO_LOGROS ────────────────────────────────────────────────────────────
-- IDs logros: 1=PRIMERA_SESION  2=CONSTANCIA  3=DEDICADO  4=CENTENARIO  5=OBJETIVO_CUMPLIDO  6=MAQUINA
INSERT INTO usuario_logros (id, usuario_id, logro_id, fecha_obtenido) VALUES
(1, 1, 1, '2025-03-01 11:05:00'),
(2, 2, 1, '2025-03-02 09:50:00'),
(3, 3, 1, '2025-03-01 19:20:00'),
(4, 3, 2, '2025-03-08 19:25:00');

SET FOREIGN_KEY_CHECKS = 1;
