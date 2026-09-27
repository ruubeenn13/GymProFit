package com.gymprofit.api.dto.admin;

import com.gymprofit.api.enums.Dificultad;
import com.gymprofit.api.enums.Equipamiento;
import com.gymprofit.api.enums.GrupoMuscular;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============================================================
// AdminEjercicioUpdateDTO — lo que guarda el editor de ejercicios (GP-085)
// Un PUT: llega el ejercicio entero. Las imágenes y el texto de equipo de la
// importación no se editan desde aquí.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminEjercicioUpdateDTO {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @Size(max = 100)
    private String nombreEn;

    private String descripcion;
    private String descripcionEn;
    private String instrucciones;
    private String instruccionesEn;

    @NotNull
    private GrupoMuscular grupoMuscular;

    @Size(max = 60)
    private String musculoPrimario;

    @Size(max = 60)
    private String musculoPrimarioEn;

    @NotNull
    private Equipamiento equipamiento;

    @NotNull
    private Dificultad dificultad;

    @NotNull
    private Boolean activo;

    // «Se dice igual en español»: true con el nombre igual al inglés lo da por revisado.
    // Un nombre distinto del inglés cuenta como revisado se mande lo que se mande.
    @NotNull
    private Boolean nombreRevisado;
}
