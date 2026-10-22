package com.gymprofit.api.service.externo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.enums.Equipamiento;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.service.ejercicio.CatalogoEs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

// ============================================================
// WgerImportServiceTest — lo que llega por la importación del catálogo.
//
// El equipamiento de un ejercicio nuevo (GP-122) y, desde GP-217, que el español sale del
// CSV del catálogo y de ningún otro sitio: ni de wger ni de un traductor automático. Sin red:
// si el import llamara a MyMemory, estos tests lo notarían porque el texto no sería el del CSV.
// ============================================================
@DisplayName("WgerImportService — lo que se importa")
class WgerImportServiceTest {

    private static final String CSV = "fed_id,nombre_es,descripcion_es\n"
            + "Prueba_Import,Prueba revisada,\"1. Túmbate en el banco.\n2. Empuja la barra.\"\n"
            + "Sin_Texto,Sin texto original,\n";

    private final ObjectMapper json = new ObjectMapper();
    private final WgerImportService servicio =
            new WgerImportService(mock(IEjercicioRepository.class), json, CatalogoEs.de(new StringReader(CSV)));

    private JsonNode ejercicioFed(String id, String nombre, String equipo) throws Exception {
        return json.readTree("""
                {"id": "%s", "name": "%s", "equipment": "%s", "level": "beginner",
                 "primaryMuscles": ["chest"], "category": "strength", "images": ["a/0.jpg", "a/1.jpg"],
                 "instructions": ["Lie on the bench.", "Push the bar."]}
                """.formatted(id, nombre, equipo));
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
        Ejercicio e = servicio.mapearFed(ejercicioFed("Prueba_Import", "Prueba Import", equipo), Map.of()).orElseThrow();

        assertEquals(esperado, e.getEquipamiento());
    }

    @Test
    @DisplayName("GP-217: el nombre y la descripción en español son los del CSV, aunque wger traiga los suyos")
    void elEspanolEsElDelCsv() throws Exception {
        Map<String, WgerImportService.WgerEs> wger = Map.of(
                "pruebaimport", new WgerImportService.WgerEs("Nombre de wger", "Descripción de wger", "Desc en"));

        Ejercicio e = servicio.mapearFed(ejercicioFed("Prueba_Import", "Prueba Import", "barbell"), wger).orElseThrow();

        assertEquals("Prueba revisada", e.getNombre());
        assertEquals("1. Túmbate en el banco.\n2. Empuja la barra.", e.getDescripcion());
    }

    @Test
    @DisplayName("GP-217: el inglés no cambia: el nombre de FED y la descripción de wger, o la de FED sin ella")
    void elInglesNoCambia() throws Exception {
        Ejercicio sinWger = servicio.mapearFed(ejercicioFed("Prueba_Import", "Prueba Import", "barbell"), Map.of()).orElseThrow();
        assertEquals("Prueba Import", sinWger.getNombreEn());
        assertEquals("1. Lie on the bench.\n2. Push the bar.", sinWger.getDescripcionEn());

        Map<String, WgerImportService.WgerEs> wger = Map.of(
                "pruebaimport", new WgerImportService.WgerEs("x", "y", "Human English text"));
        Ejercicio conWger = servicio.mapearFed(ejercicioFed("Prueba_Import", "Prueba Import", "barbell"), wger).orElseThrow();
        assertEquals("Human English text", conWger.getDescripcionEn());
    }

    @Test
    @DisplayName("GP-217: un ejercicio que no está en el CSV se queda en inglés (sin traducirlo)")
    void sinCsvSeQuedaEnIngles() throws Exception {
        Ejercicio e = servicio.mapearFed(ejercicioFed("Otro_Ejercicio", "Other Exercise", "barbell"), Map.of()).orElseThrow();

        assertEquals("Other Exercise", e.getNombre());
        assertEquals("1. Lie on the bench.\n2. Push the bar.", e.getDescripcion());
    }

    @Test
    @DisplayName("GP-217: un ejercicio del CSV sin texto original queda con la descripción vacía, no con la inglesa")
    void sinTextoQuedaVacio() throws Exception {
        JsonNode sinPasos = json.readTree("""
                {"id": "Sin_Texto", "name": "No Text", "equipment": "barbell", "level": "beginner",
                 "primaryMuscles": ["chest"], "category": "strength", "images": ["a/0.jpg", "a/1.jpg"]}
                """);

        Ejercicio e = servicio.mapearFed(sinPasos, Map.of()).orElseThrow();

        assertEquals("Sin texto original", e.getNombre());
        assertNull(e.getDescripcion());
    }
}
