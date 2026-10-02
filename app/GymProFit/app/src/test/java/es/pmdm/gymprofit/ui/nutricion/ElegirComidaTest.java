package es.pmdm.gymprofit.ui.nutricion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.ComidaQueToca;

// ============================================================
// ElegirComidaTest — la etiqueta de la comida y la hoja «¿A qué comida?» (decisión 15,
// lote 1.6.2). Los iconos son los del README del diseño (con free_breakfast, que en
// Material Symbols es el mismo glifo que local_cafe). Cada fila dice lo que lleva o «Sin
// apuntar», y «ahora toca» solo en la que toca y solo si el día es hoy.
// ============================================================
public class ElegirComidaTest {

    private static final java.util.Map<String, String> ALIAS = new HashMap<>();

    static {
        // Mismo código de glifo en la fuente (eb44); el paquete de SVG solo trae el segundo.
        ALIAS.put("free_breakfast", "local_cafe");
    }

    @Test
    public void los_iconos_son_los_del_diseno() throws Exception {
        String md = new String(Files.readAllBytes(Paths.get("..", "..", "..", "documentacion", "diseno",
                "2026-10-02-nutricion", "README.md")), StandardCharsets.UTF_8);
        String linea = md.substring(md.indexOf("Iconos de las comidas"));
        linea = linea.substring(0, linea.indexOf('\n'));
        String[] nombres = {"desayuno", "almuerzo", "comida", "merienda", "cena"};
        for (int i = 0; i < nombres.length; i++) {
            Matcher m = Pattern.compile(nombres[i] + " `([a-z_]+)`").matcher(linea);
            assertTrue("el README no da el icono de " + nombres[i], m.find());
            String simbolo = ALIAS.getOrDefault(m.group(1), m.group(1));
            int esperado = R.drawable.class.getField("ic_ms_" + simbolo).getInt(null);
            assertEquals(nombres[i], esperado, ElegirComida.icono(ComidaQueToca.TIPOS[i]));
        }
    }

    @Test
    public void lo_que_lleva_o_sin_apuntar() {
        Map<String, Integer> kcal = new HashMap<>();
        kcal.put("DESAYUNO", 450);
        kcal.put("CENA", 0);
        assertEquals(Integer.valueOf(450), ElegirComida.kcal(kcal, "DESAYUNO"));
        // Una comida a cero es una comida sin apuntar.
        assertNull(ElegirComida.kcal(kcal, "CENA"));
        assertNull(ElegirComida.kcal(kcal, "MERIENDA"));
        // Sin los datos del día (cargando o fallo): no se dice nada de ninguna.
        assertNull(ElegirComida.kcal(null, "DESAYUNO"));
    }

    @Test
    public void ahora_toca_solo_hoy_y_solo_la_que_toca() {
        assertTrue(ElegirComida.tocaAhora("MERIENDA", true, "MERIENDA"));
        assertFalse(ElegirComida.tocaAhora("MERIENDA", false, "MERIENDA"));
        assertFalse(ElegirComida.tocaAhora("CENA", true, "MERIENDA"));
    }

    @Test
    public void el_aviso_al_volver_dice_a_cual_se_anadio() {
        assertEquals(R.string.anadido_a_desayuno, ElegirComida.anadidoA("DESAYUNO"));
        assertEquals(R.string.anadido_a_cena, ElegirComida.anadidoA("CENA"));
        // Si es la misma de la que se vino, no hay aviso.
        assertNull(ElegirComida.avisoAlVolver("MERIENDA", "MERIENDA"));
        assertEquals(Integer.valueOf(R.string.anadido_a_cena), ElegirComida.avisoAlVolver("MERIENDA", "CENA"));
    }
}
