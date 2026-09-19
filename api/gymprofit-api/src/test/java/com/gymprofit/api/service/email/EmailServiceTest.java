package com.gymprofit.api.service.email;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.config.BrevoClientConfig;
import com.gymprofit.api.entity.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

// ============================================================
// EmailServiceTest — el correo sale por la API HTTP de Brevo, es obligatorio en
// producción, y el código de recuperación no se escribe en el log por ningún camino.
//
// Tres bloques: el arranque (ApplicationContextRunner, que monta solo esta bean y permite
// comprobar que el contexto FALLA sin tocar base de datos), la llamada HTTP
// (MockRestServiceServer: nunca se llama a Brevo de verdad) y el modo degradado de dev/ci.
// En todos ellos hay un appender de Logback enganchado para verificar lo que se registra
// y —sobre todo— lo que no.
// ============================================================
class EmailServiceTest {

    private static final String CODIGO = "123456";
    private static final int MINUTOS = 15;
    private static final String CLAVE_API = "xkeysib-clave-de-prueba";
    private static final String REMITENTE = "GymProFit <no-reply@gymprofit.app>";
    private static final String RESPUESTA_OK = "{\"messageId\":\"<202609190834.7@smtp-relay.mailin.fr>\"}";

    private ListAppender<ILoggingEvent> registro;
    private ch.qos.logback.classic.Logger logger;
    private final ObjectMapper json = new ObjectMapper();

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
     * El agujero que se cierra: con el perfil prod y sin proveedor de correo, la API arrancaba
     * con normalidad y /auth/forgot-password respondía 200 sin haber enviado nada. Ahora
     * el contexto no levanta, igual que ya pasaba con JWT_SECRET.
     */
    @Test
    void enProduccionSinClaveDeApi_elContextoNoArranca() {
        runner("prod").withPropertyValues("app.mail.brevo.api-key=").run(contexto -> {
            assertThat(contexto).hasFailed();
            assertThat(causaRaiz(contexto)).contains("BREVO_API_KEY");
        });
    }

    /**
     * El remitente es el otro modo de fallar en silencio: Brevo rechaza cualquier direccion
     * que no este verificada, el fallo no se propaga y el endpoint responde 200 igual. Por
     * eso MAIL_FROM tambien tiene que impedir el arranque si falta.
     */
    @Test
    void enProduccionSinRemitente_elContextoNoArranca() {
        runner("prod").withPropertyValues("app.mail.brevo.api-key=" + CLAVE_API, "app.mail.from=")
                .run(contexto -> {
                    assertThat(contexto).hasFailed();
                    assertThat(causaRaiz(contexto)).contains("MAIL_FROM");
                });
    }

    // Con el correo configurado, el mismo perfil prod arranca sin quejarse.
    @Test
    void enProduccionConClaveDeApi_elContextoArranca() {
        runner("prod").withPropertyValues("app.mail.brevo.api-key=" + CLAVE_API, "app.mail.from=" + REMITENTE)
                .run(contexto -> assertThat(contexto).hasNotFailed().hasSingleBean(EmailService.class));
    }

    // dev y ci siguen arrancando sin cuenta de correo: ahí el modo degradado es útil.
    @Test
    void fueraDeProduccionSinClaveDeApi_elContextoArranca() {
        runner("dev").withPropertyValues("app.mail.brevo.api-key=")
                .run(contexto -> assertThat(contexto).hasNotFailed().hasSingleBean(EmailService.class));

        runner("ci").withPropertyValues("app.mail.brevo.api-key=")
                .run(contexto -> assertThat(contexto).hasNotFailed().hasSingleBean(EmailService.class));
    }

    // --- Llamada a la API HTTP de Brevo -------------------------------------

