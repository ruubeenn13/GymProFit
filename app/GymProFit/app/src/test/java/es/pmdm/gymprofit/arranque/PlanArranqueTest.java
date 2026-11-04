package es.pmdm.gymprofit.arranque;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// PlanArranqueTest — GP-223, punto 6: los tiempos del arranque con un reloj falso.
//
// El reloj lo lleva el test: el plan dice cuándo quiere el siguiente tic
// (siguienteTic) y el test avanza hasta ahí, como hará el Handler de la pantalla.
// Así se prueba a la vez qué pasa en cada instante y que el plan pide los tics
// que necesita, ni antes ni después.
// ============================================================
public class PlanArranqueTest {

    private static final long TOCADO = 50_000L;

    /** La pantalla falsa: apunta cada evento con el ms desde que se tocó el icono. */
    private static final class Pantalla implements PlanArranque.Pantalla {
        final List<String> eventos = new ArrayList<>();
        long ahora;
        @Override public void empezarBrillo() { eventos.add("brillo@" + (ahora - TOCADO)); }
        @Override public void mostrarConectando() { eventos.add("conectando@" + (ahora - TOCADO)); }
        @Override public void agotado() { eventos.add("agotado@" + (ahora - TOCADO)); }
        @Override public void salir() { eventos.add("salir@" + (ahora - TOCADO)); }
    }

    /** Avanza el reloj de tic en tic hasta {@code hasta} (ms desde el toque). */
    private static void avanzar(PlanArranque plan, Pantalla p, long hasta) {
        long fin = TOCADO + hasta;
        while (true) {
            long siguiente = plan.siguienteTic(p.ahora);
            if (siguiente < 0 || siguiente > fin) break;
            p.ahora = Math.max(p.ahora, siguiente);
            plan.tic(p.ahora);
        }
        p.ahora = fin;
    }

    private static Pantalla empezar() {
        Pantalla p = new Pantalla();
        p.ahora = TOCADO;
        return p;
    }

    @Test
    public void siSabeAdondeIrProntoEsperaAQueAcabeLaEntrada() {
        Pantalla p = empezar();
        PlanArranque plan = PlanArranque.alTocar(TOCADO, false, p);
        avanzar(plan, p, 200);
        plan.decidido(p.ahora);
        assertTrue("el splash sigue mientras entra la G", plan.mantenerSplash(p.ahora));
        avanzar(plan, p, 699);
        assertTrue(p.eventos.isEmpty());
        assertTrue(plan.mantenerSplash(p.ahora));
        avanzar(plan, p, 2000);
        assertEquals(List.of("salir@700"), p.eventos);
        assertFalse(plan.mantenerSplash(TOCADO + 700));
    }

    @Test
    public void siSabeAdondeIrTrasLaEntradaSaleAlMomento() {
        Pantalla p = empezar();
        PlanArranque plan = PlanArranque.alTocar(TOCADO, false, p);
        avanzar(plan, p, 900);
        plan.decidido(p.ahora);
        assertEquals(List.of("salir@900"), p.eventos);
        assertFalse(plan.mantenerSplash(p.ahora));
    }

    @Test
    public void aUnSegundoSinSaberQuitaElSplashYEmpiezaElBrillo() {
        Pantalla p = empezar();
        PlanArranque plan = PlanArranque.alTocar(TOCADO, false, p);
        avanzar(plan, p, 999);
        assertTrue(plan.mantenerSplash(p.ahora));
        assertTrue(p.eventos.isEmpty());
        avanzar(plan, p, 1000);
        assertFalse("a 1 s el splash del sistema se va", plan.mantenerSplash(p.ahora));
        assertEquals(List.of("brillo@1000"), p.eventos);
        avanzar(plan, p, 1500);
        plan.decidido(p.ahora);
        assertEquals(List.of("brillo@1000", "salir@1500"), p.eventos);
    }

    @Test
    public void aLosTresSegundosDiceConectando() {
        Pantalla p = empezar();
        PlanArranque plan = PlanArranque.alTocar(TOCADO, false, p);
        avanzar(plan, p, 2999);
        assertEquals(List.of("brillo@1000"), p.eventos);
        avanzar(plan, p, 3000);
        assertEquals(List.of("brillo@1000", "conectando@3000"), p.eventos);
    }

    @Test
    public void aLosQuinceSegundosSeRindeYNoSaleDespues() {
        Pantalla p = empezar();
        PlanArranque plan = PlanArranque.alTocar(TOCADO, false, p);
        avanzar(plan, p, 14_999);
        assertEquals(List.of("brillo@1000", "conectando@3000"), p.eventos);
        avanzar(plan, p, 15_000);
        assertEquals(List.of("brillo@1000", "conectando@3000", "agotado@15000"), p.eventos);
        // La respuesta que llega tarde ya no cambia nada: la petición está cancelada.
        plan.decidido(TOCADO + 15_200);
        avanzar(plan, p, 30_000);
        assertEquals(3, p.eventos.size());
        assertEquals(-1, plan.siguienteTic(p.ahora));
    }

    @Test
    public void conLasAnimacionesQuitadasNoEsperaALaEntradaNiBrilla() {
        Pantalla p = empezar();
        PlanArranque plan = PlanArranque.alTocar(TOCADO, true, p);
        avanzar(plan, p, 100);
        plan.decidido(p.ahora);
        assertEquals(List.of("salir@100"), p.eventos);
        assertFalse(plan.mantenerSplash(p.ahora));

        Pantalla q = empezar();
        PlanArranque lento = PlanArranque.alTocar(TOCADO, true, q);
        avanzar(lento, q, 3000);
        assertFalse(lento.mantenerSplash(q.ahora));
        assertEquals("sin brillo: va en bucle", List.of("conectando@3000"), q.eventos);
    }

    @Test
    public void unReintentoNoTieneSplashNiEntradaPeroSiSusTiempos() {
        Pantalla p = empezar();
        PlanArranque plan = PlanArranque.reintento(TOCADO, false, p);
        assertFalse(plan.mantenerSplash(p.ahora));
        avanzar(plan, p, 100);
        plan.decidido(p.ahora);
        assertEquals(List.of("salir@100"), p.eventos);

        Pantalla q = empezar();
        PlanArranque lento = PlanArranque.reintento(TOCADO, false, q);
        avanzar(lento, q, 15_000);
        assertEquals(List.of("brillo@1000", "conectando@3000", "agotado@15000"), q.eventos);
    }

    @Test
    public void salirSoloUnaVez() {
        Pantalla p = empezar();
        PlanArranque plan = PlanArranque.alTocar(TOCADO, false, p);
        plan.decidido(p.ahora);
        plan.decidido(p.ahora);
        avanzar(plan, p, 5000);
        plan.tic(TOCADO + 6000);
        assertEquals(List.of("salir@700"), p.eventos);
    }

    @Test
    public void losTiemposSonLosDeLaFicha() {
        assertEquals(1000, PlanArranque.SPLASH_SISTEMA_MS);
        assertEquals(3000, PlanArranque.CONECTANDO_MS);
        assertEquals(15_000, PlanArranque.LIMITE_MS);
        assertEquals(700, PlanArranque.ENTRADA_MS);
    }
}
