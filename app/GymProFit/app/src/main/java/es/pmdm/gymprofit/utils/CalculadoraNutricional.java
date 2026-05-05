package es.pmdm.gymprofit.utils;

// MODIFICADO - Constantes de objetivo actualizadas para coincidir con el enum
// TipoObjetivo de la API. Añadidos: REDUCIR_GRASA_CORPORAL, MEJORAR_FLEXIBILIDAD,
// MEJORAR_VELOCIDAD, AUMENTAR_CALORIAS, MEJORAR_MOVILIDAD
public class CalculadoraNutricional {

    public static final String ACTIVIDAD_SEDENTARIO = "SEDENTARIO";
    public static final String ACTIVIDAD_LIGERO     = "LIGERO";
    public static final String ACTIVIDAD_MODERADO   = "MODERADO";
    public static final String ACTIVIDAD_ACTIVO     = "ACTIVO";

    // Valores exactos del enum TipoObjetivo de la API
    public static final String OBJETIVO_PERDER_PESO          = "PERDER_PESO";
    public static final String OBJETIVO_GANAR_MASA_MUSCULAR  = "GANAR_MASA_MUSCULAR";
    public static final String OBJETIVO_MANTENER_PESO        = "MANTENER_PESO";
    public static final String OBJETIVO_MEJORAR_RESISTENCIA  = "MEJORAR_RESISTENCIA";
    public static final String OBJETIVO_MEJORAR_FUERZA       = "MEJORAR_FUERZA";
    public static final String OBJETIVO_REDUCIR_GRASA        = "REDUCIR_GRASA_CORPORAL";
    public static final String OBJETIVO_MEJORAR_FLEXIBILIDAD = "MEJORAR_FLEXIBILIDAD";
    public static final String OBJETIVO_MEJORAR_VELOCIDAD    = "MEJORAR_VELOCIDAD";
    public static final String OBJETIVO_AUMENTAR_CALORIAS    = "AUMENTAR_CALORIAS";
    public static final String OBJETIVO_MEJORAR_MOVILIDAD    = "MEJORAR_MOVILIDAD";

    // Fórmula Mifflin-St Jeor para calcular el TMB (Tasa Metabólica Basal)
    private static double calcularTMB(double pesoKg, double alturaCm, int edad, boolean esHombre) {
        double base = (10 * pesoKg) + (6.25 * alturaCm) - (5 * edad);

        return esHombre ? base + 5 : base - 161;
    }

    private static double factorActividad(String actividad) {
        switch (actividad) {
            case ACTIVIDAD_SEDENTARIO: return 1.2;
            case ACTIVIDAD_LIGERO:     return 1.375;
            case ACTIVIDAD_ACTIVO:     return 1.725;
            default:                   return 1.55;
        }
    }

    public static ResultadoNutricional calcular(double pesoKg, double alturaCm, int edad, boolean esHombre, String actividad, String objetivo) {
        double tmb  = calcularTMB(pesoKg, alturaCm, edad, esHombre);
        double tdee = tmb * factorActividad(actividad);

        int calorias, proteinas, carbohidratos, grasas;

        switch (objetivo) {
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

            case OBJETIVO_MEJORAR_RESISTENCIA:
                // Mantenimiento con más carbohidratos (55%) para la energía aeróbica
                calorias      = (int) tdee;
                proteinas     = (int) (pesoKg * 1.6);
                carbohidratos = (int) (calorias * 0.55 / 4);
                grasas        = (int) ((calorias - proteinas * 4 - carbohidratos * 4) / 9);
                break;

            case OBJETIVO_MEJORAR_FUERZA:
                // Superávit del 10%, proteína muy alta para soportar cargas pesadas
                calorias      = (int) (tdee * 1.10);
                proteinas     = (int) (pesoKg * 2.5);
                grasas        = (int) (calorias * 0.28 / 9);
                carbohidratos = (int) ((calorias - proteinas * 4 - grasas * 9) / 4);
                break;

            case OBJETIVO_REDUCIR_GRASA:
                // Déficit del 18%, proteína muy alta para preservar masa magra en definición
                calorias      = (int) (tdee * 0.82);
                proteinas     = (int) (pesoKg * 2.2);
                grasas        = (int) (calorias * 0.22 / 9);
                carbohidratos = (int) ((calorias - proteinas * 4 - grasas * 9) / 4);
                break;

            case OBJETIVO_AUMENTAR_CALORIAS:
                // Superávit del 20%, distribución equilibrada para ganancia de peso saludable
                calorias      = (int) (tdee * 1.20);
                proteinas     = (int) (pesoKg * 1.8);
                grasas        = (int) (calorias * 0.28 / 9);
                carbohidratos = (int) ((calorias - proteinas * 4 - grasas * 9) / 4);
                break;

            case OBJETIVO_MEJORAR_FLEXIBILIDAD:
            case OBJETIVO_MEJORAR_MOVILIDAD:
                // Mantenimiento con grasas algo más altas (30%) para la salud articular
                calorias      = (int) tdee;
                proteinas     = (int) (pesoKg * 1.6);
                grasas        = (int) (calorias * 0.30 / 9);
                carbohidratos = (int) ((calorias - proteinas * 4 - grasas * 9) / 4);
                break;

            case OBJETIVO_MEJORAR_VELOCIDAD:
                // Superávit del 5%, carbos altos (55%) para energía explosiva y recuperación
                calorias      = (int) (tdee * 1.05);
                proteinas     = (int) (pesoKg * 1.8);
                carbohidratos = (int) (calorias * 0.55 / 4);
                grasas        = (int) ((calorias - proteinas * 4 - carbohidratos * 4) / 9);
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

        return new ResultadoNutricional(
                calorias,
                proteinas,
                Math.max(carbohidratos, 0),
                Math.max(grasas, 0),
                agua
        );
    }
}