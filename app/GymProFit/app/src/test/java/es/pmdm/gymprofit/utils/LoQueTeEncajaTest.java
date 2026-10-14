package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.Racion;
import es.pmdm.gymprofit.model.alimento.UltimaCantidad;

// ============================================================
// LoQueTeEncajaTest — «Lo que te encaja» (lote 1.6.7, GP-165, decisión 7 del lienzo)
// Quién entra (lo que cabe en las kcal que quedan, con su cantidad, sin escalar), el orden
// (lo que sueles en esa comida delante; después la proteína que falta, o las kcal si ya no
// falta), el motivo de cada uno, los empates y cuándo no hay sección.
// ============================================================
public class LoQueTeEncajaTest {

    private static final Set<Integer> NADA = Collections.emptySet();

    // Un alimento por 100 g con su última cantidad en gramos (o sin última si gramos ≤ 0).
    private static Alimento alimento(int id, int kcal100, double prot100, double gramos) {
        Alimento a = new Alimento();
        a.setId(id);
        a.setNombre("a" + id);
        a.setCalorias(kcal100);
        a.setProteinas(prot100);
        if (gramos > 0) a.setUltima(new UltimaCantidad(gramos, null, null, null, null, null));
        return a;
    }

    // Un plato sin peso: sus valores son de una ración, que por dentro pesa 100 g.
    private static Alimento plato(int id, int kcalRacion, double protRacion) {
        Alimento a = alimento(id, kcalRacion, protRacion, 0);
        a.setPlatoRaciones(4);
        return a;
    }

    private static List<Alimento> lista(Alimento... a) {
        return new ArrayList<>(Arrays.asList(a));
    }

    private static Set<Integer> ids(Integer... i) {
        return new HashSet<>(Arrays.asList(i));
    }

    private static List<Integer> orden(LoQueTeEncaja.Resultado r) {
        List<Integer> ids = new ArrayList<>();
        for (LoQueTeEncaja.Sugerencia s : r.sugerencias) ids.add(s.alimento.getId());
        return ids;
    }

    // ── Quién entra ─────────────────────────────────────────────────────────

    @Test
    public void con_su_ultima_cantidad_y_sin_escalar() {
        // 200 g de 150 kcal/100 g = 300 kcal: con 299 no cabe, con 300 sí.
        Alimento a = alimento(1, 150, 10, 200);
        assertNull(LoQueTeEncaja.de(299, 50, lista(), lista(a), lista(), NADA, NADA, NADA));
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(300, 50, lista(), lista(a), lista(), NADA, NADA, NADA);
        assertNotNull(r);
        LoQueTeEncaja.Sugerencia s = r.sugerencias.get(0);
        assertEquals(300, s.kcal);
        assertEquals(20, s.proteina, 0.001);
        assertEquals(200, s.cantidad.gramos(), 0);
    }

    @Test
    public void lo_que_no_cabe_no_sale_aunque_cubra_mas_proteina() {
        Alimento grande = alimento(1, 500, 40, 200);  // 1000 kcal, 80 g
        Alimento chico = alimento(2, 100, 10, 100);   // 100 kcal, 10 g
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(600, 60, lista(grande), lista(chico), lista(), NADA, NADA, NADA);
        assertNotNull(r);
        assertEquals(Collections.singletonList(2), orden(r));
    }

