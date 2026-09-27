package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.R;

// ============================================================
// Zonas — de músculo a zona del cuerpo, en un solo sitio (GP-088).
//
// La API reduce cada ejercicio a una clave de músculo (Musculos.normalizar, en la
// API), la misma para la silueta de Inicio y para los récords. Qué músculos forman
// cada zona vivía dentro de la pestaña Inicio; la pantalla de Récords agrupa por zona y
// una segunda copia de la tabla acabaría diciendo otra cosa. Por eso está aquí.
//
// El orden importa: es el de lo grave que es saltarse la zona (el de
// la frase de la silueta) y el de las secciones de la pantalla de Récords.
// ============================================================
public final class Zonas {

    /** Una zona: su nombre en frase («te falta pierna»), su título y sus músculos. */
    public static final class Zona {
        @StringRes public final int nombre;
        @StringRes public final int titulo;
        public final List<String> musculos;

        Zona(@StringRes int nombre, @StringRes int titulo, String... musculos) {
            this.nombre = nombre;
            this.titulo = titulo;
            this.musculos = Collections.unmodifiableList(Arrays.asList(musculos));
        }
    }

    public static final Zona PIERNA  = new Zona(R.string.zona_pierna,  R.string.zona_titulo_pierna,
            "cuadriceps", "isquiotibiales", "gluteos", "gemelos", "aductores");
    public static final Zona ESPALDA = new Zona(R.string.zona_espalda, R.string.zona_titulo_espalda,
            "dorsales", "trapecios", "lumbares");
    public static final Zona PECHO   = new Zona(R.string.zona_pecho,   R.string.zona_titulo_pecho, "pecho");
    public static final Zona HOMBROS = new Zona(R.string.zona_hombros, R.string.zona_titulo_hombros, "hombros");
    public static final Zona BRAZOS  = new Zona(R.string.zona_brazos,  R.string.zona_titulo_brazos,
            "biceps", "triceps", "antebrazos");
    public static final Zona CORE    = new Zona(R.string.zona_core,    R.string.zona_titulo_core, "abdominales");

    /** Las zonas en su orden. */
    public static final List<Zona> TODAS =
            Collections.unmodifiableList(Arrays.asList(PIERNA, ESPALDA, PECHO, HOMBROS, BRAZOS, CORE));

    /**
     * Las seis zonas en el orden de los chips (GP-105): «Esta semana» de Inicio y los
     * filtros de la biblioteca de Entrenar. No es el de TODAS, que va por gravedad.
     */
    public static final List<Zona> ORDEN_CHIPS =
            Collections.unmodifiableList(Arrays.asList(PECHO, ESPALDA, PIERNA, HOMBROS, BRAZOS, CORE));

    private Zonas() { }

    /**
     * Series por zona, en el orden de los chips: las seis zonas siempre, con 0 si no
     * se ha tocado ninguno de sus músculos. Los músculos sin zona no cuentan.
     *
     * @param seriesPorMusculo series por clave de músculo (la de /volumen-muscular).
     */
    public static java.util.LinkedHashMap<Zona, Integer> seriesPorZona(
            @Nullable Map<String, Integer> seriesPorMusculo) {
        java.util.LinkedHashMap<Zona, Integer> porZona = new java.util.LinkedHashMap<>();
        for (Zona z : ORDEN_CHIPS) porZona.put(z, 0);
        if (seriesPorMusculo == null) return porZona;
        for (Map.Entry<String, Integer> e : seriesPorMusculo.entrySet()) {
            Zona z = deMusculo(e.getKey());
            if (z != null && e.getValue() != null && e.getValue() > 0) {
                porZona.put(z, porZona.get(z) + e.getValue());
            }
        }
        return porZona;
    }

    /**
     * Zona de una clave de músculo.
     *
     * @return la zona, o {@code null} si el músculo no cae en ninguna (cuello, cardio
     *         o un ejercicio sin músculo conocido).
     */
    @Nullable
    public static Zona deMusculo(@Nullable String musculo) {
        if (musculo == null) return null;
        for (Zona z : TODAS) {
            if (z.musculos.contains(musculo)) return z;
        }
        return null;
    }

    /**
     * Primera zona, en el orden de TODAS, sin ninguna serie.
     *
     * @param seriesPorMusculo series por clave de músculo
     * @return la zona sin trabajar, o {@code null} si se han tocado todas.
     */
    @Nullable
    public static Zona sinTrabajar(Map<String, Integer> seriesPorMusculo) {
        for (Zona z : TODAS) {
            boolean tocada = false;
            for (String m : z.musculos) {
                Integer series = seriesPorMusculo.get(m);
                if (series != null && series > 0) { tocada = true; break; }
            }
            if (!tocada) return z;
        }
        return null;
    }
}
