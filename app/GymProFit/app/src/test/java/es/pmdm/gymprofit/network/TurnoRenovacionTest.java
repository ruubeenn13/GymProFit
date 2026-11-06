package es.pmdm.gymprofit.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// ============================================================
// TurnoRenovacionTest — la renovación a mitad de uso (GP-237 y GP-235, DEC-059).
//
// Antes, cualquier fallo de /auth/refresh (sin red, 429, 5xx) cerraba la sesión, y con
// varias peticiones esperando cada una volvía a llamar con el mismo refresh. Aquí la
// llamada es de mentira y cuenta cuántas veces se la llama; las tandas son hilos de
// verdad que esperan el cerrojo mientras la primera llamada está en el aire. El reloj es
// de mentira también: la espera tras un fallo (5 s, o Retry-After) se mide sin dormir.
// ============================================================
public class TurnoRenovacionTest {

    private static final int EN_ESPERA = 6;

    /** El reloj del turno: solo avanza cuando el test lo dice. */
    private final AtomicLong reloj = new AtomicLong(1_000);

    /** Tokens en memoria, como UtilREST. */
    private static final class TokensFalsos implements TurnoRenovacion.Tokens {
        volatile String access = "viejo", refresh = "r1";
        @Override public String access() { return access; }
        @Override public String refresh() { return refresh; }
        @Override public void guardar(String a, String r) { access = a; refresh = r; }
    }

    /** /auth/refresh de mentira: contesta lo que se le diga y, si se pide, se queda colgada. */
    private static final class LlamadaFalsa implements TurnoRenovacion.Llamada {
        final AtomicInteger llamadas = new AtomicInteger();
        volatile TurnoRenovacion.Respuesta respuesta;
        volatile CountDownLatch dentro = new CountDownLatch(1);
        volatile CountDownLatch soltar;   // null: contesta al momento

        LlamadaFalsa(TurnoRenovacion.Respuesta r) { respuesta = r; }

        @Override
        public TurnoRenovacion.Respuesta pedir(String refresh) {
            llamadas.incrementAndGet();
            dentro.countDown();
            CountDownLatch s = soltar;
            if (s != null) {
                try { s.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
            }
            return respuesta;
        }
    }

    private static TurnoRenovacion.Respuesta ok() {
        return new TurnoRenovacion.Respuesta(200, "{}", "nuevo", "r2");
    }

    private static TurnoRenovacion.Respuesta codigo(int c, String cuerpo) {
        return new TurnoRenovacion.Respuesta(c, cuerpo, null, null);
    }

    /**
     * Lanza EN_ESPERA peticiones con el access viejo a la vez: la primera llama y se queda
     * colgada hasta que todas las demás esperan el cerrojo; entonces contesta.
     */
    private List<TurnoRenovacion.Resultado> tanda(TurnoRenovacion turno, LlamadaFalsa llamada) throws Exception {
        llamada.soltar = new CountDownLatch(1);
        llamada.dentro = new CountDownLatch(1);
        CountDownLatch listos = new CountDownLatch(EN_ESPERA);
        List<TurnoRenovacion.Resultado> resultados = java.util.Collections.synchronizedList(new ArrayList<>());
        List<Thread> hilos = new ArrayList<>();
        for (int i = 0; i < EN_ESPERA; i++) {
            Thread h = new Thread(() -> {
                listos.countDown();
                resultados.add(turno.renovar("viejo"));
            });
            hilos.add(h);
            h.start();
        }
        assertTrue(listos.await(5, TimeUnit.SECONDS));
        assertTrue(llamada.dentro.await(5, TimeUnit.SECONDS));
        // Todos menos el que llama, parados en el cerrojo.
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite) {
            int bloqueados = 0;
            for (Thread h : hilos) if (h.getState() == Thread.State.BLOCKED) bloqueados++;
            if (bloqueados == EN_ESPERA - 1) break;
            Thread.sleep(5);
        }
        llamada.soltar.countDown();
        for (Thread h : hilos) h.join(5000);
        llamada.soltar = null;
        assertEquals(EN_ESPERA, resultados.size());
        return resultados;
    }

