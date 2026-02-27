-- ============================================
-- BASE DE DATOS: GymProFit - MariaDB/MySQL
-- Autor: Rubén Juan Candela
-- Ciclo: CFGS Desarrollo de Aplicaciones Multimedia (2º DAM)
-- Tablas: 13 tablas relacionadas
-- ============================================

DROP DATABASE IF EXISTS GymProFitDB;

CREATE DATABASE GymProFitDB;

USE GymProFitDB;

CREATE TABLE usuarios (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          username VARCHAR(50) NOT NULL UNIQUE,
                          password VARCHAR(255) NOT NULL,
                          email VARCHAR(100) NOT NULL UNIQUE,
                          peso DECIMAL(5,2) DEFAULT NULL,
                          altura DECIMAL(3,2) DEFAULT NULL,
                          edad INT DEFAULT NULL,
                          nivel_experiencia ENUM('Principiante', 'Intermedio', 'Avanzado') DEFAULT NULL,
                          objetivo VARCHAR(100) DEFAULT NULL,
                          fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
                          activo TINYINT(1) DEFAULT 1,
                          INDEX idx_username (username),
                          INDEX idx_email (email)
);

CREATE TABLE ejercicios (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            nombre VARCHAR(100) NOT NULL,
                            descripcion TEXT DEFAULT NULL,
                            grupo_muscular ENUM('Pecho', 'Espalda', 'Piernas', 'Hombros', 'Brazos', 'Abdomen', 'Cardio', 'Full Body') NOT NULL,
                            dificultad ENUM('Principiante', 'Intermedio', 'Avanzado') NOT NULL,
                            imagen_url VARCHAR(255) DEFAULT NULL,
                            instrucciones TEXT DEFAULT NULL,
                            calorias_quemadas INT DEFAULT NULL,
                            equipo_necesario VARCHAR(255) DEFAULT NULL,
                            activo TINYINT(1) DEFAULT 1,
                            INDEX idx_grupo_muscular (grupo_muscular),
                            INDEX idx_dificultad (dificultad)
);

CREATE TABLE rutinas (
                         id INT AUTO_INCREMENT PRIMARY KEY,
                         usuario_id INT DEFAULT NULL,
                         nombre VARCHAR(100) NOT NULL,
                         descripcion TEXT DEFAULT NULL,
                         duracion_minutos INT DEFAULT NULL,
                         nivel ENUM('Principiante', 'Intermedio', 'Avanzado') NOT NULL,
                         es_predefinida TINYINT(1) DEFAULT 0,
                         categoria VARCHAR(50) DEFAULT NULL,
                         dias_semana VARCHAR(100) DEFAULT NULL,
                         fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
                         activa TINYINT(1) DEFAULT 1,
                         FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                         INDEX idx_usuario_id (usuario_id),
                         INDEX idx_es_predefinida (es_predefinida)
);

CREATE TABLE rutina_ejercicios (
                                   id INT AUTO_INCREMENT PRIMARY KEY,
                                   rutina_id INT NOT NULL,
                                   ejercicio_id INT NOT NULL,
                                   series INT NOT NULL DEFAULT 3,
                                   repeticiones INT NOT NULL DEFAULT 10,
                                   peso_recomendado DECIMAL(5,2) DEFAULT NULL,
                                   tiempo_descanso INT DEFAULT NULL COMMENT 'Tiempo en segundos',
                                   orden INT NOT NULL DEFAULT 1,
                                   notas TEXT DEFAULT NULL,
                                   FOREIGN KEY (rutina_id) REFERENCES rutinas(id) ON DELETE CASCADE,
                                   FOREIGN KEY (ejercicio_id) REFERENCES ejercicios(id) ON DELETE CASCADE,
                                   INDEX idx_rutina_id (rutina_id),
                                   INDEX idx_ejercicio_id (ejercicio_id)
);

