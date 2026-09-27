package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// AdminEjercicioDetalleDTO — el editor de un ejercicio en la web (GP-085)
// Lo que se edita, en español y en inglés, más lo que solo se enseña: las imágenes,
// de dónde vino el ejercicio, el texto libre de equipo de la importación y en
// cuántas rutinas se usa.
// ============================================================
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminEjercicioDetalleDTO implements Serializable {

    private Integer id;
    private String nombre;
    private String nombreEn;
    private String descripcion;
    private String descripcionEn;
    private String instrucciones;
    private String instruccionesEn;
    private String grupoMuscular;
    private String musculoPrimario;
    private String musculoPrimarioEn;
    private String equipamiento;
    private String dificultad;
    private boolean activo;
    private boolean nombreRevisado;
    private String imagenUrl;
    private String imagenUrl2;
    // WGER, FREE_EXERCISE_DB o MANUAL.
    private String origen;
    // Texto libre de la importación, del que salió el equipamiento. Solo lectura.
    private String equipoNecesario;
    // Rutinas distintas, de cualquier usuario o del sistema, que lo incluyen.
    private long rutinas;
}
