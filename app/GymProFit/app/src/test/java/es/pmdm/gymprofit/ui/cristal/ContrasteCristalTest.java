package es.pmdm.gymprofit.ui.cristal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// ContrasteCristalTest — GP-285, DEC-070: las reglas de legibilidad de Liquid Glass, medidas.
//
// Regla 2 del diseño: cada par texto/superficie pasa 4,5:1 (3:1 en texto de 24 sp o más y en
// gráficos) sobre el punto MÁS DESFAVORABLE de lo que puede haber detrás: fondo + brillo +
// cristal compuestos. En oscuro ese punto es el más claro; en claro, el más oscuro. Aquí no se
// supone cuál es: se recorre el fondo entero (Ambiente.colorEn, la misma cuenta que pinta
// FondoAmbiental) en varios tamaños de pantalla y se compone cada capa de cristal encima
// con sus tres tonos (alto, medio y bajo).
//
// Regla 3: el cristal vivo pasa en los tres niveles. Detrás de la barra puede haber cualquier
// punto del fondo, con o sin una tarjeta de cristal, desenfocado (que promedia: nunca supera
// al punto extremo) y con la saturación subida, y además el velo del «+» cuando la hoja está
// abierta; encima va su tinte (niveles 1 y 2) o su superficie casi opaca (nivel 3).
//
// Lee values/colors.xml y values-night/colors.xml tal cual. Deja las cifras de cada par en
// build/reports/contraste-cristal.txt, de donde salen las de DESIGN.md.
// ============================================================
public class ContrasteCristalTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final String[] TEMAS = { "values-night", "values" };
    // Ancho × alto en dp: móvil pequeño, la maqueta, el emulador, uno estrecho y alto, una
    // tableta y un móvil tumbado.
    private static final float[][] PANTALLAS = { {360, 640}, {390, 844}, {411, 914}, {360, 800}, {600, 960}, {844, 390} };

    // Los tres tonos del relleno del cristal estático y los dos de cada cristal vivo.
    private static final String[] CRISTAL = { "gp_cristal_alto", "gp_cristal_medio", "gp_cristal_bajo" };
    private static final String[] VIVO = { "gp_vivo_alto", "gp_vivo_bajo" };
    private static final String[] VIVO_OPACO = { "gp_vivo_opaco_alto", "gp_vivo_opaco_bajo" };
    private static final String[] ORO = { "gp_cristal_oro_alto", "gp_cristal_oro_bajo" };

    private static final double TEXTO = 4.5, GRANDE = 3.0;

    // ── Regla 2: cristal estático ────────────────────────────────────────────

    @Test
    public void el_texto_sobre_el_fondo_ambiental_pasa_en_los_dos_temas() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            Set<Integer> fondo = t.fondo();
            t.medir("sobre el fondo", "gp_cristal_texto", fondo, TEXTO, fallos);
            t.medir("sobre el fondo", "gp_cristal_texto_2", fondo, TEXTO, fallos);
            t.medir("sobre el fondo", "gp_cristal_enlace", fondo, TEXTO, fallos);
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    @Test
    public void el_texto_sobre_el_cristal_estatico_pasa_en_los_dos_temas() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            Set<Integer> cristal = t.capas(t.fondo(), CRISTAL);
            for (String texto : new String[]{ "gp_cristal_texto", "gp_cristal_texto_2", "gp_cristal_acento", "gp_cristal_exito",
                    "gp_cristal_oro" }) {
                t.medir("sobre el cristal", texto, cristal, TEXTO, fallos);
            }
            // Gráficos: iconos con color, barras de progreso y la última barra de la gráfica.
            for (String grafico : new String[]{ "gp_cristal_enlace", "gp_macro_proteinas", "gp_agua" }) {
                t.medir("gráfico sobre el cristal", grafico, cristal, GRANDE, fallos);
            }
            // El chip y el botón secundario: cristal plano encima del de la tarjeta.
            Set<Integer> chip = t.capas(cristal, new String[]{ "gp_cristal_boton" });
            t.medir("sobre el chip", "gp_cristal_texto", chip, TEXTO, fallos);
            // La fila del récord: degradado dorado encima del cristal. Sus dos líneas van en el
            // texto principal (el secundario no llega) y la marca, a 26 sp, en su dorado.
            Set<Integer> oro = t.capas(cristal, ORO);
            t.medir("sobre el récord", "gp_cristal_texto", oro, TEXTO, fallos);
            t.medir("cifra sobre el récord", "gp_cristal_oro_cifra", oro, GRANDE, fallos);
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    // Lo que es translúcido y va sobre el cristal se mide ya compuesto: una barra apagada de la
    // gráfica contra el cristal que la rodea, y el relleno de una barra de progreso contra su pista.
    @Test
    public void las_graficas_sobre_el_cristal_pasan_3_a_1() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            int tenue = t.color("gp_cristal_barra_tenue"), pista = t.color("gp_cristal_pista");
            double peorBarra = Double.MAX_VALUE;
            Set<Integer> pistas = new LinkedHashSet<>();
            for (int c : t.capas(t.fondo(), CRISTAL)) {
                peorBarra = Math.min(peorBarra, contraste(Ambiente.sobre(tenue, c), c));
                pistas.add(Ambiente.sobre(pista, c));
            }
            if (peorBarra < GRANDE) {
                fallos.add(String.format(Locale.ROOT, "%s barra apagada contra el cristal %.2f:1", t.nombre, peorBarra));
            }
            for (String relleno : new String[]{ "gp_cristal_enlace", "gp_macro_proteinas", "gp_agua" }) {
                t.medir("relleno contra su pista", relleno, pistas, GRANDE, fallos);
            }
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    // ── Regla 3: cristal vivo, en los tres niveles ───────────────────────────

    @Test
    public void la_barra_se_lee_con_desenfoque_y_sin_el() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            // Lo que puede quedar detrás de un cristal vivo: el fondo, con o sin una tarjeta
            // encima, y eso mismo con la saturación del desenfoque.
            Set<Integer> detras = new LinkedHashSet<>(t.fondo());
            detras.addAll(t.capas(t.fondo(), CRISTAL));
            // Y todo eso con el velo del «+» encima: con la hoja abierta, la hoja y la barra
            // desenfocan el contenido ya oscurecido.
            detras.addAll(t.capas(detras, new String[]{ "gp_velo_cristal" }));
            Set<Integer> saturado = new LinkedHashSet<>(detras);
            for (int c : detras) saturado.add(saturar(c, CristalVivo.SATURACION));

            String[][] niveles = { VIVO, VIVO_OPACO };
            String[] nombres = { "niveles 1 y 2", "nivel 3" };
            for (int n = 0; n < niveles.length; n++) {
                Set<Integer> barra = t.capas(n == 0 ? saturado : detras, niveles[n]);
                String donde = "cristal vivo, " + nombres[n];
                // Pestañas sin elegir, y los textos de la barra de sesión y de la hoja del «+».
                t.medir(donde, "gp_vivo_texto", barra, TEXTO, fallos);
                t.medir(donde, "gp_cristal_texto", barra, TEXTO, fallos);
                t.medir(donde, "gp_cristal_texto_2", barra, TEXTO, fallos);
                t.medir(donde, "gp_vivo_acento", barra, TEXTO, fallos);
                // La pestaña elegida: su icono y su etiqueta, sobre la lente.
                Set<Integer> lente = t.capas(barra, new String[]{ "gp_lente" });
                t.medir(donde + ", lente", "gp_vivo_acento", lente, TEXTO, fallos);
            }
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    @Test
    public void el_mas_se_lee_sobre_su_degradado() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            Set<Integer> mas = new LinkedHashSet<>();
            mas.add(t.color("gp_mas_claro"));
            mas.add(t.color("gp_mas_oscuro"));
            t.medir("el «+»", "gp_mas_icono", mas, GRANDE, fallos);
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    // ── La cuenta ───────────────────────────────────────────────────────────

    // Que la cuenta de este test es la de siempre: blanco sobre negro, 21:1, y el par que
    // PaletaContrasteTest ya tiene medido (#1A0E00 sobre #FF6A00, 6,61:1).
    @Test
    public void la_cuenta_del_contraste_es_la_de_wcag() {
        assertEquals(21.0, contraste(0xFFFFFFFF, 0xFF000000), 0.001);
        assertEquals(6.61, contraste(0xFF1A0E00, 0xFFFF6A00), 0.01);
        // Un 50 % de blanco sobre negro es gris medio, no otra cosa.
        assertEquals(0xFF808080, Ambiente.sobre(0x80FFFFFF, 0xFF000000));
    }

    // Un brillo se apaga al 70 % de su radio y en su centro tiene su alfa entero. Con los
    // números escritos: un radio de 400 se apaga a 280 del centro.
    @Test
    public void un_brillo_se_apaga_al_70_por_ciento_del_radio() {
        Ambiente.Brillo b = new Ambiente.Brillo(0.5f, 0.5f, 1f, 1f, 0x80FF0000);
        assertEquals(128 / 255f, Ambiente.alfaEn(b, 200, 200, 400, 400), 0.001f);
        assertEquals(0f, Ambiente.alfaEn(b, 200 + 280, 200, 400, 400), 0.001f);
        assertTrue(Ambiente.alfaEn(b, 200 + 276, 200, 400, 400) > 0f);
        assertEquals(64 / 255f, Ambiente.alfaEn(b, 200 + 140, 200, 400, 400), 0.002f);
        // Y más allá sigue apagado.
        assertEquals(0f, Ambiente.alfaEn(b, 200 + 300, 200, 400, 400), 0f);
    }

    // ── Ayudas ──────────────────────────────────────────────────────────────

    private static final StringBuilder INFORME = new StringBuilder();

    /** Los colores de un tema y las superficies que se componen con ellos. */
    private static final class Tema {
        final String nombre;
        final Map<String, String> crudo = new HashMap<>();

        Tema(String carpeta) throws IOException {
            nombre = carpeta.equals("values") ? "claro" : "oscuro";
            leer("values");
            if (!carpeta.equals("values")) leer(carpeta);
        }

        private void leer(String carpeta) throws IOException {
            String xml = new String(Files.readAllBytes(RES.resolve(carpeta).resolve("colors.xml")), StandardCharsets.UTF_8);
            xml = xml.replaceAll("(?s)<!--.*?-->", "");
            Matcher m = Pattern.compile("<color name=\"([^\"]+)\">([^<]+)</color>").matcher(xml);
            while (m.find()) crudo.put(m.group(1), m.group(2).trim());
        }

        int color(String token) {
            String v = crudo.get(token);
            if (v == null) throw new AssertionError("no existe el color " + token + " en el tema " + nombre);
            if (v.startsWith("@color/")) return color(v.substring(7));
            long n = Long.parseLong(v.substring(1), 16);
            return (int) (v.length() == 7 ? 0xFF000000L | n : n);
        }

        /** Todos los colores que el fondo ambiental puede enseñar, en todas las pantallas. */
        Set<Integer> fondo() {
            Ambiente.Brillo[] brillos = Ambiente.inicio(color("gp_ambiente_brillo_1"),
                    color("gp_ambiente_brillo_2"), color("gp_ambiente_brillo_3"));
            int base = color("gp_ambiente_base");
            Set<Integer> colores = new LinkedHashSet<>();
            for (float[] p : PANTALLAS) {
                for (float y = 0; y <= p[1]; y += 2) {
                    for (float x = 0; x <= p[0]; x += 2) {
                        colores.add(Ambiente.colorEn(x, y, p[0], p[1], base, brillos));
                    }
                }
            }
            return colores;
        }

        /** Cada color de debajo con cada una de las capas compuesta encima. */
        Set<Integer> capas(Set<Integer> debajo, String[] tokens) {
            Set<Integer> colores = new LinkedHashSet<>();
            for (String token : tokens) {
                int capa = color(token);
                for (int c : debajo) colores.add(Ambiente.sobre(capa, c));
            }
            return colores;
        }

        /** El peor contraste del texto sobre esas superficies; si no llega, lo apunta. */
        void medir(String donde, String texto, Set<Integer> superficies, double minimo, List<String> fallos) {
            int t = color(texto);
            double peor = Double.MAX_VALUE;
            int peorColor = 0;
            for (int s : superficies) {
                double c = contraste(t, s);
                if (c < peor) {
                    peor = c;
                    peorColor = s;
                }
            }
            String linea = String.format(Locale.ROOT, "%-6s %-28s %-22s %5.2f:1 (mínimo %.1f) sobre #%06X",
                    nombre, donde, texto, peor, minimo, peorColor & 0xFFFFFF);
            synchronized (INFORME) {
                INFORME.append(linea).append('\n');
                try {
                    Path salida = Paths.get("build", "reports", "contraste-cristal.txt");
                    Files.createDirectories(salida.getParent());
                    Files.write(salida, INFORME.toString().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
            if (peor < minimo) fallos.add(linea);
        }
    }

    // La matriz de saturación de Android (ColorMatrix.setSaturation), sobre un color opaco.
    private static int saturar(int color, float s) {
        float r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        float gris = 0.213f * r + 0.715f * g + 0.072f * b;
        return 0xFF000000 | (canal(gris + (r - gris) * s) << 16) | (canal(gris + (g - gris) * s) << 8)
                | canal(gris + (b - gris) * s);
    }

    private static int canal(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }

    private static double contraste(int a, int b) {
        double la = luminancia(a), lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminancia(int color) {
        return 0.2126 * lineal((color >> 16) & 0xFF) + 0.7152 * lineal((color >> 8) & 0xFF)
                + 0.0722 * lineal(color & 0xFF);
    }

    private static double lineal(int canal) {
        double v = canal / 255.0;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }
}
