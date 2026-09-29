package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.programa.ProgramaDTO;
import com.gymprofit.api.dto.entity.programa.ProgramaDetalleDTO;
import com.gymprofit.api.dto.entity.programa.ProgramaSeguidoDTO;
import com.gymprofit.api.dto.entity.programa.SeguirProgramaDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.programa.IProgramaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// ============================================================
// ProgramaController — programas del catálogo y seguirlos (GP-074, lote 1.2.0)
// Un programa es un plan de varios días; cada día, una rutina plantilla. Quien lo
// sigue se lleva una copia de cada rutina, ajustada a su perfil y a su tiempo.
// ============================================================
@RestController
@RequestMapping("/programas")
@AllArgsConstructor
@Tag(name = "Programa Controlador", description = "Programas del catálogo y seguirlos")
public class ProgramaController {

    private final IProgramaService programaService;

    @Operation(summary = "Programas del catálogo",
            description = "Filtros opcionales: equipamiento (GIMNASIO, MANCUERNAS, PESO_CORPORAL), días por " +
                    "semana (salen los programas que los admiten) y nivel (AVANZADO y EXPERTO dan los de intermedio).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Programas; [] si ninguno casa con los filtros"),
            @ApiResponse(responseCode = "400", description = "Filtro no válido",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping
    public ResponseEntity<List<ProgramaDTO>> listar(@RequestParam(required = false) String equipamiento,
                                                    @RequestParam(required = false) Integer dias,
                                                    @RequestParam(required = false) String nivel) {
        return ResponseEntity.ok(programaService.listar(equipamiento, dias, nivel));
    }

    @Operation(summary = "Un programa con su semana y cada rutina con sus ejercicios")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "El programa"),
            @ApiResponse(responseCode = "404", description = "No hay programa con ese código",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/{codigo}")
    public ResponseEntity<ProgramaDetalleDTO> detalle(@PathVariable String codigo) {
        return ResponseEntity.ok(programaService.detalle(codigo));
    }

    @Operation(summary = "Seguir un programa",
            description = "Crea para el usuario del token una copia de cada rutina distinta del programa, " +
                    "ajustada a su nivel, su objetivo y los minutos por sesión (30, 45, 60 o 75; 60 si no llegan). " +
                    "Seguirlo otra vez crea otras copias y no toca las que ya tiene.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lo creado"),
            @ApiResponse(responseCode = "400", description = "Minutos no válidos",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "No hay programa con ese código",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/{codigo}/seguir")
    public ResponseEntity<ProgramaSeguidoDTO> seguir(@PathVariable String codigo,
                                                     @RequestBody(required = false) SeguirProgramaDTO cuerpo) {
        Integer minutos = cuerpo == null ? null : cuerpo.getMinutos();
        return new ResponseEntity<>(programaService.seguir(codigo, minutos), HttpStatus.CREATED);
    }
}
