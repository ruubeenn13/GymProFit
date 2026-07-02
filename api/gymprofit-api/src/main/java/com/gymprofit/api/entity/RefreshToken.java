package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
// ============================================================
// RefreshToken — token de refresco opaco y persistido de un usuario
// Permite renovar el access token JWT (de vida corta) sin volver a pedir
// credenciales. Al ser persistido puede rotarse (se revoca el anterior y se
// emite uno nuevo en cada refresh) y revocarse explícitamente (logout),
// algo imposible con un JWT stateless.
// ============================================================
@Table(name = "refresh_tokens")
public class RefreshToken {

    // Identificador autogenerado del refresh token.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Valor opaco del token (UUID aleatorio), único en la tabla.
    @Column(nullable = false, unique = true)
    private String token;

    // Usuario propietario del token.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Momento en el que el token deja de ser válido.
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    // Marca de revocación: un token revocado (rotado o por logout) ya no sirve.
    @Column(nullable = false)
    private boolean revocado;
}
