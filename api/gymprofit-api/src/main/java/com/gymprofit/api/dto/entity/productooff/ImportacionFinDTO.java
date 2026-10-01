package com.gymprofit.api.dto.entity.productooff;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============================================================
// ImportacionFinDTO — estado de la tabla al cerrar una importación (GP-164)
// bytes = datos + índices de productos_off según information_schema, tras ANALYZE:
// es lo que se mide contra el presupuesto de 200 MB.
// ============================================================
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportacionFinDTO {
    private long productos;
    private long bytesDatos;
    private long bytesIndices;
}
