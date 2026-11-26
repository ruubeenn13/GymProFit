package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// TablasLegalesWebTest — las tablas de la política de privacidad se leen en un móvil (fase 4
// de «Un día»).
//
// A 320 px de columna, tres columnas dejaban una o dos palabras por línea. Por debajo de
// 560 px cada fila se apila y cada celda enseña encima el nombre de su columna, sacado de su
// data-label. Si una celda nueva entra sin él, o con el de otra columna, el móvil la rotularía
// mal sin que nadie lo vea en escritorio. Los role mantienen la tabla para el lector de
// pantalla aunque cambie el display.
// ============================================================
public class TablasLegalesWebTest {

    @Test
    public void cadaCeldaLlevaElNombreDeSuColumna() throws IOException {
        String html = new String(Files.readAllBytes(WebDist.dist().resolve("privacidad.html")), StandardCharsets.UTF_8);
        Matcher tabla = Pattern.compile("(?s)<table\\b[^>]*>(.*?)</table>").matcher(html);
        int tablas = 0;
        while (tabla.find()) {
            tablas++;
            String t = tabla.group(1);
            assertTrue("una tabla sin role=\"table\"", tabla.group().startsWith("<table role=\"table\""));
            List<String> cabeceras = new ArrayList<>();
            Matcher th = Pattern.compile("<th role=\"columnheader\">(.*?)</th>").matcher(t);
            while (th.find()) cabeceras.add(th.group(1));
            assertEquals("cabeceras de la tabla " + tablas, 3, cabeceras.size());
            Matcher fila = Pattern.compile("(?s)<tr role=\"row\">(<td.*?)</tr>").matcher(t);
            int filas = 0;
            while (fila.find()) {
                filas++;
                Matcher td = Pattern.compile("<td\\b([^>]*)>").matcher(fila.group(1));
                int i = 0;
                while (td.find()) {
                    assertTrue("celda sin role=\"cell\": " + td.group(), td.group(1).contains("role=\"cell\""));
                    assertTrue("celda con el rótulo de otra columna: " + td.group(),
                            td.group(1).contains("data-label=\"" + cabeceras.get(i) + "\""));
                    i++;
                }
                assertEquals("una fila sin tres celdas", 3, i);
            }
            assertTrue("tabla sin filas", filas >= 6);
        }
        assertEquals("la política tiene dos tablas", 2, tablas);
    }

    @Test
    public void enMovilSeApilanConSuRotulo() throws IOException {
        String css = new String(Files.readAllBytes(WebDist.web().resolve("src/estilos/lectura.css")), StandardCharsets.UTF_8);
        int i = css.indexOf("@media (max-width: 560px)");
        assertTrue("lectura.css no apila las tablas en móvil", i >= 0);
        String bloque = css.substring(i);
        assertTrue("las filas no se apilan", bloque.contains("table, tbody, tr, td { display: block; }"));
        assertTrue("las celdas no enseñan su rótulo", bloque.contains("content: attr(data-label)"));
        assertTrue("el rótulo dibujado se lee dos veces con lector de pantalla", bloque.contains("attr(data-label) / \"\""));
    }
}
