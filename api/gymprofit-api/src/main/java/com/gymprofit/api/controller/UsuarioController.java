package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.usuario.UsuarioCreateDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioEstadisticasDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioPatchDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioUpdateDTO;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// UsuarioController — controlador REST de gestión de usuarios
// Expone endpoints CRUD, activación/desactivación, búsquedas por
// username/email, foto de perfil y estadísticas de entrenamiento de
// los usuarios registrados en GymProFit.
// ============================================================
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("")
@AllArgsConstructor
@Tag(name = "Usuario Controlador", description = "Gestión de los usuarios")
public class UsuarioController {

    // Servicio con la lógica de negocio de usuarios
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
    // Devuelve todos los usuarios registrados
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
    // Busca un usuario por su ID
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
    // Crea un nuevo usuario (alta administrativa, distinta del registro público)
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
    // Actualiza por completo los datos de un usuario existente
    @PutMapping("/usuarios")
    public ResponseEntity<UsuarioDTO> modificarUsuario(@Valid @RequestBody UsuarioUpdateDTO usuarioUpdateDTO) {
        UsuarioDTO usuario = usuarioService.modify(usuarioUpdateDTO);

        return ResponseEntity.ok(usuario);
    }

    @Operation(summary = "Desactiva un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario desactivado correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Desactiva (baja lógica) un usuario sin eliminarlo de la base de datos
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Map<String, Object>> borrarUsuario(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            usuarioService.deleteById(id);

            respuesta.put("mensaje", "Usuario desactivado con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al desactivar el usuario " + id);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Activa un usuario desactivado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario activado correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Reactiva un usuario previamente desactivado
    @PutMapping("/usuarios/{id}/activar")
    public ResponseEntity<Map<String, Object>> activarUsuario(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            usuarioService.activateById(id);

            respuesta.put("mensaje", "Usuario activado con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al activar el usuario " + id);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Elimina permanentemente un usuario de la base de datos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario eliminado permanentemente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Borra definitivamente un usuario y sus datos asociados (irreversible)
    @DeleteMapping("/usuarios/{id}/permanente")
    public ResponseEntity<Map<String, Object>> eliminarPermanente(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            usuarioService.permanentDeleteById(id);

            respuesta.put("mensaje", "Usuario eliminado PERMANENTEMENTE con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al eliminar permanentemente el usuario " + id);
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
    // Busca un usuario por su nombre de usuario, validando que no esté vacío
    @GetMapping("/usuarios/username/{username}")
    public ResponseEntity<UsuarioDTO> obtenerUsuarioPorUsername(@PathVariable String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new InvalidDataException("El username no puede estar vacío");
        }

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
    // Busca un usuario por su email, validando que no esté vacío
    @GetMapping("/usuarios/email/{email}")
    public ResponseEntity<UsuarioDTO> obtenerUsuarioPorEmail(@PathVariable String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidDataException("El email no puede estar vacío");
        }

        UsuarioDTO usuario = usuarioService.findByEmail(email);

        return ResponseEntity.ok(usuario);
    }

    @Operation(summary = "Verifica si existe un username")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultado de la verificación")
    })
    // Comprueba si un username ya está en uso (validación previa al registro)
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
    // Comprueba si un email ya está en uso (validación previa al registro)
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
                    content = @Content(schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron usuarios activos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Devuelve únicamente los usuarios activos (no dados de baja)
    @GetMapping("/usuarios/activos")
    public ResponseEntity<List<UsuarioDTO>> obtenerUsuariosActivos() {
        List<UsuarioDTO> usuarios = usuarioService.findActivos();

        if (usuarios.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron usuarios activos");
        }

        return ResponseEntity.ok(usuarios);
    }

    @Operation(summary = "Actualiza parcialmente un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado",
                    content = @Content(schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Actualización parcial de campos de un usuario (usado, entre otros, por el onboarding)
    @PatchMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioDTO> patchUsuario(@PathVariable Integer id, @RequestBody UsuarioPatchDTO patchDTO) {
        return ResponseEntity.ok(usuarioService.patch(id, patchDTO));
    }

    @Operation(summary = "Sube la foto de perfil de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Foto subida correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping(value = "/usuarios/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // Sube o reemplaza la foto de perfil del usuario (multipart/form-data)
    public ResponseEntity<UsuarioDTO> subirFotoPerfil(@PathVariable Integer id,
                                                      @RequestParam("foto") MultipartFile foto) {
        return ResponseEntity.ok(usuarioService.uploadFotoPerfil(id, foto));
    }

    @Operation(summary = "Obtiene la foto de perfil de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Foto encontrada"),
            @ApiResponse(responseCode = "404", description = "Foto no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Devuelve la foto de perfil del usuario como imagen JPEG
    @GetMapping("/usuarios/{id}/foto")
    public ResponseEntity<byte[]> getFotoPerfil(@PathVariable Integer id) {
        byte[] bytes = usuarioService.getFotoPerfil(id);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(bytes);
    }

    @Operation(summary = "Obtiene las estadísticas de entrenamiento de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas correctamente",
                    content = @Content(schema = @Schema(implementation = UsuarioEstadisticasDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Calcula y devuelve estadísticas agregadas de entrenamiento del usuario
    @GetMapping("/usuarios/{id}/estadisticas")
    public ResponseEntity<UsuarioEstadisticasDTO> getEstadisticas(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.getEstadisticas(id));
    }
}