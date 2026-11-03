package es.pmdm.gymprofit.utils;

// ============================================================
// FechaUtils — utilidades de formato de fechas para la capa de vista.
// Con la migración a Retrofit+Gson (etapa 2) los POJOs guardan la fecha tal cual
// la envía la API (ISO-8601, p. ej. "2026-07-03T11:04:02.110442"), en lugar de
// una cadena ya formateada. El formateo para MOSTRAR pasa aquí (antes lo hacía
// UtilJSONParser.parseFecha al deserializar).
//
// Toda fecha que se enseña sale de aquí y en el idioma de la app (DEC-020, GP-228): nada de
// «dd/MM» escrito a mano ni de Locale.getDefault(). FechasLocalizadasTest lo vigila.
// La regla de los días relativos («Hoy», «Ayer · martes», «Lunes», «Jueves pasado», «12 oct»)
// es una sola para toda la app: diaRelativo.
// ============================================================
public final class FechaUtils {

    private FechaUtils() {}

    /**
     * Idioma de la interfaz, el que eligió el usuario en la app.
     *
     * <p>No es {@code Locale.getDefault()}: ese es el del proceso, y con el idioma por
     * app puede seguir en el del sistema. Con él, la fecha de Inicio llegó a salir en
     * inglés con la app en español (GP-071).
     *
     * @param ctx contexto con la configuración de la app
     * @return el locale con el que se están resolviendo los recursos
     */
    public static java.util.Locale localeDeLaApp(android.content.Context ctx) {
        return ctx.getResources().getConfiguration().getLocales().get(0);
    }

