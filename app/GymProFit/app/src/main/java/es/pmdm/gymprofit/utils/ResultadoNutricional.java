package es.pmdm.gymprofit.utils;

public class ResultadoNutricional {

    public int calorias;
    public int proteinas;
    public int carbohidratos;
    public int grasas;
    public double agua;

    public ResultadoNutricional(int calorias, int proteinas, int carbohidratos, int grasas, double agua) {
        this.calorias = calorias;
        this.proteinas = proteinas;
        this.carbohidratos = carbohidratos;
        this.grasas = grasas;
        this.agua = agua;
    }
}
