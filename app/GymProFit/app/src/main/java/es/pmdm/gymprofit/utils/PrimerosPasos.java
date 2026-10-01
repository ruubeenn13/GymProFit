package es.pmdm.gymprofit.utils;

// ============================================================
// PrimerosPasos — la tarjeta de Inicio del primer día (GP-103, tablero 13 del lienzo).
//
// Tres pasos —tu plan, tu primer entrenamiento y tu primera comida— que se marcan solos
// según lo que haya hecho la cuenta, sin candados (decisión 11). La tarjeta solo sale a
// una cuenta recién hecha (o que acaba de responder el cuestionario), se puede ocultar
// y desaparece al completarse. Sin Android, para probarlo en la JVM.
// ============================================================
public final class PrimerosPasos {

    public static final int TOTAL = 3;

    public final boolean plan, entreno, comida;

    public PrimerosPasos(boolean plan, boolean entreno, boolean comida) {
        this.plan = plan;
        this.entreno = entreno;
        this.comida = comida;
    }

    /** Cuántos están hechos. */
    public int hechos() {
        return (plan ? 1 : 0) + (entreno ? 1 : 0) + (comida ? 1 : 0);
    }

    public boolean completos() {
        return hechos() == TOTAL;
    }

    /**
     * Si la tarjeta se enseña.
     *
     * @param primerDia la cuenta acaba de hacer el alta o el cuestionario.
     * @param oculta    la persona la cerró.
     */
    public boolean visible(boolean primerDia, boolean oculta) {
        return primerDia && !oculta && !completos();
    }
}
