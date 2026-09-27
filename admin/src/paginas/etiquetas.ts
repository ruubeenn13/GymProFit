// Nombres en español de las listas cerradas del catálogo, los mismos que la app
// (values/strings.xml: grupo_*, dificultad). El equipamiento llega con su
// etiqueta desde /admin/ejercicios/resumen.

export const GRUPOS: Record<string, string> = {
  PECHO: 'Pecho',
  ESPALDA: 'Espalda',
  PIERNAS: 'Piernas',
  HOMBROS: 'Hombros',
  BRAZOS: 'Brazos',
  ABDOMEN: 'Abdomen',
  CARDIO: 'Cardio',
  FULLBODY: 'Cuerpo completo',
};

export const DIFICULTADES: Record<string, string> = {
  PRINCIPIANTE: 'Principiante',
  INTERMEDIO: 'Intermedio',
  AVANZADO: 'Avanzado',
};

export const ORIGEN_EJERCICIO: Record<string, string> = {
  WGER: 'Del catálogo de wger',
  FREE_EXERCISE_DB: 'Del catálogo de free-exercise-db',
  MANUAL: 'Creado a mano',
};
