package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.model.alimento.Alimento;

// ============================================================
// PedidoCantidad — los cuerpos de añadir y de actualizar una cantidad (lote 1.6.1)
//
// Añadir (POST /comidas/anadir): el alimento por su id o, si es un producto que aún no
// está en el catálogo (id 0), por su código, que la API materializa; la cantidad en
// gramos o en raciones: la ración por su id o, si no lo tiene (producto sin
// materializar), por su posición, que es la misma al materializarse.
// Actualizar (PATCH /alimentos-comida/{id}): la ración y cuántas, y la API pone los
// gramos; o solo los gramos, que quitan la ración que hubiera.
// ============================================================
public final class PedidoCantidad {

    private PedidoCantidad() {
    }

    @NonNull
    public static Map<String, Object> anadir(@NonNull Alimento alimento, @NonNull CantidadFicha cantidad,
                                             @NonNull String fecha, @NonNull String tipoComida) {
        Map<String, Object> b = new HashMap<>();
        b.put("fecha", fecha);
        b.put("tipoComida", tipoComida);
        if (alimento.getId() > 0) b.put("alimentoId", alimento.getId());
        else b.put("barcode", alimento.getBarcode());
        CantidadFicha.Unidad u = cantidad.unidad();
        if (u.esGramos()) {
            b.put("cantidadGramos", decimal(cantidad.gramos()));
        } else {
            if (u.racionId != null) b.put("racionId", u.racionId);
            else b.put("racionIndice", u.indice);
            b.put("raciones", decimal(cantidad.valor()));
        }
        return b;
    }

    @NonNull
    public static Map<String, Object> actualizar(@NonNull CantidadFicha cantidad) {
        Map<String, Object> b = new HashMap<>();
        CantidadFicha.Unidad u = cantidad.unidad();
        if (!u.esGramos() && u.racionId != null) {
            b.put("racionId", u.racionId);
            b.put("raciones", decimal(cantidad.valor()));
        } else {
            b.put("cantidadGramos", decimal(cantidad.gramos()));
        }
        return b;
    }

    private static BigDecimal decimal(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    }
}
