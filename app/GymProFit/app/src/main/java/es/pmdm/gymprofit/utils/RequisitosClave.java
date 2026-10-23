package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

// ============================================================
// RequisitosClave — los requisitos que se marcan solos mientras se escribe (GP-216,
// tablero 20): la contraseña nueva y, con el mismo patrón, el correo nuevo.
//
// Las reglas de la contraseña son las de PoliticaCuenta (DEC-034): 8 caracteres como
// mínimo, contados como caracteres y no como unidades UTF-16, y 72 bytes en UTF-8 como
// máximo; sin reglas de composición. Se evalúa sobre el texto recortado, porque la app
// recorta los espacios de los extremos antes de enviar. La lista de contraseñas comunes y el
// nombre los comprueba la API, que responde con un código que la pantalla pone en el campo.
// Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class RequisitosClave {

    /** Con la contraseña nueva escrita, ocho caracteres como mínimo. */
    public final boolean longitud;
    /** Distinta de la actual; sin la actual escrita no se acusa a nadie. */
    public final boolean distinta;
    /** Cabe en 72 bytes UTF-8, lo que admite BCrypt. */
    public final boolean cabe;
    /** Una frase (tres palabras o más, 15 caracteres o más): un extra, no un requisito. */
    public final boolean frase;
    /** La barra de fortaleza: de 0 (vacía) a 4. */
    public final int segmentos;

    private RequisitosClave(boolean longitud, boolean distinta, boolean cabe, boolean frase, int segmentos) {
        this.longitud = longitud;
        this.distinta = distinta;
        this.cabe = cabe;
        this.frase = frase;
        this.segmentos = segmentos;
    }

    /**
     * Evalúa la contraseña nueva.
     *
     * @param nueva  lo que hay en el campo de la nueva; se recorta.
     * @param actual lo que hay en el de la actual; se recorta. Vacío si aún no se ha escrito.
     */
    @NonNull
    public static RequisitosClave de(@Nullable String nueva, @Nullable String actual) {
        String n = nueva == null ? "" : nueva.trim();
        String a = actual == null ? "" : actual.trim();
        int caracteres = n.codePointCount(0, n.length());
        boolean longitud = caracteres >= PoliticaCuenta.PASSWORD_MIN_CARACTERES;
        boolean cabe = n.getBytes(StandardCharsets.UTF_8).length <= PoliticaCuenta.PASSWORD_MAX_BYTES;
        boolean distinta = a.isEmpty() || !n.equals(a);
        boolean frase = caracteres >= 15 && n.split("\\s+").length >= 3;

        int segmentos;
        if (n.isEmpty()) segmentos = 0;
        else if (!cabe || caracteres < PoliticaCuenta.PASSWORD_MIN_CARACTERES) segmentos = 1;
        else if (frase) segmentos = 4;
        else if (caracteres >= 12) segmentos = 3;
        else segmentos = 2;
        return new RequisitosClave(longitud, distinta, cabe, frase, segmentos);
    }

    /** Se puede enviar: ocho caracteres, distinta de la actual y dentro de los 72 bytes. */
    public boolean aceptable() {
        return longitud && distinta && cabe;
    }

    // ── Correo ──────────────────────────────────────────────────────────────────

    // Algo, una @, algo, un punto y algo: la forma. El patrón de Android, más fino, decide al enviar.
    private static final Pattern FORMA = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** Los requisitos del correo nuevo, con el mismo patrón que la contraseña. */
    public static final class CorreoNuevo {
        public final boolean forma;
        public final boolean distinto;
        public final boolean cabeCorreo;

        CorreoNuevo(boolean forma, boolean distinto, boolean cabeCorreo) {
            this.forma = forma;
            this.distinto = distinto;
            this.cabeCorreo = cabeCorreo;
        }

        public boolean aceptable() {
            return forma && distinto && cabeCorreo;
        }
    }

    /**
     * Evalúa el correo nuevo.
     *
     * @param nuevo  lo escrito; se recorta.
     * @param actual el correo de la cuenta, o null si aún no ha cargado (entonces no se acusa a nadie).
     */
    @NonNull
    public static CorreoNuevo correo(@Nullable String nuevo, @Nullable String actual) {
        String n = nuevo == null ? "" : nuevo.trim();
        boolean forma = FORMA.matcher(n).matches();
        // Sin distinguir mayúsculas, como la restricción única de la base.
        boolean distinto = actual == null || !n.equalsIgnoreCase(actual.trim());
        return new CorreoNuevo(forma, distinto, PoliticaCuenta.correoLongitudValida(n));
    }
}
