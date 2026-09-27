package com.gymprofit.api.controller;

import com.gymprofit.api.dto.admin.AdminEjercicioDTO;
import com.gymprofit.api.dto.admin.AdminEjercicioDetalleDTO;
import com.gymprofit.api.dto.admin.AdminEjercicioUpdateDTO;
import com.gymprofit.api.dto.admin.AdminEjerciciosResumenDTO;
import com.gymprofit.api.dto.common.PageDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.admin.IAdminEjercicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ============================================================
// AdminEjercicioController — el catálogo de ejercicios en la web (GP-085)
// Rutas nuevas, solo ADMIN (regla /admin/**). /admin/ejercicios/busqueda, la del
// panel de la app, no cambia: aquí va paginado y con los campos de revisión.
// ============================================================
@RestController
@RequestMapping("/admin/ejercicios")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Panel de administración")
public class AdminEjercicioController {

    private final IAdminEjercicioService adminEjercicioService;

    @Operation(summary = "Ejercicios paginados, con búsqueda en los dos idiomas y filtros (ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de ejercicios, con el total"),
            @ApiResponse(responseCode = "400", description = "Grupo o equipamiento desconocido",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping
    public ResponseEntity<PageDTO<AdminEjercicioDTO>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) String equipamiento,
            @RequestParam(defaultValue = "false") boolean sinRevisar,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminEjercicioService.listar(q, grupo, equipamiento, sinRevisar, page, size));
    }

    @Operation(summary = "Activos, sin revisar y opciones de equipamiento (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Resumen del catálogo de ejercicios")
    @GetMapping("/resumen")
    public ResponseEntity<AdminEjerciciosResumenDTO> resumen() {
        return ResponseEntity.ok(adminEjercicioService.resumen());
    }

    @Operation(summary = "Un ejercicio para el editor, con en cuántas rutinas se usa (ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio"),
            @ApiResponse(responseCode = "404", description = "No existe",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AdminEjercicioDetalleDTO> detalle(@PathVariable Integer id) {
        return ResponseEntity.ok(adminEjercicioService.detalle(id));
    }

    @Operation(summary = "Guarda un ejercicio entero (ADMIN)",
            description = "Nombres, descripciones e instrucciones en español e inglés, grupo, músculo, "
                    + "equipamiento, dificultad, activo y nombre revisado. Un nombre en español distinto "
                    + "del inglés lo marca como revisado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio guardado"),
            @ApiResponse(responseCode = "400", description = "Datos no válidos",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "No existe",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<AdminEjercicioDetalleDTO> guardar(@PathVariable Integer id,
                                                            @Valid @RequestBody AdminEjercicioUpdateDTO dto) {
        return ResponseEntity.ok(adminEjercicioService.guardar(id, dto));
    }
}
