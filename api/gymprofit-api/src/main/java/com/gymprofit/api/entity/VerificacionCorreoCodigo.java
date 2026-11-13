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
// VerificacionCorreoCodigo — código de un solo uso para verificar el correo (GP-045)
// La misma forma que PasswordResetCodigo, con una columna más: la dirección a la que
// se mandó. El código verifica esa dirección y ninguna otra, así que si el correo de
// la cuenta cambia entre que se pide y se canjea, el código viejo ya no sirve.
// ============================================================
@Table(name = "verificacion_correo_codigos")
public class VerificacionCorreoCodigo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Cuenta para la que se emitió.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Dirección a la que se mandó el código.
    @Column(nullable = false, length = 100)
    private String email;

    // Hash BCrypt de los seis dígitos; el código en claro solo existe en el correo.
    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    // Momento a partir del cual el código deja de valer.
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    // Consumido, o muerto porque se emitió otro.
    @Column(nullable = false)
    private boolean usado;

    // Intentos fallidos acumulados sobre este código.
    @Column(nullable = false)
    private int intentos;
}
