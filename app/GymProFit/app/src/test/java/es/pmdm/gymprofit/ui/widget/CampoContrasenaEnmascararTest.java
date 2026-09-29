package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

// ============================================================
// CampoContrasenaEnmascararTest — GP-131: qué texto queda en el nodo de una contraseña
// oculta. El montaje con el delegado real de Material está en CampoContrasenaTest
// (androidTest); aquí, los casos del texto en sí.
// ============================================================
public class CampoContrasenaEnmascararTest {

    @Test
    public void la_contrasena_se_cambia_por_un_punto_por_caracter() {
        assertEquals("••••", CampoContrasena.enmascarar("abc1", "abc1").toString());
    }

    @Test
    public void lo_que_material_anade_detras_se_conserva() {
        // Por debajo de Android 8, Material escribe «texto, pista».
        assertEquals("•••, Contraseña",
                CampoContrasena.enmascarar("abc, Contraseña", "abc").toString());
    }

    @Test
    public void vacio_o_con_la_pista_no_se_toca() {
        assertEquals("Contraseña", CampoContrasena.enmascarar("Contraseña", "").toString());
        assertNull(CampoContrasena.enmascarar(null, "abc"));
        // El nodo no empieza por lo escrito: no hay nada que enmascarar.
        assertEquals("Contraseña", CampoContrasena.enmascarar("Contraseña", "xyz").toString());
    }
}
