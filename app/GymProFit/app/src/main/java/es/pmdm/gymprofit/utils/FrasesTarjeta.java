package es.pmdm.gymprofit.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.sesion.SeriesMusculo;

// ============================================================
// FrasesTarjeta — las líneas de las tarjetas de sesión y de rutina, en frase (GP-232).
//
// Dentro de estas tarjetas todo se dice como lo diría una persona, sin «·»: «Pecho, espalda y
// brazos», «58 minutos, 18 series y 1 récord», «7 ejercicios y 55 minutos». En inglés, con la
// coma de serie de EE. UU. («Chest, back, and arms»), que solo lleva una lista de tres o más.
// El resto de la app sigue con sus separadores: por eso no se toca Enumeracion.
//
// Sin dependencias de Android salvo Textos.de, para probarla en la JVM con las cadenas de
// values/ y values-en/.
// ============================================================
public final class FrasesTarjeta {

    /** Cuántas zonas se nombran como mucho. */
    public static final int MAX_ZONAS = 3;

    private FrasesTarjeta() { }

    /** Un texto con su número: «18 series», «1 récord». */
    public interface Contar { @NonNull String de(int n); }

    /** Los textos de la tarjeta, sacados de los recursos (o puestos a mano en un test). */
    public static final class Textos {
        final String y;
        final String ySerie;
        final Contar minutos;
        final Contar series;
        final Contar records;
        final Contar ejercicios;

        /**
         * @param y          « y » / « and »: entre dos.
         * @param ySerie     « y » / «, and »: antes del último de tres o más.
         */
        public Textos(@NonNull String y, @NonNull String ySerie, @NonNull Contar minutos, @NonNull Contar series,
                      @NonNull Contar records, @NonNull Contar ejercicios) {
            this.y = y;
            this.ySerie = ySerie;
            this.minutos = minutos;
            this.series = series;
            this.records = records;
            this.ejercicios = ejercicios;
        }

        /** Los de la app, en su idioma. */
        @NonNull
        public static Textos de(@NonNull Context ctx) {
            android.content.res.Resources r = ctx.getResources();
            return new Textos(ctx.getString(R.string.enumeracion_y), ctx.getString(R.string.tarjeta_y_serie),
                    n -> r.getQuantityString(R.plurals.tarjeta_minutos, n, n),
                    n -> r.getQuantityString(R.plurals.tarjeta_series, n, n),
                    n -> r.getQuantityString(R.plurals.tarjeta_records, n, n),
                    n -> r.getQuantityString(R.plurals.tarjeta_ejercicios, n, n));
        }
    }

    /** Una línea con la parte del récord marcada, para pintarla en dorado. */
    public static final class Linea {
        @NonNull public final String texto;
        /** Dónde empieza el récord en el texto, o -1 si no lo lleva. */
        public final int inicioRecord;

        Linea(@NonNull String texto, int inicioRecord) {
            this.texto = texto;
            this.inicioRecord = inicioRecord;
        }
    }

    /**
     * «a», «a y b», «a, b y c» («a, b, and c» en inglés).
     *
     * @param partes lo que se enumera; vacío da "".
     */
    @NonNull
    public static String lista(@NonNull List<String> partes, @NonNull Textos t) {
        if (partes.size() <= 2) return Enumeracion.unir(partes, t.y);
        return Enumeracion.unir(partes, t.ySerie);
    }

    /**
     * Las zonas de unas series por músculo, de más a menos series y como mucho {@link #MAX_ZONAS}.
     * Los músculos sin zona (cuello, cardio) no cuentan.
     */
    @NonNull
    public static List<Zonas.Zona> zonas(@NonNull List<SeriesMusculo> musculos) {
        List<ZonasDelDia.Ejercicio> lista = new ArrayList<>();
        for (SeriesMusculo m : musculos) lista.add(new ZonasDelDia.Ejercicio(m.getMusculo(), m.getSeries()));
        List<Zonas.Zona> todas = ZonasDelDia.de(lista).zonas;
        return todas.size() > MAX_ZONAS ? new ArrayList<>(todas.subList(0, MAX_ZONAS)) : todas;
    }

    /**
     * «Pecho, espalda y brazos»: los nombres de zona en frase (los de {@code Zonas.Zona.nombre},
     * en minúscula), con la primera letra en mayúscula.
     *
     * @return "" si no hay ninguna.
     */
    @NonNull
    public static String frase(@NonNull List<String> nombresDeZona, @NonNull Locale idioma, @NonNull Textos t) {
        return Enumeracion.alPrincipio(lista(nombresDeZona, t), idioma);
    }

    /**
     * Minutos, series y récords de una sesión: «58 minutos, 18 series y 1 récord». Los récords
     * solo si los hubo; sin ejercicios apuntados, solo los minutos (los kilos dicen el resto).
     *
     * @param minutos null si no se sabe.
     */
    @NonNull
    public static Linea meta(@Nullable Integer minutos, int series, int records, boolean sinEjercicios,
                             @NonNull Textos t) {
        List<String> partes = new ArrayList<>();
        if (minutos != null && minutos > 0) partes.add(t.minutos.de(minutos));
        if (!sinEjercicios) partes.add(t.series.de(series));
        String record = records > 0 ? t.records.de(records) : null;
        if (record != null) partes.add(record);
        String texto = lista(partes, t);
        return new Linea(texto, record == null ? -1 : texto.length() - record.length());
    }

    /** «7 ejercicios y 55 minutos», la pauta de una rutina. */
    @NonNull
    public static String pauta(int ejercicios, int minutos, @NonNull Textos t) {
        List<String> partes = new ArrayList<>();
        if (ejercicios > 0) partes.add(t.ejercicios.de(ejercicios));
        if (minutos > 0) partes.add(t.minutos.de(minutos));
        return lista(partes, t);
    }

    /**
     * Una frase seguida de los récords: «La última vez, hace 6 días, con 2 récords» o «Ayer,
     * 13.640 kg y 1 récord». El enlace (« con », « y ») lo da quien llama.
     */
    @NonNull
    public static Linea conRecords(@NonNull String antes, @NonNull String enlace, int records, @NonNull Textos t) {
        if (records <= 0) return new Linea(antes, -1);
        String record = t.records.de(records);
        String texto = antes + enlace + record;
        return new Linea(texto, texto.length() - record.length());
    }

    /**
     * Las rutinas de un ciclo en frase, para TalkBack: si todas comparten el nombre menos la última
     * palabra, se dice una vez («Cuerpo completo A, B y C»); si no, enteras («Torso A, Pierna A,
     * Torso B y Pierna B»).
     */
    @NonNull
    public static String ciclo(@NonNull List<String> nombres, @NonNull Textos t) {
        String comun = null;
        List<String> finales = new ArrayList<>();
        for (String n : nombres) {
            String limpio = n == null ? "" : n.trim();
            int corte = limpio.lastIndexOf(' ');
            if (corte <= 0) { comun = null; finales = null; break; }
            String antes = limpio.substring(0, corte);
            if (comun == null) comun = antes;
            else if (!comun.equals(antes)) { comun = null; finales = null; break; }
            finales.add(limpio.substring(corte + 1));
        }
        if (comun != null && finales != null && finales.size() > 1) return comun + " " + lista(finales, t);
        return lista(nombres, t);
    }

    /** La parte de un número en un texto de récords: para decirlo entero a TalkBack. */
    @NonNull
    public static String records(int n, @NonNull Textos t) {
        return t.records.de(n);
    }
}
