package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.Locale;

// ============================================================
// Musculos — de un ejercicio a la clave de músculo que entiende la silueta (GP-216).
//
// Es la misma tabla que Musculos de la API (service/ejercicio/Musculos.java), que reduce así
// cada ejercicio en /volumen-muscular y en los récords. La app la necesita para las zonas de
// «Hoy toca»: los ejercicios de una rutina llegan crudos y la API no ofrece sus zonas. Si la
// tabla de la API cambia, esta cambia con ella.
// ============================================================
public final class Musculos {

    private Musculos() { }

    /**
     * Reduce el músculo de un ejercicio a una de las claves que sabe pintar la silueta.
     * Manda el músculo primario; sin él, el músculo más representativo del grupo grueso.
     *
     * @return la clave, o null si no hay nada que pintar (cardio o ejercicio sin datos).
     */
    @Nullable
    public static String normalizar(@Nullable String musculoPrimario, @Nullable String grupo) {
        if (musculoPrimario != null && !musculoPrimario.trim().isEmpty()) {
            switch (sinTildes(musculoPrimario.trim().toLowerCase(Locale.ROOT))) {
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
        switch (grupo.toUpperCase(Locale.ROOT)) {
            case "PECHO":    return "pecho";
            case "ESPALDA":  return "dorsales";
            case "PIERNAS":  return "cuadriceps";
            case "HOMBROS":  return "hombros";
            case "BRAZOS":   return "biceps";
            case "ABDOMEN":  return "abdominales";
            case "FULLBODY": return "pecho";
            default:         return null;    // el cardio no enciende ningún músculo
        }
    }

    // Quita las tildes para que «bíceps» y «biceps» sean el mismo músculo.
    private static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
