package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.Locale;

// ============================================================
// ResumenPerfil — qué enseñan «Tu objetivo» y «Tus datos» en Perfil (GP-265)
//
// Una cuenta sin objetivo (alta sin él, cuestionario sin acabar) veía «Tu objetivo» con
// el nombre vacío, y «Tus datos» con 70 kg, 170 cm y «Moderada»: los valores por defecto
// de los getters de PreferencesManager, no datos suyos. Aquí se separa lo que la cuenta
// tiene de lo que no, para que la pantalla enseñe el vacío en vez de inventar.
// ============================================================
public final class ResumenPerfil {

    private ResumenPerfil() { }

    /**
     * El objetivo que se enseña: el de la API si lo trae y, mientras llega, el guardado.
     *
     * @return uno de los cuatro de {@link CalculadoraNutricional}, o null si no hay
     *         ninguno (o es un valor que la app no conoce): entonces va el estado vacío.
     */
    @Nullable
    public static String objetivo(@Nullable String deLaApi, @Nullable String guardado) {
        String api = conocido(deLaApi);
        return api != null ? api : conocido(guardado);
    }

    /**
     * Dice si salen las fichas de kcal y proteína: solo con objetivo y con el plan que la
     * app ya calcula (DiaNutricion.objetivo); sin datos para calcularlo, no se enseñan.
     */
    public static boolean conFichas(@Nullable String objetivo, boolean hayPlan) {
        return objetivo != null && hayPlan;
    }

    /**
     * Un peso o una altura guardados, leídos con {@code PreferencesManager.leer}.
     *
     * @return el valor, o null si no hay nada guardado (nunca el de serie).
     */
    @Nullable
    public static Double medida(@Nullable String leido) {
        if (leido == null) return null;
        try {
            double v = Double.parseDouble(leido.trim());
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * La edad: la de la API si la hay, y si no la guardada.
     *
     * @return la edad, o null si no se sabe (nunca los 25 de serie).
     */
    @Nullable
    public static Integer edad(@Nullable Integer deLaApi, @Nullable String leido) {
        if (deLaApi != null && deLaApi > 0) return deLaApi;
        Double guardada = medida(leido);
        return guardada == null ? null : (int) Math.round(guardada);
    }

    @Nullable
    private static String conocido(@Nullable String objetivo) {
        if (objetivo == null) return null;
        String o = objetivo.trim().toUpperCase(Locale.ROOT);
        switch (o) {
            case CalculadoraNutricional.OBJETIVO_PERDER_PESO:
            case CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR:
            case CalculadoraNutricional.OBJETIVO_MANTENER_PESO:
            case CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA:
                return o;
            default:
                return null;
        }
    }
}
