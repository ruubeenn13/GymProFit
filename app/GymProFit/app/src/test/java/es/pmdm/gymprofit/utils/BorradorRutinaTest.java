package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.pmdm.gymprofit.model.rutina.BorradorRutina;

// ============================================================
// BorradorRutinaTest — crear y editar una rutina (GP-244, puntos 6 a 9)
//
// Lo que la pantalla de «Tu rutina» hace con la lista y lo que dice: el nombre propuesto
// por las zonas (las elegidas o, buscando entre todos, las de los ejercicios), la frase del
// tiempo (cabe uno más, justo y te pasas), subir, bajar y quitar para ordenar sin arrastrar,
// y que sin ejercicios no se guarda. Con las cadenas de verdad, en español y en inglés.
// ============================================================
public class BorradorRutinaTest {

    private static final Cadenas ES = Cadenas.es();
    private static final Cadenas EN = Cadenas.en();

    private static BorradorRutina.Textos textos(Cadenas c) {
        return new BorradorRutina.Textos(c.s("enumeracion_y"), c.s("tarjeta_y_serie"), c.idioma(),
                c.s("crear_nombre_por_defecto"),
                (m, e) -> c.s("crear_tiempo_cabe", m, e),
                e -> c.s("crear_tiempo_justo", e),
                (d, e) -> c.s("crear_tiempo_te_pasas", d, e),
                m -> c.s("crear_tiempo_75", m),
                z -> c.s(BorradorRutinaTest.clave(z)));
    }

    // El nombre en frase de cada zona, como en strings.xml (zona_*).
    static String clave(String grupo) {
        switch (grupo) {
            case "PECHO": return "zona_pecho";
            case "ESPALDA": return "zona_espalda";
            case "PIERNAS": return "zona_pierna";
            case "HOMBROS": return "zona_hombros";
            case "BRAZOS": return "zona_brazos";
            default: return "zona_core";
        }
    }

    private static BorradorRutina con(String... grupos) {
        BorradorRutina b = new BorradorRutina();
        int id = 1;
        for (String g : grupos) b.anadir(id++, "Ejercicio " + id, g, null);
        return b;
    }

    // ── El nombre propuesto ─────────────────────────────────────────────

    @Test
    public void el_nombre_sale_de_las_zonas_elegidas_en_su_orden() {
        BorradorRutina b = new BorradorRutina();
        b.zonas = new ArrayList<>(Arrays.asList("HOMBROS", "PECHO"));
        assertEquals("Pecho y hombros", b.nombrePropuesto(textos(ES)));
        assertEquals("Chest and shoulders", b.nombrePropuesto(textos(EN)));
        b.zonas = new ArrayList<>(Arrays.asList("ABDOMEN", "PIERNAS", "ESPALDA"));
        assertEquals("Espalda, pierna y abdomen", b.nombrePropuesto(textos(ES)));
        assertEquals("Back, legs, and core", b.nombrePropuesto(textos(EN)));
    }

    @Test
    public void buscando_entre_todos_sale_de_las_zonas_de_los_ejercicios() {
        // Sin zonas elegidas: dos de brazos, uno de pecho y un cardio, que no es zona.
        BorradorRutina b = con("PECHO", "BRAZOS", "BRAZOS", "CARDIO");
        assertEquals("Brazos y pecho", b.nombrePropuesto(textos(ES)));
        assertEquals("Arms and chest", b.nombrePropuesto(textos(EN)));
        // Sin ninguna zona, el de por defecto.
        assertEquals("Mi rutina", con("CARDIO", "FULLBODY").nombrePropuesto(textos(ES)));
        assertEquals("My routine", new BorradorRutina().nombrePropuesto(textos(EN)));
    }

    // ── La frase del tiempo ─────────────────────────────────────────────

    @Test
    public void la_frase_del_tiempo_cuando_cabe_uno_mas() {
        // Cuatro por defecto son 35 minutos; con uno más, unos 40: caben en 45.
        BorradorRutina b = con("PECHO", "PECHO", "HOMBROS", "HOMBROS");
        b.minutos = 45;
        assertEquals(35, b.minutosEstimados());
        assertEquals("35 de tus 45 minutos: te cabe un ejercicio más.", b.fraseTiempo(textos(ES)));
        assertEquals("35 of your 45 minutes: you have room for one more exercise.", b.fraseTiempo(textos(EN)));
    }

