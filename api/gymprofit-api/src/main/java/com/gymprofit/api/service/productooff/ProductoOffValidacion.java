package com.gymprofit.api.service.productooff;

import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;

import java.util.Optional;

// ============================================================
// ProductoOffValidacion — qué producto de Open Food Facts es aceptable (GP-164)
// Las mismas reglas que datos/productos/filtrar_off.py, que filtra antes de mandar:
// aquí se repiten porque la API no se fía de quien manda, y porque la lectura de un
// código suelto a Open Food Facts (GP-160) pasa por el mismo filtro.
//
// Con nombre, código numérico y los cuatro valores; nada por debajo de 0 ni por encima
// de 100 g por cada 100 g; kcal entre 0 y 900 que cuadran con los macros.
// ============================================================
public final class ProductoOffValidacion {

    // Mismo margen que el script: las etiquetas redondean y algunos fabricantes calculan
    // con factores propios; con menos se perdían productos buenos, con más entraban los
    // que confunden kJ y kcal (×4,18).
    static final double MARGEN_KCAL_ABSOLUTO = 20.0;
    static final double MARGEN_KCAL_RELATIVO = 0.20;

    private ProductoOffValidacion() {
    }

    /**
     * Devuelve el producto limpio (textos recortados a su columna, cifras redondeadas) o
     * vacío si no es aceptable. No modifica el de entrada.
     *
     * @param p producto tal como llega.
     * @return el producto listo para guardar, o vacío si no pasa las comprobaciones.
     */
    public static Optional<ProductoOffImportDTO> limpiar(ProductoOffImportDTO p) {
        if (p == null) return Optional.empty();
        String codigo = p.getCodigo() == null ? "" : p.getCodigo().trim();
        String nombre = recortar(p.getNombre(), 200);
        if (codigo.isEmpty() || codigo.length() > 32 || !codigo.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        if (nombre == null || p.getKcal() == null || p.getProteinas() == null
                || p.getCarbohidratos() == null || p.getGrasas() == null) {
            return Optional.empty();
        }
        double kcal = p.getKcal();
        double prot = p.getProteinas();
        double carb = p.getCarbohidratos();
        double gras = p.getGrasas();
        double fibra = p.getFibra() == null ? 0 : p.getFibra();
        for (double v : new double[]{prot, carb, gras, fibra}) {
            if (Double.isNaN(v) || v < 0 || v > 100) return Optional.empty();
        }
        if (prot + carb + gras + fibra > 100) return Optional.empty();
        if (Double.isNaN(kcal) || kcal < 0 || kcal > 900) return Optional.empty();
        double alcohol = p.getAlcohol() == null ? 0 : Math.max(0, p.getAlcohol());
        double polioles = p.getPolioles() == null ? 0 : Math.max(0, p.getPolioles());
        if (!kcalCuadran(kcal, prot, carb, gras, fibra, alcohol, polioles)) return Optional.empty();

        Double racion = p.getRacionGramos();
        if (racion != null && (racion <= 0 || racion > 2000)) racion = null;

        return Optional.of(new ProductoOffImportDTO(
                codigo, nombre, recortar(p.getMarca(), 100),
                redondear(kcal, 1), redondear(prot, 2), redondear(carb, 2), redondear(gras, 2),
                p.getFibra() == null ? null : redondear(fibra, 2),
                racion == null ? null : redondear(racion, 1),
                recortar(p.getRacionTexto(), 60), recortar(p.getEnvase(), 60),
                p.getEscaneos() == null ? 0 : Math.max(0, p.getEscaneos()),
                null, null));
    }

    /** ¿Las kcal declaradas se parecen a 4·P + 4·C + 9·G + 2·fibra + 7·alcohol + 2,4·polioles? */
    static boolean kcalCuadran(double kcal, double p, double c, double g, double fibra,
                               double alcohol, double polioles) {
        double calculadas = 4 * p + 4 * c + 9 * g + 2 * fibra + 7 * alcohol + 2.4 * polioles;
        double margen = Math.max(MARGEN_KCAL_ABSOLUTO, MARGEN_KCAL_RELATIVO * Math.max(kcal, calculadas));
        return Math.abs(kcal - calculadas) <= margen;
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null || texto.isBlank()) return null;
        String limpio = texto.strip();
        if (limpio.length() <= maximo) return limpio;
        // Sin partir un emoji por la mitad: la base no admite medio carácter.
        int corte = Character.isHighSurrogate(limpio.charAt(maximo - 1)) ? maximo - 1 : maximo;
        return limpio.substring(0, corte);
    }

    private static double redondear(double valor, int decimales) {
        double factor = Math.pow(10, decimales);
        return Math.round(valor * factor) / factor;
    }
}
