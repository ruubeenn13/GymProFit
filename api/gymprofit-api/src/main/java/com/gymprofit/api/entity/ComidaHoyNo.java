package com.gymprofit.api.entity;

import com.gymprofit.api.enums.TipoComida;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ============================================================
// ComidaHoyNo — una comida que una cuenta ese día no hace, «Hoy no» (lote 1.6.8)
// Cuenta para el día completo igual que una comida apuntada.
// ============================================================
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "comidas_hoy_no")
public class ComidaHoyNo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comida", nullable = false, length = 20)
    private TipoComida tipoComida;

    @Column(nullable = false)
    private LocalDateTime creado;
}
