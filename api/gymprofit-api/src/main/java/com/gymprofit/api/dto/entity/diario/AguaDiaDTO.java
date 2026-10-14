package com.gymprofit.api.dto.entity.diario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// ============================================================
// AguaDiaDTO — el agua de un día tal como quedó guardada (lote 1.6.8)
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AguaDiaDTO {

    private LocalDate fecha;

    private int mililitros;
}
