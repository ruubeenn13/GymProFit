package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

// ============================================================
// PlanPrevio — cómo quedaría el plan con los datos del formulario, antes de guardar
// (GP-216, tablero 18).
//
// Es la misma cuenta que hace Guardar: CalculadoraNutricional con el % de grasa reciente
// (DEC-054). El lienzo traía una fórmula suya de ejemplo; la que manda es la de la app, para
// que lo que se ve abajo sea exactamente lo que queda guardado.
// ============================================================
public final class PlanPrevio {

    public final int kcal;
    public final int proteinas;
    public final int proteinaMinima;
    /** De 14 a 17 años, «Perder grasa» da las calorías de mantenimiento (GP-103). */
    public final boolean mantenimientoPorEdad;
    /** Kcal de ahora a lo que quedaría: positivo si sube. */
    public final int difKcal;
    public final int difProteinas;

    /** Si el plan que quedaría es el de ahora: ni las kcal ni la proteína cambian. */
    public boolean sinCambios() {
        return difKcal == 0 && difProteinas == 0;
    }

    /** Qué se dice de la diferencia «respecto a ahora». */
    public enum Aviso { NINGUNO, IGUAL, MAS_KCAL, MENOS_KCAL, MAS_PROTEINA, MENOS_PROTEINA }

    /**
     * La diferencia solo se cuenta si el usuario ha tocado algún dato: con el formulario como estaba al
     * abrir es «sin cambios», aunque lo guardado (DEC-054, plan anterior) no coincida con la fórmula de hoy.
     *
     * @param formularioCambiado si algún dato del formulario difiere de lo que había al abrir.
     * @param hayObjetivoGuardado si existe un objetivo guardado con el que comparar.
     */
    public Aviso aviso(boolean formularioCambiado, boolean hayObjetivoGuardado) {
        if (!hayObjetivoGuardado) return Aviso.NINGUNO;
        if (!formularioCambiado || sinCambios()) return Aviso.IGUAL;
        // Las kcal mandan; si son las mismas y cambia la proteína, se dice la proteína.
        if (difKcal != 0) return difKcal > 0 ? Aviso.MAS_KCAL : Aviso.MENOS_KCAL;
        return difProteinas > 0 ? Aviso.MAS_PROTEINA : Aviso.MENOS_PROTEINA;
    }

    private PlanPrevio(ResultadoNutricional r, int kcalActuales, int proteinasActuales) {
        this.kcal = r.calorias;
        this.proteinas = r.proteinas;
        this.proteinaMinima = r.proteinaMinima;
        this.mantenimientoPorEdad = r.mantenimientoPorEdad;
        this.difKcal = r.calorias - kcalActuales;
        this.difProteinas = r.proteinas - proteinasActuales;
    }

    /**
     * El plan para esos datos.
     *
     * @param grasa            el % de grasa más reciente que haya, o null.
     * @param ahoraMs          el momento de la cuenta, para saber cuántos días tiene esa medición.
     * @param kcalActuales     las kcal del objetivo guardado hoy (0 si no hay).
     * @param proteinasActuales los gramos de proteína del objetivo guardado hoy (0 si no hay).
     * @return null si peso, altura o edad no valen todavía: no se inventa un plan.
     */
    @Nullable
    public static PlanPrevio de(DatosPerfil d, @Nullable GrasaReciente.Medida grasa, long ahoraMs,
                                int kcalActuales, int proteinasActuales) {
        if (d.peso <= 0 || d.altura <= 0 || d.edad < ReglasEdad.MINIMA) return null;
        ResultadoNutricional r = CalculadoraNutricional.calcular(d.peso, d.altura, d.edad,
                "HOMBRE".equals(d.sexo), d.actividad, d.objetivo,
                grasa == null ? null : grasa.pct, grasa == null ? 0 : grasa.dias(ahoraMs));
        return new PlanPrevio(r, kcalActuales, proteinasActuales);
    }
}
