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
// salían en español. Aquí se le pone el idioma elegido; si no hay ninguno, el que resulta de
// la lista del sistema entre los de la app (IdiomaApp, GP-240).
// ============================================================
final class IdiomaContexto {

    private IdiomaContexto() { }

    /** El mismo contexto con el idioma de la app, o él mismo si no se eligió ninguno. */
    @NonNull
    static Context de(@NonNull Context ctx) {
        LocaleListCompat elegidos = AppCompatDelegate.getApplicationLocales();
        java.util.Locale idioma = elegidos.isEmpty() || elegidos.get(0) == null
                ? es.pmdm.gymprofit.utils.IdiomaApp.sinElegir(ctx) : elegidos.get(0);
        if (idioma == null) return ctx;
        Configuration c = new Configuration(ctx.getResources().getConfiguration());
        c.setLocale(idioma);
        return ctx.createConfigurationContext(c);
    }
}