    @Test
    public void la_frase_del_tiempo_cuando_va_justo() {
        // Cinco por defecto, unos 40: con uno más serían 50, que ya no caben.
        BorradorRutina b = con("PECHO", "PECHO", "HOMBROS", "HOMBROS", "BRAZOS");
        b.minutos = 45;
        assertEquals(40, b.minutosEstimados());
        assertEquals("Justo en tus 45 minutos.", b.fraseTiempo(textos(ES)));
        assertEquals("Right on your 45 minutes.", b.fraseTiempo(textos(EN)));
    }

    @Test
    public void la_frase_del_tiempo_cuando_te_pasas() {
        BorradorRutina b = con("PECHO", "PECHO", "HOMBROS", "HOMBROS", "BRAZOS", "BRAZOS", "ESPALDA");
        b.minutos = 45;
        // Siete por defecto, unos 55: el ejemplo del README.
        assertEquals(55, b.minutosEstimados());
        assertEquals("Te pasas unos 10 minutos de tus 45.", b.fraseTiempo(textos(ES)));
        assertEquals("About 10 minutes over your 45.", b.fraseTiempo(textos(EN)));
        // Con 75 o más no hay techo: diez por defecto son unos 80.
        BorradorRutina diez = con("PECHO", "PECHO", "PECHO", "PECHO", "PECHO", "BRAZOS", "BRAZOS", "BRAZOS",
                "BRAZOS", "BRAZOS");
        diez.minutos = 75;
        assertEquals("Unos 80 minutos, en tus 75 o más.", diez.fraseTiempo(textos(ES)));
        assertEquals("About 80 minutes, within your 75 or more.", diez.fraseTiempo(textos(EN)));
        // Al editar no hay tiempo elegido: sin frase.
        b.minutos = null;
        assertEquals(null, b.fraseTiempo(textos(ES)));
    }

    // ── Ordenar sin arrastrar ───────────────────────────────────────────

    private static List<Integer> ids(BorradorRutina b) {
        List<Integer> l = new ArrayList<>();
        for (BorradorRutina.Ejercicio e : b.ejercicios) l.add(e.ejercicioId);
        return l;
    }

    @Test
    public void subir_bajar_y_quitar() {
        BorradorRutina b = con("PECHO", "ESPALDA", "PIERNAS");
        assertTrue(b.subir(2));
        assertEquals(Arrays.asList(1, 3, 2), ids(b));
        assertFalse("el primero no sube", b.subir(0));
        assertTrue(b.bajar(0));
        assertEquals(Arrays.asList(3, 1, 2), ids(b));
        assertFalse("el último no baja", b.bajar(2));
        b.quitar(1);
        assertEquals(Arrays.asList(3, 2), ids(b));
        assertFalse("fuera de la lista no hace nada", b.subir(5));
    }

    @Test
    public void cada_ejercicio_entra_con_3_series_de_8_a_12_y_90_segundos() {
        BorradorRutina b = con("PECHO");
        BorradorRutina.Ejercicio e = b.ejercicios.get(0);
        assertEquals(3, e.series);
        assertEquals(8, e.min);
        assertEquals(12, e.max);
        assertEquals(90, e.descanso);
        assertFalse(e.segundos);
        // El mismo ejercicio no entra dos veces.
        assertFalse(b.anadir(1, "Otra vez", "PECHO", null));
        assertEquals(1, b.ejercicios.size());
    }

    // ── Guardar ─────────────────────────────────────────────────────────

    @Test
    public void sin_ejercicios_no_se_guarda() {
        BorradorRutina b = new BorradorRutina();
        b.nombre = "Pecho y hombros";
        assertFalse(b.sePuedeGuardar());
        b.anadir(7, "Press de banca", "PECHO", "Barra");
        assertTrue(b.sePuedeGuardar());
        b.quitar(0);
        assertFalse(b.sePuedeGuardar());
        // Sin nombre tampoco.
        b.anadir(7, "Press de banca", "PECHO", "Barra");
        b.nombre = "  ";
        assertFalse(b.sePuedeGuardar());
    }

    @Test
    public void el_pie_lo_dice() {
        BorradorRutina b = new BorradorRutina();
        assertEquals(ES.s("crear_pie_vacio"), "Añade al menos un ejercicio.");
        assertEquals(0, b.ejercicios.size());
        b = con("PECHO", "PECHO", "HOMBROS", "HOMBROS");
        assertEquals("4 ejercicios y unos 35 minutos.", ES.p("crear_pie", 4, 4, b.minutosEstimados()));
        assertEquals("4 exercises and about 35 minutes.", EN.p("crear_pie", 4, 4, b.minutosEstimados()));
        assertEquals(12, b.seriesTotales());
    }
}
