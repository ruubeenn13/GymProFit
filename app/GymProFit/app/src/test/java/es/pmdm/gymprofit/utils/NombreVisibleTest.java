package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// NombreVisibleTest — GP-116: qué nombre se enseña.
//
// Inicio («Hola, …»), el título del perfil en Progreso y las iniciales del avatar usan
// el nombre para mostrar; sin nombre, el de usuario, como hasta ahora.
// ============================================================
public class NombreVisibleTest {

    @Test
    public void con_nombre_se_usa_el_nombre() {
        assertEquals("Ana María", NombreVisible.de("Ana María", "ana92"));
    }

    @Test
    public void sin_nombre_se_usa_el_de_usuario() {
        assertEquals("ana92", NombreVisible.de(null, "ana92"));
        assertEquals("ana92", NombreVisible.de("", "ana92"));
        assertEquals("un nombre en blanco es no tener nombre", "ana92", NombreVisible.de("   ", "ana92"));
    }

    @Test
    public void el_nombre_sale_recortado() {
        assertEquals("Ana", NombreVisible.de("  Ana ", "ana92"));
    }

    @Test
    public void sin_nada_queda_vacio() {
        assertEquals("", NombreVisible.de(null, null));
    }

    @Test
    public void lo_que_se_manda_va_recortado_y_en_blanco_para_borrar() {
        assertEquals("Ana", NombreVisible.paraEnviar("  Ana  "));
        // La API ignora los null: para borrar hay que mandarlo en blanco.
        assertEquals("", NombreVisible.paraEnviar("   "));
        assertEquals("", NombreVisible.paraEnviar(null));
    }
}
