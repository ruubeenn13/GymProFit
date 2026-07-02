package com.gymprofit.api.controller;

import com.gymprofit.api.dto.common.CountDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoCreateDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoPatchDTO;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.alimento.IAlimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// AlimentoController — CRUD y consultas del catálogo de alimentos
// Gestiona alta, modificación, activación/desactivación (borrado lógico)
// y borrado permanente de alimentos, así como búsquedas por nombre,
// categoría, calorías, usuario y los conteos usados en nutrición.
// ============================================================
@RestController
@RequestMapping("")
@AllArgsConstructor
@Tag(name = "Alimento Controlador", description = "Gestión de los alimentos")
public class AlimentoController {

    private final IAlimentoService alimentoService;

    @Operation(summary = "Obtiene todos los alimentos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de alimentos",
                    content = @Content(schema = @Schema(implementation = AlimentoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Alimentos no encontrados",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener los alimentos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/alimentos")
    public ResponseEntity<List<AlimentoDTO>> findAll() {
        List<AlimentoDTO> alimentos = alimentoService.findAll();

        return ResponseEntity.ok(alimentos);
    }

    @Operation(summary = "Obtiene un alimento por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimento encontrado",
                    content = @Content(schema = @Schema(implementation = AlimentoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Alimento no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/alimentos/{id}")
    public ResponseEntity<AlimentoDTO> obtenerAlimento(@PathVariable Integer id) {
        AlimentoDTO alimento = alimentoService.findById(id);

        return ResponseEntity.ok(alimento);
    }

    @Operation(summary = "Crea un nuevo alimento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimento creado correctamente",
                    content = @Content(schema = @Schema(implementation = AlimentoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/alimentos")
    public ResponseEntity<AlimentoDTO> guardarAlimento(@Valid @RequestBody AlimentoCreateDTO alimentoCreateDTO) {
        AlimentoDTO alimento = alimentoService.save(alimentoCreateDTO);

        return ResponseEntity.ok(alimento);
    }

    @Operation(summary = "Modifica un alimento existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimento modificado correctamente",
                    content = @Content(schema = @Schema(implementation = AlimentoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Alimento no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/alimentos")
    public ResponseEntity<AlimentoDTO> modificarAlimento(@Valid @RequestBody AlimentoDTO alimentoDTO) {
        AlimentoDTO alimento = alimentoService.modify(alimentoDTO);

        return ResponseEntity.ok(alimento);
    }

    @Operation(summary = "Desactiva un alimento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimento desactivado correctamente"),
            @ApiResponse(responseCode = "404", description = "Alimento no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/alimentos/{id}")
    public ResponseEntity<Map<String, Object>> borrarAlimentos(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        alimentoService.deleteById(id);

        respuesta.put("mensaje", "Alimento desactivado con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Activa un alimento desactivado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimento activado correctamente"),
            @ApiResponse(responseCode = "404", description = "Alimento no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/alimentos/{id}/activar")
    public ResponseEntity<Map<String, Object>> activarAlimento(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        alimentoService.activateById(id);

        respuesta.put("mensaje", "Alimento activado con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Elimina permanentemente un alimento de la base de datos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimento eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Alimento no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/alimentos/{id}/permanente")
    public ResponseEntity<Map<String, Object>> eliminarPermanentemente(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        alimentoService.permanentDeleteById(id);

        respuesta.put("mensaje", "Alimento eliminado permanentemente");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Busca alimentos por nombre")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimentos encontrados",
                    content = @Content(schema = @Schema(implementation = AlimentoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron alimentos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/alimentos/nombre/{nombre}")
    public ResponseEntity<List<AlimentoDTO>> obtenerAlimentosPorNombre(@PathVariable String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new InvalidDataException("El nombre no puede estar vacío");
        }

        List<AlimentoDTO> alimentos = alimentoService.findByNombre(nombre);

        if (alimentos.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron alimentos con el nombre: " + nombre);
        }

        return ResponseEntity.ok(alimentos);
    }

    // Lista canónica y fija de categorías válidas para clasificar un alimento.
    public static final List<String> CATEGORIAS = Arrays.asList(
            "Carnes y aves", "Pescado y marisco", "Huevos", "Lácteos", "Legumbres",
            "Cereales y pan", "Frutas", "Verduras", "Frutos secos", "Aceites y grasas",
            "Bebidas", "Suplementos", "Snacks", "Otro"
    );

    @Operation(summary = "Devuelve la lista canónica de categorías de alimentos")
    @ApiResponse(responseCode = "200", description = "Lista de categorías")
    @GetMapping("/alimentos/categorias")
    public ResponseEntity<List<String>> obtenerCategorias() {
        return ResponseEntity.ok(CATEGORIAS);
    }

    @Operation(summary = "Busca alimentos por categoría")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimentos encontrados",
                    content = @Content(schema = @Schema(implementation = AlimentoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron alimentos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/alimentos/categoria/{categoria}")
    public ResponseEntity<List<AlimentoDTO>> obtenerAlimentosPorCategoria(@PathVariable String categoria) {
        if (categoria == null || categoria.trim().isEmpty()) {
            throw new InvalidDataException("La categoría no puede estar vacía");
        }

        List<AlimentoDTO> alimentos = alimentoService.findByCategoria(categoria);

        if (alimentos.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron alimentos de la categoría: " + categoria);
        }

        return ResponseEntity.ok(alimentos);
    }

    @Operation(summary = "Obtiene todos los alimentos activos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de alimentos activos",
                    content = @Content(schema = @Schema(implementation = AlimentoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron alimentos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/alimentos/activos")
    public ResponseEntity<List<AlimentoDTO>> obtenerAlimentosActivos() {
        List<AlimentoDTO> alimentos = alimentoService.findActivos();

        if (alimentos.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron alimentos activos");
        }

        return ResponseEntity.ok(alimentos);
    }

    @Operation(summary = "Busca alimentos por rango de calorías")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimentos encontrados",
                    content = @Content(schema = @Schema(implementation = AlimentoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Rango de calorías inválido",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron alimentos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/alimentos/calorias")
    public ResponseEntity<List<AlimentoDTO>> obtenerAlimentosPorCalorias(@RequestParam Integer min,
                                                                         @RequestParam Integer max) {
        if (min == null || max == null) {
            throw new InvalidDataException("Los valores min y max son obligatorios");
        }

        if (min < 0 || max < 0) {
            throw new InvalidDataException("Las calorías no pueden ser negativas");
        }

        if (min > max) {
            throw new InvalidDataException("El valor mínimo no puede ser mayor que el máximo");
        }

        List<AlimentoDTO> alimentos = alimentoService.findByCaloriasBetween(min, max);

        if (alimentos.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron alimentos entre " + min + " calorías y " + max + " calorías");
        }

        return ResponseEntity.ok(alimentos);
    }

    @Operation(summary = "Cuenta los alimentos activos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cantidad de alimentos")
    })
    @GetMapping("/alimentos/count/activos")
    public ResponseEntity<CountDTO> contarAlimentosActivos() {
        Long count = alimentoService.countActivos();

        return ResponseEntity.ok(new CountDTO(count));
    }

    @Operation(summary = "Cuenta los alimentos por categoría")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cantidad de alimentos"),
            @ApiResponse(responseCode = "400", description = "Categoría vacía",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/alimentos/count/categoria/{categoria}")
    public ResponseEntity<CountDTO> contarAlimentosPorCategoria(@PathVariable String categoria) {
        if (categoria == null || categoria.trim().isEmpty()) {
            throw new InvalidDataException("La categoría no puede estar vacía");
        }

        Long count = alimentoService.countByCategoria(categoria);

        return ResponseEntity.ok(new CountDTO(count));
    }

    @Operation(summary = "Obtiene los alimentos de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimentos del usuario",
                    content = @Content(schema = @Schema(implementation = AlimentoDTO.class)))
    })
    @GetMapping("/alimentos/usuario/{usuarioId}")
    public ResponseEntity<List<AlimentoDTO>> obtenerAlimentosPorUsuario(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(alimentoService.findByUsuarioId(usuarioId));
    }

    @Operation(summary = "Actualiza parcialmente un alimento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimento actualizado",
                    content = @Content(schema = @Schema(implementation = AlimentoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Alimento no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PatchMapping("/alimentos/{id}")
    public ResponseEntity<AlimentoDTO> patchAlimento(@PathVariable Integer id, @RequestBody AlimentoPatchDTO patchDTO) {
        return ResponseEntity.ok(alimentoService.patch(id, patchDTO));
    }
}