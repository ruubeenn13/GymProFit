package es.pmdm.gymprofit.utils;

// ============================================================
// ResultadoNutricional — modelo simple (POJO) con el resultado de un cálculo nutricional.
// Agrupa calorías diarias, macros (proteínas/carbohidratos/grasas) y agua recomendada,
// producido por CalculadoraNutricional y persistido vía PreferencesManager.
// ============================================================
public class ResultadoNutricional {

    public int calorias;
    public int proteinas;
    public int carbohidratos;
    public int grasas;
    public double agua;
    /**
     * De 14 a 17 años, «Perder grasa» da las calorías de mantenimiento (GP-103, decisión 6
     * del lienzo del alta): sin déficit a esa edad. Las pantallas lo dicen cuando es true.
     */
    public boolean mantenimientoPorEdad;
    /**
     * Mínimo eficaz de proteína en gramos (GP-206): 1,6 g/kg del peso de referencia, nunca por
     * encima de {@link #proteinas}. 0 si el objetivo se guardó antes de GP-206 y no se conoce.
     */
    public int proteinaMinima;
    /** Sobre qué se calculó la proteína: una de las constantes {@code BASE_*}. */
    public int baseProteina;

    public static final int BASE_DESCONOCIDA = 0, BASE_MASA_MAGRA = 1, BASE_PESO = 2, BASE_PESO_REFERENCIA = 3;

    /** True si hay un rango que enseñar: el mínimo se conoce y es menor que la cifra recomendada. */
    public boolean tieneRangoProteina() { return proteinaMinima > 0 && proteinaMinima < proteinas; }

    // Construye el resultado con todos los valores calculados.
    public ResultadoNutricional(int calorias, int proteinas, int carbohidratos, int grasas, double agua) {
        this.calorias = calorias;
        this.proteinas = proteinas;
        this.carbohidratos = carbohidratos;
        this.grasas = grasas;
        this.agua = agua;
    }
}
