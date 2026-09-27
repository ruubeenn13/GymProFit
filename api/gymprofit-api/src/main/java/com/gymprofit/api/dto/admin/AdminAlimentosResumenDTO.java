package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

// ============================================================
// AdminAlimentosResumenDTO — cabecera y filtros de la pantalla de alimentos (GP-085)
// Cuántos hay en el catálogo, cuántos sin nombre en inglés y qué categorías usa.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminAlimentosResumenDTO implements Serializable {

    private long catalogo;
    private long sinIngles;
    private List<String> categorias;
}
