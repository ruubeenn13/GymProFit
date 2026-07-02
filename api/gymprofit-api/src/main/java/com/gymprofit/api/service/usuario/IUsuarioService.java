package com.gymprofit.api.service.usuario;

import com.gymprofit.api.dto.admin.AdminEstadisticasDTO;
import com.gymprofit.api.dto.admin.AdminUsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioCreateDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioEstadisticasDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioUpdateDTO;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

// ============================================================
// IUsuarioService — contrato del servicio de usuarios.
// Extiende UserDetailsService para integrarse con Spring Security (login),
// y define el CRUD de usuarios más operaciones administrativas (estadísticas,
// activar/desactivar, cambiar rol, foto de perfil).
// ============================================================
public interface IUsuarioService extends UserDetailsService {

    // Lista todos los usuarios del sistema.
    List<UsuarioDTO> findAll();

    // Busca un usuario por su id.
    UsuarioDTO findById(Integer id);

    // Crea un nuevo usuario.
    UsuarioDTO save(UsuarioCreateDTO usuarioCreateDTO);

    // Sustituye los datos de un usuario existente.
    UsuarioDTO modify(UsuarioUpdateDTO usuarioUpdateDTO);

    // Baja lógica de un usuario (desactiva la cuenta).
    void deleteById(Integer id);

    // Reactiva un usuario previamente desactivado.
    void activateById(Integer id);

    // Elimina permanentemente un usuario de la base de datos.
    void permanentDeleteById(Integer id);

    // Busca un usuario por username.
    UsuarioDTO findByUsername(String username);

    // Busca un usuario por email.
    UsuarioDTO findByEmail(String email);

    // Comprueba si ya existe un usuario con ese username.
    Boolean existsByUsername(String username);

    // Comprueba si ya existe un usuario con ese email.
    Boolean existsByEmail(String email);

    // Lista los usuarios activos.
    List<UsuarioDTO> findActivos();

    // Actualiza parcialmente un usuario con los campos no nulos del patch.
    UsuarioDTO patch(Integer id, com.gymprofit.api.dto.entity.usuario.UsuarioPatchDTO patchDTO);

    // Obtiene las estadísticas personales de un usuario (entrenamientos, progreso...).
    UsuarioEstadisticasDTO getEstadisticas(Integer usuarioId);

    // Lista usuarios paginados con filtros para el panel de administración.
    List<AdminUsuarioDTO> getUsuariosAdmin(Boolean activo, String rol, String username, int page, int size);

    // Obtiene estadísticas globales de la aplicación (uso administrativo).
    AdminEstadisticasDTO getEstadisticasGlobales();

    // Alterna el estado activo/inactivo de un usuario (uso administrativo).
    void toggleActivo(Integer id);

    // Cambia el rol de un usuario (uso administrativo).
    void cambiarRol(Integer id, String nuevoRol);

    // Sube y asocia una foto de perfil al usuario.
    UsuarioDTO uploadFotoPerfil(Integer id, org.springframework.web.multipart.MultipartFile file);

    // Obtiene los bytes de la foto de perfil de un usuario.
    byte[] getFotoPerfil(Integer id);
}