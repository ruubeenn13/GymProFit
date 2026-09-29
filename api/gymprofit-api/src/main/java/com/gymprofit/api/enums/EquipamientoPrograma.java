package com.gymprofit.api.enums;

// ============================================================
// EquipamientoPrograma — con qué material se hace un programa (GP-074)
// Es lo que pregunta el alta, no el aparato de cada ejercicio (eso es Equipamiento):
// un programa de gimnasio usa barras, máquinas y poleas; uno de mancuernas, mancuernas
// y un banco; uno de peso corporal, una barra de dominadas y una silla o un banco.
// ============================================================
public enum EquipamientoPrograma {
    GIMNASIO,
    MANCUERNAS,
    PESO_CORPORAL
}
