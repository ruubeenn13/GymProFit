package es.pmdm.gymprofit.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.programa.VistaPrevia;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;

// ============================================================
// TiempoRutinas — lo que dice la hoja «¿Cuánto tiempo tienes por sesión?» (GP-243).
//
// La API calcula la vista previa (VistaPrevia, reglas del catálogo): qué ejercicios se
// quedan, con sus series, y cuáles salen. Aquí solo se cuenta eso en frase:
//   · la frase de arriba, para todas las rutinas: «Con 30 minutos, cada rutina se queda en
//     sus 3 básicos.»;
//   · la línea de cada rutina: «3 de 7 ejercicios. Los básicos, a 2 series.» o, sin cambio,
//     «Los 7 ejercicios, enteros.»;
//   · sus bloques: uno por ejercicio, los que se quedan con sus series y en su orden, y
//     detrás los que salen.
// Las series de los básicos salen de la API (seriesBasicos) solo si cambian: si no, se
// cuentan en los básicos que se quedan, y si no son todas iguales no se inventa un número.
//
// Sin Android salvo Textos.de, para probarla en la JVM con las cadenas de values*/.
// ============================================================
public final class TiempoRutinas {

    private static final String BASICO = "BASICO";

    private TiempoRutinas() { }

    /** Los textos, sacados de los recursos (o de las cadenas de verdad en un test). */
    public interface Textos {
        /** «3 de 7 ejercicios.» */
        String quedan(int quedan, int total);
        /** «Los 7 ejercicios, enteros.» */
        String enteros(int total);
        /** «Los 7 ejercicios.» */
        String todos(int total);
        /** «Los básicos, a 2 series.» */
        String basicosA(int series);
        /** «Los básicos, con sus 3 series.» */
        String basicosCon(int series);
        /** «Los básicos, con todas sus series.» */
        String basicosConTodas();
        /** «30 minutos», «75 minutos o más». */
        String minutos(int minutos);
        /** «Con 60 minutos, tus rutinas se quedan enteras.» */
        String fraseIgual(String minutos);
        /** «Con 30 minutos, cada rutina se queda en sus 3 básicos.» */
        String fraseBasicos(String minutos, int basicos);
        /** «Con 30 minutos, cada rutina se queda en sus básicos.» */
        String fraseSusBasicos(String minutos);
        /** «Con 75 minutos o más, los básicos pasan a 4 series.» */
        String fraseSeries(String minutos, int series);
        /** «Con 45 minutos, salen 2 ejercicios de tus rutinas.» */
        String fraseSalen(String minutos, int salen);

        /** Los de la app, en su idioma. */
        @NonNull
        static Textos de(@NonNull Context c) {
            return new Textos() {
                @Override public String quedan(int q, int t) {
                    return c.getResources().getQuantityString(R.plurals.tiempo_linea_quedan, t, q, t);
                }
                @Override public String enteros(int t) {
                    return c.getResources().getQuantityString(R.plurals.tiempo_linea_enteros, t, t);
                }
                @Override public String todos(int t) {
                    return c.getResources().getQuantityString(R.plurals.tiempo_linea_todos, t, t);
                }
                @Override public String basicosA(int s) {
                    return c.getResources().getQuantityString(R.plurals.tiempo_linea_basicos_a, s, s);
                }
                @Override public String basicosCon(int s) {
                    return c.getResources().getQuantityString(R.plurals.tiempo_linea_basicos_con, s, s);
                }
                @Override public String basicosConTodas() { return c.getString(R.string.tiempo_linea_basicos_todas); }
                @Override public String minutos(int m) {
                    return m >= 75 ? c.getString(R.string.tiempo_minutos_75)
                            : c.getResources().getQuantityString(R.plurals.tiempo_minutos, m, m);
                }
                @Override public String fraseIgual(String m) { return c.getString(R.string.tiempo_frase_igual, m); }
                @Override public String fraseBasicos(String m, int b) {
                    return c.getResources().getQuantityString(R.plurals.tiempo_frase_basicos, b, m, b);
                }
                @Override public String fraseSusBasicos(String m) {
                    return c.getString(R.string.tiempo_frase_sus_basicos, m);
                }
                @Override public String fraseSeries(String m, int s) {
                    return c.getResources().getQuantityString(R.plurals.tiempo_frase_series, s, m, s);
                }
                @Override public String fraseSalen(String m, int n) {
                    return c.getResources().getQuantityString(R.plurals.tiempo_frase_salen, n, m, n);
                }
            };
        }
    }

