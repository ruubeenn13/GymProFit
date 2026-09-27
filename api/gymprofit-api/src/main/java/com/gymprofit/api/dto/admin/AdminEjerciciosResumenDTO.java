package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

// ============================================================
// AdminEjerciciosResumenDTO — cabecera y opciones de la pantalla de ejercicios (GP-085)
// Cuántos hay activos, cuántos faltan por revisar, y la lista cerrada de
// equipamiento con sus etiquetas, para que la web no la repita a mano.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminEjerciciosResumenDTO implements Serializable {

    private long activos;
    private long sinRevisar;
    private List<Opcion> equipamientos;

    /** Un valor de una lista cerrada con su etiqueta en los dos idiomas. */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Opcion implements Serializable {
        private String codigo;
        private String etiqueta;
        private String etiquetaEn;
    }
}
