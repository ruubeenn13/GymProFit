package com.gymprofit.api.dto.entity.progresoejercicio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
// RecordDestacadoDTO — el mejor levantamiento del usuario, listo para enseñar
// Lleva el nombre del ejercicio además del id porque nace para una tarjeta de la
// pantalla de inicio: sin el nombre, la app tendría que pedir el ejercicio en una
// segunda llamada solo para poder escribir una línea de texto.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordDestacadoDTO implements Serializable {

    private Integer ejercicioId;

    // Nombre en el idioma en que está guardado el catálogo.
    private String ejercicioNombre;

    // Peso del récord, en kilos.
    private BigDecimal peso;

    private Integer repeticiones;

    // Cuándo se hizo. Sirve para decir "hace 3 días" en la tarjeta.
    private LocalDateTime fecha;
}
