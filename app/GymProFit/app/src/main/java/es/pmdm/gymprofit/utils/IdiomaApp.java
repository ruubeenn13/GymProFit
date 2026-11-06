package es.pmdm.gymprofit.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// ============================================================
// IdiomaApp — en qué idioma se pinta la app cuando el usuario no ha elegido ninguno (GP-240).
//
// La app tiene dos idiomas: español (values/) e inglés (values-en/). Sin idioma elegido sigue
// la lista de idiomas del sistema entre esos dos —el primero que sea español o inglés— y, si
// no hay ninguno, español. Todo va en ese idioma: los textos de la app, los de AppCompat y
// Material (el filtro de idiomas de app/build.gradle deja en la APK solo «es» y «en»), y las
// fechas y los números, que salen de locale(): en-US o es-ES.
//
// Antes, con el sistema en [francés, inglés] Android elegía el francés de las librerías y la
// app salía en español (values/); y con [catalán, español] las fechas salían en catalán,
// porque se formateaban con el primer idioma de la configuración.
//
// Con idioma elegido manda ese (AppCompatDelegate.setApplicationLocales, GymProFitApp).
// ============================================================
public final class IdiomaApp {

    private static final Locale ESPANOL = new Locale("es", "ES");

    private IdiomaApp() { }

    /**
     * El idioma en que se pinta la app.
     *
     * @param elegido        el que guardó el usuario («es», «en») o vacío si no eligió.
     * @param idiomasSistema la lista de idiomas del sistema, en orden, como etiquetas BCP 47.
     * @return «es» o «en».
     */
    public static String resolver(String elegido, List<String> idiomasSistema) {
        if ("en".equals(elegido)) return "en";
        if ("es".equals(elegido)) return "es";
        if (idiomasSistema != null) {
            for (String etiqueta : idiomasSistema) {
                if (etiqueta == null) continue;
                String idioma = Locale.forLanguageTag(etiqueta).getLanguage();
                if ("es".equals(idioma) || "en".equals(idioma)) return idioma;
            }
        }
        return "es";
    }

    /** Locale de formato para unos recursos en inglés o en español: en-US o es-ES. */
    public static Locale locale(boolean recursosEnIngles) {
        return recursosEnIngles ? Locale.US : ESPANOL;
    }

    /** {@link #locale(boolean)} a partir de «en» o «es». */
    public static Locale locale(String idioma) {
        return locale("en".equals(idioma));
    }

    /**
     * El idioma de la app sin idioma elegido, o {@code null} si el usuario eligió uno (y
     * entonces lo aplica AppCompat). Lee la lista del sistema, no la del proceso: esa la
     * cambia {@link #fijarPorDefecto}.
     */
    public static Locale sinElegir(Context ctx) {
        String elegido = PreferencesManager.idiomaGuardado(ctx);
        if (elegido != null && !elegido.isEmpty()) return null;
        return locale(resolver(null, idiomasDelSistema()));
    }

    /**
     * Pone el idioma de la app sin idioma elegido en una configuración que se va a aplicar
     * encima de la de una pantalla (ScaleUtils.wrap) o de un contexto (notificaciones).
     */
    public static void aplicarSinElegir(Context ctx, Configuration override) {
        Locale l = sinElegir(ctx);
        if (l != null) override.setLocales(new LocaleList(l));
    }

    /**
     * El idioma por defecto del proceso, el que usan las librerías que formatean con
     * {@code Locale.getDefault()} (el calendario de Material) y la cabecera Accept-Language:
     * sin idioma elegido, el de la app. Android lo vuelve a poner en el del sistema con cada
     * cambio de configuración, así que GymProFitApp lo llama también entonces.
     */
    public static void fijarPorDefecto(Context ctx) {
        Locale l = sinElegir(ctx);
        if (l != null) LocaleList.setDefault(new LocaleList(l));
    }

    private static List<String> idiomasDelSistema() {
        LocaleList lista = Resources.getSystem().getConfiguration().getLocales();
        List<String> etiquetas = new ArrayList<>(lista.size());
        for (int i = 0; i < lista.size(); i++) etiquetas.add(lista.get(i).toLanguageTag());
        return etiquetas;
    }
}
