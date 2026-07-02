package com.gymprofit.api.dto.entity.logro;

import com.gymprofit.api.enums.TipoLogro;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// LogroDTO — DTO de salida con la información completa de un logro
// Representa un logro tal como se devuelve al cliente, incluyendo
// su identificador y tipo (enum TipoLogro).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogroDTO implements Serializable {
    private Integer id;
    private String nombre;
    private String descripcion;
    private TipoLogro tipo;
}
