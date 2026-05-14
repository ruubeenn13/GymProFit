package com.gymprofit.api.dto.entity.usuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.gymprofit.api.enums.TipoObjetivo;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioUpdateDTO implements Serializable {
    private Integer id;
    private String email;
    private Double peso;
    private Double altura;
    private Integer edad;
    private String nivelExperiencia;
    private TipoObjetivo objetivo;
    private Boolean activo;
}
