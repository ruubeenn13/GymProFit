package com.gymprofit.api.dto.entity.usuario;

import com.gymprofit.api.enums.NivelActividad;
import com.gymprofit.api.enums.Sexo;
import com.gymprofit.api.enums.TipoObjetivo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// UsuarioPatchDTO — DTO para actualización parcial (PATCH) de un usuario
// Contiene solo los campos modificables del perfil de usuario; los
// campos null se ignoran en el service y no sobrescriben el valor actual.
// Usado por ejemplo en el flujo de onboarding de la app Android.
// "email" solo se admite si es el actual: ver UsuarioService.patch (GP-083).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioPatchDTO implements Serializable {
    private String email;
    private BigDecimal peso;
    private BigDecimal altura;
    private Integer edad;
    private String nivelExperiencia;
    private TipoObjetivo objetivo;
    // GP-111. Enums: un valor fuera de la lista no se deserializa y da 400 antes de tocar nada.
    private Sexo sexo;
    private NivelActividad nivelActividad;
    // Sin "activo" a propósito (GP-083): el usuario no decide si su cuenta está activa.
    // Si un cliente lo manda, Jackson lo ignora como cualquier campo desconocido.
}
