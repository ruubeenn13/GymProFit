package com.gymprofit.api.enums;

// ============================================================
// TipoObjetivo — tipos de objetivos personales que puede fijarse un usuario.
//
// Son cuatro a propósito. Antes había doce, pero seis de ellos (resistencia,
// flexibilidad, velocidad, movilidad, reto y "otro") no cambiaban el plan
// nutricional: todos acababan en calorías de mantenimiento con pequeños retoques
// de macros, así que el usuario elegía entre doce tarjetas para llegar a cuatro
// resultados distintos. "Reducir grasa corporal" y "perder peso" eran además el
// mismo objetivo contado de dos maneras.
//
// Cada valor que queda produce un cálculo calórico DISTINTO. Si algún día se
// añade otro, que sea porque cambia el cálculo, no porque suene bien.
//
// La poda de los valores antiguos y la reasignación de los datos existentes
// están en la migración V202609181900__Poda_objetivos_a_cuatro.sql.
// ============================================================
public enum TipoObjetivo {
    /** Déficit calórico. Absorbe al antiguo REDUCIR_GRASA_CORPORAL. */
    PERDER_PESO,
    /** Superávit alto con proteína muy alta. Absorbe al antiguo AUMENTAR_CALORIAS. */
    GANAR_MASA_MUSCULAR,
    /** Calorías de mantenimiento. Destino de los objetivos sin intención calórica. */
    MANTENER_PESO,
    /** Superávit moderado con la proteína más alta de los cuatro. */
    MEJORAR_FUERZA
}
