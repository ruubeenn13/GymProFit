package com.gymprofit.api.service.usuario;

import com.gymprofit.api.dto.admin.AdminEstadisticasDTO;
import com.gymprofit.api.dto.admin.AdminUsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioCreateDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioEstadisticasDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioUpdateDTO;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface IUsuarioService extends UserDetailsService {

    List<UsuarioDTO> findAll();

    UsuarioDTO findById(Integer id);

    UsuarioDTO save(UsuarioCreateDTO usuarioCreateDTO);

    UsuarioDTO modify(UsuarioUpdateDTO usuarioUpdateDTO);

    void deleteById(Integer id);

    void activateById(Integer id);

    void permanentDeleteById(Integer id);

    UsuarioDTO findByUsername(String username);

    UsuarioDTO findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    List<UsuarioDTO> findActivos();

    UsuarioDTO patch(Integer id, com.gymprofit.api.dto.entity.usuario.UsuarioPatchDTO patchDTO);

    UsuarioEstadisticasDTO getEstadisticas(Integer usuarioId);

    List<AdminUsuarioDTO> getUsuariosAdmin(Boolean activo, String rol, String username, int page, int size);

    AdminEstadisticasDTO getEstadisticasGlobales();
}