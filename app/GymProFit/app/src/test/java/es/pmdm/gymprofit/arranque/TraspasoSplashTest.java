package es.pmdm.gymprofit.arranque;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// ============================================================
// TraspasoSplashTest — GP-223: cuándo se quita la copia del splash del sistema.
//
// La copia sin icono se queda debajo de la réplica hasta que el fotograma que pinta la
// réplica en su sitio se ha entregado y ha pasado otro, o hasta el límite. Nada de lo que
// espera al splash (la salida, el montaje de Inicio) empieza con la copia puesta.
//
// El respaldo (la escucha de salida no llega) cuenta en Android 12 o más desde que la app
// entrega su primer fotograma, y dura RESPALDO_MS: en el emulador la escucha llegó 630 ms
// después de ese fotograma, y contar desde la petición de salida daba el splash por ido
// antes de que la app se viera (traza de 9963274). Por debajo de Android 12 la copia se
// quita en el acto y el respaldo es el de siempre, ESPERA_SPLASH_MS desde la petición.
// ============================================================
public class TraspasoSplashTest {

    private static final long T0 = 70_000L;

    /** La pantalla falsa: apunta cada evento con el ms desde T0. */
    private static final class Pantalla implements TraspasoSplash.Pantalla {
        final List<String> eventos = new ArrayList<>();
        long ahora = T0;
        @Override public void quitarCopia() { eventos.add("quitar@" + (ahora - T0)); }
        @Override public void splashFuera() { eventos.add("fuera@" + (ahora - T0)); }
        @Override public void respaldo() { eventos.add("respaldo@" + (ahora - T0)); }
    }

    /** Avanza el reloj de tic en tic hasta {@code hasta} (ms desde T0), como el Handler. */
    private static void avanzar(TraspasoSplash t, Pantalla p, long hasta) {
        long fin = T0 + hasta;
        while (true) {
            long siguiente = t.siguienteTic();
            if (siguiente < 0 || siguiente > fin) break;
            p.ahora = Math.max(p.ahora, siguiente);
            t.tic(p.ahora);
        }
        p.ahora = fin;
    }

    // ── El traspaso ────────────────────────────────────────────────────────

