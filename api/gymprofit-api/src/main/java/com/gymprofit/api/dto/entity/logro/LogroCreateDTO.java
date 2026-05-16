package com.gymprofit.api.dto.entity.logro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogroCreateDTO implements Serializable {
    private String nombre;
    private String descripcion;
    private String tipo;
}
