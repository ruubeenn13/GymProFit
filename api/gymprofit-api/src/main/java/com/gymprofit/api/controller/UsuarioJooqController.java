package com.gymprofit.api.controller;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.jooq.UsuarioJooqDTO;
import com.gymprofit.api.repository.jooq.usuario.IUsuarioJooqRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ============================================================
// UsuarioJooqController — controlador REST de consultas avanzadas con JOOQ
// Expone endpoints de solo lectura, restringidos a ADMIN, que usan JOOQ
// (en vez de JPA) para consultas dinámicas/complejas sobre usuarios:
// filtros por nivel, edad y búsqueda combinada. Cada una devuelve solo la cuenta de quien
// llama (GP-198): las demás se gestionan desde /admin, que no enseña sus datos de salud.
// ============================================================
@RestController
@RequestMapping("")
@AllArgsConstructor
@Tag(name = "Usuario JOOQ Controlador", description = "Consultas avanzadas de usuarios con JOOQ")
public class UsuarioJooqController {

    // Repositorio JOOQ con las consultas dinámicas sobre usuarios
    private final IUsuarioJooqRepository usuarioJooqRepository;

    private final SecurityUtils securityUtils;

    // La cuenta de quien llama mediante JOOQ (solo ADMIN, GP-198)
    @Operation(summary = "La cuenta de quien llama con JOOQ (solo ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de usuarios")
    })
    @GetMapping("/jooq/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioJooqDTO>> findAll() {
        return ResponseEntity.ok(propia(usuarioJooqRepository.findAll()));
    }

    // Devuelve los usuarios activos mediante JOOQ (solo ADMIN)
    @Operation(summary = "La cuenta de quien llama, si está activa, con JOOQ (solo ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de usuarios activos")
    })
    @GetMapping("/jooq/usuarios/activos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioJooqDTO>> findActivos() {
        return ResponseEntity.ok(propia(usuarioJooqRepository.findActivos()));
    }

    // Filtra usuarios por nivel de experiencia mediante JOOQ (solo ADMIN)
    @Operation(summary = "La cuenta de quien llama si tiene ese nivel, con JOOQ (solo ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuarios encontrados")
    })
    @GetMapping("/jooq/usuarios/nivel/{nivelExperiencia}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioJooqDTO>> findByNivelExperiencia(@PathVariable String nivelExperiencia) {
        return ResponseEntity.ok(propia(usuarioJooqRepository.findByNivelExperiencia(nivelExperiencia)));
    }

    // Filtra usuarios por rango de edad mediante JOOQ (solo ADMIN)
    @Operation(summary = "La cuenta de quien llama si su edad está en el rango, con JOOQ (solo ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuarios encontrados")
    })
    @GetMapping("/jooq/usuarios/edad")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioJooqDTO>> findByEdadBetween(@RequestParam Integer edadMin,
                                                                  @RequestParam Integer edadMax) {
        return ResponseEntity.ok(propia(usuarioJooqRepository.findByEdadBetween(edadMin, edadMax)));
    }

    // Búsqueda combinada dinámica (username/nivel/edad máx) mediante JOOQ (solo ADMIN)
    @Operation(summary = "La cuenta de quien llama si cumple los filtros, con JOOQ (solo ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuarios encontrados")
    })
    @GetMapping("/jooq/usuarios/busqueda")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioJooqDTO>> busquedaAvanzada(@RequestParam(required = false) String username,
                                                                 @RequestParam(required = false) String nivelExperiencia,
                                                                 @RequestParam(required = false) Integer edadMax) {
        return ResponseEntity.ok(propia(usuarioJooqRepository.busquedaAvanzada(username, nivelExperiencia, edadMax)));
    }

    // Solo la cuenta de quien llama (GP-198).
    private List<UsuarioJooqDTO> propia(List<UsuarioJooqDTO> usuarios) {
        Integer yo = securityUtils.getCurrentUserId();
        return usuarios.stream().filter(u -> yo.equals(u.getId())).toList();
    }
}
