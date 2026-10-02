package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.AnadirRespuesta;
import es.pmdm.gymprofit.model.comida.CantidadAnterior;

// ============================================================
// AnadidosTest — el «+» y el ✓ de Añadir (lote 1.6.3, B2)
// La fila cambia al tocar y la API va detrás, de una en una por alimento: un segundo
// toque mientras el primero viaja espera su turno, nunca dos líneas ni un borrado
// perdido. Un servidor de mentira guarda las peticiones y las responde cuando el test
// quiere, en el orden que quiere.
// ============================================================
public class AnadidosTest {

    /** Las peticiones que llegan, sin responder hasta que el test lo diga. */
    private static final class Servidor implements Anadidos.Servidor {
        final List<Anadidos.Respuesta<AnadirRespuesta>> anadir = new ArrayList<>();
        final List<Anadidos.Anadido> deshacerQue = new ArrayList<>();
        final List<Anadidos.Respuesta<Void>> deshacer = new ArrayList<>();
        int lineas = 100;

        @Override
        public void anadir(@NonNull Anadidos.Pedido pedido, @NonNull Anadidos.Respuesta<AnadirRespuesta> r) {
            anadir.add(r);
        }

        @Override
        public void deshacer(@NonNull Anadidos.Anadido a, @NonNull Anadidos.Respuesta<Void> r) {
            deshacerQue.add(a);
            deshacer.add(r);
        }

        void anadidoOk(int alimentoId, @Nullable CantidadAnterior anterior) {
            AlimentoComida linea = new AlimentoComida();
            linea.setId(++lineas);
            linea.setAlimentoId(alimentoId);
            anadir.remove(0).ok(new AnadirRespuesta(null, linea, anterior));
        }

        void deshecho() {
            deshacer.remove(0).ok(null);
        }
    }

    private Servidor servidor;
    private Anadidos anadidos;
    private final List<String> fallos = new ArrayList<>();

    @Before
    public void antes() {
        servidor = new Servidor();
        anadidos = new Anadidos(servidor, new Anadidos.Oyente() {
            @Override public void cambio(@NonNull String clave) { }

            @Override
            public void fallo(@NonNull String clave, boolean alAnadir, int code, @Nullable String message) {
                fallos.add((alAnadir ? "anadir " : "quitar ") + clave);
            }
        });
    }

    private static Anadidos.Pedido pedido(int id, String tipo, long kcal) {
        return new Anadidos.Pedido(id, null, tipo, new HashMap<>(), "1 envase (200 g)", kcal);
    }

