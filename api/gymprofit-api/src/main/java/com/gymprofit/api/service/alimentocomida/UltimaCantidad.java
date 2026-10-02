package com.gymprofit.api.service.alimentocomida;

import com.gymprofit.api.dto.entity.alimento.UltimaCantidadDTO;
import com.gymprofit.api.entity.AlimentoRacion;
import com.gymprofit.api.service.busqueda.UnidadesRacion;

import java.math.BigDecimal;
import java.util.List;

// ============================================================
// UltimaCantidad — la última línea de un alimento, lista para la app (lote 1.6.3, A1)
// La búsqueda y los favoritos leen de la base los gramos, la ración y cuántas de la
// línea más reciente; esto le pone a la ración su nombre y su unidad en el idioma de la
// petición, con las raciones del alimento que ya están cargadas (sin otra consulta), y
// aplica GP-177: si la ración ya no pesa lo mismo, solo los gramos.
// ============================================================
public final class UltimaCantidad {

    /** Lo que se lee de la base de una línea: gramos y, si la tiene, la ración y cuántas. */
    public record Linea(BigDecimal gramos, Integer racionId, BigDecimal raciones) {
    }

    private UltimaCantidad() {
    }

    /**
     * @param linea     la línea más reciente; null si nunca se ha apuntado.
     * @param deAlimento las raciones del alimento (cargadas), donde se busca la de la línea.
     * @param ingles    si la petición llega en inglés.
     * @return la última cantidad, o null si no hay línea.
     */
    public static UltimaCantidadDTO de(Linea linea, List<AlimentoRacion> deAlimento, boolean ingles) {
        if (linea == null || linea.gramos() == null) return null;
        UltimaCantidadDTO dto = new UltimaCantidadDTO();
        dto.setCantidadGramos(linea.gramos());
        if (linea.racionId() == null || deAlimento == null) return dto;
        AlimentoRacion racion = deAlimento.stream()
                .filter(r -> linea.racionId().equals(r.getId()))
                .findFirst().orElse(null);
        if (racion == null || !RacionVigente.cuadra(linea.gramos(), racion.getGramos(), linea.raciones())) return dto;
        String nombre = ingles ? racion.getNombreEn() : racion.getNombre();
        dto.setRacionId(racion.getId());
        dto.setRacionNombre(nombre);
        dto.setRacionGramos(racion.getGramos());
        dto.setRaciones(linea.raciones());
        UnidadesRacion.de(nombre, ingles).ifPresent(u -> {
            dto.setRacionUnidad(u.singular());
            dto.setRacionUnidadPlural(u.plural());
        });
        return dto;
    }
}
