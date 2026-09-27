package com.gymprofit.api.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

// ============================================================
// EquipamientoTest — de equipo_necesario a la lista cerrada (GP-122).
// Los casos son los textos que escribe la importación (free-exercise-db y wger) y
// los que dejó la migración V202609271301: el mapeo en Java tiene que dar lo mismo.
// ============================================================
@DisplayName("Equipamiento — desde equipo_necesario")
class EquipamientoTest {

    @ParameterizedTest(name = "{0} / {1} → {2}")
    @CsvSource(delimiter = ';', value = {
            // Lo que escribe la importación de free-exercise-db (EQUIPO_FED)
            "Sin equipo; Bodyweight; PESO_CORPORAL",
            "Máquina; Machine; MAQUINA",
            "Mancuernas; Dumbbell; MANCUERNAS",
            "Barra; Barbell; BARRA",
            "Polea; Cable; POLEA",
            "Kettlebell; Kettlebell; KETTLEBELL",
            "Banda elástica; Resistance band; BANDA",
            "Balón medicinal; Medicine ball; OTRO",
            "Fitball; Exercise ball; OTRO",
            "Barra Z; EZ-bar; BARRA",
            "Rodillo de espuma; Foam roller; OTRO",
            // Listas de wger: gana el primero del orden barra, mancuernas, kettlebell,
            // polea, máquina, banda, peso corporal
            "Banco, Mancuernas; Bench, Dumbbell; MANCUERNAS",
            "Barra de dominadas; Pull-up bar; PESO_CORPORAL",
            "Barra de dominadas, Mancuernas; Pull-up bar, Dumbbell; MANCUERNAS",
            "Esterilla; Gym mat; PESO_CORPORAL",
            "Banco; Bench; OTRO",
            "Máquina de polea; Cable machine; POLEA",
            "Cinta de correr; Treadmill; MAQUINA",
            "Sin equipo; No equipment; PESO_CORPORAL",
    })
    void mapea(String es, String en, Equipamiento esperado) {
        assertEquals(esperado, Equipamiento.desdeEquipoNecesario(es, en));
    }

    @Test
    @DisplayName("Sin texto en ningún idioma, o solo en uno, no falla")
    void sinTexto() {
        assertEquals(Equipamiento.OTRO, Equipamiento.desdeEquipoNecesario(null, null));
        assertEquals(Equipamiento.OTRO, Equipamiento.desdeEquipoNecesario("  ", ""));
        assertEquals(Equipamiento.MANCUERNAS, Equipamiento.desdeEquipoNecesario(null, "Dumbbell"));
        assertEquals(Equipamiento.BARRA, Equipamiento.desdeEquipoNecesario("BARRA", null));
    }
}
