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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// MovimientoTest — GP-104: lo que dice DEC-039 es lo que hace la app.
//
// La tabla de los doce momentos de DEC-039 nombra cada constante de Movimiento entre
// comillas invertidas y da sus milisegundos en la columna de duración, en el mismo
// orden. Este test lee las dos cosas —el Markdown y el código— y falla si un número
// se separa, si la tabla nombra una constante que no existe o si Movimiento tiene una
// duración que la tabla no recoge. Igual con las cuatro curvas.
// ============================================================
public class MovimientoTest {

    private static final Path CODIGO = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit",
            "utils", "Movimiento.java");

    /** Duraciones de Movimiento que no son un momento del lienzo y no van en la tabla. */
    private static final Set<String> FUERA_DE_LA_TABLA = Set.of("RESPIRA");

    @Test
    public void cadaDuracionDeLaTablaEsLaDelCodigo() throws IOException {
        Map<String, Long> codigo = duracionesDelCodigo();
        Map<String, Long> tabla = duracionesDeLaTabla();

        assertFalse("la tabla de DEC-039 no se ha podido leer", tabla.isEmpty());
        for (Map.Entry<String, Long> e : tabla.entrySet()) {
            assertTrue("DEC-039 nombra " + e.getKey() + ", que no está en Movimiento",
                    codigo.containsKey(e.getKey()));
            assertEquals("duración de " + e.getKey() + " en DEC-039 frente a Movimiento",
                    codigo.get(e.getKey()), e.getValue());
        }
        Set<String> sinTabla = new TreeSet<>(codigo.keySet());
        sinTabla.removeAll(tabla.keySet());
        sinTabla.removeAll(FUERA_DE_LA_TABLA);
        assertTrue("duraciones de Movimiento que DEC-039 no recoge: " + sinTabla, sinTabla.isEmpty());
    }

    @Test
    public void lasCuatroCurvasSonLasDeLaDecision() throws IOException {
        String codigo = new String(Files.readAllBytes(CODIGO), StandardCharsets.UTF_8);
        String dec = decision();
        String[][] curvas = {
                {"ENFATIZADA", ".05,.7,.1,1", "0.05f, 0.7f, 0.1f, 1f"},
                {"ESTANDAR", ".2,0,0,1", "0.2f, 0f, 0f, 1f"},
                {"REBOTE", ".34,1.56,.64,1", "0.34f, 1.56f, 0.64f, 1f"},
                {"TIEMBLA", ".36,.07,.19,.97", "0.36f, 0.07f, 0.19f, 0.97f"},
        };
        for (String[] c : curvas) {
            assertTrue("DEC-039 no da la curva " + c[1], dec.contains(c[1]));
            assertTrue("Movimiento." + c[0] + " no es " + c[2],
                    codigo.contains(c[0] + " = new PathInterpolator(" + c[2] + ")"));
        }
    }

    // `public static final long NOMBRE = 123;`
    private static Map<String, Long> duracionesDelCodigo() throws IOException {
        String codigo = new String(Files.readAllBytes(CODIGO), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("public static final long (\\w+) = (\\d+);").matcher(codigo);
        Map<String, Long> d = new LinkedHashMap<>();
        while (m.find()) d.put(m.group(1), Long.parseLong(m.group(2)));
        return d;
    }

    // Cada fila de la tabla: las constantes de la segunda columna, en orden, con los
    // números de 10 o más de la tercera (los «×2» y la fórmula de la cuenta no cuentan).
    private static Map<String, Long> duracionesDeLaTabla() throws IOException {
        Map<String, Long> d = new LinkedHashMap<>();
        Pattern nombre = Pattern.compile("`([A-Z_]+)`");
        Pattern numero = Pattern.compile("\\d+");
        for (String linea : decision().split("\n")) {
            if (!linea.matches("\\| \\d+ · .*")) continue;
            String[] col = linea.split("\\|");
            List<String> nombres = new ArrayList<>();
            Matcher mn = nombre.matcher(col[2]);
            while (mn.find()) if (!nombres.contains(mn.group(1))) nombres.add(mn.group(1));
            List<Long> numeros = new ArrayList<>();
            Matcher mm = numero.matcher(col[3]);
            while (mm.find()) {
                long n = Long.parseLong(mm.group());
                if (n >= 10) numeros.add(n);
            }
            assertEquals("en la fila «" + col[1].trim() + "» de DEC-039 no casan nombres y números: "
                    + nombres + " / " + numeros, nombres.size(), numeros.size());
            for (int i = 0; i < nombres.size(); i++) d.put(nombres.get(i), numeros.get(i));
        }
        return d;
    }

    // El texto de DEC-039, hasta la siguiente decisión.
    private static String decision() throws IOException {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("documentacion"))) raiz = raiz.getParent();
        assertTrue("no se encuentra la carpeta documentacion", raiz != null);
        String todo = new String(Files.readAllBytes(raiz.resolve("documentacion").resolve("PRODUCT-DECISIONS.md")),
                StandardCharsets.UTF_8).replace("\r\n", "\n");
        int desde = todo.indexOf("### DEC-039");
        assertTrue("no está DEC-039", desde >= 0);
        int hasta = todo.indexOf("\n### ", desde + 5);
        return todo.substring(desde, hasta < 0 ? todo.length() : hasta);
    }
}
