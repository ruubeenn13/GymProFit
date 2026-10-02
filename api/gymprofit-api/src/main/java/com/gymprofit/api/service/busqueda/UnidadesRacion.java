package com.gymprofit.api.service.busqueda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// ============================================================
// UnidadesRacion — la unidad de una ración, en singular y en plural (GP-172, DEC-043)
//
// «1 rebanada» → rebanada / rebanadas. El plural no se calcula: es un dato, el diccionario
// versionado busqueda/unidades_racion.txt, por idioma («1 flan»: flanes / flans). Lo usan
// RacionDTO y la línea de una comida para que la app escriba «2 rebanadas (56 g)».
// Vale para los básicos, para los productos (también los ya materializados) y para una
// ración propia, sin columnas nuevas. Un nombre que no está, o que no empieza por «1 »
// («Media taza»), no tiene unidad: la app lo enseña con «×», como en la 1.6.1.
// ============================================================
public final class UnidadesRacion {

    /** La unidad de una ración, sin el «1» y sin paréntesis. */
    public record Unidad(String singular, String plural) {
    }

    private static final Map<String, Unidad> ES = new HashMap<>();
    private static final Map<String, Unidad> EN = new HashMap<>();

    static {
        try (InputStream in = UnidadesRacion.class.getResourceAsStream("/busqueda/unidades_racion.txt")) {
            if (in == null) throw new IllegalStateException("Falta busqueda/unidades_racion.txt");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linea;
            while ((linea = r.readLine()) != null) {
                linea = linea.strip();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                // «es: 1 rebanada = rebanada | rebanadas»
                int dosPuntos = linea.indexOf(':');
                int igual = linea.indexOf(" = ");
                int barra = linea.indexOf(" | ");
                if (dosPuntos < 0 || igual < 0 || barra < igual) {
                    throw new IllegalStateException("Línea mal formada en unidades_racion.txt: " + linea);
                }
                String idioma = linea.substring(0, dosPuntos).strip();
                String nombre = linea.substring(dosPuntos + 1, igual).strip();
                Unidad u = new Unidad(linea.substring(igual + 3, barra).strip(), linea.substring(barra + 3).strip());
                ("en".equals(idioma) ? EN : ES).put(nombre, u);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private UnidadesRacion() {
    }

    /**
     * @param nombre el nombre de la ración tal como se guarda («1 rebanada»).
     * @param ingles si el nombre es el inglés.
     * @return su unidad, o vacío si no empieza por «1 » o no está en el diccionario.
     */
    public static Optional<Unidad> de(String nombre, boolean ingles) {
        if (nombre == null || !nombre.startsWith("1 ")) return Optional.empty();
        return Optional.ofNullable((ingles ? EN : ES).get(nombre.strip()));
    }
}
