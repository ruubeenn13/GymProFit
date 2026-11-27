package es.pmdm.gymprofit.ui.cristal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
// AmbienteTest — GP-285, lote 1b: el fondo ambiental es suave.
//
// El propietario veía «bandas y bordes de mancha» en el fondo. Medido en los tres emuladores:
//
//   · Cada brillo bajaba en línea recta hasta apagarse al 70 % del radio: la pendiente pasaba
//     de golpe a cero y el ojo ve ahí un canto (y una punta en el centro). Ahora baja con una
//     curva sin esquinas: plana en el centro y plana al apagarse.
//   · Eran tres degradados de la GPU, cada uno con el tramado de su versión de Android: bien en
//     Android 12 o más, y en Android 7 un damero con saltos de hasta 10 niveles entre píxeles
//     vecinos. Ahora el fondo se calcula una vez, a resolución completa, con un tramado propio
//     de menos de un nivel, igual en todas las versiones (Ambiente.pintar).
//
// Lo que se comprueba se compara con la cuenta exacta (Ambiente.mezclaEn), no con cifras
// escritas a mano, y con los colores de colors.xml de los dos temas.
// ============================================================
public class AmbienteTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path JAVA = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit");
    // La pantalla de los emuladores, en px: el fondo se pinta a resolución completa.
    private static final int ANCHO = 1080, ALTO = 2400;
    // Lado de la ventana en la que se promedia para ver si el tramado recupera los decimales.
    private static final int VENTANA = 16;

    // Sin canto: junto al centro y junto al borde la curva es plana. Se compara con la recta
    // de antes, que a un 1 % del borde aún tenía un 1 % del alfa del centro.
    @Test
    public void un_brillo_no_tiene_canto_ni_punta() {
        Ambiente.Brillo b = new Ambiente.Brillo(0.5f, 0.5f, 1f, 1f, 0xFFFF0000);
        float lado = 1000f, fin = Ambiente.FIN * lado, paso = fin / 100f;
        float centro = Ambiente.alfaEn(b, 500, 500, lado, lado);
        float casiCentro = Ambiente.alfaEn(b, 500 + paso, 500, lado, lado);
        float casiBorde = Ambiente.alfaEn(b, 500 + fin - paso, 500, lado, lado);
        float recta = centro / 100f;
        assertTrue("junto al borde tiene que quedar muy por debajo de la recta: " + casiBorde + " frente a " + recta,
                casiBorde < recta / 10f);
        assertTrue("junto al centro tiene que caer mucho menos que la recta: " + (centro - casiCentro),
                centro - casiCentro < recta / 10f);
        // Y nunca sube al alejarse.
        float antes = centro;
        for (float d = 0; d <= fin + paso; d += paso) {
            float a = Ambiente.alfaEn(b, 500 + d, 500, lado, lado);
            assertTrue("el brillo sube a " + d + " del centro", a <= antes + 1e-6f);
            antes = a;
        }
    }

    // Lo pintado es lo que se mide: cada píxel, a un nivel como mucho de la cuenta exacta (lo
    // que puede mover el tramado) más la centésima que deja interpolar entre puntos exactos.
    @Test
    public void lo_pintado_es_lo_que_mide_el_contraste() throws IOException {
        for (String tema : new String[]{ "values-night", "values" }) {
            int base = color(tema, "gp_ambiente_base");
            Ambiente.Brillo[] brillos = brillos(tema);
            int[] px = pintar(base, brillos);
            float[] exacto = new float[3];
            for (int y = 0; y < ALTO; y++) {
                for (int x = 0; x < ANCHO; x++) {
                    Ambiente.mezclaEn(x, y, ANCHO, ALTO, base, brillos, exacto);
                    int p = px[y * ANCHO + x];
                    assertEquals("opaco", 0xFF, p >>> 24);
                    for (int c = 0; c < 3; c++) {
                        float d = Math.abs(((p >> (16 - 8 * c)) & 0xFF) - exacto[c]);
                        assertTrue(tema + " (" + x + "," + y + ") canal " + c + " a " + d + " niveles", d < 1.01f);
                    }
                }
            }
        }
    }

    // El tramado: la media de cada ventana de píxeles pintados recupera los decimales que un
    // píxel de 8 bits no puede llevar. Redondear sin más deja ventanas enteras a casi medio
    // nivel de su valor (eso es una banda); con tramado se quedan a menos de la mitad de eso.
    @Test
    public void el_fondo_se_pinta_con_tramado() throws IOException {
        for (String tema : new String[]{ "values-night", "values" }) {
            int base = color(tema, "gp_ambiente_base");
            Ambiente.Brillo[] brillos = brillos(tema);
            int[] pintado = pintar(base, brillos);
            int[] redondeado = new int[ANCHO * ALTO];
            for (int y = 0; y < ALTO; y++) {
                for (int x = 0; x < ANCHO; x++) {
                    redondeado[y * ANCHO + x] = Ambiente.colorEn(x, y, ANCHO, ALTO, base, brillos);
                }
            }
            float sinTramado = peorVentana(redondeado, base, brillos);
            float conTramado = peorVentana(pintado, base, brillos);
            assertTrue(tema + ": la peor ventana se queda a " + conTramado + " niveles; sin tramado, a " + sinTramado,
                    conTramado < sinTramado / 2f);
        }
    }

    // El fondo ya no son degradados de la GPU: cada versión de Android los trama a su manera.
    @Test
    public void el_fondo_no_usa_degradados_de_la_gpu() throws IOException {
        String fondo = new String(Files.readAllBytes(JAVA.resolve("ui/cristal/FondoAmbiental.java")), StandardCharsets.UTF_8);
        assertFalse(fondo.contains("RadialGradient"));
        assertFalse(fondo.contains("LinearGradient"));
        assertTrue(fondo.contains("Ambiente.pintar("));
    }

    // ── Ayudas ──────────────────────────────────────────────────────────────

    private static int[] pintar(int base, Ambiente.Brillo[] brillos) {
        int[] px = new int[ANCHO * ALTO];
        Ambiente.pintar(px, ANCHO, ALTO, 0, ALTO, base, brillos);
        return px;
    }

    // La mayor distancia, en niveles, entre la media de una ventana pintada y la de la cuenta exacta.
    private static float peorVentana(int[] px, int base, Ambiente.Brillo[] brillos) {
        float peor = 0f;
        float[] exacto = new float[3];
        for (int y0 = 0; y0 + VENTANA <= ALTO; y0 += VENTANA) {
            for (int x0 = 0; x0 + VENTANA <= ANCHO; x0 += VENTANA) {
                double[] suma = new double[3], ideal = new double[3];
                for (int y = y0; y < y0 + VENTANA; y++) {
                    for (int x = x0; x < x0 + VENTANA; x++) {
                        Ambiente.mezclaEn(x, y, ANCHO, ALTO, base, brillos, exacto);
                        int p = px[y * ANCHO + x];
                        for (int c = 0; c < 3; c++) {
                            suma[c] += (p >> (16 - 8 * c)) & 0xFF;
                            ideal[c] += exacto[c];
                        }
                    }
                }
                for (int c = 0; c < 3; c++) {
                    peor = Math.max(peor, (float) Math.abs(suma[c] - ideal[c]) / (VENTANA * VENTANA));
                }
            }
        }
        return peor;
    }

    private static Ambiente.Brillo[] brillos(String tema) throws IOException {
        return Ambiente.inicio(color(tema, "gp_ambiente_brillo_1"), color(tema, "gp_ambiente_brillo_2"),
                color(tema, "gp_ambiente_brillo_3"), color(tema, "gp_ambiente_brillo_4"));
    }

    // Un color del tema: el de su carpeta o, si no lo redefine, el de values.
    private static int color(String tema, String token) throws IOException {
        for (String carpeta : new String[]{ tema, "values" }) {
            String xml = new String(Files.readAllBytes(RES.resolve(carpeta).resolve("colors.xml")), StandardCharsets.UTF_8)
                    .replaceAll("(?s)<!--.*?-->", "");
            Matcher m = Pattern.compile("<color name=\"" + token + "\">#([0-9A-Fa-f]+)</color>").matcher(xml);
            if (m.find()) {
                long n = Long.parseLong(m.group(1), 16);
                return (int) (m.group(1).length() == 6 ? 0xFF000000L | n : n);
            }
        }
        throw new AssertionError("no existe el color " + token);
    }
}
