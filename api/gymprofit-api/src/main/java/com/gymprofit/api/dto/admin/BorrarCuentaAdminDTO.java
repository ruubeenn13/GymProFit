package com.gymprofit.api.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============================================================
// BorrarCuentaAdminDTO — borrado de una cuenta a petición de su titular (GP-085)
// confirmacion es el nombre de usuario de la cuenta, escrito a mano: la misma
// fricción que pide la web, repetida en el servidor, que es la que protege.
// motivo queda en el log (por ejemplo, la fecha del correo a privacidad@).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BorrarCuentaAdminDTO {

    @NotBlank
    private String confirmacion;

    @NotBlank
    @Size(max = 500)
    private String motivo;
}
