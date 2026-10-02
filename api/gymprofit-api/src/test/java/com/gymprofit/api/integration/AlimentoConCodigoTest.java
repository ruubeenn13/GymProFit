package com.gymprofit.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// AlimentoConCodigoTest — POST /alimentos con código de barras (lote 1.6.1)
// «Créalo», desde el escáner o desde la búsqueda, crea un alimento TUYO con el código
// que no existía. El código es único por dueño (DEC-040): repetido entre lo tuyo, 409;
// el mismo código en el alimento de otra persona no choca ni se nota.
// ============================================================
class AlimentoConCodigoTest extends AbstractOwnershipTest {

    private static String cuerpo(String nombre, String codigo) {
        return "{\"nombre\":\"" + nombre + "\",\"calorias\":120,\"proteinas\":4,\"carbohidratos\":20,"
                + "\"grasas\":2,\"porcionGramos\":100,\"barcode\":\"" + codigo + "\"}";
    }

    @Test
    @DisplayName("crear un alimento con su código lo guarda, y escanear ese código lo encuentra")
    void crea_con_codigo() throws Exception {
        pedir(owner, "POST /alimentos", cuerpo("Pan del horno de abajo", "8400000710015"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value("8400000710015"))
                .andExpect(jsonPath("$.usuarioId").value(owner.getId()));

        pedir(owner, "GET /alimentos/codigo/8400000710015")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pan del horno de abajo"));
    }

    @Test
    @DisplayName("el mismo código dos veces entre lo tuyo es un 409")
    void repetido_409() throws Exception {
        pedir(owner, "POST /alimentos", cuerpo("Primero", "8400000710022")).andExpect(status().isOk());
        pedir(owner, "POST /alimentos", cuerpo("Segundo", "8400000710022")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("el mismo código en el alimento de otra persona no choca")
    void otro_dueno_no_choca() throws Exception {
        pedir(owner, "POST /alimentos", cuerpo("De owner", "8400000710039")).andExpect(status().isOk());
        pedir(attacker, "POST /alimentos", cuerpo("De attacker", "8400000710039"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(attacker.getId()));
    }

    @Test
    @DisplayName("un código con algo que no sean cifras es un 400")
    void codigo_invalido_400() throws Exception {
        pedir(owner, "POST /alimentos", cuerpo("Raro", "84ab0071")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("sin código, como hasta ahora")
    void sin_codigo() throws Exception {
        pedir(owner, "POST /alimentos",
                "{\"nombre\":\"Sin código\",\"calorias\":100,\"porcionGramos\":100}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").doesNotExist());
    }
}
