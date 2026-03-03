package com.gymprofit.api.dto.entity.comida;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComidaCreateDTO implements Serializable {
    private Integer usuarioId;
    private LocalDateTime fecha;
    private String tipoComida;
    private String notas;
}
