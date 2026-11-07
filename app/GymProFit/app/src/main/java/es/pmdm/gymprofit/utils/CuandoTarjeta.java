package es.pmdm.gymprofit.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import es.pmdm.gymprofit.R;

// ============================================================
// CuandoTarjeta — cuándo fue una sesión, como lo dicen sus tarjetas (GP-232).
//
// En el Historial completo la semana la dice su cabecera, así que la tarjeta da el día y la
// hora: «Hoy, 18:05», «Ayer, 18:05» y «Martes 3, 18:05». En Inicio y Progreso, el día relativo
// de siempre (FechaUtils.diaRelativo) con coma en vez de «·»: «Ayer, martes». La cabecera de la
// semana: «ESTA SEMANA», «SEMANA PASADA» y «SEMANA DEL 19 OCT». Para TalkBack, en frase: «ayer a
// las 18:05», «el lunes 2 a las 19:10».
//
// Sin dependencias de Android salvo Textos.de, para probarla en la JVM.
// ============================================================
public final class CuandoTarjeta {

    private CuandoTarjeta() { }

    /** Los textos, sacados de los recursos (o puestos a mano en un test). */
    public static final class Textos {
        final String hoy, ayer, cuando, patronDia, patronHora, a11yHoy, a11yAyer, a11yOtro;
        final String estaSemana, semanaPasada, semanaDel, patronSemana, patronSemanaA11y;
        final FechaUtils.TextosDia dia;

        /**
         * @param cuando       «%1$s, %2$s»: el día y la hora.
         * @param patronDia    «EEEE d».
         * @param patronHora   «HH:mm» / «h:mm a».
         * @param a11yHoy      «hoy a las %1$s».
         * @param a11yAyer     «ayer a las %1$s».
         * @param a11yOtro     «el %1$s a las %2$s».
         * @param semanaDel    «Semana del %1$s».
         * @param patronSemana «d MMM» / «MMM d».
         * @param dia          los del día relativo, con la coma por separador.
         * @param patronSemanaA11y «d 'de' MMMM» / «MMMM d»: el mes entero, para TalkBack.
         */
        public Textos(@NonNull String hoy, @NonNull String ayer, @NonNull String cuando, @NonNull String patronDia,
                      @NonNull String patronHora, @NonNull String a11yHoy, @NonNull String a11yAyer,
                      @NonNull String a11yOtro, @NonNull String estaSemana, @NonNull String semanaPasada,
                      @NonNull String semanaDel, @NonNull String patronSemana, @NonNull FechaUtils.TextosDia dia,
                      @NonNull String patronSemanaA11y) {
            this.hoy = hoy;
            this.ayer = ayer;
            this.cuando = cuando;
            this.patronDia = patronDia;
            this.patronHora = patronHora;
            this.a11yHoy = a11yHoy;
            this.a11yAyer = a11yAyer;
            this.a11yOtro = a11yOtro;
            this.estaSemana = estaSemana;
            this.semanaPasada = semanaPasada;
            this.semanaDel = semanaDel;
            this.patronSemana = patronSemana;
            this.patronSemanaA11y = patronSemanaA11y;
            this.dia = dia;
        }

        /** Los de la app, en su idioma. */
        @NonNull
        public static Textos de(@NonNull Context ctx) {
            return new Textos(ctx.getString(R.string.tiempo_hoy_mayuscula), ctx.getString(R.string.tiempo_ayer_mayuscula),
                    ctx.getString(R.string.tarjeta_cuando), ctx.getString(R.string.tarjeta_dia_patron),
                    ctx.getString(R.string.tarjeta_hora_patron), ctx.getString(R.string.tarjeta_cuando_hoy_a11y),
                    ctx.getString(R.string.tarjeta_cuando_ayer_a11y), ctx.getString(R.string.tarjeta_cuando_otro_a11y),
                    ctx.getString(R.string.historial_esta_semana), ctx.getString(R.string.historial_semana_pasada),
                    ctx.getString(R.string.historial_semana_del), ctx.getString(R.string.inicio_fecha_corta_patron),
                    new FechaUtils.TextosDia(ctx.getString(R.string.tiempo_hoy_mayuscula),
                            ctx.getString(R.string.tiempo_ayer_mayuscula), ctx.getString(R.string.tarjeta_cuando),
                            ctx.getString(R.string.inicio_dia_pasado), ctx.getString(R.string.inicio_fecha_corta_patron)),
                    ctx.getString(R.string.historial_semana_patron_a11y));
        }
    }

