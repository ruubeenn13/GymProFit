package es.pmdm.gymprofit.utils;

import java.util.regex.Pattern;

// ============================================================
// PoliticaCuenta — las reglas de los datos de una cuenta, copiadas de la API.
//
// Una sola fuente para el registro y para recuperar contraseña (GP-095). El
// registro se había quedado en «al menos 6 caracteres» mientras la API pedía de 8 a
// 100 con minúscula, mayúscula, dígito y símbolo: la app daba por buena una
// contraseña que la API rechazaba con 400, y el usuario solo veía «Error al crear
// la cuenta».
//
// Tiene que aceptar EXACTAMENTE lo que acepta RegisterDTO. Por eso el dígito es
// [0-9] y no \d: en Android \d casa también con dígitos de otras escrituras, y en
// la API no. Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class PoliticaCuenta {

    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 100;
    public static final int USUARIO_MIN = 3;
    public static final int USUARIO_MAX = 50;
    public static final int CORREO_MAX = 100;

    // Mismo patrón que el @Pattern de RegisterDTO; la longitud va aparte, como su @Size.
    private static final Pattern PASSWORD =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$");

    // Códigos que manda la API en "cause" cuando el registro choca con otra cuenta.
    static final String CODIGO_USUARIO_EN_USO = "USERNAME_EN_USO";
    static final String CODIGO_CORREO_EN_USO = "EMAIL_EN_USO";

    /** Qué dato del registro tiene ya otra cuenta, según el código de la API. */
    public enum CampoEnUso { USUARIO, CORREO, DESCONOCIDO }

    private PoliticaCuenta() {}

    /**
     * Dice si la API aceptará esta contraseña: de 8 a 100 caracteres, con
     * minúscula, mayúscula, dígito y símbolo.
     *
     * @param password contraseña tal cual se enviará, o null.
     */
    public static boolean passwordValida(String password) {
        return password != null
                && password.length() >= PASSWORD_MIN
                && password.length() <= PASSWORD_MAX
                && PASSWORD.matcher(password).matches();
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
