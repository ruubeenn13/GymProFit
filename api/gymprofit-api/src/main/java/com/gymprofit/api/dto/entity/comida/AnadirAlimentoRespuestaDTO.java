package com.gymprofit.api.dto.entity.comida;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============================================================
// AnadirAlimentoRespuestaDTO — lo que devuelve POST /comidas/anadir (lote 1.6.1)
// La comida, con sus totales ya recalculados, y la línea que se ha creado o sumado.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnadirAlimentoRespuestaDTO {
    private ComidaDTO comida;
    private AlimentoComidaDTO linea;
}
