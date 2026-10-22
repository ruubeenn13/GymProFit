package es.pmdm.gymprofit.envivo;

import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

// ============================================================
// IdiomaContexto — el contexto de la aplicación en el idioma que eligió el usuario.
//
// Las notificaciones se pintan desde la clase Application o desde un receptor, con un
// contexto que no sigue el idioma de la app (el de las pantallas sí): con la app en inglés
// salían en español. Aquí se le pone el idioma elegido; si no hay ninguno, el del sistema.
// ============================================================
final class IdiomaContexto {

    private IdiomaContexto() { }

    /** El mismo contexto con el idioma de la app, o él mismo si no se eligió ninguno. */
    @NonNull
    static Context de(@NonNull Context ctx) {
        LocaleListCompat elegidos = AppCompatDelegate.getApplicationLocales();
        if (elegidos.isEmpty() || elegidos.get(0) == null) return ctx;
        Configuration c = new Configuration(ctx.getResources().getConfiguration());
        c.setLocale(elegidos.get(0));
        return ctx.createConfigurationContext(c);
    }
}
