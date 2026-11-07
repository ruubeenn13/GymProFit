package es.pmdm.gymprofit.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.sesion.VezRutina;

// ============================================================
// CifraTarjeta — la cifra grande de una tarjeta de sesión o de rutina (GP-232).
//
// Con los programas de peso corporal, una sesión a 0 kilos no es un caso raro: es un tipo de
// usuario entero. Ninguna tarjeta enseña «0 kg» ni «0 series». En cascada:
//   · con kilos, los kilos, con sus barras y la comparación con la anterior;
//   · sin kilos y con series, «3 series» en la misma condensada, sin barras ni comparación;
//   · sin series, el aviso en el texto secundario: «Sin ejercicios apuntados» si no hay
//     ejercicios y «Sin series hechas» si los hay sin ninguna serie.
// De una vez de una rutina no se sabe si tenía ejercicios: sin series, «Sin series hechas»,
// que vale en los dos casos.
// También la cabecera de una semana: sin kilos, solo «3 entrenos».
//
// Una sola decisión para Historial, Inicio, Progreso, Entrenar y «¿Qué rutina hoy?». Sin
// dependencias de Android salvo Textos.de, para probarla en la JVM con las cadenas de verdad.
// ============================================================
public final class CifraTarjeta {

    /** Qué se enseña en el sitio de los kilos. */
    public enum Tipo { KILOS, SERIES, SIN_EJERCICIOS, SIN_SERIES }

    /** Un texto con su número y la cifra ya escrita: «13.640 kilos», «2 entrenos y 22.050 kg». */
    public interface ContarCon { @NonNull String de(int n, @NonNull String cifra); }

    /** Los textos, sacados de los recursos (o puestos a mano en un test). */
    public static final class Textos {
        final Locale idioma;
        final FrasesTarjeta.Contar series;
        final ContarCon kilosA11y;
        final String sinEjercicios;
        final String sinSeries;
        final ContarCon semana;
        final ContarCon semanaA11y;
        final FrasesTarjeta.Contar semanaSinKilos;

        public Textos(@NonNull Locale idioma, @NonNull FrasesTarjeta.Contar series, @NonNull ContarCon kilosA11y,
                      @NonNull String sinEjercicios, @NonNull String sinSeries, @NonNull ContarCon semana,
                      @NonNull ContarCon semanaA11y, @NonNull FrasesTarjeta.Contar semanaSinKilos) {
            this.idioma = idioma;
            this.series = series;
            this.kilosA11y = kilosA11y;
            this.sinEjercicios = sinEjercicios;
            this.sinSeries = sinSeries;
            this.semana = semana;
            this.semanaA11y = semanaA11y;
            this.semanaSinKilos = semanaSinKilos;
        }

        /** Los de la app, en su idioma. */
        @NonNull
        public static Textos de(@NonNull Context ctx, @NonNull Locale idioma) {
            android.content.res.Resources r = ctx.getResources();
            return new Textos(idioma,
                    n -> r.getQuantityString(R.plurals.tarjeta_series, n, n),
                    (n, cifra) -> r.getQuantityString(R.plurals.tarjeta_kilos_a11y, n, cifra),
                    ctx.getString(R.string.tarjeta_sin_ejercicios), ctx.getString(R.string.tarjeta_sin_series),
                    (n, cifra) -> r.getQuantityString(R.plurals.historial_semana_resumen, n, n, cifra),
                    (n, cifra) -> r.getQuantityString(R.plurals.historial_semana_a11y, n, n, cifra),
                    n -> r.getQuantityString(R.plurals.historial_semana_sin_kilos, n, n));
        }
    }

    @NonNull public final Tipo tipo;
    /** Los kilos, redondeados; 0 salvo en {@link Tipo#KILOS}. */
    public final long kilos;
    public final int series;

    private CifraTarjeta(@NonNull Tipo tipo, long kilos, int series) {
        this.tipo = tipo;
        this.kilos = kilos;
        this.series = series;
    }

    /** La de una sesión: sus kilos, sus series hechas y cuántos ejercicios apuntó. */
    @NonNull
    public static CifraTarjeta deSesion(double kilos, int series, int ejercicios) {
        long k = Math.round(Math.max(0, kilos));
        if (k > 0) return new CifraTarjeta(Tipo.KILOS, k, series);
        if (series > 0) return new CifraTarjeta(Tipo.SERIES, 0, series);
        return new CifraTarjeta(ejercicios == 0 ? Tipo.SIN_EJERCICIOS : Tipo.SIN_SERIES, 0, 0);
    }

    /** La de una vez de una rutina (su última vez, en Entrenar y en la hoja). */
    @NonNull
    public static CifraTarjeta deVez(@NonNull VezRutina v) {
        // Sin saber sus ejercicios: sin series, «Sin series hechas» vale con y sin ellos.
        return deSesion(v.getKilos(), v.getSeries(), -1);
    }

    /** Solo con kilos hay barras y comparación con la anterior. */
    public boolean conKilos() {
        return tipo == Tipo.KILOS;
    }

    /** Los avisos van en el texto secundario; las cifras, en la condensada. */
    public boolean esAviso() {
        return tipo == Tipo.SIN_EJERCICIOS || tipo == Tipo.SIN_SERIES;
    }

    /** «13.640», en el idioma de la app. */
    @NonNull
    public static String numero(long n, @NonNull Locale idioma) {
        return NumberFormat.getIntegerInstance(idioma).format(n);
    }

    /**
     * Lo que se escribe en el sitio de los kilos salvo los kilos, que llevan su «kg» pequeño y los
     * pinta quien llama: «3 series», «Sin ejercicios apuntados» o «Sin series hechas».
     *
     * @return null con {@link Tipo#KILOS}.
     */
    @Nullable
    public String texto(@NonNull Textos t) {
        switch (tipo) {
            case SERIES: return t.series.de(series);
            case SIN_EJERCICIOS: return t.sinEjercicios;
            case SIN_SERIES: return t.sinSeries;
            default: return null;
        }
    }

    /**
     * Para dentro de una frase («Hoy, sin series hechas», «La última vez, …, 3 series»): el aviso en
     * minúscula; las cifras, igual.
     */
    @NonNull
    public String enFrase(@NonNull Textos t) {
        String s = a11y(t);
        return esAviso() && !s.isEmpty() ? s.substring(0, 1).toLowerCase(t.idioma) + s.substring(1) : s;
    }

    /** Para TalkBack: «13.640 kilos», «3 series», «Sin ejercicios apuntados»; nunca «0 kilos». */
    @NonNull
    public String a11y(@NonNull Textos t) {
        if (tipo == Tipo.KILOS) {
            return t.kilosA11y.de((int) Math.min(Integer.MAX_VALUE, kilos), numero(kilos, t.idioma));
        }
        String s = texto(t);
        return s == null ? "" : s;
    }

    /** La cabecera de una semana: «2 entrenos y 22.050 kg», o «3 entrenos» si no hubo kilos. */
    @NonNull
    public static String semana(int entrenos, double kilos, @NonNull Textos t) {
        long k = Math.round(Math.max(0, kilos));
        return k > 0 ? t.semana.de(entrenos, numero(k, t.idioma)) : t.semanaSinKilos.de(entrenos);
    }

    /** La misma, para TalkBack: «2 entrenos y 22.050 kilos», o «3 entrenos». */
    @NonNull
    public static String semanaA11y(int entrenos, double kilos, @NonNull Textos t) {
        long k = Math.round(Math.max(0, kilos));
        return k > 0 ? t.semanaA11y.de(entrenos, numero(k, t.idioma)) : t.semanaSinKilos.de(entrenos);
    }
}