CREATE TABLE sesiones_entrenamiento (
                                        id INT AUTO_INCREMENT PRIMARY KEY,
                                        usuario_id INT NOT NULL,
                                        rutina_id INT DEFAULT NULL,
                                        fecha_inicio DATETIME NOT NULL,
                                        fecha_fin DATETIME DEFAULT NULL,
                                        duracion_minutos INT DEFAULT NULL,
                                        calorias_quemadas INT DEFAULT NULL,
                                        notas TEXT DEFAULT NULL,
                                        completada TINYINT(1) DEFAULT 0,
                                        FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                                        FOREIGN KEY (rutina_id) REFERENCES rutinas(id) ON DELETE SET NULL,
                                        INDEX idx_usuario_id (usuario_id),
                                        INDEX idx_fecha_inicio (fecha_inicio)
);

CREATE TABLE ejercicios_realizados (
                                       id INT AUTO_INCREMENT PRIMARY KEY,
                                       sesion_id INT NOT NULL,
                                       ejercicio_id INT NOT NULL,
                                       series_completadas INT DEFAULT NULL,
                                       repeticiones_reales INT DEFAULT NULL,
                                       peso_usado DECIMAL(5,2) DEFAULT NULL,
                                       tiempo_segundos INT DEFAULT NULL,
                                       notas TEXT DEFAULT NULL,
                                       FOREIGN KEY (sesion_id) REFERENCES sesiones_entrenamiento(id) ON DELETE CASCADE,
                                       FOREIGN KEY (ejercicio_id) REFERENCES ejercicios(id) ON DELETE CASCADE,
                                       INDEX idx_sesion_id (sesion_id)
);

CREATE TABLE mediciones_corporales (
                                       id INT AUTO_INCREMENT PRIMARY KEY,
                                       usuario_id INT NOT NULL,
                                       fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       peso DECIMAL(5,2) NOT NULL,
                                       altura DECIMAL(3,2) DEFAULT NULL,
                                       imc DECIMAL(4,2) DEFAULT NULL COMMENT 'Índice de Masa Corporal',
                                       grasa_corporal DECIMAL(4,2) DEFAULT NULL COMMENT 'Porcentaje de grasa',
                                       masa_muscular DECIMAL(4,2) DEFAULT NULL COMMENT 'Porcentaje de músculo',
                                       cintura DECIMAL(5,2) DEFAULT NULL COMMENT 'Centímetros',
                                       pecho DECIMAL(5,2) DEFAULT NULL COMMENT 'Centímetros',
                                       brazos DECIMAL(5,2) DEFAULT NULL COMMENT 'Centímetros',
                                       piernas DECIMAL(5,2) DEFAULT NULL COMMENT 'Centímetros',
                                       notas TEXT DEFAULT NULL,
                                       FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                                       INDEX idx_usuario_id (usuario_id),
                                       INDEX idx_fecha (fecha)
);

CREATE TABLE objetivos_personales (
                                      id INT AUTO_INCREMENT PRIMARY KEY,
                                      usuario_id INT NOT NULL,
                                      tipo_objetivo VARCHAR(50) NOT NULL,
                                      descripcion TEXT NOT NULL,
                                      valor_actual DECIMAL(10,2) DEFAULT NULL,
                                      valor_objetivo DECIMAL(10,2) NOT NULL,
                                      unidad VARCHAR(20) DEFAULT NULL,
                                      fecha_inicio DATE NOT NULL,
                                      fecha_limite DATE DEFAULT NULL,
                                      completado TINYINT(1) DEFAULT 0,
                                      fecha_completado DATETIME DEFAULT NULL,
                                      FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                                      INDEX idx_usuario_id (usuario_id),
                                      INDEX idx_completado (completado)
);

CREATE TABLE alimentos (
                           id INT AUTO_INCREMENT PRIMARY KEY,
                           nombre VARCHAR(100) NOT NULL,
                           categoria VARCHAR(50) DEFAULT NULL,
                           calorias INT NOT NULL,
                           proteinas DECIMAL(5,2) DEFAULT NULL,
                           carbohidratos DECIMAL(5,2) DEFAULT NULL,
                           grasas DECIMAL(5,2) DEFAULT NULL,
                           fibra DECIMAL(5,2) DEFAULT NULL,
                           porcion_gramos INT DEFAULT NULL,
                           descripcion TEXT DEFAULT NULL,
                           activo TINYINT(1) DEFAULT 1,
                           INDEX idx_categoria (categoria)
);