    @Test
    public void el_mas_se_vuelve_check_al_tocar_sin_esperar_a_la_api() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        assertTrue(anadidos.marcado("a:7"));
        assertEquals(1, servidor.anadir.size());
        assertTrue(anadidos.ocupado());
        servidor.anadidoOk(7, null);
        assertFalse(anadidos.ocupado());
        assertEquals(101, anadidos.hecho("a:7").getLinea().getId());
        assertTrue(anadidos.huboCambios());
    }

    @Test
    public void tocar_otra_vez_mientras_viaja_espera_su_turno_y_lo_quita() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        // Se ve ya en «+», pero nada sale a la API hasta que vuelva la primera.
        assertFalse(anadidos.marcado("a:7"));
        assertTrue(servidor.deshacer.isEmpty());
        servidor.anadidoOk(7, null);
        // Ahora sí: se deshace esa línea, y solo esa.
        assertEquals(1, servidor.deshacer.size());
        assertEquals(101, servidor.deshacerQue.get(0).getLinea().getId());
        servidor.deshecho();
        assertNull(anadidos.hecho("a:7"));
        assertFalse(anadidos.ocupado());
    }

    @Test
    public void tres_toques_seguidos_dejan_una_sola_linea() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        assertTrue(anadidos.marcado("a:7"));
        servidor.anadidoOk(7, null);
        // Quería ✓ y ya lo es: ni otra línea ni un borrado.
        assertTrue(servidor.anadir.isEmpty());
        assertTrue(servidor.deshacer.isEmpty());
        assertEquals(101, anadidos.hecho("a:7").getLinea().getId());
    }

    @Test
    public void cuatro_toques_quitan_y_vuelven_a_anadir_en_orden() {
        for (int i = 0; i < 4; i++) anadidos.tocar(pedido(7, "MERIENDA", 120));
        assertFalse(anadidos.marcado("a:7"));
        servidor.anadidoOk(7, null);
        assertEquals(1, servidor.deshacer.size());
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        servidor.deshecho();
        // Tras borrar, el quinto toque pide añadir otra vez, una sola.
        assertEquals(1, servidor.anadir.size());
        servidor.anadidoOk(7, null);
        assertEquals(102, anadidos.hecho("a:7").getLinea().getId());
    }

    @Test
    public void si_anadir_falla_vuelve_a_mas_y_se_dice() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        servidor.anadir.remove(0).fallo(500, "x");
        assertFalse(anadidos.marcado("a:7"));
        assertEquals(List.of("anadir a:7"), fallos);
        assertFalse(anadidos.ocupado());
    }

    @Test
    public void si_quitar_falla_vuelve_a_check_y_se_dice() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        servidor.anadidoOk(7, null);
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        servidor.deshacer.remove(0).fallo(0, null);
        assertTrue(anadidos.marcado("a:7"));
        assertEquals(List.of("quitar a:7"), fallos);
        assertEquals(101, anadidos.hecho("a:7").getLinea().getId());
    }

    @Test
    public void quitar_lo_sumado_lleva_su_cantidad_de_antes() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        CantidadAnterior antes = new CantidadAnterior(200, 9, 1.0);
        servidor.anadidoOk(7, antes);
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        assertEquals(200, servidor.deshacerQue.get(0).getAnterior().getCantidadGramos(), 0);
        assertEquals(Integer.valueOf(9), servidor.deshacerQue.get(0).getAnterior().getRacionId());
    }

    @Test
    public void lo_anadido_desde_la_ficha_cuenta_y_su_check_lo_quita() {
        AlimentoComida linea = new AlimentoComida();
        linea.setId(55);
        linea.setAlimentoId(8);
        anadidos.marcar(new Anadidos.Anadido(8, "8400000000017", "CENA", linea, null, "150 g", 165));
        assertTrue(anadidos.marcado("a:8"));
        // La fila de un producto sin materializar lo encuentra por su código.
        assertTrue(anadidos.marcado("c:8400000000017"));
        assertEquals(1, anadidos.resumen().cuantos);
        anadidos.tocar(new Anadidos.Pedido(0, "8400000000017", "CENA", new HashMap<>(), "150 g", 165));
        assertEquals(55, servidor.deshacerQue.get(0).getLinea().getId());
    }

    @Test
    public void la_barra_suma_lo_marcado_y_dice_sus_comidas() {
        anadidos.tocar(pedido(1, "MERIENDA", 120));
        anadidos.tocar(pedido(2, "MERIENDA", 140));
        Anadidos.Resumen r = anadidos.resumen();
        assertEquals(2, r.cuantos);
        assertEquals(260, r.kcal);
        assertEquals(1, r.comidas.size());
        anadidos.tocar(pedido(3, "CENA", 63));
        assertEquals(2, anadidos.resumen().comidas.size());
        // Quitado, ya no cuenta.
        anadidos.tocar(pedido(1, "MERIENDA", 120));
        assertEquals(2, anadidos.resumen().cuantos);
        assertEquals(203, anadidos.resumen().kcal);
    }

    @Test
    public void salir_espera_a_que_todo_llegue() {
        List<String> hecho = new ArrayList<>();
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        anadidos.alTerminar(() -> hecho.add("fuera"));
        assertTrue(hecho.isEmpty());
        servidor.anadidoOk(7, null);
        assertEquals(List.of("fuera"), hecho);
        // Sin nada en vuelo, al momento.
        anadidos.alTerminar(() -> hecho.add("otra"));
        assertEquals(2, hecho.size());
    }
}
