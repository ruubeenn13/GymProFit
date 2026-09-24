package com.gymprofit.api.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// ============================================================
// TipoLogroTest — el umbral de cada logro y su comparación (GP-079).
// ============================================================
@DisplayName("TipoLogro — umbrales")
class TipoLogroTest {

    @Test
    @DisplayName("Cada tipo se alcanza justo en su umbral, no uno antes")
    void alcanzadoEnElBorde() {
        for (TipoLogro tipo : TipoLogro.values()) {
            assertFalse(tipo.alcanzado(tipo.getUmbral() - 1L), tipo + " uno antes del umbral");
            assertTrue(tipo.alcanzado(tipo.getUmbral()), tipo + " en el umbral");
        }
    }

    /**
     * Los umbrales publicados. Si alguien cambia uno, este test obliga a hacerlo a
     * propósito: la descripción de la tabla logros repite el número en prosa y
     * habría que cambiarla también.
     */
    @Test
    @DisplayName("Los umbrales son los que dicen las descripciones del catálogo")
    void umbralesPublicados() {
        assertEquals(1, TipoLogro.PRIMERA_SESION.getUmbral());
        assertEquals(7, TipoLogro.CONSTANCIA.getUmbral());
        assertEquals(30, TipoLogro.DEDICADO.getUmbral());
        assertEquals(100, TipoLogro.CENTENARIO.getUmbral());
        assertEquals(1, TipoLogro.OBJETIVO_CUMPLIDO.getUmbral());
        assertEquals(10, TipoLogro.MAQUINA.getUmbral());
    }
}
