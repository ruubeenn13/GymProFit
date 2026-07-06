package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
// ============================================================
// DeviceToken — token FCM de un dispositivo del usuario (push).
// Cada dispositivo (móvil) registra su token de Firebase Cloud Messaging para
// poder recibir notificaciones push. Un usuario puede tener varios (multi-dispositivo);
// el token es único: si Firebase lo reasigna, se actualiza el usuario propietario.
// ============================================================
@Table(name = "device_tokens")
public class DeviceToken {

    // Identificador autogenerado del registro.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Token FCM del dispositivo, único en la tabla.
    @Column(nullable = false, unique = true)
    private String token;

    // Usuario propietario del dispositivo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Plataforma del dispositivo (por ahora siempre ANDROID).
    @Column(nullable = false, length = 20)
    private String plataforma;

    // Idioma del usuario en este dispositivo (código ISO de 2 letras, p.ej. "es", "en").
    // Se usa para resolver el idioma de las notificaciones push generadas por el servidor.
    @Column(nullable = false, length = 5)
    private String idioma;

    // Momento del primer registro del token.
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    // Última vez que se refrescó/reasignó el token.
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}
