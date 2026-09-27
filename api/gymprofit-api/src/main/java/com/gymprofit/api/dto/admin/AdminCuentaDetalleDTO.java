package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// AdminCuentaDetalleDTO — la ficha de una cuenta (GP-085)
// La cuenta y cuántas sesiones y comidas tiene. Cuántas, no cuáles: el contenido de
// un entrenamiento o de una comida es dato de salud y no sale por /admin.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminCuentaDetalleDTO implements Serializable {

    private AdminCuentaDTO cuenta;
    private long sesiones;
    private long comidas;
}
