package es.pmdm.gymprofit.model;

import java.util.List;

// ============================================================
// PageDTO — página de resultados devuelta por los endpoints paginados
// (/alimentos/buscar, /ejercicios/buscar). Gson la deserializa de forma
// genérica; el flag last permite implementar scroll infinito sin pedir
// páginas de más.
// ============================================================
public class PageDTO<T> {

    // Elementos de la página actual
    private List<T> content;
    // Índice de la página actual (empezando en 0)
    private int page;
    // Tamaño de página solicitado
    private int size;
    // Total de elementos que cumplen el filtro
    private long totalElements;
    // Total de páginas
    private int totalPages;
    // true si es la última página
    private boolean last;

    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean isLast() { return last; }
}
