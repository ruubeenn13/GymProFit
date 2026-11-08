package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

// ============================================================
// DuracionRutinaTest — la duración de una rutina en la app, la misma que en la API (GP-244, punto 9)
//
// Lee la MISMA tabla que ReglasProgramaTest en la API
// (api/gymprofit-api/src/test/resources/programas/duracion_rutina_casos.csv): si la copia de
// la app se separa de ReglasPrograma.centesimas, o del redondeo a 5 minutos, falla. Gradle
// ejecuta los tests desde el módulo (app/GymProFit/app) y el CI tiene el repositorio entero.
// ============================================================
public class DuracionRutinaTest {

    private static final File CASOS =
            new File("../../../api/gymprofit-api/src/test/resources/programas/duracion_rutina_casos.csv");

    @Test
    public void la_tabla_de_la_api_sale_igual() throws Exception {
        assertTrue("No encuentro la tabla compartida: " + CASOS.getAbsolutePath(), CASOS.isFile());
        int casos = 0;
        for (String linea : Files.readAllLines(CASOS.toPath(), StandardCharsets.UTF_8)) {
            if (linea.trim().isEmpty() || linea.startsWith("#")) continue;
            String[] c = linea.trim().split(";");
            List<DuracionRutina.Pieza> r = new ArrayList<>();
            if (!"-".equals(c[1])) {
                for (String e : c[1].split("\\|")) {
                    String[] v = e.split("/");
                    r.add(new DuracionRutina.Pieza(Integer.parseInt(v[0]), Integer.parseInt(v[1]),
                            Integer.parseInt(v[2]), "S".equals(v[3]), !"-".equals(v[4]), Integer.parseInt(v[5])));
                }
            }
            long cs = DuracionRutina.centesimas(r);
            assertEquals(c[0], Long.parseLong(c[2]), cs);
            assertEquals(c[0], Integer.parseInt(c[3]), DuracionRutina.redondear(cs));
            casos++;
        }
        assertTrue(casos >= 8);
    }

    @Test
    public void cuatro_ejercicios_por_defecto_son_los_35_minutos_del_tablero() {
        List<DuracionRutina.Pieza> r = new ArrayList<>();
        for (int i = 0; i < 4; i++) r.add(DuracionRutina.Pieza.porDefecto());
        assertEquals(35, DuracionRutina.minutos(r));
    }
}
