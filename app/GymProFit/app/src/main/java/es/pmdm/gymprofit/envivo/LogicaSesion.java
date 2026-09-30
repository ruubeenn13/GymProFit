package es.pmdm.gymprofit.envivo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.envivo.UltimaVez;
import es.pmdm.gymprofit.utils.Numeros;

// ============================================================
// LogicaSesion — las reglas de la sesión en vivo (GP-012 y GP-014), sin Android.
//
// Todo lo que decide algo está aquí para probarlo en la JVM: el reloj y la duración,
// qué dice «Anterior», qué pista lleva cada campo, qué pasa al marcar una serie y qué
// se manda al guardar. La pantalla solo pinta lo que sale de aquí, con los textos de
// strings.xml.
//
// Marcar es confirmar (GP-014): una serie marcada con un campo vacío se queda con lo de
// su pista, y se escribe en el campo para que lo que se ve sea lo que se guarda. Sin
// última vez, la pista de las repeticiones es el rango de la pauta y lo que se confirma
// es su mínimo: el rango no es un número, y el mínimo es lo único de él que no
// presume de más.
// ============================================================
public final class LogicaSesion {

    private LogicaSesion() { }

    /** Duración máxima que admite la hoja de terminar, en minutos. */
    public static final int MAX_MINUTOS = 600;

    /** Segundos como máximo de una serie por tiempo (lo que admite la API). */
    public static final int MAX_SEGUNDOS = 3600;

    // ── Reloj ────────────────────────────────────────────────

    /**
     * Minutos del reloj, como los propone la hoja de terminar: los enteros transcurridos,
     * al menos 1 y como mucho {@link #MAX_MINUTOS}.
     */
    public static int minutosReloj(long inicioMs, long ahoraMs) {
        long min = Math.max(0, ahoraMs - inicioMs) / 60_000L;
        return (int) Math.max(1, Math.min(MAX_MINUTOS, min));
    }

