package com.gymprofit.api.exceptions;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

// ============================================================
// ExcepcionConClave — base de los errores que llegan al cliente (GP-109)
//
// El texto no se escribe en el throw: se lleva una clave de messages.properties
// y sus argumentos, y ControllerExceptionHandler la resuelve en el idioma de la
// petición (español por defecto, inglés con Accept-Language: en).
//
// getMessage() sigue devolviendo el texto en español, resuelto al construir la
// excepción, para que el log y los tests lean lo mismo que antes. Una clave que no
// exista revienta aquí mismo con MissingResourceException, y ClavesErrorTest
// comprueba que todas las que usa el código están en los dos idiomas.
// ============================================================
public abstract class ExcepcionConClave extends RuntimeException {

    // messages.properties es el español; sin caer al idioma del sistema.
    private static final ResourceBundle ESPANOL = ResourceBundle.getBundle("messages", Locale.ROOT,
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));

    private final String clave;
    private final Object[] args;

    protected ExcepcionConClave(String clave, Object[] args, Throwable cause) {
        super(resolver(clave, textos(args)), cause);
        this.clave = clave;
        this.args = textos(args);
    }

    /** Clave del mensaje en messages*.properties. */
    public String getClave() {
        return clave;
    }

    /** Argumentos del mensaje, ya pasados a texto. */
    public Object[] getArgs() {
        return args.clone();
    }

    // Los argumentos van como texto: MessageFormat formatearía un id 1234 como «1.234».
    private static String[] textos(Object[] args) {
        return args == null ? new String[0] : Arrays.stream(args).map(String::valueOf).toArray(String[]::new);
    }

    // Igual que Spring: sin argumentos el patrón no pasa por MessageFormat, así que
    // un apóstrofo suelto se queda como está.
    private static String resolver(String clave, Object[] args) {
        String patron = ESPANOL.getString(clave);
        return args.length == 0 ? patron : new MessageFormat(patron, Locale.ROOT).format(args);
    }
}