CREATE TABLE comidas (
                         id INT AUTO_INCREMENT PRIMARY KEY,
                         usuario_id INT NOT NULL,
                         fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
                         tipo_comida ENUM('Desayuno', 'Media Mañana', 'Comida', 'Merienda', 'Cena', 'Snack') NOT NULL,
                         total_calorias INT DEFAULT NULL,
                         total_proteinas DECIMAL(5,2) DEFAULT NULL,
                         total_carbohidratos DECIMAL(5,2) DEFAULT NULL,
                         total_grasas DECIMAL(5,2) DEFAULT NULL,
                         notas TEXT DEFAULT NULL,
                         FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                         INDEX idx_usuario_id (usuario_id),
                         INDEX idx_fecha (fecha),
                         INDEX idx_tipo_comida (tipo_comida)
);

CREATE TABLE alimentos_comida (
                                  id INT AUTO_INCREMENT PRIMARY KEY,
                                  comida_id INT NOT NULL,
                                  alimento_id INT NOT NULL,
                                  cantidad_gramos DECIMAL(6,2) NOT NULL,
                                  calorias_totales INT DEFAULT NULL,
                                  FOREIGN KEY (comida_id) REFERENCES comidas(id) ON DELETE CASCADE,
                                  FOREIGN KEY (alimento_id) REFERENCES alimentos(id) ON DELETE CASCADE
);

CREATE TABLE progreso_ejercicios (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     usuario_id INT NOT NULL,
                                     ejercicio_id INT NOT NULL,
                                     fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
                                     mejor_peso DECIMAL(5,2) DEFAULT NULL,
                                     mejor_repeticiones INT DEFAULT NULL,
                                     mejor_tiempo_segundos INT DEFAULT NULL,
                                     notas TEXT DEFAULT NULL,
                                     FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                                     FOREIGN KEY (ejercicio_id) REFERENCES ejercicios(id) ON DELETE CASCADE,
                                     INDEX idx_usuario_id (usuario_id),
                                     INDEX idx_ejercicio_id (ejercicio_id)
);

CREATE TABLE notificaciones (
                                id INT AUTO_INCREMENT PRIMARY KEY,
                                usuario_id INT NOT NULL,
                                titulo VARCHAR(100) NOT NULL,
                                mensaje TEXT NOT NULL,
                                tipo ENUM('Recordatorio', 'Logro', 'Objetivo', 'Sistema') NOT NULL,
                                fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
                                fecha_programada DATETIME DEFAULT NULL,
                                leida TINYINT(1) DEFAULT 0,
                                FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                                INDEX idx_usuario_id (usuario_id),
                                INDEX idx_leida (leida)
);

INSERT INTO usuarios (username, password, email, peso, altura, edad, nivel_experiencia, objetivo) VALUES
                                                                                                      ('admin', 'admin123', 'admin@gymprofit.com', 75.00, 1.75, 25, 'Avanzado', 'Mantenimiento'),
                                                                                                      ('usuario1', 'pass123', 'usuario1@gmail.com', 70.00, 1.70, 22, 'Intermedio', 'Ganar masa muscular'),
                                                                                                      ('ruben_fitness', 'ruben123', 'ruben@gmail.com', 85.00, 1.85, 19, 'Principiante', 'Perder peso');