    /** El reloj en pantalla: «23:14», o «1:02:03» pasada la hora. Igual en ES y EN. */
    @NonNull
    public static String reloj(long inicioMs, long ahoraMs) {
        long s = Math.max(0, ahoraMs - inicioMs) / 1000L;
        long h = s / 3600, m = (s % 3600) / 60, seg = s % 60;
        return h > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, seg)
                : String.format(Locale.ROOT, "%d:%02d", m, seg);
    }

    /** Segundos en «0:40», «1:05». */
    @NonNull
    public static String tiempo(int segundos) {
        int s = Math.max(0, segundos);
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    /**
     * Lo que se teclea en una serie por tiempo: «40» son segundos y «0:40» o «1:05»,
     * minutos y segundos. Null si no es un tiempo válido de 1 a {@link #MAX_SEGUNDOS}.
     */
    @Nullable
    public static Integer segundosDe(@Nullable String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        if (t.isEmpty()) return null;
        int dos = t.indexOf(':');
        if (dos < 0) return Numeros.entero(t, 1, MAX_SEGUNDOS);
        Integer min = Numeros.entero(t.substring(0, dos), 0, 60);
        Integer seg = Numeros.entero(t.substring(dos + 1), 0, 59);
        if (min == null || seg == null || t.substring(dos + 1).trim().length() != 2) return null;
        int total = min * 60 + seg;
        return total >= 1 && total <= MAX_SEGUNDOS ? total : null;
    }

    // ── Totales ──────────────────────────────────────────────

    /** Series hechas y totales, y ejercicios con alguna hecha. */
    public static final class Totales {
        public final int seriesHechas, seriesTotales, ejerciciosHechos, ejerciciosTotales;

        Totales(int seriesHechas, int seriesTotales, int ejerciciosHechos, int ejerciciosTotales) {
            this.seriesHechas = seriesHechas;
            this.seriesTotales = seriesTotales;
            this.ejerciciosHechos = ejerciciosHechos;
            this.ejerciciosTotales = ejerciciosTotales;
        }

        /** Series sin marcar: las que no se guardarán. */
        public int sinMarcar() { return seriesTotales - seriesHechas; }
    }

    @NonNull
    public static Totales totales(@NonNull SesionEnCurso s) {
        int hechas = 0, total = 0, ejHechos = 0;
        for (SesionEnCurso.Ejercicio e : s.ejercicios) {
            int deEste = 0;
            for (SesionEnCurso.Serie serie : e.series) {
                total++;
                if (serie.hecha) deEste++;
            }
            hechas += deEste;
            if (deEste > 0) ejHechos++;
        }
        return new Totales(hechas, total, ejHechos, s.ejercicios.size());
    }

    // ── Por tiempo y pauta ───────────────────────────────────

    /**
     * Si las series del ejercicio se miden en segundos. Lo dice la pauta; un ejercicio
     * añadido durante la sesión no la tiene, y entonces lo dice la última vez.
     */
    public static boolean porTiempo(@NonNull SesionEnCurso.Ejercicio e) {
        if (e.medida != null) return "SEGUNDOS".equals(e.medida);
        for (SesionEnCurso.SerieAnterior a : e.anterior) {
            if (a.segundos != null && a.segundos > 0) return true;
        }
        return false;
    }

    /** Mínimo de la pauta (el rango, o el número de siempre), o null sin pauta. */
    @Nullable
    public static Integer minimoPauta(@NonNull SesionEnCurso.Ejercicio e) {
        if (e.minimo != null) return e.minimo;
        if (e.maximo != null) return e.maximo;
        return e.repeticionesPauta > 0 ? e.repeticionesPauta : null;
    }

    /** Máximo de la pauta, o null sin pauta. */
    @Nullable
    public static Integer maximoPauta(@NonNull SesionEnCurso.Ejercicio e) {
        if (e.maximo != null) return e.maximo;
        if (e.minimo != null) return e.minimo;
        return e.repeticionesPauta > 0 ? e.repeticionesPauta : null;
    }

    // ── La última vez (GP-014) ───────────────────────────────

    /** La serie del mismo número de la última vez, o null si no la hubo. */
    @Nullable
    public static SesionEnCurso.SerieAnterior anterior(@NonNull SesionEnCurso.Ejercicio e, int numero) {
        for (SesionEnCurso.SerieAnterior a : e.anterior) {
            if (a.numero == numero) return a;
        }
        return null;
    }

    /** Si el ejercicio se ha preguntado y no tiene ninguna vez: «Primera vez». */
    public static boolean primeraVez(@NonNull SesionEnCurso.Ejercicio e) {
        return e.anteriorCargado && e.anterior.isEmpty();
    }

    /**
     * Pone lo de /sesiones/ultima-vez en los ejercicios pedidos. Los que no vienen en
     * la respuesta no tienen ninguna vez; se marcan igual como preguntados.
     */
    public static void aplicarUltimaVez(@NonNull SesionEnCurso s, @NonNull List<Integer> pedidos,
                                        @Nullable List<UltimaVez> respuesta) {
        Map<Integer, UltimaVez> porId = new HashMap<>();
        if (respuesta != null) for (UltimaVez u : respuesta) porId.put(u.ejercicioId, u);
        for (SesionEnCurso.Ejercicio e : s.ejercicios) {
            if (!pedidos.contains(e.ejercicioId)) continue;
            UltimaVez u = porId.get(e.ejercicioId);
            e.anterior = new ArrayList<>();
            e.anteriorFecha = null;
            if (u != null && u.series != null) {
                e.anteriorFecha = u.fecha;
                for (UltimaVez.Serie serie : u.series) {
                    e.anterior.add(new SesionEnCurso.SerieAnterior(serie.numero,
                            serie.peso != null ? serie.peso.toPlainString() : null,
                            serie.repeticiones != null ? serie.repeticiones : 0, serie.segundos));
                }
            }
            e.anteriorCargado = true;
        }
    }

    /** Ids de los ejercicios aún sin preguntar, sin repetir, en su orden. */
    @NonNull
    public static List<Integer> sinAnterior(@NonNull SesionEnCurso s) {
        List<Integer> ids = new ArrayList<>();
        for (SesionEnCurso.Ejercicio e : s.ejercicios) {
            if (!e.anteriorCargado && !ids.contains(e.ejercicioId)) ids.add(e.ejercicioId);
        }
        return ids;
    }

    /** Kilos para la vista: «57,5», «60», con el separador del idioma y sin ceros de más. */
    @NonNull
    public static String kilos(@NonNull BigDecimal kilos, @NonNull Locale locale) {
        DecimalFormat f = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(locale));
        return f.format(kilos);
    }

    @Nullable
    private static BigDecimal pesoDe(@Nullable String exacto) {
        if (exacto == null) return null;
        try {
            BigDecimal b = new BigDecimal(exacto);
            return b.signum() > 0 ? b : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Textos que la vista pasa desde strings.xml, para que aquí no haya ninguno. */
    public interface Textos {
        /** «57,5 × 12». */
        String pesoPorReps(String kilos, int reps);
        /** «12 reps», con su plural. */
        String reps(int reps);
        /** «8–12». */
        String rango(int min, int max);
        /** «30–60 s» o «45 s». */
        String segundos(String numero);
        /** «—»: no la hubo. */
        String nada();
    }

    /** Lo que dice «Anterior» en la fila de la serie {@code numero}. */
    @NonNull
    public static String textoAnterior(@NonNull SesionEnCurso.Ejercicio e, int numero,
                                       @NonNull Textos t, @NonNull Locale locale) {
        SesionEnCurso.SerieAnterior a = anterior(e, numero);
        if (a == null) return t.nada();
        if (a.segundos != null && a.segundos > 0) return tiempo(a.segundos);
        BigDecimal peso = pesoDe(a.peso);
        return peso != null ? t.pesoPorReps(kilos(peso, locale), a.repeticiones) : t.reps(a.repeticiones);
    }

    /** Pista del campo de kilos: los de la última vez, o nada. */
    @NonNull
    public static String pistaPeso(@NonNull SesionEnCurso.Ejercicio e, int numero, @NonNull Locale locale) {
        SesionEnCurso.SerieAnterior a = anterior(e, numero);
        BigDecimal peso = a != null ? pesoDe(a.peso) : null;
        return peso != null ? kilos(peso, locale) : "";
    }

    /** Pista de las repeticiones: las de la última vez; sin ella, el rango de la pauta. */
    @NonNull
    public static String pistaReps(@NonNull SesionEnCurso.Ejercicio e, int numero, @NonNull Textos t) {
        SesionEnCurso.SerieAnterior a = anterior(e, numero);
        if (a != null && a.repeticiones > 0) return String.valueOf(a.repeticiones);
        Integer min = minimoPauta(e), max = maximoPauta(e);
        if (min == null) return "";
        return max != null && !max.equals(min) ? t.rango(min, max) : String.valueOf(min);
    }

    /** Pista de los segundos: los de la última vez («0:40»); sin ella, la pauta («30–60 s»). */
    @NonNull
    public static String pistaSegundos(@NonNull SesionEnCurso.Ejercicio e, int numero, @NonNull Textos t) {
        SesionEnCurso.SerieAnterior a = anterior(e, numero);
        if (a != null && a.segundos != null && a.segundos > 0) return tiempo(a.segundos);
        Integer min = minimoPauta(e), max = maximoPauta(e);
        if (min == null) return "";
        return t.segundos(max != null && !max.equals(min) ? t.rango(min, max) : String.valueOf(min));
    }

    // ── Marcar ───────────────────────────────────────────────

    /** Qué pasó al tocar el botón de marcar. */
    public enum Marcado {
        /** La serie queda hecha. */
        HECHA,
        /** La serie deja de estar hecha. */
        DESMARCADA,
        /** Sin repeticiones ni pista de dónde sacarlas: hay que escribirlas. */
        FALTAN_REPS,
        /** Sin segundos ni pista: hay que escribirlos o usar el cronómetro. */
        FALTAN_SEGUNDOS,
        /** Lo escrito no es un número válido. */
        NO_VALIDO
    }

    /**
     * Marca o desmarca la serie. Al marcar, cada campo vacío se rellena con lo que
     * confirma su pista: la última vez, o el mínimo de la pauta en las repeticiones.
     * Lo que no se puede confirmar ni leer deja la serie sin marcar.
     */
    @NonNull
    public static Marcado marcar(@NonNull SesionEnCurso.Ejercicio e, int indice, @NonNull Locale locale) {
        SesionEnCurso.Serie s = e.series.get(indice);
        if (s.hecha) {
            s.hecha = false;
            return Marcado.DESMARCADA;
        }
        int numero = indice + 1;
        SesionEnCurso.SerieAnterior a = anterior(e, numero);
        if (porTiempo(e)) {
            String seg = s.segundos.trim();
            if (seg.isEmpty()) {
                Integer confirmado = a != null && a.segundos != null && a.segundos > 0 ? a.segundos : minimoPauta(e);
                if (confirmado == null) return Marcado.FALTAN_SEGUNDOS;
                s.segundos = tiempo(confirmado);
            } else if (segundosDe(seg) == null) {
                return Marcado.NO_VALIDO;
            }
            s.hecha = true;
            return Marcado.HECHA;
        }

        String reps = s.repeticiones.trim();
        String peso = s.peso.trim();
        String repsFinal = reps;
        if (reps.isEmpty()) {
            Integer confirmado = a != null && a.repeticiones > 0 ? Integer.valueOf(a.repeticiones) : minimoPauta(e);
            if (confirmado == null) return Marcado.FALTAN_REPS;
            repsFinal = String.valueOf(confirmado);
        } else if (Numeros.entero(reps, 1, 100) == null) {
            return Marcado.NO_VALIDO;
        }
        String pesoFinal = peso;
        if (peso.isEmpty()) {
            BigDecimal anteriorPeso = a != null ? pesoDe(a.peso) : null;
            pesoFinal = anteriorPeso != null ? kilos(anteriorPeso, locale) : "";
        } else if (Numeros.exacto(peso, 0, 500) == null) {
            return Marcado.NO_VALIDO;
        }
        s.repeticiones = repsFinal;
        s.peso = pesoFinal;
        s.hecha = true;
        return Marcado.HECHA;
    }

    // ── Guardar ──────────────────────────────────────────────

    /** «yyyy-MM-ddTHH:mm:ss» en la zona del móvil, como la espera la API. */
    @NonNull
    public static String fechaApi(long ms, @NonNull TimeZone zona) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        f.setTimeZone(zona);
        return f.format(new Date(ms));
    }

    /**
     * El cuerpo de POST /sesiones/completa: fechaInicio, la del reloj; la duración que se
     * le pase (la de la hoja); solo los ejercicios con alguna serie marcada y, de cada
     * uno, las marcadas, renumeradas desde 1.
     */
    @NonNull
    public static Map<String, Object> cuerpo(@NonNull SesionEnCurso s, @NonNull String clave, int minutos,
                                             @NonNull TimeZone zona) {
        Map<String, Object> body = new HashMap<>();
        body.put("claveIdempotencia", clave);
        if (s.rutinaId != null) body.put("rutinaId", s.rutinaId);
        body.put("fechaInicio", fechaApi(s.inicioMs, zona));
        body.put("duracionMinutos", minutos);
        body.put("completada", true);
        if (s.valoracion != null && s.valoracion >= 1 && s.valoracion <= 5) body.put("valoracion", s.valoracion);
        String notas = s.notas != null ? s.notas.trim() : "";
        if (!notas.isEmpty()) body.put("notas", notas);

        List<Map<String, Object>> ejercicios = new ArrayList<>();
        for (SesionEnCurso.Ejercicio e : s.ejercicios) {
            List<Map<String, Object>> series = seriesDelCuerpo(e);
            if (series.isEmpty()) continue;
            Map<String, Object> ej = new HashMap<>();
            ej.put("ejercicioId", e.ejercicioId);
            ej.put("series", series);
            ejercicios.add(ej);
        }
        body.put("ejercicios", ejercicios);
        return body;
    }

    // Las marcadas, renumeradas. Una marcada que no se puede leer (no debería: marcar ya
    // lo comprueba) se descarta antes que inventarle un valor.
    private static List<Map<String, Object>> seriesDelCuerpo(SesionEnCurso.Ejercicio e) {
        List<Map<String, Object>> series = new ArrayList<>();
        boolean tiempo = porTiempo(e);
        int numero = 0;
        for (SesionEnCurso.Serie s : e.series) {
            if (!s.hecha) continue;
            Map<String, Object> fila = new HashMap<>();
            if (tiempo) {
                Integer seg = segundosDe(s.segundos);
                if (seg == null) continue;
                fila.put("repeticiones", 0);
                fila.put("segundos", seg);
            } else {
                Integer reps = Numeros.entero(s.repeticiones, 1, 100);
                if (reps == null) continue;
                fila.put("repeticiones", reps);
                BigDecimal peso = Numeros.exacto(s.peso, 0, 500);
                if (peso != null && peso.signum() > 0) fila.put("peso", peso);
            }
            fila.put("numero", ++numero);
            fila.put("completada", true);
            series.add(fila);
        }
        return series;
    }
}
