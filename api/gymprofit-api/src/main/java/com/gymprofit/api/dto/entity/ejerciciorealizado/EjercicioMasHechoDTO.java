package com.gymprofit.api.dto.entity.ejerciciorealizado;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// EjercicioMasHechoDTO — un ejercicio de «Los que más haces» (GP-216, Biblioteca)
// El id del catálogo y en cuántas sesiones terminadas del usuario ha salido. La app ya tiene
// el catálogo entero, así que no hace falta más: nombre, foto y nivel los pone ella.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EjercicioMasHechoDTO implements Serializable {

    // Id del ejercicio del catálogo.
    private Integer ejercicioId;

    // Sesiones terminadas del usuario en las que aparece (una vez por sesión).
    private Long veces;
}
