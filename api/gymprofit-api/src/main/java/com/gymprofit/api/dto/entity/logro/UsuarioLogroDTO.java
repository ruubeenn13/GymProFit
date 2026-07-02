package com.gymprofit.api.dto.entity.logro;

import com.gymprofit.api.enums.TipoLogro;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

// ============================================================
// UsuarioLogroDTO — DTO de un logro obtenido por un usuario
// Representa la relación usuario-logro, con los datos del logro
// desnormalizados y la fecha en que el usuario lo consiguió.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioLogroDTO implements Serializable {
    private Integer id;
    private Integer logroId;
    private String logroNombre;
    private String logroDescripcion;
    private TipoLogro logroTipo;
    // Fecha y hora en que el usuario obtuvo el logro
    private LocalDateTime fechaObtenido;
}
