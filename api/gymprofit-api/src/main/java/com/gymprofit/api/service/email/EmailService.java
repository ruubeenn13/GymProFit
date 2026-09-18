package com.gymprofit.api.service.email;

import com.gymprofit.api.entity.Usuario;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

// ============================================================
// EmailService — envío de los correos transaccionales de GymProFit
// Ahora mismo solo manda uno: el código de recuperación de contraseña.
//
// El proveedor es Brevo por SMTP y se configura por variables de entorno. Cuando
// no hay credenciales —desarrollo, tests, CI— Spring no crea ningún JavaMailSender
// y el servicio escribe el código en el log en vez de fallar: así el flujo entero
// se puede probar en local sin cuenta de correo, y la única diferencia con
// producción es dónde aparece el código.
// ============================================================
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    // ObjectProvider y no inyección directa: el bean solo existe si spring.mail.host
    // está configurado, y la API tiene que arrancar igual cuando no lo está.
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    // Remitente. Sin dominio propio todavía, va el remitente compartido de Brevo.
    @Value("${app.mail.from:GymProFit <no-reply@gymprofit.app>}")
    private String remitente;

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
            // Sin SMTP configurado. No es un error: es el modo de desarrollo.
            logger.warn("SMTP no configurado; código de recuperación de '{}' (válido {} min): {}",
                    usuario.getUsername(), minutosValidez, codigo);
            return;
        }

        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, StandardCharsets.UTF_8.name());
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
