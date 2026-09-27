package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// AdminAlimentoDTO — un alimento del catálogo en la web de administración (GP-085)
// Lleva todo lo que edita el editor, así que la lista basta y no hace falta una
// ficha aparte: se guarda con PATCH /alimentos/{id}, la ruta de siempre. Valores
// por 100 g. Solo catálogo: un alimento con dueño no sale nunca en esta lista.
// ============================================================
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminAlimentoDTO implements Serializable {

    private Integer id;
    private String nombre;
    private String nombreEn;
    private String marca;
    private String categoria;
    private String barcode;
    private Integer calorias;
    private BigDecimal proteinas;
    private BigDecimal carbohidratos;
    private BigDecimal grasas;
    private BigDecimal fibra;
    private Integer porcionGramos;
    private boolean activo;
    // OPEN_FOOD_FACTS si tiene código de barras (vino del escáner); MANUAL si no.
    private String origen;
}
