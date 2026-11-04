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
// espera al splash (la salida, el montaje de Inicio) empieza con la copia puesta, tampoco
// por la espera de ESPERA_SPLASH_MS. Por debajo de Android 12 se quita en el acto.
// ============================================================
public class TraspasoSplashTest {

    private static final long T0 = 70_000L;

    /** La pantalla falsa: apunta cada evento con el ms desde T0. */
    private static final class Pantalla implements TraspasoSplash.Pantalla {
        final List<String> eventos = new ArrayList<>();
        long ahora = T0;
        @Override public void quitarCopia() { eventos.add("quitar@" + (ahora - T0)); }
        @Override public void splashFuera() { eventos.add("fuera@" + (ahora - T0)); }
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
    public void laEsperaDelSplashNoSacaNadaConLaCopiaPuesta() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        // La salida ya espera al splash cuando llega la escucha de salida.
        t.esperar(p.ahora);
        avanzar(t, p, 100);
        t.encima(p.ahora);
        avanzar(t, p, TraspasoSplash.ESPERA_SPLASH_MS + 50);
        assertTrue("a los 600 ms la copia sigue puesta: nada sale", p.eventos.isEmpty());
        assertFalse(t.fuera());
        avanzar(t, p, 1099);
        assertTrue(p.eventos.isEmpty());
        avanzar(t, p, 1100);
        assertEquals(Arrays.asList("quitar@1100", "fuera@1100"), p.eventos);
    }

    @Test
    public void siLaEscuchaNoLlegaLaEsperaDaElSplashPorIdo() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.esperar(p.ahora);
        avanzar(t, p, 599);
        assertTrue(p.eventos.isEmpty());
        avanzar(t, p, 600);
        assertEquals(Collections.singletonList("fuera@600"), p.eventos);
    }

    @Test
    public void unaEscuchaQueLlegaTardeQuitaSuCopiaSinRepetirNada() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.esperar(p.ahora);
        avanzar(t, p, 600);
        avanzar(t, p, 700);
        t.encima(p.ahora);
        t.entregado(p.ahora);
        t.fotograma(p.ahora);
        avanzar(t, p, 3000);
        assertEquals(Arrays.asList("fuera@600", "quitar@700"), p.eventos);
    }

    @Test
    public void esperarDosVecesNoAlargaLaEspera() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.esperar(p.ahora);
        avanzar(t, p, 400);
        t.esperar(p.ahora);
        avanzar(t, p, 600);
        assertEquals(Collections.singletonList("fuera@600"), p.eventos);
    }

    @Test
    public void sinEsperarNiEscuchaNoPideTics() {
        TraspasoSplash t = TraspasoSplash.nuevo(true, new Pantalla());
        assertEquals(-1, t.siguienteTic());
        assertFalse(t.fuera());
    }

    @Test
    public void cadaCosaUnaVez() {
        Pantalla p = new Pantalla();
        TraspasoSplash t = TraspasoSplash.nuevo(true, p);
        t.encima(p.ahora);
        t.entregado(p.ahora);
        t.fotograma(p.ahora);
        t.fotograma(p.ahora + 16);
        t.entregado(p.ahora + 20);
        t.encima(p.ahora + 30);
        avanzar(t, p, 2000);
        assertEquals(Arrays.asList("quitar@0", "fuera@0"), p.eventos);
    }

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
}