    /**
     * Fecha de un ISO-8601 en formato medio del idioma dado: «23 sept 2026» en
     * español, «Sep 23, 2026» en inglés. Solo usa la parte de fecha.
     *
     * @param iso    fecha ISO, con o sin hora
     * @param locale idioma de la interfaz
     * @return la fecha formateada, o {@code null} si no se puede leer
     */
    public static String formatearFechaMedia(String iso, java.util.Locale locale) {
        if (iso == null || iso.length() < 10) return null;
        try {
            java.text.SimpleDateFormat entrada = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            entrada.setLenient(false);
            java.util.Date fecha = entrada.parse(iso.substring(0, 10));
            return java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale).format(fecha);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    /**
     * Fecha y hora de un ISO-8601 en el formato del idioma dado: «30 sept 2026 18:05» en español,
     * «Sep 30, 2026 6:05 PM» en inglés (en Android; la JVM pone una coma antes de la hora). Sin hora en el ISO, solo la fecha.
     *
     * @return la fecha formateada, o {@code null} si no se puede leer
     */
    public static String formatearFechaHoraMedia(String iso, java.util.Locale locale) {
        java.util.Date fecha = leer(iso, iso != null && iso.length() >= 16);
        if (fecha == null) return null;
        java.text.DateFormat f = iso.length() >= 16
                ? java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT, locale)
                : java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale);
        return f.format(fecha);
    }

    /**
     * Fecha corta con el patrón del idioma ({@code inicio_fecha_corta_patron}: «d MMM» en español,
     * «MMM d» en inglés): «28 sept» / «Sep 28».
     *
     * @return la fecha formateada, o {@code null} si no se puede leer
     */
    public static String fechaCorta(String iso, java.util.Locale locale, String patron) {
        java.util.Date fecha = leer(iso, false);
        return fecha == null ? null : new java.text.SimpleDateFormat(patron, locale).format(fecha);
    }

    /** En qué tramo cae un día respecto a hoy, para decirlo como lo dice la app. */
    public enum Relativo {
        /** Hoy. */
        HOY,
        /** Ayer. */
        AYER,
        /** De 2 a 6 días: el día de la semana. */
        ESTA_SEMANA,
        /** De 7 a 13 días: «el jueves pasado». */
        SEMANA_PASADA,
        /** Dos semanas o más: la fecha corta. */
        FECHA
    }

    /**
     * El tramo de un día ISO respecto a {@code hoy}.
     *
     * @return null si la fecha no se entiende o es futura.
     */
    public static Relativo relativo(String iso, String hoy) {
        int dias = TiempoRelativo.diasEntre(iso, hoy);
        if (dias < 0) return null;
        if (dias == 0) return Relativo.HOY;
        if (dias == 1) return Relativo.AYER;
        if (dias < 7) return Relativo.ESTA_SEMANA;
        if (dias < 14) return Relativo.SEMANA_PASADA;
        return Relativo.FECHA;
    }

    /** Los textos de la regla de días relativos, sacados de los recursos (o puestos a mano en un test). */
    public static final class TextosDia {
        final String hoy, ayer, separador, pasado, patronCorto;

        /**
         * @param hoy         «Hoy».
         * @param ayer        «Ayer».
         * @param separador   «%1$s · %2$s».
         * @param pasado      «%1$s pasado» / «Last %1$s».
         * @param patronCorto «d MMM» / «MMM d».
         */
        public TextosDia(String hoy, String ayer, String separador, String pasado, String patronCorto) {
            this.hoy = hoy;
            this.ayer = ayer;
            this.separador = separador;
            this.pasado = pasado;
            this.patronCorto = patronCorto;
        }

        /** Los de la app, en su idioma. */
        public static TextosDia de(android.content.Context ctx) {
            return new TextosDia(ctx.getString(es.pmdm.gymprofit.R.string.tiempo_hoy_mayuscula),
                    ctx.getString(es.pmdm.gymprofit.R.string.tiempo_ayer_mayuscula),
                    ctx.getString(es.pmdm.gymprofit.R.string.hoy_toca_separador),
                    ctx.getString(es.pmdm.gymprofit.R.string.inicio_dia_pasado),
                    ctx.getString(es.pmdm.gymprofit.R.string.inicio_fecha_corta_patron));
        }
    }

    /**
     * La regla única de los días relativos: «Hoy», «Ayer · martes», «Lunes», «Jueves pasado» y,
     * pasadas dos semanas, «12 oct». La usan Inicio, el historial de Progreso y «Tus últimas veces»
     * de la ficha del ejercicio.
     *
     * @param iso    el día, ISO («yyyy-MM-dd…»).
     * @param hoy    hoy, «yyyy-MM-dd».
     * @return vacío si la fecha no se entiende o es futura.
     */
    public static String diaRelativo(String iso, String hoy, java.util.Locale idioma, TextosDia t) {
        Relativo r = relativo(iso, hoy);
        java.util.Date dia = leer(iso, false);
        if (r == null || dia == null) return "";
        if (r == Relativo.HOY) return t.hoy;
        if (r == Relativo.FECHA) return mayuscula(new java.text.SimpleDateFormat(t.patronCorto, idioma).format(dia), idioma);
        String diaSemana = mayuscula(new java.text.SimpleDateFormat("EEEE", idioma).format(dia), idioma);
        // En español los días van en minúscula dentro de una frase; en inglés, siempre en mayúscula.
        String enFrase = "es".equals(idioma.getLanguage()) ? diaSemana.toLowerCase(idioma) : diaSemana;
        if (r == Relativo.AYER) return String.format(idioma, t.separador, t.ayer, enFrase);
        if (r == Relativo.ESTA_SEMANA) return diaSemana;
        return mayuscula(String.format(idioma, t.pasado, enFrase), idioma);
    }

    /** {@link #diaRelativo(String, String, java.util.Locale, TextosDia)} con hoy y los textos de la app. */
    public static String diaRelativo(android.content.Context ctx, String iso) {
        return diaRelativo(iso, TiempoRelativo.hoy(), localeDeLaApp(ctx), TextosDia.de(ctx));
    }

    private static String mayuscula(String s, java.util.Locale idioma) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(idioma) + s.substring(1);
    }

    // El día (y la hora, si se pide) de un ISO; null si no se entiende.
    private static java.util.Date leer(String iso, boolean conHora) {
        if (iso == null || iso.length() < (conHora ? 16 : 10)) return null;
        try {
            java.text.SimpleDateFormat entrada = new java.text.SimpleDateFormat(
                    conHora ? "yyyy-MM-dd'T'HH:mm" : "yyyy-MM-dd", java.util.Locale.US);
            entrada.setLenient(false);
            return entrada.parse(iso.substring(0, conHora ? 16 : 10));
        } catch (java.text.ParseException e) {
            return null;
        }
    }
}
