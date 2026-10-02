package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.pmdm.gymprofit.R;

// ============================================================
// CategoriasTest — la categoría de un alimento, su nombre y su icono (GP-175, lote 1.6.2)
// La API manda la clave canónica en español («Carnes y aves»); la app la traduce y le
// pone el icono de la tabla del diseño (documentacion/diseno/2026-10-02-nutricion). Una
// clave desconocida lleva el icono general; sin categoría, «Otro».
// ============================================================
public class CategoriasTest {

    private static final Path README = Paths.get("..", "..", "..", "documentacion", "diseno",
            "2026-10-02-nutricion", "README.md");
    private static final Path API = Paths.get("..", "..", "..", "api", "gymprofit-api", "src", "main", "java",
            "com", "gymprofit", "api", "controller", "AlimentoController.java");

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    /** La tabla «| Categoría | Icono |» del README: clave → símbolo. */
    private static Map<String, String> tablaDelDiseno() throws IOException {
        String md = leer(README);
        String tabla = md.substring(md.indexOf("| Categoría | Icono |"));
        Map<String, String> m = new LinkedHashMap<>();
        Matcher f = Pattern.compile("\\| ([^|`]+?) \\| `([a-z_]+)` \\|").matcher(tabla);
        while (f.find()) m.put(f.group(1).trim(), f.group(2));
        return m;
    }

    private static int drawable(String simbolo) throws ReflectiveOperationException {
        return R.drawable.class.getField("ic_ms_" + simbolo).getInt(null);
    }

    @Test
    public void las_claves_son_las_de_la_api() throws IOException {
        String java = leer(API);
        String lista = java.substring(java.indexOf("CATEGORIAS = Arrays.asList("));
        lista = lista.substring(0, lista.indexOf(");"));
        Matcher m = Pattern.compile("\"([^\"]+)\"").matcher(lista);
        int i = 0;
        while (m.find()) assertEquals(m.group(1), Categorias.CLAVES.get(i++));
        assertEquals(14, i);
        assertEquals(14, Categorias.CLAVES.size());
    }

    @Test
    public void el_icono_de_cada_una_es_el_del_diseno() throws Exception {
        Map<String, String> tabla = tablaDelDiseno();
        for (String clave : Categorias.CLAVES) {
            String fila = clave.equals(Categorias.OTRO) ? "Otro, o sin categoría" : clave;
            assertTrue("el diseño no dice el icono de " + clave, tabla.containsKey(fila));
            assertEquals(clave, drawable(tabla.get(fila)), Categorias.icono(clave));
        }
    }

    @Test
    public void desconocida_o_sin_categoria_el_icono_general() throws Exception {
        assertEquals(drawable("restaurant"), Categorias.icono("Comida rara"));
        assertEquals(drawable("restaurant"), Categorias.icono(null));
        assertEquals(drawable("restaurant"), Categorias.icono("  "));
    }

    @Test
    public void cada_una_tiene_su_nombre_en_es_y_en() throws IOException {
        String es = leer(Paths.get("src", "main", "res", "values", "strings.xml"));
        String en = leer(Paths.get("src", "main", "res", "values-en", "strings.xml"));
        Set<Integer> vistos = new HashSet<>();
        for (String clave : Categorias.CLAVES) {
            int id = Categorias.nombre(clave);
            assertTrue("nombre repetido en " + clave, vistos.add(id));
            String recurso = recurso(id);
            assertTrue(recurso + " en ES", es.contains("name=\"" + recurso + "\""));
            assertTrue(recurso + " en EN", en.contains("name=\"" + recurso + "\""));
        }
        // El nombre en español es la clave misma: lo que ve el usuario no cambia.
        Matcher m = Pattern.compile("name=\"categoria_carnes\">([^<]+)<").matcher(es);
        assertTrue(m.find());
        assertEquals("Carnes y aves", m.group(1));
        assertFalse(en.contains("name=\"categoria_carnes\">Carnes y aves<"));
    }

    @Test
    public void sin_categoria_es_otro() {
        assertEquals(Categorias.nombre(Categorias.OTRO), Categorias.nombre(null));
        assertEquals(Categorias.nombre(Categorias.OTRO), Categorias.nombre(""));
        // Desconocida: no tiene nombre traducido y se escribe tal cual (0).
        assertEquals(0, Categorias.nombre("Comida rara"));
    }

    @Test
    public void bebidas_va_por_ml() {
        assertTrue(Categorias.esBebida("Bebidas"));
        assertFalse(Categorias.esBebida("Lácteos"));
        assertFalse(Categorias.esBebida(null));
    }

    private static String recurso(int id) {
        for (java.lang.reflect.Field f : R.string.class.getFields()) {
            try {
                if (f.getInt(null) == id) return f.getName();
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
        throw new AssertionError("sin recurso " + id);
    }
}