    /** «Hoy, 18:05», «Ayer, 18:05» o «Martes 3, 18:05»; "" si la fecha no se entiende. */
    @NonNull
    public static String historial(@Nullable String iso, @NonNull String hoy, @NonNull Locale idioma, @NonNull Textos t) {
        Date fecha = leer(iso);
        if (fecha == null) return "";
        String hora = new SimpleDateFormat(t.patronHora, idioma).format(fecha);
        int dias = TiempoRelativo.diasEntre(iso, hoy);
        String dia = dias == 0 ? t.hoy : dias == 1 ? t.ayer
                : Enumeracion.alPrincipio(new SimpleDateFormat(t.patronDia, idioma).format(fecha), idioma);
        return String.format(idioma, t.cuando, dia, hora);
    }

    /** «ayer a las 18:05», «el lunes 2 a las 19:10»: para TalkBack. */
    @NonNull
    public static String a11y(@Nullable String iso, @NonNull String hoy, @NonNull Locale idioma, @NonNull Textos t) {
        Date fecha = leer(iso);
        if (fecha == null) return "";
        String hora = new SimpleDateFormat(t.patronHora, idioma).format(fecha);
        int dias = TiempoRelativo.diasEntre(iso, hoy);
        if (dias == 0) return String.format(idioma, t.a11yHoy, hora);
        if (dias == 1) return String.format(idioma, t.a11yAyer, hora);
        String dia = new SimpleDateFormat(t.patronDia, idioma).format(fecha);
        // En español el día va en minúscula dentro de la frase; en inglés, siempre en mayúscula.
        if (!"es".equals(idioma.getLanguage())) dia = Enumeracion.alPrincipio(dia, idioma);
        return String.format(idioma, t.a11yOtro, dia, hora);
    }

    /** «Ayer, martes», «Lunes», «Martes pasado» o «12 oct»: el día relativo, sin «·». */
    @NonNull
    public static String relativo(@Nullable String iso, @NonNull String hoy, @NonNull Locale idioma, @NonNull Textos t) {
        return FechaUtils.diaRelativo(iso, hoy, idioma, t.dia);
    }

    /** «ESTA SEMANA», «SEMANA PASADA» o «SEMANA DEL 19 OCT», por el lunes de la semana. */
    @NonNull
    public static String semana(@Nullable String lunes, @NonNull String hoy, @NonNull Locale idioma, @NonNull Textos t) {
        return semana(lunes, hoy, idioma, t, t.patronSemana).toUpperCase(idioma);
    }

    /**
     * La misma cabecera en frase, para TalkBack (en mayúsculas deletrea): «Esta semana», «Semana
     * del 19 de octubre», «Week of October 19», con el mes entero (abreviado, el sintetizador lo
     * lee a medias).
     */
    @NonNull
    public static String semanaEnFrase(@Nullable String lunes, @NonNull String hoy, @NonNull Locale idioma,
                                       @NonNull Textos t) {
        return semana(lunes, hoy, idioma, t, t.patronSemanaA11y);
    }

    private static String semana(@Nullable String lunes, @NonNull String hoy, @NonNull Locale idioma,
                                 @NonNull Textos t, @NonNull String patron) {
        int dias = TiempoRelativo.diasEntre(lunes, hoy);
        if (dias >= 0 && dias < 7) return t.estaSemana;
        if (dias >= 7 && dias < 14) return t.semanaPasada;
        String fecha = FechaUtils.fechaCorta(lunes, idioma, patron);
        return String.format(idioma, t.semanaDel, fecha == null ? "" : fecha);
    }

    // «2026-11-03T18:05:00» (o con fracciones); null si no se entiende.
    @Nullable
    private static Date leer(@Nullable String iso) {
        if (iso == null || iso.length() < 16) return null;
        try {
            SimpleDateFormat entrada = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ROOT);
            entrada.setLenient(false);
            return entrada.parse(iso.substring(0, 16));
        } catch (ParseException e) {
            return null;
        }
    }
}
