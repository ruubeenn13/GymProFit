-- ============================================================================
-- Se retira progreso_ejercicios (GP-088).
--
-- La tabla guardaba el "mejor peso" de cada usuario en cada ejercicio, pero
-- ningún flujo real la escribía: solo su CRUD, que la app no usaba, y el guion
-- de datos de ejemplo. La app sí la LEÍA para la tarjeta de récord de inicio y
-- para la gráfica de la ficha de ejercicio, así que un usuario que solo
-- entrenaba no veía nunca ni una cosa ni la otra.
--
-- Los récords pasan a calcularse al pedirlos, a partir de series_realizadas. Se
-- decidió midiendo: con 450 sesiones y 10 800 series de un mismo usuario la
-- consulta tarda 11,4 ms, y guardarlos obligaría a recalcularlos en cada borrado
-- o edición de una sesión, cada uno un sitio donde un récord guardado puede
-- quedarse mintiendo.
--
-- Las filas que hubiera se pierden a propósito: eran datos sembrados a mano, sin
-- sesión detrás, y no pueden colarse como récords.
-- ============================================================================

DROP TABLE IF EXISTS progreso_ejercicios;
