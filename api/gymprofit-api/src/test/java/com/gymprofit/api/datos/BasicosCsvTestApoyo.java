package com.gymprofit.api.datos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// BasicosCsvTestApoyo — lee datos/basicos/*.csv para los tests (GP-127)
// ============================================================
public final class BasicosCsvTestApoyo {

    private static final Path CARPETA = Path.of("datos", "basicos");

    private BasicosCsvTestApoyo() {
    }

    /** Filas de basicos.csv, por nombre de columna. */
    public static List<Map<String, String>> basicos() throws IOException {
        return leerCsv(CARPETA.resolve("basicos.csv"));
    }

    /** Filas de raciones.csv, por nombre de columna. */
    public static List<Map<String, String>> raciones() throws IOException {
        return leerCsv(CARPETA.resolve("raciones.csv"));
    }

    /** CSV con «;» y comillas dobles (las que escribe csv.DictWriter de Python). */
    public static List<Map<String, String>> leerCsv(Path ruta) throws IOException {
        List<String> lineas = Files.readAllLines(ruta, StandardCharsets.UTF_8);
        List<String> cabecera = campos(lineas.get(0));
        List<Map<String, String>> filas = new ArrayList<>();
        for (String linea : lineas.subList(1, lineas.size())) {
            if (linea.isBlank()) continue;
            List<String> valores = campos(linea);
            Map<String, String> fila = new HashMap<>();
            for (int i = 0; i < cabecera.size(); i++) {
                fila.put(cabecera.get(i), i < valores.size() ? valores.get(i) : "");
            }
            filas.add(fila);
        }
        return filas;
    }

    private static List<String> campos(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char ch = linea.charAt(i);
            if (entreComillas) {
                if (ch == '"' && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else if (ch == '"') {
                    entreComillas = false;
                } else {
                    actual.append(ch);
                }
            } else if (ch == '"') {
                entreComillas = true;
            } else if (ch == ';') {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(ch);
            }
        }
        campos.add(actual.toString());
        return campos;
    }
}
