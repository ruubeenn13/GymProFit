package com.gymprofit.api.service.productooff;

import com.gymprofit.api.entity.ProductoOff;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// ============================================================
// RacionesProducto — las raciones de un producto de Open Food Facts (lote 1.6.1)
//
// Por orden, que es el orden en que la app las ofrece (la primera, por defecto, en la
// ficha, la hoja del escáner y el «+»):
//   - El envase, «1 envase», si se lee sin ambigüedad (EnvaseProducto); en un multipack,
//     «1 unidad». Solo hasta 500 g: un paquete de 1 kg de arroz o un litro de leche no
//     se comen de una vez, y como cantidad por defecto apuntarían un disparate.
//   - La ración que declara el producto, «1 ración», si la trae y no pesa lo mismo que
//     el envase.
// Va primero el envase, salvo que la ración declarada sea más pequeña que él y no sea
// un multipack (GP-182, lote 1.6.4): unas galletas de 400 g con ración de 30 proponen
// 30 g, no el paquete. En un multipack la unidad ya es lo que se come de una vez.
// La usan el materializador (filas de alimento_raciones) y la búsqueda (un producto sin
// materializar, sin ids).
// ============================================================
public final class RacionesProducto {

    /** Más que esto, el envase no es una ración. */
    public static final BigDecimal ENVASE_MAXIMO = new BigDecimal("500");

    /** Una ración del producto, con sus nombres y de dónde sale. */
    public record Racion(String nombre, String nombreEn, BigDecimal gramos, String fuente) {
    }

    private RacionesProducto() {
    }

    /**
     * Las raciones del producto, en orden.
     *
     * @param producto producto de productos_off.
     * @return de 0 a 2 raciones.
     */
    public static List<Racion> de(ProductoOff producto) {
        return de(producto.getEnvase(), producto.getRacionGramos(), producto.getRacionTexto());
    }

    static List<Racion> de(String envase, BigDecimal racionGramos, String racionTexto) {
        List<Racion> raciones = new ArrayList<>();
        Optional<EnvaseProducto> leido = EnvaseProducto.leer(envase)
                .filter(e -> e.gramos().compareTo(ENVASE_MAXIMO) <= 0);
        leido.ifPresent(e -> raciones.add(e.unidad()
                ? new Racion("1 unidad", "1 unit", e.gramos(), recortar("Open Food Facts: envase " + envase))
                : new Racion("1 envase", "1 pack", e.gramos(), recortar("Open Food Facts: envase " + envase))));

        if (racionGramos != null && racionGramos.signum() > 0
                && leido.map(e -> e.gramos().compareTo(racionGramos) != 0).orElse(true)) {
            Racion racion = new Racion("1 ración", "1 serving", racionGramos,
                    recortar("Open Food Facts: " + (racionTexto == null
                            ? racionGramos.stripTrailingZeros().toPlainString() + " g" : racionTexto)));
            boolean delante = leido.map(e -> !e.unidad() && racionGramos.compareTo(e.gramos()) < 0).orElse(false);
            raciones.add(delante ? 0 : raciones.size(), racion);
        }
        return raciones;
    }

    private static String recortar(String texto) {
        if (texto.length() <= 255) return texto;
        int corte = Character.isHighSurrogate(texto.charAt(254)) ? 254 : 255;
        return texto.substring(0, corte);
    }
}
