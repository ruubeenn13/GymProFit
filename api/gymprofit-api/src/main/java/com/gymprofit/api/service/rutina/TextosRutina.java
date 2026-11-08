package com.gymprofit.api.service.rutina;

import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.RutinaEjercicio;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;

// ============================================================
// TextosRutina — el nombre, la descripción, la categoría y las notas de una rutina en el idioma de la
// petición (GP-245)
//
// Hay tres clases de rutina:
//   · del catálogo, con las dos columnas (nombre y nombre_en): se elige una;
//   · copia de una plantilla, hecha al seguir un programa con el texto del idioma de aquella
//     petición: si su texto sigue siendo el de su plantilla en cualquiera de los dos idiomas,
//     se da el de la plantilla en el idioma de esta petición (quien lo siguió en inglés y pasa
//     la app a español ve «Torso A», no «Upper A»);
//   · y la escrita por el usuario, o la copia que renombró: su texto, sin traducir.
// Sin columnas nuevas: la copia guarda un solo texto y la plantilla tiene los dos.
// ============================================================
public final class TextosRutina {

    private TextosRutina() { }

    /** Si la petición en curso llegó en inglés (Accept-Language → LocaleContextHolder). */
    public static boolean enIngles() {
        return "en".equals(LocaleContextHolder.getLocale().getLanguage());
    }

    /**
     * Un texto de una rutina en el idioma pedido.
     *
     * @param en          si se pide en inglés.
     * @param propio      el texto de la rutina (el único que tiene una copia o una propia).
     * @param propioEn    su columna en inglés; solo la tienen las del catálogo.
     * @param plantilla   el texto en español de su plantilla, o null si no es copia.
     * @param plantillaEn el de su plantilla en inglés, o null.
     * @return el texto que se enseña; {@code propio} si no hay nada que traducir.
     */
    public static String localizado(boolean en, String propio, String propioEn, String plantilla, String plantillaEn) {
        if (propio == null) return null;
        if (!vacio(propioEn)) return en ? propioEn : propio;
        if (plantilla != null && (propio.equals(plantilla) || propio.equals(plantillaEn))) {
            return en && !vacio(plantillaEn) ? plantillaEn : plantilla;
        }
        return propio;
    }

    /** El nombre de la rutina en el idioma pedido. */
    public static String nombre(Rutina r, boolean en) {
        Rutina p = r.getPlantilla();
        return localizado(en, r.getNombre(), r.getNombreEn(),
                p == null ? null : p.getNombre(), p == null ? null : p.getNombreEn());
    }

    /** La descripción de la rutina en el idioma pedido. */
    public static String descripcion(Rutina r, boolean en) {
        Rutina p = r.getPlantilla();
        return localizado(en, r.getDescripcion(), r.getDescripcionEn(),
                p == null ? null : p.getDescripcion(), p == null ? null : p.getDescripcionEn());
    }

    /** La categoría de la rutina en el idioma pedido. */
    public static String categoria(Rutina r, boolean en) {
        Rutina p = r.getPlantilla();
        return localizado(en, r.getCategoria(), r.getCategoriaEn(),
                p == null ? null : p.getCategoria(), p == null ? null : p.getCategoriaEn());
    }

    /**
     * La nota de un ejercicio de una rutina en el idioma pedido. Las del catálogo tienen las dos
     * columnas; la de una copia, el texto del idioma en que se copió: si es el de alguna nota de su
     * plantilla, en cualquiera de los dos idiomas, sale la de la plantilla en el idioma pedido; si
     * el usuario la cambió, la suya.
     *
     * @param plantilla las filas de su plantilla (vacío si no es copia).
     */
    public static String nota(boolean en, RutinaEjercicio re, List<RutinaEjercicio> plantilla) {
        String propia = re.getNotas();
        if (propia == null) return null;
        if (!vacio(re.getNotasEn())) return en ? re.getNotasEn() : propia;
        for (RutinaEjercicio p : plantilla) {
            if (propia.equals(p.getNotas()) || propia.equals(p.getNotasEn())) {
                return localizado(en, propia, null, p.getNotas(), p.getNotasEn());
            }
        }
        return propia;
    }

    /**
     * Lo que una consulta SQL tiene que añadir para llamar a {@link #localizado} con el nombre:
     * las columnas {@code rutina_nombre_en}, {@code plantilla_nombre} y {@code plantilla_nombre_en}
     * de la rutina {@code r} y su plantilla. Va tras el {@code LEFT JOIN rutinas r}.
     */
    public static final String JOIN_PLANTILLA = " LEFT JOIN rutinas rp ON rp.id = r.plantilla_id ";

    /** Las columnas que añade {@link #JOIN_PLANTILLA}, para el SELECT. */
    public static final String COLUMNAS_PLANTILLA =
            " r.nombre_en AS rutina_nombre_en, rp.nombre AS plantilla_nombre, rp.nombre_en AS plantilla_nombre_en ";

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
