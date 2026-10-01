package com.gymprofit.api.dto.entity.productooff;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============================================================
// ProductoOffImportDTO — un producto tal como lo manda la importación (GP-164)
// Mismos nombres que escribe datos/productos/filtrar_off.py. Valores por 100 g.
// La API vuelve a comprobarlo todo: quien tiene la clave no tiene por qué mandar
// datos buenos.
// ============================================================
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoOffImportDTO {
    private String codigo;
    private String nombre;
    private String marca;
    private Double kcal;
    private Double proteinas;
    private Double carbohidratos;
    private Double grasas;
    private Double fibra;
    private Double racionGramos;
    private String racionTexto;
    private String envase;
    private Integer escaneos;
    // Solo para comprobar que las kcal cuadran (7 y 2,4 kcal/g); no se guardan.
    private Double alcohol;
    private Double polioles;
}