INSERT INTO ejercicios (nombre, descripcion, grupo_muscular, dificultad, instrucciones, calorias_quemadas, equipo_necesario) VALUES
                                                                                                                                 ('Press de banca', 'Ejercicio básico para pecho', 'Pecho', 'Intermedio', 'Acostado en banco plano, baja la barra hasta el pecho', 50, 'Barra, banco'),
                                                                                                                                 ('Flexiones', 'Ejercicio de peso corporal', 'Pecho', 'Principiante', 'Posición de plancha, baja el cuerpo', 30, 'Ninguno'),
                                                                                                                                 ('Press inclinado mancuernas', 'Parte superior del pecho', 'Pecho', 'Intermedio', 'En banco inclinado 30-45°', 45, 'Mancuernas, banco'),
                                                                                                                                 ('Aperturas mancuernas', 'Aislamiento del pecho', 'Pecho', 'Intermedio', 'Abre los brazos en arco', 40, 'Mancuernas, banco');

INSERT INTO ejercicios (nombre, descripcion, grupo_muscular, dificultad, instrucciones, calorias_quemadas, equipo_necesario) VALUES
                                                                                                                                 ('Dominadas', 'Ejercicio de peso corporal', 'Espalda', 'Avanzado', 'Eleva tu cuerpo hasta que la barbilla supere la barra', 60, 'Barra de dominadas'),
                                                                                                                                 ('Remo con barra', 'Ejercicio compuesto', 'Espalda', 'Intermedio', 'Inclinado hacia adelante, tira de la barra', 55, 'Barra'),
                                                                                                                                 ('Peso muerto', 'Ejercicio compuesto', 'Espalda', 'Avanzado', 'Levanta la barra manteniendo la espalda recta', 70, 'Barra'),
                                                                                                                                 ('Jalón al pecho', 'Ejercicio de máquina', 'Espalda', 'Principiante', 'Tira de la barra hacia el pecho', 45, 'Máquina de polea');

INSERT INTO ejercicios (nombre, descripcion, grupo_muscular, dificultad, instrucciones, calorias_quemadas, equipo_necesario) VALUES
                                                                                                                                 ('Sentadillas', 'Ejercicio básico de piernas', 'Piernas', 'Intermedio', 'Baja hasta que los muslos estén paralelos', 65, 'Barra'),
                                                                                                                                 ('Prensa de piernas', 'Ejercicio de máquina', 'Piernas', 'Principiante', 'Empuja la plataforma con los pies', 60, 'Máquina de prensa'),
                                                                                                                                 ('Zancadas', 'Ejercicio unilateral', 'Piernas', 'Intermedio', 'Da un paso largo hacia adelante', 50, 'Mancuernas'),
                                                                                                                                 ('Curl femoral', 'Aislamiento isquiotibiales', 'Piernas', 'Principiante', 'Flexiona las piernas llevando talones a glúteos', 35, 'Máquina');

INSERT INTO ejercicios (nombre, descripcion, grupo_muscular, dificultad, instrucciones, calorias_quemadas, equipo_necesario) VALUES
                                                                                                                                 ('Press militar', 'Ejercicio básico hombros', 'Hombros', 'Intermedio', 'Empuja la barra desde los hombros', 50, 'Barra'),
                                                                                                                                 ('Elevaciones laterales', 'Aislamiento deltoides', 'Hombros', 'Principiante', 'Levanta los brazos a los lados', 30, 'Mancuernas'),
                                                                                                                                 ('Elevaciones frontales', 'Deltoides anterior', 'Hombros', 'Principiante', 'Levanta mancuernas al frente', 30, 'Mancuernas'),
                                                                                                                                 ('Pájaros', 'Deltoides posterior', 'Hombros', 'Intermedio', 'Inclinado, abre los brazos', 35, 'Mancuernas');

INSERT INTO ejercicios (nombre, descripcion, grupo_muscular, dificultad, instrucciones, calorias_quemadas, equipo_necesario) VALUES
                                                                                                                                 ('Curl bíceps barra', 'Ejercicio básico bíceps', 'Brazos', 'Principiante', 'Flexiona los codos levantando la barra', 25, 'Barra'),
                                                                                                                                 ('Curl martillo', 'Bíceps y antebrazo', 'Brazos', 'Principiante', 'Agarre neutro, flexiona codos', 25, 'Mancuernas'),
                                                                                                                                 ('Extensiones tríceps', 'Ejercicio básico tríceps', 'Brazos', 'Principiante', 'Extiende codos empujando peso', 30, 'Polea o mancuerna'),
                                                                                                                                 ('Fondos paralelas', 'Ejercicio avanzado tríceps', 'Brazos', 'Avanzado', 'En barras paralelas, baja el cuerpo', 45, 'Barras paralelas');

