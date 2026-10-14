package com.gymprofit.api.dto.entity.plato;

import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

// ============================================================
// LoMioDTO — la pestaña «Mis platos» (lote 1.6.6, A3)
// Los platos de la cuenta y, aparte, los alimentos que ha creado. Cada uno con sus
// raciones, si es favorito y su última cantidad; del usado más recientemente al menos, y
// los nunca usados, los más nuevos primero.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoMioDTO implements Serializable {
    private List<AlimentoDTO> platos;
    private List<AlimentoDTO> alimentos;
}
