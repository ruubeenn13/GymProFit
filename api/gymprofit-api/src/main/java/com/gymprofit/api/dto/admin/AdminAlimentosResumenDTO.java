package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

// ============================================================
// AdminAlimentosResumenDTO — cabecera y filtros de la pantalla de alimentos (GP-085)
// Cuántos hay en el catálogo, cuántos sin nombre en inglés, qué categorías usa,
// cuántos de cada fuente y cuántos productos de España hay para elegir.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminAlimentosResumenDTO implements Serializable {

    private long catalogo;
    private long sinIngles;
    private List<String> categorias;
    // Cuántos del catálogo hay de cada fuente: CIQUAL, USDA, OFF y MANUAL (GP-127).
    private java.util.Map<String, Long> porFuente;
    // Cuántos productos de España hay en productos_off, materializados o no (GP-164).
    private long productos;

    public AdminAlimentosResumenDTO(long catalogo, long sinIngles, List<String> categorias) {
        this(catalogo, sinIngles, categorias, java.util.Map.of(), 0);
    }
}
