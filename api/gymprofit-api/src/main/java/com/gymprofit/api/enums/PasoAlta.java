package com.gymprofit.api.enums;

// ============================================================
// PasoAlta — las pantallas del alta que cuenta el embudo (GP-019, DEC-064), en orden.
//
// Son las del alta de verdad, de la bienvenida a «¿Te avisamos?». CUENTA es «Todo
// listo»: la cuenta ya existe. Un nombre que no esté aquí se rechaza con 400, así que la
// tabla no se llena de lo que alguien quiera mandar. Cambiar un nombre rompe las builds
// que ya avisan: solo se añade.
// ============================================================
public enum PasoAlta {
    BIENVENIDA,
    OBJETIVO,
    NIVEL,
    SOBRE_TI,
    DONDE,
    DIAS,
    PLAN,
    GUARDA,
    CUENTA,
    AVISOS
}
