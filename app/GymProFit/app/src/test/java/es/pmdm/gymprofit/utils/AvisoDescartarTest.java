package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// AvisoDescartarTest — GP-098: el aviso sale solo si hay algo que perder.
//
// Sin cambios, salir es directo; con algo apuntado, se pregunta. Lo que cuenta en Apuntar
// entreno (algo marcado, la nota o la cara) lo decide su ViewModel desde GP-241 y se prueba en
// RegistrarSesionViewModelTest (hay_datos_con_algo_marcado_la_nota_o_la_cara).
// ============================================================
public class AvisoDescartarTest {

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
