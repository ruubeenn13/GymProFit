package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// TitularesWebTest — ningún titular de la portada ni de la beta crece o mengua sin tope
// (GP-203).
//
// Los titulares grandes llenan su columna línea a línea con --k. Calculados sobre 100vw
// tocaban el borde derecho a 360–375 px, porque vw cuenta también la barra de
// desplazamiento y el trazo de la letra sobresale de su caja. Ahora salen del ancho real
// de la columna (cqi), con holgura, y van dentro de un clamp() con mínimo y máximo en px.
// Este test falla si un tamaño de letra fluido —el que depende del ancho de la pantalla,
// del contenedor o de --k— no está acotado así. Que encaje de verdad a 320, 375 y 1440
// se mide en el navegador: aquí solo se comprueba que nadie quita los topes.
// ============================================================
public class TitularesWebTest {

    private static final String[] HOJAS = {"portada.css", "beta.css"};

    /** Un tamaño que depende del ancho: de la pantalla, del contenedor o del --k del titular. */
    private static final Pattern FLUIDO = Pattern.compile("\\d(vw|vi|vmin|vmax|cqi|cqw)\\b|var\\(--k\\)|var\\(--ancho");

    /** clamp(<mínimo>px, <lo que sea>, <máximo>px) */
    private static final Pattern ACOTADO = Pattern.compile("^clamp\\(\\s*\\d+(\\.\\d+)?px\\s*,.+,\\s*\\d+(\\.\\d+)?px\\s*\\)$");

    @Test
    public void todoTamanoFluidoVaAcotado() throws IOException {
        int fluidos = 0;
        for (String hoja : HOJAS) {
            Matcher tamano = Pattern.compile("font-size:\\s*([^;]+);").matcher(leer(hoja));
            while (tamano.find()) {
                String valor = tamano.group(1).trim();
                if (!FLUIDO.matcher(valor).find()) continue;
                fluidos++;
                assertTrue(hoja + ": un tamaño fluido sin clamp() con mínimo y máximo en px: " + valor,
                        ACOTADO.matcher(valor).matches());
            }
        }
        assertTrue("no se ha encontrado ningún tamaño fluido", fluidos >= 4);
    }

    @Test
    public void losTitularesQueLlenanElAnchoSalenDeSuColumna() throws IOException {
        comprobarTitular("portada.css", ".titular > span", ".titular {");
        comprobarTitular("portada.css", ".problema h2 > span", ".problema h2 {");
        comprobarTitular("beta.css", ".titular span", ".titular {");
    }

    @Test
    public void cadaLineaConKEstaEnUnTitularAcotado() throws IOException {
        // Toda línea con --k vive en un h1.titular o en el h2 del problema, los únicos acotados
        for (String pagina : new String[]{"index.html", "beta.html", "beta/revisa.html", "beta/dentro.html",
                "beta/entrar.html"}) {
            String html = new String(Files.readAllBytes(web().resolve(pagina)), StandardCharsets.UTF_8);
            int lineas = html.split("style=\"--k:", -1).length - 1;
            int enTitulares = 0;
            Matcher titular = Pattern.compile("<h1 class=\"titular\"[^>]*>(.*?)</h1>|<h2 id=\"problema\">(.*?)</h2>",
                    Pattern.DOTALL).matcher(html);
            while (titular.find()) {
                String dentro = titular.group(1) != null ? titular.group(1) : titular.group(2);
                enTitulares += dentro.split("style=\"--k:", -1).length - 1;
            }
            assertTrue(pagina + " no tiene ninguna línea con --k", lineas > 0);
            assertEquals(pagina + ": hay líneas con --k fuera de un titular acotado", lineas, enTitulares);
        }
    }

    // La regla de las líneas va acotada y su último tamaño sale del contenedor; el padre es el contenedor.
    private static void comprobarTitular(String hoja, String lineas, String padre) throws IOException {
        String css = leer(hoja);
        String regla = bloque(css, lineas + " {");
        Matcher tamano = Pattern.compile("font-size:\\s*([^;]+);").matcher(regla);
        String ultimo = null;
        while (tamano.find()) {
            ultimo = tamano.group(1).trim();
            assertTrue(hoja + " " + lineas + ": tamaño sin acotar: " + ultimo, ACOTADO.matcher(ultimo).matches());
        }
        assertNotNull(hoja + " " + lineas + " no tiene tamaño", ultimo);
        assertTrue(hoja + " " + lineas + ": el tamaño no sale del ancho de la columna (cqi): " + ultimo,
                ultimo.contains("cqi"));
        assertTrue(hoja + " " + padre + " no es el contenedor de sus líneas",
                bloque(css, padre).contains("container-type: inline-size"));
    }

    private static String bloque(String css, String inicio) {
        int i = css.indexOf(inicio);
        assertTrue("no se encuentra la regla " + inicio, i >= 0);
        return css.substring(i, css.indexOf('}', i));
    }

    private static String leer(String nombre) throws IOException {
        return new String(Files.readAllBytes(web().resolve(nombre)), StandardCharsets.UTF_8);
    }

    private static Path web() {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("web"))) raiz = raiz.getParent();
        assertNotNull("no se encuentra la carpeta web", raiz);
        return raiz.resolve("web");
    }
}
