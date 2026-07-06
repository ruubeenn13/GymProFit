package com.gymprofit.api.service.notificacion;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.gymprofit.api.entity.DeviceToken;
import com.gymprofit.api.repository.jpa.IDeviceTokenRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// ============================================================
// PushNotificationService — envío de notificaciones push vía Firebase Cloud Messaging.
// Busca los tokens FCM del usuario destinatario y les envía la notificación. Es
// tolerante a fallos: si Firebase no está inicializado (push desactivado por falta
// de credencial), hace no-op; un token muerto (UNREGISTERED/INVALID) se borra de la BD.
// Nunca lanza excepción hacia arriba: un fallo de push no debe romper la lógica de
// negocio que lo dispara (p.ej. crear una notificación in-app).
// ============================================================
@Service
@AllArgsConstructor
public class PushNotificationService {

    private final IDeviceTokenRepository deviceTokenRepository;
    private final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    // Envía una push a todos los dispositivos del usuario. Silencioso si push desactivado.
    @Transactional
    public void enviarA(Integer usuarioId, String titulo, String cuerpo) {
        // Push desactivado (sin credencial Firebase): no hacer nada.
        if (FirebaseApp.getApps().isEmpty()) {
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUsuarioId(usuarioId);
        if (tokens.isEmpty()) {
            return;
        }

        for (DeviceToken deviceToken : tokens) {
            try {
                Message mensaje = Message.builder()
                        .setToken(deviceToken.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(titulo)
                                .setBody(cuerpo)
                                .build())
                        // Prioridad alta: la notificación se entrega aunque la app esté en background.
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .build())
                        .build();

                FirebaseMessaging.getInstance().send(mensaje);
            } catch (FirebaseMessagingException e) {
                // Token muerto (app desinstalada / token caducado): se elimina de la BD.
                MessagingErrorCode code = e.getMessagingErrorCode();
                if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    deviceTokenRepository.deleteByToken(deviceToken.getToken());
                    logger.info("Token FCM muerto eliminado (usuario {}): {}", usuarioId, code);
                } else {
                    logger.warn("Fallo enviando push a un token del usuario {}: {}", usuarioId, e.getMessage());
                }
            } catch (Exception e) {
                logger.warn("Error inesperado enviando push al usuario {}: {}", usuarioId, e.getMessage());
            }
        }
    }
}
