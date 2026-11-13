package com.gymprofit.api.service.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.config.CorreoAsyncConfig;
import com.gymprofit.api.entity.Usuario;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// EmailService — envío de los correos transaccionales de GymProFit
// Manda dos, los dos con un código de seis dígitos: el de recuperación de contraseña y
// el de verificación del correo (GP-045). Comparten todo menos los textos (ver Correo).
//
// El proveedor es Brevo y el transporte su API HTTP, NO su SMTP. Render bloquea los
// puertos 25, 465 y 587 en los servicios del plan gratuito, así que JavaMailSender no
// podía entregar nada desde producción por mucho que la configuración fuese correcta
// (ver BrevoClientConfig). La clave de API va por variable de entorno.
//
// En PROD la configuración de correo es obligatoria y la app no arranca sin ella
// (verificarConfiguracionDeCorreo). Antes la propiedad del servidor tenía default vacío
// también en prod: sin ella la API arrancaba con normalidad, /auth/forgot-password
// respondía 200 «te hemos enviado un código» y el código acababa escrito en el log en
// claro. La única vía de recuperar una cuenta fallaba de forma indistinguible del éxito
// y encima dejaba en los logs algo que durante 15 minutos equivale a la contraseña.
//
// Fuera de prod —dev, ci— no hace falta cuenta de correo: sin clave de API el código se
// entrega en un fichero local (ver entregarSinApi), nunca en el log.
//
// El envío es ASÍNCRONO, en el pool acotado de CorreoAsyncConfig. Entregar es una llamada
// HTTP a un tercero y antes se hacía dentro de la petición: con Brevo lento, los hilos de
// Tomcat se quedaban esperando y en una instancia de 512 MB eso tumba la API entera, no
// solo el correo. La contrapartida es que ningún fallo de envío puede llegar ya a una
// respuesta: el log es la única señal, y por eso los errores de aquí son ERROR y llevan
// el id del usuario afectado.
// ============================================================
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    // Sello del nombre del fichero de entrega local fuera de prod.
    private static final DateTimeFormatter SELLO_FICHERO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    // "Nombre Visible <direccion@dominio>". El grupo del nombre es perezoso y el de la
    // dirección excluye '<' y '>' para que un nombre con espacios no se cuele dentro.
    private static final Pattern REMITENTE_CON_NOMBRE = Pattern.compile("^\\s*(.*?)\\s*<\\s*([^<>]+?)\\s*>\\s*$");

    // Nombre del remitente cuando app.mail.from trae solo la dirección: es el nombre del
    // producto, no un dato inventado, y Brevo enseña la dirección pelada si se deja vacío.
    private static final String NOMBRE_POR_DEFECTO = "GymProFit";

    // Tope de lo que se registra de un cuerpo de error del proveedor. Brevo devuelve un
    // JSON corto, pero un proxy o una página de error por el medio pueden devolver HTML.
    private static final int MAX_CUERPO_EN_LOG = 500;

    // Cliente HTTP dedicado a Brevo, con la URL de envío y los timeouts ya fijados.
    private final RestClient brevoRestClient;

    // Para leer el messageId de la respuesta. Es el ObjectMapper de Spring Boot, no se crea otro.
    private final ObjectMapper objectMapper;

    // Para distinguir prod del resto: en prod no hay modo degradado que valga.
    private final Environment environment;

    // Remitente. Fuera de prod vale cualquier cosa porque no se envía nada real; en prod
    // application-prod.properties lo mapea a MAIL_FROM sin default, y tiene que ser una
    // direccion verificada en Brevo o de un dominio autenticado, o el envío se rechaza.
    @Value("${app.mail.from:GymProFit <no-reply@gymprofit.app>}")
    private String remitente;

    // Clave de API de Brevo. OJO: NO es la clave SMTP; son credenciales distintas y se
    // generan en pestañas distintas del panel (SMTP & API > API Keys, no > SMTP). En prod
    // se mapea a BREVO_API_KEY sin default, igual que JWT_SECRET. Se lee la propiedad ya
    // resuelta y no la variable: así también se detecta el caso de variable definida pero
    // vacía, que un placeholder sin default no ve.
    @Value("${app.mail.brevo.api-key:}")
    private String apiKey;

    // Buzón local de dev/ci. Por defecto dentro del directorio de build, que ya está
    // fuera del repositorio y se borra con un mvn clean.
    @Value("${app.mail.outbox.dir:target/mail-outbox}")
    private String outboxDir;

    /**
     * Aborta el arranque si el perfil prod está activo y falta configuración de correo.
     * <p>
     * Mismo criterio que jwt.secret: un secreto que falta en producción no se suple con un
     * valor por defecto, se convierte en un fallo de arranque. Aquí además el modo degradado
     * era peor que no arrancar, porque dejaba los códigos de un solo uso en los logs.
     * <p>
     * Se comprueban la clave de API y el remitente, y por el mismo motivo: sin clave no se
     * envía, y con un remitente que no esté verificado en Brevo el proveedor rechaza el
     * correo. En los dos casos el envío falla sin propagar y POST /auth/forgot-password
     * responde 200 igual, así que el único sitio donde eso se puede detener es el arranque.
     * <p>
     * Si las variables no están definidas, application-prod.properties ya no resuelve y el
     * contexto falla antes de llegar aquí. Este control cubre el otro caso: definidas pero vacías.
     *
     * @throws IllegalStateException si el perfil prod está activo sin clave de API o sin remitente.
     */
    @PostConstruct
    void verificarConfiguracionDeCorreo() {
        if (!esProduccion()) {
            return;
        }

        List<String> faltan = new ArrayList<>();
        if (!StringUtils.hasText(apiKey)) {
            faltan.add("BREVO_API_KEY");
        }
        if (!StringUtils.hasText(remitente)) {
            faltan.add("MAIL_FROM");
        }

        if (!faltan.isEmpty()) {
            throw new IllegalStateException(
                    "Configuración de correo incompleta con el perfil prod activo, falta: "
                    + String.join(", ", faltan) + ". En producción el correo es obligatorio "
                    + "(BREVO_API_KEY y MAIL_FROM, esta última con una dirección verificada en el "
                    + "proveedor), porque sin él POST /auth/forgot-password responde 200 sin haber "
                    + "enviado nada y la recuperación de cuenta deja de funcionar en silencio.");
        }
    }

    /**
     * Envía por correo el código de recuperación de contraseña, fuera del hilo que llama.
     * <p>
     * El código va también en el asunto: en un aviso de móvil, un OTP escondido dentro
     * del cuerpo obliga a abrir el correo para leer seis dígitos.
     * <p>
     * Es asíncrono ({@link CorreoAsyncConfig#EXECUTOR}) porque la entrega es una llamada
     * HTTP a un tercero y antes ocurría dentro de {@code POST /auth/forgot-password}: un
     * proveedor lento retenía hilos de petición hasta el timeout. El que llama no espera
     * respuesta ni la tiene: el método devuelve en cuanto la tarea queda encolada.
     * <p>
     * <strong>Todo lo que necesita viaja en los parámetros.</strong> El código llega ya
     * generado y no se relee de la base de datos, porque la tarea puede arrancar antes de
     * que confirme la transacción que lo guardó y encontraría la tabla sin él. El usuario
     * llega cargado, y de él solo se leen columnas simples.
     * <p>
     * Un fallo de envío se registra pero no se propaga, y ahora además no podría: nadie
     * espera al otro lado. El ERROR del log es la única señal que queda, así que lleva el
     * id del usuario y lo que respondiera el proveedor.
     *
     * @param usuario destinatario del código, ya cargado.
     * @param codigo  los seis dígitos en claro (lo único que sale del servidor sin hashear).
     * @param minutosValidez minutos que el código seguirá sirviendo.
     */
    @Async(CorreoAsyncConfig.EXECUTOR)
    @Override
    public void enviarCodigoRecuperacion(Usuario usuario, String codigo, int minutosValidez) {
        entregar(Correo.RECUPERACION, usuario, codigo, minutosValidez);
    }

    /**
     * Envía el código para verificar el correo (GP-045), fuera del hilo que llama.
     * <p>
     * Todo como {@link #enviarCodigoRecuperacion}: el mismo pool, la misma plantilla con el
     * código de protagonista, el código en el asunto y ningún fallo que suba. Solo cambian
     * los textos, que dicen para qué es el código.
     *
     * @param usuario        destinatario, ya cargado; el código va a su correo actual.
     * @param codigo         los seis dígitos en claro.
     * @param minutosValidez minutos que el código seguirá sirviendo.
     */
    @Async(CorreoAsyncConfig.EXECUTOR)
    @Override
    public void enviarCodigoVerificacion(Usuario usuario, String codigo, int minutosValidez) {
        entregar(Correo.VERIFICACION, usuario, codigo, minutosValidez);
    }

    // La entrega de cualquiera de los dos correos con código: por la API de Brevo o, sin
    // clave, al buzón local. Ningún fallo sale de aquí.
    private void entregar(Correo correo, Usuario usuario, String codigo, int minutosValidez) {
        if (!StringUtils.hasText(apiKey)) {
            entregarSinApi(correo, usuario, codigo, minutosValidez);
            return;
        }

        try {
            // Armar el mensaje va DENTRO del try: leer la plantilla o interpretar el
            // remitente puede fallar, y en un hilo asíncrono una excepción suelta no
            // llega a ninguna respuesta, se pierde en el executor.
            MensajeBrevo mensaje = new MensajeBrevo(
                    remitenteDeBrevo(),
                    List.of(new MensajeBrevo.Destinatario(usuario.getEmail(), usuario.getUsername())),
                    asunto(codigo),
                    cuerpoHtml(correo, usuario, codigo, minutosValidez),
                    cuerpoTexto(correo, usuario, codigo, minutosValidez));

            // onStatus con predicado siempre cierto y manejador vacío desactiva el
            // comportamiento por defecto de RestClient, que lanza en 4xx/5xx. Aquí interesa
            // el cuerpo del error para poder diagnosticarlo, no una excepción.
            ResponseEntity<String> respuesta = brevoRestClient.post()
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(mensaje)
                    .retrieve()
                    .onStatus(estado -> true, (peticion, resultado) -> { })
                    .toEntity(String.class);

            if (respuesta.getStatusCode().is2xxSuccessful()) {
                logger.info("Código de {} enviado al usuario id={} (messageId={})",
                        correo.nombre, usuario.getId(), messageId(respuesta.getBody()));
            } else {
                logger.error("Brevo rechazó el correo de {} del usuario id={}: HTTP {} · {}",
                        correo.nombre, usuario.getId(), respuesta.getStatusCode().value(),
                        cuerpoParaElLog(respuesta.getBody(), codigo));
            }
        } catch (Exception e) {
            // Timeout, DNS, TLS, plantilla ilegible: nada de esto llega al cliente, que ya
            // ha recibido su 200 hace rato. Este ERROR es todo lo que queda para verlo.
            logger.error("No se pudo entregar a Brevo el código de {} del usuario id={}",
                    correo.nombre, usuario.getId(), e);
        }
    }

    /**
     * Entrega el código cuando no hay clave de API. Solo puede ocurrir fuera de producción.
     * <p>
     * El código completo se escribe en un fichero del buzón local y del log solo sale la ruta.
     * Es a propósito: durante su validez el código <em>es</em> la contraseña de la cuenta, y el
     * log es el peor sitio donde dejarlo —se agrega, se rota a un servicio de terceros, se
     * conserva semanas y lo lee mucha más gente que el disco de la máquina de desarrollo—.
     * El fichero vive en el directorio de build, se borra con un mvn clean y sirve igual para
     * probar el flujo entero sin cuenta de correo. Tampoco se registran dígitos sueltos del
     * código: dos de seis ya reducen el espacio de búsqueda a cien intentos.
     */
    private void entregarSinApi(Correo correo, Usuario usuario, String codigo, int minutosValidez) {
        if (esProduccion()) {
            // Cinturón y tirantes: verificarConfiguracionDeCorreo impide llegar hasta aquí en
            // prod, pero si alguna vez se llegara, el código NO acaba escrito en ningún sitio.
            logger.error("Clave de API de Brevo no disponible con el perfil prod activo: no se ha "
                    + "enviado el código de {} al usuario id={}", correo.nombre, usuario.getId());
            return;
        }

        try {
            Path buzon = Path.of(outboxDir);
            Files.createDirectories(buzon);
            Path fichero = buzon.resolve(correo.prefijoBuzon + usuario.getId() + "-"
                    + LocalDateTime.now().format(SELLO_FICHERO) + ".txt");
            Files.writeString(fichero, cuerpoTexto(correo, usuario, codigo, minutosValidez), StandardCharsets.UTF_8);
            logger.warn("Correo no configurado; código de {} de '{}' (válido {} min) escrito en {}",
                    correo.nombre, usuario.getUsername(), minutosValidez, fichero.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Correo no configurado y tampoco se pudo escribir el código en el buzón local '{}'. "
                    + "El usuario id={} no ha recibido su código.", outboxDir, usuario.getId(), e);
        }
    }

    /**
     * Parte app.mail.from en las dos mitades que quiere la API: nombre y dirección.
     * <p>
     * El formato acordado es «Nombre &lt;direccion@dominio&gt;», que es el que entiende un
     * cliente de correo y el que ya está documentado y dado de alta en Render. La API HTTP
     * los quiere separados, así que se parten aquí en vez de cambiar el formato de la
     * variable. Si viene solo la dirección, el nombre pasa a ser el del producto.
     *
     * @return el remitente en las dos piezas que espera Brevo.
     */
    private MensajeBrevo.Remitente remitenteDeBrevo() {
        Matcher partes = REMITENTE_CON_NOMBRE.matcher(remitente);
        if (partes.matches()) {
            String nombre = partes.group(1);
            return new MensajeBrevo.Remitente(
                    StringUtils.hasText(nombre) ? nombre : NOMBRE_POR_DEFECTO, partes.group(2));
        }
        return new MensajeBrevo.Remitente(NOMBRE_POR_DEFECTO, remitente.trim());
    }

    /** Saca el messageId de la respuesta de Brevo, que es lo único que sirve para rastrear un envío. */
    private String messageId(String cuerpo) {
        if (!StringUtils.hasText(cuerpo)) {
            return "sin messageId";
        }
        try {
            JsonNode identificador = objectMapper.readTree(cuerpo).path("messageId");
            return identificador.isMissingNode() ? "sin messageId" : identificador.asText();
        } catch (Exception e) {
            // La respuesta no era el JSON esperado. Es información que falta, no un fallo de envío.
            return "sin messageId";
        }
    }

    /**
     * Prepara un cuerpo de error para el log: lo recorta y le tacha el código.
     * <p>
     * Brevo no devuelve la petición en sus errores, pero lo que se registra viene de un
     * tercero y podría hacerlo —o podría hacerlo un proxy por el medio—, y el código no
     * puede acabar en el log por ninguna vía. Tacharlo cuesta una llamada y cierra el camino.
     */
    private String cuerpoParaElLog(String cuerpo, String codigo) {
        if (!StringUtils.hasText(cuerpo)) {
            return "(sin cuerpo)";
        }
        String limpio = cuerpo.replace(codigo, "······");
        return limpio.length() > MAX_CUERPO_EN_LOG
                ? limpio.substring(0, MAX_CUERPO_EN_LOG) + "… (recortado)"
                : limpio;
    }

    // El perfil prod es el único sin modo degradado: ahí el correo es infraestructura obligatoria.
    private boolean esProduccion() {
        return environment.matchesProfiles("prod");
    }

    // ── Textos (lote 24) ─────────────────────────────────────────────────────
    // Los dos correos en español y en inglés, según el idioma de la petición que los
    // provocó (Accept-Language de la app; CorreoAsyncConfig lo trae hasta este hilo). La
    // cuenta no guarda idioma: el de la app al pedir el código es el mejor que hay.

    // Si el correo va en inglés. Cualquier otro idioma, en español, el de la casa. Sin
    // contexto de idioma (fuera de una petición) también en español: getLocale() caería al
    // de la JVM, que en el CI y en Render es inglés.
    private static boolean ingles() {
        org.springframework.context.i18n.LocaleContext contexto = LocaleContextHolder.getLocaleContext();
        java.util.Locale idioma = contexto == null ? null : contexto.getLocale();
        return idioma != null && "en".equals(idioma.getLanguage());
    }

    /**
     * El asunto, que es también el preencabezado: el código delante, para leerlo en el
     * aviso del móvil sin abrir el correo.
     */
    private static String asunto(String codigo) {
        return ingles() ? codigo + " is your GymProFit code" : codigo + " es tu código de GymProFit";
    }

    // «Hola, Rubén:» con el nombre visible; sin él, «Hola:». En inglés, «Hi Rubén,» y «Hi,».
    private static String saludo(Usuario usuario) {
        String nombre = usuario.getNombre();
        boolean conNombre = StringUtils.hasText(nombre);
        if (ingles()) return conNombre ? "Hi " + nombre.trim() + "," : "Hi,";
        return conNombre ? "Hola, " + nombre.trim() + ":" : "Hola:";
    }

    private static String titular(Correo correo) {
        if (correo == Correo.VERIFICACION) return ingles() ? "Verify your email" : "Verifica tu correo";
        return ingles() ? "Reset your password" : "Restablece tu contraseña";
    }

    private static String instruccion(Correo correo) {
        if (correo == Correo.VERIFICACION) {
            return ingles() ? "Enter this code in the app to verify your email."
                    : "Escribe este código en la aplicación para verificar tu correo.";
        }
        return ingles() ? "Enter this code in the app to choose a new password."
                : "Escribe este código en la aplicación para elegir una contraseña nueva.";
    }

    private static String caducidad(int minutos) {
        return ingles() ? "It expires in " + minutos + " minutes and works only once."
                : "Caduca en " + minutos + " minutos y solo sirve una vez.";
    }

    private static String cierre(Correo correo) {
        if (correo == Correo.VERIFICACION) {
            return ingles() ? "Did not sign up for GymProFit? Ignore this email. Without the code, nobody can verify this address."
                    : "¿No te has dado de alta en GymProFit? Ignora este correo. Sin el código, nadie puede verificar esta dirección.";
        }
        return ingles() ? "Did not ask for this code? Ignore this email. Your password has not changed and nobody has signed in to your account."
                : "¿No has pedido este código? Ignora este correo. Tu contraseña no ha cambiado y nadie ha entrado en tu cuenta.";
    }

    // Alternativa en texto plano, para clientes que no pintan HTML. Dice lo mismo que el
    // HTML y en el mismo orden: primero el propósito, luego el código, luego la caducidad.
    private String cuerpoTexto(Correo correo, Usuario usuario, String codigo, int minutos) {
        return titular(correo) + "\n\n"
                + saludo(usuario) + "\n" + instruccion(correo) + "\n\n"
                + codigo + "\n\n"
                + caducidad(minutos) + "\n\n"
                + cierre(correo) + "\n\n"
                + (ingles() ? "Questions? Write to us at " : "¿Dudas? Escríbenos a ")
                + PlantillaCorreo.CORREO_SOPORTE + "\n";
    }

    /**
     * Cuerpo HTML de los dos correos con código: forma ACCIÓN con el código de protagonista
     * y pie TRANSACCIONAL, porque los ha pedido el usuario (o su alta) y no se puede dar de
     * baja de ellos sin quedarse sin forma de recuperar o verificar la cuenta.
     * <p>
     * El titular dice el propósito; el saludo va en la primera línea del párrafo, con su
     * nombre visible y dos puntos («Hola, Rubén:»), o «Hola:» si no lo tiene. El nombre lo
     * elige el usuario y acaba dentro del HTML: se escapa aquí.
     *
     * @param correo  cuál de los dos.
     * @param usuario destinatario, del que sale el nombre que se saluda.
     * @param codigo  los seis dígitos.
     * @param minutos minutos de validez.
     * @return el documento HTML completo.
     */
    private String cuerpoHtml(Correo correo, Usuario usuario, String codigo, int minutos) {
        String parrafo = PlantillaCorreo.escapar(saludo(usuario)) + "<br>" + PlantillaCorreo.escapar(instruccion(correo));
        String nota = PlantillaCorreo.escapar(caducidad(minutos));
        return PlantillaCorreo.renderizar(
                ingles() ? PlantillaCorreo.Idioma.EN : PlantillaCorreo.Idioma.ES,
                asunto(codigo),
                titular(correo),
                PlantillaCorreo.Accion.conCodigo(parrafo, ingles() ? "YOUR CODE" : "TU CÓDIGO", codigo, nota),
                PlantillaCorreo.escapar(cierre(correo)),
                PlantillaCorreo.Pie.transaccional());
    }

    // Los dos correos con código. Lo que los distingue fuera de los textos: el nombre que
    // sale en el log y el prefijo del fichero del buzón local.
    private enum Correo {
        RECUPERACION("recuperación", "codigo-"),
        VERIFICACION("verificación del correo", "verificacion-");

        private final String nombre;
        private final String prefijoBuzon;

        Correo(String nombre, String prefijoBuzon) {
            this.nombre = nombre;
            this.prefijoBuzon = prefijoBuzon;
        }
    }
}