    /** Un bloque de la tarjeta de una rutina: un ejercicio, con sus series si se queda. */
    public static final class Bloque {
        public final boolean queda;
        public final int series;

        Bloque(boolean queda, int series) {
            this.queda = queda;
            this.series = series;
        }
    }

    /** Los bloques de una rutina: los que se quedan, en su orden y con sus series; luego los que salen. */
    @NonNull
    public static List<Bloque> bloques(@NonNull VistaPrevia.Rutina r) {
        List<Bloque> b = new ArrayList<>();
        for (RutinaEjercicio e : quedan(r)) b.add(new Bloque(true, Math.max(0, e.getSeries())));
        for (int i = 0; i < fuera(r).size(); i++) b.add(new Bloque(false, 0));
        return b;
    }

    /** La línea de una rutina, bajo sus bloques. */
    @NonNull
    public static String linea(@NonNull Textos t, @NonNull VistaPrevia.Rutina r) {
        int quedan = quedan(r).size();
        int total = quedan + fuera(r).size();
        Integer cambian = r.getSeriesBasicos();
        if (fuera(r).isEmpty()) {
            return cambian == null ? t.enteros(total) : t.todos(total) + " " + t.basicosA(cambian);
        }
        String inicio = t.quedan(quedan, total);
        if (cambian != null) return inicio + " " + t.basicosA(cambian);
        Integer series = seriesDeLosBasicos(r);
        if (series == null) return basicos(r) == 0 ? inicio : inicio + " " + t.basicosConTodas();
        return inicio + " " + t.basicosCon(series);
    }

    /**
     * La frase de arriba, para todas las rutinas a la vez.
     *
     * @param minutos los elegidos: 30, 45, 60 o 75 (que se dice «75 minutos o más»).
     */
    @NonNull
    public static String frase(@NonNull Textos t, int minutos, @Nullable List<VistaPrevia.Rutina> rutinas) {
        String m = t.minutos(minutos);
        List<VistaPrevia.Rutina> rs = rutinas != null ? rutinas : Collections.emptyList();
        int salen = 0;
        boolean cambianSeries = false;
        boolean cadaUnaEnSusBasicos = !rs.isEmpty();
        Integer basicosIguales = null;
        Integer seriesIguales = null;
        boolean seriesDistintas = false;
        for (int i = 0; i < rs.size(); i++) {
            VistaPrevia.Rutina r = rs.get(i);
            salen += fuera(r).size();
            if (r.getSeriesBasicos() != null) {
                cambianSeries = true;
                if (seriesIguales == null) seriesIguales = r.getSeriesBasicos();
                else if (!seriesIguales.equals(r.getSeriesBasicos())) seriesDistintas = true;
            }
            int b = basicos(r);
            // Se queda en sus básicos: salió algo y lo que queda son todos básicos.
            if (fuera(r).isEmpty() || b == 0 || b != quedan(r).size()) cadaUnaEnSusBasicos = false;
            if (i == 0) basicosIguales = b;
            else if (basicosIguales != null && basicosIguales != b) basicosIguales = null;
        }
        if (cadaUnaEnSusBasicos) {
            return basicosIguales != null ? t.fraseBasicos(m, basicosIguales) : t.fraseSusBasicos(m);
        }
        if (salen > 0) return t.fraseSalen(m, salen);
        if (cambianSeries && !seriesDistintas) return t.fraseSeries(m, seriesIguales);
        return t.fraseIgual(m);
    }

    // Lo que se queda y lo que sale, sin nulos.
    private static List<RutinaEjercicio> quedan(VistaPrevia.Rutina r) {
        return r.getEjercicios() != null ? r.getEjercicios() : Collections.emptyList();
    }

    private static List<String> fuera(VistaPrevia.Rutina r) {
        return r.getQuitados() != null ? r.getQuitados() : Collections.emptyList();
    }

    private static int basicos(VistaPrevia.Rutina r) {
        int n = 0;
        for (RutinaEjercicio e : quedan(r)) if (BASICO.equals(e.getTipo())) n++;
        return n;
    }

    // Las series de los básicos que se quedan, si son todas iguales; si no, o sin básicos, null.
    @Nullable
    private static Integer seriesDeLosBasicos(VistaPrevia.Rutina r) {
        Integer s = null;
        for (RutinaEjercicio e : quedan(r)) {
            if (!BASICO.equals(e.getTipo())) continue;
            if (s == null) s = e.getSeries();
            else if (s != e.getSeries()) return null;
        }
        return s;
    }
}
