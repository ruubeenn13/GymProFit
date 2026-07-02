package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
// AdminUsuarioDTO — vista de usuario para el panel de administración
// Representación de un usuario con sus datos físicos y de cuenta
// (rol, estado, fecha de registro) pensada para los listados y
// operaciones del panel de administración de GymProFit.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUsuarioDTO implements Serializable {
    private Integer id;
    private String username;
    private String email;
    private BigDecimal peso;
    private BigDecimal altura;
    private Integer edad;
    private String nivelExperiencia;
    private String objetivo;
    // Estado activo/inactivo del usuario (0/1)
    private Byte activo;
    private LocalDateTime fechaRegistro;
    // Rol del usuario (USER, ADMIN, GUEST, etc.)
    private String rol;
}
