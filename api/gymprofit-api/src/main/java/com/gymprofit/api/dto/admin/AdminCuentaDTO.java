package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

// ============================================================
// AdminCuentaDTO — una cuenta en la web de administración (GP-085)
// Solo datos de cuenta. Nada de salud: ni peso, ni altura, ni edad, ni objetivo, ni
// nivel. Por eso es un DTO nuevo y no AdminUsuarioDTO, que lleva el peso y la altura y
// sigue sirviendo al panel de la app sin cambiar de forma.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminCuentaDTO implements Serializable {

    private Integer id;
    private String username;
    private String email;
    // Alta y último acceso como instantes, con su zona: en la base van en el reloj del
    // servidor (UTC en Render) y sin zona, y la web los leería como hora local.
    private OffsetDateTime fechaRegistro;
    // null si no ha entrado desde que existe la columna (27-09-2026).
    private OffsetDateTime ultimoAcceso;
    // ADMIN, USER o GUEST.
    private String rol;
    private boolean activo;
}
