package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Categorias;

// ============================================================
// TintesCategoriaTest — los iconos de alimento van teñidos por su categoría, como el tablero
// (GP-225), con colores de dato que pasan 3:1 sobre el círculo (surface_2) en los dos temas.
// ============================================================
public class TintesCategoriaTest {

    private static final Path RES = Paths.get("src", "main", "res");

    @Test
    public void cadaCategoriaConSuColor() {
        assertEquals("gp_macro_proteinas", Categorias.tinte("Carnes y aves"));
        assertEquals("gp_macro_proteinas", Categorias.tinte("Huevos"));
        assertEquals("gp_macro_carbos", Categorias.tinte("Cereales y pan"));
        assertEquals("gp_macro_carbos", Categorias.tinte("Frutas"));
        assertEquals("gp_macro_grasas", Categorias.tinte("Aceites y grasas"));
        assertEquals("gp_success", Categorias.tinte("Verduras"));
        assertEquals("gp_agua", Categorias.tinte("Bebidas"));
        assertEquals(R.color.gp_macro_proteinas, Categorias.color("Pescado y marisco"));
    }

    @Test
    public void sinCategoriaElColorDelTexto() {
        assertNull(Categorias.tinte(Categorias.OTRO));
        assertNull(Categorias.tinte(null));
        assertNull(Categorias.tinte("Algo raro"));
        assertEquals(0, Categorias.color(null));
    }

    @Test
    public void todosPasanTresAUnoSobreElCirculo() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : new String[]{ "values", "values-night" }) {
            for (String clave : Categorias.CLAVES) {
                String t = Categorias.tinte(clave);
                if (t == null) continue;
                double r = MedidorNivelTest.contraste(color(tema, t), color(tema, "gp_surface_2"));
                if (r < 3.0) fallos.add(String.format("%s: %s (%s) = %.2f", tema, clave, t, r));
            }
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    private static String color(String tema, String nombre) throws IOException {
        String v = buscar(leer(RES.resolve(tema + "/colors.xml")), nombre);
        if (v == null) v = buscar(leer(RES.resolve("values/colors.xml")), nombre);
        if (v == null) throw new AssertionError("no está " + nombre);
        return v;
    }

    private static String buscar(String xml, String nombre) {
        Matcher m = Pattern.compile("<color name=\"" + nombre + "\">(#[0-9A-Fa-f]{6})</color>").matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
