package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.List;

import es.pmdm.gymprofit.model.comida.Comida;

// ============================================================
// DiaNutricion — lo consumido en un día frente al objetivo (GP-105).
//
// «Nutrición de hoy» en Inicio y la tarjeta de kcal de la pestaña Nutrición enseñan
// lo mismo, así que lo calculan aquí y en ningún otro sitio: la misma fuente (las
// comidas del día de la API) y el mismo objetivo (CalculadoraNutricional con el
// perfil guardado). Antes lo hacía solo NutricionFragment.
//
// Las reglas de color también viven aquí (GP-011 y anteriores): la proteína es un
// suelo —llegar es bueno, se pone en verde—; carbohidratos y grasas son un techo
// —pasarse se pone en rojo—.
// ============================================================
public final class DiaNutricion {

    public final int kcal;
    public final double proteinas, carbohidratos, grasas;
    public final int objetivoKcal, objetivoProteinas, objetivoCarbohidratos, objetivoGrasas;

    /** Cómo se pinta una cifra respecto a su objetivo. */
    public enum Estado { NORMAL, LOGRADO, PASADO }

    DiaNutricion(int kcal, double proteinas, double carbohidratos, double grasas,
                 int objetivoKcal, int objetivoProteinas, int objetivoCarbohidratos, int objetivoGrasas) {
        this.kcal = kcal;
        this.proteinas = proteinas;
        this.carbohidratos = carbohidratos;
        this.grasas = grasas;
        this.objetivoKcal = objetivoKcal;
        this.objetivoProteinas = objetivoProteinas;
        this.objetivoCarbohidratos = objetivoCarbohidratos;
        this.objetivoGrasas = objetivoGrasas;
    }

    /**
     * Suma las comidas del día y las compara con el objetivo.
     *
     * @param comidas comidas del día (puede ser null o estar vacía).
     * @param objetivo objetivo diario, de {@link #objetivo(PreferencesManager)}.
     */
    public static DiaNutricion de(@Nullable List<Comida> comidas, ResultadoNutricional objetivo) {
        int kcal = 0;
        double p = 0, c = 0, g = 0;
        if (comidas != null) {
            for (Comida x : comidas) {
                kcal += x.getTotalCalorias();
                p += x.getTotalProteinas();
                c += x.getTotalCarbohidratos();
                g += x.getTotalGrasas();
            }
        }
        return new DiaNutricion(kcal, p, c, g,
                objetivo.calorias, objetivo.proteinas, objetivo.carbohidratos, objetivo.grasas);
    }

    /**
     * El objetivo diario a partir del perfil guardado, y lo deja guardado para las
     * pantallas que lo leen de preferencias.
     */
    public static ResultadoNutricional objetivo(PreferencesManager prefs) {
        ResultadoNutricional r = CalculadoraNutricional.calcular(
                prefs.getPeso(), prefs.getAltura(), prefs.getEdad(),
                "HOMBRE".equals(prefs.getSexo()), prefs.getActividad(), prefs.getObjetivo());
        prefs.saveResultadoNutricional(r.calorias, r.proteinas, r.carbohidratos, r.grasas, r.agua);
        return r;
    }

    /** Kcal que quedan hasta el objetivo; negativo si ya se ha pasado. */
    public int kcalRestantes() { return objetivoKcal - kcal; }

    /** Porcentaje 0..100 de una cifra sobre su objetivo, para las barras. */
    public static int porcentaje(double valor, int objetivo) {
        if (objetivo <= 0) return 0;
        return (int) Math.min(100, Math.max(0, valor * 100.0 / objetivo));
    }

    public Estado estadoProteinas() {
        return proteinas >= objetivoProteinas && objetivoProteinas > 0 ? Estado.LOGRADO : Estado.NORMAL;
    }

    public Estado estadoCarbohidratos() {
        return carbohidratos > objetivoCarbohidratos ? Estado.PASADO : Estado.NORMAL;
    }

    public Estado estadoGrasas() {
        return grasas > objetivoGrasas ? Estado.PASADO : Estado.NORMAL;
    }
}
