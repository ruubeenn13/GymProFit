package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// NombreVisible — el nombre que se enseña del usuario (GP-116).
//
// El nombre para mostrar es opcional y aparte del de usuario: con él se saluda en
// Inicio, se titula el perfil en Progreso y se sacan las iniciales del avatar. Sin él,
// el de usuario, como antes de la 1.1.3. El «@usuario» del subtítulo sigue siendo el
// de usuario siempre.
// ============================================================
public final class NombreVisible {

    private NombreVisible() {}

    /**
     * El nombre para mostrar recortado o, si no hay o está en blanco, el de usuario.
     *
     * @return nunca null; vacío si no hay ninguno de los dos.
     */
    @NonNull
    public static String de(@Nullable String nombre, @Nullable String username) {
        if (nombre != null && !nombre.trim().isEmpty()) return nombre.trim();
        return username == null ? "" : username;
    }

    /**
     * Lo que se manda a la API en el campo {@code nombre} del PATCH.
     *
     * <p>Recortado, y vacío si está en blanco: la API ignora un null, así que borrar el
     * nombre es mandarlo en blanco.
     */
    @NonNull
    public static String paraEnviar(@Nullable CharSequence escrito) {
        return escrito == null ? "" : escrito.toString().trim();
    }
}
