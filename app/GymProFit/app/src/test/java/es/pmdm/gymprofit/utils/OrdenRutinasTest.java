package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.pmdm.gymprofit.model.programa.Programa;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.rutina.Rutina;

// ============================================================
// OrdenRutinasTest — un solo orden para «¿Qué rutina hoy?» y «¿Qué entrenaste?» (GP-241,
// punto 6): la que toca hoy; el resto del ciclo del programa en el orden en que viene tras
// la de hoy; las tuyas, en el orden de «Mis rutinas».
// ============================================================
public class OrdenRutinasTest {

    // Programa de cuatro días: A, B, C y D (ids 1 a 4). Propias: E, F y G (ids 5 a 7).
    private static final int A = 1, B = 2, C = 3, D = 4, E = 5, F = 6, G = 7;

    @Test
    public void con_programa_y_la_de_hoy_en_mitad_del_ciclo_el_resto_sigue_tras_ella() {
        // Las activas llegan mezcladas, como de la API: el orden no lo da la lista.
        List<Rutina> activas = Arrays.asList(propia(E), deprograma(B), propia(F), deprograma(D),
                deprograma(A), propia(G), deprograma(C));
        ProgramaQueSigue seguido = seguido(2, A, B, C, D);   // hoy toca C (posición 2)

        OrdenRutinas.Orden o = OrdenRutinas.ordenar(activas, seguido, C);

        assertEquals(Arrays.asList(C, D, A, B), ids(o.programa));
        assertEquals(Arrays.asList(E, F, G), ids(o.propias));
        assertEquals(Arrays.asList(C, D, A, B, E, F, G), ids(o.todas()));
        assertEquals("Cuerpo completo", o.nombrePrograma);
    }

    @Test
    public void con_programa_y_la_de_hoy_al_principio_va_en_el_orden_del_ciclo() {
        List<Rutina> activas = Arrays.asList(deprograma(C), deprograma(A), deprograma(B), propia(E));
        OrdenRutinas.Orden o = OrdenRutinas.ordenar(activas, seguido(0, A, B, C), A);

        assertEquals(Arrays.asList(A, B, C, E), ids(o.todas()));
    }

    @Test
    public void una_rutina_que_se_repite_en_el_ciclo_sale_una_vez_en_su_primer_sitio_tras_hoy() {
        List<Rutina> activas = Arrays.asList(deprograma(A), deprograma(B), deprograma(C));
        // A, B, A, C con hoy en la 1 (B): B, A (posición 2), C, y la A de la 0 ya ha salido.
        OrdenRutinas.Orden o = OrdenRutinas.ordenar(activas, seguido(1, A, B, A, C), B);

        assertEquals(Arrays.asList(B, A, C), ids(o.programa));
    }

    @Test
    public void sin_programa_la_de_hoy_primero_y_las_demas_en_el_orden_de_mis_rutinas() {
        List<Rutina> activas = Arrays.asList(propia(E), propia(F), propia(G));
        OrdenRutinas.Orden o = OrdenRutinas.ordenar(activas, null, F);

        assertTrue(o.programa.isEmpty());
        assertEquals(Arrays.asList(F, E, G), ids(o.propias));
        assertEquals(Arrays.asList(F, E, G), ids(o.todas()));
        assertNull(o.nombrePrograma);
    }

    @Test
    public void si_la_de_hoy_es_tuya_va_la_primera_tambien_delante_del_programa() {
        // Sin el programa que sigue (lo dejó y quedan sus rutinas), HoyToca elige entre las tuyas.
        List<Rutina> activas = Arrays.asList(deprograma(A), propia(E), deprograma(B), propia(F));
        OrdenRutinas.Orden o = OrdenRutinas.ordenar(activas, null, F);

        assertEquals(Arrays.asList(A, B), ids(o.programa));
        assertEquals(Arrays.asList(F, E), ids(o.propias));
        assertEquals(Arrays.asList(F, A, B, E), ids(o.todas()));
    }

    @Test
    public void sin_ninguna_que_toque_se_queda_el_orden_de_siempre() {
        List<Rutina> activas = Arrays.asList(propia(E), propia(F));
        assertEquals(Arrays.asList(E, F), ids(OrdenRutinas.ordenar(activas, null, null).todas()));
        assertTrue(OrdenRutinas.ordenar(null, null, null).todas().isEmpty());
    }

    @Test
    public void una_del_programa_que_no_esta_en_el_ciclo_va_al_final_del_programa() {
        List<Rutina> activas = Arrays.asList(deprograma(D), deprograma(A), deprograma(B), propia(E));
        OrdenRutinas.Orden o = OrdenRutinas.ordenar(activas, seguido(1, A, B), B);

        assertEquals(Arrays.asList(B, A, D), ids(o.programa));
    }

    @Test
    public void un_dia_borrado_del_ciclo_no_cuenta() {
        List<Rutina> activas = Arrays.asList(deprograma(A), deprograma(C));
        ProgramaQueSigue s = seguido(0, A, B, C);
        s.getCiclo().set(1, new ProgramaQueSigue.DiaCiclo(1, "P", B, "B", false, false));

        assertEquals(Arrays.asList(A, C), ids(OrdenRutinas.ordenar(activas, s, A).programa));
    }

    // ── Apoyo ─────────────────────────────────────────────────

    private static Rutina propia(int id) {
        Rutina r = new Rutina();
        r.setId(id);
        r.setNombre("R" + id);
        return r;
    }

    private static Rutina deprograma(int id) {
        Rutina r = propia(id);
        r.setProgramaUsuarioId(77);
        r.setProgramaNombre("Cuerpo completo");
        return r;
    }

    private static ProgramaQueSigue seguido(int posicionHoy, int... rutinas) {
        ProgramaQueSigue s = new ProgramaQueSigue();
        Programa p = new Programa();
        p.setNombre("Cuerpo completo");
        s.setPrograma(p);
        s.setPosicionHoy(posicionHoy);
        List<ProgramaQueSigue.DiaCiclo> ciclo = new ArrayList<>();
        for (int i = 0; i < rutinas.length; i++) {
            ciclo.add(new ProgramaQueSigue.DiaCiclo(i, "P" + i, rutinas[i], "R" + rutinas[i], true, false));
        }
        s.setCiclo(ciclo);
        return s;
    }

    private static List<Integer> ids(List<Rutina> rutinas) {
        List<Integer> ids = new ArrayList<>();
        for (Rutina r : rutinas) ids.add(r.getId());
        return ids;
    }
}
