package com.gymprofit.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

// ============================================================
// FirebaseConfig — inicializa el Firebase Admin SDK una sola vez al arrancar.
// La service-account key es un SECRETO y no se commitea: se carga por variable de
// entorno (contenido JSON en prod/Render, o ruta a fichero en local). Si NO hay
// credencial (p.ej. en CI o en un dev sin configurar), NO inicializa Firebase, deja
// el push DESACTIVADO y el arranque continúa con normalidad (PushNotificationService
// lo detecta y hace no-op). Así los tests y el build no dependen de la clave.
// ============================================================
@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    // Contenido JSON completo de la service-account key (recomendado en prod/Render).
    @Value("${firebase.credentials.json:}")
    private String credentialsJson;

    // Ruta a un fichero con la service-account key (cómodo en local; fichero gitignoreado).
    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    // Inicializa FirebaseApp si hay credencial disponible; si no, deja push desactivado.
    @PostConstruct
    public void init() {
        // Ya inicializado (p.ej. devtools recarga): no duplicar.
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            GoogleCredentials credentials;
            if (StringUtils.hasText(credentialsJson)) {
                credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)));
            } else if (StringUtils.hasText(credentialsPath)) {
                credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
            } else {
                logger.warn("Firebase: sin credencial (firebase.credentials.json/path vacíos) → " +
                        "notificaciones push DESACTIVADAS. El resto de la app funciona con normalidad.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp.initializeApp(options);
            logger.info("Firebase Admin SDK inicializado: notificaciones push ACTIVADAS.");
        } catch (Exception e) {
            // Un fallo cargando la credencial no debe tumbar el arranque: solo desactiva push.
            logger.error("Firebase: fallo al inicializar el Admin SDK → push desactivado. Causa: {}",
                    e.getMessage());
        }
    }
}
