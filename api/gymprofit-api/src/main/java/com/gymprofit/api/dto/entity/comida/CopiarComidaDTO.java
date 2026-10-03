package com.gymprofit.api.dto.entity.comida;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// ============================================================
// CopiarComidaDTO — POST /comidas/copiar: una comida entera a otra (lote 1.6.4, A2)
// La comida de origen, por su id, y la de destino por su día y su tipo, como en
// POST /comidas/anadir: se encuentra o se crea. Sin usuario: es el del token (DEC-013).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CopiarComidaDTO {

    // La comida que se copia: de la cuenta del token, o 403.
    @NotNull
    private Integer comidaId;

    // Día de la comida de destino.
    @NotNull
    private LocalDate fecha;

    // Tipo de la comida de destino.
    @NotBlank
    private String tipoComida;
}
