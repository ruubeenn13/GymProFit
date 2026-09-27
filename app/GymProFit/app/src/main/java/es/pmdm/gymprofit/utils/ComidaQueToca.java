package es.pmdm.gymprofit.utils;

import androidx.annotation.StringRes;

import es.pmdm.gymprofit.R;

// ============================================================
// ComidaQueToca — qué comida toca por la hora (GP-105).
//
// La usan el atajo «Registrar comida» del «+» y el «+» naranja de Nutrición, así que
// la regla vive en un solo sitio:
//   antes de las 10:00 → desayuno; hasta las 12:59 → almuerzo; hasta las 16:29 →
//   comida; hasta las 19:59 → merienda; después → cena.
// Los tipos son los de la API (DESAYUNO…CENA), en el orden del día.
// ============================================================
public final class ComidaQueToca {

    /** Las cinco comidas de la app, en el orden del día. */
    public static final String[] TIPOS = {"DESAYUNO", "ALMUERZO", "COMIDA", "MERIENDA", "CENA"};

    private ComidaQueToca() { }

    /**
     * La comida que toca a una hora del día.
     *
     * @param hora   0..23
     * @param minuto 0..59
     * @return uno de {@link #TIPOS}.
     */
    public static String segunHora(int hora, int minuto) {
        int m = hora * 60 + minuto;
        if (m < 10 * 60) return "DESAYUNO";
        if (m <= 12 * 60 + 59) return "ALMUERZO";
        if (m <= 16 * 60 + 29) return "COMIDA";
        if (m <= 19 * 60 + 59) return "MERIENDA";
        return "CENA";
    }

    /** La que toca ahora, con el reloj del teléfono. */
    public static String ahora() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        return segunHora(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE));
    }

    /** Nombre de la comida con mayúscula, para títulos («Cena»). */
    @StringRes
    public static int titulo(String tipo) {
        switch (tipo) {
            case "DESAYUNO": return R.string.nutricion_desayuno;
            case "ALMUERZO": return R.string.nutricion_almuerzo;
            case "COMIDA":   return R.string.nutricion_comida;
            case "MERIENDA": return R.string.nutricion_merienda;
            default:         return R.string.nutricion_cena;
        }
    }

    /** Nombre de la comida en minúscula, para dentro de una frase («Ahora toca: cena»). */
    @StringRes
    public static int enFrase(String tipo) {
        switch (tipo) {
            case "DESAYUNO": return R.string.comida_min_desayuno;
            case "ALMUERZO": return R.string.comida_min_almuerzo;
            case "COMIDA":   return R.string.comida_min_comida;
            case "MERIENDA": return R.string.comida_min_merienda;
            default:         return R.string.comida_min_cena;
        }
    }
}
