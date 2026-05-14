package com.gymprofit.api.dto.entity.usuario;

import com.gymprofit.api.enums.TipoObjetivo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioPatchDTO implements Serializable {
    private String email;
    private BigDecimal peso;
    private BigDecimal altura;
    private Integer edad;
    private String nivelExperiencia;
    private TipoObjetivo objetivo;
    private Boolean activo;
}
