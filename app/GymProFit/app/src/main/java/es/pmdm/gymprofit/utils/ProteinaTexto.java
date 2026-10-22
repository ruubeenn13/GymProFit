package es.pmdm.gymprofit.utils;

import androidx.annotation.StringRes;

import es.pmdm.gymprofit.R;

/**
 * Los textos de «sobre qué se calculó la proteína» (GP-206, DEC-054), para el alta y Nutrición.
 */
public final class ProteinaTexto {

    private ProteinaTexto() { }

    /**
     * @param base una de {@code ResultadoNutricional.BASE_*}
     * @return el recurso que lo explica, o 0 si no se sabe (objetivo guardado antes de GP-206:
     *         no se afirma nada que no se haya calculado)
     */
    @StringRes
    public static int base(int base) {
        switch (base) {
            case ResultadoNutricional.BASE_MASA_MAGRA: return R.string.proteina_base_masa_magra;
            case ResultadoNutricional.BASE_PESO: return R.string.proteina_base_peso;
            case ResultadoNutricional.BASE_PESO_REFERENCIA: return R.string.proteina_base_peso_referencia;
            default: return 0;
        }
    }
}
