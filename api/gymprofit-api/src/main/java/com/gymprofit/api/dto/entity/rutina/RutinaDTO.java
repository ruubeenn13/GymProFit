package com.gymprofit.api.dto.entity.rutina;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SeriesMusculoDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.VezRutinaDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// RutinaDTO — DTO de salida que representa una rutina de entrenamiento completa
// Expone los datos de la rutina, incluyendo campos calculados como el número de
// ejercicios y las calorías aproximadas, sin exponer la entidad JPA directamente.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RutinaDTO implements Serializable {
    private Integer id;
    private String usuarioId;
    private String nombre;
    private String descripcion;
    private Integer duracionMinutos;
    private String nivel;
    private Boolean esPredefinida;
    private String categoria;
    private String diasSemana;
    private LocalDateTime fechaCreacion;
    // Indica si la rutina está activa (visible/usable) o desactivada
    private Boolean activa;
    // Número de ejercicios que componen la rutina (calculado)
    private Integer numEjercicios;
    // Solo en una copia de un programa (GP-074): la plantilla de la que sale y el
    // «programa que sigue» del que forma parte. El orden de la semana sale del programa
    // (GET /programas/{codigo}), por el código de la plantilla. Nulos en el resto.
    private Integer plantillaId;
    private String plantillaCodigo;
    private Integer programaUsuarioId;
    private String programaCodigo;
    // Nombre de ese programa, en el idioma de la petición (lote 1.2.1).
    private String programaNombre;
    // Solo en GET /rutinas/usuario/{id}/activas (GP-232), para las tarjetas de Entrenar y la
    // hoja «¿Qué rutina hoy?»: las series pautadas por músculo, de más a menos, y las 6 últimas
    // veces que se hizo, de la más vieja a la más nueva. En el resto de rutas no salen.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SeriesMusculoDTO> musculos;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<VezRutinaDTO> ultimas;
    // NO HAY CAMPO DE CALORÍAS AQUÍ, Y ES A PROPÓSITO (DEC-004 / GP-010).
    // El gasto calórico de un entrenamiento no se puede estimar con los datos que
    // tiene la app, así que no se estima. La columna sigue en la base de datos hasta
    // la migración que la retire, pero ni se lee ni se escribe.
}
