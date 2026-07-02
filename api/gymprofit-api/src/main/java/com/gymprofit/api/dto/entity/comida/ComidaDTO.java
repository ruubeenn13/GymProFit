package com.gymprofit.api.dto.entity.comida;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
// ComidaDTO — representación completa de una comida registrada
// Devuelto por la API al consultar comidas de un usuario. Incluye los
// totales nutricionales agregados (calorías, proteínas, carbohidratos, grasas)
// calculados a partir de los alimentos asociados a la comida.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComidaDTO implements Serializable {
    // Identificador de la comida
    private Integer id;
    // Id del usuario propietario
    private Integer usuarioId;
    // Fecha y hora de la comida
    private LocalDateTime fecha;
    // Tipo de comida (desayuno, almuerzo, cena, etc.)
    private String tipoComida;
    // Total de calorías sumadas de los alimentos que la componen
    private Integer totalCalorias;
    // Total de proteínas (g)
    private BigDecimal totalProteinas;
    // Total de carbohidratos (g)
    private BigDecimal totalCarbohidratos;
    // Total de grasas (g)
    private BigDecimal totalGrasas;
    // Notas u observaciones opcionales
    private String notas;

}
