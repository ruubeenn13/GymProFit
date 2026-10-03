package es.pmdm.gymprofit.utils;

// ============================================================
// ErrorFoto — por qué no se ha subido la foto de perfil (GP-188, lote 1.6.5)
// Antes era «Error al subir la foto» para todo. Ahora, por su causa: sin conexión (no se
// ha podido hablar con la API y el móvil no tiene red), demasiado grande (413) o lo demás,
// que dice UiFeedback como en el resto de la app.
// ============================================================
public final class ErrorFoto {

    public enum Causa { SIN_CONEXION, PESA_DEMASIADO, OTRA }

    private ErrorFoto() {
    }

    /**
     * @param code   el código HTTP; -1 si no hubo respuesta.
     * @param hayRed si el móvil tiene red ahora.
     */
    public static Causa causa(int code, boolean hayRed) {
        if (code == 413) return Causa.PESA_DEMASIADO;
        if (code == -1 && !hayRed) return Causa.SIN_CONEXION;
        return Causa.OTRA;
    }
}