INSERT INTO ejercicios (nombre, descripcion, grupo_muscular, dificultad, instrucciones, calorias_quemadas, equipo_necesario) VALUES
                                                                                                                                 ('Crunches', 'Abdominales superiores', 'Abdomen', 'Principiante', 'Eleva el torso contrayendo abdominales', 20, 'Ninguno'),
                                                                                                                                 ('Plancha', 'Ejercicio isométrico', 'Abdomen', 'Intermedio', 'Mantén posición de flexión', 25, 'Ninguno'),
                                                                                                                                 ('Elevación piernas', 'Abdominales inferiores', 'Abdomen', 'Intermedio', 'Eleva piernas rectas hasta 90 grados', 30, 'Ninguno'),
                                                                                                                                 ('Bicicleta', 'Ejercicio para oblicuos', 'Abdomen', 'Principiante', 'Simula pedaleo con codo a rodilla', 35, 'Ninguno');

INSERT INTO ejercicios (nombre, descripcion, grupo_muscular, dificultad, instrucciones, calorias_quemadas, equipo_necesario) VALUES
                                                                                                                                 ('Carrera cinta', 'Cardio bajo impacto', 'Cardio', 'Principiante', 'Corre en cinta a ritmo moderado', 100, 'Cinta de correr'),
                                                                                                                                 ('Burpees', 'Cardio intenso', 'Cardio', 'Avanzado', 'Baja a flexión, salta hacia arriba', 120, 'Ninguno'),
                                                                                                                                 ('Saltar cuerda', 'Cardio alta intensidad', 'Cardio', 'Intermedio', 'Salta coordinando giro de cuerda', 110, 'Cuerda');

INSERT INTO rutinas (usuario_id, nombre, descripcion, duracion_minutos, nivel, es_predefinida, categoria, dias_semana) VALUES
                                                                                                                           (NULL, 'Full Body Principiante', 'Rutina completa 3 días por semana', 45, 'Principiante', 1, 'Full Body', 'Lunes,Miércoles,Viernes'),
                                                                                                                           (NULL, 'Upper Body Intermedio', 'Tren superior nivel intermedio', 60, 'Intermedio', 1, 'Upper Body', 'Lunes,Jueves'),
                                                                                                                           (NULL, 'Lower Body Avanzado', 'Rutina intensa de piernas', 50, 'Avanzado', 1, 'Lower Body', 'Martes,Viernes'),
                                                                                                                           (NULL, 'Push - Empuje', 'Pecho, hombros y tríceps', 55, 'Intermedio', 1, 'Push/Pull/Legs', 'Lunes,Jueves'),
                                                                                                                           (NULL, 'Pull - Tirón', 'Espalda y bíceps', 55, 'Intermedio', 1, 'Push/Pull/Legs', 'Martes,Viernes'),
                                                                                                                           (2, 'Mi rutina Pecho-Brazos', 'Rutina personalizada volumen', 40, 'Intermedio', 0, 'Upper Body', 'Lunes,Miércoles');

INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, series, repeticiones, tiempo_descanso, orden) VALUES
                                                                                                          (1, 2, 3, 12, 60, 1),
                                                                                                          (1, 9, 3, 10, 90, 2),
                                                                                                          (1, 6, 3, 8, 90, 3),
                                                                                                          (1, 13, 3, 12, 60, 4),
                                                                                                          (1, 21, 3, 15, 45, 5);

INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, series, repeticiones, peso_recomendado, tiempo_descanso, orden) VALUES
                                                                                                                            (2, 1, 4, 10, 60.00, 90, 1),
                                                                                                                            (2, 5, 4, 8, NULL, 120, 2),
                                                                                                                            (2, 13, 3, 12, 40.00, 60, 3),
                                                                                                                            (2, 17, 3, 12, 20.00, 60, 4);

INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, series, repeticiones, peso_recomendado, tiempo_descanso, orden) VALUES
                                                                                                                            (6, 1, 4, 10, 60.00, 90, 1),
                                                                                                                            (6, 3, 3, 12, 20.00, 60, 2),
                                                                                                                            (6, 17, 3, 12, 15.00, 60, 3),
                                                                                                                            (6, 19, 3, 12, 25.00, 60, 4);

INSERT INTO alimentos (nombre, categoria, calorias, proteinas, carbohidratos, grasas, fibra, porcion_gramos) VALUES
                                                                                                                 ('Pechuga de pollo', 'Proteínas', 165, 31.00, 0.00, 3.60, 0.00, 100),
                                                                                                                 ('Arroz blanco', 'Carbohidratos', 130, 2.70, 28.00, 0.30, 0.40, 100),
                                                                                                                 ('Brócoli', 'Verduras', 34, 2.80, 7.00, 0.40, 2.60, 100),
                                                                                                                 ('Plátano', 'Frutas', 89, 1.10, 23.00, 0.30, 2.60, 100),
                                                                                                                 ('Huevos', 'Proteínas', 155, 13.00, 1.10, 11.00, 0.00, 100),
                                                                                                                 ('Avena', 'Carbohidratos', 389, 16.90, 66.00, 6.90, 10.60, 100),
                                                                                                                 ('Salmón', 'Proteínas', 208, 20.00, 0.00, 13.00, 0.00, 100),
                                                                                                                 ('Batata', 'Carbohidratos', 86, 1.60, 20.00, 0.10, 3.00, 100);

INSERT INTO mediciones_corporales (usuario_id, fecha, peso, altura, imc, grasa_corporal, masa_muscular) VALUES
                                                                                                            (2, DATE_SUB(NOW(), INTERVAL 30 DAY), 72.00, 1.70, 24.90, 18.00, 35.00),
                                                                                                            (2, DATE_SUB(NOW(), INTERVAL 15 DAY), 71.00, 1.70, 24.60, 17.50, 35.50),
                                                                                                            (2, NOW(), 70.00, 1.70, 24.20, 17.00, 36.00);

INSERT INTO objetivos_personales (usuario_id, tipo_objetivo, descripcion, valor_actual, valor_objetivo, unidad, fecha_inicio, fecha_limite) VALUES
                                                                                                                                                (2, 'Peso', 'Alcanzar 68kg', 70.00, 68.00, 'kg', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 60 DAY)),
                                                                                                                                                (2, 'Fuerza', 'Press de banca 80kg', 60.00, 80.00, 'kg', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY));

INSERT INTO sesiones_entrenamiento (usuario_id, rutina_id, fecha_inicio, fecha_fin, duracion_minutos, calorias_quemadas, completada) VALUES
                                                                                                                                         (2, 6, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 45 MINUTE), 45, 250, 1),
                                                                                                                                         (2, 6, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 40 MINUTE), 40, 220, 1);

INSERT INTO ejercicios_realizados (sesion_id, ejercicio_id, series_completadas, repeticiones_reales, peso_usado) VALUES
                                                                                                                     (1, 1, 4, 10, 60.00),
                                                                                                                     (1, 3, 3, 12, 20.00),
                                                                                                                     (1, 17, 3, 12, 15.00);

INSERT INTO notificaciones (usuario_id, titulo, mensaje, tipo, fecha_programada) VALUES
                                                                                     (2, '¡Hora de entrenar!', 'Tu rutina de pecho te espera', 'Recordatorio', DATE_ADD(NOW(), INTERVAL 1 HOUR)),
                                                                                     (2, '¡Nuevo logro!', 'Has completado 10 entrenamientos este mes', 'Logro', NOW());

SHOW TABLES;

SELECT
    TABLE_NAME AS 'Tabla',
    TABLE_ROWS AS 'Filas',
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024, 2) AS 'Tamaño (KB)'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'GymProFitDB'
ORDER BY TABLE_NAME;