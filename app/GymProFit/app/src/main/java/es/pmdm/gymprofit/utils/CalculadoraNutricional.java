package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

// MODIFICADO - Constantes de objetivo actualizadas para coincidir con el enum
// TipoObjetivo de la API. Añadidos: REDUCIR_GRASA_CORPORAL, MEJORAR_FLEXIBILIDAD,
// MEJORAR_VELOCIDAD, AUMENTAR_CALORIAS, MEJORAR_MOVILIDAD
// ============================================================
// CalculadoraNutricional — utilidad estática para calcular objetivos nutricionales diarios.
// Aplica la fórmula de Mifflin-St Jeor para el TMB, un factor de actividad física
// para el TDEE, y reparte las calorías en macros según el objetivo del usuario.
// ============================================================

/**
 * Objetivos nutricionales diarios (GP-206, DEC-054).
 *
 * <p><b>Proteína.</b> Se ajusta a la evidencia y a los datos de cada persona:
 * <ul>
 *   <li>Con un % de grasa válido (de {@link #GRASA_MIN} a {@link #GRASA_MAX} %, medido hace
 *       {@link #GRASA_DIAS_MAX} días o menos) se calcula sobre masa magra, MM = peso × (1 − %/100):
 *       2,4 g/kg MM al perder grasa y 2,0 g/kg MM en el resto.</li>
 *   <li>Sin él, sobre el peso de referencia: perder 2,0 · ganar 1,8 · fuerza 1,8 · mantener
 *       1,6 g/kg. El peso de referencia es el total, salvo con IMC ≥ 30: el que daría IMC 25
 *       con su altura.</li>
 *   <li>El mínimo eficaz es 1,6 g/kg del peso de referencia (sin pasar nunca de la recomendada).</li>
 * </ul>
 *
 * <p>Fuentes: Morton RW et al., «A systematic review, meta-analysis and meta-regression of the
 * effect of protein supplementation on resistance training-induced gains in muscle mass and
 * strength in healthy adults», Br J Sports Med 2018;52:376-384 (hacia 1,6 g/kg/día el beneficio
 * deja de crecer); y Jäger R et al., «International Society of Sports Nutrition Position Stand:
 * protein and exercise», J Int Soc Sports Nutr 2017;14:20 (1,4–2,0 g/kg/día; más, sobre masa
 * magra, en déficit calórico).
 *
 * <p>Calorías, grasas y agua no dependen del % de grasa; los carbohidratos salen del resto.
 */
public class CalculadoraNutricional {

    /** % de grasa admitido; fuera de este rango se trata como dato ausente. */
    public static final double GRASA_MIN = 3, GRASA_MAX = 60;
    /** Antigüedad máxima, en días, de una medición con % de grasa para usarla. */
    public static final int GRASA_DIAS_MAX = 90;
    private static final double IMC_OBESIDAD = 30, IMC_REFERENCIA = 25;
    private static final double MINIMO_G_KG = 1.6;

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

    // g/kg de peso de referencia cuando no hay un % de grasa válido.
    private static double gKgSinGrasa(String calculo) {
        switch (calculo) {
            case OBJETIVO_PERDER_PESO:         return 2.0;
            case OBJETIVO_GANAR_MASA_MUSCULAR:
            case OBJETIVO_MEJORAR_FUERZA:      return 1.8;
            default:                           return 1.6;
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
        return calcular(pesoKg, alturaCm, edad, esHombre, actividad, objetivo, null, 0);
    }

    /**
     * Como {@link #calcular(double, double, int, boolean, String, String)} pero con el % de
     * grasa de la última medición que lo tenga.
     *
     * @param grasaPct          % de grasa corporal, o null si no hay medición con él (nunca un 0 por «vacío»)
     * @param diasDesdeMedicion días desde esa medición; se ignora si {@code grasaPct} es null
     */
    public static ResultadoNutricional calcular(double pesoKg, double alturaCm, int edad, boolean esHombre,
                                                String actividad, String objetivo,
                                                @Nullable Double grasaPct, int diasDesdeMedicion) {
        double tmb  = calcularTMB(pesoKg, alturaCm, edad, esHombre);
        double tdee = tmb * factorActividad(actividad);

        int calorias, carbohidratos, grasas;

        boolean sinDeficit = OBJETIVO_PERDER_PESO.equals(objetivo) && edad > 0 && edad <= EDAD_SIN_DEFICIT;
        String calculo = sinDeficit ? OBJETIVO_MANTENER_PESO : (objetivo == null ? "" : objetivo);

        double alturaM = alturaCm / 100.0;
        boolean obesidad = alturaM > 0 && pesoKg / (alturaM * alturaM) >= IMC_OBESIDAD;
        double pesoReferencia = obesidad ? IMC_REFERENCIA * alturaM * alturaM : pesoKg;
        boolean sobreMasaMagra = grasaPct != null && grasaPct >= GRASA_MIN && grasaPct <= GRASA_MAX
                && diasDesdeMedicion >= 0 && diasDesdeMedicion <= GRASA_DIAS_MAX;
        int proteinas;
        if (sobreMasaMagra) {
            double masaMagra = pesoKg * (1 - grasaPct / 100.0);
            proteinas = (int) (masaMagra * (OBJETIVO_PERDER_PESO.equals(calculo) ? 2.4 : 2.0));
        } else {
            proteinas = (int) (pesoReferencia * gKgSinGrasa(calculo));
        }
        int minimo = Math.min((int) Math.round(pesoReferencia * MINIMO_G_KG), proteinas);

        switch (calculo) {
            case OBJETIVO_PERDER_PESO:
                // Déficit del 20%, proteína alta para preservar músculo
                calorias      = (int) (tdee * 0.80);
                grasas        = (int) (calorias * 0.25 / 9);
                carbohidratos = (int) ((calorias - proteinas * 4 - grasas * 9) / 4);
                break;

            case OBJETIVO_GANAR_MASA_MUSCULAR:
                // Superávit del 15%, proteína muy alta para construir músculo
                calorias      = (int) (tdee * 1.15);
                grasas        = (int) (calorias * 0.25 / 9);
                carbohidratos = (int) ((calorias - proteinas * 4 - grasas * 9) / 4);
                break;

            case OBJETIVO_MEJORAR_FUERZA:
                // Superávit del 10%, proteína muy alta para soportar cargas pesadas
                calorias      = (int) (tdee * 1.10);
                grasas        = (int) (calorias * 0.28 / 9);
                carbohidratos = (int) ((calorias - proteinas * 4 - grasas * 9) / 4);
                break;

            default:
                // Calorías de mantenimiento con distribución equilibrada
                calorias      = (int) tdee;
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
        r.proteinaMinima = minimo;
        r.baseProteina = sobreMasaMagra ? ResultadoNutricional.BASE_MASA_MAGRA
                : obesidad ? ResultadoNutricional.BASE_PESO_REFERENCIA : ResultadoNutricional.BASE_PESO;
        return r;
    }
}