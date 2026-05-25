package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminEstadisticasDTO implements Serializable {
    private Long totalUsuarios;
    private Long usuariosActivos;
    private Long totalSesiones;
    private Long sesionesHoy;
    private Long totalEjerciciosRealizados;
    private Long totalObjetivosCompletados;
    private Long totalLogrosOtorgados;
    private Long rutinasPredefinidas;
    private Long ejerciciosActivos;
}
