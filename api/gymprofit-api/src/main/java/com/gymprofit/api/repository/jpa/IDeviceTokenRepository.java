package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.DeviceToken;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============================================================
// IDeviceTokenRepository — repositorio JPA de tokens FCM de dispositivo.
// Acceso a los tokens push: búsqueda por valor, por usuario y borrado del
// token (para reasignar o limpiar tokens muertos). No exportado como recurso REST.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IDeviceTokenRepository extends JpaRepository<DeviceToken, Integer> {

    // Busca un token de dispositivo por su valor FCM.
    Optional<DeviceToken> findByToken(String token);

    // Devuelve todos los tokens de dispositivo de un usuario (multi-dispositivo).
    List<DeviceToken> findByUsuarioId(Integer usuarioId);

    // Borra un token concreto (token muerto reportado por FCM, o al hacer logout).
    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.token = :token")
    void deleteByToken(@Param("token") String token);
}