    // ── Una llamada por intento (GP-235) ────────────────────────────────

    @Test
    public void con_la_sesion_viva_una_llamada_y_todas_se_repiten_con_el_token_nuevo() throws Exception {
        TokensFalsos tokens = new TokensFalsos();
        LlamadaFalsa llamada = new LlamadaFalsa(ok());
        TurnoRenovacion turno = new TurnoRenovacion(llamada, tokens, reloj::get);

        for (TurnoRenovacion.Resultado r : tanda(turno, llamada)) {
            assertEquals(TurnoRenovacion.Tipo.RENOVADA, r.tipo);
            assertEquals("nuevo", r.token);
        }
        assertEquals(1, llamada.llamadas.get());
        assertEquals("r2", tokens.refresh);
    }

    @Test
    public void con_la_sesion_revocada_una_llamada_y_todas_cierran() throws Exception {
        TokensFalsos tokens = new TokensFalsos();
        LlamadaFalsa llamada = new LlamadaFalsa(codigo(401, "{\"cause\":null}"));
        TurnoRenovacion turno = new TurnoRenovacion(llamada, tokens, reloj::get);

        for (TurnoRenovacion.Resultado r : tanda(turno, llamada)) {
            assertEquals(TurnoRenovacion.Tipo.CERRADA, r.tipo);
            assertFalse(r.cuentaDesactivada);
        }
        assertEquals(1, llamada.llamadas.get());
    }

    @Test
    public void sin_respuesta_una_llamada_por_tanda_y_todas_fallan_sin_cerrar() throws Exception {
        TokensFalsos tokens = new TokensFalsos();
        LlamadaFalsa llamada = new LlamadaFalsa(TurnoRenovacion.Respuesta.sinRespuesta());
        TurnoRenovacion turno = new TurnoRenovacion(llamada, tokens, reloj::get);

        for (TurnoRenovacion.Resultado r : tanda(turno, llamada)) {
            assertEquals(TurnoRenovacion.Tipo.SIN_RESPUESTA, r.tipo);
        }
        assertEquals(1, llamada.llamadas.get());
        assertEquals("viejo", tokens.access);
        assertEquals("r1", tokens.refresh);
    }

    // ── Lo que se recuerda y lo que no ──────────────────────────────────

    @Test
    public void un_refresh_rechazado_se_recuerda_y_quien_llega_luego_con_el_no_llama() {
        TokensFalsos tokens = new TokensFalsos();
        LlamadaFalsa llamada = new LlamadaFalsa(codigo(401, null));
        TurnoRenovacion turno = new TurnoRenovacion(llamada, tokens, reloj::get);

        assertEquals(TurnoRenovacion.Tipo.CERRADA, turno.renovar("viejo").tipo);
        // El 401 aún no ha llegado a la pantalla que borra los tokens: el refresh sigue ahí.
        // Pasada cualquier espera, es el recuerdo el que la para.
        reloj.addAndGet(60_000);
        assertEquals(TurnoRenovacion.Tipo.CERRADA, turno.renovar("viejo").tipo);
        assertEquals(1, llamada.llamadas.get());
    }

