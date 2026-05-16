package com.gymprofit.api.dto.entity.logro;

import com.gymprofit.api.enums.TipoLogro;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioLogroDTO implements Serializable {
    private Integer id;
    private Integer logroId;
    private String logroNombre;
    private String logroDescripcion;
    private TipoLogro logroTipo;
    private LocalDateTime fechaObtenido;
}
