package com.gymprofit.api.service.usuario;

import com.gymprofit.api.dto.entity.usuario.UsuarioCreateDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioUpdateDTO;

import java.util.List;

public interface IUsuarioService {

    List<UsuarioDTO> findAll();

    UsuarioDTO findById(Integer id);
    UsuarioDTO save(UsuarioCreateDTO usuarioCreateDTO);

    void deleteById(Integer id);
    void activateById(Integer id);
    void permanentDeleteById(Integer id);

    UsuarioDTO modify(UsuarioUpdateDTO usuarioUpdateDTO);
    UsuarioDTO findByUsername(String username);
    UsuarioDTO findByEmail(String email);

    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    List<UsuarioDTO> findActivos();
}
