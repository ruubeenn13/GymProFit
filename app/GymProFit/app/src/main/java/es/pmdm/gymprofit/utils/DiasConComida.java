package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.model.comida.ResumenDiarioNutricion;

// ============================================================
// DiasConComida — los días que cuentan en Estadísticas de nutrición (lote 12).
//
// GET /comidas/usuario/{id}/resumen devuelve un día por cada comida que exista, también una
// vacía: la que queda al quitar su último alimento o al marcar «Hoy no». Contarla daba «2 días
// registrados» con media 0 kcal junto a «No hay comidas apuntadas en estas fechas». Un día
// cuenta si tiene algo apuntado: kcal o algún macro.
// ============================================================
public final class DiasConComida {

    private DiasConComida() {}

    /** Los días con algo apuntado, en el mismo orden. */
    @NonNull
    public static List<ResumenDiarioNutricion> de(@Nullable List<ResumenDiarioNutricion> dias) {
        List<ResumenDiarioNutricion> out = new ArrayList<>();
        if (dias == null) return out;
        for (ResumenDiarioNutricion r : dias) {
            if (r == null) continue;
            if (r.getCalorias() > 0 || r.getProteinas() > 0 || r.getCarbohidratos() > 0 || r.getGrasas() > 0) {
                out.add(r);
            }
        }
        return out;
    }
}
