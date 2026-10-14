package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.Racion;
import es.pmdm.gymprofit.model.alimento.UltimaCantidad;
import es.pmdm.gymprofit.model.plato.IngredienteElegido;

// ============================================================
// PlatosAppTest — un plato en la app (lote 1.6.6, B1 a B5)
//   · Sin peso hecho no hay gramos en ningún sitio: ni en la cantidad que se elige, ni
//     en lo que dice una fila o una línea («1 ración», «2 raciones»).
//   · Con peso hecho, como cualquier alimento: su ración y los gramos.
//   · El «+» de un plato añade la última cantidad en raciones, o una ración.
//   · «Mis platos»: los platos con «Crear un plato» (sin ninguno, la línea que lo explica)
//     y lo creado con «Crear un alimento»; en modo ingredientes no hay platos.
//   · Los ingredientes del plato: el «+» y el ✓, la ficha y «Deshacer».
// ============================================================
public class PlatosAppTest {

    private static final double D = 0.0001;
    private static final Locale ES = new Locale("es", "ES");
    private static final Cantidades.Formatos F = formatos();

    private static Cantidades.Formatos formatos() {
        try {
            String xml = new String(Files.readAllBytes(Paths.get("src", "main", "res", "values", "strings.xml")),
                    StandardCharsets.UTF_8);
            return new Cantidades.Formatos(texto(xml, "cantidad_una_racion"), texto(xml, "cantidad_raciones"),
                    texto(xml, "cantidad_con_unidad"), texto(xml, "cantidad_gramos"),
                    texto(xml, "cantidad_sin_gramos_unidad"), texto(xml, "cantidad_sin_gramos_raciones"));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static String texto(String xml, String nombre) {
        Matcher m = Pattern.compile("name=\"" + nombre + "\">([^<]*)<").matcher(xml);
        if (!m.find()) throw new AssertionError("falta " + nombre);
        return m.group(1).replace("\\u00A0", " ");
    }

    /** «1 ración», con su unidad como la manda la API. */
    private static final class RacionPlato extends Racion {
        RacionPlato(int id, double gramos) {
            super(id, "1 ración", gramos);
        }

        @Override public String getUnidad() { return "ración"; }

        @Override public String getUnidadPlural() { return "raciones"; }
    }

    private static Alimento plato(Double peso, double gramosRacion) {
        Alimento a = new Alimento();
        a.setId(40);
        a.setNombre("Lentejas de casa");
        a.setCalorias(peso == null ? 474 : 105);
        a.setProteinas(peso == null ? 25 : 5.56);
        a.setPlatoRaciones(4);
        a.setPlatoPesoGramos(peso);
        List<Racion> r = new ArrayList<>();
        r.add(new RacionPlato(peso == null ? 7 : 8, gramosRacion));
        a.setRaciones(r);
        return a;
    }

    private static Alimento alimento(int id, String nombre) {
        Alimento a = new Alimento();
        a.setId(id);
        a.setNombre(nombre);
        a.setCalorias(100);
        return a;
    }

    private static String sinDuro(String s) {
        return s.replace(' ', ' ');
    }

    // --- Sin peso hecho, sin gramos ------------------------------------------

    @Test
    public void sin_peso_la_cantidad_es_solo_en_raciones() {
        CantidadFicha c = CantidadFicha.nueva(plato(null, 100));
        assertFalse(c.conGramos());
        assertEquals(1, c.unidades().size());
        assertFalse(c.unidad().esGramos());
        assertEquals(1, c.valor(), D);
        assertEquals(100, c.gramos(), D);
        assertEquals("1 ración", sinDuro(AnadirRapido.texto(F, ES, c)));
        c.mas();
        assertEquals("2 raciones", sinDuro(AnadirRapido.texto(F, ES, c)));
        assertEquals(948, AnadirRapido.kcal(plato(null, 100), c));
    }

    @Test
    public void con_peso_se_elige_en_raciones_o_en_gramos() {
        CantidadFicha c = CantidadFicha.nueva(plato(1800.0, 450));
        assertTrue(c.conGramos());
        assertEquals(2, c.unidades().size());
        assertEquals("1 ración (450 g)", sinDuro(AnadirRapido.texto(F, ES, c)));
        assertEquals(473, AnadirRapido.kcal(plato(1800.0, 450), c));
        c.elegir(1);
        assertEquals("450 g", sinDuro(AnadirRapido.texto(F, ES, c)));
    }

    @Test
    public void una_linea_de_un_plato_sin_peso_no_ensena_gramos() {
        assertEquals("1 ración", sinDuro(Cantidades.texto(F, ES, "1 ración", "ración", "raciones", 1.0, 100, true)));
        assertEquals("2 raciones", sinDuro(Cantidades.texto(F, ES, "1 ración", "ración", "raciones", 2.0, 200, true)));
        assertEquals("1,5 raciones", sinDuro(Cantidades.texto(F, ES, "1 ración", "ración", "raciones", 1.5, 150, true)));
        // Una API sin unidad: sin gramos igual.
        assertEquals("1 ración", sinDuro(Cantidades.texto(F, ES, "1 ración", null, null, 1.0, 100, true)));
        assertEquals("2 × ración", sinDuro(Cantidades.texto(F, ES, "1 ración", null, null, 2.0, 200, true)));
        // Con peso hecho, como cualquier alimento.
        assertEquals("1 ración (450 g)", sinDuro(Cantidades.texto(F, ES, "1 ración", "ración", "raciones", 1.0, 450, false)));
    }

    @Test
    public void el_mas_de_un_plato_repite_las_raciones_o_pone_una() {
        Alimento a = plato(null, 100);
        assertEquals(1, AnadirRapido.cantidad(a).valor(), D);
        a.setUltima(new UltimaCantidad(200, 7, "1 ración", 2.0, "ración", "raciones"));
        CantidadFicha c = AnadirRapido.cantidad(a);
        assertFalse(c.unidad().esGramos());
        assertEquals(2, c.valor(), D);
        assertEquals("2 raciones", sinDuro(AnadirRapido.texto(F, ES, c)));
        Map<String, Object> cuerpo = PedidoCantidad.anadir(a, c, "2026-10-14", "CENA");
        assertEquals(7, cuerpo.get("racionId"));
        assertNull(cuerpo.get("cantidadGramos"));
    }

    // --- Mis platos ------------------------------------------------------------

    @Test
    public void mis_platos_con_y_sin_platos() {
        List<GruposBusqueda.Elemento> vacia = GruposBusqueda.misPlatos(new ArrayList<>(), new ArrayList<>());
        assertEquals(GruposBusqueda.Tipo.CABECERA, vacia.get(0).tipo);
        assertEquals(GruposBusqueda.Tipo.SIN_PLATOS, vacia.get(1).tipo);
        assertEquals(GruposBusqueda.Tipo.CREAR_PLATO, vacia.get(2).tipo);
        assertEquals(GruposBusqueda.Seccion.CREADOS, vacia.get(3).seccion);
        assertEquals(GruposBusqueda.Tipo.CREAR_ALIMENTO, vacia.get(4).tipo);
        assertEquals(5, vacia.size());

        List<Alimento> platos = List.of(plato(null, 100));
        List<Alimento> creados = List.of(alimento(3, "Kéfir"));
        List<GruposBusqueda.Elemento> l = GruposBusqueda.misPlatos(platos, creados);
        assertEquals(GruposBusqueda.Tipo.ALIMENTO, l.get(1).tipo);
        assertEquals(GruposBusqueda.Seccion.MIS_PLATOS, l.get(1).seccion);
        assertEquals(GruposBusqueda.Tipo.CREAR_PLATO, l.get(2).tipo);
        assertEquals(GruposBusqueda.Tipo.ALIMENTO, l.get(4).tipo);
        assertEquals(GruposBusqueda.Seccion.CREADOS, l.get(4).seccion);
        assertEquals(GruposBusqueda.Tipo.CREAR_ALIMENTO, l.get(5).tipo);
        for (GruposBusqueda.Elemento e : l) assertFalse(e.tipo == GruposBusqueda.Tipo.SIN_PLATOS);
    }

    @Test
    public void en_modo_ingredientes_no_hay_platos() {
        List<Alimento> lista = List.of(alimento(1, "Patata"), plato(null, 100), alimento(2, "Cebolla"));
        List<Alimento> quedan = GruposBusqueda.sinPlatos(lista);
        assertEquals(2, quedan.size());
        for (Alimento a : quedan) assertFalse(a.esPlato());
    }

    // --- Los ingredientes ------------------------------------------------------

    @Test
    public void el_mas_y_el_check_de_un_ingrediente() {
        IngredientesPlato i = new IngredientesPlato();
        Alimento patata = alimento(1, "Patata");
        assertTrue(i.alternar(patata, CantidadFicha.nueva(patata.getRaciones())));
        assertTrue(i.tiene(patata));
        assertEquals(100, i.lista().get(0).getGramos(), D);
        assertFalse(i.alternar(patata, CantidadFicha.nueva(patata.getRaciones())));
        assertFalse(i.tiene(patata));
        assertEquals(0, i.cuantos());
    }

    @Test
    public void la_ficha_cambia_la_cantidad_en_su_sitio_y_deshacer_devuelve_a_su_sitio() {
        IngredientesPlato i = new IngredientesPlato();
        Alimento a = alimento(1, "A"), b = alimento(2, "B"), c = alimento(3, "C");
        for (Alimento x : List.of(a, b, c)) i.alternar(x, CantidadFicha.nueva(x.getRaciones()));
        CantidadFicha doble = CantidadFicha.nueva(b.getRaciones());
        doble.escribir(250);
        i.poner(IngredienteElegido.de(b, doble));
        assertEquals(3, i.cuantos());
        assertEquals(250, i.lista().get(1).getGramos(), D);
        IngredienteElegido quitado = i.quitar(1);
        assertEquals(2, i.cuantos());
        i.devolver(1, quitado);
        assertEquals("B", i.lista().get(1).getAlimento().getNombre());
        // Devolverlo dos veces no lo repite.
        i.devolver(1, quitado);
        assertEquals(3, i.cuantos());
        // Total: 100 + 250 + 100 g de 100 kcal por 100 g.
        assertEquals(450, i.total().kcal);
    }

    @Test
    public void como_mucho_cincuenta() {
        IngredientesPlato i = new IngredientesPlato();
        for (int k = 1; k <= 50; k++) i.alternar(alimento(k, "x" + k), CantidadFicha.nueva(new ArrayList<>()));
        assertTrue(i.lleno());
        assertFalse(i.alternar(alimento(51, "y"), CantidadFicha.nueva(new ArrayList<>())));
        assertEquals(50, i.cuantos());
    }

    @Test
    public void el_cuerpo_de_un_plato() {
        Alimento pan = alimento(5, "Pan");
        List<Racion> rebanada = new ArrayList<>();
        rebanada.add(new Racion(9, "1 rebanada", 28));
        pan.setRaciones(rebanada);
        CantidadFicha dos = CantidadFicha.nueva(pan.getRaciones());
        dos.mas();
        Alimento sinId = alimento(0, "Producto");
        sinId.setBarcode("8410000000000");
        List<IngredienteElegido> lista = List.of(IngredienteElegido.de(pan, dos),
                IngredienteElegido.de(sinId, CantidadFicha.nueva(sinId.getRaciones())));
        Map<String, Object> b = PedidoCantidad.plato(" Tostadas ", 4, 1800.0, lista);
        assertEquals("Tostadas", b.get("nombre"));
        assertEquals(4, b.get("raciones"));
        assertEquals(0, new java.math.BigDecimal("1800").compareTo((java.math.BigDecimal) b.get("pesoGramos")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ing = (List<Map<String, Object>>) b.get("ingredientes");
        assertEquals(5, ing.get(0).get("alimentoId"));
        assertEquals(9, ing.get(0).get("racionId"));
        assertEquals(0, new java.math.BigDecimal("2").compareTo((java.math.BigDecimal) ing.get(0).get("raciones")));
        assertEquals("8410000000000", ing.get(1).get("barcode"));
        assertEquals(0, new java.math.BigDecimal("100").compareTo((java.math.BigDecimal) ing.get(1).get("cantidadGramos")));
        assertNull(PedidoCantidad.plato("x", 1, null, lista).get("pesoGramos"));
    }
}
