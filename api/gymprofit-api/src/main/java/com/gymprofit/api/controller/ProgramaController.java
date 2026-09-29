package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.programa.ProgramaDTO;
import com.gymprofit.api.dto.entity.programa.ProgramaDetalleDTO;
import com.gymprofit.api.dto.entity.programa.ProgramaQueSigueDTO;
import com.gymprofit.api.dto.entity.programa.ProgramaSeguidoDTO;
import com.gymprofit.api.dto.entity.programa.RecomendadoDTO;
import com.gymprofit.api.dto.entity.programa.VistaPreviaDTO;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @Operation(summary = "El programa recomendado",
            description = "El de la tabla del catálogo para el nivel del perfil del token, con el nivel usado y, " +
                    "si no es el obvio, el porqué en el idioma de la petición. Sin nivel en el perfil, como " +
                    "principiante; AVANZADO y EXPERTO, como intermedio.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "El recomendado"),
            @ApiResponse(responseCode = "400", description = "Falta el equipamiento o los días no van de 2 a 6",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/recomendado")
    public ResponseEntity<RecomendadoDTO> recomendado(@RequestParam(required = false) String equipamiento,
                                                      @RequestParam(required = false) Integer dias) {
        return ResponseEntity.ok(programaService.recomendado(equipamiento, dias));
    }

    @Operation(summary = "El programa que sigue el usuario",
            description = "Sus minutos, su ciclo con las rutinas del usuario y la posición que toca hoy. " +
                    "204 si no sigue ninguno.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "El programa que sigue"),
            @ApiResponse(responseCode = "204", description = "No sigue ninguno")
    })
    @GetMapping("/seguido")
    public ResponseEntity<ProgramaQueSigueDTO> seguido() {
        return programaService.seguido().map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Dejar el programa que sigue",
            description = "Le pone fecha de fin y desactiva sus rutinas; sesiones y récords se quedan. " +
                    "204 también si no seguía ninguno.")
    @ApiResponse(responseCode = "204", description = "Hecho")
    @DeleteMapping("/seguido")
    public ResponseEntity<Void> dejar() {
        programaService.dejarSeguido();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Vista previa de seguir un programa",
            description = "Cada rutina como quedaría al seguirlo con esos minutos y el perfil del token, con las " +
                    "mismas reglas, sin guardar nada: duración, ejercicios, los que se quitan, las series de los " +
                    "básicos si cambian y los ajustes del perfil que se aplican.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "La vista previa"),
            @ApiResponse(responseCode = "400", description = "Minutos no válidos",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "No hay programa con ese código",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/{codigo}/vista-previa")
    public ResponseEntity<VistaPreviaDTO> vistaPrevia(@PathVariable String codigo,
                                                      @RequestParam(required = false) Integer minutos) {
        return ResponseEntity.ok(programaService.vistaPrevia(codigo, minutos));
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
                    "Antes deja el que siguiera, como DELETE /programas/seguido; si es el mismo programa " +
                    "(cambiar el tiempo), el nuevo empieza en la posición que tocaba.")
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
