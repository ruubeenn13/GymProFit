package com.gymprofit.api.service.programa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// CicloProgramaTest — la rutina que toca en un programa que se sigue (GP-074, 1.2.1)
// ============================================================
@DisplayName("GP-074 — la rutina que toca en el ciclo")
class CicloProgramaTest {

    private static final List<String> TP = List.of("TORSO-A", "PIERNA-A", "TORSO-B", "PIERNA-B");
    private static final List<String> ETP = List.of("EMPUJE", "TIRON", "PIERNA-A", "EMPUJE", "TIRON", "PIERNA-B");

    private static CicloPrograma.Estado calcular(List<String> semana, int inicial, String... sesiones) {
        return CicloPrograma.calcular(semana, new HashSet<>(semana), inicial, List.of(sesiones));
    }

    @Test
    @DisplayName("sin sesiones toca la posición inicial")
    void sin_sesiones() {
        assertThat(calcular(TP, 1).posicionHoy()).isEqualTo(1);
        assertThat(calcular(TP, 3).posicionHoy()).isEqualTo(3);
        assertThat(calcular(TP, 1).hechas()).isEmpty();
        // Una posición fuera de rango cuenta como 1.
        assertThat(calcular(TP, 9).posicionHoy()).isEqualTo(1);
    }

    @Test
    @DisplayName("cada sesión lleva a la siguiente; faltar un día no salta ninguna (solo cuentan las sesiones)")
    void en_orden() {
        CicloPrograma.Estado e = calcular(TP, 1, "TORSO-A");
        assertThat(e.posicionHoy()).isEqualTo(2);
        assertThat(e.hechas()).containsExactly(1);
        e = calcular(TP, 1, "TORSO-A", "PIERNA-A", "TORSO-B");
        assertThat(e.posicionHoy()).isEqualTo(4);
        assertThat(e.hechas()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("al acabar la vuelta toca la primera y la barra empieza de cero")
    void vuelta_completa() {
        CicloPrograma.Estado e = calcular(TP, 1, "TORSO-A", "PIERNA-A", "TORSO-B", "PIERNA-B");
        assertThat(e.posicionHoy()).isEqualTo(1);
        assertThat(e.hechas()).isEmpty();
        e = calcular(TP, 1, "TORSO-A", "PIERNA-A", "TORSO-B", "PIERNA-B", "TORSO-A");
        assertThat(e.posicionHoy()).isEqualTo(2);
        assertThat(e.hechas()).containsExactly(1);
    }

    @Test
    @DisplayName("una fuera de orden salta las de en medio")
    void fuera_de_orden() {
        CicloPrograma.Estado e = calcular(TP, 1, "TORSO-B");
        assertThat(e.posicionHoy()).isEqualTo(4);
        assertThat(e.hechas()).containsExactly(3);
        // Hacer una que ya pasó da la vuelta: la siguiente aparición es la de la vuelta nueva.
        e = calcular(TP, 1, "TORSO-A", "PIERNA-A", "TORSO-B", "TORSO-A");
        assertThat(e.posicionHoy()).isEqualTo(2);
        assertThat(e.hechas()).containsExactly(1);
    }

    @Test
    @DisplayName("las repetidas del de 6 días: cuenta la aparición más próxima")
    void repetidas() {
        assertThat(calcular(ETP, 1, "EMPUJE", "TIRON", "PIERNA-A", "EMPUJE").posicionHoy()).isEqualTo(5);
        assertThat(calcular(ETP, 1, "EMPUJE", "TIRON", "PIERNA-A", "EMPUJE", "TIRON").posicionHoy()).isEqualTo(6);
        CicloPrograma.Estado e = calcular(ETP, 1, "EMPUJE", "TIRON", "PIERNA-A", "EMPUJE", "TIRON", "PIERNA-B",
                "EMPUJE");
        assertThat(e.posicionHoy()).isEqualTo(2);
        assertThat(e.hechas()).containsExactly(1);
        // Desde la posición 4, Empuje es la de la posición 4, no la 1.
        assertThat(calcular(ETP, 4, "EMPUJE").posicionHoy()).isEqualTo(5);
    }

    @Test
    @DisplayName("se saltan las rutinas borradas; si están todas borradas, no toca ninguna")
    void borradas() {
        Set<String> sinPiernaA = new HashSet<>(TP);
        sinPiernaA.remove("PIERNA-A");
        assertThat(CicloPrograma.calcular(TP, sinPiernaA, 1, List.of("TORSO-A")).posicionHoy()).isEqualTo(3);
        // Una sesión hecha con una rutina borrada después sigue contando.
        assertThat(CicloPrograma.calcular(TP, sinPiernaA, 1, List.of("TORSO-A", "PIERNA-A")).posicionHoy())
                .isEqualTo(3);
        assertThat(CicloPrograma.calcular(TP, Set.of(), 1, List.of("TORSO-A")).posicionHoy()).isNull();
    }

    @Test
    @DisplayName("desde una posición que no es la 1, las anteriores cuentan como hechas (GP-134)")
    void posicion_inicial_a_mitad_de_vuelta() {
        // Cambiar el tiempo a mitad de vuelta sigue el programa otra vez desde la que tocaba:
        // la barra no puede perder las que ya estaban hechas.
        assertThat(calcular(TP, 3).hechas()).containsExactly(1, 2);
        assertThat(calcular(TP, 3).posicionHoy()).isEqualTo(3);

        CicloPrograma.Estado e = calcular(TP, 3, "TORSO-B");
        assertThat(e.posicionHoy()).isEqualTo(4);
        assertThat(e.hechas()).containsExactly(1, 2, 3);

        // Al acabar esa vuelta, la barra empieza de cero como siempre.
        e = calcular(TP, 3, "TORSO-B", "PIERNA-B");
        assertThat(e.posicionHoy()).isEqualTo(1);
        assertThat(e.hechas()).isEmpty();

        // Hacer una de antes de la inicial da la vuelta: esa es de la vuelta nueva.
        e = calcular(TP, 3, "TORSO-A");
        assertThat(e.posicionHoy()).isEqualTo(2);
        assertThat(e.hechas()).containsExactly(1);

        // Fuera de rango cuenta como 1: nada hecho.
        assertThat(calcular(TP, 9).hechas()).isEmpty();
    }

    @Test
    @DisplayName("una sesión de una rutina que no es del ciclo no mueve nada")
    void ajena() {
        assertThat(calcular(TP, 2, "OTRA").posicionHoy()).isEqualTo(2);
    }
}
