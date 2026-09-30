package com.gymprofit.api.dto.auth;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.gymprofit.api.enums.NivelActividad;
import com.gymprofit.api.enums.Sexo;
import com.gymprofit.api.enums.TipoObjetivo;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// RegisterDTO — datos de entrada para el registro de un nuevo usuario
// Contiene las credenciales básicas (username, password, email) y los
// datos físicos/objetivos iniciales usados para dar de alta el perfil
// en el endpoint /auth/register de GymProFit.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO implements Serializable {
    // Nombre de usuario único, entre 3 y 50 caracteres y sin «@». Opcional desde el alta
    // nueva (GP-103): si no llega, la API lo propone con la parte del correo (NombreUsuario).
    @Size(min = 3, max = 50)
    private String username;

    // Contraseña en texto plano (se hashea en el servicio). Política de GP-101 (DEC-034):
    // mínimo 8 caracteres, máximo 72 bytes, sin reglas de composición. La lista de
    // bloqueo y el nombre los comprueba PoliticaContrasena en el servicio.
    @NotBlank
    @ContrasenaNueva
    @ToString.Exclude
    private String password;

    // Correo electrónico del usuario, debe tener formato válido
    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    // Peso corporal inicial (kg), opcional pero debe ser positivo si se indica
    @Positive
    private BigDecimal peso;

    // Altura inicial (cm/m), opcional pero debe ser positiva si se indica
    @Positive
    private BigDecimal altura;

    // Edad del usuario, entre 0 y 120 años
    @Min(0) @Max(120)
    private Integer edad;

    private String nivelExperiencia;
    private TipoObjetivo objetivo;

    // El resto del perfil, para que la cuenta nazca con él en una sola llamada (GP-103).
    // Nombre para mostrar con la regla del PATCH (GP-116): recortado, en blanco no se
    // guarda y con más de 40 caracteres es 400. Enums: fuera de su lista, 400.
    private String nombre;
    private Sexo sexo;
    private NivelActividad nivelActividad;
}
