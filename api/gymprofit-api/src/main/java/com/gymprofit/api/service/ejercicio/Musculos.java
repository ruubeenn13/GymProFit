package com.gymprofit.api.service.ejercicio;

// ============================================================
// Musculos — las claves de músculo que entiende la app.
//
// Vivía dentro de SesionEntrenamientoService para la silueta de Home. GP-088 la
// necesita también para agrupar los récords por zona, y dos copias de esta tabla
// acabarían diciendo cosas distintas: por eso está aquí, una sola vez.
// ============================================================
public final class Musculos {

    private Musculos() { }

    /**
     * Reduce el músculo de un ejercicio a una de las claves que la silueta sabe pintar.
     * <p>
     * Manda el músculo primario. Cuando falta —pasa en buena parte del catálogo
     * importado de wger— se cae al grupo grueso y se elige el músculo más
     * representativo de ese grupo, que es preferible a no pintar nada.
     *
     * @return clave normalizada, o {@code null} si no hay nada que pintar (CARDIO).
     */
    public static String normalizar(String musculoPrimario, Object grupo) {
        if (musculoPrimario != null && !musculoPrimario.isBlank()) {
            String limpio = sinTildes(musculoPrimario.trim().toLowerCase());
            switch (limpio) {
                case "abdominales":     return "abdominales";
                case "aductores":       return "aductores";
                case "abductores":      return "gluteos";
                case "biceps":          return "biceps";
                case "gemelos":         return "gemelos";
                case "pecho":           return "pecho";
                case "antebrazos":      return "antebrazos";
                case "gluteos":         return "gluteos";
                case "isquiotibiales":  return "isquiotibiales";
                case "dorsales":
                case "espalda media":   return "dorsales";
                case "lumbares":        return "lumbares";
                case "cuello":          return "cuello";
                case "cuadriceps":      return "cuadriceps";
                case "hombros":         return "hombros";
                case "trapecios":       return "trapecios";
                case "triceps":         return "triceps";
                default:                break;   // cae al grupo grueso
            }
        }

        if (grupo == null) return null;

        switch (grupo.toString().toUpperCase()) {
            case "PECHO":    return "pecho";
            case "ESPALDA":  return "dorsales";
            case "PIERNAS":  return "cuadriceps";
            case "HOMBROS":  return "hombros";
            case "BRAZOS":   return "biceps";
            case "ABDOMEN":  return "abdominales";
            case "FULLBODY": return "pecho";
            // CARDIO no tiene músculo que encender: no se inventa uno.
            default:         return null;
        }
    }

    // Quita las tildes para que "bíceps" y "biceps" sean el mismo músculo.
    private static String sinTildes(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
