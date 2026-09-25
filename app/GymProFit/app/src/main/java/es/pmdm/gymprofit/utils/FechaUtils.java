package es.pmdm.gymprofit.utils;

// ============================================================
// FechaUtils — utilidades de formato de fechas para la capa de vista.
// Con la migración a Retrofit+Gson (etapa 2) los POJOs guardan la fecha tal cual
// la envía la API (ISO-8601, p. ej. "2026-07-03T11:04:02.110442"), en lugar de
// una cadena ya formateada. El formateo para MOSTRAR pasa aquí (antes lo hacía
// UtilJSONParser.parseFecha al deserializar).
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

    // Convierte una fecha ISO-8601 al formato de visualización "dd/MM/yyyy HH:mm".
    // Robusto a fracciones de segundo (solo usa los caracteres de fecha y hora:minuto).
    // Si el valor es null/vacío o no encaja, devuelve "" o el original sin tocar.
    public static String formatearFechaHora(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        if (iso.length() >= 10) {
            String[] p = iso.substring(0, 10).split("-");
            if (p.length == 3) {
                String r = p[2] + "/" + p[1] + "/" + p[0];
                if (iso.length() >= 16) r += " " + iso.substring(11, 16);
                return r;
            }
        }
        return iso;
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
}
