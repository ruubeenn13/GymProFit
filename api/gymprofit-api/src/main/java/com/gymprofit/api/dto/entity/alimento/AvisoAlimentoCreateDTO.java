package com.gymprofit.api.dto.entity.alimento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============================================================
// AvisoAlimentoCreateDTO — POST /alimentos/avisos: reportar un alimento (lote 1.6.1)
// El alimento por su id o, si es un producto que aún no está en el catálogo, por su
// código; uno de los dos. El motivo, de MotivoAviso. Nada más: sin texto libre.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvisoAlimentoCreateDTO {

    private Integer alimentoId;

    @Pattern(regexp = "[0-9]{1,32}")
    private String barcode;

    // VALORES, NOMBRE, RACION, REPETIDO u OTRO.
    @NotBlank
    private String motivo;
}
