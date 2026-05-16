package com.gymprofit.api.repository.jooq.usuario;

import com.gymprofit.api.dto.entity.usuario.UsuarioEstadisticasDTO;
import com.gymprofit.api.dto.jooq.UsuarioJooqDTO;

import java.util.List;

public interface IUsuarioJooqRepository {

    List<UsuarioJooqDTO> findAll();

    List<UsuarioJooqDTO> findActivos();

    List<UsuarioJooqDTO> findByNivelExperiencia(String nivelExperiencia);

    List<UsuarioJooqDTO> findByEdadBetween(Integer edadMin, Integer edadMax);

    List<UsuarioJooqDTO> busquedaAvanzada(String username, String nivelExperiencia, Integer edadMax);

    UsuarioEstadisticasDTO getEstadisticas(Integer usuarioId);
}
