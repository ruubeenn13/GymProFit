package es.pmdm.gymprofit.ui.nutricion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import es.pmdm.gymprofit.R;

// ============================================================
// CopiarComidaTest — el nombre accesible de «Copiar ayer» en Nutrición (GP-216, lote 3).
// El revisor vio que TalkBack leía «Sí, copiar la comida de ayer», la respuesta de la
// tarjeta «¿Copiar la de ayer?»: el texto visible no estaba en el nombre y el control por
// voz no lo encontraba. Cada comida tiene ahora el suyo, que empieza por «Copiar ayer».
// ============================================================
public class CopiarComidaTest {

    @Test
    public void copiar_ayer_tiene_su_nombre_por_comida_y_no_es_el_si_de_la_tarjeta() {
        assertEquals(R.string.nutricion_copiar_ayer_desayuno, CopiarComida.copiarAyer("DESAYUNO"));
        assertEquals(R.string.nutricion_copiar_ayer_almuerzo, CopiarComida.copiarAyer("ALMUERZO"));
        assertEquals(R.string.nutricion_copiar_ayer_comida, CopiarComida.copiarAyer("COMIDA"));
        assertEquals(R.string.nutricion_copiar_ayer_merienda, CopiarComida.copiarAyer("MERIENDA"));
        assertEquals(R.string.nutricion_copiar_ayer_cena, CopiarComida.copiarAyer("CENA"));
        assertNotEquals(CopiarComida.siCopiar("COMIDA"), CopiarComida.copiarAyer("COMIDA"));
    }
}
