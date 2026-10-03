package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// ReducirFotoTest — la cuenta de la foto de perfil antes de subirla (GP-188, lote 1.6.5)
// Cuánto reducir al decodificar, cuánto girar según el EXIF y qué cuadrado recortar.
// ============================================================
public class ReducirFotoTest {

    @Test
    public void reduce_en_potencias_de_dos_sin_bajar_del_lado() {
        // Una foto de 4000 × 3000: el lado corto, 3000; /4 da 750 (≥ 512), /8 daría 375.
        assertEquals(4, ReducirFoto.muestreo(4000, 3000, 512));
        // 12 Mpx de móvil en vertical.
        assertEquals(4, ReducirFoto.muestreo(3024, 4032, 512));
        // 1024 justos: /2 da 512.
        assertEquals(2, ReducirFoto.muestreo(1024, 2000, 512));
        // Más pequeña que el lado: entera.
        assertEquals(1, ReducirFoto.muestreo(400, 300, 512));
        assertEquals(1, ReducirFoto.muestreo(0, 0, 512));
    }

    @Test
    public void el_exif_dice_cuanto_girar_y_si_hay_espejo() {
        assertEquals(0, ReducirFoto.giro(ReducirFoto.EXIF_NORMAL).grados);
        assertEquals(0, ReducirFoto.giro(0).grados);
        assertEquals(90, ReducirFoto.giro(ReducirFoto.EXIF_GIRA_90).grados);
        assertEquals(180, ReducirFoto.giro(ReducirFoto.EXIF_GIRA_180).grados);
        assertEquals(270, ReducirFoto.giro(ReducirFoto.EXIF_GIRA_270).grados);
        assertFalse(ReducirFoto.giro(ReducirFoto.EXIF_GIRA_90).espejo);
        assertTrue(ReducirFoto.giro(ReducirFoto.EXIF_ESPEJO_HORIZONTAL).espejo);
        assertEquals(0, ReducirFoto.giro(ReducirFoto.EXIF_ESPEJO_HORIZONTAL).grados);
        assertEquals(180, ReducirFoto.giro(ReducirFoto.EXIF_ESPEJO_VERTICAL).grados);
        assertTrue(ReducirFoto.giro(ReducirFoto.EXIF_ESPEJO_VERTICAL).espejo);
        assertEquals(90, ReducirFoto.giro(ReducirFoto.EXIF_TRANSPONER).grados);
        assertTrue(ReducirFoto.giro(ReducirFoto.EXIF_TRANSPONER).espejo);
        assertEquals(270, ReducirFoto.giro(ReducirFoto.EXIF_TRANSVERSAL).grados);
        assertTrue(ReducirFoto.giro(ReducirFoto.EXIF_TRANSVERSAL).espejo);
    }

    @Test
    public void girar_90_cambia_ancho_por_alto_antes_de_recortar() {
        assertTrue(ReducirFoto.giro(ReducirFoto.EXIF_GIRA_90).cambiaLados());
        assertTrue(ReducirFoto.giro(ReducirFoto.EXIF_TRANSVERSAL).cambiaLados());
        assertFalse(ReducirFoto.giro(ReducirFoto.EXIF_GIRA_180).cambiaLados());
    }

    @Test
    public void recorta_el_cuadrado_central() {
        // Apaisada 1000 × 750: un cuadrado de 750 con 125 a cada lado.
        int[] r = ReducirFoto.recorte(1000, 750);
        assertEquals(125, r[0]);
        assertEquals(0, r[1]);
        assertEquals(750, r[2]);
        // Vertical 750 × 1000: arriba y abajo.
        r = ReducirFoto.recorte(750, 1000);
        assertEquals(0, r[0]);
        assertEquals(125, r[1]);
        assertEquals(750, r[2]);
        // Cuadrada: entera.
        r = ReducirFoto.recorte(600, 600);
        assertEquals(0, r[0]);
        assertEquals(0, r[1]);
        assertEquals(600, r[2]);
        // Impar: el sobrante redondea hacia arriba-izquierda.
        r = ReducirFoto.recorte(1001, 750);
        assertEquals(125, r[0]);
    }

    @Test
    public void lo_que_sube_es_512_en_jpeg_85() {
        assertEquals(512, ReducirFoto.LADO);
        assertEquals(85, ReducirFoto.CALIDAD);
    }
}
