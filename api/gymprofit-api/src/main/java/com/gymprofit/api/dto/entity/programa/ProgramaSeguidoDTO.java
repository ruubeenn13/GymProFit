package com.gymprofit.api.dto.entity.programa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// ProgramaSeguidoDTO — lo que crea seguir un programa (GP-074)
// La fila de «programa que sigue» y las copias de sus rutinas, ya ajustadas al nivel,
// al objetivo y al tiempo, con los textos en el idioma de la petición.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Un programa que sigue el usuario, con sus copias de las rutinas")
public class ProgramaSeguidoDTO implements Serializable {
    @Schema(description = "Id del «programa que sigue»; lo llevan las copias en programaUsuarioId", example = "12")
    private Integer id;
    @Schema(example = "GIM-TP")
    private String programaCodigo;
    @Schema(example = "45")
    private Integer minutos;
    private LocalDateTime fechaInicio;
    @Schema(description = "Por dónde empieza el ciclo: 1, o la que tocaba si era el mismo programa", example = "1")
    private Integer posicionInicial;
    @Schema(description = "Una copia por cada rutina distinta del programa")
    private List<RutinaConEjerciciosDTO> rutinas;
}
