package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
// ============================================================
// UsuarioLogro — entidad JPA que registra la obtención de un logro
// por parte de un usuario.
// Es la tabla intermedia entre Usuario y Logro, guardando además
// la fecha en la que el usuario consiguió dicho logro.
// ============================================================
@Entity
@Table(name = "usuario_logros")
public class UsuarioLogro {

    // Identificador único autoincremental del registro.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Usuario que ha obtenido el logro.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Logro obtenido por el usuario.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "logro_id", nullable = false)
    private Logro logro;

    // Fecha y hora en que se obtuvo el logro.
    @Column(name = "fecha_obtenido", nullable = false)
    private LocalDateTime fechaObtenido;
}
