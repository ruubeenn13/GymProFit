package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.model.alimento.Alimento;

// ============================================================
// GruposBusquedaTest — las listas de Añadir y de Buscar (lote 1.6.1)
// Sin escribir (o con menos de 2 letras): «Recientes» (TUYO) y «Habituales» (BASICO),
// sin «¿No lo encuentras?». Buscando: Tuyo, Básicos y Productos, en ese orden, cada
// uno con su cabecera solo si tiene algo, y al final «¿No lo encuentras?», también
// cuando no hay nada.
// ============================================================
public class GruposBusquedaTest {

    private static Alimento a(String nombre, String grupo) {
        Alimento x = new Alimento();
        x.setNombre(nombre);
        x.setGrupo(grupo);
        return x;
    }

    @Test
    public void menos_de_dos_letras_no_es_buscar() {
        assertFalse(GruposBusqueda.esBusqueda(null));
        assertFalse(GruposBusqueda.esBusqueda(""));
        assertFalse(GruposBusqueda.esBusqueda(" p "));
        assertTrue(GruposBusqueda.esBusqueda("po"));
    }

    @Test
    public void sin_texto_recientes_y_habituales() {
        List<GruposBusqueda.Elemento> l = GruposBusqueda.de(Arrays.asList(
                a("Yogur", "TUYO"), a("Pan", "BASICO"), a("Huevo", "BASICO")), false);
        assertEquals(5, l.size());
        assertEquals(GruposBusqueda.Seccion.RECIENTES, l.get(0).seccion);
        assertTrue(l.get(0).esCabecera());
        assertEquals("Yogur", l.get(1).alimento.getNombre());
        assertEquals(GruposBusqueda.Seccion.HABITUALES, l.get(2).seccion);
        assertFalse(GruposBusqueda.tieneNoLoEncuentras(l));
    }

    @Test
    public void sin_nada_tuyo_solo_habituales() {
        List<GruposBusqueda.Elemento> l = GruposBusqueda.de(Collections.singletonList(a("Pan", "BASICO")), false);
        assertEquals(GruposBusqueda.Seccion.HABITUALES, l.get(0).seccion);
        assertEquals(2, l.size());
    }

    @Test
    public void buscando_tres_grupos_en_orden_y_al_final_no_lo_encuentras() {
        Alimento producto = a("Pechuga en lonchas", "PRODUCTO");
        List<GruposBusqueda.Elemento> l = GruposBusqueda.de(Arrays.asList(
                a("Mi pollo", "TUYO"), a("Pechuga", "BASICO"), producto), true);
        assertEquals(GruposBusqueda.Seccion.TUYO, l.get(0).seccion);
        assertEquals(GruposBusqueda.Seccion.BASICOS, l.get(2).seccion);
        assertEquals(GruposBusqueda.Seccion.PRODUCTOS, l.get(4).seccion);
        assertSame(producto, l.get(5).alimento);
        assertEquals(GruposBusqueda.Tipo.NO_LO_ENCUENTRAS, l.get(6).tipo);
        assertEquals(7, l.size());
    }

    @Test
    public void buscando_sin_resultados_solo_no_lo_encuentras() {
        List<GruposBusqueda.Elemento> l = GruposBusqueda.de(Collections.emptyList(), true);
        assertEquals(1, l.size());
        assertEquals(GruposBusqueda.Tipo.NO_LO_ENCUENTRAS, l.get(0).tipo);
    }

    @Test
    public void un_grupo_desconocido_o_sin_grupo_va_con_los_basicos() {
        List<GruposBusqueda.Elemento> l = GruposBusqueda.de(Arrays.asList(a("Raro", null), a("Otro", "NUEVO")), true);
        assertEquals(GruposBusqueda.Seccion.BASICOS, l.get(0).seccion);
        assertEquals(4, l.size());
    }

    @Test
    public void la_pagina_siguiente_continua_el_grupo_sin_repetir_cabecera() {
        List<GruposBusqueda.Elemento> l = GruposBusqueda.de(Arrays.asList(
                a("B1", "BASICO"), a("P1", "PRODUCTO"), a("P2", "PRODUCTO")), true);
        long cabecerasProductos = l.stream().filter(e -> e.esCabecera() && e.seccion == GruposBusqueda.Seccion.PRODUCTOS).count();
        assertEquals(1, cabecerasProductos);
    }

    // ── Lote 1.6.3 ──────────────────────────────────────────────────────────

    private static java.util.List<Alimento> recientes(int n) {
        java.util.List<Alimento> l = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) l.add(a("Tuyo " + i, "TUYO"));
        l.add(a("Pan", "BASICO"));
        return l;
    }

    @Test
    public void recientes_seis_y_ver_todos() {
        List<GruposBusqueda.Elemento> l = GruposBusqueda.de(recientes(9), false, false);
        assertTrue(l.get(0).verTodos);
        // Cabecera, seis recientes, cabecera de habituales y el pan.
        assertEquals(1 + 6 + 2, l.size());
        assertEquals("Tuyo 5", l.get(6).alimento.getNombre());
        assertEquals(GruposBusqueda.Seccion.HABITUALES, l.get(7).seccion);
        // «Ver todos» despliega el resto ahí mismo.
        List<GruposBusqueda.Elemento> todos = GruposBusqueda.de(recientes(9), false, true);
        assertFalse(todos.get(0).verTodos);
        assertEquals(1 + 9 + 2, todos.size());
    }

    @Test
    public void con_seis_o_menos_no_hay_ver_todos() {
        assertFalse(GruposBusqueda.de(recientes(6), false, false).get(0).verTodos);
        assertEquals(1 + 6 + 2, GruposBusqueda.de(recientes(6), false, false).size());
    }

    @Test
    public void favoritos_con_la_propuesta_arriba_y_cuantos() {
        Alimento pan = a("Pan integral", null);
        List<GruposBusqueda.Elemento> l = GruposBusqueda.favoritos(
                Arrays.asList(a("Yogur", null), a("Pollo", null)),
                new es.pmdm.gymprofit.model.alimento.Favoritos.Propuesta(pan, 4));
        assertEquals(GruposBusqueda.Tipo.PROPUESTA, l.get(0).tipo);
        assertSame(pan, l.get(0).alimento);
        assertEquals(4, l.get(0).propuesta.getVeces());
        assertEquals(GruposBusqueda.Seccion.FAVORITOS, l.get(1).seccion);
        assertEquals(2, l.get(1).cuantos);
        assertEquals(4, l.size());
    }

    @Test
    public void sin_favoritos_ni_propuesta_no_hay_nada() {
        assertTrue(GruposBusqueda.favoritos(Collections.emptyList(), null).isEmpty());
        // Con propuesta y sin favoritos, solo la propuesta.
        assertEquals(1, GruposBusqueda.favoritos(null,
                new es.pmdm.gymprofit.model.alimento.Favoritos.Propuesta(a("Pan", null), 5)).size());
    }
}