    @Test
    public void tras_un_sin_respuesta_no_se_vuelve_a_llamar_en_5_s_y_luego_si() {
        TokensFalsos tokens = new TokensFalsos();
        LlamadaFalsa llamada = new LlamadaFalsa(TurnoRenovacion.Respuesta.sinRespuesta());
        TurnoRenovacion turno = new TurnoRenovacion(llamada, tokens, reloj::get);

        assertEquals(TurnoRenovacion.Tipo.SIN_RESPUESTA, turno.renovar("viejo").tipo);
        // Lo que llega en la espera falla igual, sin llamar: también un 401 de una
        // petición que salió después del intento (la vuelta a Inicio, medida con 3).
        llamada.respuesta = ok();
        reloj.addAndGet(TurnoRenovacion.ESPERA_SIN_RESPUESTA_MS - 1);
        assertEquals(TurnoRenovacion.Tipo.SIN_RESPUESTA, turno.renovar("viejo").tipo);
        assertEquals(1, llamada.llamadas.get());
        // Pasada la espera, la siguiente acción vuelve a probar, y con la red de vuelta renueva.
        reloj.addAndGet(1);
        TurnoRenovacion.Resultado r = turno.renovar("viejo");
        assertEquals(TurnoRenovacion.Tipo.RENOVADA, r.tipo);
        assertEquals("nuevo", r.token);
        assertEquals(2, llamada.llamadas.get());
    }

    @Test
    public void la_espera_es_del_refresh_con_que_se_intento() {
        TokensFalsos tokens = new TokensFalsos();
        LlamadaFalsa llamada = new LlamadaFalsa(TurnoRenovacion.Respuesta.sinRespuesta());
        TurnoRenovacion turno = new TurnoRenovacion(llamada, tokens, reloj::get);

        assertEquals(TurnoRenovacion.Tipo.SIN_RESPUESTA, turno.renovar("viejo").tipo);
        // Otra sesión (Entrar de nuevo) trae otro refresh: no hereda la espera.
        tokens.access = "otro";
        tokens.refresh = "r9";
        llamada.respuesta = ok();
        assertEquals(TurnoRenovacion.Tipo.RENOVADA, turno.renovar("otro").tipo);
        assertEquals(2, llamada.llamadas.get());
    }

    // ── 429: la espera que diga Retry-After (GP-096) ────────────────────

    @Test
    public void con_429_no_se_vuelve_a_llamar_hasta_que_pase_retry_after_y_se_dice_cuanto_falta() {
        TokensFalsos tokens = new TokensFalsos();
        LlamadaFalsa llamada = new LlamadaFalsa(new TurnoRenovacion.Respuesta(429, "{}", null, null, 30));
        TurnoRenovacion turno = new TurnoRenovacion(llamada, tokens, reloj::get);

        TurnoRenovacion.Resultado r = turno.renovar("viejo");
        assertEquals(TurnoRenovacion.Tipo.LIMITADA, r.tipo);
        assertEquals(30, r.segundosDeEspera);

        reloj.addAndGet(10_400);
        r = turno.renovar("viejo");
        assertEquals(TurnoRenovacion.Tipo.LIMITADA, r.tipo);
        assertEquals("redondea hacia arriba: no invita a volver antes", 20, r.segundosDeEspera);
        // Pasados los 5 s de un sin respuesta, un 429 sigue esperando lo suyo.
        assertEquals(1, llamada.llamadas.get());

        llamada.respuesta = ok();
        reloj.addAndGet(19_600);
        assertEquals(TurnoRenovacion.Tipo.RENOVADA, turno.renovar("viejo").tipo);
        assertEquals(2, llamada.llamadas.get());
    }

    @Test
    public void con_429_sin_retry_after_espera_los_5_s_de_siempre() {
        TokensFalsos tokens = new TokensFalsos();
        LlamadaFalsa llamada = new LlamadaFalsa(new TurnoRenovacion.Respuesta(429, "{}", null, null, null));
        TurnoRenovacion turno = new TurnoRenovacion(llamada, tokens, reloj::get);

        TurnoRenovacion.Resultado r = turno.renovar("viejo");
        assertEquals(TurnoRenovacion.Tipo.LIMITADA, r.tipo);
        assertEquals(5, r.segundosDeEspera);
        reloj.addAndGet(TurnoRenovacion.ESPERA_SIN_RESPUESTA_MS);
        llamada.respuesta = ok();
        assertEquals(TurnoRenovacion.Tipo.RENOVADA, turno.renovar("viejo").tipo);
    }

