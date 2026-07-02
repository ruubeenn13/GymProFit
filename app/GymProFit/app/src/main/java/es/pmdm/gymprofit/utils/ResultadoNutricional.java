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

    // Construye el resultado con todos los valores calculados.
    public ResultadoNutricional(int calorias, int proteinas, int carbohidratos, int grasas, double agua) {
        this.calorias = calorias;
        this.proteinas = proteinas;
        this.carbohidratos = carbohidratos;
        this.grasas = grasas;
        this.agua = agua;
    }
}
