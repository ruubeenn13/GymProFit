package com.gymprofit.api.service.busqueda;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// UnidadesRacionTest — la unidad de cada ración, en singular y en plural (GP-172)
// Recorre datos/basicos/raciones.csv: a todo nombre que empieza por «1 » le tienen que
// salir las cuatro formas (singular y plural, en español y en inglés), y a los que no
// empiezan por «1 » ninguna. Y las de RacionesProducto y las de una ración propia.
// ============================================================
class UnidadesRacionTest {

    @Test
    @DisplayName("todo nombre de raciones.csv que empieza por «1 » tiene su unidad en los dos idiomas")
    void raciones_csv_completo() throws IOException {
        List<String> faltan = new ArrayList<>();
        List<String> sobran = new ArrayList<>();
        List<String> lineas = Files.readAllLines(Path.of("datos", "basicos", "raciones.csv"), StandardCharsets.UTF_8);
        for (String linea : lineas.subList(1, lineas.size())) {
            String[] c = linea.split(";");
            comprobar(c[2], false, faltan, sobran);
            comprobar(c[3], true, faltan, sobran);
        }
        assertThat(faltan).as("sin unidad").isEmpty();
        assertThat(sobran).as("con unidad sin empezar por «1 »").isEmpty();
    }

    private static void comprobar(String nombre, boolean ingles, List<String> faltan, List<String> sobran) {
        Optional<UnidadesRacion.Unidad> u = UnidadesRacion.de(nombre, ingles);
        if (nombre.startsWith("1 ")) {
            if (u.isEmpty() || u.get().singular().isBlank() || u.get().plural().isBlank()) {
                faltan.add((ingles ? "en: " : "es: ") + nombre);
            }
        } else if (u.isPresent()) {
            sobran.add((ingles ? "en: " : "es: ") + nombre);
        }
    }

    @Test
    @DisplayName("singular y plural, sin el «1» y sin paréntesis")
    void formas() {
        assertThat(UnidadesRacion.de("1 rebanada", false)).contains(new UnidadesRacion.Unidad("rebanada", "rebanadas"));
        assertThat(UnidadesRacion.de("1 slice", true)).contains(new UnidadesRacion.Unidad("slice", "slices"));
        assertThat(UnidadesRacion.de("1 vaso (250 ml)", false))
                .contains(new UnidadesRacion.Unidad("vaso de 250 ml", "vasos de 250 ml"));
        assertThat(UnidadesRacion.de("1 flan", false).orElseThrow().plural()).isEqualTo("flanes");
        assertThat(UnidadesRacion.de("1 flan", true).orElseThrow().plural()).isEqualTo("flans");
    }

    @Test
    @DisplayName("las de RacionesProducto y las de una ración propia también")
    void productos_y_propias() {
        for (String es : new String[]{"1 envase", "1 unidad", "1 ración", "1 rebanada"}) {
            assertThat(UnidadesRacion.de(es, false)).as(es).isPresent();
        }
        for (String en : new String[]{"1 pack", "1 unit", "1 serving", "1 slice"}) {
            assertThat(UnidadesRacion.de(en, true)).as(en).isPresent();
        }
    }

    @Test
    @DisplayName("sin «1 » delante, o un nombre que no conoce, no hay unidad")
    void sin_unidad() {
        assertThat(UnidadesRacion.de("Media taza", false)).isEmpty();
        assertThat(UnidadesRacion.de("5 nuggets", true)).isEmpty();
        assertThat(UnidadesRacion.de("1 cosa rara", false)).isEmpty();
        assertThat(UnidadesRacion.de(null, false)).isEmpty();
    }
}
