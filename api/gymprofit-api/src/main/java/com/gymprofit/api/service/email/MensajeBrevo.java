package com.gymprofit.api.service.email;

import java.util.List;

// ============================================================
// MensajeBrevo — cuerpo JSON de POST https://api.brevo.com/v3/smtp/email
//
// Los nombres de los campos son los que exige el proveedor, por eso están en inglés
// y no siguen la nomenclatura del resto del proyecto: son el contrato de un tercero.
// ============================================================
record MensajeBrevo(Remitente sender,
                    List<Destinatario> to,
                    String subject,
                    String htmlContent,
                    String textContent) {

    /** Remitente. La API quiere nombre y dirección por separado, no "Nombre <correo>". */
    record Remitente(String name, String email) {
    }

    /** Destinatario. El nombre es opcional para Brevo, pero mejora cómo se ve el correo. */
    record Destinatario(String email, String name) {
    }
}
