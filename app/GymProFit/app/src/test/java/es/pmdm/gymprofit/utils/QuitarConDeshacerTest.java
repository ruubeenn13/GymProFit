package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// QuitarConDeshacerTest — quitar un alimento de la comida sin preguntar, con «Deshacer»
// (decisión 16, momento 20, lote 1.6.2)
// Quitar no llama a la API: la fila se va y queda pendiente. «Deshacer» la devuelve sin
// llamar a nada. El borrado se manda cuando el aviso se va: por tiempo, porque se quita
// otra, o porque se sale de la pantalla; al salir, se espera la respuesta antes de volver
// al diario. Si la API falla, la fila vuelve a su sitio y se dice.
// ============================================================
public class QuitarConDeshacerTest {

    /** Una API de mentira: guarda los envíos y responde cuando se le dice. */
    private final List<String> enviados = new ArrayList<>();
    private final List<QuitarConDeshacer.Respuesta> respuestas = new ArrayList<>();
    private final List<String> devueltos = new ArrayList<>();
    private int listos;
    private QuitarConDeshacer<String> q;

    @Before
    public void preparar() {
        q = new QuitarConDeshacer<>((item, r) -> {
            enviados.add(item);
            respuestas.add(r);
        }, new QuitarConDeshacer.Vista<String>() {
            @Override
            public void devolver(String item, int posicion, int code, String message) {
                devueltos.add(item + "@" + posicion);
            }

            @Override
            public void listoParaSalir() {
                listos++;
            }
        });
    }

    @Test
    public void quitar_no_llama_a_la_api() {
        q.quitar("arroz", 1);
        assertTrue(enviados.isEmpty());
        assertTrue(q.hayPendiente());
    }

    @Test
    public void deshacer_devuelve_la_fila_sin_llamar_a_nada() {
        q.quitar("arroz", 1);
        QuitarConDeshacer.Pendiente<String> p = q.deshacer();
        assertNotNull(p);
        assertEquals("arroz", p.item);
        assertEquals(1, p.posicion);
        assertFalse(q.hayPendiente());
        // El aviso se va después de deshacer: no hay nada que mandar.
        q.confirmar();
        assertTrue(enviados.isEmpty());
        assertNull(q.deshacer());
    }

    @Test
    public void se_manda_cuando_el_aviso_se_va() {
        q.quitar("arroz", 1);
        q.confirmar();
        assertEquals(1, enviados.size());
        assertFalse(q.hayPendiente());
        // Y una segunda confirmación no lo manda otra vez.
        q.confirmar();
        assertEquals(1, enviados.size());
    }

    @Test
    public void quitar_otra_manda_la_anterior() {
        q.quitar("arroz", 1);
        q.quitar("aceite", 2);
        assertEquals(1, enviados.size());
        assertEquals("arroz", enviados.get(0));
        // «Deshacer» ya solo devuelve la última.
        assertEquals("aceite", q.deshacer().item);
    }

    @Test
    public void si_falla_la_fila_vuelve_a_su_sitio() {
        q.quitar("arroz", 1);
        q.confirmar();
        respuestas.get(0).fallo(500, "error");
        assertEquals(1, devueltos.size());
        assertEquals("arroz@1", devueltos.get(0));
    }

    @Test
    public void al_salir_espera_la_respuesta() {
        q.quitar("arroz", 1);
        q.salir();
        assertEquals(1, enviados.size());
        assertEquals(0, listos);
        respuestas.get(0).ok();
        assertEquals(1, listos);
    }

    @Test
    public void al_salir_sin_nada_pendiente_vuelve_al_momento() {
        q.salir();
        assertEquals(1, listos);
        assertTrue(enviados.isEmpty());
    }

    @Test
    public void al_salir_tambien_espera_un_envio_que_ya_estaba_en_vuelo() {
        q.quitar("arroz", 1);
        q.confirmar();
        q.salir();
        assertEquals(0, listos);
        respuestas.get(0).fallo(0, null);
        // Falló: se dice (la fila vuelve) y se sale igual.
        assertEquals(1, devueltos.size());
        assertEquals(1, listos);
    }

    @Test
    public void una_respuesta_no_cuenta_dos_veces() {
        q.quitar("arroz", 1);
        q.salir();
        respuestas.get(0).ok();
        respuestas.get(0).ok();
        assertEquals(1, listos);
    }
}
