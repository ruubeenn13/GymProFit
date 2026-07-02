package com.gymprofit.api.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// CountDTO — respuesta tipada para endpoints de conteo
// Encapsula el número de elementos devuelto por los endpoints de
// tipo /count, ofreciendo un contrato claro en Swagger en lugar de
// un Map<String, Object> genérico.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CountDTO implements Serializable {
    // Número de elementos contados
    private long count;
}
