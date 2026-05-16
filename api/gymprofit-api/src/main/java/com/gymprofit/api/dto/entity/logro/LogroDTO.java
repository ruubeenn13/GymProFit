package com.gymprofit.api.dto.entity.logro;

import com.gymprofit.api.enums.TipoLogro;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogroDTO implements Serializable {
    private Integer id;
    private String nombre;
    private String descripcion;
    private TipoLogro tipo;
}
