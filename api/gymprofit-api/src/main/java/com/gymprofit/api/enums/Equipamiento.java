package com.gymprofit.api.enums;

import java.util.Locale;

// ============================================================
// Equipamiento — con qué se hace un ejercicio, como lista cerrada (GP-085)
// Sustituye para filtrar al texto libre de equipo_necesario, que viene de la
// importación con 36 formas distintas de decir lo mismo. El relleno inicial está en
// la migración V202609271301; lo que se importa después pasa por
// desdeEquipoNecesario, con el mismo criterio (GP-122).
// ============================================================
public enum Equipamiento {
    BARRA("Barra", "Barbell"),
    MANCUERNAS("Mancuernas", "Dumbbells"),
    KETTLEBELL("Kettlebell", "Kettlebell"),
    MAQUINA("Máquina", "Machine"),
    POLEA("Polea", "Cable"),
    PESO_CORPORAL("Peso corporal", "Bodyweight"),
    BANDA("Banda", "Resistance band"),
    OTRO("Otro", "Other");

    private final String etiqueta;
    private final String etiquetaEn;

    Equipamiento(String etiqueta, String etiquetaEn) {
        this.etiqueta = etiqueta;
        this.etiquetaEn = etiquetaEn;
    }

    /** Nombre en español. */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Nombre en inglés. */
    public String getEtiquetaEn() {
        return etiquetaEn;
    }

    /**
     * El equipamiento que corresponde al texto libre de equipo_necesario, con el mismo
     * criterio que la migración V202609271301: con varios aparatos gana el primero de
     * barra, mancuernas, kettlebell, polea, máquina, banda y peso corporal; lo que no
     * casa con ninguno es OTRO. La barra de dominadas cuenta como peso corporal, y la
     * polea va antes que la máquina porque la importación llama «Cable machine» a la polea.
     *
     * @param equipo   equipo_necesario, en español (puede ser null)
     * @param equipoEn equipo_necesario_en, en inglés (puede ser null)
     * @return nunca null
     */
    public static Equipamiento desdeEquipoNecesario(String equipo, String equipoEn) {
        String t = String.join(" | ", equipo == null ? "" : equipo, equipoEn == null ? "" : equipoEn)
                .toLowerCase(Locale.ROOT)
                .replace("barra de dominadas", "dominadas")
                .replace("barra dominadas", "dominadas")
                .replace("pull-up bar", "dominadas");
        if (contiene(t, "barra", "barbell")) return BARRA;
        if (contiene(t, "mancuerna", "dumbbell")) return MANCUERNAS;
        if (contiene(t, "kettlebell")) return KETTLEBELL;
        if (contiene(t, "polea", "cable")) return POLEA;
        if (contiene(t, "quina", "machine", "cinta de correr")) return MAQUINA;
        if (contiene(t, "banda", "band")) return BANDA;
        if (contiene(t, "sin equipo", "bodyweight", "no equipment", "dominadas", "esterilla")) return PESO_CORPORAL;
        return OTRO;
    }

    private static boolean contiene(String texto, String... trozos) {
        for (String trozo : trozos) {
            if (texto.contains(trozo)) return true;
        }
        return false;
    }
}
