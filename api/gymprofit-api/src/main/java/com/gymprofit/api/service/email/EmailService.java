package com.gymprofit.api.service.email;

import com.gymprofit.api.entity.Usuario;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// ============================================================
// EmailService — envío de los correos transaccionales de GymProFit
// Ahora mismo solo manda uno: el código de recuperación de contraseña.
//
// El proveedor es Brevo por SMTP y se configura por variables de entorno.
//
// En PROD la configuración de correo es obligatoria y la app no arranca sin ella
// (verificarConfiguracionDeCorreo). Antes spring.mail.host tenía default vacío también
// en prod: sin MAIL_HOST la API arrancaba con normalidad, /auth/forgot-password respondía
// 200 «te hemos enviado un código» y el código acababa escrito en el log en claro. La
// única vía de recuperar una cuenta fallaba de forma indistinguible del éxito y encima
// dejaba en los logs algo que durante 15 minutos equivale a la contraseña.
//
// Fuera de prod —dev, ci— no hace falta cuenta de correo: sin JavaMailSender el código
// se entrega en un fichero local (ver entregarSinSmtp), nunca en el log.
// ============================================================
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    // Sello del nombre del fichero de entrega local fuera de prod.
    private static final DateTimeFormatter SELLO_FICHERO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    // ObjectProvider y no inyección directa: el bean solo existe si spring.mail.host
    // está configurado, y fuera de prod la API tiene que arrancar igual cuando no lo está.
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    // Para distinguir prod del resto: en prod no hay modo degradado que valga.
    private final Environment environment;

    // Remitente. Fuera de prod vale cualquier cosa porque no se envía nada real; en prod
    // application-prod.properties lo mapea a MAIL_FROM sin default, y tiene que ser una
    // direccion verificada en Brevo o de un dominio autenticado, o el envío se rechaza.
    @Value("${app.mail.from:GymProFit <no-reply@gymprofit.app>}")
    private String remitente;

    // Se lee la propiedad ya resuelta y no MAIL_HOST directamente: así también se detecta
    // el caso de una variable definida pero vacía, que el placeholder sin default no ve.
    @Value("${spring.mail.host:}")
    private String mailHost;

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
     * Se comprueban el servidor y el remitente, y por el mismo motivo: sin servidor no se
     * envía, y con un remitente que no esté verificado en Brevo el proveedor rechaza el
     * correo. En los dos casos el catch del envío se traga el fallo y
     * POST /auth/forgot-password responde 200 igual, así que el único sitio donde eso se
     * puede detener es el arranque.
     * <p>
     * Si las variables no están definidas, application-prod.properties ya no resuelve y el
     * contexto falla antes de llegar aquí. Este control cubre el otro caso: definidas pero vacías.
     *
     * @throws IllegalStateException si el perfil prod está activo sin servidor o sin remitente.
     */
    @PostConstruct
    void verificarConfiguracionDeCorreo() {
        if (!esProduccion()) {
            return;
        }

        List<String> faltan = new ArrayList<>();
        if (!StringUtils.hasText(mailHost)) {
            faltan.add("MAIL_HOST");
        }
        if (!StringUtils.hasText(remitente)) {
            faltan.add("MAIL_FROM");
        }

        if (!faltan.isEmpty()) {
            throw new IllegalStateException(
                    "Configuración de correo incompleta con el perfil prod activo, falta: "
                    + String.join(", ", faltan) + ". En producción el correo es obligatorio "
                    + "(MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD y MAIL_FROM, esta última con una "
                    + "dirección verificada en el proveedor), porque sin él POST /auth/forgot-password "
                    + "responde 200 sin haber enviado nada y la recuperación de cuenta deja de "
                    + "funcionar en silencio.");
        }
    }

    /**
     * Envía por correo el código de recuperación de contraseña.
     * <p>
     * El código va también en el asunto: en un aviso de móvil, un OTP escondido dentro
     * del cuerpo obliga a abrir el correo para leer seis dígitos.
     * <p>
     * Un fallo de envío se registra pero no se propaga. El endpoint que llama a esto
     * responde siempre lo mismo para no revelar qué cuentas existen, así que dejar
     * escapar la excepción convertiría un error de SMTP en un detector de cuentas.
     *
     * @param usuario destinatario del código.
     * @param codigo  los seis dígitos en claro (lo único que sale del servidor sin hashear).
     * @param minutosValidez minutos que el código seguirá sirviendo.
     */
    @Override
    public void enviarCodigoRecuperacion(Usuario usuario, String codigo, int minutosValidez) {
        String asunto = codigo + " · Tu código de GymProFit";

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            entregarSinSmtp(usuario, codigo, minutosValidez);
            return;
        }

        try {
            MimeMessage mensaje = sender.createMimeMessage();
            // multipart = true, obligatorio: setText(texto, html) monta un multipart/alternative
            // y con el flag a false lanzaba IllegalStateException. Como el catch de abajo se traga
            // el fallo para no delatar qué cuentas existen, el correo NUNCA salía y el endpoint
            // respondía 200 igual; solo quedaba una línea de error en el log.
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, StandardCharsets.UTF_8.name());
            helper.setFrom(remitente);
            helper.setTo(usuario.getEmail());
            helper.setSubject(asunto);
            helper.setText(cuerpoTexto(usuario, codigo, minutosValidez),
                    cuerpoHtml(usuario, codigo, minutosValidez));
            sender.send(mensaje);
            logger.info("Código de recuperación enviado al usuario id={}", usuario.getId());
        } catch (Exception e) {
            logger.error("No se pudo enviar el código de recuperación al usuario id={}", usuario.getId(), e);
        }
    }

    /**
     * Entrega el código cuando no hay SMTP. Solo puede ocurrir fuera de producción.
     * <p>
     * El código completo se escribe en un fichero del buzón local y del log solo sale la ruta.
     * Es a propósito: durante su validez el código <em>es</em> la contraseña de la cuenta, y el
     * log es el peor sitio donde dejarlo —se agrega, se rota a un servicio de terceros, se
     * conserva semanas y lo lee mucha más gente que el disco de la máquina de desarrollo—.
     * El fichero vive en el directorio de build, se borra con un mvn clean y sirve igual para
     * probar el flujo entero sin cuenta de correo. Tampoco se registran dígitos sueltos del
     * código: dos de seis ya reducen el espacio de búsqueda a cien intentos.
     */
    private void entregarSinSmtp(Usuario usuario, String codigo, int minutosValidez) {
        if (esProduccion()) {
            // Cinturón y tirantes: verificarConfiguracionDeCorreo impide llegar hasta aquí en
            // prod, pero si alguna vez se llegara, el código NO acaba escrito en ningún sitio.
            logger.error("SMTP no disponible con el perfil prod activo: no se ha enviado el código de "
                    + "recuperación al usuario id={}", usuario.getId());
            return;
        }

        try {
            Path buzon = Path.of(outboxDir);
            Files.createDirectories(buzon);
            Path fichero = buzon.resolve("codigo-" + usuario.getId() + "-"
                    + LocalDateTime.now().format(SELLO_FICHERO) + ".txt");
            Files.writeString(fichero, cuerpoTexto(usuario, codigo, minutosValidez), StandardCharsets.UTF_8);
            logger.warn("SMTP no configurado; código de recuperación de '{}' (válido {} min) escrito en {}",
                    usuario.getUsername(), minutosValidez, fichero.toAbsolutePath());
        } catch (IOException e) {
            logger.error("SMTP no configurado y tampoco se pudo escribir el código en el buzón local '{}'. "
                    + "El usuario id={} no ha recibido su código.", outboxDir, usuario.getId(), e);
        }
    }

    // El perfil prod es el único sin modo degradado: ahí el correo es infraestructura obligatoria.
    private boolean esProduccion() {
        return environment.matchesProfiles("prod");
    }

    // Alternativa en texto plano, para clientes que no pintan HTML.
    private String cuerpoTexto(Usuario usuario, String codigo, int minutos) {
        return "Hola, " + usuario.getUsername() + ".\n\n"
                + "Tu código para restablecer la contraseña de GymProFit es " + codigo + ".\n"
                + "Caduca en " + minutos + " minutos y solo sirve una vez.\n\n"
                + "Si no has pedido este código, ignora este correo: tu contraseña no ha cambiado.\n";
    }

    /**
     * Cuerpo HTML del correo, en la línea visual acordada: tarjeta blanca sobre fondo
     * hueso y el código como único acento naranja.
     * <p>
     * Se escribe con estilos en línea y sin imágenes a propósito. Gmail descarta los
     * fondos con imagen y su proxy no siempre carga las remotas, así que el logotipo va
     * como texto: prefiero un correo que se ve igual en todas partes a uno que en Gmail
     * llega descabezado. Solo modo claro, que es lo decidido para esta familia de correos.
     */
    private String cuerpoHtml(Usuario usuario, String codigo, int minutos) {
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"color-scheme\" content=\"light only\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
                + "<body style=\"margin:0;padding:0;background:#FAF8F5;\">"
                // Preencabezado: lo que se lee en la bandeja antes de abrir.
                + "<div style=\"display:none;max-height:0;overflow:hidden;\">"
                + "Código " + codigo + ", válido " + minutos + " minutos.</div>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#FAF8F5;padding:32px 16px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width:480px;background:#FFFFFF;border:1px solid #ECE7E1;border-radius:14px;"
                + "padding:32px;font-family:Helvetica,Arial,sans-serif;\">"
                + "<tr><td style=\"font-size:20px;font-weight:700;color:#1B1917;padding-bottom:24px;\">"
                + "Gym<span style=\"color:#B83E00;\">ProFit</span></td></tr>"
                + "<tr><td style=\"font-size:22px;font-weight:700;color:#1B1917;padding-bottom:8px;\">"
                + "Hola, " + escapar(usuario.getUsername()) + "</td></tr>"
                + "<tr><td style=\"font-size:15px;line-height:22px;color:#57534E;padding-bottom:24px;\">"
                + "Has pedido restablecer tu contraseña. Escribe este código en la aplicación:</td></tr>"
                + "<tr><td align=\"center\" style=\"padding-bottom:24px;\">"
                + "<div style=\"font-size:34px;font-weight:700;letter-spacing:10px;color:#B83E00;"
                + "background:#FFF7F2;border:1px solid #F5DCCB;border-radius:10px;padding:18px 12px;\">"
                + codigo + "</div></td></tr>"
                + "<tr><td style=\"font-size:14px;line-height:21px;color:#57534E;\">"
                + "Caduca en " + minutos + " minutos y solo sirve una vez.</td></tr>"
                + "<tr><td style=\"font-size:13px;line-height:20px;color:#8A817C;padding-top:20px;"
                + "border-top:1px solid #ECE7E1;margin-top:20px;\">"
                + "Si no has pedido este código, ignora este correo: tu contraseña no ha cambiado.</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    // El nombre de usuario lo elige el usuario y acaba dentro del HTML del correo.
    private String escapar(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
