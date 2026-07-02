package com.gymprofit.api.entity;

import com.gymprofit.api.enums.TipoNotificacion;
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
// Notificacion — mensaje dirigido a un usuario dentro de la app
// Representa un aviso (recordatorio de entrenamiento, logro conseguido, etc.)
// que se muestra al usuario, con soporte para programación futura y control
// de estado de lectura.
// ============================================================
@Table(name = "notificaciones")
public class Notificacion {

    // Identificador autogenerado de la notificación.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Título breve de la notificación.
    @Column(nullable = false, length = 100)
    private String titulo;

    // Contenido/texto completo del mensaje.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    // Categoría de la notificación (recordatorio, logro, sistema, etc.).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo;

    // Fecha en la que se creó la notificación.
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    // Fecha en la que está programado el envío/aparición de la notificación.
    @Column(name = "fecha_programada")
    private LocalDateTime fechaProgramada;

    // Indica si el usuario ya la ha marcado como leída.
    @Column(columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean leida;

    // Usuario destinatario de la notificación.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}