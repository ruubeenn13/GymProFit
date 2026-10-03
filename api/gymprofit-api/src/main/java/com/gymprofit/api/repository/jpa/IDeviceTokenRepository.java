package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============================================================
// IDeviceTokenRepository — repositorio JPA de tokens FCM de dispositivo.
// Acceso a los tokens push: búsqueda por valor, por usuario y borrado del
// token (para reasignar o limpiar tokens muertos).
// ============================================================
@Repository
public interface IDeviceTokenRepository extends JpaRepository<DeviceToken, Integer> {

    // Busca un token de dispositivo por su valor FCM.
    Optional<DeviceToken> findByToken(String token);

    // Devuelve todos los tokens de dispositivo de un usuario (multi-dispositivo).
    List<DeviceToken> findByUsuarioId(Integer usuarioId);

    // Borra un token concreto (token muerto reportado por FCM, o al hacer logout).
    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.token = :token")
    void deleteByToken(@Param("token") String token);

    // Token más reciente de un usuario (por fecha de actualización): se usa para
    // resolver el idioma actual del usuario en las notificaciones programadas
    // (el dispositivo usado más recientemente marca el idioma vigente).
    Optional<DeviceToken> findTopByUsuarioIdOrderByFechaActualizacionDesc(Integer usuarioId);

    // Ids de usuario distintos con al menos un dispositivo registrado y ese tipo de aviso
    // encendido (GP-112): los recordatorios programados solo se generan para ellos (sin
    // dispositivo, además, serían notificaciones para cuentas que nunca recibirán la push).
    @Query("SELECT DISTINCT d.usuario.id FROM DeviceToken d WHERE d.usuario.avisosEntrenar = true")
    List<Integer> findUsuarioIdsConAvisosEntrenar();

    @Query("SELECT DISTINCT d.usuario.id FROM DeviceToken d WHERE d.usuario.avisosComidas = true")
    List<Integer> findUsuarioIdsConAvisosComidas();

    @Query("SELECT DISTINCT d.usuario.id FROM DeviceToken d WHERE d.usuario.avisosProgreso = true")
    List<Integer> findUsuarioIdsConAvisosProgreso();
}
