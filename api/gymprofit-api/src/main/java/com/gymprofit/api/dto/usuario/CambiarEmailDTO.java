package com.gymprofit.api.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// CambiarEmailDTO — cuerpo de PUT /usuarios/me/email (GP-083)
// El correo nuevo y la contraseña actual. A quién se le cambia NO viaja aquí: sale
// del token (DEC-013).
//
// La contraseña se pide por lo mismo que en el borrado de cuenta: el correo es la
// llave de la recuperación de contraseña, y quien lo cambia se queda con la cuenta.
// Un token robado no debe bastar para eso.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CambiarEmailDTO implements Serializable {

    // Correo nuevo. Mismo límite que la columna usuarios.email.
    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    // Contraseña actual en texto plano, solo para comparar contra el hash guardado.
    @NotBlank
    private String password;
}
