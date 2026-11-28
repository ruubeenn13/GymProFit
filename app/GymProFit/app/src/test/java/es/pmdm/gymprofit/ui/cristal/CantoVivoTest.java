package es.pmdm.gymprofit.ui.cristal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// CantoVivoTest — el velo que desvanece la lista junto al canto de un cristal vivo (GP-285,
// remate del lote 2; DEC-070): lo que pasa por detrás se desvanece ANTES del canto, nunca
// queda nítido y cortado por él.
//
// Las cuentas de CantoVivo y el alfa de cada fila (BordesSistema.alfas), con números escritos,
// y las medidas de dimens.xml que las alimentan. El caso es el de la captura de la revisión
// (API 36, claro, letra 1,0): el resumen acaba en la y 480 px, con radio 24 dp (63 px a
// densidad 2,625).
// ============================================================
public class CantoVivoTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final int CANTO = 480, RADIO = 63, MARGEN = 5, RAMPA = 37;

    // El alfa del velo en una y de la pantalla. La fila 0 de alfas() es la punta de la rampa:
    // bajo un cristal va abajo; sobre él, arriba.
    private static int alfa(CantoVivo.Velo v, boolean macizoArriba, int y) {
        int[] alfas = BordesSistema.alfas(v.alto, v.macizo, v.entrada);
        int fila = y - v.arriba;
        return alfas[macizoArriba ? v.alto - 1 - fila : fila];
    }

    @Test
    public void bajo_el_resumen_el_velo_es_macizo_en_el_canto_y_se_desvanece_hacia_abajo() {
        CantoVivo.Velo v = CantoVivo.bajo(CANTO, RADIO, MARGEN, RAMPA);
        // Empieza un radio por dentro del cristal y acaba al final de la rampa.
        assertEquals(CANTO - RADIO, v.arriba);
        assertEquals(CANTO + MARGEN + RAMPA, v.arriba + v.alto);
        // Opaco en el canto del cristal y en todo el margen de fuera: el canto no corta nada a
        // medio desvanecer. Y desde un cuarto de radio por dentro: tapa los picos de las esquinas.
        for (int y = CANTO - RADIO / 4; y < CANTO + MARGEN; y++) {
            assertEquals("opaco en la y " + y, 255, alfa(v, true, y));
        }
        // Hacia la lista, la rampa: transparente al final y, a media rampa, medio velo. Con el
        // cuadrado de los bordes del sistema ahí quedaba un cuarto, y en una rampa tan corta el
        // texto llegaba al canto todavía a medio ver.
        assertEquals("transparente al final de la rampa", 0, alfa(v, true, CANTO + MARGEN + RAMPA - 1));
        int media = alfa(v, true, CANTO + MARGEN + RAMPA / 2);
        assertTrue("a media rampa, medio velo: " + media, media >= 118 && media <= 140);
        // A un tercio de rampa del canto ya tapa más de dos tercios.
        assertTrue("cerca del canto tapa más de dos tercios", alfa(v, true, CANTO + MARGEN + RAMPA / 3) >= 170);
        int antes = 255;
        for (int y = CANTO + MARGEN; y < CANTO + MARGEN + RAMPA; y++) {
            int a = alfa(v, true, y);
            assertTrue("baja sin escalones en la y " + y + ": " + antes + " → " + a, a <= antes && antes - a <= 12);
            antes = a;
        }
    }

    // Por dentro del cristal el velo no acaba en seco: en los márgenes de los lados, donde no hay
    // cristal que lo tape, su canto cortaba en una raya la sombra de la tarjeta de debajo.
    @Test
    public void por_dentro_del_cristal_el_velo_tambien_entra_poco_a_poco() {
        CantoVivo.Velo v = CantoVivo.bajo(CANTO, RADIO, MARGEN, RAMPA);
        assertEquals("tres cuartos de radio de entrada", 47, v.entrada);
        assertEquals("transparente en su canto de dentro", 0, alfa(v, true, v.arriba));
        int antes = 0;
        for (int y = v.arriba; y <= CANTO - RADIO / 4; y++) {
            int a = alfa(v, true, y);
            assertTrue("sube sin saltos en la y " + y + ": " + antes + " → " + a, a >= antes && a - antes <= 12);
            antes = a;
        }
        assertEquals(255, antes);
    }

    @Test
    public void sobre_el_descanso_el_velo_es_macizo_en_el_canto_y_se_desvanece_hacia_arriba() {
        int canto = 1800, radio = 79;
        CantoVivo.Velo v = CantoVivo.sobre(canto, radio, MARGEN, RAMPA);
        assertEquals(canto - MARGEN - RAMPA, v.arriba);
        assertEquals("acaba un radio por dentro del panel", canto + radio, v.arriba + v.alto);
        for (int y = canto - MARGEN; y <= canto + radio / 4; y++) {
            assertEquals("opaco en la y " + y, 255, alfa(v, false, y));
        }
        assertEquals("transparente arriba, hacia la lista", 0, alfa(v, false, v.arriba));
        assertEquals("y en su canto de dentro", 0, alfa(v, false, v.arriba + v.alto - 1));
    }

    // Lo macizo llega a un cuarto de radio por dentro del canto. A esa altura la esquina redonda
    // del cristal se mete un tercio de radio (8 dp con radio 24): es lo más ancho que queda sin
    // tapar del todo a cada lado, y ahí, de la tarjeta de debajo, solo están su canto y su
    // relleno de 16 dp, nunca texto.
    @Test
    public void un_cuarto_de_radio_macizo_basta_para_los_picos_de_las_esquinas() {
        float radio = 24f, altura = radio / 4f;
        float y = radio - altura;
        float pico = radio - (float) Math.sqrt(radio * radio - y * y);
        assertTrue("el pico a un cuarto de radio mide " + pico + " dp", pico < 8.2f);
        CantoVivo.Velo v = CantoVivo.bajo(1000, 64f, 0, 10);
        assertEquals("macizo desde un cuarto de radio por dentro", 16, v.macizo - v.entrada);
    }

    // En reposo el velo no toca la lista: la lista deja libre, además de lo que ya la separa
    // del cristal, lo que le falte a eso para llegar al margen y la rampa del velo.
    @Test
    public void en_reposo_el_velo_no_toca_la_primera_tarjeta() throws Exception {
        assertEquals("lo que falta", 21, CantoVivo.sitio(63, 42));
        assertEquals("si ya cabe, nada", 0, CantoVivo.sitio(40, 42));
        String dimens = new String(Files.readAllBytes(RES.resolve("values").resolve("dimens.xml")), StandardCharsets.UTF_8);
        int margen = dp(dimens, "envivo_velo_margen"), rampa = dp(dimens, "envivo_velo_rampa");
        assertTrue("hay margen", margen > 0);
        assertTrue("la rampa da para una línea de texto y media: " + rampa + " dp", rampa >= 20);
        // Lo que separa el resumen de la primera tarjeta es el margen de arriba de la cabecera
        // de un ejercicio, y la pantalla lo cuenta con ese mismo dimen.
        String item = new String(Files.readAllBytes(RES.resolve("layout").resolve("item_envivo_ejercicio.xml")),
                StandardCharsets.UTF_8);
        assertTrue(item.contains("android:layout_marginTop=\"@dimen/envivo_tarjeta_separacion\""));
        int separacion = dp(dimens, "envivo_tarjeta_separacion");
        assertEquals("8 dp más de aire bajo el resumen", 8, CantoVivo.sitio(CantoVivo.hueco(margen, rampa), separacion));
        String codigo = new String(Files.readAllBytes(Paths.get("src", "main", "java", "es", "pmdm", "gymprofit", "ui",
                "activities", "SesionEnVivoActivity.java")), StandardCharsets.UTF_8);
        assertTrue("arriba, la lista deja sitio al velo",
                codigo.contains("cabecera.getHeight() + CantoVivo.sitio(huecoVelo, dimen(R.dimen.envivo_tarjeta_separacion))"));
        assertTrue("y abajo, con el descanso", codigo.contains("CantoVivo.sitio(huecoVelo, pieza.getTop())"));
    }

    private static int dp(String dimens, String nombre) {
        Matcher m = Pattern.compile("<dimen name=\"" + nombre + "\">(\\d+)dp</dimen>").matcher(dimens);
        if (!m.find()) throw new AssertionError("no existe el dimen " + nombre);
        return Integer.parseInt(m.group(1));
    }
}
