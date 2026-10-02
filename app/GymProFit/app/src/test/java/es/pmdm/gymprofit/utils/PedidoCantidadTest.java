package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.Racion;

// ============================================================
// PedidoCantidadTest — lo que viaja al añadir y al actualizar (lote 1.6.1)
// Añadir: el alimento por su id o, si aún no está en el catálogo, por su código; la
// cantidad en gramos o en raciones (por id, o por posición si la ración no tiene id).
// Actualizar: la ración y cuántas (la API pone los gramos), o solo los gramos.
// ============================================================
public class PedidoCantidadTest {

    private static Alimento yogur(int id, Integer racionId) {
        Alimento a = new Alimento();
        a.setId(id);
        a.setBarcode("8410076472915");
        a.setRaciones(Arrays.asList(new Racion(racionId, "1 envase", 200), new Racion(null, "1 ración", 125)));
        return a;
    }

    @Test
    public void anadir_del_catalogo_por_racion() {
        Alimento a = yogur(7, 11);
        Map<String, Object> b = PedidoCantidad.anadir(a, CantidadFicha.nueva(a.getRaciones()), "2026-10-02", "MERIENDA");
        assertEquals("2026-10-02", b.get("fecha"));
        assertEquals("MERIENDA", b.get("tipoComida"));
        assertEquals(7, b.get("alimentoId"));
        assertFalse(b.containsKey("barcode"));
        assertEquals(11, b.get("racionId"));
        assertEquals(0, new BigDecimal("1").compareTo((BigDecimal) b.get("raciones")));
        assertFalse(b.containsKey("cantidadGramos"));
    }

    @Test
    public void anadir_un_producto_sin_materializar_va_por_codigo_y_posicion() {
        Alimento a = yogur(0, null);
        CantidadFicha c = CantidadFicha.nueva(a.getRaciones());
        c.elegir(1);
        Map<String, Object> b = PedidoCantidad.anadir(a, c, "2026-10-02", "MERIENDA");
        assertFalse(b.containsKey("alimentoId"));
        assertEquals("8410076472915", b.get("barcode"));
        assertFalse(b.containsKey("racionId"));
        assertEquals(1, b.get("racionIndice"));
    }

    @Test
    public void anadir_en_gramos() {
        Alimento a = yogur(7, 11);
        a.setRaciones(Collections.emptyList());
        Map<String, Object> b = PedidoCantidad.anadir(a, CantidadFicha.nueva(a.getRaciones()), "2026-10-02", "CENA");
        assertEquals(0, new BigDecimal("100").compareTo((BigDecimal) b.get("cantidadGramos")));
        assertFalse(b.containsKey("racionId"));
        assertFalse(b.containsKey("raciones"));
    }

    @Test
    public void actualizar_por_racion_deja_los_gramos_a_la_api() {
        Alimento a = yogur(7, 11);
        CantidadFicha c = CantidadFicha.nueva(a.getRaciones());
        c.mas();
        Map<String, Object> b = PedidoCantidad.actualizar(c);
        assertEquals(11, b.get("racionId"));
        assertEquals(0, new BigDecimal("2").compareTo((BigDecimal) b.get("raciones")));
        assertFalse(b.containsKey("cantidadGramos"));
    }

    @Test
    public void actualizar_en_gramos_quita_la_racion() {
        Alimento a = yogur(7, 11);
        CantidadFicha c = CantidadFicha.nueva(a.getRaciones());
        c.elegir(2);
        Map<String, Object> b = PedidoCantidad.actualizar(c);
        assertEquals(0, new BigDecimal("200").compareTo((BigDecimal) b.get("cantidadGramos")));
        assertFalse(b.containsKey("racionId"));
    }

    @Test
    public void actualizar_una_racion_sin_id_va_en_gramos() {
        // Una línea de un alimento ya materializado siempre trae ids; si no, gramos.
        Alimento a = yogur(7, null);
        Map<String, Object> b = PedidoCantidad.actualizar(CantidadFicha.nueva(a.getRaciones()));
        assertEquals(0, new BigDecimal("200").compareTo((BigDecimal) b.get("cantidadGramos")));
    }
}
