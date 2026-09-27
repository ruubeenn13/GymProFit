package com.gymprofit.api.controller;

import com.gymprofit.api.dto.admin.AdminCuentaDTO;
import com.gymprofit.api.dto.admin.AdminCuentaDetalleDTO;
import com.gymprofit.api.dto.admin.BorrarCuentaAdminDTO;
import com.gymprofit.api.dto.common.PageDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.admin.IAdminCuentaService;
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
// AdminCuentaController — cuentas de la web de administración (GP-085)
// Rutas nuevas bajo /admin/cuentas, solo ADMIN (regla /admin/** de SecurityConfig).
// Las de /admin/usuarios, que usa el panel de la app, no cambian de forma.
// ============================================================
@RestController
@RequestMapping("/admin/cuentas")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Panel de administración")
public class AdminCuentaController {

    private final IAdminCuentaService adminCuentaService;

    @Operation(summary = "Cuentas paginadas, con búsqueda por usuario o correo (ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de cuentas, con el total"),
            @ApiResponse(responseCode = "400", description = "Rol desconocido",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping
    public ResponseEntity<PageDTO<AdminCuentaDTO>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminCuentaService.listar(q, rol, activo, page, size));
    }

    @Operation(summary = "Ficha de una cuenta: sus datos y cuántas sesiones y comidas tiene (ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ficha"),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AdminCuentaDetalleDTO> detalle(@PathVariable Integer id) {
        return ResponseEntity.ok(adminCuentaService.detalle(id));
    }

    @Operation(summary = "Borra una cuenta a petición de su titular (ADMIN)",
            description = "El borrado de GP-008. La confirmación es el nombre de usuario de la cuenta. "
                    + "No vale para la propia cuenta ni para otra de administración.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cuenta borrada con todos sus datos"),
            @ApiResponse(responseCode = "400", description = "Confirmación distinta del nombre de usuario, o sin motivo",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "409", description = "Es la propia cuenta o una cuenta ADMIN",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable Integer id, @Valid @RequestBody BorrarCuentaAdminDTO dto) {
        adminCuentaService.borrarAPeticion(id, dto);
        return ResponseEntity.noContent().build();
    }
}
