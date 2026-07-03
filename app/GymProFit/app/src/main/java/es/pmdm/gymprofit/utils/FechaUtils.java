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
}
