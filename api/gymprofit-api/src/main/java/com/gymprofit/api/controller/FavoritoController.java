package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.dto.entity.alimento.FavoritosDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.favorito.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ============================================================
// FavoritoController — los favoritos de la cuenta del token (lote 1.6.3)
// Marcar y desmarcar por id, sin cuerpo y repetibles; marcar por código un producto
// que aún no está en el catálogo; la lista, con su propuesta; y rechazar la propuesta.
// Solo USER y ADMIN: el invitado no tiene favoritos, como no añade (SecurityConfig).
// ============================================================
@RestController
@RequestMapping("/favoritos")
@AllArgsConstructor
@Tag(name = "Favoritos", description = "Los alimentos favoritos de la cuenta")
public class FavoritoController {

    private final FavoritoService favoritoService;

    @Operation(summary = "Los favoritos de la cuenta, por uso, y la propuesta de uno nuevo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Favoritos y propuesta (o null)",
                    content = @Content(schema = @Schema(implementation = FavoritosDTO.class))),
            @ApiResponse(responseCode = "403", description = "Invitado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping
    public ResponseEntity<FavoritosDTO> lista() {
        return ResponseEntity.ok(favoritoService.lista());
    }

    @Operation(summary = "Marca un alimento como favorito (repetible)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "El alimento, con favorito a true",
                    content = @Content(schema = @Schema(implementation = AlimentoDTO.class))),
            @ApiResponse(responseCode = "403", description = "No existe o es propio de otra cuenta",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/{alimentoId}")
    public ResponseEntity<AlimentoDTO> marcar(@PathVariable Integer alimentoId) {
        return ResponseEntity.ok(favoritoService.marcar(alimentoId));
    }

    @Operation(summary = "Marca por su código un producto, materializándolo si hace falta (repetible)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "El alimento con su id, con favorito a true",
                    content = @Content(schema = @Schema(implementation = AlimentoDTO.class))),
            @ApiResponse(responseCode = "400", description = "El código no son solo cifras",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "No existe en ninguna parte",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "503", description = "Sin cupo de lecturas a Open Food Facts (Retry-After)",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/codigo/{codigo}")
    public ResponseEntity<AlimentoDTO> marcarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(favoritoService.marcarPorCodigo(codigo));
    }

    @Operation(summary = "Quita un favorito (repetible)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ya no es favorito"),
            @ApiResponse(responseCode = "403", description = "No existe o es propio de otra cuenta",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/{alimentoId}")
    public ResponseEntity<Void> quitar(@PathVariable Integer alimentoId) {
        favoritoService.quitar(alimentoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Rechaza la propuesta de ese alimento: no se vuelve a proponer (repetible)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Rechazada"),
            @ApiResponse(responseCode = "403", description = "No existe o es propio de otra cuenta",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/rechazados/{alimentoId}")
    public ResponseEntity<Void> rechazar(@PathVariable Integer alimentoId) {
        favoritoService.rechazar(alimentoId);
        return ResponseEntity.noContent().build();
    }
}