    @Test
    public void laCopiaSeQuedaHastaQueSeEntregaLaReplicaYPasaOtroFotograma() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.encima(p.ahora);
        assertTrue("con la réplica encima, la copia sigue", p.eventos.isEmpty());
        assertFalse(t.fuera());
        avanzar(t, p, 40);
        t.entregado(p.ahora);
        assertTrue("entregada la réplica, aún falta un fotograma", p.eventos.isEmpty());
        avanzar(t, p, 56);
        t.fotograma(p.ahora);
        assertEquals(Arrays.asList("quitar@56", "fuera@56"), p.eventos);
        assertTrue(t.fuera());
        assertEquals(-1, t.siguienteTic());
    }

    @Test
    public void unFotogramaAntesDeEntregarseLaReplicaNoCuenta() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.encima(p.ahora);
        t.fotograma(p.ahora + 16);
        t.fotograma(p.ahora + 33);
        assertTrue(p.eventos.isEmpty());
        t.entregado(p.ahora + 40);
        assertTrue(p.eventos.isEmpty());
        p.ahora += 50;
        t.fotograma(p.ahora);
        assertEquals(Arrays.asList("quitar@50", "fuera@50"), p.eventos);
    }

    @Test
    public void siNoSeEntregaNuncaSeQuitaAlSegundo() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.encima(p.ahora);
        assertEquals(T0 + TraspasoSplash.LIMITE_MS, t.siguienteTic());
        avanzar(t, p, 999);
        assertTrue(p.eventos.isEmpty());
        avanzar(t, p, 1000);
        assertEquals(Arrays.asList("quitar@1000", "fuera@1000"), p.eventos);
    }

    @Test
    public void entregadaPeroSinOtroFotogramaTambienSeQuitaAlSegundo() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.encima(p.ahora);
        t.entregado(p.ahora + 30);
        avanzar(t, p, 1000);
        assertEquals(Arrays.asList("quitar@1000", "fuera@1000"), p.eventos);
    }

    @Test
    public void cadaCosaUnaVez() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.primerFotograma(p.ahora);
        t.esperar(p.ahora);
        t.encima(p.ahora);
        t.entregado(p.ahora);
        t.fotograma(p.ahora);
        t.fotograma(p.ahora + 16);
        t.entregado(p.ahora + 20);
        t.encima(p.ahora + 30);
        t.primerFotograma(p.ahora + 40);
        avanzar(t, p, 5000);
        assertEquals(Arrays.asList("quitar@0", "fuera@0"), p.eventos);
    }

    // ── El respaldo, en Android 12 o más ───────────────────────────────────

    @Test
    public void conLaEscuchaA630msDelPrimerFotogramaLaCopiaEsperaALaReplica() {
        // Como en la traza: la salida se pide antes del primer fotograma de la app, que llega
        // a los 100 ms, y la escucha 630 ms después de ese fotograma.
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.esperar(p.ahora);
        avanzar(t, p, 100);
        t.primerFotograma(p.ahora);
        avanzar(t, p, 730);
        assertTrue("el respaldo no salta antes de la escucha", p.eventos.isEmpty());
        t.encima(p.ahora);
        assertTrue("la copia se queda bajo la réplica", p.eventos.isEmpty());
        avanzar(t, p, 1200);
        t.entregado(p.ahora);
        avanzar(t, p, 1230);
        t.fotograma(p.ahora);
        assertEquals(Arrays.asList("quitar@1230", "fuera@1230"), p.eventos);
        avanzar(t, p, 5000);
        assertEquals(2, p.eventos.size());
    }

    @Test
    public void sinEscuchaElRespaldoSaltaALos2sDelPrimerFotogramaYNoAntes() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.esperar(p.ahora);
        avanzar(t, p, 1500);
        assertTrue("sin primer fotograma no corre", p.eventos.isEmpty());
        t.primerFotograma(p.ahora);
        avanzar(t, p, 1500 + TraspasoSplash.RESPALDO_MS - 1);
        assertTrue(p.eventos.isEmpty());
        avanzar(t, p, 1500 + TraspasoSplash.RESPALDO_MS);
        assertEquals(Arrays.asList("respaldo@3500", "fuera@3500"), p.eventos);
        assertTrue(t.fuera());
    }

    @Test
    public void unaSalidaPedidaTrasElPrimerFotogramaCuentaDesdeElFotograma() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.primerFotograma(p.ahora);
        avanzar(t, p, 1500);
        assertTrue("sin salida pedida no hay nada que respaldar", p.eventos.isEmpty());
        t.esperar(p.ahora);
        avanzar(t, p, 1999);
        assertTrue(p.eventos.isEmpty());
        avanzar(t, p, 2000);
        assertEquals(Arrays.asList("respaldo@2000", "fuera@2000"), p.eventos);
    }

    @Test
    public void unaSalidaPedidaPasadoElRespaldoSaleEnElActo() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.primerFotograma(p.ahora);
        avanzar(t, p, 2600);
        t.esperar(p.ahora);
        assertTrue("pide el tic ya", t.siguienteTic() <= p.ahora);
        avanzar(t, p, 2600);
        assertEquals(Arrays.asList("respaldo@2600", "fuera@2600"), p.eventos);
    }

    @Test
    public void laEscuchaCancelaElRespaldo() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.esperar(p.ahora);
        t.primerFotograma(p.ahora);
        avanzar(t, p, 1900);
        t.encima(p.ahora);
        avanzar(t, p, 2500);
        assertTrue("a los 2 s la copia sigue puesta: el respaldo ya no cuenta", p.eventos.isEmpty());
        avanzar(t, p, 2900);
        assertEquals(Arrays.asList("quitar@2900", "fuera@2900"), p.eventos);
    }

    @Test
    public void unaEscuchaQueLlegaTrasElRespaldoQuitaSuCopiaSinRepetirNada() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.esperar(p.ahora);
        t.primerFotograma(p.ahora);
        avanzar(t, p, 2100);
        t.encima(p.ahora);
        t.entregado(p.ahora);
        t.fotograma(p.ahora);
        avanzar(t, p, 5000);
        assertEquals(Arrays.asList("respaldo@2000", "fuera@2000", "quitar@2100"), p.eventos);
    }

    @Test
    public void sinEsperarNiEscuchaNoPideTics() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        assertEquals(-1, t.siguienteTic());
        t.primerFotograma(p.ahora);
        assertEquals(-1, t.siguienteTic());
        assertFalse(t.fuera());
    }

    // ── Por debajo de Android 12 ───────────────────────────────────────────

    @Test
    public void porDebajoDeAndroid12SeQuitaEnElActo() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(false, p);
        t.esperar(p.ahora);
        avanzar(t, p, 120);
        t.encima(p.ahora);
        assertEquals(Arrays.asList("quitar@120", "fuera@120"), p.eventos);
        assertTrue(t.fuera());
        assertEquals(-1, t.siguienteTic());
    }

    @Test
    public void porDebajoDeAndroid12ElRespaldoCuentaDesdeLaPeticion() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(false, p);
        t.esperar(p.ahora);
        avanzar(t, p, TraspasoSplash.ESPERA_SPLASH_MS - 1);
        assertTrue(p.eventos.isEmpty());
        avanzar(t, p, TraspasoSplash.ESPERA_SPLASH_MS);
        assertEquals(Arrays.asList("respaldo@600", "fuera@600"), p.eventos);
    }

    @Test
    public void porDebajoDeAndroid12EsperarDosVecesNoAlargaLaEspera() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(false, p);
        t.esperar(p.ahora);
        avanzar(t, p, 400);
        t.esperar(p.ahora);
        avanzar(t, p, 600);
        assertEquals(Collections.singletonList("respaldo@600"), p.eventos.subList(0, 1));
    }
}
