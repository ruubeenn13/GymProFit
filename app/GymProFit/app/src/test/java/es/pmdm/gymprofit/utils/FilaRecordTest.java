package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Locale;

/**
 * GP-225: un récord se escribe y se anuncia igual en la lista de Récords y en Progreso. Antes, la
 * lista decía «hace 1 mes» y Progreso «Hace 1 mes»; la plancha, «Plancha, 1:15» en una y
 * «1:15 de tiempo» en otra.
 */
public class FilaRecordTest {

    private static final Locale ES = new Locale("es", "ES");

    @Test
    public void elApoyoEmpiezaEnMayuscula() {
        assertEquals("Hace 1 mes", FilaRecord.apoyo("hace 1 mes", null, "%1$s · antes %2$s", ES));
        // La marca anterior no se parte, ni la separa del «antes» un salto de línea (lote 12): el
        // espacio de la cadena es duro y los de la marca pasan a duros.
        assertEquals("Hace 2 semanas · antes\u00A080\u00A0kg\u00A0×\u00A08",
                FilaRecord.apoyo("hace 2 semanas", "80 kg × 8", "%1$s · antes\u00A0%2$s", ES));
        assertEquals("80\u00A0kg\u00A0×\u00A08", FilaRecord.apoyo(null, "80 kg × 8", "%1$s · antes\u00A0%2$s", ES));
        assertNull(FilaRecord.apoyo(null, null, "%1$s · antes %2$s", ES));
    }

    @Test
    public void laPlanchaDiceQueEsUnTiempo() {
        assertEquals("Plancha. Hace 1 mes. 1:15 de tiempo",
                FilaRecord.dicho("Plancha", "Hace 1 mes", "1:15", true, "%1$s de tiempo"));
        assertEquals("Press banca. 82,5 kg × 10",
                FilaRecord.dicho("Press banca", null, "82,5 kg × 10", false, "%1$s de tiempo"));
    }
}
