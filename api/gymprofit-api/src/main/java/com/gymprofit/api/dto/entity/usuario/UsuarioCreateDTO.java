package com.gymprofit.api.dto.entity.usuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.gymprofit.api.enums.TipoObjetivo;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioCreateDTO implements Serializable {
    private String username;
    private String password;
    private String email;
    private Double peso;
    private Double altura;
    private Integer edad;
    private String nivelExperiencia;
    private TipoObjetivo objetivo;
}
