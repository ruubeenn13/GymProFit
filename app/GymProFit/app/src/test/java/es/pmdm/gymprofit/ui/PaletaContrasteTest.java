package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// PaletaContrasteTest — GP-063: la paleta pasa AA en los dos temas.
//
// Lee colors.xml y values-night/colors.xml tal cual y mide cada par que la app
// pinta de verdad: texto a 4,5:1 y bordes a 3:1 (WCAG 1.4.3 y 1.4.11), contra
// el fondo, la tarjeta y la tarjeta TEÑIDA. Esa última no está en ningún XML:
// Widget.GymProFit.Card lleva 1dp de elevación, y Material 3 la mezcla con un
// 5 % de colorPrimary (medido en captura: #FBF5F2 en claro, #262122 en oscuro).
//
// La excepción documentada es gp_divider: el separador decorativo de dentro de
// una tarjeta, que no delimita ningún componente y por eso no entra aquí.
//
// El segundo bloque fija que ningún TextView de res/layout vuelva a llevar
// android:alpha: un alpha mezcla el color con el fondo y tira el ratio abajo,
// que es justo de donde salían los 2,2:1 de la auditoría.
// ============================================================
public class PaletaContrasteTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final String[] TEMAS = { "values", "values-night" };

    // Superficies sobre las que se pinta texto o borde. "TINTADA" es la tarjeta
    // con elevación, calculada a partir de gp_surface y gp_primary.
    private static final String FONDO = "gp_background", TARJETA = "gp_surface", TINTADA = "*tintada";

    @Test
    public void textos_pasan_4_5_a_1_en_los_dos_temas() throws IOException {
        List<String> fallos = new ArrayList<>();
        String[][] pares = {
                { "gp_on_surface", FONDO }, { "gp_on_surface", TARJETA }, { "gp_on_surface", TINTADA },
                { "gp_text_secondary", FONDO }, { "gp_text_secondary", TARJETA }, { "gp_text_secondary", TINTADA },
                { "gp_text_secondary", "gp_surface_2" },
                { "gp_primary", FONDO }, { "gp_primary", TARJETA }, { "gp_primary", TINTADA },
                { "gp_on_primary", "gp_primary" }, { "gp_on_primary_secondary", "gp_primary" },
                { "gp_on_primary_container", "gp_primary_container" },
                { "gp_gold_text", FONDO }, { "gp_gold_text", TARJETA }, { "gp_gold_text", TINTADA },
                { "gp_macro_proteinas", FONDO }, { "gp_macro_proteinas", TARJETA },
                { "gp_macro_carbos", FONDO }, { "gp_macro_carbos", TARJETA },
                { "gp_macro_grasas", FONDO }, { "gp_macro_grasas", TARJETA },
                { "gp_success", "gp_success_container" }, { "gp_success", FONDO }, { "gp_success", TARJETA },
                { "gp_error", "gp_error_container" }, { "gp_error", FONDO }, { "gp_error", TARJETA },
        };
        for (String tema : TEMAS) medir(tema, pares, 4.5, fallos);
        if (!fallos.isEmpty()) fail("Pares por debajo de 4,5:1:\n" + String.join("\n", fallos));
    }

    @Test
    public void bordes_y_graficos_pasan_3_a_1_en_los_dos_temas() throws IOException {
        List<String> fallos = new ArrayList<>();
        String[][] pares = {
                { "gp_stroke", FONDO }, { "gp_stroke", TARJETA }, { "gp_stroke", TINTADA },
                { "gp_gold", FONDO }, { "gp_gold", TARJETA }, { "gp_gold", TINTADA },
        };
        for (String tema : TEMAS) medir(tema, pares, 3.0, fallos);
        if (!fallos.isEmpty()) fail("Pares por debajo de 3:1:\n" + String.join("\n", fallos));
    }

    @Test
    public void ningun_texto_lleva_alpha_en_los_layouts() throws IOException {
        Pattern etiqueta = Pattern.compile("<([\\w.]+)((?:[^>\"]|\"[^\"]*\")*)>");
        Pattern deTexto = Pattern.compile("(TextView|Button|Chip|EditText|CheckBox|RadioButton|Switch)$");
        List<String> fallos = new ArrayList<>();
        try (DirectoryStream<Path> carpetas = Files.newDirectoryStream(RES, "layout*")) {
            for (Path carpeta : carpetas) {
                try (DirectoryStream<Path> xmls = Files.newDirectoryStream(carpeta, "*.xml")) {
                    for (Path xml : xmls) {
                        Matcher m = etiqueta.matcher(leer(xml));
                        while (m.find()) {
                            if (deTexto.matcher(m.group(1)).find() && m.group(2).contains("android:alpha=")) {
                                fallos.add(carpeta.getFileName() + "/" + xml.getFileName() + ": " + m.group(1));
                            }
                        }
                    }
                }
            }
        }
        if (!fallos.isEmpty()) fail("Texto con android:alpha:\n" + String.join("\n", fallos));
    }

    private static void medir(String tema, String[][] pares, double minimo, List<String> fallos) throws IOException {
        String claro = leer(RES.resolve("values/colors.xml"));
        String propio = leer(RES.resolve(tema + "/colors.xml"));
        for (String[] par : pares) {
            String fg = resolver(par[0], propio, claro);
            String bg = TINTADA.equals(par[1])
                    ? mezcla(resolver("gp_primary", propio, claro), resolver("gp_surface", propio, claro), 0.05)
                    : resolver(par[1], propio, claro);
            double r = MedidorNivelTest.contraste(fg, bg);
            if (r < minimo) fallos.add(String.format("%s: %s sobre %s = %.2f", tema, par[0], par[1], r));
        }
    }

    // El color del tema, o el de values/ si el tema no lo redefine (así funciona Android).
    private static String resolver(String nombre, String propio, String claro) {
        String v = buscar(propio, nombre);
        if (v == null) v = buscar(claro, nombre);
        assertTrue("no está " + nombre, v != null);
        return v;
    }

    private static String buscar(String xml, String nombre) {
        Matcher m = Pattern.compile("<color name=\"" + nombre + "\">(#[0-9A-Fa-f]{6})</color>").matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    // Mezcla alfa de fg sobre bg, como la superposición de elevación de Material.
    private static String mezcla(String fg, String bg, double alfa) {
        StringBuilder sb = new StringBuilder("#");
        for (int i = 0; i < 3; i++) {
            int f = Integer.parseInt(fg.substring(1 + 2 * i, 3 + 2 * i), 16);
            int b = Integer.parseInt(bg.substring(1 + 2 * i, 3 + 2 * i), 16);
            sb.append(String.format("%02X", Math.round(alfa * f + (1 - alfa) * b)));
        }
        return sb.toString();
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
