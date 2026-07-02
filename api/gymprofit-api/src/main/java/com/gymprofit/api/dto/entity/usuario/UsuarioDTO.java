package com.gymprofit.api.dto.entity.usuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.gymprofit.api.enums.TipoObjetivo;

import java.io.Serializable;

// ============================================================
// UsuarioDTO — DTO de salida con los datos públicos de un usuario
// Representa la información de un usuario tal como se expone en las
// respuestas de la API (sin password ni datos sensibles), usada para
// listados, perfil y detalle de usuario en la app Android.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDTO implements Serializable {
    private Integer id;
    private String username;
    private String email;
    private String peso;
    private Double altura;
    private Integer edad;
    private String nivelExperiencia;
    private TipoObjetivo objetivo;
    private String fechaRegistro;
    private Boolean activo;
    private String fotoPerfil;
}
