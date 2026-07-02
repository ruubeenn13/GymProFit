package com.gymprofit.api.entity;

import com.gymprofit.api.enums.RoleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
// ============================================================
// Role — rol de seguridad asignable a un usuario
// Entidad de catálogo que representa los roles del sistema (ADMIN, USER,
// GUEST, etc.) usados por Spring Security para autorizar el acceso a los
// distintos endpoints de la API de GymProFit.
// ============================================================
@Table(name = "roles")
public class Role {

    // Identificador autogenerado del rol.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nombre del rol (enum RoleType), único en la tabla.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleType nombre;
}