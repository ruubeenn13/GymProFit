package com.gymprofit.api.controller;

import com.gymprofit.api.dto.admin.AdminEstadisticasDTO;
import com.gymprofit.api.dto.admin.AdminRutinaDTO;
import com.gymprofit.api.dto.admin.AdminUsuarioDTO;
import com.gymprofit.api.dto.jooq.AlimentoJooqDTO;
import com.gymprofit.api.dto.jooq.EjercicioJooqDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.alimento.IAlimentoService;
import com.gymprofit.api.service.ejercicio.IEjercicioService;
import com.gymprofit.api.service.rutina.IRutinaService;
import com.gymprofit.api.service.usuario.IUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// AdminController — endpoints del panel de administración (ADMIN)
// Expone operaciones de gestión de usuarios, rutinas predefinidas,
// ejercicios y alimentos (búsquedas incluyendo inactivos) y las
// estadísticas globales de la aplicación GymProFit.
// ============================================================
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Panel de administración")
public class AdminController {

    private final IUsuarioService usuarioService;
    private final IRutinaService rutinaService;
    private final IEjercicioService ejercicioService;
    private final IAlimentoService alimentoService;

    // ─── Usuarios ────────────────────────────────────────────────────────────

    @Operation(summary = "Lista usuarios con filtros dinámicos (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Listado de usuarios",
            content = @Content(schema = @Schema(implementation = AdminUsuarioDTO.class)))
    @GetMapping("/usuarios")
    public ResponseEntity<List<AdminUsuarioDTO>> getUsuarios(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(usuarioService.getUsuariosAdmin(activo, rol, username, page, size));
    }

    @Operation(summary = "Activa o desactiva un usuario (ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado del usuario actualizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PatchMapping("/usuarios/{id}/toggle-activo")
    public ResponseEntity<Map<String, Object>> toggleActivoUsuario(@PathVariable Integer id) {
        usuarioService.toggleActivo(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("mensaje", "Estado activo del usuario " + id + " actualizado correctamente");
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Cambia el rol de un usuario (ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol actualizado"),
            @ApiResponse(responseCode = "400", description = "Rol inválido",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "Usuario o rol no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PatchMapping("/usuarios/{id}/rol")
    public ResponseEntity<Map<String, Object>> cambiarRolUsuario(
            @PathVariable Integer id,
            @RequestParam String nuevoRol) {
        usuarioService.cambiarRol(id, nuevoRol);
        Map<String, Object> resp = new HashMap<>();
        resp.put("mensaje", "Rol del usuario " + id + " cambiado a " + nuevoRol.toUpperCase());
        return ResponseEntity.ok(resp);
    }

    // ─── Rutinas predefinidas ─────────────────────────────────────────────────

    @Operation(summary = "Búsqueda de rutinas predefinidas con filtros (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Rutinas predefinidas encontradas",
            content = @Content(schema = @Schema(implementation = AdminRutinaDTO.class)))
    @GetMapping("/rutinas/predefinidas/busqueda")
    public ResponseEntity<List<AdminRutinaDTO>> buscarRutinasPredefinidas(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String nivel,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean activa) {

        return ResponseEntity.ok(
                rutinaService.busquedaRutinasPredefinidas(nombre, nivel, categoria, activa));
    }

    // ─── Ejercicios ───────────────────────────────────────────────────────────

    @Operation(summary = "Búsqueda de ejercicios con filtros incluyendo inactivos (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Ejercicios encontrados",
            content = @Content(schema = @Schema(implementation = EjercicioJooqDTO.class)))
    @GetMapping("/ejercicios/busqueda")
    public ResponseEntity<List<EjercicioJooqDTO>> buscarEjerciciosAdmin(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String grupoMuscular,
            @RequestParam(required = false) String dificultad,
            @RequestParam(required = false) Boolean activo) {

        return ResponseEntity.ok(
                ejercicioService.busquedaAdmin(nombre, grupoMuscular, dificultad, activo));
    }

    // ─── Alimentos ────────────────────────────────────────────────────────────

    @Operation(summary = "Búsqueda de alimentos con filtros incluyendo inactivos (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Alimentos encontrados",
            content = @Content(schema = @Schema(implementation = AlimentoJooqDTO.class)))
    @GetMapping("/alimentos/busqueda")
    public ResponseEntity<List<AlimentoJooqDTO>> buscarAlimentosAdmin(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean activo) {

        return ResponseEntity.ok(
                alimentoService.busquedaAdmin(nombre, categoria, activo));
    }

    // ─── Estadísticas ─────────────────────────────────────────────────────────

    @Operation(summary = "Estadísticas globales de la aplicación (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Estadísticas globales",
            content = @Content(schema = @Schema(implementation = AdminEstadisticasDTO.class)))
    @GetMapping("/estadisticas-globales")
    public ResponseEntity<AdminEstadisticasDTO> getEstadisticasGlobales() {
        return ResponseEntity.ok(usuarioService.getEstadisticasGlobales());
    }
}
