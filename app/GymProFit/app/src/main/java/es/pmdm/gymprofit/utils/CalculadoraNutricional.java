package es.pmdm.gymprofit.utils;

// MODIFICADO - Constantes de objetivo actualizadas para coincidir con el enum
// TipoObjetivo de la API. Añadidos: REDUCIR_GRASA_CORPORAL, MEJORAR_FLEXIBILIDAD,
// MEJORAR_VELOCIDAD, AUMENTAR_CALORIAS, MEJORAR_MOVILIDAD
// ============================================================
// CalculadoraNutricional — utilidad estática para calcular objetivos nutricionales diarios.
// Aplica la fórmula de Mifflin-St Jeor para el TMB, un factor de actividad física
// para el TDEE, y reparte las calorías en macros según el objetivo del usuario.
// ============================================================
public class CalculadoraNutricional {

    // Niveles de actividad física soportados (deben coincidir con los usados por la API).
    public static final String ACTIVIDAD_SEDENTARIO = "SEDENTARIO";
    public static final String ACTIVIDAD_LIGERO     = "LIGERO";
    public static final String ACTIVIDAD_MODERADO   = "MODERADO";
    public static final String ACTIVIDAD_ACTIVO     = "ACTIVO";

    // Valores exactos del enum TipoObjetivo de la API
    public static final String OBJETIVO_PERDER_PESO          = "PERDER_PESO";
    public static final String OBJETIVO_GANAR_MASA_MUSCULAR  = "GANAR_MASA_MUSCULAR";
    public static final String OBJETIVO_MANTENER_PESO        = "MANTENER_PESO";
    public static final String OBJETIVO_MEJORAR_FUERZA       = "MEJORAR_FUERZA";

    // Fórmula Mifflin-St Jeor para calcular el TMB (Tasa Metabólica Basal)
    private static double calcularTMB(double pesoKg, double alturaCm, int edad, boolean esHombre) {
        double base = (10 * pesoKg) + (6.25 * alturaCm) - (5 * edad);

        return esHombre ? base + 5 : base - 161;
    }

    // Devuelve el multiplicador de actividad física para pasar de TMB a TDEE.
    private static double factorActividad(String actividad) {
        switch (actividad) {
            case ACTIVIDAD_SEDENTARIO: return 1.2;
            case ACTIVIDAD_LIGERO:     return 1.375;
            case ACTIVIDAD_ACTIVO:     return 1.725;
            default:                   return 1.55;
        }
    }

    /** Hasta esta edad, incluida, «Perder grasa» no lleva déficit (GP-103). */
    public static final int EDAD_SIN_DEFICIT = 17;

    // Calcula calorías diarias y reparto de macros (proteínas/carbos/grasas) más el
    // agua recomendada, según los datos físicos del usuario, su actividad y su objetivo.
    //
    // De 14 a 17 años, «Perder grasa» da las de mantenimiento (decisión 6 del lienzo del
    // alta): un déficit a esa edad es cosa de un profesional, no de una fórmula. Va aquí,
    // y no en la pantalla del plan, para que valga también al recalcular desde Editar
    // perfil y en Nutrición. El resultado lo marca para que las pantallas lo digan.
    public static ResultadoNutricional calcular(double pesoKg, double alturaCm, int edad, boolean esHombre, String actividad, String objetivo) {
        double tmb  = calcularTMB(pesoKg, alturaCm, edad, esHombre);
        double tdee = tmb * factorActividad(actividad);

        int calorias, proteinas, carbohidratos, grasas;

        boolean sinDeficit = OBJETIVO_PERDER_PESO.equals(objetivo) && edad > 0 && edad <= EDAD_SIN_DEFICIT;
        String calculo = sinDeficit ? OBJETIVO_MANTENER_PESO : (objetivo == null ? "" : objetivo);

        switch (calculo) {
            case OBJETIVO_PERDER_PESO:
                // Déficit del 20%, proteína alta para preservar músculo
                calorias      = (int) (tdee * 0.80);
                proteinas     = (int) (pesoKg * 2.0);
                grasas        = (int) (calorias * 0.25 / 9);
                carbohidratos = (int) ((calorias - proteinas * 4 - grasas * 9) / 4);
                break;

            case OBJETIVO_GANAR_MASA_MUSCULAR:
                // Superávit del 15%, proteína muy alta para construir músculo
                calorias      = (int) (tdee * 1.15);
                proteinas     = (int) (pesoKg * 2.2);
                grasas        = (int) (calorias * 0.25 / 9);
                carbohidratos = (int) ((calorias - proteinas * 4 - grasas * 9) / 4);
                break;

            case OBJETIVO_MEJORAR_FUERZA:
                // Superávit del 10%, proteína muy alta para soportar cargas pesadas
                calorias      = (int) (tdee * 1.10);
                proteinas     = (int) (pesoKg * 2.5);
                grasas        = (int) (calorias * 0.28 / 9);
                carbohidratos = (int) ((calorias - proteinas * 4 - grasas * 9) / 4);
                break;

            default:
                // Calorías de mantenimiento con distribución equilibrada
                calorias      = (int) tdee;
                proteinas     = (int) (pesoKg * 1.8);
                grasas        = (int) (calorias * 0.25 / 9);
                carbohidratos = (int) ((calorias - proteinas * 4 - grasas * 9) / 4);
                break;
        }

        // Agua recomendada: 35ml por kg de peso corporal
        double agua = Math.round(pesoKg * 0.035 * 10.0) / 10.0;

        ResultadoNutricional r = new ResultadoNutricional(
                calorias,
                proteinas,
                Math.max(carbohidratos, 0),
                Math.max(grasas, 0),
                agua
        );
        r.mantenimientoPorEdad = sinDeficit;
        return r;
    }
}