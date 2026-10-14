package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.model.alimento.Alimento;

// ============================================================
// CuentaPlatoTest — «Por ración» es lo que se apuntará (lote 1.6.6)
// Lee la MISMA tabla que la API (api/gymprofit-api/src/test/resources/platos/
// cuenta_plato_casos.csv, PlatosCuentaTest): si una cuenta se separa de la otra, falla.
// Gradle ejecuta los tests desde el módulo (app/GymProFit/app), y el CI tiene el
// repositorio entero.
// ============================================================
public class CuentaPlatoTest {

    private static final File CASOS = new File("../../../api/gymprofit-api/src/test/resources/platos/cuenta_plato_casos.csv");

    @Test
    public void la_tabla_de_la_api_sale_igual() throws Exception {
        assertTrue("No encuentro la tabla compartida: " + CASOS.getAbsolutePath(), CASOS.isFile());
        int casos = 0;
        for (String linea : Files.readAllLines(CASOS.toPath(), StandardCharsets.UTF_8)) {
            if (linea.trim().isEmpty() || linea.startsWith("#")) continue;
            String[] c = linea.split(";");
            List<Alimento> alimentos = new ArrayList<>();
            List<Double> gramos = new ArrayList<>();
            for (String ing : c[1].split("\\|")) {
                String[] partes = ing.split("@");
                String[] v = partes[0].split("/");
                Alimento a = new Alimento();
                a.setCalorias(Integer.parseInt(v[0]));
                a.setProteinas(Double.parseDouble(v[1]));
                a.setCarbohidratos(Double.parseDouble(v[2]));
                a.setGrasas(Double.parseDouble(v[3]));
                alimentos.add(a);
                gramos.add(Double.parseDouble(partes[1]));
            }
            int raciones = Integer.parseInt(c[2]);
            Double peso = "-".equals(c[3]) ? null : Double.parseDouble(c[3]);
            CuentaPlato.Cifras total = CuentaPlato.total(alimentos, gramos);
            CuentaPlato.Cifras g = CuentaPlato.guardado(total, raciones, peso);
            String caso = c[0];
            assertEquals(caso + " kcal guardadas", Integer.parseInt(c[4]), g.kcal);
            iguales(caso + " proteínas guardadas", c[5], g.proteinas);
            iguales(caso + " carbohidratos guardados", c[6], g.carbohidratos);
            iguales(caso + " grasas guardadas", c[7], g.grasas);
            if (peso != null) assertEquals(caso + " gramos", Double.parseDouble(c[8]), CuentaPlato.gramosRacion(peso, raciones), 0);
            CuentaPlato.Cifras r = CuentaPlato.porRacion(total, raciones, peso);
            assertEquals(caso + " kcal de 1 ración", Integer.parseInt(c[9]), r.kcal);
            iguales(caso + " proteínas de 1 ración", c[10], r.proteinas);
            iguales(caso + " carbohidratos de 1 ración", c[11], r.carbohidratos);
            iguales(caso + " grasas de 1 ración", c[12], r.grasas);
            casos++;
        }
        assertTrue(casos >= 5);
    }

    @Test
    public void el_tablero_10_sin_peso_y_con_peso() throws Exception {
        // Lo que tiene que decir la pantalla: 474 kcal y 25 g; con 1.800 g, 105 por 100 g y 473.
        List<Alimento> a = new ArrayList<>();
        List<Double> g = new ArrayList<>();
        int[][] kcal = {{338, 300}, {461, 80}, {77, 300}, {41, 150}, {40, 100}, {900, 20}};
        double[] prot = {24, 24, 2, 1, 1.3, 0};
        for (int i = 0; i < kcal.length; i++) {
            Alimento x = new Alimento();
            x.setCalorias(kcal[i][0]);
            x.setProteinas(prot[i]);
            a.add(x);
            g.add((double) kcal[i][1]);
        }
        CuentaPlato.Cifras total = CuentaPlato.total(a, g);
        assertEquals(1896, total.kcal);
        assertEquals(474, CuentaPlato.porRacion(total, 4, null).kcal);
        assertEquals(25, CuentaPlato.porRacion(total, 4, null).proteinas.intValue());
        assertEquals(105, CuentaPlato.guardado(total, 4, 1800.0).kcal);
        assertEquals(473, CuentaPlato.porRacion(total, 4, 1800.0).kcal);
    }

    @Test
    public void el_peso_que_no_vale() {
        assertTrue(CuentaPlato.pesoMalo(3, 4));
        assertFalse(CuentaPlato.pesoMalo(4, 4));
        assertTrue(CuentaPlato.pesoMalo(50001, 1));
        Alimento aceite = new Alimento();
        aceite.setCalorias(900);
        CuentaPlato.Cifras total = CuentaPlato.total(List.of(aceite), List.of(100.0));
        assertFalse(CuentaPlato.demasiadasKcal(total, 1, 100));
        assertTrue(CuentaPlato.demasiadasKcal(total, 1, 99));
    }

    private static void iguales(String que, String esperado, BigDecimal real) {
        assertEquals(que, 0, new BigDecimal(esperado).compareTo(real));
    }
}
