-- GP-221 · Tabla de bloqueos de ShedLock.
--
-- Con dos instancias de la API, cada una tiene su propio planificador y las tareas
-- @Scheduled correrían dos veces: dos recordatorios iguales, dos pushes de la misma
-- notificación programada. ShedLock guarda aquí una fila por tarea; la instancia que
-- consigue escribirla corre, la otra se salta ese ciclo.
--
-- Es el esquema que pide ShedLock para MySQL y MariaDB (name tiene que ser la clave
-- primaria). lock_until lleva DEFAULT explícito para que un servidor con
-- explicit_defaults_for_timestamp apagado no le añada ON UPDATE CURRENT_TIMESTAMP a la
-- primera columna TIMESTAMP. Las horas las escribe la propia base (usingDbTime), en UTC.
--
-- Sin FK ni entidad JPA: ddl-auto=validate no la mira y la versión anterior de la API
-- sigue arrancando si se vuelve atrás.
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_at  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
