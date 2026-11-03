package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;

import es.pmdm.gymprofit.model.medicion.MedicionCorporal;

// ============================================================
// MedicionHoyTest — una medición por día (GP-216, lote 4): «Apuntar peso» de Progreso y Medidas
// corrigen la de hoy o crean la de hoy copiando la última, sin perder lo que no se toca.
// ============================================================
public class MedicionHoyTest {

    private static MedicionCorporal medicion(String fecha) {
        MedicionCorporal m = new MedicionCorporal();
        m.setFecha(fecha);
        m.setPeso(80.5);
        m.setCintura(84);
        m.setNotas("tras vacaciones");
        return m;
    }

    @Test
    public void esDeHoySoloSiCoincideElDia() {
        assertTrue(MedicionHoy.esDeHoy(medicion("2026-11-03T08:15:00"), "2026-11-03"));
        assertFalse(MedicionHoy.esDeHoy(medicion("2026-11-02T23:59:00"), "2026-11-03"));
        assertFalse(MedicionHoy.esDeHoy(null, "2026-11-03"));
        assertFalse(MedicionHoy.esDeHoy(medicion(null), "2026-11-03"));
    }

    @Test
    public void laCopiaLlevaLoQueTeniaLaUltima() {
        Map<String, Object> b = MedicionHoy.copiaDe(medicion("2026-11-02T08:00:00"), 7);
        assertEquals(7, b.get("usuarioId"));
        assertEquals(BigDecimal.valueOf(80.5), b.get("peso"));
        assertEquals(BigDecimal.valueOf(84.0), b.get("cintura"));
        assertEquals("tras vacaciones", b.get("notas"));
        assertFalse("lo que no se midió no se inventa", b.containsKey("pecho"));
        assertFalse(b.containsKey("grasaCorporal"));
    }

    @Test
    public void sinUltimaSoloElUsuario() {
        Map<String, Object> b = MedicionHoy.copiaDe(null, 7);
        assertEquals(1, b.size());
        assertEquals(7, b.get("usuarioId"));
    }
}