    @Test
    public void con_429_una_llamada_por_tanda_y_todas_esperan() throws Exception {
        TokensFalsos tokens = new TokensFalsos();
        LlamadaFalsa llamada = new LlamadaFalsa(new TurnoRenovacion.Respuesta(429, "{}", null, null, 30));
        TurnoRenovacion turno = new TurnoRenovacion(llamada, tokens, reloj::get);

        for (TurnoRenovacion.Resultado r : tanda(turno, llamada)) {
            assertEquals(TurnoRenovacion.Tipo.LIMITADA, r.tipo);
            assertEquals(30, r.segundosDeEspera);
        }
        assertEquals(1, llamada.llamadas.get());
        assertEquals("r1", tokens.refresh);
    }

    @Test
    public void quien_llega_con_un_access_ya_renovado_por_otro_no_llama() {
        TokensFalsos tokens = new TokensFalsos();
        tokens.access = "nuevo";
        LlamadaFalsa llamada = new LlamadaFalsa(ok());
        TurnoRenovacion turno = new TurnoRenovacion(llamada, tokens, reloj::get);

        TurnoRenovacion.Resultado r = turno.renovar("viejo");
        assertEquals(TurnoRenovacion.Tipo.RENOVADA, r.tipo);
        assertEquals("nuevo", r.token);
        assertEquals(0, llamada.llamadas.get());
    }

    // ── Solo un 401 cierra (GP-237) ─────────────────────────────────────

    @Test
    public void sin_red_429_y_5xx_no_cierran_ni_tocan_los_tokens() {
        int[] codigos = {-1, 408, 429, 500, 502, 503, 504};
        for (int c : codigos) {
            TokensFalsos tokens = new TokensFalsos();
            TurnoRenovacion turno = new TurnoRenovacion(new LlamadaFalsa(codigo(c, "{}")), tokens, reloj::get);
            assertEquals("código " + c, c == 429 ? TurnoRenovacion.Tipo.LIMITADA : TurnoRenovacion.Tipo.SIN_RESPUESTA,
                    turno.renovar("viejo").tipo);
            assertEquals("viejo", tokens.access);
            assertEquals("r1", tokens.refresh);
        }
    }

    @Test
    public void un_200_sin_los_dos_tokens_es_sin_respuesta() {
        TokensFalsos tokens = new TokensFalsos();
        TurnoRenovacion turno = new TurnoRenovacion(
                new LlamadaFalsa(new TurnoRenovacion.Respuesta(200, "no es json", null, null)), tokens, reloj::get);
        assertEquals(TurnoRenovacion.Tipo.SIN_RESPUESTA, turno.renovar("viejo").tipo);
        assertEquals("r1", tokens.refresh);
    }

    @Test
    public void un_401_de_cuenta_desactivada_cierra_con_su_aviso_tambien_al_recordarlo() {
        TokensFalsos tokens = new TokensFalsos();
        LlamadaFalsa llamada = new LlamadaFalsa(codigo(401, "{\"cause\":\"CUENTA_DESACTIVADA\"}"));
        TurnoRenovacion turno = new TurnoRenovacion(llamada, tokens, reloj::get);

        TurnoRenovacion.Resultado r = turno.renovar("viejo");
        assertEquals(TurnoRenovacion.Tipo.CERRADA, r.tipo);
        assertTrue(r.cuentaDesactivada);
        assertTrue(turno.renovar("viejo").cuentaDesactivada);
    }

    @Test
    public void sin_refresh_guardado_no_llama() {
        TokensFalsos tokens = new TokensFalsos();
        tokens.refresh = null;
        LlamadaFalsa llamada = new LlamadaFalsa(ok());
        TurnoRenovacion turno = new TurnoRenovacion(llamada, tokens, reloj::get);
        assertEquals(TurnoRenovacion.Tipo.SIN_REFRESH, turno.renovar("viejo").tipo);
        assertEquals(0, llamada.llamadas.get());
    }
}
