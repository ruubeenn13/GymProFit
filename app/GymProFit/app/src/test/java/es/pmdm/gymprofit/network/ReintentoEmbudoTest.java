package es.pmdm.gymprofit.network;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// ============================================================
// ReintentoEmbudoTest — GP-264: un aviso del embudo se reintenta como mucho una vez.
//
// La causa vista en producción: tras la ráfaga de la prueba del cupo, los cuatro primeros
// pasos del alta recibieron 429 desde la misma IP y se perdieron sin reintento. Un 429 no
// cuenta, así que reintentarlo pasado el Retry-After (tope 60 s) no puede contar doble.
// ============================================================
public class ReintentoEmbudoTest {

    /** Cada intento consume la siguiente respuesta de la lista. */
    private final Deque<java.util.function.Consumer<ReintentoEmbudo.Resultado>> respuestas = new ArrayDeque<>();
    private int intentos;
    /** Las esperas que se han pedido al programador, en ms. */
    private final List<Long> esperas = new ArrayList<>();

    private final ReintentoEmbudo.Intento intento = r -> {
        intentos++;
        respuestas.removeFirst().accept(r);
    };

    // Ejecuta en el acto lo diferido y apunta cuánto había que esperar.
    private final ReintentoEmbudo.Programador programador = (ms, tarea) -> {
        esperas.add(ms);
        tarea.run();
    };

    private void enviar() {
        ReintentoEmbudo.enviar(intento, programador);
    }

    @Test
    public void contado_a_la_primera_no_reintenta() {
        respuestas.add(ReintentoEmbudo.Resultado::contado);
        enviar();
        assertEquals(1, intentos);
        assertTrue(esperas.isEmpty());
    }

    @Test
    public void un_429_con_retry_after_se_reintenta_pasado_ese_tiempo() {
        respuestas.add(r -> r.rechazado(429, "7"));
        respuestas.add(ReintentoEmbudo.Resultado::contado);
        enviar();
        assertEquals(2, intentos);
        assertEquals(Arrays.asList(7_000L), esperas);
    }

    @Test
    public void el_retry_after_tiene_un_tope_de_60_segundos() {
        respuestas.add(r -> r.rechazado(429, "3600"));
        respuestas.add(ReintentoEmbudo.Resultado::contado);
        enviar();
        assertEquals(Arrays.asList(60_000L), esperas);
    }

    @Test
    public void un_429_sin_retry_after_espera_60_segundos() {
        respuestas.add(r -> r.rechazado(429, null));
        respuestas.add(ReintentoEmbudo.Resultado::contado);
        enviar();
        assertEquals(2, intentos);
        assertEquals(Arrays.asList(60_000L), esperas);
    }

    @Test
    public void un_retry_after_que_no_es_un_numero_espera_60_segundos() {
        respuestas.add(r -> r.rechazado(429, "Wed, 21 Oct 2026 07:28:00 GMT"));
        respuestas.add(ReintentoEmbudo.Resultado::contado);
        enviar();
        assertEquals(Arrays.asList(60_000L), esperas);
    }

    @Test
    public void un_fallo_de_red_se_reintenta_una_vez() {
        respuestas.add(ReintentoEmbudo.Resultado::falloDeRed);
        respuestas.add(ReintentoEmbudo.Resultado::contado);
        enviar();
        assertEquals(2, intentos);
    }

    @Test
    public void dos_fallos_seguidos_no_dan_un_tercer_intento() {
        respuestas.add(r -> r.rechazado(429, "1"));
        respuestas.add(r -> r.rechazado(429, "1"));
        enviar();
        assertEquals(2, intentos);

        intentos = 0;
        respuestas.add(ReintentoEmbudo.Resultado::falloDeRed);
        respuestas.add(ReintentoEmbudo.Resultado::falloDeRed);
        enviar();
        assertEquals(2, intentos);

        intentos = 0;
        respuestas.add(ReintentoEmbudo.Resultado::falloDeRed);
        respuestas.add(r -> r.rechazado(429, "1"));
        enviar();
        assertEquals(2, intentos);
    }

    @Test
    public void otros_rechazos_no_se_reintentan() {
        // Un 400 (paso que la API no conoce) o un 500 no cambian por insistir.
        respuestas.add(r -> r.rechazado(400, null));
        enviar();
        respuestas.add(r -> r.rechazado(500, null));
        enviar();
        assertEquals(2, intentos);
        assertTrue(esperas.isEmpty());
    }
}