    /**
     * El envío de verdad: POST a la URL de Brevo, la clave en la cabecera api-key y el cuerpo
     * con las cinco piezas que exige la API. Se comprueba además que el remitente llega
     * PARTIDO en nombre y dirección, que es lo que obliga a parsear app.mail.from.
     */
    @Test
    void conClaveDeApi_seLlamaALaApiConElCuerpoCorrecto(@TempDir Path buzon) throws Exception {
        RestClient.Builder builder = builderDeBrevo();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        EmailService servicio = servicio("prod", buzon, builder.build(), CLAVE_API);
        StringBuilder enviado = new StringBuilder();

        brevo.expect(requestTo(BrevoClientConfig.URL_ENVIO))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", CLAVE_API))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // MockRestServiceServer valida la petición pero no la devuelve, así que el
                // cuerpo se captura aquí para poder afirmar sobre el JSON exacto que sale.
                .andExpect(peticion -> enviado.append(((MockClientHttpRequest) peticion).getBodyAsString()))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON).body(RESPUESTA_OK));

        servicio.enviarCodigoRecuperacion(usuario(), CODIGO, MINUTOS);

        brevo.verify();
        JsonNode cuerpo = json.readTree(enviado.toString());
        assertThat(cuerpo.path("sender").path("name").asText()).isEqualTo("GymProFit");
        assertThat(cuerpo.path("sender").path("email").asText()).isEqualTo("no-reply@gymprofit.app");
        assertThat(cuerpo.path("to").get(0).path("email").asText()).isEqualTo("ruben@example.com");
        assertThat(cuerpo.path("to").get(0).path("name").asText()).isEqualTo("ruben");
        assertThat(cuerpo.path("subject").asText()).contains(CODIGO);
        assertThat(cuerpo.path("textContent").asText()).contains(CODIGO).contains("15 minutos");
        assertThat(cuerpo.path("htmlContent").asText()).contains(CODIGO).contains("<!DOCTYPE html>");
    }

    /**
     * El correo de recuperación usa la plantilla con forma ACCIÓN y pie TRANSACCIONAL.
     * <p>
     * Lo que se comprueba aquí no es la maquetación —eso es cosa de PlantillaCorreoTest—
     * sino que este correo concreto elige bien: titular que dice el propósito en vez de
     * saludar, el código dentro de su panel, y <b>sin enlace de baja</b>, que es la parte con
     * consecuencias. Darse de baja de este correo equivale a quedarse sin forma de recuperar
     * la cuenta, así que el día que alguien copie y pegue esto para escribir el resumen
     * semanal, la variante de pie tiene que ser una decisión y no un descuido heredado.
     */
    @Test
    void elCorreoDeRecuperacion_usaLaPlantillaConPieTransaccional(@TempDir Path buzon) throws Exception {
        RestClient.Builder builder = builderDeBrevo();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        StringBuilder enviado = new StringBuilder();
        brevo.expect(requestTo(BrevoClientConfig.URL_ENVIO))
                .andExpect(peticion -> enviado.append(((MockClientHttpRequest) peticion).getBodyAsString()))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON).body(RESPUESTA_OK));

        servicio("prod", buzon, builder.build(), CLAVE_API)
                .enviarCodigoRecuperacion(usuario(), CODIGO, MINUTOS);

        String html = json.readTree(enviado.toString()).path("htmlContent").asText();
        assertThat(html).contains("Restablece tu contraseña").doesNotContain("Hola, ruben</");
        assertThat(html).contains("TU CÓDIGO").contains(CODIGO);
        assertThat(html).contains("Tu contraseña no ha cambiado y nadie ha entrado en tu cuenta.");
        assertThat(html).doesNotContain("Darse de baja");
    }

    /**
     * El nombre de usuario entra en el HTML del correo y lo elige el usuario. Sin escapar,
     * un nombre con etiquetas cuela marcado ajeno —incluido un enlace pulsable— dentro de un
     * correo que llega firmado por GymProFit.
     */
    @Test
    void unNombreDeUsuarioConHtml_saleEscapadoEnElCorreo(@TempDir Path buzon) throws Exception {
        RestClient.Builder builder = builderDeBrevo();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        StringBuilder enviado = new StringBuilder();
        brevo.expect(requestTo(BrevoClientConfig.URL_ENVIO))
                .andExpect(peticion -> enviado.append(((MockClientHttpRequest) peticion).getBodyAsString()))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON).body(RESPUESTA_OK));

        Usuario usuario = usuario();
        usuario.setUsername("<a href=\"http://malo\">pulsa</a>");
        servicio("prod", buzon, builder.build(), CLAVE_API)
                .enviarCodigoRecuperacion(usuario, CODIGO, MINUTOS);

        String html = json.readTree(enviado.toString()).path("htmlContent").asText();
        assertThat(html).contains("&lt;a href=&quot;http://malo&quot;&gt;pulsa&lt;/a&gt;");
        assertThat(html).doesNotContain("<a href=\"http://malo\"");
    }

    /**
     * En éxito se registra el messageId, que es lo único que sirve para rastrear un envío en
     * el panel del proveedor. Con SMTP no había nada equivalente. El código, ni rastro.
     */
    @Test
    void enExito_seRegistraElMessageIdYNoElCodigo(@TempDir Path buzon) {
        RestClient.Builder builder = builderDeBrevo();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        brevo.expect(requestTo(BrevoClientConfig.URL_ENVIO))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON).body(RESPUESTA_OK));

        servicio("prod", buzon, builder.build(), CLAVE_API)
                .enviarCodigoRecuperacion(usuario(), CODIGO, MINUTOS);

        assertThat(mensajesRegistrados())
                .anyMatch(mensaje -> mensaje.contains("<202609190834.7@smtp-relay.mailin.fr>"));
        assertThat(mensajesRegistrados()).noneMatch(mensaje -> mensaje.contains(CODIGO));
        assertThat(registro.list).noneMatch(evento -> evento.getLevel() == Level.ERROR);
        assertThat(ficherosDe(buzon)).isEmpty();
    }

    /**
     * Un error de la API no puede propagarse —una excepción aquí convertiría
     * /auth/forgot-password en un detector de qué cuentas existen— pero sí tiene que dejar
     * el estado y el cuerpo en el log: es la señal de diagnóstico que con SMTP no existía.
     */
    @Test
    void siLaApiDevuelveError_noSePropagaPeroQuedaElEstadoYElCuerpo(@TempDir Path buzon) {
        RestClient.Builder builder = builderDeBrevo();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        brevo.expect(requestTo(BrevoClientConfig.URL_ENVIO))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"invalid_parameter\",\"message\":\"sender email is not valid\"}"));

        servicio("prod", buzon, builder.build(), CLAVE_API)
                .enviarCodigoRecuperacion(usuario(), CODIGO, MINUTOS);

        brevo.verify();
        assertThat(mensajesRegistrados()).anyMatch(mensaje ->
                mensaje.contains("400") && mensaje.contains("sender email is not valid"));
        assertThat(registro.list).anyMatch(evento -> evento.getLevel() == Level.ERROR);
        assertThat(mensajesRegistrados()).noneMatch(mensaje -> mensaje.contains(CODIGO));
    }

    /**
     * El cuerpo de error viene de un tercero. Si alguna vez devolviera la petición de vuelta,
     * el código acabaría en el log por la puerta de atrás: por eso se tacha antes de registrar.
     */
    @Test
    void siElErrorDevuelveElCodigo_seTachaAntesDeRegistrarlo(@TempDir Path buzon) {
        RestClient.Builder builder = builderDeBrevo();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        brevo.expect(requestTo(BrevoClientConfig.URL_ENVIO))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"message\":\"rejected\",\"subject\":\"" + CODIGO + " · Tu código\"}"));

        servicio("prod", buzon, builder.build(), CLAVE_API)
                .enviarCodigoRecuperacion(usuario(), CODIGO, MINUTOS);

        assertThat(mensajesRegistrados()).noneMatch(mensaje -> mensaje.contains(CODIGO));
        assertThat(mensajesRegistrados()).anyMatch(mensaje -> mensaje.contains("rejected"));
    }

    /**
     * Un proveedor que no responde tampoco puede tumbar la petición: la excepción de
     * transporte se queda dentro y el endpoint sigue respondiendo lo mismo de siempre.
     */
    @Test
    void siLaApiNoResponde_laExcepcionNoSalePeroQuedaEnElLog(@TempDir Path buzon) {
        RestClient.Builder builder = builderDeBrevo();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        brevo.expect(requestTo(BrevoClientConfig.URL_ENVIO))
                .andRespond(withException(new SocketTimeoutException("Connect timed out")));

        servicio("prod", buzon, builder.build(), CLAVE_API)
                .enviarCodigoRecuperacion(usuario(), CODIGO, MINUTOS);

        assertThat(registro.list).anyMatch(evento -> evento.getLevel() == Level.ERROR);
        assertThat(mensajesRegistrados()).noneMatch(mensaje -> mensaje.contains(CODIGO));
        assertThat(ficherosDe(buzon)).isEmpty();
    }

    // --- Modo degradado de dev/ci -------------------------------------------

    /**
     * En dev/ci el flujo se tiene que poder probar sin cuenta de correo, así que el código
     * completo queda recuperable — pero en un fichero del directorio de build, no en el log.
     * Y sin clave de API no se llama a nadie: el servidor simulado no espera ninguna petición.
     */
    @Test
    void fueraDeProduccionSinClaveDeApi_elCodigoVaAlBuzonLocalYNoAlLog(@TempDir Path buzon) throws IOException {
        RestClient.Builder builder = builderDeBrevo();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        EmailService servicio = servicio("dev", buzon, builder.build(), "");

        servicio.enviarCodigoRecuperacion(usuario(), CODIGO, MINUTOS);

        brevo.verify(); // sin expectativas: no se ha hecho ninguna llamada HTTP
        List<Path> entregas = ficherosDe(buzon);
        assertThat(entregas).hasSize(1);
        assertThat(Files.readString(entregas.get(0), StandardCharsets.UTF_8)).contains(CODIGO);
        assertThat(mensajesRegistrados()).anyMatch(mensaje -> mensaje.contains(buzon.toString()));
        assertThat(mensajesRegistrados()).noneMatch(mensaje -> mensaje.contains(CODIGO));
    }

    /**
     * En producción no hay modo degradado que valga: si por lo que sea no hubiese clave,
     * el código no se escribe ni en el log ni en disco. El guard de arranque ya impide
     * llegar hasta aquí, esto es la segunda barrera.
     */
    @Test
    void enProduccionSinClaveDeApi_niSeEntregaElCodigoNiSeRegistra(@TempDir Path buzon) {
        servicio("prod", buzon, builderDeBrevo().build(), "")
                .enviarCodigoRecuperacion(usuario(), CODIGO, MINUTOS);

        assertThat(ficherosDe(buzon)).isEmpty();
        assertThat(mensajesRegistrados()).noneMatch(mensaje -> mensaje.contains(CODIGO));
        assertThat(registro.list).anyMatch(evento -> evento.getLevel() == Level.ERROR);
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
        EmailService emailService(Environment environment) {
            return new EmailService(RestClient.create(), new ObjectMapper(), environment);
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

    // Builder apuntando a la URL real de Brevo; el MockRestServiceServer lo intercepta antes
    // de que salga nada a la red.
    private RestClient.Builder builderDeBrevo() {
        return RestClient.builder().baseUrl(BrevoClientConfig.URL_ENVIO);
    }

    private EmailService servicio(String perfil, Path buzon, RestClient cliente, String clave) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(perfil);

        EmailService servicio = new EmailService(cliente, json, environment);
        ReflectionTestUtils.setField(servicio, "remitente", REMITENTE);
        ReflectionTestUtils.setField(servicio, "apiKey", clave);
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
