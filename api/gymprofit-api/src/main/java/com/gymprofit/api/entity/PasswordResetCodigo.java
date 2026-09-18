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
// PasswordResetCodigo — código de un solo uso para recuperar la contraseña
// Se emite cuando alguien dice haber olvidado su contraseña: seis dígitos que
// viajan a su correo y que aquí solo existen como hash BCrypt. Caduca pronto,
// sirve una única vez y cuenta los intentos fallidos, porque un código de seis
// dígitos sin límite de intentos se adivina a fuerza bruta.
// ============================================================
@Table(name = "password_reset_codigos")
public class PasswordResetCodigo {

    // Identificador autogenerado de la solicitud.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Cuenta para la que se pidió el código.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Hash BCrypt de los seis dígitos; el código en claro solo existe en el correo.
    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    // Momento a partir del cual el código deja de valer.
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    // Marca de consumido: un código restablece una contraseña y solo una.
    @Column(nullable = false)
    private boolean usado;

    // Intentos fallidos de verificación acumulados sobre este código.
    @Column(nullable = false)
    private int intentos;
}
