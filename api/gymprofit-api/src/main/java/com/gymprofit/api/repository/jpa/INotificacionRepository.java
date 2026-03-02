package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Notificacion;
import com.gymprofit.api.enums.TipoNotificacion;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Hidden
@Repository
public interface INotificacionRepository extends CrudRepository<Notificacion, Integer> {

    List<Notificacion> findByUsuarioId(Integer usuarioId);

    List<Notificacion> findByUsuarioIdAndLeidaFalse(Integer usuarioId);

    List<Notificacion> findByUsuarioIdAndTipo(Integer usuarioId, TipoNotificacion tipo);

    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);
}
