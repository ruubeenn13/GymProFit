package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.programa.RutinaConEjercicios;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;

// ============================================================
// TuProgramaTest — qué va a «Tu programa», qué a «Mis rutinas» y de dónde sale «Hoy
// toca» (GP-074, lote 1.2.1).
// ============================================================
public class TuProgramaTest {

    private static Rutina propia(int id) {
        Rutina r = new Rutina();
        r.setId(id);
        r.setNombre("Propia " + id);
        return r;
    }

    private static RutinaConEjercicios delPrograma(int id, String nombre) {
        RutinaConEjercicios r = new RutinaConEjercicios();
        r.setId(id);
        r.setNombre(nombre);
        r.setProgramaUsuarioId(7);
        r.setPlantillaCodigo("GIM-" + nombre);
        return r;
    }

    // Sigue Torso y pierna; hoy toca la 2 (Pierna A) y la 1 ya está hecha.
    private static ProgramaQueSigue siguiendo() {
        ProgramaQueSigue s = new ProgramaQueSigue();
        s.setPosicionHoy(2);
        s.setCiclo(Arrays.asList(
                new ProgramaQueSigue.DiaCiclo(1, "TA", 11, "Torso A", true, true),
                new ProgramaQueSigue.DiaCiclo(2, "PA", 12, "Pierna A", true, false),
                new ProgramaQueSigue.DiaCiclo(3, "TB", 13, "Torso B", false, false),
                new ProgramaQueSigue.DiaCiclo(4, "PB", 14, "Pierna B", true, false)));
        s.setRutinas(new ArrayList<>(Arrays.asList(delPrograma(12, "Pierna A"), delPrograma(14, "Pierna B"),
                delPrograma(11, "Torso A"))));
        return s;
    }

    @Test
    public void mis_rutinas_son_solo_las_propias() {
        List<Rutina> activas = Arrays.asList(propia(1), delPrograma(12, "Pierna A"), propia(2));
        List<Rutina> mias = TuPrograma.misRutinas(activas);
        assertEquals(2, mias.size());
        assertEquals(1, mias.get(0).getId());
        assertEquals(2, mias.get(1).getId());
        assertTrue(TuPrograma.misRutinas(null).isEmpty());
    }

    @Test
    public void hoy_es_la_primera_que_manda_la_api_y_el_resto_las_demas_en_orden() {
        ProgramaQueSigue s = siguiendo();
        assertEquals("Pierna A", TuPrograma.hoy(s).getNombre());
        List<RutinaConEjercicios> resto = TuPrograma.resto(s);
        assertEquals(2, resto.size());
        assertEquals("Pierna B", resto.get(0).getNombre());
        assertEquals("Torso A", resto.get(1).getNombre());
        assertFalse(TuPrograma.todasBorradas(s));
    }

    @Test
    public void con_todas_borradas_no_toca_ninguna_y_se_dice() {
        ProgramaQueSigue s = siguiendo();
        s.setPosicionHoy(null);
        s.setRutinas(Collections.emptyList());
        assertNull(TuPrograma.hoy(s));
        assertTrue(TuPrograma.resto(s).isEmpty());
        assertTrue(TuPrograma.todasBorradas(s));
        assertFalse("sin programa no hay nada que borrar", TuPrograma.todasBorradas(null));
    }

    @Test
    public void la_barra_marca_hechas_hoy_pendientes_y_borradas() {
        assertEquals(Arrays.asList(TuPrograma.EstadoDia.HECHA, TuPrograma.EstadoDia.HOY,
                TuPrograma.EstadoDia.BORRADA, TuPrograma.EstadoDia.PENDIENTE), TuPrograma.barra(siguiendo()));
        assertEquals(1, TuPrograma.hechas(siguiendo()));
        assertTrue(TuPrograma.barra(null).isEmpty());
    }

    @Test
    public void siguiendo_un_programa_hoy_toca_sale_del_programa_y_no_de_mis_rutinas() {
        List<Rutina> activas = Arrays.asList(propia(1), delPrograma(12, "Pierna A"));
        HoyToca.Eleccion e = HoyToca.elegir(siguiendo(), activas, Collections.<SesionEntrenamiento>emptyList());
        assertEquals(12, e.rutina.getId());
        assertTrue(e.delPrograma);
    }

    @Test
    public void sin_programa_hoy_toca_es_la_regla_de_siempre_sobre_mis_rutinas() {
        List<Rutina> activas = Arrays.asList(propia(1), propia(2));
        HoyToca.Eleccion e = HoyToca.elegir(null, activas, Collections.<SesionEntrenamiento>emptyList());
        assertEquals(1, e.rutina.getId());
        assertFalse(e.delPrograma);
    }

    @Test
    public void con_todas_las_del_programa_borradas_hoy_toca_vuelve_a_mis_rutinas() {
        ProgramaQueSigue s = siguiendo();
        s.setPosicionHoy(null);
        s.setRutinas(Collections.emptyList());
        HoyToca.Eleccion e = HoyToca.elegir(s, Arrays.asList(propia(5)), null);
        assertEquals(5, e.rutina.getId());
        assertFalse(e.delPrograma);
        assertNull(HoyToca.elegir(s, Collections.<Rutina>emptyList(), null));
    }

    @Test
    public void enumerar_como_en_una_frase() {
        assertEquals("", TuPrograma.enumerar(Collections.<String>emptyList(), ", ", " ni "));
        assertEquals("A", TuPrograma.enumerar(Collections.singletonList("A"), ", ", " ni "));
        assertEquals("A ni B", TuPrograma.enumerar(Arrays.asList("A", "B"), ", ", " ni "));
        assertEquals("A, B ni C", TuPrograma.enumerar(Arrays.asList("A", "B", "C"), ", ", " ni "));
    }
}
