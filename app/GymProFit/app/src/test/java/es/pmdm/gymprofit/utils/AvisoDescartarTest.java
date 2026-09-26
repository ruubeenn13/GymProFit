package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.ui.adapters.EjercicioPesoAdapter;

// ============================================================
// AvisoDescartarTest — GP-098: el aviso sale solo si hay algo que perder.
//
// Sin cambios, salir es directo; con algo apuntado, se pregunta. Lo difícil es
// Registrar sesión, cuyas series nacen con las repeticiones del plan ya escritas:
// eso no es algo apuntado, y si contara el aviso saldría siempre.
// ============================================================
public class AvisoDescartarTest {

    private static List<EjercicioPesoAdapter.Item> unEjercicio() {
        List<EjercicioPesoAdapter.Item> items = new ArrayList<>();
        items.add(new EjercicioPesoAdapter.Item(1, "Press banca", 3, 10));
        return items;
    }

    @Test
    public void sesion_recien_abierta_no_avisa() {
        assertFalse(AvisoDescartar.sesionConDatos("", "", 0f, unEjercicio()));
        assertFalse(AvisoDescartar.sesionConDatos(null, "  ", 0f, Collections.emptyList()));
    }

    @Test
    public void sesion_con_duracion_notas_o_valoracion_avisa() {
        assertTrue(AvisoDescartar.sesionConDatos("45", "", 0f, unEjercicio()));
        assertTrue(AvisoDescartar.sesionConDatos("", "Buen día", 0f, unEjercicio()));
        assertTrue(AvisoDescartar.sesionConDatos("", "", 3f, unEjercicio()));
    }

    @Test
    public void sesion_con_una_serie_tocada_avisa() {
        List<EjercicioPesoAdapter.Item> peso = unEjercicio();
        peso.get(0).realizadas.get(1).peso = "60";
        assertTrue(AvisoDescartar.sesionConDatos("", "", 0f, peso));

        List<EjercicioPesoAdapter.Item> reps = unEjercicio();
        reps.get(0).realizadas.get(0).repeticiones = "8";
        assertTrue(AvisoDescartar.sesionConDatos("", "", 0f, reps));

        List<EjercicioPesoAdapter.Item> hecha = unEjercicio();
        hecha.get(0).realizadas.get(2).completada = true;
        assertTrue(AvisoDescartar.sesionConDatos("", "", 0f, hecha));

        List<EjercicioPesoAdapter.Item> otra = unEjercicio();
        otra.get(0).realizadas.add(new EjercicioPesoAdapter.Serie(4, "10"));
        assertTrue(AvisoDescartar.sesionConDatos("", "", 0f, otra));
    }

    @Test
    public void texto_solo_espacios_no_cuenta() {
        assertFalse(AvisoDescartar.hayTexto("", "   ", null));
        assertTrue(AvisoDescartar.hayTexto("", "a"));
    }

    @Test
    public void editor_sin_cambios_no_avisa_y_con_cambios_si() {
        assertFalse(AvisoDescartar.distinto("Pierna", "Pierna"));
        assertFalse(AvisoDescartar.distinto("Pierna", "Pierna "));
        assertFalse(AvisoDescartar.distinto(null, ""));
        assertTrue(AvisoDescartar.distinto("Pierna", "Pierna y glúteo"));
        assertTrue(AvisoDescartar.distinto("60", ""));
    }
}
