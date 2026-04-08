package com.gymprofit.api.controller;

import com.gymprofit.api.dto.jooq.EjercicioJooqDTO;
import com.gymprofit.api.repository.jooq.ejercicio.IEjercicioJooqRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Tag(name = "Ejercicio JOOQ Controlador", description = "Consultas avanzadas de ejercicios con JOOQ")
public class EjercicioJooqController {

    private final IEjercicioJooqRepository ejercicioJooqRepository;

    @Operation(summary = "Obtiene todos los ejercicios con JOOQ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de ejercicios")
    })
    @GetMapping("/jooq/ejercicios")
    @PreAuthorize("hasAnyRole('GUEST', 'USER', 'ADMIN')")
    public ResponseEntity<List<EjercicioJooqDTO>> findAll() {
        return ResponseEntity.ok(ejercicioJooqRepository.findAll());
    }

    @Operation(summary = "Obtiene todos los ejercicios con JOOQ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de ejercicios activos")
    })
    @GetMapping("/jooq/ejercicios/activos")
    @PreAuthorize("hasAnyRole('GUEST', 'USER', 'ADMIN')")
    public ResponseEntity<List<EjercicioJooqDTO>> findActivos() {
        return ResponseEntity.ok(ejercicioJooqRepository.findActivos());
    }

    @Operation(summary = "Busca ejercicios por grupo muscular y dificultad con JOOQ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios encontrados")
    })
    @GetMapping("/jooq/ejercicios/filtro/{grupoMuscular}/{dificultad}")
    @PreAuthorize("hasAnyRole('GUEST', 'USER', 'ADMIN')")
    public ResponseEntity<List<EjercicioJooqDTO>> findByGrupoMuscularAndDificultad(@PathVariable String grupoMuscular,
                                                                                   @PathVariable String dificultad) {
        return ResponseEntity.ok(ejercicioJooqRepository.findByGrupoMuscularAndDificultad(grupoMuscular, dificultad));
    }

    @Operation(summary = "Busca ejercicios por rango de calorías quemadas con JOOQ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios encontrados")
    })
    @GetMapping("/jooq/ejercicios/calorias")
    @PreAuthorize("hasAnyRole('GUEST', 'USER', 'ADMIN')")
    public ResponseEntity<List<EjercicioJooqDTO>> findByCaloriasQuemadasBetween(@RequestParam Integer min,
                                                                                @RequestParam Integer max) {
        return ResponseEntity.ok(ejercicioJooqRepository.findByCaloriasQuemadasBetween(min, max));
    }

    @Operation(summary = "Búsqueda avanzada dinámica de ejercicios con JOOQ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios encontrados")
    })
    @GetMapping("/jooq/ejercicios/busqueda")
    @PreAuthorize("hasAnyRole('GUEST', 'USER', 'ADMIN')")
    public ResponseEntity<List<EjercicioJooqDTO>> busquedaAvanzada(@RequestParam(required = false) String nombre,
                                                                   @RequestParam(required = false) String grupoMuscular,
                                                                   @RequestParam(required = false) String dificultad,
                                                                   @RequestParam(required = false) Integer caloriasMax) {
        return ResponseEntity.ok(ejercicioJooqRepository.busquedaAvanzada(nombre, grupoMuscular, dificultad, caloriasMax));
    }
}
