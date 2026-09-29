package com.gymprofit.api.service.programa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.enums.MedidaSerie;
import com.gymprofit.api.enums.Nivel;
import com.gymprofit.api.enums.NivelExperiencia;
import com.gymprofit.api.enums.PorLado;
import com.gymprofit.api.enums.TipoEjercicioRutina;
import com.gymprofit.api.enums.TipoObjetivo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ============================================================
// ReglasProgramaTest — las reglas del punto 3 del catálogo, contra el JSON (GP-074)
//
// Sin contexto ni base: ReglasPrograma es lógica pura. Se recorren las 31 rutinas con el
// nivel de cada programa en que salen, los cinco niveles de perfil (con «sin nivel»), los
// cinco objetivos (con «sin objetivo») y los cuatro tiempos, y se comprueba lo que el
// catálogo promete: con 30 min, sesiones de 21 a 30; con 45, de 34 a 45; con 60 y 75,
// nunca más del tiempo elegido. Y que la duración de cada plantilla es la del JSON.
// ============================================================
@DisplayName("GP-074 — reglas de los programas contra el catálogo")
class ReglasProgramaTest {

    // Los de peso corporal en la base (ver CatalogoPlantillasDePrueba en integration).
    private static final Set<String> PESO_CORPORAL = Set.of(
            "Single_Leg_Glute_Bridge", "Dead_Bug", "Floor_Glute-Ham_Raise", "Step-up_with_Knee_Raise",
            "Plank", "Glute_Kickback", "Pullups", "Push-Ups_-_Close_Triceps_Position",
            "Push-Ups_With_Feet_Elevated", "Pushups", "Bench_Dips", "Reverse_Crunch", "Bodyweight_Squat",
            "Bodyweight_Walking_Lunge", "Butt_Lift_Bridge", "Side_Bridge", "Chin-Up");

    private static JsonNode catalogo;
    // Por código de rutina, los niveles de los programas en que sale.
    private static final Map<String, Set<Nivel>> NIVELES = new HashMap<>();

    @BeforeAll
    static void leer() throws Exception {
        try (InputStream in = new ClassPathResource("db/semillas/catalogo-plantillas-v1.json").getInputStream()) {
            catalogo = new ObjectMapper().readTree(in);
        }
        for (JsonNode p : catalogo.get("programas")) {
            for (JsonNode c : p.get("semana")) {
                NIVELES.computeIfAbsent(c.asText(), k -> new HashSet<>()).add(Nivel.valueOf(p.get("nivel").asText()));
            }
        }
    }

    static List<ReglasPrograma.Ejercicio> ejercicios(JsonNode rutina) {
        List<ReglasPrograma.Ejercicio> lista = new ArrayList<>();
        int i = 0;
        for (JsonNode e : rutina.get("ejercicios")) {
            lista.add(new ReglasPrograma.Ejercicio(i++, TipoEjercicioRutina.valueOf(e.get("tipo").asText()),
                    e.get("series").asInt(), e.get("min").asInt(), e.get("max").asInt(),
                    MedidaSerie.valueOf(e.get("medida").asText()),
                    e.get("porLado").isNull() ? null : PorLado.valueOf(e.get("porLado").asText()),
                    e.get("descansoSegundos").asInt(), PESO_CORPORAL.contains(e.get("fedId").asText())));
        }
        return lista;
    }

    private static List<NivelExperiencia> nivelesDePerfil() {
        List<NivelExperiencia> n = new ArrayList<>(Arrays.asList(NivelExperiencia.values()));
        n.add(null);
        return n;
    }

    private static List<TipoObjetivo> objetivos() {
        List<TipoObjetivo> o = new ArrayList<>(Arrays.asList(TipoObjetivo.values()));
        o.add(null);
        return o;
    }

    @Test
    @DisplayName("la duración de cada plantilla, sin ajustes, es la del JSON")
    void duracion_de_las_plantillas() {
        assertThat(catalogo.get("rutinas")).hasSize(31);
        for (JsonNode r : catalogo.get("rutinas")) {
            long cs = ReglasPrograma.centesimas(ejercicios(r));
            assertThat(ReglasPrograma.redondear(cs)).as(r.get("codigo").asText())
                    .isEqualTo(r.get("duracionMinutos").asInt());
            // Con 60 min y el nivel del programa no se toca nada: sale la plantilla tal cual.
            for (Nivel nivel : NIVELES.get(r.get("codigo").asText())) {
                ReglasPrograma.Resultado res = ReglasPrograma.aplicar(ejercicios(r), nivel,
                        NivelExperiencia.valueOf(nivel.name()), TipoObjetivo.GANAR_MASA_MUSCULAR, 60);
                assertThat(res.ejercicios()).as(r.get("codigo").asText()).isEqualTo(ejercicios(r));
                assertThat(res.duracion()).isEqualTo(r.get("duracionMinutos").asInt());
            }
        }
    }

