package com.gymprofit.api.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

// ============================================================
// PageDTO — respuesta tipada para endpoints paginados
// Envuelve una página de resultados con sus metadatos (página actual,
// tamaño, totales) para que el cliente pueda implementar scroll
// infinito o paginación clásica sin descargar el catálogo entero.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageDTO<T> implements Serializable {

    // Elementos de la página actual
    private List<T> content;

    // Índice de la página actual (empezando en 0)
    private int page;

    // Tamaño de página solicitado
    private int size;

    // Número total de elementos que cumplen el filtro
    private long totalElements;

    // Número total de páginas
    private int totalPages;

    // true si esta es la última página
    private boolean last;

    // Construye el PageDTO a partir de una página de Spring Data y la
    // lista de contenido ya mapeada a DTO (el mapeo lo hace el service).
    public static <T> PageDTO<T> of(org.springframework.data.domain.Page<?> page, List<T> content) {
        return new PageDTO<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
