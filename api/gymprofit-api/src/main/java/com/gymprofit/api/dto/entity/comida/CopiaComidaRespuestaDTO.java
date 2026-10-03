package com.gymprofit.api.dto.entity.comida;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ============================================================
// CopiaComidaRespuestaDTO — lo que devuelve POST /comidas/copiar (lote 1.6.4, A2)
// La comida de destino, con sus totales ya recalculados, y por cada alimento copiado
// su línea y lo que tenía antes, como POST /comidas/anadir: con eso la app quita
// exacto lo que se copió, alimento a alimento.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CopiaComidaRespuestaDTO {
    private ComidaDTO comida;
    private List<Copiada> lineas;

    /** Un alimento copiado: su línea en el destino y lo que tenía antes (null si es nueva). */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Copiada {
        private AlimentoComidaDTO linea;
        private CantidadAnteriorDTO anterior;
    }
}
