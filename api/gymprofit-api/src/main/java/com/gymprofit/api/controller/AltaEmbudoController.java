package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.alta.PasoAltaDTO;
import com.gymprofit.api.service.alta.EmbudoAltaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ============================================================
// AltaEmbudoController — el embudo del alta (GP-019, DEC-064)
//
// Pública y sin token: la app avisa al llegar a cada pantalla del alta, antes de que
// haya cuenta. No lee la cuenta ni la IP (el cupo por IP lo pone AuthRateLimitFilter, en
// memoria, y no se guarda) y solo pasa el paso al servicio.
// ============================================================
@RestController
@RequestMapping("/alta/pasos")
@RequiredArgsConstructor
@Tag(name = "Alta", description = "Recuento anónimo del alta")
public class AltaEmbudoController {

    private final EmbudoAltaService servicio;

    @Operation(summary = "Cuenta que alguien ha llegado a un paso del alta, sin saber quién")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contado"),
            @ApiResponse(responseCode = "400", description = "Paso que no existe o sin paso"),
            @ApiResponse(responseCode = "429", description = "Demasiados pasos desde la misma IP")
    })
    @PostMapping
    public ResponseEntity<Void> contar(@Valid @RequestBody PasoAltaDTO pedido) {
        servicio.contar(pedido.paso());
        return ResponseEntity.noContent().build();
    }
}
