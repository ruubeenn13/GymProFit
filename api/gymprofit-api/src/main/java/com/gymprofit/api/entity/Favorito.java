package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// ============================================================
// Favorito — un alimento que una cuenta ha marcado con el corazón (lote 1.6.3)
// De una cuenta y de un alimento que esa cuenta puede ver: del catálogo o suyo.
// ============================================================
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "favoritos")
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "alimento_id", nullable = false)
    private Integer alimentoId;

    @Column(nullable = false)
    private LocalDateTime creado;
}
