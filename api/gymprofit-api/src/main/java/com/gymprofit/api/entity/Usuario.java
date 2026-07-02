package com.gymprofit.api.entity;

import com.gymprofit.api.enums.NivelExperiencia;
import com.gymprofit.api.enums.TipoObjetivo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
// ============================================================
// Usuario — entidad JPA que representa a un usuario de GymProFit.
// Almacena credenciales, datos físicos y de perfil, y su relación
// con roles. Implementa UserDetails para integrarse directamente
// con Spring Security en el proceso de autenticación/autorización.
// ============================================================
@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {

    // Identificador único autoincremental del usuario.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nombre de usuario único usado para login.
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // Contraseña cifrada (hash).
    @Column(nullable = false)
    private String password;

    // Correo electrónico único del usuario.
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // Peso corporal en kg.
    @Column(precision = 5, scale = 2)
    private BigDecimal peso;

    // Altura en metros.
    @Column(precision = 3, scale = 2)
    private BigDecimal altura;

    // Edad del usuario.
    private Integer edad;

    // Nivel de experiencia en entrenamiento (principiante, intermedio, etc.).
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_experiencia")
    private NivelExperiencia nivelExperiencia;

    // Objetivo personal del usuario (perder peso, ganar músculo, etc.).
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TipoObjetivo objetivo;

    // Fecha en la que se registró el usuario.
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    // Indica si la cuenta está activa (usado también por isEnabled()).
    @Column(columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean activo;

    // Ruta/nombre del archivo de la foto de perfil.
    @Column(name = "foto_perfil")
    private String fotoPerfil;

    // Roles asignados al usuario (relación N:M vía tabla usuario_roles).
    // EAGER a propósito: Spring Security accede a getAuthorities()/roles del principal
    // fuera de transacción (filtro JWT y checks de autorización); en LAZY lanzaría
    // LazyInitializationException y rompería login/seguridad.
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(
            name = "usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles;

    // Convierte los roles del usuario en authorities de Spring Security
    // con el prefijo "ROLE_" requerido por el framework.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getNombre().name()))
                .collect(Collectors.toList());
    }

    // La cuenta nunca expira en este sistema.
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // La cuenta nunca se bloquea en este sistema.
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // Las credenciales nunca expiran en este sistema.
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // La cuenta está habilitada solo si el campo "activo" es true.
    @Override
    public boolean isEnabled() {
        return activo != null && activo;
    }
}