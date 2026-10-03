package es.pmdm.gymprofit.ui.nutricion;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.ComidaReciente;
import es.pmdm.gymprofit.utils.ComidaQueToca;
import es.pmdm.gymprofit.utils.ComidasRecientes;
import es.pmdm.gymprofit.utils.FechaUtils;

// ============================================================
// CopiarComida — los textos de copiar una comida (lote 1.6.4)
// «Merienda de ayer», «Almuerzo de hoy», «Cena del lunes», «Comida del 28 de septiembre»
// (ComidasRecientes dice cuál toca); lo que lleva, en una línea («Pan integral, Pechuga
// de pavo · 203 kcal»); y el «Sí, copiar…» de cada comida, para TalkBack.
// ============================================================
public final class CopiarComida {

    private static final int[] SI_COPIAR = {R.string.copiar_ayer_desayuno, R.string.copiar_ayer_almuerzo,
            R.string.copiar_ayer_comida, R.string.copiar_ayer_merienda, R.string.copiar_ayer_cena};
    private static final int[] SUELES = {R.string.sueles_desayuno, R.string.sueles_almuerzo,
            R.string.sueles_comida, R.string.sueles_merienda, R.string.sueles_cena};

    private CopiarComida() {
    }

    /** «Merienda de ayer», frente al día de hoy. */
    @NonNull
    public static String nombre(@NonNull Context ctx, @NonNull ComidaReciente c) {
        Locale idioma = FechaUtils.localeDeLaApp(ctx);
        String comida = ctx.getString(ComidaQueToca.enFrase(c.getTipoComida()));
        Calendar dia = ComidasRecientes.dia(c.getFecha());
        String texto;
        switch (ComidasRecientes.cuando(c.getFecha() != null ? c.getFecha() : "", Calendar.getInstance())) {
            case HOY:
                texto = ctx.getString(R.string.reciente_hoy, comida);
                break;
            case AYER:
                texto = ctx.getString(R.string.reciente_ayer, comida);
                break;
            case SEMANA:
                texto = ctx.getString(R.string.reciente_semana, comida,
                        new SimpleDateFormat("EEEE", idioma).format(dia.getTime()));
                break;
            default:
                texto = dia == null ? comida : ctx.getString(R.string.reciente_fecha, comida,
                        new SimpleDateFormat(ctx.getString(R.string.reciente_fecha_patron), idioma).format(dia.getTime()));
        }
        // La comida va en minúscula para que el inglés diga «Yesterday's snack»; el
        // principio de la frase, siempre en mayúscula.
        return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase(idioma) + texto.substring(1);
    }

    /** «Pan integral, Pechuga de pavo · 203 kcal». */
    @NonNull
    public static String detalle(@NonNull Context ctx, @NonNull ComidaReciente c) {
        NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(ctx));
        return ctx.getString(R.string.fila_cantidad_kcal, ComidasRecientes.nombres(c.getLineas()), nf.format(c.getKcal()));
    }

    /** «Sí, copiar la merienda de ayer». */
    @StringRes
    public static int siCopiar(@NonNull String tipo) {
        return SI_COPIAR[indice(tipo)];
    }

    /** «Lo que sueles merendar». */
    @StringRes
    public static int sueles(@NonNull String tipo) {
        return SUELES[indice(tipo)];
    }

    private static int indice(String tipo) {
        for (int i = 0; i < ComidaQueToca.TIPOS.length; i++) if (ComidaQueToca.TIPOS[i].equals(tipo)) return i;
        return 3;
    }
}
