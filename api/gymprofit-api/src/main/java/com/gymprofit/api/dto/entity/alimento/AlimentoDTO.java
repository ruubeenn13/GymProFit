package com.gymprofit.api.dto.entity.alimento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

// ============================================================
// AlimentoDTO — representación completa de un alimento para lectura
// Se usa como respuesta en las operaciones de consulta del catálogo
// de alimentos, incluyendo su información nutricional y de propiedad.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentoDTO implements Serializable {
    private Integer id;
    private String nombre;
    private String categoria;
    private Integer calorias;
    private BigDecimal proteinas;
    private BigDecimal carbohidratos;
    private BigDecimal grasas;
    private BigDecimal fibra;
    private Integer porcionGramos;
    private String descripcion;
    // Indica si el alimento está activo (visible/usable) o dado de baja lógica
    private Boolean activo;
    // Id del usuario propietario si es un alimento personalizado (null si es global)
    private Integer usuarioId;
    // Código de barras (Open Food Facts); en resultados de búsqueda externa el
    // id viene null y este campo permite importar el producto a la BD local
    private String barcode;
    // Marca/fabricante del producto (Open Food Facts)
    private String marca;

    // --- GP-127 / GP-162: campos nuevos y opcionales. Una app que no los conozca los ignora.

    // De dónde salen los datos: CIQUAL, USDA, OFF; null si se hizo a mano.
    private String fuente;
    // true en los básicos, curados uno a uno.
    private Boolean revisado;
    // Raciones con nombre y peso; lista vacía si no tiene.
    private List<RacionDTO> raciones;
    // Solo en la búsqueda: TUYO, BASICO o PRODUCTO (el grupo en el que sale el resultado).
    private String grupo;

    // --- Lote 1.6.3: opcionales, como los de arriba.

    // La categoría canónica, sin traducir («Lácteos»), o null (A6). `categoria` sigue
    // como estaba: en inglés se traduce en los básicos, y las builds repartidas la enseñan.
    private String categoriaClave;
    // Si es favorito de la cuenta que pregunta (A3): en la búsqueda, en GET
    // /alimentos/{id}, en GET /alimentos/codigo/{codigo} y en la lista de favoritos.
    private Boolean favorito;
    // Lo último que apuntaste (A1): en la búsqueda, solo en TUYO; y en los favoritos.
    // Null si nunca lo has apuntado.
    private UltimaCantidadDTO ultima;
}
