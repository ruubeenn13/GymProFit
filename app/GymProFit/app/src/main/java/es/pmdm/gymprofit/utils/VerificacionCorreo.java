package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// VerificacionCorreo — lo que decide la pantalla del código y el aviso de Perfil (GP-045)
//
// Sin Android dentro, para probarlo en la JVM. La cuenta se usa entera sin verificar
// (DEC-066): nada de aquí bloquea, solo decide qué se enseña.
// ============================================================
public final class VerificacionCorreo {

    private VerificacionCorreo() { }

    /** Qué pasó al pedir otro código. */
    public enum Reenvio {
        /** Código emitido: llegará al correo. */
        ENVIADO,
        /** El correo ya estaba verificado (en otro móvil, por ejemplo). */
        YA_VERIFICADO,
        /** Red, límite de peticiones o servidor: se enseña el error y se puede repetir. */
        FALLO
    }

    /** Qué pasó al mandar el código. */
    public enum Canje {
        /** Correo verificado. */
        VERIFICADO,
        /** Código equivocado, caducado o agotado: el error va al campo. */
        CODIGO_MALO,
        /** Red, límite de peticiones o servidor. */
        FALLO
    }

    /**
     * Dice si lo escrito es un código entero: seis dígitos, sin contar los espacios de
     * los extremos (los pega así el portapapeles de algunos correos).
     *
     * @param texto lo que hay en el campo.
     * @return true si se puede mandar.
     */
    public static boolean codigoCompleto(@Nullable CharSequence texto) {
        return texto != null && texto.toString().trim().matches("\\d{6}");
    }

    /**
     * Dice si Perfil enseña el aviso de correo sin verificar.
     *
     * @param verificado el {@code emailVerificado} del perfil; null si la API no lo manda.
     * @return true solo si la API dice que no está verificado. Con null no se avisa: un
     *         aviso que nunca se puede quitar es peor que ninguno.
     */
    public static boolean mostrarAviso(@Nullable Boolean verificado) {
        return Boolean.FALSE.equals(verificado);
    }

    /**
     * Traduce la respuesta de {@code POST /usuarios/me/email/reenviar}.
     *
     * @param code estado HTTP, o un negativo sin respuesta.
     */
    @NonNull
    public static Reenvio reenvio(int code) {
        if (code >= 200 && code < 300) return Reenvio.ENVIADO;
        if (code == 409) return Reenvio.YA_VERIFICADO;
        return Reenvio.FALLO;
    }

    /**
     * Traduce la respuesta de {@code POST /usuarios/me/email/verificar}.
     *
     * @param code estado HTTP, o un negativo sin respuesta.
     */
    @NonNull
    public static Canje canje(int code) {
        if (code >= 200 && code < 300) return Canje.VERIFICADO;
        if (code == 400) return Canje.CODIGO_MALO;
        return Canje.FALLO;
    }

    // ── Diseño nuevo: lo que la app cuenta por su cuenta ────────────────────
    // La API da el mismo 400 a todo fallo (DEC-066) y no dice cuánto le queda al código ni
    // cuántos intentos. El móvil lo lleva desde que se emitió (PreferencesManager), con las
    // mismas reglas que el servidor (CodigoUnSoloUso): si se desvía, lo que manda es el 400.

    /** Vida del código, la de la API. */
    public static final int MINUTOS = 15;

    /** Intentos por código, los de la API. */
    public static final int MAX_INTENTOS = 5;

    private static final long MS_VIDA = MINUTOS * 60_000L;

    /**
     * Dice si el código ya no vale: pasaron 15 minutos, se gastaron los cinco intentos, o
     * no se sabe cuándo se mandó (se entra desde Perfil días después).
     *
     * @param emitidoEn cuándo se emitió, en ms; 0 o menos si no se sabe.
     * @param fallos    códigos equivocados desde entonces.
     * @param ahora     la hora, en ms.
     */
    public static boolean caducado(long emitidoEn, int fallos, long ahora) {
        return emitidoEn <= 0 || fallos >= MAX_INTENTOS || ahora - emitidoEn >= MS_VIDA;
    }

    /** Milisegundos que le quedan al código; 0 si ya caducó. */
    public static long msHastaCaducar(long emitidoEn, long ahora) {
        return Math.max(0L, emitidoEn + MS_VIDA - ahora);
    }

    /** Intentos que le quedan al código tras {@code fallos} equivocados. */
    public static int intentosRestantes(int fallos) {
        return Math.max(0, MAX_INTENTOS - fallos);
    }

    /**
     * Las cifras de un texto, hasta seis: lo que se pega desde un correo puede traer
     * espacios, guiones o el «Tu código:» de delante.
     */
    @NonNull
    public static String soloCifras(@Nullable CharSequence texto) {
        if (texto == null) return "";
        StringBuilder b = new StringBuilder(6);
        for (int i = 0; i < texto.length() && b.length() < 6; i++) {
            char c = texto.charAt(i);
            if (c >= '0' && c <= '9') b.append(c);
        }
        return b.toString();
    }

    /**
     * Dice si un texto copiado es un código: exactamente seis cifras, con lo que haya
     * alrededor (espacios, guiones). Siete cifras no son un código nuestro.
     */
    public static boolean seisCifras(@Nullable CharSequence texto) {
        if (texto == null) return false;
        int cifras = 0;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c >= '0' && c <= '9') cifras++;
        }
        return cifras == 6;
    }

    /** «0:42»: la espera del reenvío en minutos y segundos. */
    @NonNull
    public static String cuentaAtras(int segundos) {
        int s = Math.max(0, segundos);
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }
}