    @Test
    @DisplayName("con 30 min, de 21 a 30; con 45, de 34 a 45; con 60 y 75, nunca más: en todas las rutinas, niveles y objetivos")
    void los_tiempos_se_cumplen_siempre() {
        Map<Integer, double[]> extremos = new HashMap<>();
        int casos = 0;
        for (JsonNode r : catalogo.get("rutinas")) {
            for (Nivel nivelPrograma : NIVELES.get(r.get("codigo").asText())) {
                for (NivelExperiencia perfil : nivelesDePerfil()) {
                    for (TipoObjetivo objetivo : objetivos()) {
                        for (int minutos : List.of(30, 45, 60, 75)) {
                            ReglasPrograma.Resultado res = ReglasPrograma.aplicar(ejercicios(r), nivelPrograma,
                                    perfil, objetivo, minutos);
                            String caso = r.get("codigo").asText() + " " + nivelPrograma + " " + perfil + " "
                                    + objetivo + " " + minutos;
                            assertThat(res.minutos()).as(caso).isLessThanOrEqualTo(minutos);
                            assertThat(res.duracion()).as(caso).isLessThanOrEqualTo(minutos);
                            if (minutos == 30) assertThat(res.minutos()).as(caso).isGreaterThanOrEqualTo(21);
                            if (minutos == 45) assertThat(res.minutos()).as(caso).isGreaterThanOrEqualTo(34);
                            assertThat(res.ejercicios()).as(caso).isNotEmpty();
                            extremos.merge(minutos, new double[]{res.minutos(), res.minutos()},
                                    (a, b) -> new double[]{Math.min(a[0], b[0]), Math.max(a[1], b[1])});
                            casos++;
                        }
                    }
                }
            }
        }
        // 31 rutinas (ninguna sale en dos niveles) × 5 perfiles × 5 objetivos × 4 tiempos.
        assertThat(casos).isEqualTo(31 * 5 * 5 * 4);
        // Los extremos que da el script del catálogo: el recorte no se queda corto de más.
        assertThat(extremos.get(30)[0]).isLessThan(22);
        assertThat(extremos.get(45)[0]).isLessThan(35);
    }

    @Test
    @DisplayName("avanzado o experto en un programa de intermedio: básicos a 4 series y extras a 3; en principiante, nada")
    void series_de_avanzado() {
        JsonNode torso = rutina("GIM-TORSO-A");
        for (NivelExperiencia n : List.of(NivelExperiencia.AVANZADO, NivelExperiencia.EXPERTO)) {
            ReglasPrograma.Resultado res = ReglasPrograma.aplicar(ejercicios(torso), Nivel.INTERMEDIO, n, null, 75);
            assertThat(res.avanzado()).isTrue();
            // Con 75 min no se recorta nada y los básicos suben uno más: 5 y 3.
            assertThat(res.ejercicios()).extracting(ReglasPrograma.Ejercicio::series)
                    .containsExactly(5, 5, 5, 3, 3, 3, 3);
        }
        ReglasPrograma.Resultado principiante = ReglasPrograma.aplicar(ejercicios(rutina("GIM-PE-A")),
                Nivel.PRINCIPIANTE, NivelExperiencia.AVANZADO, null, 60);
        assertThat(principiante.avanzado()).isFalse();
        assertThat(principiante.ejercicios()).isEqualTo(ejercicios(rutina("GIM-PE-A")));
    }

    @Test
    @DisplayName("«Ganar fuerza»: básicos a 4–6 con 3 min, salvo los de peso corporal; los extras no cambian")
    void objetivo_de_fuerza() {
        // GIM-CC-C: hip thrust y press son de carga; las dominadas, de peso corporal.
        ReglasPrograma.Resultado res = ReglasPrograma.aplicar(ejercicios(rutina("GIM-CC-C")), Nivel.INTERMEDIO,
                NivelExperiencia.INTERMEDIO, TipoObjetivo.MEJORAR_FUERZA, 75);
        List<ReglasPrograma.Ejercicio> e = res.ejercicios();
        assertThat(e.get(0)).extracting("min", "max", "descanso").containsExactly(4, 6, 180);
        assertThat(e.get(1)).extracting("min", "max", "descanso").containsExactly(4, 6, 180);
        assertThat(e.get(2)).extracting("min", "max", "descanso").containsExactly(6, 10, 120);
        for (ReglasPrograma.Ejercicio x : e) {
            if (x.tipo() == TipoEjercicioRutina.EXTRA) {
                assertThat(x).isEqualTo(ejercicios(rutina("GIM-CC-C")).get(x.indice()));
            }
        }
        // En peso corporal no cambia ningún básico.
        ReglasPrograma.Resultado pc = ReglasPrograma.aplicar(ejercicios(rutina("PC-TORSO-A")), Nivel.INTERMEDIO,
                NivelExperiencia.INTERMEDIO, TipoObjetivo.MEJORAR_FUERZA, 60);
        assertThat(pc.ejercicios()).isEqualTo(ejercicios(rutina("PC-TORSO-A")));
    }

