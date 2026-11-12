package com.gymprofit.api.config;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.regex.Pattern;

// ============================================================
// FiltroDatosSentry — lo último que pasa cada aviso antes de salir hacia Sentry (GP-221).
//
// sendDefaultPii=false ya impide que el SDK adjunte IP, usuario y cookies, pero eso es una
// opción que cualquiera puede cambiar sin darse cuenta. Este filtro lo garantiza aunque
// alguien la encienda:
//   · De la petición solo quedan el método y la ruta. Fuera el cuerpo, las cabeceras
//     (Authorization incluida), las cookies, la query (algunas rutas llevan un código en
//     ella) y las variables del servidor.
//   · Sin usuario, sin breadcrumbs y sin extras.
//   · En el mensaje y en el texto de cada excepción se tapan los correos, los JWT y lo que
//     venga tras «Bearer»: hay excepciones que repiten el dato que no encontraron.
//
// Es un bean, así que la autoconfiguración de Sentry lo engancha sola. Sin SENTRY_DSN no se
// usa nunca: no hay SDK que llame a execute.
// ============================================================
@Component
public class FiltroDatosSentry implements SentryOptions.BeforeSendCallback {

    private static final Pattern CORREO = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern JWT = Pattern.compile("eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+");
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+\\S+");

    /**
     * Deja el aviso sin datos personales ni credenciales.
     *
     * @param evento el aviso que va a salir
     * @param pista  datos extra del SDK; no se usan
     * @return el mismo aviso, ya limpio
     */
    @Override
    public SentryEvent execute(SentryEvent evento, Hint pista) {
        Request peticion = evento.getRequest();
        if (peticion != null) {
            Request limpia = new Request();
            limpia.setMethod(peticion.getMethod());
            limpia.setUrl(peticion.getUrl());
            evento.setRequest(limpia);
        }
        evento.setUser(null);
        evento.setBreadcrumbs(new ArrayList<>());
        evento.setExtras(null);

        Message mensaje = evento.getMessage();
        if (mensaje != null) {
            mensaje.setFormatted(tapar(mensaje.getFormatted()));
            mensaje.setMessage(tapar(mensaje.getMessage()));
            mensaje.setParams(null);
        }
        if (evento.getExceptions() != null) {
            for (SentryException e : evento.getExceptions()) {
                e.setValue(tapar(e.getValue()));
            }
        }
        return evento;
    }

    /** Tapa correos, JWT y tokens Bearer de un texto. */
    static String tapar(String texto) {
        if (texto == null) return null;
        String limpio = BEARER.matcher(texto).replaceAll("Bearer [tapado]");
        limpio = JWT.matcher(limpio).replaceAll("[jwt tapado]");
        return CORREO.matcher(limpio).replaceAll("[correo tapado]");
    }
}
