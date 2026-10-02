package com.gymprofit.api.dto.entity.alimento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ============================================================
// FavoritosDTO — GET /favoritos (lote 1.6.3, A3 y A4)
// Los favoritos de la cuenta, por uso, y como mucho una propuesta.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavoritosDTO {
    // Cada uno con sus raciones, `ultima` y `favorito`, ordenados por uso.
    private List<AlimentoDTO> favoritos;
    // Un alimento que se propone como favorito, o null.
    private PropuestaFavoritoDTO propuesta;

    /** La propuesta: el alimento y cuántas comidas lo llevan en los últimos 14 días. */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PropuestaFavoritoDTO {
        private AlimentoDTO alimento;
        private Integer veces;
    }
}
