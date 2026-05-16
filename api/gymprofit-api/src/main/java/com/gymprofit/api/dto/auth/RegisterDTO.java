package com.gymprofit.api.dto.auth;

import jakarta.validation.constraints.*;
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
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @Positive
    private BigDecimal peso;

    @Positive
    private BigDecimal altura;

    @Min(0) @Max(120)
    private Integer edad;

    private String nivelExperiencia;
    private TipoObjetivo objetivo;
    private List<Integer> roles;
}
