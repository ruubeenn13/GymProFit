package com.gymprofit.api.service.record;

import com.gymprofit.api.service.record.CalculadoraRecords.Evento;
import com.gymprofit.api.service.record.CalculadoraRecords.Resultado;
import com.gymprofit.api.service.record.CalculadoraRecords.Serie;
import com.gymprofit.api.service.record.CalculadoraRecords.Tipo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// CalculadoraRecordsTest — reglas de los récords (GP-088), sin base de datos.
// ============================================================
class CalculadoraRecordsTest {

    private static final int PRESS = 1;
    private static final int DOMINADAS = 2;
    private static final LocalDateTime LUNES = LocalDateTime.of(2026, 9, 21, 18, 0);

    private static Serie serie(int sesion, int diasDespues, int ejercicio, String peso, int reps) {
        return new Serie(sesion, LUNES.plusDays(diasDespues), ejercicio,
                peso == null ? null : new BigDecimal(peso), reps);
    }

    @Test
    @DisplayName("La primera marca de un ejercicio es el punto de partida, no un récord")
    void primeraMarca_noEsRecord() {
        Resultado r = CalculadoraRecords.calcular(List.of(
                serie(10, 0, PRESS, "80", 5),
                serie(10, 0, PRESS, "80", 4)));

        assertThat(r.records()).isEmpty();
        assertThat(r.eventos()).hasSize(1);
        assertThat(r.eventos().get(0).esPrimera()).isTrue();
        assertThat(r.recordVigentePorEjercicio()).isEmpty();
    }

    @Test
    @DisplayName("Igualar una marca no es récord")
    void igualar_noEsRecord() {
        Resultado r = CalculadoraRecords.calcular(List.of(
                serie(10, 0, PRESS, "80", 5),
                serie(11, 2, PRESS, "80.00", 5)));

        assertThat(r.records()).isEmpty();
    }

    @Test
    @DisplayName("Superar el peso es récord, y dice cuál era la marca anterior")
    void superarPeso_esRecord() {
        Resultado r = CalculadoraRecords.calcular(List.of(
                serie(10, 0, PRESS, "80", 5),
                serie(11, 2, PRESS, "82.5", 5)));

        assertThat(r.records()).hasSize(1);
        Evento e = r.records().get(0);
        assertThat(e.marca().peso()).isEqualByComparingTo("82.5");
        assertThat(e.marca().repeticiones()).isEqualTo(5);
        assertThat(e.anterior().peso()).isEqualByComparingTo("80");
        assertThat(e.marca().sesionId()).isEqualTo(11);
    }

    @Test
    @DisplayName("Con el mismo peso, más repeticiones supera la marca")
    void mismoPesoMasRepeticiones_esRecord() {
        Resultado r = CalculadoraRecords.calcular(List.of(
                serie(10, 0, PRESS, "80", 5),
                serie(11, 2, PRESS, "80", 6)));

        assertThat(r.records()).hasSize(1);
        assertThat(r.records().get(0).marca().repeticiones()).isEqualTo(6);
    }

    @Test
    @DisplayName("Menos peso con más repeticiones no es récord de peso")
    void menosPesoMasReps_noEsRecord() {
        Resultado r = CalculadoraRecords.calcular(List.of(
                serie(10, 0, PRESS, "80", 5),
                serie(11, 2, PRESS, "75", 10)));

        assertThat(r.records()).isEmpty();
    }

    @Test
    @DisplayName("Borrar la sesión del récord lo devuelve al anterior")
    void sinLaSesionDelRecord_vuelveElAnterior() {
        List<Serie> historia = new ArrayList<>(List.of(
                serie(10, 0, PRESS, "80", 5),
                serie(11, 2, PRESS, "82.5", 5),
                serie(12, 4, PRESS, "85", 3)));

        Evento vigente = CalculadoraRecords.calcular(historia).recordVigentePorEjercicio().get(PRESS);
        assertThat(vigente.marca().peso()).isEqualByComparingTo("85");

        historia.removeIf(s -> s.sesionId() == 12);
        vigente = CalculadoraRecords.calcular(historia).recordVigentePorEjercicio().get(PRESS);
        assertThat(vigente.marca().peso()).isEqualByComparingTo("82.5");
        assertThat(vigente.anterior().peso()).isEqualByComparingTo("80");
    }

    @Test
    @DisplayName("Sin peso, la marca son las repeticiones máximas")
    void sinPeso_cuentanRepeticiones() {
        Resultado r = CalculadoraRecords.calcular(List.of(
                serie(10, 0, DOMINADAS, null, 8),
                serie(11, 2, DOMINADAS, "0", 8),
                serie(12, 4, DOMINADAS, null, 10)));

        assertThat(r.records()).hasSize(1);
        assertThat(r.records().get(0).marca().tipo()).isEqualTo(Tipo.REPETICIONES);
        assertThat(r.records().get(0).marca().repeticiones()).isEqualTo(10);
        assertThat(r.records().get(0).anterior().repeticiones()).isEqualTo(8);
    }

    @Test
    @DisplayName("Lastradas y sin lastre son marcas distintas; manda la de peso")
    void conYSinPeso_sonMarcasDistintas() {
        Resultado r = CalculadoraRecords.calcular(List.of(
                serie(10, 0, DOMINADAS, null, 8),
                serie(11, 2, DOMINADAS, "10", 5),
                serie(12, 4, DOMINADAS, null, 12),
                serie(13, 6, DOMINADAS, "12.5", 5)));

        assertThat(r.records()).hasSize(2);
        assertThat(r.recordVigentePorEjercicio().get(DOMINADAS).marca().tipo()).isEqualTo(Tipo.PESO);
    }

    @Test
    @DisplayName("Las series sin repeticiones no cuentan")
    void ceroRepeticiones_noCuenta() {
        Resultado r = CalculadoraRecords.calcular(List.of(
                serie(10, 0, PRESS, "80", 5),
                serie(11, 2, PRESS, "100", 0)));

        assertThat(r.records()).isEmpty();
    }

    @Test
    @DisplayName("La progresión es la mejor serie de cada sesión, en orden")
    void progresion_mejorSeriePorSesion() {
        Resultado r = CalculadoraRecords.calcular(List.of(
                serie(11, 2, PRESS, "70", 8),
                serie(11, 2, PRESS, "75", 6),
                serie(10, 0, PRESS, "80", 5)));

        assertThat(r.progresion().get(PRESS)).extracting(m -> m.sesionId()).containsExactly(10, 11);
        assertThat(r.progresion().get(PRESS).get(1).peso()).isEqualByComparingTo("75");
    }

    @Test
    @DisplayName("Los eventos de una sesión incluyen récords y primeras marcas")
    void deSesion_recordsYPrimeras() {
        Resultado r = CalculadoraRecords.calcular(List.of(
                serie(10, 0, PRESS, "80", 5),
                serie(11, 2, PRESS, "82.5", 5),
                serie(11, 2, DOMINADAS, null, 8)));

        assertThat(r.deSesion(11)).hasSize(2);
        assertThat(r.deSesion(11)).filteredOn(Evento::esPrimera).hasSize(1);
    }

    @Test
    @DisplayName("1RM de Epley: 82,5 kg x 5 son 96,3 kg")
    void epley() {
        assertThat(CalculadoraRecords.unoRmEpley(new BigDecimal("82.5"), 5)).isEqualByComparingTo("96.3");
    }
}
