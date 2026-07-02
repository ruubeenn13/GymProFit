package com.gymprofit.api.dto.jooq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// UsuarioJooqDTO — proyección plana de Usuario para consultas jOOQ
// DTO usado en consultas complejas/joins con jOOQ donde no se necesita
// la entidad JPA completa. Sus campos (enums como String) reflejan
// directamente las columnas seleccionadas en la query.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioJooqDTO implements Serializable {
    private Integer id;
    private String username;
    private String email;
    private BigDecimal peso;
    private BigDecimal altura;
    private Integer edad;
    private String nivelExperiencia;
    private String objetivo;
    private Byte activo;
}
