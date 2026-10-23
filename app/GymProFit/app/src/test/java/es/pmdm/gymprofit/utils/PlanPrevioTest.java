package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// PlanPrevioTest — el plan que enseña «Tus datos y objetivos» antes de guardar (GP-216,
// tablero 18). Usa la fórmula real de la app (CalculadoraNutricional, DEC-054), no la del
// lienzo: lo que se ve antes de guardar es lo que se guarda.
// ============================================================
public class PlanPrevioTest {

    private static DatosPerfil dani() {
        return new DatosPerfil("Dani", 78.6, 178, 29, "HOMBRE",
                CalculadoraNutricional.ACTIVIDAD_MODERADO,
                CalculadoraNutricional.OBJETIVO_PERDER_PESO, "INTERMEDIO");
    }

    @Test
    public void elPlanEsElDeLaCalculadoraRealConSusProteinasDeDEC054() {
        PlanPrevio p = PlanPrevio.de(dani(), null, 0L, 0, 0);
        ResultadoNutricional r = CalculadoraNutricional.calcular(78.6, 178, 29, true,
                CalculadoraNutricional.ACTIVIDAD_MODERADO, CalculadoraNutricional.OBJETIVO_PERDER_PESO);
        assertNotNull(p);
        assertEquals(r.calorias, p.kcal);
        assertEquals(r.proteinas, p.proteinas);
        assertEquals(r.proteinaMinima, p.proteinaMinima);
        // Perder grasa, sin %: 2,0 g/kg del peso (IMC < 30). No los 2,0 / 2,2 de antes de GP-206.
        assertEquals((int) (78.6 * 2.0), p.proteinas);
    }

    @Test
    public void conGrasaRecienteLaProteinaVaSobreMasaMagra() {
        long hoy = 10L * 24 * 60 * 60 * 1000;
        GrasaReciente.Medida m = new GrasaReciente.Medida(20, hoy - 5L * 24 * 60 * 60 * 1000);
        PlanPrevio con = PlanPrevio.de(dani(), m, hoy, 0, 0);
        PlanPrevio sin = PlanPrevio.de(dani(), null, hoy, 0, 0);
        assertEquals((int) (78.6 * 0.8 * 2.4), con.proteinas);
        assertNotEquals(sin.proteinas, con.proteinas);
    }

    @Test
    public void ladiferenciaEsContraLasKcalGuardadas() {
        PlanPrevio base = PlanPrevio.de(dani(), null, 0L, 0, 0);
        PlanPrevio igual = PlanPrevio.de(dani(), null, 0L, base.kcal, base.proteinas);
        assertEquals(0, igual.difKcal);
        assertEquals(0, igual.difProteinas);

        PlanPrevio mas = PlanPrevio.de(dani().conPeso(80.0), null, 0L, base.kcal, base.proteinas);
        assertTrue(mas.difKcal > 0);
        assertTrue(mas.difProteinas > 0);

        PlanPrevio menos = PlanPrevio.de(dani().conEdad(40), null, 0L, base.kcal, base.proteinas);
        assertTrue(menos.difKcal < 0);
    }

    @Test
    public void cambiarElObjetivoCambiaElPlan() {
        PlanPrevio perder = PlanPrevio.de(dani(), null, 0L, 0, 0);
        PlanPrevio ganar = PlanPrevio.de(dani().conObjetivo(CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR),
                null, 0L, 0, 0);
        assertTrue(ganar.kcal > perder.kcal);
    }

    @Test
    public void menorDeDiecisieteQuePierdeGrasaDaMantenimientoYLoDice() {
        PlanPrevio menor = PlanPrevio.de(dani().conEdad(16), null, 0L, 0, 0);
        PlanPrevio mantener = PlanPrevio.de(dani().conEdad(16)
                .conObjetivo(CalculadoraNutricional.OBJETIVO_MANTENER_PESO), null, 0L, 0, 0);
        assertEquals(mantener.kcal, menor.kcal);
        assertTrue(menor.mantenimientoPorEdad);
        assertFalse(mantener.mantenimientoPorEdad);
    }

    @Test
    public void sinPesoOAlturaOEdadValidosNoHayPlan() {
        assertNull(PlanPrevio.de(dani().conPeso(0), null, 0L, 0, 0));
        assertNull(PlanPrevio.de(dani().conAltura(0), null, 0L, 0, 0));
        assertNull(PlanPrevio.de(dani().conEdad(0), null, 0L, 0, 0));
    }

    // ── DatosPerfil: los botones \u2212/+ ─────────────────────────────────────────

    @Test
    public void elPesoSubeYBajaDeUnaDecimaSinArrastrarErroresDeComa() {
        DatosPerfil d = dani();
        for (int i = 0; i < 10; i++) d = d.masPeso();
        assertEquals(79.6, d.peso, 1e-9);
        for (int i = 0; i < 30; i++) d = d.menosPeso();
        assertEquals(76.6, d.peso, 1e-9);
    }

    @Test
    public void losBotonesSePararnEnLosLimites() {
        assertEquals(DatosPerfil.PESO_MIN, dani().conPeso(DatosPerfil.PESO_MIN).menosPeso().peso, 0);
        assertEquals(DatosPerfil.PESO_MAX, dani().conPeso(DatosPerfil.PESO_MAX).masPeso().peso, 0);
        assertEquals(DatosPerfil.ALTURA_MIN, dani().conAltura(DatosPerfil.ALTURA_MIN).menosAltura().altura);
        assertEquals(DatosPerfil.ALTURA_MAX, dani().conAltura(DatosPerfil.ALTURA_MAX).masAltura().altura);
        assertEquals(ReglasEdad.MINIMA, dani().conEdad(ReglasEdad.MINIMA).menosEdad().edad);
        assertEquals(ReglasEdad.MAXIMA, dani().conEdad(ReglasEdad.MAXIMA).masEdad().edad);
    }

    // ── «Guardar» apagado si nada cambia ──────────────────────────────────────

    @Test
    public void guardarSoloSeEnciendeSiAlgoCambia() {
        DatosPerfil inicial = dani();
        assertFalse(inicial.distintoDe(dani()));
        assertTrue(inicial.distintoDe(inicial.conPeso(78.7)));
        assertTrue(inicial.distintoDe(inicial.conNombre("Daniel")));
        assertTrue(inicial.distintoDe(inicial.conNivel("AVANZADO")));
        assertTrue(inicial.distintoDe(inicial.conSexo("MUJER")));
    }

    @Test
    public void volverAlValorInicialApagaGuardarOtraVez() {
        DatosPerfil inicial = dani();
        DatosPerfil cambiado = inicial.masPeso();
        assertTrue(inicial.distintoDe(cambiado));
        assertFalse(inicial.distintoDe(cambiado.menosPeso()));
    }

    @Test
    public void elNombreEnBlancoYConEspaciosEsElMismoNombre() {
        assertFalse(dani().conNombre("").distintoDe(dani().conNombre("   ")));
        assertFalse(dani().conNombre("Dani").distintoDe(dani().conNombre(" Dani ")));
    }
}
