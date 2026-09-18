package com.gymprofit.api.service.email;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.gymprofit.api.entity.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ============================================================
// EmailServiceTest — el correo es obligatorio en producción y el código de
// recuperación no se escribe en el log en ningún perfil.
//
// Dos bloques: el arranque (ApplicationContextRunner, que monta solo esta bean y
// permite comprobar que el contexto FALLA sin tocar base de datos) y la entrega del
// código, con el servicio construido a mano y un appender de Logback enganchado para
// verificar lo que se registra y lo que no.
// ============================================================
class EmailServiceTest {

    private static final String CODIGO = "123456";
    private static final int MINUTOS = 15;
    private static final String HOST_DE_CORREO = "smtp-relay.brevo.com";
    private static final String REMITENTE = "GymProFit <no-reply@gymprofit.app>";

    private ListAppender<ILoggingEvent> registro;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void engancharElLog() {
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(EmailService.class);
        registro = new ListAppender<>();
        registro.start();
        logger.addAppender(registro);
    }

    @AfterEach
    void soltarElLog() {
        logger.detachAppender(registro);
        registro.stop();
    }

    // --- Arranque -----------------------------------------------------------

    /**
     * El agujero que se cierra: con el perfil prod y sin servidor SMTP, la API arrancaba
     * con normalidad y /auth/forgot-password respondía 200 sin haber enviado nada. Ahora
     * el contexto no levanta, igual que ya pasaba con JWT_SECRET.
     */
    @Test
    void enProduccionSinServidorDeCorreo_elContextoNoArranca() {
        runner("prod").withPropertyValues("spring.mail.host=").run(contexto -> {
            assertThat(contexto).hasFailed();
            assertThat(causaRaiz(contexto)).contains("MAIL_HOST");
        });
    }

    /**
     * El remitente es el otro modo de fallar en silencio: Brevo rechaza cualquier direccion
     * que no este verificada, el catch del envio se traga el rechazo y el endpoint responde
     * 200 igual. Por eso MAIL_FROM tambien tiene que impedir el arranque si falta.
     */
    @Test
    void enProduccionSinRemitente_elContextoNoArranca() {
        runner("prod").withPropertyValues("spring.mail.host=" + HOST_DE_CORREO, "app.mail.from=")
                .run(contexto -> {
                    assertThat(contexto).hasFailed();
                    assertThat(causaRaiz(contexto)).contains("MAIL_FROM");
                });
    }

    // Con el correo configurado, el mismo perfil prod arranca sin quejarse.
    @Test
    void enProduccionConServidorDeCorreo_elContextoArranca() {
        runner("prod").withPropertyValues("spring.mail.host=" + HOST_DE_CORREO, "app.mail.from=" + REMITENTE)
                .run(contexto -> assertThat(contexto).hasNotFailed().hasSingleBean(EmailService.class));
    }

    // dev y ci siguen arrancando sin cuenta de correo: ahí el modo degradado es útil.
    @Test
    void fueraDeProduccionSinServidorDeCorreo_elContextoArranca() {
        runner("dev").withPropertyValues("spring.mail.host=")
                .run(contexto -> assertThat(contexto).hasNotFailed().hasSingleBean(EmailService.class));

        runner("ci").withPropertyValues("spring.mail.host=")
                .run(contexto -> assertThat(contexto).hasNotFailed().hasSingleBean(EmailService.class));
    }

    // --- Entrega del código -------------------------------------------------

    /**
     * En dev/ci el flujo se tiene que poder probar sin cuenta de correo, así que el código
     * completo queda recuperable — pero en un fichero del directorio de build, no en el log.
     */
    @Test
    void fueraDeProduccionSinSmtp_elCodigoVaAlBuzonLocalYNoAlLog(@TempDir Path buzon) throws IOException {
        EmailService servicio = servicioSinSmtp("dev", buzon);

        servicio.enviarCodigoRecuperacion(usuario(), CODIGO, MINUTOS);

        List<Path> entregas = ficherosDe(buzon);
        assertThat(entregas).hasSize(1);
        assertThat(Files.readString(entregas.get(0), StandardCharsets.UTF_8)).contains(CODIGO);
        assertThat(mensajesRegistrados()).anyMatch(mensaje -> mensaje.contains(buzon.toString()));
        assertThat(mensajesRegistrados()).noneMatch(mensaje -> mensaje.contains(CODIGO));
    }

