package com.gymprofit.api.dto.entity.alimento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// RacionDTO — una ración con nombre de un alimento (GP-127)
// El nombre llega en el idioma de la petición y sin los gramos: «1 rebanada», 28.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RacionDTO implements Serializable {
    // Id de la ración (lote 1.6.1), para elegirla al añadir; null en un producto que
    // aún no está en el catálogo.
    private Integer id;
    private String nombre;
    private BigDecimal gramos;

    // --- Lote 1.6.2 (GP-172): la unidad, en el idioma de la petición, o null si el
    // nombre no empieza por «1 » («Media taza»). «1 rebanada» → rebanada / rebanadas.
    private String unidad;
    private String unidadPlural;
    // La clave de una ración propia (UNIDAD, RACION, ENVASE, REBANADA); null en las demás.
    private String clave;

    /**
     * Una ración con su unidad sacada del diccionario (UnidadesRacion).
     *
     * @param ingles si {@code nombre} es el inglés.
     */
    public static RacionDTO de(Integer id, String nombre, BigDecimal gramos, boolean ingles, String clave) {
        java.util.Optional<com.gymprofit.api.service.busqueda.UnidadesRacion.Unidad> u =
                com.gymprofit.api.service.busqueda.UnidadesRacion.de(nombre, ingles);
        return new RacionDTO(id, nombre, gramos, u.map(x -> x.singular()).orElse(null),
                u.map(x -> x.plural()).orElse(null), clave);
    }
}
