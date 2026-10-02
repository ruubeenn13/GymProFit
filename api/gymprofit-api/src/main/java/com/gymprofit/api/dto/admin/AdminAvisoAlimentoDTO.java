package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// ============================================================
// AdminAvisoAlimentoDTO — un aviso pendiente en la web de administración (lote 1.6.1)
// Con lo justo para reconocerlo (nombre, marca, fuente y código) y, si está en el
// catálogo, el alimento entero, para editarlo ahí mismo con el editor de siempre. Un
// producto aún sin materializar no tiene alimento: se reconoce por su código.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminAvisoAlimentoDTO {
    private Integer id;
    private Integer alimentoId;
    private String barcode;
    private String nombre;
    private String marca;
    private String fuente;
    private String motivo;
    private int veces;
    private LocalDateTime creado;
    private LocalDateTime actualizado;
    // El alimento del catálogo, como en /admin/alimentos; null si es un producto sin materializar.
    private AdminAlimentoDTO alimento;
}
