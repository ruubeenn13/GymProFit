package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.usuario.UsuarioCreateDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioUpdateDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.usuario.IUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.hibernate.engine.spi.VersionValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Tag(name = "Usuario Controlador", description = "Gestión de los usuarios")
public class UsuarioController {

    private final IUsuarioService usuarioService;

    @Operation(summary = "Obtiene todos los usuarios")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de usuarios",
                    content = @Content(schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuarios no encontrados",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener los usuarios",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioDTO>> findAll() {
        List<UsuarioDTO> usuarios = usuarioService.findAll();
        return ResponseEntity.ok(usuarios);
    }

    @Operation(summary = "Obtiene un usuario por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioDTO> obtenerUsuario(@PathVariable Integer id) {
        UsuarioDTO usuario = usuarioService.findById(id);
        return ResponseEntity.ok(usuario);
    }

    @Operation(summary = "Crea un nuevo usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario creado correctamente",
                    content = @Content(schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario duplicado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioDTO> guardarUsuario(@Valid @RequestBody UsuarioCreateDTO usuarioCreateDTO) {
        UsuarioDTO usuario = usuarioService.save(usuarioCreateDTO);
        return ResponseEntity.ok(usuario);
    }

    @Operation(summary = "Modifica un usuario existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario modificado correctamente",
                    content = @Content(schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "400", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/usuarios")
    public ResponseEntity<UsuarioDTO> modificarUsuario(@Valid @RequestBody UsuarioUpdateDTO usuarioUpdateDTO) {
        UsuarioDTO usuario = usuarioService.modify(usuarioUpdateDTO);
        return ResponseEntity.ok(usuario);
    }

    @Operation(summary = "Elimina un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Map<String, Object>> borrarUsuario(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            usuarioService.deleteById(id);
            respuesta.put("meensaje", "Usuario elimnado con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al borrar el usuario " + id);
            respuesta.put("error", e.getMessage());
            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Busca un usuario por su nombre")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/usuarios/username/{username}")
    public ResponseEntity<UsuarioDTO> obtenerUsuarioPorUsername(@PathVariable String username) {
        UsuarioDTO usuario = usuarioService.findByUsername(username);
        return ResponseEntity.ok(usuario);
    }

    @Operation(summary = "Busca un usuario por email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/usuarios/email/{email}")
    public ResponseEntity<UsuarioDTO> obtenerUsuarioPorEmail(@PathVariable String email) {
        UsuarioDTO usuario = usuarioService.findByEmail(email);
        return ResponseEntity.ok(usuario);
    }

    @Operation(summary = "Verifica si existe un username")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultado de la verificación")
    })
    @GetMapping("/usuarios/exists/username/{username}")
    public ResponseEntity<Map<String, Object>> existeUsername(@PathVariable String username) {
        Map<String, Object> respuesta = new HashMap<>();
        Boolean exists = usuarioService.existsByUsername(username);

        respuesta.put("exists", exists);
        respuesta.put("username", username);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Verifica si existe un email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultado de la verificación")
    })
    @GetMapping("/usuarios/exists/email/{email}")
    public ResponseEntity<Map<String, Object>> existeEmail(@PathVariable String email) {
        Map<String, Object> respuesta = new HashMap<>();
        Boolean exists = usuarioService.existsByEmail(email);

        respuesta.put("exists", exists);
        respuesta.put("email", email);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Obtiene todos los usuarios activos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de usuarios activos",
                    content = @Content(schema = @Schema(implementation = UsuarioDTO.class)))
    })
    @GetMapping("/usuarios/activos")
    public ResponseEntity<List<UsuarioDTO>> obtenerUsuariosActivos() {
        List<UsuarioDTO> usuarios = usuarioService.findActivos();
        return ResponseEntity.ok(usuarios);
    }
}