    /**
     * En producción no hay modo degradado que valga: si por lo que sea no hubiese sender,
     * el código no se escribe ni en el log ni en disco. El guard de arranque ya impide
     * llegar hasta aquí, esto es la segunda barrera.
     */
    @Test
    void enProduccionSinSmtp_niSeEntregaElCodigoNiSeRegistra(@TempDir Path buzon) {
        EmailService servicio = servicioSinSmtp("prod", buzon);

        servicio.enviarCodigoRecuperacion(usuario(), CODIGO, MINUTOS);

        assertThat(ficherosDe(buzon)).isEmpty();
        assertThat(mensajesRegistrados()).noneMatch(mensaje -> mensaje.contains(CODIGO));
        assertThat(registro.list).anyMatch(evento -> evento.getLevel() == Level.ERROR);
    }

    // Con SMTP disponible se envía el correo y no se toca el buzón local.
    @Test
    void conSmtpDisponible_seEnviaElCorreoYNoSeEscribeNada(@TempDir Path buzon) {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        EmailService servicio = servicio("prod", buzon, sender);

        servicio.enviarCodigoRecuperacion(usuario(), CODIGO, MINUTOS);

        verify(sender).send(any(jakarta.mail.internet.MimeMessage.class));
        assertThat(ficherosDe(buzon)).isEmpty();
        assertThat(mensajesRegistrados()).noneMatch(mensaje -> mensaje.contains(CODIGO));
        // Sin errores: el correo se monta y sale. Esta comprobación es la que destapó que
        // MimeMessageHelper estaba en modo no-multipart y el envío reventaba siempre.
        assertThat(registro.list).noneMatch(evento -> evento.getLevel() == Level.ERROR);
    }

    // Un fallo de SMTP no se propaga (delataría qué cuentas existen) ni deja el código en el log.
    @Test
    void siElEnvioFalla_niSePropagaNiSeRegistraElCodigo(@TempDir Path buzon) {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("smtp caído"))
                .when(sender).send(any(jakarta.mail.internet.MimeMessage.class));
        EmailService servicio = servicio("prod", buzon, sender);

        servicio.enviarCodigoRecuperacion(usuario(), CODIGO, MINUTOS);

        assertThat(mensajesRegistrados()).noneMatch(mensaje -> mensaje.contains(CODIGO));
        assertThat(registro.list).anyMatch(evento -> evento.getLevel() == Level.ERROR);
        assertThat(ficherosDe(buzon)).isEmpty();
    }

    // --- Ayudas -------------------------------------------------------------

    private ApplicationContextRunner runner(String perfil) {
        return new ApplicationContextRunner()
                .withInitializer(contexto -> contexto.getEnvironment().setActiveProfiles(perfil))
                .withUserConfiguration(ConfiguracionMinima.class);
    }

    // Contexto mínimo: la bean bajo prueba y el resolutor de ${...} que necesitan sus @Value.
    @Configuration(proxyBeanMethods = false)
    static class ConfiguracionMinima {

        @Bean
        static PropertySourcesPlaceholderConfigurer placeholders() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        EmailService emailService(ObjectProvider<JavaMailSender> proveedor, Environment environment) {
            return new EmailService(proveedor, environment);
        }
    }

    private String causaRaiz(AssertableApplicationContext contexto) {
        Throwable fallo = contexto.getStartupFailure();
        StringBuilder texto = new StringBuilder();
        while (fallo != null) {
            texto.append(fallo.getMessage()).append('\n');
            fallo = fallo.getCause();
        }
        return texto.toString();
    }

    private EmailService servicioSinSmtp(String perfil, Path buzon) {
        return servicio(perfil, buzon, null);
    }

    @SuppressWarnings("unchecked")
    private EmailService servicio(String perfil, Path buzon, JavaMailSender sender) {
        ObjectProvider<JavaMailSender> proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(sender);

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(perfil);

        EmailService servicio = new EmailService(proveedor, environment);
        ReflectionTestUtils.setField(servicio, "remitente", REMITENTE);
        ReflectionTestUtils.setField(servicio, "mailHost", sender == null ? "" : HOST_DE_CORREO);
        ReflectionTestUtils.setField(servicio, "outboxDir", buzon.toString());
        return servicio;
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(7);
        usuario.setUsername("ruben");
        usuario.setEmail("ruben@example.com");
        return usuario;
    }

    private List<String> mensajesRegistrados() {
        return registro.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private List<Path> ficherosDe(Path carpeta) {
        try (var contenido = Files.list(carpeta)) {
            return contenido.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw new AssertionError("No se pudo leer el buzón local " + carpeta, e);
        }
    }
}
