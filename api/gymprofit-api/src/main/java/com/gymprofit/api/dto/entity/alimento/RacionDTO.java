package com.gymprofit.api.dto.entity.alimento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// RacionDTO — una ración con nombre de un alimento (GP-127)
// El nombre llega en el idioma de la petición y sin los gramos: «1 rebanada», 28.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RacionDTO implements Serializable {
    // Id de la ración (lote 1.6.1), para elegirla al añadir; null en un producto que
    // aún no está en el catálogo.
    private Integer id;
    private String nombre;
    private BigDecimal gramos;
}
