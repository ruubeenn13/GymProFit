package com.gymprofit.api.service.productooff;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// EnvaseProducto — lo que trae un envase de Open Food Facts, leído sin ambigüedad (1.6.1)
//
// El campo `envase` es texto libre («500 g», «4 x 125 g», «500 g (4 x 125 g)», «1 l»,
// «250», «6 pcs»…). Se lee solo lo que no admite dos lecturas:
//   · una cantidad con su unidad de masa o de volumen: el envase entero;
//   · un multipack de unidades iguales («4 x 125 g», o el total con el desglose entre
//     paréntesis si cuadra): una unidad.
// Lo demás no se lee: cifras sin unidad, piezas, onzas (de peso o de volumen, no se
// sabe), neto y escurrido, o un desglose que no suma el total.
//
// El volumen cuenta como masa, 1 ml = 1 g: Open Food Facts da los valores de un líquido
// por 100 ml en los mismos campos que por 100 g, y la app ya los trata igual.
// ============================================================
/**
 * @param gramos gramos del envase entero, o de una unidad si es un multipack.
 * @param unidad true si es una unidad de un multipack.
 * @param veces  cuántas unidades trae; 1 si es el envase entero.
 */
public record EnvaseProducto(BigDecimal gramos, boolean unidad, int veces) {

    // Más de 10 kg en un envase no es un envase de comer: o es un error, o no sirve.
    private static final BigDecimal MAXIMO = new BigDecimal("10000");

    private static final String NUM = "(\\d+(?:\\.\\d+)?)";
    private static final String UNIDAD =
            "(g|gr|grs|gramos?|grams?|kg|kgs|kilos?|kilogramos?|ml|cl|l|lt|litros?|liters?|litres?)";
    private static final Pattern SOLO = Pattern.compile("^" + NUM + "\\s*" + UNIDAD + "$");
    private static final Pattern POR = Pattern.compile("^(\\d+)\\s*x\\s*" + NUM + "\\s*" + UNIDAD + "$");
    private static final Pattern POR_AL_REVES = Pattern.compile("^" + NUM + "\\s*" + UNIDAD + "\\s*x\\s*(\\d+)$");
    private static final Pattern CON_DESGLOSE = Pattern.compile("^(.+?)\\s*\\((.+)\\)$");

    /**
     * Lee el envase.
     *
     * @param texto el campo `envase` de productos_off; null o vacío, nada.
     * @return el envase entero o una unidad del multipack, o vacío si no se lee sin dudas.
     */
    public static Optional<EnvaseProducto> leer(String texto) {
        if (texto == null) return Optional.empty();
        String t = texto.trim().toLowerCase(Locale.ROOT).replace(',', '.');
        while (t.endsWith(".")) t = t.substring(0, t.length() - 1).trim();
        if (t.isEmpty()) return Optional.empty();

        Matcher desglose = CON_DESGLOSE.matcher(t);
        if (desglose.matches()) {
            // «500 g (4 x 125 g)»: vale si el total es una cantidad y el desglose, un
            // multipack que suma ese total.
            Optional<BigDecimal> total = cantidad(desglose.group(1).trim());
            Optional<EnvaseProducto> unidad = multipack(desglose.group(2).trim());
            if (total.isEmpty() || unidad.isEmpty()) return Optional.empty();
            return unidad.filter(u -> cuadra(total.get(), u));
        }
        Optional<EnvaseProducto> unidad = multipack(t);
        if (unidad.isPresent()) return unidad;
        return cantidad(t).map(g -> new EnvaseProducto(g, false, 1));
    }

    private static Optional<BigDecimal> cantidad(String t) {
        Matcher m = SOLO.matcher(t);
        if (!m.matches()) return Optional.empty();
        return gramos(m.group(1), m.group(2));
    }

    // «4 x 125 g» o «125 g x 4». «1 x 30 g» es un envase de 30 g, no un multipack.
    private static Optional<EnvaseProducto> multipack(String t) {
        Matcher m = POR.matcher(t);
        String veces, num, unidad;
        if (m.matches()) {
            veces = m.group(1);
            num = m.group(2);
            unidad = m.group(3);
        } else {
            m = POR_AL_REVES.matcher(t);
            if (!m.matches()) return Optional.empty();
            num = m.group(1);
            unidad = m.group(2);
            veces = m.group(3);
        }
        int n = Integer.parseInt(veces);
        if (n < 1 || n > 100) return Optional.empty();
        Optional<BigDecimal> g = gramos(num, unidad);
        if (g.isEmpty()) return Optional.empty();
        if (n == 1) return Optional.of(new EnvaseProducto(g.get(), false, 1));
        if (g.get().multiply(BigDecimal.valueOf(n)).compareTo(MAXIMO) > 0) return Optional.empty();
        return Optional.of(new EnvaseProducto(g.get(), true, n));
    }

    private static boolean cuadra(BigDecimal total, EnvaseProducto unidad) {
        BigDecimal suma = unidad.gramos.multiply(BigDecimal.valueOf(unidad.veces));
        // Medio gramo de redondeo por unidad, como mucho.
        return suma.subtract(total).abs().compareTo(BigDecimal.valueOf(unidad.veces).multiply(new BigDecimal("0.5"))) <= 0;
    }

    private static Optional<BigDecimal> gramos(String num, String unidad) {
        BigDecimal n;
        try {
            n = new BigDecimal(num);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        BigDecimal factor = switch (unidad) {
            case "kg", "kgs", "kilo", "kilos", "kilogramo", "kilogramos", "l", "lt", "litro", "litros",
                 "liter", "liters", "litre", "litres" -> BigDecimal.valueOf(1000);
            case "cl" -> BigDecimal.TEN;
            default -> BigDecimal.ONE;
        };
        BigDecimal g = n.multiply(factor).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        if (g.signum() <= 0 || g.compareTo(MAXIMO) > 0) return Optional.empty();
        return Optional.of(g);
    }
}
