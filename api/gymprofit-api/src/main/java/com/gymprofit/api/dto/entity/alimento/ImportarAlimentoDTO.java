package com.gymprofit.api.dto.entity.alimento;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// ImportarAlimentoDTO — cuerpo de POST /alimentos/importar
// Identifica por código de barras el producto de Open Food Facts que el
// usuario ha seleccionado y quiere materializar en el catálogo local.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportarAlimentoDTO implements Serializable {

    // Código de barras del producto en Open Food Facts
    @NotBlank(message = "El código de barras es obligatorio")
    private String barcode;
}
