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
@Entity
// ============================================================
// ProgramaUsuario — un programa que sigue un usuario (GP-074)
// Al seguirlo, el usuario se lleva una copia de cada rutina distinta del programa,
// enlazada a esta fila. Seguirlo otra vez crea otra fila y otras copias. La borra el
// borrado de cuenta (DEC-031).
// ============================================================
@Table(name = "programas_usuario")
public class ProgramaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programa_id", nullable = false)
    private Programa programa;

    // Minutos por sesión con los que se ajustaron las copias: 30, 45, 60 o 75.
    @Column(nullable = false)
    private Integer minutos;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;
}
