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
// Las reglas de color también viven aquí: la proteína es un suelo —llegar es bueno,
// se pone en verde—. Carbohidratos y grasas no se pintan: pasarse no es un fallo y el
// lienzo de nutrición no tiene rojo («sin culpa»); lo que sobra lo dice el texto.
// ============================================================
public final class DiaNutricion {

    public final int kcal;
    public final double proteinas, carbohidratos, grasas;
    public final int objetivoKcal, objetivoProteinas, objetivoCarbohidratos, objetivoGrasas;
    /**
     * Sin peso ni altura no hay objetivo (GP-103, «Prefiero no decirlo»): se cuenta lo
     * comido y no se compara con nada. Antes salía un objetivo de 70 kg y 170 cm que no
     * era de nadie.
     */
    public final boolean sinObjetivo;
    /** De 14 a 17 años con «Perder grasa»: el objetivo es el de mantenimiento, y se dice. */
    public final boolean mantenimientoPorEdad;
    /** Mínimo eficaz de proteína y sobre qué se calculó (GP-206); 0 si no se conocen. */
    public final int proteinaMinima, baseProteina;

    /** Cómo se pinta una cifra respecto a su objetivo. */
    public enum Estado { NORMAL, LOGRADO }

    DiaNutricion(int kcal, double proteinas, double carbohidratos, double grasas,
                 int objetivoKcal, int objetivoProteinas, int objetivoCarbohidratos, int objetivoGrasas,
                 boolean sinObjetivo, boolean mantenimientoPorEdad, int proteinaMinima, int baseProteina) {
        this.proteinaMinima = proteinaMinima;
        this.baseProteina = baseProteina;
        this.sinObjetivo = sinObjetivo;
        this.mantenimientoPorEdad = mantenimientoPorEdad;
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
     * @param objetivo objetivo diario, de {@link #objetivo(PreferencesManager)}; null si no hay.
     */
    public static DiaNutricion de(@Nullable List<Comida> comidas, @Nullable ResultadoNutricional objetivo) {
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
        if (objetivo == null) return new DiaNutricion(kcal, p, c, g, 0, 0, 0, 0, true, false, 0, 0);
        return new DiaNutricion(kcal, p, c, g,
                objetivo.calorias, objetivo.proteinas, objetivo.carbohidratos, objetivo.grasas,
                false, objetivo.mantenimientoPorEdad, objetivo.proteinaMinima, objetivo.baseProteina);
    }

    /**
     * El objetivo diario guardado. Lo guardado no se recalcula solo (GP-206, DEC-054): solo se
     * calcula si no hay nada, y recalculan el alta, Editar perfil y el perfil remoto con
     * {@link #recalcular}. Así un cambio en la regla no mueve el objetivo de nadie al abrir una
     * pantalla.
     *
     * @return null si el perfil no tiene peso y altura de esta cuenta: «Prefiero no
     *         decirlo» en el alta (GP-103), o una cuenta que nunca los puso.
     */
    @Nullable
    public static ResultadoNutricional objetivo(PreferencesManager prefs) {
        if (!prefs.hayDatosParaCalorias()) return null;
        if (!prefs.hayObjetivoGuardado()) return recalcular(prefs);
        ResultadoNutricional r = new ResultadoNutricional(prefs.getCaloriasDiarias(), prefs.getProteinasDiarias(),
                prefs.getCarbosDiarios(), prefs.getGrasasDiarias(), prefs.getAguaDiaria());
        r.proteinaMinima = prefs.getProteinaMinima();
        r.baseProteina = prefs.getBaseProteina();
        int edad = prefs.getEdad();
        r.mantenimientoPorEdad = CalculadoraNutricional.OBJETIVO_PERDER_PESO.equals(prefs.getObjetivo())
                && edad > 0 && edad <= CalculadoraNutricional.EDAD_SIN_DEFICIT;
        return r;
    }

    /**
     * Calcula el objetivo con el perfil guardado y el % de grasa reciente que haya en el
     * móvil, y lo deja guardado.
     *
     * @return null si no hay datos para calcular (ver {@link #objetivo})
     */
    @Nullable
    public static ResultadoNutricional recalcular(PreferencesManager prefs) {
        return recalcular(prefs, prefs.getGrasaReciente());
    }

    /** Como {@link #recalcular(PreferencesManager)} con un % de grasa dado (null: ninguno). */
    @Nullable
    public static ResultadoNutricional recalcular(PreferencesManager prefs, @Nullable GrasaReciente.Medida grasa) {
        if (!prefs.hayDatosParaCalorias()) return null;
        ResultadoNutricional r = CalculadoraNutricional.calcular(
                prefs.getPeso(), prefs.getAltura(), prefs.getEdad(),
                "HOMBRE".equals(prefs.getSexo()), prefs.getActividad(), prefs.getObjetivo(),
                grasa == null ? null : grasa.pct, grasa == null ? 0 : grasa.dias(System.currentTimeMillis()));
        prefs.saveResultadoNutricional(r);
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
}
