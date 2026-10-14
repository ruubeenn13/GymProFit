package com.gymprofit.api.service.alimentocomida;

import com.gymprofit.api.dto.entity.alimento.UltimaCantidadDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoRacion;
import com.gymprofit.api.service.busqueda.UnidadesRacion;
import com.gymprofit.api.service.plato.Platos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// ============================================================
// UltimaCantidad — la última línea de un alimento, lista para la app (lote 1.6.3, A1)
// La búsqueda y los favoritos leen de la base los gramos, la ración y cuántas de la
// línea más reciente; esto le pone a la ración su nombre y su unidad en el idioma de la
// petición, con las raciones del alimento que ya están cargadas (sin otra consulta), y
// aplica GP-177: si la ración ya no pesa lo mismo, solo los gramos.
//
// Un plato (lote 1.6.6, DEC-047): lo apuntado por raciones se repite en raciones, de las
// que tiene hoy; lo apuntado en gramos, en gramos si aún tiene peso hecho, y si no, nada
// (el «+» añade una ración, como si nunca se hubiera apuntado).
// ============================================================
public final class UltimaCantidad {

    /** Lo que se lee de la base de una línea: gramos y, si la tiene, la ración y cuántas. */
    public record Linea(BigDecimal gramos, Integer racionId, BigDecimal raciones) {
    }

    private UltimaCantidad() {
    }

    /**
     * @param linea    la línea más reciente; null si nunca se ha apuntado.
     * @param alimento el alimento, con sus raciones cargadas, donde se busca la de la línea.
     * @param ingles   si la petición llega en inglés.
     * @return la última cantidad, o null si no hay línea (o, en un plato, no se puede repetir).
     */
    public static UltimaCantidadDTO de(Linea linea, Alimento alimento, boolean ingles) {
        if (linea == null || linea.gramos() == null) return null;
        List<AlimentoRacion> deAlimento = alimento.getRaciones();
        AlimentoRacion racion = linea.racionId() == null || deAlimento == null ? null : deAlimento.stream()
                .filter(r -> linea.racionId().equals(r.getId()))
                .findFirst().orElse(null);
        if (Platos.esPlato(alimento)) return dePlato(linea, alimento, racion, ingles);
        UltimaCantidadDTO dto = new UltimaCantidadDTO();
        dto.setCantidadGramos(linea.gramos());
        if (racion == null || !RacionVigente.cuadra(linea.gramos(), racion.getGramos(), linea.raciones())) return dto;
        ponerRacion(dto, racion, linea.raciones(), ingles);
        return dto;
    }

    private static UltimaCantidadDTO dePlato(Linea linea, Alimento plato, AlimentoRacion racion, boolean ingles) {
        AlimentoRacion actual = Platos.racionActual(plato);
        boolean porRaciones = Platos.esRacionSinPeso(racion) || (Platos.esRacionDePlato(racion)
                && RacionVigente.cuadra(linea.gramos(), racion.getGramos(), linea.raciones()));
        UltimaCantidadDTO dto = new UltimaCantidadDTO();
        if (porRaciones && actual != null) {
            dto.setCantidadGramos(actual.getGramos().multiply(linea.raciones()).setScale(2, RoundingMode.HALF_UP));
            ponerRacion(dto, actual, linea.raciones(), ingles);
            return dto;
        }
        if (!Platos.conPeso(plato)) return null;
        dto.setCantidadGramos(linea.gramos());
        return dto;
    }

    private static void ponerRacion(UltimaCantidadDTO dto, AlimentoRacion racion, BigDecimal raciones, boolean ingles) {
        String nombre = ingles ? racion.getNombreEn() : racion.getNombre();
        dto.setRacionId(racion.getId());
        dto.setRacionNombre(nombre);
        dto.setRacionGramos(racion.getGramos());
        dto.setRaciones(raciones);
        dto.setRacionSinPeso(Platos.esRacionSinPeso(racion) ? Boolean.TRUE : null);
        UnidadesRacion.de(nombre, ingles).ifPresent(u -> {
            dto.setRacionUnidad(u.singular());
            dto.setRacionUnidadPlural(u.plural());
        });
    }
}
