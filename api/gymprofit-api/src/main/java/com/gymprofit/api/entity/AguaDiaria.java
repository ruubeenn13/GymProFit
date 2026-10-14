package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ============================================================
// AguaDiaria — el agua que una cuenta ha bebido un día, en mililitros (lote 1.6.8)
// Una fila por cuenta y día; sin fila, el día va a 0.
// ============================================================
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "agua_diaria")
public class AguaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Integer mililitros;

    @Column(nullable = false)
    private LocalDateTime actualizado;
}
