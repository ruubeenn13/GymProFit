package com.gymprofit.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.gymprofit.api.enums.TipoObjetivo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO implements Serializable {
    private String username;
    private String password;
    private String email;
    private BigDecimal peso;
    private BigDecimal altura;
    private Integer edad;
    private String nivelExperiencia;
    private TipoObjetivo objetivo;
    private List<Integer> roles;
}
