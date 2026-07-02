package com.gymprofit.api.dto.entity.usuario;

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
    private Boolean activo;
}
