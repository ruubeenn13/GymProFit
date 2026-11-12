package com.gymprofit.api.entity;

import com.gymprofit.api.enums.PasoAlta;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

// ============================================================
// PasoAltaDia — cuántas veces se llegó a un paso del alta un día (GP-019, DEC-064)
// Una fila por día de Madrid y paso, y solo el total: nada dice de quién.
// Se escribe con IPasoAltaDiaRepository.sumar, en una sentencia.
// ============================================================
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "alta_pasos")
public class PasoAltaDia {

    @EmbeddedId
    private Clave clave;

    @Column(nullable = false)
    private Integer total;

    /** El día y el paso, la clave de la fila. */
    @Embeddable
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @EqualsAndHashCode
    public static class Clave implements Serializable {

        @Column(nullable = false)
        private LocalDate dia;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 16)
        private PasoAlta paso;
    }
}
