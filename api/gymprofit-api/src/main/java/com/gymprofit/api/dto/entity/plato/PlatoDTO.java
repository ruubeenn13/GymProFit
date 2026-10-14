package com.gymprofit.api.dto.entity.plato;

import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

// ============================================================
// PlatoDTO — un plato propio con sus ingredientes (lote 1.6.6, DEC-047)
// alimento es el plato como lo ve cualquier lista (con su ración de hoy); porRacion, lo
// que se apunta con «1 ración», con los valores guardados; total, la suma de sus
// ingredientes con los valores de hoy, que es lo que saldría al guardarlo otra vez.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlatoDTO implements Serializable {
    private Integer id;
    private String nombre;
    private Integer raciones;
    private BigDecimal pesoGramos;
    private Boolean activo;
    private AlimentoDTO alimento;
    private ValoresDTO total;
    private ValoresDTO porRacion;
    private List<IngredienteDTO> ingredientes;
}
