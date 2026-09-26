package es.pmdm.gymprofit.utils;

import java.nio.charset.StandardCharsets;

// ============================================================
// PoliticaCuenta — las reglas de los datos de una cuenta, copiadas de la API.
//
// Una sola fuente para el registro y para recuperar contraseña (GP-095): si la app
// da por buena una contraseña que la API rechaza, el usuario solo ve un error suelto.
//
// Contraseña (GP-101, DEC-034), la de @ContrasenaNueva en la API: mínimo 8
// CARACTERES, contados como caracteres y no como unidades UTF-16, y máximo 72 BYTES en
// UTF-8, lo que admite BCrypt. Sin reglas de composición. La lista de contraseñas
// comunes y el nombre los comprueba la API, que responde con un código en "cause".
// Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class PoliticaCuenta {

    public static final int PASSWORD_MIN_CARACTERES = 8;
    public static final int PASSWORD_MAX_BYTES = 72;
    public static final int USUARIO_MIN = 3;
    public static final int USUARIO_MAX = 50;
    public static final int CORREO_MAX = 100;

    // Códigos que manda la API en "cause" cuando el registro choca con otra cuenta.
    static final String CODIGO_USUARIO_EN_USO = "USERNAME_EN_USO";
    static final String CODIGO_CORREO_EN_USO = "EMAIL_EN_USO";
    // Y cuando la contraseña nueva no vale para la cuenta (GP-101).
    static final String CODIGO_PASSWORD_COMUN = "PASSWORD_COMUN";
    static final String CODIGO_PASSWORD_CONTIENE_NOMBRE = "PASSWORD_CONTIENE_NOMBRE";

    /** Qué dato del registro tiene ya otra cuenta, según el código de la API. */
    public enum CampoEnUso { USUARIO, CORREO, DESCONOCIDO }

    /** Qué le pasa a una contraseña antes de enviarla. */
    public enum ProblemaPassword { CORTA, LARGA }

    /** Por qué la API ha rechazado una contraseña nueva de forma válida. */
    public enum RechazoPassword { COMUN, CONTIENE_NOMBRE, NINGUNO }

    private PoliticaCuenta() {}

    /**
     * Qué le falta a una contraseña para que la API la acepte en forma, o null si nada.
     *
     * @param password contraseña tal cual se enviará, o null.
     */
    public static ProblemaPassword problemaPassword(String password) {
        if (password == null || password.codePointCount(0, password.length()) < PASSWORD_MIN_CARACTERES) {
            return ProblemaPassword.CORTA;
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            return ProblemaPassword.LARGA;
        }
        return null;
    }

    /** Si la API aceptará la forma de esta contraseña. */
    public static boolean passwordValida(String password) {
        return problemaPassword(password) == null;
    }

    /**
     * Lee del cuerpo de un 400 si la API ha rechazado la contraseña nueva y por qué.
     * Busca el código y no el texto, igual que {@link #campoEnUso(String)}.
     *
     * @param cuerpo cuerpo de error tal cual, o null.
     */
    public static RechazoPassword rechazoPassword(String cuerpo) {
        if (cuerpo == null) return RechazoPassword.NINGUNO;
        if (cuerpo.contains(CODIGO_PASSWORD_COMUN)) return RechazoPassword.COMUN;
        if (cuerpo.contains(CODIGO_PASSWORD_CONTIENE_NOMBRE)) return RechazoPassword.CONTIENE_NOMBRE;
        return RechazoPassword.NINGUNO;
    }

    /**
     * Dice si el nombre de usuario cabe en lo que pide la API: de 3 a 50 caracteres.
     * No limita qué caracteres: la API tampoco lo hace.
     *
     * @param usuario nombre ya recortado, o null.
     */
    public static boolean usuarioValido(String usuario) {
        return usuario != null
                && usuario.length() >= USUARIO_MIN
                && usuario.length() <= USUARIO_MAX;
    }

    /**
     * Dice si el correo no pasa del máximo de la API. El formato se comprueba aparte,
     * con el patrón de Android, que no existe en la JVM de los tests.
     *
     * @param correo correo ya recortado, o null.
     */
    public static boolean correoLongitudValida(String correo) {
        return correo != null && correo.length() <= CORREO_MAX;
    }

    /**
     * Lee del cuerpo de un 400 del registro qué dato está en uso.
     *
     * <p>Busca el código y no el texto, que puede cambiar. Tampoco parsea: sin
     * cabecera Accept la API puede responder en XML, y el código aparece igual.
     * Una API anterior a GP-095 no manda código: entonces es DESCONOCIDO y no se
     * adivina por el mensaje.
     *
     * @param cuerpo cuerpo de error tal cual, o null.
     */
    public static CampoEnUso campoEnUso(String cuerpo) {
        if (cuerpo == null) return CampoEnUso.DESCONOCIDO;
        if (cuerpo.contains(CODIGO_USUARIO_EN_USO)) return CampoEnUso.USUARIO;
        if (cuerpo.contains(CODIGO_CORREO_EN_USO)) return CampoEnUso.CORREO;
        return CampoEnUso.DESCONOCIDO;
    }
}
