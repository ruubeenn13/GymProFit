package com.gymprofit.api.dto.entity.record;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

// ============================================================
// RecordsDTO — los récords del usuario del token (GP-088)
//
// Dos listas en una respuesta porque las dos pantallas que la piden necesitan las
// dos: la tarjeta de inicio enseña los recientes y cuenta el total, y la pantalla
// Récords enseña el total y marca como nuevos los recientes.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordsDTO implements Serializable {

    // Récord vigente de cada ejercicio que tiene alguno, del más reciente al más antiguo.
    private List<RecordDTO> records;

    // Récords batidos desde la fecha pedida, del más reciente al más antiguo.
    // Puede haber dos del mismo ejercicio si se superó dos veces.
    private List<RecordDTO> recientes;
}
