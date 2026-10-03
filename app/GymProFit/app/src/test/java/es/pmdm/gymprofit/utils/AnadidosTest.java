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
                fallos.add((alAnadir ? "anadir " : "quitar ") + Anadidos.parteAlimento(clave));
            }
        });
    }

    // La fila de ese alimento en la merienda.
    private static final String M1 = Anadidos.clave("MERIENDA", 1, null);
    private static final String M7 = Anadidos.clave("MERIENDA", 7, null);

    private static Anadidos.Pedido pedido(int id, String tipo, long kcal) {
        return new Anadidos.Pedido(id, null, tipo, new HashMap<>(), "1 envase (200 g)", kcal);
    }

    @Test
    public void el_mas_se_vuelve_check_al_tocar_sin_esperar_a_la_api() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        assertTrue(anadidos.marcado(M7));
        assertEquals(1, servidor.anadir.size());
        assertTrue(anadidos.ocupado());
        servidor.anadidoOk(7, null);
        assertFalse(anadidos.ocupado());
        assertEquals(101, anadidos.hecho(M7).getLinea().getId());
        assertTrue(anadidos.huboCambios());
    }

    @Test
    public void tocar_otra_vez_mientras_viaja_espera_su_turno_y_lo_quita() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        // Se ve ya en «+», pero nada sale a la API hasta que vuelva la primera.
        assertFalse(anadidos.marcado(M7));
        assertTrue(servidor.deshacer.isEmpty());
        servidor.anadidoOk(7, null);
        // Ahora sí: se deshace esa línea, y solo esa.
        assertEquals(1, servidor.deshacer.size());
        assertEquals(101, servidor.deshacerQue.get(0).getLinea().getId());
        servidor.deshecho();
        assertNull(anadidos.hecho(M7));
        assertFalse(anadidos.ocupado());
    }

    @Test
    public void tres_toques_seguidos_dejan_una_sola_linea() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        assertTrue(anadidos.marcado(M7));
        servidor.anadidoOk(7, null);
        // Quería ✓ y ya lo es: ni otra línea ni un borrado.
        assertTrue(servidor.anadir.isEmpty());
        assertTrue(servidor.deshacer.isEmpty());
        assertEquals(101, anadidos.hecho(M7).getLinea().getId());
    }

    @Test
    public void cuatro_toques_quitan_y_vuelven_a_anadir_en_orden() {
        for (int i = 0; i < 4; i++) anadidos.tocar(pedido(7, "MERIENDA", 120));
        assertFalse(anadidos.marcado(M7));
        servidor.anadidoOk(7, null);
        assertEquals(1, servidor.deshacer.size());
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        servidor.deshecho();
        // Tras borrar, el quinto toque pide añadir otra vez, una sola.
        assertEquals(1, servidor.anadir.size());
        servidor.anadidoOk(7, null);
        assertEquals(102, anadidos.hecho(M7).getLinea().getId());
    }

    @Test
    public void si_anadir_falla_vuelve_a_mas_y_se_dice() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        servidor.anadir.remove(0).fallo(500, "x");
        assertFalse(anadidos.marcado(M7));
        assertEquals(List.of("anadir a:7"), fallos);
        assertFalse(anadidos.ocupado());
    }

    @Test
    public void si_quitar_falla_vuelve_a_check_y_se_dice() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        servidor.anadidoOk(7, null);
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        servidor.deshacer.remove(0).fallo(0, null);
        assertTrue(anadidos.marcado(M7));
        assertEquals(List.of("quitar a:7"), fallos);
        assertEquals(101, anadidos.hecho(M7).getLinea().getId());
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
        assertTrue(anadidos.marcado(Anadidos.clave("CENA", 8, null)));
        // La fila de un producto sin materializar lo encuentra por su código.
        assertTrue(anadidos.marcado(Anadidos.clave("CENA", 0, "8400000000017")));
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

    // ── Lote 1.6.4 ──────────────────────────────────────────────────────────

    @Test
    public void gp185_lo_anadido_a_la_merienda_no_marca_la_cena() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        servidor.anadidoOk(7, null);
        String cena = Anadidos.clave("CENA", 7, null);
        // Con la etiqueta en la cena, la fila vuelve a «+»…
        assertFalse(anadidos.marcado(cena));
        assertNull(anadidos.texto(cena));
        // …y se puede añadir también a la cena: otra petición, otra línea.
        anadidos.tocar(pedido(7, "CENA", 120));
        assertEquals(1, servidor.anadir.size());
        servidor.anadidoOk(7, null);
        assertTrue(anadidos.marcado(cena));
        assertTrue(anadidos.marcado(M7));
        // Las dos cuentan en la barra, en dos comidas.
        assertEquals(2, anadidos.resumen().cuantos);
        assertEquals(2, anadidos.resumen().comidas.size());
        // Quitar la de la cena no toca la de la merienda.
        anadidos.tocar(pedido(7, "CENA", 120));
        assertEquals(102, servidor.deshacerQue.get(0).getLinea().getId());
    }

    @Test
    public void gp183_la_ficha_de_una_fila_en_viaje_se_abre_al_llegar_la_linea() {
        List<Integer> abiertas = new ArrayList<>();
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        anadidos.cuandoEste(M7, a -> abiertas.add(a.getLinea().getId()));
        assertTrue(abiertas.isEmpty());
        servidor.anadidoOk(7, null);
        assertEquals(List.of(101), abiertas);
        // Con la línea ya hecha, al momento.
        anadidos.cuandoEste(M7, a -> abiertas.add(-a.getLinea().getId()));
        assertEquals(List.of(101, -101), abiertas);
    }

    @Test
    public void gp183_si_anadir_falla_no_se_abre_nada() {
        List<Integer> abiertas = new ArrayList<>();
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        anadidos.cuandoEste(M7, a -> abiertas.add(a.getLinea().getId()));
        servidor.anadir.remove(0).fallo(500, "x");
        assertTrue(abiertas.isEmpty());
        assertEquals(List.of("anadir a:7"), fallos);
        // Ni después, si se vuelve a añadir.
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        servidor.anadidoOk(7, null);
        assertTrue(abiertas.isEmpty());
    }

    @Test
    public void gp183_si_se_quita_mientras_viaja_no_se_abre() {
        List<Integer> abiertas = new ArrayList<>();
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        anadidos.cuandoEste(M7, a -> abiertas.add(a.getLinea().getId()));
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        servidor.anadidoOk(7, null);
        assertTrue(abiertas.isEmpty());
    }

    @Test
    public void lo_copiado_sobre_un_check_suma_y_quitarlo_deja_la_linea_como_antes_de_los_dos() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        CantidadAnterior original = new CantidadAnterior(50, null, null);
        servidor.anadidoOk(7, original);
        // Una copia trae el mismo alimento a la misma comida: se suma a esa línea.
        AlimentoComida sumada = new AlimentoComida();
        sumada.setId(101);
        sumada.setAlimentoId(7);
        anadidos.marcar(new Anadidos.Anadido(7, null, "MERIENDA", sumada, new CantidadAnterior(250, null, null),
                "60 g", 63));
        assertEquals(1, anadidos.resumen().cuantos);
        assertEquals(183, anadidos.resumen().kcal);
        // El ✓ lo quita todo: vuelve a lo que había antes del «+».
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        assertEquals(50, servidor.deshacerQue.get(0).getAnterior().getCantidadGramos(), 0);
    }

    @Test
    public void salir_espera_tambien_a_una_copia_en_vuelo() {
        List<String> hecho = new ArrayList<>();
        anadidos.empezarAparte();
        assertTrue(anadidos.ocupado());
        anadidos.alTerminar(() -> hecho.add("fuera"));
        assertTrue(hecho.isEmpty());
        anadidos.terminarAparte();
        assertEquals(List.of("fuera"), hecho);
        assertFalse(anadidos.ocupado());
    }

    @Test
    public void una_copia_que_llega_mientras_su_mas_viaja_se_suma_al_llegar_y_el_check_quita_las_dos() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        // En la API, la copia llegó antes: creó la línea 101 y el «+» se le sumó.
        AlimentoComida copiada = new AlimentoComida();
        copiada.setId(101);
        copiada.setAlimentoId(7);
        anadidos.marcar(new Anadidos.Anadido(7, null, "MERIENDA", copiada, null, "60 g", 63));
        AlimentoComida sumada = new AlimentoComida();
        sumada.setId(101);
        sumada.setAlimentoId(7);
        servidor.anadir.remove(0).ok(new AnadirRespuesta(null, sumada, new CantidadAnterior(60, null, null)));
        assertEquals(1, anadidos.resumen().cuantos);
        assertEquals(183, anadidos.resumen().kcal);
        // Lo más antiguo es la línea nueva de la copia: el ✓ la borra entera.
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        assertNull(servidor.deshacerQue.get(0).getAnterior());
    }

    @Test
    public void una_copia_que_llega_mientras_su_mas_falla_cuenta_igual() {
        anadidos.tocar(pedido(7, "MERIENDA", 120));
        AlimentoComida copiada = new AlimentoComida();
        copiada.setId(101);
        copiada.setAlimentoId(7);
        anadidos.marcar(new Anadidos.Anadido(7, null, "MERIENDA", copiada, null, "60 g", 63));
        servidor.anadir.remove(0).fallo(500, "x");
        assertTrue(anadidos.marcado(M7));
        assertEquals(63, anadidos.resumen().kcal);
    }
}
