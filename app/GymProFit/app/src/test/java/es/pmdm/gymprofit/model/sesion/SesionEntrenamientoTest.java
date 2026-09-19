package es.pmdm.gymprofit.model.sesion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

// ============================================================
// SesionEntrenamientoTest — el entrenamiento libre sobrevive a Gson (GP-057).
//
// El fallo que estos tests impiden reintroducir no se ve leyendo el modelo:
// "private int rutinaId" parece inofensivo. Pero Gson no tiene dónde escribir un
// null en un int, así que una sesión con rutina_id NULL llegaba como rutinaId == 0,
// que no es "sin rutina" sino "la rutina número cero", y la app se quedaba buscando
// en el mapa de nombres una rutina que no existe.
//
// Se prueba contra el JSON de verdad, no contra el setter, porque el que se
// equivocaba era el deserializador.
// ============================================================
public class SesionEntrenamientoTest {

    private final Gson gson = new Gson();

    @Test
    public void rutina_id_nulo_llega_como_null_y_no_como_cero() {
        String json = "{\"id\":7,\"usuarioId\":3,\"rutinaId\":null,\"duracionMinutos\":45}";

        SesionEntrenamiento sesion = gson.fromJson(json, SesionEntrenamiento.class);

        assertNull(sesion.getRutinaId());
        assertTrue(sesion.esEntrenamientoLibre());
    }

    @Test
    public void rutina_id_ausente_tambien_es_entrenamiento_libre() {
        // La API puede omitir el campo en vez de mandarlo a null; para el cliente
        // significa lo mismo.
        String json = "{\"id\":7,\"usuarioId\":3,\"duracionMinutos\":45}";

        SesionEntrenamiento sesion = gson.fromJson(json, SesionEntrenamiento.class);

        assertNull(sesion.getRutinaId());
        assertTrue(sesion.esEntrenamientoLibre());
    }

    @Test
    public void rutina_id_con_valor_se_conserva() {
        String json = "{\"id\":7,\"usuarioId\":3,\"rutinaId\":12,\"duracionMinutos\":45}";

        SesionEntrenamiento sesion = gson.fromJson(json, SesionEntrenamiento.class);

        assertEquals(Integer.valueOf(12), sesion.getRutinaId());
        assertFalse(sesion.esEntrenamientoLibre());
    }

    @Test
    public void el_cero_es_una_rutina_como_otra_cualquiera_no_la_ausencia_de_rutina() {
        // Justo la confusión que causaba el primitivo: si el servidor mandara un 0,
        // sería un id, no un hueco.
        String json = "{\"id\":7,\"usuarioId\":3,\"rutinaId\":0}";

        SesionEntrenamiento sesion = gson.fromJson(json, SesionEntrenamiento.class);

        assertEquals(Integer.valueOf(0), sesion.getRutinaId());
        assertFalse(sesion.esEntrenamientoLibre());
    }
}
