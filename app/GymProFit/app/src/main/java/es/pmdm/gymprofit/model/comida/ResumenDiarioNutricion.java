package es.pmdm.gymprofit.model.comida;

// ============================================================
// ResumenDiarioNutricion — totales nutricionales de UN día (POJO Gson).
// Espeja ResumenDiarioNutricionDTO de la API (GET /comidas/usuario/{id}/resumen):
// suma de calorías y macros de todas las comidas del día. Alimenta la gráfica de
// kcal/macros del histórico de nutrición.
// ============================================================
public class ResumenDiarioNutricion {
    // Día (yyyy-MM-dd)
    private String fecha;
    // Calorías totales del día
    private int calorias;
    // Proteínas totales (g)
    private double proteinas;
    // Carbohidratos totales (g)
    private double carbohidratos;
    // Grasas totales (g)
    private double grasas;

    public String getFecha() { return fecha; }
    public int getCalorias() { return calorias; }
    public double getProteinas() { return proteinas; }
    public double getCarbohidratos() { return carbohidratos; }
    public double getGrasas() { return grasas; }
}
