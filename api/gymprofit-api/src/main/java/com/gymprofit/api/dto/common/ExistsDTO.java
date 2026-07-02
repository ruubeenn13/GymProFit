package com.gymprofit.api.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// ExistsDTO — respuesta tipada para endpoints de existencia
// Encapsula el resultado booleano de los endpoints de tipo /exists,
// ofreciendo un contrato claro en Swagger en lugar de un
// Map<String, Object> genérico.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExistsDTO implements Serializable {
    // Indica si el elemento consultado existe
    private boolean existe;
}
