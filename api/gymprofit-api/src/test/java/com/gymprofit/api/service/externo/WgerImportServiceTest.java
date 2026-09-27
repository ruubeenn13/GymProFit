package com.gymprofit.api.service.externo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.enums.Equipamiento;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

// ============================================================
// WgerImportServiceTest — el equipamiento de lo que llega por la importación (GP-122).
// Sin red: el nombre en español sale del índice de wger que se le pasa, así que no
// se llama al traductor.
// ============================================================
@DisplayName("WgerImportService — equipamiento de los ejercicios importados")
class WgerImportServiceTest {

    private final ObjectMapper json = new ObjectMapper();
    private final WgerImportService servicio = new WgerImportService(mock(IEjercicioRepository.class), json);

    private JsonNode ejercicioFed(String equipo) throws Exception {
        return json.readTree("""
                {"id": "Prueba_Import", "name": "Prueba Import", "equipment": "%s", "level": "beginner",
                 "primaryMuscles": ["chest"], "category": "strength", "images": ["a/0.jpg", "a/1.jpg"]}
                """.formatted(equipo));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "barbell, BARRA",
            "dumbbell, MANCUERNAS",
            "kettlebells, KETTLEBELL",
            "cable, POLEA",
            "machine, MAQUINA",
            "bands, BANDA",
            "body only, PESO_CORPORAL",
            "e-z curl bar, BARRA",
            "medicine ball, OTRO",
    })
    @DisplayName("Un ejercicio nuevo llega con el equipamiento de su equipo, no como «Otro»")
    void equipamientoDelImportado(String equipo, Equipamiento esperado) throws Exception {
        Map<String, WgerImportService.WgerEs> wger =
                Map.of("pruebaimport", new WgerImportService.WgerEs("Prueba importada", "Desc", "Desc en"));

        Ejercicio e = servicio.mapearFed(ejercicioFed(equipo), wger).orElseThrow();

        assertEquals(esperado, e.getEquipamiento());
    }
}
