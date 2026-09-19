package com.gymprofit.api.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// EliminarCuentaDTO — reautenticación para borrar la cuenta
// Solo lleva la contraseña actual. La identidad del que borra NO viaja aquí: sale
// del token (DEC-013), así que este cuerpo no puede decir a quién se borra.
//
// La contraseña se pide aunque la petición ya venga autenticada porque el borrado
// es irreversible y no hay periodo de gracia: un token robado, o un móvil
// desbloqueado un minuto, no deben bastar para vaciar una cuenta.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EliminarCuentaDTO implements Serializable {

    // Contraseña actual en texto plano, solo para comparar contra el hash guardado.
    @NotBlank
    private String password;
}
