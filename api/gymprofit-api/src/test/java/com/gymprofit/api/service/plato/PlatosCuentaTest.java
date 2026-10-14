package com.gymprofit.api.service.plato;

import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.service.alimentocomida.ValoresLinea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// PlatosCuentaTest — la cuenta de un plato, con la tabla que comparte con la app (1.6.6)
// Cada caso de platos/cuenta_plato_casos.csv: la suma de sus ingredientes tal como se
// apuntarían, lo que se guarda en el alimento del plato y lo que vale «1 ración». La app
// (CuentaPlatoTest) lee el mismo fichero: lo que dice «Por ración» es lo que se apunta.
// ============================================================
@DisplayName("Lote 1.6.6 — la cuenta de un plato, igual que en la app")
class PlatosCuentaTest {

    record Caso(String nombre, List<Alimento> alimentos, List<BigDecimal> gramos, int raciones, BigDecimal peso,
                String[] esperado) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    static List<Caso> casos() throws IOException {
        List<Caso> casos = new ArrayList<>();
        try (InputStream in = PlatosCuentaTest.class.getResourceAsStream("/platos/cuenta_plato_casos.csv")) {
            for (String linea : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (linea.isBlank() || linea.startsWith("#")) continue;
                String[] c = linea.split(";");
                List<Alimento> alimentos = new ArrayList<>();
                List<BigDecimal> gramos = new ArrayList<>();
                for (String ing : c[1].split("\\|")) {
                    String[] partes = ing.split("@");
                    String[] v = partes[0].split("/");
                    Alimento a = new Alimento();
                    a.setCalorias(Integer.parseInt(v[0]));
                    a.setProteinas(new BigDecimal(v[1]));
                    a.setCarbohidratos(new BigDecimal(v[2]));
                    a.setGrasas(new BigDecimal(v[3]));
                    a.setFibra("-".equals(v[4]) ? null : new BigDecimal(v[4]));
                    alimentos.add(a);
                    gramos.add(new BigDecimal(partes[1]));
                }
                casos.add(new Caso(c[0], alimentos, gramos, Integer.parseInt(c[2]),
                        "-".equals(c[3]) ? null : new BigDecimal(c[3]), java.util.Arrays.copyOfRange(c, 4, 13)));
            }
        }
        assertThat(casos).hasSizeGreaterThanOrEqualTo(5);
        return casos;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casos")
    @DisplayName("lo guardado y «1 ración»")
    void cuenta(Caso caso) {
        ValoresLinea total = ValoresLinea.CERO;
        for (int i = 0; i < caso.alimentos().size(); i++) {
            total = total.mas(ValoresLinea.de(caso.alimentos().get(i), caso.gramos().get(i)));
        }
        Platos.Valores v = Platos.aGuardar(total, caso.raciones(), caso.peso());
        String[] e = caso.esperado();
        assertThat(v.kcal()).as("kcal guardadas").isEqualTo(Integer.parseInt(e[0]));
        assertThat(v.proteinas()).as("proteínas guardadas").isEqualByComparingTo(e[1]);
        assertThat(v.carbohidratos()).as("carbohidratos guardados").isEqualByComparingTo(e[2]);
        assertThat(v.grasas()).as("grasas guardadas").isEqualByComparingTo(e[3]);

        // «1 ración»: la cuenta de cualquier alimento, con los valores guardados y los gramos
        // de su ración (100 por dentro sin peso hecho).
        Alimento plato = new Alimento();
        plato.setCalorias(v.kcal());
        plato.setProteinas(v.proteinas());
        plato.setCarbohidratos(v.carbohidratos());
        plato.setGrasas(v.grasas());
        BigDecimal gramos = caso.peso() == null ? Platos.GRAMOS_SIN_PESO : Platos.gramosRacion(caso.peso(), caso.raciones());
        if (caso.peso() != null) assertThat(gramos).as("gramos de la ración").isEqualByComparingTo(e[4]);
        else assertThat(e[4]).isEqualTo("-");
        ValoresLinea racion = ValoresLinea.de(plato, gramos);
        assertThat(racion.kcal()).as("kcal de 1 ración").isEqualTo(Integer.parseInt(e[5]));
        assertThat(racion.proteinas()).as("proteínas de 1 ración").isEqualByComparingTo(e[6]);
        assertThat(racion.carbohidratos()).as("carbohidratos de 1 ración").isEqualByComparingTo(e[7]);
        assertThat(racion.grasas()).as("grasas de 1 ración").isEqualByComparingTo(e[8]);
    }
}
