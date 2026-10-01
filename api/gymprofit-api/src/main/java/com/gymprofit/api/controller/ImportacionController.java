package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.productooff.ImportacionFinDTO;
import com.gymprofit.api.dto.entity.productooff.ImportacionLoteDTO;
import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.productooff.ProductoOffService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// ============================================================
// ImportacionController — entrada de la importación semanal de productos (GP-164)
// Solo la usa el workflow importar-productos.yml (DEC-041). La cerradura no está
// aquí: ClaveImportacion la comprueba antes de leer el cuerpo.
// ============================================================
@RestController
@RequestMapping("/importacion/productos")
@Hidden
public class ImportacionController {

    private final ProductoOffService productoOffService;

    public ImportacionController(ProductoOffService productoOffService) {
        this.productoOffService = productoOffService;
    }

    @Operation(summary = "Guarda un lote de productos de Open Food Facts (solo la importación)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lote procesado",
                    content = @Content(schema = @Schema(implementation = ImportacionLoteDTO.class))),
            @ApiResponse(responseCode = "400", description = "Lote vacío o demasiado grande",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "403", description = "Sin la clave de importación",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping
    public ResponseEntity<ImportacionLoteDTO> importarLote(@RequestBody List<ProductoOffImportDTO> lote) {
        if (lote == null || lote.isEmpty() || lote.size() > ProductoOffService.MAX_LOTE) {
            throw new InvalidDataException("error.importacion.loteInvalido", ProductoOffService.MAX_LOTE);
        }
        return ResponseEntity.ok(productoOffService.importarLote(lote));
    }

    @Operation(summary = "Cierra la importación: cuántos productos hay y cuánto ocupan")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de la tabla",
                    content = @Content(schema = @Schema(implementation = ImportacionFinDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin la clave de importación",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/fin")
    public ResponseEntity<ImportacionFinDTO> cerrar() {
        return ResponseEntity.ok(productoOffService.cerrar());
    }
}