    @Test
    @DisplayName("el recorte va en orden: extras desde el final, luego series de los básicos hasta 2, luego el último básico")
    void orden_del_recorte() {
        List<ReglasPrograma.Ejercicio> plantilla = ejercicios(rutina("GIM-TORSO-A"));

        // Con 45 min salen los extras del final primero: los básicos se quedan como están.
        ReglasPrograma.Resultado r45 = ReglasPrograma.aplicar(plantilla, Nivel.INTERMEDIO,
                NivelExperiencia.INTERMEDIO, null, 45);
        assertThat(r45.ejercicios()).extracting(ReglasPrograma.Ejercicio::indice).startsWith(0, 1, 2, 3);
        assertThat(r45.ejercicios().size()).isLessThan(plantilla.size());
        assertThat(r45.ejercicios().subList(0, 3)).extracting(ReglasPrograma.Ejercicio::series).containsOnly(3);

        // Con fuerza y avanzado en 30 min ya no queda ningún extra y los básicos bajan a 2
        // antes de quitar ninguno.
        ReglasPrograma.Resultado r30 = ReglasPrograma.aplicar(plantilla, Nivel.INTERMEDIO,
                NivelExperiencia.AVANZADO, TipoObjetivo.MEJORAR_FUERZA, 30);
        assertThat(r30.ejercicios()).allMatch(x -> x.tipo() == TipoEjercicioRutina.BASICO);
        assertThat(r30.ejercicios()).extracting(ReglasPrograma.Ejercicio::series).containsOnly(2);
        assertThat(r30.ejercicios()).extracting(ReglasPrograma.Ejercicio::indice)
                .isEqualTo(List.of(0, 1, 2).subList(0, r30.ejercicios().size()));
    }

    @Test
    @DisplayName("la fórmula: calentamiento, cambios, trabajo por lado y sin descanso tras la última serie")
    void formula_de_la_duracion() {
        // 2 ejercicios: 8 min + 1 cambio. Press 2 × 8–12 (35 s por serie) con 120 s; plancha
        // lateral 2 × 20–40 s por lado (60 s por serie) con 60 s, sin descanso al final.
        List<ReglasPrograma.Ejercicio> r = List.of(
                new ReglasPrograma.Ejercicio(0, TipoEjercicioRutina.BASICO, 2, 8, 12, MedidaSerie.REPETICIONES,
                        null, 120, false),
                new ReglasPrograma.Ejercicio(1, TipoEjercicioRutina.EXTRA, 2, 20, 40, MedidaSerie.SEGUNDOS,
                        PorLado.LADO, 60, true));
        long esperado = (480 + 60 + 35 + 120 + 35 + 120 + 60 + 60 + 60) * 100L;
        assertThat(ReglasPrograma.centesimas(r)).isEqualTo(esperado);
        // 1030 s son 17,2 min: se redondea a 15. 17,5 min, la mitad: hacia arriba, a 20.
        assertThat(ReglasPrograma.redondear(esperado)).isEqualTo(15);
        assertThat(ReglasPrograma.redondear(1050 * 100L)).isEqualTo(20);
        assertThat(ReglasPrograma.redondear(1049 * 100L)).isEqualTo(15);
    }

    @Test
    @DisplayName("solo se aceptan 30, 45, 60 y 75 minutos")
    void minutos_no_validos() {
        assertThatThrownBy(() -> ReglasPrograma.aplicar(ejercicios(rutina("GIM-PE-A")), Nivel.PRINCIPIANTE,
                null, null, 50)).isInstanceOf(IllegalArgumentException.class);
    }

    private static JsonNode rutina(String codigo) {
        for (JsonNode r : catalogo.get("rutinas")) {
            if (r.get("codigo").asText().equals(codigo)) return r;
        }
        throw new IllegalArgumentException(codigo);
    }
}
