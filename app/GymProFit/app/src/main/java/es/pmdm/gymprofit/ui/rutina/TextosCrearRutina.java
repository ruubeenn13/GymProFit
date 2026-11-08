package es.pmdm.gymprofit.ui.rutina;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.BorradorRutina;
import es.pmdm.gymprofit.utils.Enumeracion;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// TextosCrearRutina — lo que se dice de un ejercicio al crear o editar una rutina (GP-244).
//
// Todo en frase y sin «·»: la fila de «Elige ejercicios» dice «Pecho, con barra» y la de «Tu
// rutina», «3 series de 8 a 12». El equipo del catálogo llega como un nombre suelto («Barra»,
// «Barbell»); los habituales se dicen con su preposición («con barra», «en polea») y el resto,
// tal cual en minúscula. Las zonas del paso 1 son los GrupoMuscular de la API.
// ============================================================
public final class TextosCrearRutina {

    private TextosCrearRutina() { }

    /** El nombre de una zona del paso 1 («Pecho», «Abdomen»). */
    @StringRes
    public static int zona(@Nullable String grupo) {
        if (grupo == null) return R.string.crear_zona_abdomen;
        switch (grupo) {
            case "PECHO": return R.string.crear_zona_pecho;
            case "ESPALDA": return R.string.crear_zona_espalda;
            case "PIERNAS": return R.string.crear_zona_pierna;
            case "HOMBROS": return R.string.crear_zona_hombros;
            case "BRAZOS": return R.string.crear_zona_brazos;
            default: return R.string.crear_zona_abdomen;
        }
    }

    /** Sus dos ejemplos («Press de banca y aperturas»). */
    @StringRes
    public static int ejemplos(@Nullable String grupo) {
        if (grupo == null) return R.string.crear_zona_abdomen_ej;
        switch (grupo) {
            case "PECHO": return R.string.crear_zona_pecho_ej;
            case "ESPALDA": return R.string.crear_zona_espalda_ej;
            case "PIERNAS": return R.string.crear_zona_pierna_ej;
            case "HOMBROS": return R.string.crear_zona_hombros_ej;
            case "BRAZOS": return R.string.crear_zona_brazos_ej;
            default: return R.string.crear_zona_abdomen_ej;
        }
    }

    /**
     * El equipo en frase: el recurso de los habituales, o 0 si no es uno de ellos.
     *
     * @param equipo como viene del catálogo, en español o en inglés.
     */
    @StringRes
    public static int equipo(@Nullable String equipo) {
        if (equipo == null) return 0;
        String e = equipo.trim().toLowerCase(Locale.ROOT);
        if (e.equals("barra z") || e.equals("ez-bar") || e.equals("ez bar")) return R.string.equipo_barra_z;
        if (e.equals("barra") || e.equals("barbell")) return R.string.equipo_barra;
        if (e.equals("mancuernas") || e.equals("mancuerna") || e.equals("dumbbell") || e.equals("dumbbells")) {
            return R.string.equipo_mancuernas;
        }
        if (e.equals("polea") || e.equals("cable")) return R.string.equipo_polea;
        if (e.equals("máquina") || e.equals("maquina") || e.equals("machine")) return R.string.equipo_maquina;
        if (e.equals("kettlebell")) return R.string.equipo_kettlebell;
        if (e.equals("banda elástica") || e.equals("banda elastica") || e.equals("resistance band")) {
            return R.string.equipo_banda;
        }
        if (e.equals("sin equipo") || e.equals("bodyweight") || e.equals("body only")) return R.string.equipo_sin;
        return 0;
    }

    /** «Pecho, con barra»; sin equipo conocido, «Pecho, balón medicinal»; sin equipo, «Pecho». */
    @NonNull
    public static String detalle(@NonNull Context ctx, @Nullable String grupo, @Nullable String equipo) {
        String zona = UIHelper.traducirGrupoMuscular(ctx, grupo);
        if (equipo == null || equipo.trim().isEmpty()) return zona;
        int frase = equipo(equipo);
        Locale idioma = FechaUtils.localeDeLaApp(ctx);
        String e = frase != 0 ? ctx.getString(frase) : Enumeracion.enMedio(equipo.trim(), idioma);
        return ctx.getString(R.string.elegir_ej_fila_detalle, zona, e);
    }

    /** «3 series de 8 a 12», «3 series de 30 a 45 segundos», con «por pierna» si va por lado. */
    @NonNull
    public static String pauta(@NonNull Context ctx, @NonNull BorradorRutina.Ejercicio e) {
        boolean rango = e.max != e.min;
        String cifra = rango ? ctx.getString(R.string.apuntar_pauta_rango, e.min, e.max) : String.valueOf(e.min);
        String cantidad = e.segundos
                ? ctx.getResources().getQuantityString(R.plurals.apuntar_pauta_segundos, e.max, cifra)
                : cifra;
        if (e.porLado != null) {
            switch (e.porLado) {
                case "PIERNA": cantidad = ctx.getString(R.string.apuntar_pauta_por_pierna, cantidad); break;
                case "BRAZO": cantidad = ctx.getString(R.string.apuntar_pauta_por_brazo, cantidad); break;
                case "LADO": cantidad = ctx.getString(R.string.apuntar_pauta_por_lado, cantidad); break;
                default: break;
            }
        }
        return ctx.getResources().getQuantityString(R.plurals.apuntar_pauta_series, e.series, e.series, cantidad);
    }

    /** El nivel en minúscula para ir en medio de una frase («intermedio»). */
    @NonNull
    public static String nivel(@NonNull Context ctx, @Nullable String nivel) {
        return UIHelper.traducirNivel(ctx, nivel).toLowerCase(FechaUtils.localeDeLaApp(ctx));
    }
}
