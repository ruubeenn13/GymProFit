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
// La tabla de los momentos de DEC-039 nombra cada constante de Movimiento entre
// comillas invertidas y da sus milisegundos en la columna de duración, en el mismo
// orden. Este test lee las dos cosas —el Markdown y el código— y falla si un número
// se separa, si la tabla nombra una constante que no existe o si Movimiento tiene una
// duración que la tabla no recoge. Igual con las cuatro curvas.
// ============================================================
public class MovimientoTest {

    private static final Path CODIGO = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit",
            "utils", "Movimiento.java");

    /** Duraciones de Movimiento que la tabla no recoge a propósito: ninguna («Respira» va como «— · Respira»). */
    private static final Set<String> FUERA_DE_LA_TABLA = Set.of();

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

    // Lote 1.6.1: la nutrición trae los momentos 16 (la lista en cascada) y 17 (código
    // leído) a la tabla de DEC-039, con sus constantes en Movimiento.
    @Test
    public void losMomentosDeLaNutricionEstanEnLaTablaYEnElCodigo() throws IOException {
        Map<String, Long> tabla = duracionesDeLaTabla();
        Map<String, Long> codigo = duracionesDelCodigo();
        String dec = decision();
        assertTrue("falta el momento 16 en DEC-039", dec.contains("| 16 · "));
        assertTrue("falta el momento 17 en DEC-039", dec.contains("| 17 · "));
        for (String c : new String[]{"CASCADA", "CASCADA_ESCALON", "ENCAJA", "HOJA", "BARRIDO"}) {
            assertTrue(c + " no está en la tabla", tabla.containsKey(c));
            assertTrue(c + " no está en Movimiento", codigo.containsKey(c));
        }
        assertEquals(Long.valueOf(350), codigo.get("CASCADA"));
        assertEquals(Long.valueOf(40), codigo.get("CASCADA_ESCALON"));
        assertEquals(Long.valueOf(200), codigo.get("ENCAJA"));
        assertEquals(Long.valueOf(450), codigo.get("HOJA"));
    }

    @Test
    public void elResumenDelEntrenoEstaEnLaTablaYEnElCodigo() throws IOException {
        // Momento 28 del lienzo de la firma visual (GP-210, paso 1): los kilos cuentan desde cero
        // (CIFRAS), los récords entran con el momento 12, la comparación cae en cascada
        // (CASCADA, cada CASCADA_ESCALON) y el medallón del logro salta con TROFEO. No trae
        // nombres nuevos: reutiliza los de los momentos 7, 12 y 16.
        Map<String, Long> tabla = duracionesDeLaTabla();
        Map<String, Long> codigo = duracionesDelCodigo();
        assertTrue("falta el momento 28 en DEC-039", decision().contains("| 28 · "));
        for (String c : new String[]{"CIFRAS", "CASCADA", "CASCADA_ESCALON", "TROFEO"}) {
            assertTrue(c + " no está en la tabla", tabla.containsKey(c));
            assertTrue(c + " no está en Movimiento", codigo.containsKey(c));
        }
        assertEquals(Long.valueOf(1200), codigo.get("CIFRAS"));
        assertEquals(Long.valueOf(350), codigo.get("CASCADA"));
        assertEquals(Long.valueOf(40), codigo.get("CASCADA_ESCALON"));
        assertEquals(Long.valueOf(450), codigo.get("TROFEO"));
    }

    @Test
    public void borrarYDeshacerEstaEnLaTablaYEnElCodigo() throws IOException {
        // Momento 20 del lienzo del 02-10 (lote 1.6.2): la fila se va, la comida se
        // recoloca y el anillo pasa a su valor nuevo con BARRA.
        Map<String, Long> tabla = duracionesDeLaTabla();
        Map<String, Long> codigo = duracionesDelCodigo();
        assertTrue("falta el momento 20 en DEC-039", decision().contains("| 20 · "));
        for (String c : new String[]{"SALE", "RECOLOCA"}) {
            assertTrue(c + " no está en la tabla", tabla.containsKey(c));
            assertTrue(c + " no está en Movimiento", codigo.containsKey(c));
        }
        assertEquals(Long.valueOf(380), codigo.get("SALE"));
        assertEquals(Long.valueOf(250), codigo.get("RECOLOCA"));
    }

    @Test
    public void elMasYElCorazonEstanEnLaTablaYEnElCodigo() throws IOException {
        // Momentos 15 y 19 del lienzo del 02-10 (lote 1.6.3), con los números del
        // tablero «Así se mueve».
        Map<String, Long> tabla = duracionesDeLaTabla();
        Map<String, Long> codigo = duracionesDelCodigo();
        assertTrue("falta el momento 15 en DEC-039", decision().contains("| 15 · "));
        assertTrue("falta el momento 19 en DEC-039", decision().contains("| 19 · "));
        for (String c : new String[]{"HUNDE", "VERDE", "CHECK", "CORAZON", "RESALTA"}) {
            assertTrue(c + " no está en la tabla", tabla.containsKey(c));
            assertTrue(c + " no está en Movimiento", codigo.containsKey(c));
        }
        assertEquals(Long.valueOf(70), codigo.get("HUNDE"));
        assertEquals(Long.valueOf(120), codigo.get("VERDE"));
        assertEquals(Long.valueOf(260), codigo.get("CHECK"));
        assertEquals(Long.valueOf(450), codigo.get("CORAZON"));
        assertEquals(Long.valueOf(1400), codigo.get("RESALTA"));
    }

    @Test
    public void laCopiaDeAyerEstaEnLaTablaYEnElCodigo() throws IOException {
        // Momento 18 del lienzo del 02-10 (lote 1.6.4), con los números del tablero «Así se
        // mueve»: la tarjeta se pliega del 16 al 22 % de un bucle de 3 s (180 ms) y cada
        // alimento cae del 20 al 32 % (360 ms), el segundo 120 ms después.
        Map<String, Long> tabla = duracionesDeLaTabla();
        Map<String, Long> codigo = duracionesDelCodigo();
        assertTrue("falta el momento 18 en DEC-039", decision().contains("| 18 · "));
        for (String c : new String[]{"PLIEGA", "CAE", "CAE_ESCALON"}) {
            assertTrue(c + " no está en la tabla", tabla.containsKey(c));
            assertTrue(c + " no está en Movimiento", codigo.containsKey(c));
        }
        assertEquals(Long.valueOf(180), codigo.get("PLIEGA"));
        assertEquals(Long.valueOf(360), codigo.get("CAE"));
        assertEquals(Long.valueOf(120), codigo.get("CAE_ESCALON"));
    }

    @Test
    public void elDiaCompletoYElVasoEstanEnLaTablaYEnElCodigo() throws IOException {
        // Momentos 21 y 22 (lote 1.6.8), del tablero 3 y de «Así se mueve»: el anillo se
        // cierra en 600 ms, el punto salta en 450 y el vaso se llena en 450.
        Map<String, Long> tabla = duracionesDeLaTabla();
        Map<String, Long> codigo = duracionesDelCodigo();
        assertTrue("falta el momento 21 en DEC-039", decision().contains("| 21 · "));
        assertTrue("falta el momento 22 en DEC-039", decision().contains("| 22 · "));
        for (String c : new String[]{"CIERRA", "PUNTO", "VASO", "HALO", "BRILLO"}) {
            assertTrue(c + " no está en la tabla", tabla.containsKey(c));
            assertTrue(c + " no está en Movimiento", codigo.containsKey(c));
        }
        assertEquals(Long.valueOf(600), codigo.get("CIERRA"));
        assertEquals(Long.valueOf(450), codigo.get("PUNTO"));
        assertEquals(Long.valueOf(450), codigo.get("VASO"));
    }

    @Test
    public void elPlatoEstaEnLaTablaYEnElCodigo() throws IOException {
        // Momento 23 (lote 1.6.6), con los números de los tableros 9 y 10: las cifras caen
        // en 250 ms, el velo en 250, la hoja sube en 450 y su ✓ salta en 450, 200 después.
        Map<String, Long> tabla = duracionesDeLaTabla();
        Map<String, Long> codigo = duracionesDelCodigo();
        assertTrue("falta el momento 23 en DEC-039", decision().contains("| 23 · "));
        for (String c : new String[]{"CIFRA", "VELO", "SALTA_OK", "SALTA_OK_RETRASO", "HOJA", "CASCADA"}) {
            assertTrue(c + " no está en la tabla", tabla.containsKey(c));
            assertTrue(c + " no está en Movimiento", codigo.containsKey(c));
        }
        assertEquals(Long.valueOf(250), codigo.get("CIFRA"));
        assertEquals(Long.valueOf(250), codigo.get("VELO"));
        assertEquals(Long.valueOf(450), codigo.get("SALTA_OK"));
        assertEquals(Long.valueOf(200), codigo.get("SALTA_OK_RETRASO"));
    }

    @Test
    public void laSesionEnVivoYElDescansoEstanEnLaTablaYEnElCodigo() throws IOException {
        // Momentos 24 a 27 del lienzo de la firma visual (GP-210, paso 3). El 24 usa RELLENO,
        // CHECK y BARRA, que ya existían, y estrena SUMA (600, la cifra que suma al marcar);
        // el 25 estrena ORO (900) y reutiliza ENTRA, TROFEO, CHISPAS y BRILLO; el 26 estrena
        // BAJA (250) y reutiliza HOJA y LATIDO. El 27 es la notificación del sistema y no
        // trae número.
        Map<String, Long> tabla = duracionesDeLaTabla();
        Map<String, Long> codigo = duracionesDelCodigo();
        String dec = decision();
        for (String n : new String[]{"24", "25", "26", "27"}) {
            assertTrue("falta el momento " + n + " en DEC-039", dec.contains("| " + n + " · "));
        }
        for (String c : new String[]{"RELLENO", "CHECK", "SUMA", "BARRA", "ORO", "ENTRA", "TROFEO", "CHISPAS",
                "BRILLO", "HOJA", "LATIDO", "BAJA"}) {
            assertTrue(c + " no está en la tabla", tabla.containsKey(c));
            assertTrue(c + " no está en Movimiento", codigo.containsKey(c));
        }
        assertEquals(Long.valueOf(150), codigo.get("RELLENO"));
        assertEquals(Long.valueOf(260), codigo.get("CHECK"));
        assertEquals(Long.valueOf(600), codigo.get("SUMA"));
        assertEquals(Long.valueOf(500), codigo.get("BARRA"));
        assertEquals(Long.valueOf(900), codigo.get("ORO"));
        assertEquals(Long.valueOf(350), codigo.get("ENTRA"));
        assertEquals(Long.valueOf(450), codigo.get("HOJA"));
        assertEquals(Long.valueOf(900), codigo.get("LATIDO"));
        assertEquals(Long.valueOf(250), codigo.get("BAJA"));
    }

    @Test
    public void lasTransicionesYElBotonQueCambiaDeFormaEstanEnLaTablaYEnElCodigo() throws IOException {
        // GP-210 paso 2 (Material motion y Expressive): cuatro filas sin número en DEC-039 —tarjeta a
        // detalle, avanzar en un flujo, atrás en una hoja propia y el botón que cambia de forma—.
        Map<String, Long> tabla = duracionesDeLaTabla();
        Map<String, Long> codigo = duracionesDelCodigo();
        String dec = decision();
        for (String f : new String[]{"Tarjeta a detalle", "Avanzar en un flujo", "Atrás en una hoja propia", "El botón cambia de forma"}) {
            assertTrue("falta la fila «" + f + "» en DEC-039", dec.contains("| — · " + f));
        }
        for (String c : new String[]{"CONTENEDOR_ABRE", "CONTENEDOR_VUELVE", "EJE_X", "ATRAS_SUELTA", "TOQUE"}) {
            assertTrue(c + " no está en la tabla", tabla.containsKey(c));
            assertTrue(c + " no está en Movimiento", codigo.containsKey(c));
        }
        assertEquals("el contenedor crece en 400 ms", Long.valueOf(400), codigo.get("CONTENEDOR_ABRE"));
        assertEquals("y vuelve en 350", Long.valueOf(350), codigo.get("CONTENEDOR_VUELVE"));
        assertEquals("eje X compartido en 300", Long.valueOf(300), codigo.get("EJE_X"));
        assertEquals("al soltar el atrás, 250", Long.valueOf(250), codigo.get("ATRAS_SUELTA"));
        assertEquals("la forma del botón, con el toque", Long.valueOf(120), codigo.get("TOQUE"));

        String m = new String(Files.readAllBytes(CODIGO), StandardCharsets.UTF_8);
        assertTrue("eje X de 30 dp", m.contains("EJE_X_DP = 30;"));
        assertTrue("la hoja sigue al dedo hasta 0,9", m.contains("ATRAS_ESCALA = 0.9f;"));
        // La dec lo dice con las mismas cifras.
        assertTrue(dec.contains("30 dp"));
        assertTrue(dec.contains("90 %"));
        assertTrue(dec.contains("de 26 a 12 dp"));
    }

    @Test
    public void lasTransicionesNoEscribenMilisegundosNiCurvas() throws IOException {
        // Las clases nuevas piden el momento a Movimiento: ni una duración ni una curva suelta.
        Path base = CODIGO.getParent().getParent();
        for (String f : new String[]{"utils/Transiciones.java", "utils/AtrasHoja.java", "ui/widget/BotonExpresivo.java"}) {
            String c = new String(Files.readAllBytes(base.resolve(f)), StandardCharsets.UTF_8);
            assertFalse(f + " escribe una duración", Pattern.compile("setDuration\\(\\s*\\d").matcher(c).find());
            assertFalse(f + " escribe una curva", c.contains("new PathInterpolator") || c.contains("DecelerateInterpolator")
                    || c.contains("AccelerateDecelerateInterpolator"));
        }
        String t = new String(Files.readAllBytes(base.resolve("utils/Transiciones.java")), StandardCharsets.UTF_8);
        assertTrue(t.contains("Movimiento.CONTENEDOR_ABRE") && t.contains("Movimiento.CONTENEDOR_VUELVE"));
        assertTrue(t.contains("Movimiento.ENFATIZADA") && t.contains("Movimiento.ESTANDAR"));
        assertTrue("con «Quitar animaciones» no hay transición de escena", t.contains("Movimiento.quieto("));
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
            if (!linea.matches("\\| (\\d+|—) · .*")) continue;
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
