package com.gymprofit.api.dto.entity.logro;

import com.gymprofit.api.enums.TipoLogro;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

// ============================================================
// LogroProgresoDTO — un logro del catálogo visto por el usuario del token:
// si lo tiene, desde cuándo, y si no, cuánto le falta (GP-079).
//
// Junta en una respuesta lo que antes eran dos llamadas (catálogo + obtenidos)
// y añade lo que ninguna daba: el umbral y dónde está el usuario.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Logro del catálogo con el estado y el progreso del usuario autenticado")
public class LogroProgresoDTO implements Serializable {

    @Schema(description = "Id del logro en el catálogo", example = "2")
    private Integer logroId;

    @Schema(description = "Nombre, en el idioma del Accept-Language si hay traducción", example = "Constancia")
    private String nombre;

    @Schema(description = "Descripción, en el idioma del Accept-Language si hay traducción")
    private String descripcion;

    @Schema(description = "Tipo estable del logro", example = "CONSTANCIA")
    private TipoLogro tipo;

    @Schema(description = "Qué se cuenta para este logro", example = "SESIONES_COMPLETADAS")
    private TipoLogro.Metrica metrica;

    @Schema(description = "Cuánto hace falta en la métrica", example = "7")
    private Integer umbral;

    @Schema(description = "Lo que lleva el usuario, sin pasar del umbral", example = "3")
    private Integer progreso;

    @Schema(description = "Si el usuario ya tiene el logro concedido")
    private boolean conseguido;

    @Schema(description = "Cuándo se concedió; nulo si no está conseguido")
    private LocalDateTime fechaObtenido;
}
