package com.gymprofit.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

// ============================================================
// BrevoClientConfig — el cliente HTTP con el que se entregan los correos.
//
// El transporte es la API HTTP de Brevo y no SMTP. No es una preferencia: Render
// bloquea los puertos 25, 465 y 587 en los servicios del plan gratuito, así que
// JavaMailSender no podía entregar NADA desde producción, y encima el
// MailHealthIndicator que venía con el starter de correo abría una conexión SMTP en
// cada health check y tumbaba la instancia entera (4b35d6e). La API HTTP va por
// HTTPS en el 443, que no está bloqueado.
//
// RestClient y no un cliente nuevo: ya viene con spring-boot-starter-web.
// ============================================================
@Configuration(proxyBeanMethods = false)
public class BrevoClientConfig {

    /** Endpoint de envío transaccional de Brevo. Público porque los tests lo esperan. */
    public static final String URL_ENVIO = "https://api.brevo.com/v3/smtp/email";

    // Timeouts explícitos y cortos. El envío ocurre dentro de POST /auth/forgot-password,
    // o sea en el hilo que atiende a un usuario que está esperando: un proveedor lento no
    // puede colgar esa petición. Con los valores por defecto (sin límite en
    // HttpURLConnection) un Brevo caído dejaría hilos de Tomcat bloqueados hasta agotar el
    // pool, que es exactamente como se cayó producción la vez anterior.
    private static final Duration TIMEOUT_CONEXION = Duration.ofSeconds(3);
    private static final Duration TIMEOUT_LECTURA = Duration.ofSeconds(5);

    /**
     * Cliente HTTP dedicado a Brevo, con la URL de envío ya fijada y timeouts propios.
     * <p>
     * Se le pone una {@link SimpleClientHttpRequestFactory} a propósito: es la del JDK, no
     * arrastra ninguna dependencia y para un volumen de correos de recuperación de
     * contraseña no hace falta un pool de conexiones.
     *
     * @param builder el {@code RestClient.Builder} que autoconfigura Spring Boot.
     * @return cliente apuntando a {@link #URL_ENVIO}.
     */
    @Bean
    RestClient brevoRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(TIMEOUT_CONEXION);
        fabrica.setReadTimeout(TIMEOUT_LECTURA);
        return builder.baseUrl(URL_ENVIO).requestFactory(fabrica).build();
    }
}
