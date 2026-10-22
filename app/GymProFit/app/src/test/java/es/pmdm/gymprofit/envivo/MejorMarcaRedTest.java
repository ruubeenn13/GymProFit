package es.pmdm.gymprofit.envivo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.util.List;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.record.PuntoProgresion;

// ============================================================
// MejorMarcaRedTest — la mejor marca de un ejercicio a partir de su progresión (GP-210
// paso 3), para quien ya entrenó pero aún no ha batido ningún récord: su única marca
// es el punto de partida y /records no la trae.
// ============================================================
public class MejorMarcaRedTest {

    private static List<PuntoProgresion> puntos(String json) {
        return new Gson().fromJson(json, new TypeToken<List<PuntoProgresion>>() { }.getType());
    }

    @Test
    public void sin_puntos_no_hay_marca() {
        assertNull(RedSesionEnVivo.mejorDe(null));
        assertNull(RedSesionEnVivo.mejorDe(puntos("[]")));
    }

    @Test
    public void la_mejor_es_la_que_manda_la_regla_de_gp_088() {
        SesionEnCurso.Marca m = RedSesionEnVivo.mejorDe(puntos(
                "[{\"tipo\":\"PESO\",\"peso\":80,\"repeticiones\":8},"
                        + "{\"tipo\":\"PESO\",\"peso\":82.5,\"repeticiones\":5},"
                        + "{\"tipo\":\"PESO\",\"peso\":80,\"repeticiones\":10}]"));
        assertEquals("PESO", m.tipo);
        assertEquals("82.5", m.peso);
        assertEquals(5, m.repeticiones);
    }

    @Test
    public void con_peso_manda_sobre_sin_peso() {
        SesionEnCurso.Marca m = RedSesionEnVivo.mejorDe(puntos(
                "[{\"tipo\":\"REPETICIONES\",\"repeticiones\":20},"
                        + "{\"tipo\":\"PESO\",\"peso\":10,\"repeticiones\":5}]"));
        assertEquals("PESO", m.tipo);
        assertEquals("10", m.peso);
    }

    @Test
    public void por_tiempo_manda_la_serie_mas_larga() {
        SesionEnCurso.Marca m = RedSesionEnVivo.mejorDe(puntos(
                "[{\"tipo\":\"TIEMPO\",\"segundos\":45},{\"tipo\":\"TIEMPO\",\"segundos\":60}]"));
        assertEquals("TIEMPO", m.tipo);
        assertEquals(60, m.segundos);
    }
}