    @Test
    public void sin_ultima_una_racion_y_un_plato_una_racion() {
        Alimento pan = alimento(1, 250, 9, 0);
        pan.setRaciones(new ArrayList<>(Collections.singletonList(new Racion(31, "1 rebanada", 28))));
        Alimento lentejas = plato(2, 474, 25);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 60, lista(lentejas), lista(pan), lista(), NADA, NADA, NADA);
        assertNotNull(r);
        LoQueTeEncaja.Sugerencia primero = r.sugerencias.get(0);
        assertEquals(2, primero.alimento.getId());
        assertEquals(474, primero.kcal);
        assertEquals(25, primero.proteina, 0.001);
        LoQueTeEncaja.Sugerencia segundo = r.sugerencias.get(1);
        assertEquals(28, segundo.cantidad.gramos(), 0);
        assertEquals(70, segundo.kcal);
    }

    @Test
    public void como_mucho_tres_y_sin_repetir_lo_que_esta_en_dos_listas() {
        Alimento a = alimento(1, 100, 30, 100);
        Alimento b = alimento(2, 100, 20, 100);
        Alimento c = alimento(3, 100, 10, 100);
        Alimento d = alimento(4, 100, 5, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 100, lista(a), lista(a, b), lista(b, c, d), NADA, NADA, NADA);
        assertNotNull(r);
        assertEquals(Arrays.asList(1, 2, 3), orden(r));
    }

    @Test
    public void lo_que_no_suma_nada_o_no_esta_guardado_no_sale() {
        Alimento agua = alimento(1, 0, 0, 250);
        Alimento externo = alimento(0, 100, 10, 100);
        assertNull(LoQueTeEncaja.de(1000, 50, lista(), lista(agua), lista(externo), NADA, NADA, NADA));
    }

    // ── Sin sección ─────────────────────────────────────────────────────────

    @Test
    public void sin_candidatos_no_hay_seccion() {
        assertNull(LoQueTeEncaja.de(1000, 50, lista(), lista(), lista(), NADA, NADA, NADA));
        assertNull(LoQueTeEncaja.de(1000, 50, null, null, null, null, null, NADA));
    }

    @Test
    public void sin_kcal_que_quedan_no_hay_seccion() {
        Alimento a = alimento(1, 100, 10, 100);
        assertNull(LoQueTeEncaja.de(0, 50, lista(a), lista(), lista(), NADA, NADA, NADA));
        assertNull(LoQueTeEncaja.de(-200, 50, lista(a), lista(), lista(), NADA, NADA, NADA));
    }

    // ── El orden ────────────────────────────────────────────────────────────

    @Test
    public void lo_que_sueles_en_esa_comida_va_delante_aunque_cubra_menos() {
        Alimento proteico = alimento(1, 100, 30, 100);
        Alimento yogur = alimento(2, 100, 5, 100);
        Alimento ayer = alimento(3, 100, 8, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 50, lista(), lista(proteico, yogur), lista(ayer),
                ids(2), ids(3), NADA);
        assertNotNull(r);
        // Lo habitual y lo de ayer delante, por proteína entre ellos; después, lo demás.
        assertEquals(Arrays.asList(3, 2, 1), orden(r));
        assertEquals(LoQueTeEncaja.Motivo.AYER, r.sugerencias.get(0).motivo);
        assertEquals(LoQueTeEncaja.Motivo.SUELES, r.sugerencias.get(1).motivo);
        assertEquals(LoQueTeEncaja.Motivo.PROTEINA, r.sugerencias.get(2).motivo);
    }

    @Test
    public void despues_lo_que_mas_cubre_de_la_proteina_que_falta() {
        Alimento poco = alimento(1, 100, 5, 100);
        Alimento mucho = alimento(2, 100, 30, 100);
        Alimento medio = alimento(3, 100, 15, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 50, lista(poco), lista(mucho), lista(medio), NADA, NADA, NADA);
        assertNotNull(r);
        assertEquals(Arrays.asList(2, 3, 1), orden(r));
    }

    @Test
    public void pasar_de_lo_que_falta_no_cubre_mas_que_cubrirlo_justo() {
        // Faltan 20 g: 40 g y 20 g cubren lo mismo (todo); decide la kcal, la menor.
        Alimento sobra = alimento(1, 400, 40, 100);
        Alimento justo = alimento(2, 150, 20, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 20, lista(sobra, justo), lista(), lista(), NADA, NADA, NADA);
        assertNotNull(r);
        assertEquals(Arrays.asList(2, 1), orden(r));
        assertEquals(200, r.sugerencias.get(1).cubre);
    }

    @Test
    public void sin_proteina_pendiente_el_que_mejor_encaja_en_las_kcal() {
        Alimento chico = alimento(1, 100, 30, 100);
        Alimento justo = alimento(2, 480, 1, 100);
        Alimento medio = alimento(3, 300, 10, 100);
        Alimento noCabe = alimento(4, 600, 50, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(500, 0.3, lista(chico), lista(justo, noCabe), lista(medio),
                NADA, NADA, NADA);
        assertNotNull(r);
        assertEquals(Arrays.asList(2, 3, 1), orden(r));
        assertEquals(0, r.proteinaQuedan);
        for (LoQueTeEncaja.Sugerencia s : r.sugerencias) {
            assertEquals(-1, s.cubre);
            assertEquals(LoQueTeEncaja.Motivo.CABE, s.motivo);
        }
    }

    @Test
    public void sin_proteina_pendiente_lo_que_sueles_sigue_delante() {
        Alimento justo = alimento(1, 480, 1, 100);
        Alimento habitual = alimento(2, 100, 1, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(500, -10, lista(justo), lista(habitual), lista(), ids(2), NADA, NADA);
        assertNotNull(r);
        assertEquals(Arrays.asList(2, 1), orden(r));
        assertEquals(LoQueTeEncaja.Motivo.SUELES, r.sugerencias.get(0).motivo);
    }

    // ── Empates ─────────────────────────────────────────────────────────────

    @Test
    public void con_la_misma_proteina_va_antes_el_de_menos_kcal() {
        Alimento caro = alimento(1, 300, 20, 100);
        Alimento barato = alimento(2, 120, 20, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 50, lista(caro, barato), lista(), lista(), NADA, NADA, NADA);
        assertNotNull(r);
        assertEquals(Arrays.asList(2, 1), orden(r));
    }

    @Test
    public void con_todo_igual_se_queda_el_orden_de_uso_platos_favoritos_recientes() {
        Alimento reciente = alimento(1, 100, 10, 100);
        Alimento favorito = alimento(2, 100, 10, 100);
        Alimento plato = plato(3, 100, 10);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 50, lista(plato), lista(favorito), lista(reciente),
                NADA, NADA, NADA);
        assertNotNull(r);
        assertEquals(Arrays.asList(3, 2, 1), orden(r));
    }

    @Test
    public void sin_proteina_pendiente_y_las_mismas_kcal_se_queda_el_orden_de_uso() {
        Alimento a = alimento(1, 200, 10, 100);
        Alimento b = alimento(2, 200, 30, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(500, 0, lista(), lista(a, b), lista(), NADA, NADA, NADA);
        assertNotNull(r);
        assertEquals(Arrays.asList(1, 2), orden(r));
    }

    // ── El motivo y la barra ────────────────────────────────────────────────

    @Test
    public void habitual_y_de_ayer_dice_lo_sueles() {
        Alimento a = alimento(1, 100, 10, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 50, lista(), lista(a), lista(), ids(1), ids(1), NADA);
        assertNotNull(r);
        assertEquals(LoQueTeEncaja.Motivo.SUELES, r.sugerencias.get(0).motivo);
    }

    @Test
    public void mucha_proteina_se_la_lleva_el_que_mas_cubre_de_los_que_salen() {
        Alimento cuarto = alimento(1, 100, 12.5, 100);   // 12,5 de 50: el 25 %
        Alimento menos = alimento(2, 100, 12.0, 100);    // el 24 %
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 50, lista(cuarto, menos), lista(), lista(), NADA, NADA, NADA);
        assertNotNull(r);
        assertEquals(LoQueTeEncaja.Motivo.PROTEINA, r.sugerencias.get(0).motivo);
        assertEquals(25, r.sugerencias.get(0).cubre);
        assertEquals(LoQueTeEncaja.Motivo.CABE, r.sugerencias.get(1).motivo);
        assertEquals(24, r.sugerencias.get(1).cubre);
    }

    @Test
    public void mucha_proteina_es_relativa_a_los_demas_no_un_umbral() {
        // Faltan 100 g: 8, 5 y 3 g son poco frente a lo que falta, pero el de 8 es el que más.
        Alimento ocho = alimento(1, 100, 8, 100);
        Alimento cinco = alimento(2, 100, 5, 100);
        Alimento tres = alimento(3, 100, 3, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 100, lista(tres, cinco, ocho), lista(), lista(),
                NADA, NADA, NADA);
        assertNotNull(r);
        assertEquals(Arrays.asList(1, 2, 3), orden(r));
        assertEquals(LoQueTeEncaja.Motivo.PROTEINA, r.sugerencias.get(0).motivo);
        assertEquals(LoQueTeEncaja.Motivo.CABE, r.sugerencias.get(1).motivo);
        assertEquals(LoQueTeEncaja.Motivo.CABE, r.sugerencias.get(2).motivo);
    }

    @Test
    public void con_mucha_proteina_pendiente_solo_uno_es_mucha_proteina() {
        // Antes, con 25 % fijo, los tres decían «Mucha proteína»: faltan 40 g y cubren 30, 20 y 15.
        Alimento a = alimento(1, 100, 30, 100);
        Alimento b = alimento(2, 100, 20, 100);
        Alimento c = alimento(3, 100, 15, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 40, lista(a, b, c), lista(), lista(), NADA, NADA, NADA);
        assertNotNull(r);
        int proteicos = 0;
        for (LoQueTeEncaja.Sugerencia s : r.sugerencias) if (s.motivo == LoQueTeEncaja.Motivo.PROTEINA) proteicos++;
        assertEquals(1, proteicos);
        assertEquals(LoQueTeEncaja.Motivo.PROTEINA, r.sugerencias.get(0).motivo);
    }

    @Test
    public void si_el_que_mas_cubre_es_lo_que_sueles_dice_lo_sueles_y_nadie_mas_es_mucha_proteina() {
        Alimento habitual = alimento(1, 100, 30, 100);
        Alimento otro = alimento(2, 100, 20, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 50, lista(), lista(habitual, otro), lista(),
                ids(1), NADA, NADA);
        assertNotNull(r);
        assertEquals(LoQueTeEncaja.Motivo.SUELES, r.sugerencias.get(0).motivo);
        assertEquals(LoQueTeEncaja.Motivo.CABE, r.sugerencias.get(1).motivo);
    }

    @Test
    public void solo_o_empatado_no_hay_con_quien_compararse() {
        Alimento solo = alimento(1, 100, 30, 100);
        LoQueTeEncaja.Resultado uno = LoQueTeEncaja.de(1000, 50, lista(solo), lista(), lista(), NADA, NADA, NADA);
        assertNotNull(uno);
        assertEquals(LoQueTeEncaja.Motivo.CABE, uno.sugerencias.get(0).motivo);

        Alimento a = alimento(2, 100, 20, 100);
        Alimento b = alimento(3, 100, 20, 100);
        LoQueTeEncaja.Resultado dos = LoQueTeEncaja.de(1000, 50, lista(a, b), lista(), lista(), NADA, NADA, NADA);
        assertNotNull(dos);
        for (LoQueTeEncaja.Sugerencia s : dos.sugerencias) assertEquals(LoQueTeEncaja.Motivo.CABE, s.motivo);
    }

    @Test
    public void el_que_no_cubre_nada_no_es_mucha_proteina() {
        Alimento algo = alimento(1, 100, 2, 100);
        Alimento nada = alimento(2, 100, 0, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 50, lista(algo, nada), lista(), lista(), NADA, NADA, NADA);
        assertNotNull(r);
        assertEquals(LoQueTeEncaja.Motivo.PROTEINA, r.sugerencias.get(0).motivo);
        assertEquals(LoQueTeEncaja.Motivo.CABE, r.sugerencias.get(1).motivo);
    }

    // ── Lo que ya está en esa comida ────────────────────────────────────────

    @Test
    public void no_propone_lo_que_ya_esta_apuntado_en_esa_comida_ese_dia() {
        Alimento apuntado = alimento(1, 100, 30, 100);
        Alimento otro = alimento(2, 100, 10, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 50, lista(apuntado), lista(apuntado, otro), lista(otro),
                ids(1), ids(1), ids(1));
        assertNotNull(r);
        assertEquals(Collections.singletonList(2), orden(r));
    }

    @Test
    public void si_todo_esta_ya_apuntado_no_hay_seccion() {
        Alimento a = alimento(1, 100, 30, 100);
        assertNull(LoQueTeEncaja.de(1000, 50, lista(a), lista(), lista(), NADA, NADA, ids(1)));
    }

    @Test
    public void la_cabecera_redondea_lo_que_queda() {
        Alimento a = alimento(1, 100, 10, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1161, 53.6, lista(a), lista(), lista(), NADA, NADA, NADA);
        assertNotNull(r);
        assertEquals(1161, r.kcalQuedan);
        assertEquals(54, r.proteinaQuedan);
        assertTrue(r.conProteina());
        assertEquals(19, r.sugerencias.get(0).cubre);
    }

    @Test
    public void con_menos_de_medio_gramo_pendiente_no_falta_proteina() {
        Alimento a = alimento(1, 100, 10, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 0.49, lista(a), lista(), lista(), NADA, NADA, NADA);
        assertNotNull(r);
        assertEquals(0, r.proteinaQuedan);
        assertEquals(-1, r.sugerencias.get(0).cubre);
    }

    // ── En la pestaña «Mis platos» ──────────────────────────────────────────

    @Test
    public void en_mis_platos_va_arriba_con_su_cabecera_y_una_tarjeta_por_sugerencia() {
        Alimento a = alimento(1, 100, 30, 100);
        Alimento b = alimento(2, 100, 20, 100);
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(1000, 50, lista(a), lista(b), lista(), NADA, NADA, NADA);
        List<GruposBusqueda.Elemento> l = GruposBusqueda.misPlatos(lista(a), lista(), r);
        assertEquals(GruposBusqueda.Tipo.ENCAJA_CABECERA, l.get(0).tipo);
        assertEquals(r, l.get(0).encaja);
        assertEquals(GruposBusqueda.Tipo.ENCAJA, l.get(1).tipo);
        assertEquals(1, l.get(1).alimento.getId());
        assertEquals(r.sugerencias.get(0), l.get(1).sugerencia);
        assertEquals(GruposBusqueda.Tipo.ENCAJA, l.get(2).tipo);
        assertEquals(GruposBusqueda.Seccion.MIS_PLATOS, l.get(3).seccion);
        assertEquals(3, GruposBusqueda.filasEncaja(l));
    }

    @Test
    public void sin_seccion_mis_platos_queda_como_siempre() {
        Alimento a = alimento(1, 100, 30, 100);
        assertEquals(GruposBusqueda.misPlatos(lista(a), lista()).size(),
                GruposBusqueda.misPlatos(lista(a), lista(), null).size());
        List<GruposBusqueda.Elemento> l = GruposBusqueda.misPlatos(lista(a), lista(), null);
        assertEquals(GruposBusqueda.Tipo.CABECERA, l.get(0).tipo);
        assertEquals(0, GruposBusqueda.filasEncaja(l));
    }
}
