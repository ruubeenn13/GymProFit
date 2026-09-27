package com.gymprofit.api.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

// ============================================================
// MensajesError — textos de error en el idioma de la petición (GP-109)
//
// Español por defecto (spring.web.locale=es) y en inglés con Accept-Language: en.
// Dentro de Spring MVC el idioma ya está en LocaleContextHolder; los filtros de
// seguridad corren antes del DispatcherServlet y lo sacan de la propia petición
// con el mismo LocaleResolver, para que un 401 o un 429 hablen igual que un 400.
// ============================================================
@Component
@RequiredArgsConstructor
public class MensajesError {

    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    /**
     * Texto de una clave en el idioma de la petición en curso (dentro de Spring MVC).
     *
     * @param clave clave de messages*.properties
     * @param args  argumentos del mensaje
     */
    public String texto(String clave, Object... args) {
        return messageSource.getMessage(clave, args, LocaleContextHolder.getLocale());
    }

    /**
     * Texto de una clave en el idioma de una petición que aún no ha llegado a Spring MVC
     * (filtros y puntos de entrada de seguridad).
     *
     * @param request petición de la que sale el Accept-Language
     * @param clave   clave de messages*.properties
     * @param args    argumentos del mensaje
     */
    public String texto(HttpServletRequest request, String clave, Object... args) {
        return messageSource.getMessage(clave, args, localeResolver.resolveLocale(request));
    }

    /**
     * Texto de un estado HTTP de error sin manejador propio (405, 415…). Si no hay
     * clave para ese estado, queda la frase estándar en inglés.
     *
     * @param estado código HTTP
     * @param frase  frase estándar del estado
     */
    public String textoEstado(int estado, String frase) {
        return messageSource.getMessage("error.http." + estado, null, frase, LocaleContextHolder.getLocale());
    }

    /**
     * Texto de una excepción con clave. Si la clave faltara en el idioma pedido, queda el
     * español con el que se construyó.
     *
     * @param ex excepción de dominio
     */
    public String texto(ExcepcionConClave ex) {
        return messageSource.getMessage(ex.getClave(), ex.getArgs(), ex.getMessage(),
                LocaleContextHolder.getLocale());
    }
}
