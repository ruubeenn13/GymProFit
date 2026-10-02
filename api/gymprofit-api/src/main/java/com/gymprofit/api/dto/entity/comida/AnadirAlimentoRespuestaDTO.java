package com.gymprofit.api.dto.entity.comida;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============================================================
// AnadirAlimentoRespuestaDTO — lo que devuelve POST /comidas/anadir (lote 1.6.1)
// La comida, con sus totales ya recalculados, la línea que se ha creado o sumado y lo
// que tenía antes, si ya estaba.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnadirAlimentoRespuestaDTO {
    private ComidaDTO comida;
    private AlimentoComidaDTO linea;
    // Lote 1.6.3 (A2): null si la línea es nueva; si se sumó a una que ya estaba, la
    // cantidad que tenía antes. Con eso la app deshace: borra la línea nueva, o la deja
    // como estaba con el PATCH. Siempre presente en el JSON, también cuando es null.
    private CantidadAnteriorDTO anterior;
}
