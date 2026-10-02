package com.gymprofit.api.entity;

import com.gymprofit.api.enums.MotivoAviso;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// ============================================================
// AvisoAlimento — un aviso de que un alimento tiene algo mal (lote 1.6.1)
// De un alimento del catálogo o del código de un producto sin materializar, con un
// motivo y las veces que se ha repetido. Sin quién lo envió (ver la migración).
// ============================================================
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "avisos_alimento")
public class AvisoAlimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // El alimento del catálogo; null si es un producto aún sin materializar.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alimento_id")
    private Alimento alimento;

    // El código del producto sin materializar; null si hay alimento.
    @Column(length = 32)
    private String barcode;

    // «a:<id>» o «c:<código>»: lo que hace único el aviso abierto.
    @Column(nullable = false, length = 40)
    private String clave;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MotivoAviso motivo;

    // Cuántas cuentas distintas lo han enviado mientras estaba abierto.
    @Column(nullable = false)
    private int veces;

    // true mientras está pendiente; null al resolverlo.
    @Column
    private Boolean abierto;

    @Column(nullable = false)
    private LocalDateTime creado;

    @Column(nullable = false)
    private LocalDateTime actualizado;

    @Column
    private LocalDateTime resuelto;
}
