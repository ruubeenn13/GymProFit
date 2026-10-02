package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;

import java.util.Locale;

import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.UltimaCantidad;

// ============================================================
// AnadirRapido — qué añade el «+» de una fila (decisión 1 del lienzo, lote 1.6.3)
//
// Sin preguntar: la última cantidad que apuntaste de ese alimento, si la hay (con su
// ración si se apuntó así y la ración sigue pesando lo mismo, que eso ya lo decide la
// API); si no, lo que propondría la ficha: la primera ración por una, o 100 g. Así el
// «+» y la ficha nunca proponen cosas distintas para lo mismo.
// Sin vistas, para probarlo: la fila pinta el texto y Añadir manda el pedido.
// ============================================================
public final class AnadirRapido {

    private AnadirRapido() {
    }

    /** La cantidad que añade el «+». */
    @NonNull
    public static CantidadFicha cantidad(@NonNull Alimento a) {
        UltimaCantidad u = a.getUltima();
        if (u != null && u.getCantidadGramos() > 0) {
            return CantidadFicha.deLinea(a.getRaciones(), u.getRacionId(), u.getRaciones(), u.getCantidadGramos());
        }
        return CantidadFicha.nueva(a.getRaciones());
    }

    /** ¿Tiene una última cantidad que enseñar? */
    public static boolean tieneUltima(@NonNull Alimento a) {
        return a.getUltima() != null && a.getUltima().getCantidadGramos() > 0;
    }

    /** Las kcal de esa cantidad, como las cuenta la API (redondeo de kcal × g / 100). */
    public static long kcal(@NonNull Alimento a, @NonNull CantidadFicha c) {
        return Math.round(a.getCalorias() * c.gramos() / 100.0);
    }

    /** «1 envase (200 g)», «2 rebanadas (56 g)» o «150 g». */
    @NonNull
    public static String texto(@NonNull Cantidades.Formatos f, @NonNull Locale locale, @NonNull CantidadFicha c) {
        CantidadFicha.Unidad u = c.unidad();
        return Cantidades.texto(f, locale, u.nombre, u.unidad, u.unidadPlural, c.raciones(), c.gramos());
    }
}
