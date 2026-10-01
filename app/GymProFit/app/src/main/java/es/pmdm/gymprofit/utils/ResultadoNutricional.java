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

    // Construye el resultado con todos los valores calculados.
    public ResultadoNutricional(int calorias, int proteinas, int carbohidratos, int grasas, double agua) {
        this.calorias = calorias;
        this.proteinas = proteinas;
        this.carbohidratos = carbohidratos;
        this.grasas = grasas;
        this.agua = agua;
    }
}
