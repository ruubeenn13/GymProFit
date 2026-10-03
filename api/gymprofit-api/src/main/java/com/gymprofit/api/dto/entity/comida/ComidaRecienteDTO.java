package com.gymprofit.api.dto.entity.comida;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

// ============================================================
// ComidaRecienteDTO — una comida que se puede copiar (lote 1.6.4, A1)
// GET /comidas/recientes: «Merienda de ayer», con lo que se copiaría. Las líneas, como
// las da la lista de líneas de una comida (nombre en el idioma de la petición, cantidad
// con la regla de GP-177), y solo las que se pueden copiar: sin alimentos desactivados
// ni que la cuenta ya no vea. Las kcal son las de esas líneas.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComidaRecienteDTO {
    private Integer id;
    // DESAYUNO, ALMUERZO, COMIDA, MERIENDA, CENA o SNACK.
    private String tipoComida;
    private LocalDate fecha;
    private Integer kcal;
    private List<AlimentoComidaDTO> lineas;
}
