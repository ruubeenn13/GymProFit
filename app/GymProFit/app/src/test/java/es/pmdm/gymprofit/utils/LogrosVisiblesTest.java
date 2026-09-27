package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import es.pmdm.gymprofit.model.logro.LogroProgreso;

// ============================================================
// LogrosVisiblesTest — GP-114: los logros que dependen de objetivos personales no se
// enseñan mientras no haya pantalla para crear objetivos.
// ============================================================
public class LogrosVisiblesTest {

    private static final List<LogroProgreso> CATALOGO = Arrays.asList(
            new LogroProgreso("PRIMERA_SESION", true),
            new LogroProgreso("OBJETIVO_CUMPLIDO", true),
            new LogroProgreso("CONSTANCIA", false),
            new LogroProgreso("MAQUINA", false));

    @Test
    public void objetivo_cumplido_y_maquina_no_salen() {
        List<LogroProgreso> visibles = LogrosVisibles.filtrar(CATALOGO);
        assertEquals(2, visibles.size());
        assertEquals("PRIMERA_SESION", visibles.get(0).getTipo());
        assertEquals("CONSTANCIA", visibles.get(1).getTipo());
    }

    @Test
    public void la_cifra_de_progreso_tampoco_los_cuenta() {
        assertEquals(1, LogrosVisibles.conseguidos(CATALOGO));
        assertEquals(0, LogrosVisibles.conseguidos(null));
    }
}
