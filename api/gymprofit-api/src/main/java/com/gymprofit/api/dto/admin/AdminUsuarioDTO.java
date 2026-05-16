package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUsuarioDTO implements Serializable {
    private Integer id;
    private String username;
    private String email;
    private BigDecimal peso;
    private BigDecimal altura;
    private Integer edad;
    private String nivelExperiencia;
    private String objetivo;
    private Byte activo;
    private LocalDateTime fechaRegistro;
    private String rol;
}
