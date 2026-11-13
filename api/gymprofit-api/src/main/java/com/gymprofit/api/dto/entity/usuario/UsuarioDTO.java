package com.gymprofit.api.dto.entity.usuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.gymprofit.api.enums.NivelActividad;
import com.gymprofit.api.enums.Sexo;
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
    // Nombre para mostrar (GP-116); null si no tiene, y la app usa el username.
    private String nombre;
    private String email;
    // GP-045: si el correo está verificado. Solo de salida: el PATCH y el PUT del perfil
    // usan otros DTO y no lo pueden poner; lo cambia solo POST /usuarios/me/email/verificar.
    private Boolean emailVerificado;
    private String peso;
    private Double altura;
    private Integer edad;
    private String nivelExperiencia;
    private TipoObjetivo objetivo;
    // Opcionales (GP-111): null si el usuario no los ha dicho.
    private Sexo sexo;
    private NivelActividad nivelActividad;
    private String fechaRegistro;
    // Avisos por tipo (GP-112): entrenar (inactividad), comidas y progreso.
    private Boolean avisosEntrenar;
    private Boolean avisosComidas;
    private Boolean avisosProgreso;
    private Boolean activo;
    private String fotoPerfil;
}
