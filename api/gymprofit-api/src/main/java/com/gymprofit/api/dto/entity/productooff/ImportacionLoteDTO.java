package com.gymprofit.api.dto.entity.productooff;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============================================================
// ImportacionLoteDTO — resultado de un lote de la importación (GP-164)
// recibidos = guardados + descartados. Los descartados no pasan las comprobaciones
// de ProductoOffValidacion (los mismos filtros que el script).
// ============================================================
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportacionLoteDTO {
    private int recibidos;
    private int guardados;
    private int descartados;
}
