package com.gymprofit.api.service.programa;

import com.gymprofit.api.enums.EquipamientoPrograma;
import com.gymprofit.api.enums.NivelExperiencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// RecomendacionProgramaTest — la tabla del punto 1 del catálogo, entera (GP-074, 1.2.1)
//
// Una fila por nivel de perfil («-» es sin nivel) y equipamiento, con el programa y el
// motivo de cada número de días, de 2 a 6. Escrita a mano desde el catálogo, no desde
// el código: 5 niveles × 3 equipamientos × 5 días = 75 casos.
// ============================================================
@DisplayName("GP-074 — el programa recomendado")
class RecomendacionProgramaTest {

    @ParameterizedTest(name = "{0} · {1}")
    @CsvSource(delimiter = ';', value = {
            // nivel; equipamiento; programa con 2, 3, 4, 5 y 6 días; motivo con 2, 3, 4, 5 y 6 días
            "-;GIMNASIO;GIM-PE,GIM-PE,GIM-PE,GIM-PE,GIM-PE;sinNivel,sinNivel,sinNivelMasDias,sinNivelMasDias,sinNivelMasDias",
            "-;MANCUERNAS;MAN-PE,MAN-PE,MAN-PE,MAN-PE,MAN-PE;sinNivel,sinNivel,sinNivelMasDias,sinNivelMasDias,sinNivelMasDias",
            "-;PESO_CORPORAL;PC-PE,PC-PE,PC-PE,PC-PE,PC-PE;sinNivel,sinNivel,sinNivelMasDias,sinNivelMasDias,sinNivelMasDias",
            "PRINCIPIANTE;GIMNASIO;GIM-PE,GIM-PE,GIM-PE,GIM-PE,GIM-PE;-,-,principianteMasDias,principianteMasDias,principianteMasDias",
            "PRINCIPIANTE;MANCUERNAS;MAN-PE,MAN-PE,MAN-PE,MAN-PE,MAN-PE;-,-,principianteMasDias,principianteMasDias,principianteMasDias",
            "PRINCIPIANTE;PESO_CORPORAL;PC-PE,PC-PE,PC-PE,PC-PE,PC-PE;-,-,principianteMasDias,principianteMasDias,principianteMasDias",
            "INTERMEDIO;GIMNASIO;GIM-CC,GIM-CC,GIM-TP,GIM-TPET,GIM-ETP;-,-,-,-,-",
            "INTERMEDIO;MANCUERNAS;MAN-CC,MAN-CC,MAN-TP,MAN-TPET,MAN-ETP;-,-,-,-,-",
            "INTERMEDIO;PESO_CORPORAL;PC-CC,PC-CC,PC-TP,PC-TP,PC-TP;-,-,-,pesoCorporalMasDias,pesoCorporalMasDias",
            "AVANZADO;GIMNASIO;GIM-CC,GIM-CC,GIM-TP,GIM-TPET,GIM-ETP;avanzado,avanzado,avanzado,avanzado,avanzado",
            "AVANZADO;MANCUERNAS;MAN-CC,MAN-CC,MAN-TP,MAN-TPET,MAN-ETP;avanzado,avanzado,avanzado,avanzado,avanzado",
            "AVANZADO;PESO_CORPORAL;PC-CC,PC-CC,PC-TP,PC-TP,PC-TP;avanzado,avanzado,avanzado,avanzadoPesoCorporalMasDias,avanzadoPesoCorporalMasDias",
            "EXPERTO;GIMNASIO;GIM-CC,GIM-CC,GIM-TP,GIM-TPET,GIM-ETP;avanzado,avanzado,avanzado,avanzado,avanzado",
            "EXPERTO;MANCUERNAS;MAN-CC,MAN-CC,MAN-TP,MAN-TPET,MAN-ETP;avanzado,avanzado,avanzado,avanzado,avanzado",
            "EXPERTO;PESO_CORPORAL;PC-CC,PC-CC,PC-TP,PC-TP,PC-TP;avanzado,avanzado,avanzado,avanzadoPesoCorporalMasDias,avanzadoPesoCorporalMasDias"})
    @DisplayName("la tabla del catálogo")
    void tabla(String nivel, String equipamiento, String programas, String motivos) {
        NivelExperiencia n = nivel.equals("-") ? null : NivelExperiencia.valueOf(nivel);
        String[] p = programas.split(",");
        String[] m = motivos.split(",");
        for (int dias = 2; dias <= 6; dias++) {
            RecomendacionPrograma.Recomendacion r =
                    RecomendacionPrograma.elegir(EquipamientoPrograma.valueOf(equipamiento), dias, n);
            String caso = nivel + " " + equipamiento + " " + dias + " días";
            assertThat(r.codigo()).as(caso).isEqualTo(p[dias - 2]);
            String motivo = m[dias - 2];
            assertThat(r.motivo()).as(caso).isEqualTo(motivo.equals("-") ? null : "programa.recomendado." + motivo);
        }
    }
}
