package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// ============================================================
// PropuestaFavorito — un alimento que ya no se propone a esa cuenta (lote 1.6.3, A4)
// Porque rechazó la propuesta (RECHAZADA) o porque alguna vez fue su favorito
// (FAVORITO). La propuesta sale una sola vez por alimento, y nunca la de algo que ya
// conoces como favorito.
// ============================================================
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "propuestas_favorito")
public class PropuestaFavorito {

    public static final String RECHAZADA = "RECHAZADA";
    public static final String FAVORITO = "FAVORITO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "alimento_id", nullable = false)
    private Integer alimentoId;

    @Column(nullable = false, length = 10)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime creado;
}
