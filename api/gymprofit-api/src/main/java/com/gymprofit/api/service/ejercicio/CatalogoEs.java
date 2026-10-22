package com.gymprofit.api.service.ejercicio;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// ============================================================
// CatalogoEs — el español del catálogo de ejercicios (GP-217)
//
// La fuente de verdad del español de los nombres y las descripciones es un CSV versionado,
// catalogo/ejercicios-es.csv, con tres columnas: fed_id (la clave estable del ejercicio),
// nombre_es y descripcion_es. Está escrito a mano, sin traductor automático, siguiendo
// documentacion/catalogo/glosario-ejercicios.md. La importación (WgerImportService) lo lee
// y no lo pisa, y la migración de datos lo aplica a lo que ya hay en la base.
//
// El CSV es RFC 4180: campos entre comillas con "" para una comilla y saltos de línea dentro
// de la descripción (un paso por línea). Sin librerías: son cuarenta líneas.
// ============================================================
@Component
public class CatalogoEs {

    /** Dónde está el CSV dentro de los recursos. */
    public static final String RECURSO = "catalogo/ejercicios-es.csv";

    /**
     * El español de un ejercicio.
     *
     * @param descripcion los pasos numerados, uno por línea; vacía si el ejercicio no tiene
     *                    texto original que traducir.
     */
    public record Entrada(String fedId, String nombre, String descripcion) { }

    private final Map<String, Entrada> porFedId;

    /** Lee el CSV de los recursos; sin él (o mal formado) la aplicación no arranca. */
    public CatalogoEs() {
        this(cargarRecurso());
    }

    CatalogoEs(Map<String, Entrada> porFedId) {
        this.porFedId = Collections.unmodifiableMap(porFedId);
    }

    /** Un catálogo a partir de un CSV cualquiera (para los tests). */
    public static CatalogoEs de(Reader csv) {
        try {
            return new CatalogoEs(leer(csv));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Lo que dice el CSV de un ejercicio, o vacío si no está. */
    public Optional<Entrada> de(String fedId) {
        return Optional.ofNullable(porFedId.get(fedId));
    }

    /** Todas las entradas, en el orden del fichero. */
    public Collection<Entrada> todas() {
        return porFedId.values();
    }

    /** Cuántos ejercicios trae. */
    public int tamano() {
        return porFedId.size();
    }

    private static Map<String, Entrada> cargarRecurso() {
        try (InputStream in = CatalogoEs.class.getClassLoader().getResourceAsStream(RECURSO)) {
            if (in == null) {
                throw new IllegalStateException("No está el catálogo en español: " + RECURSO);
            }
            return leer(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + RECURSO, e);
        }
    }

    /**
     * Lee el CSV. La primera fila es la cabecera (fed_id, nombre_es, descripcion_es).
     *
     * @throws IllegalStateException si falta la cabecera, una fila no tiene tres columnas, un
     *                               fed_id o un nombre vienen vacíos o se repiten.
     */
    static Map<String, Entrada> leer(Reader reader) throws IOException {
        List<List<String>> filas = filas(reader);
        if (filas.isEmpty() || !filas.get(0).equals(List.of("fed_id", "nombre_es", "descripcion_es"))) {
            throw new IllegalStateException("El CSV del catálogo debe empezar por fed_id,nombre_es,descripcion_es");
        }
        Map<String, Entrada> mapa = new LinkedHashMap<>();
        java.util.Set<String> nombres = new java.util.HashSet<>();
        for (int i = 1; i < filas.size(); i++) {
            List<String> f = filas.get(i);
            if (f.size() == 1 && f.get(0).isEmpty()) continue; // línea en blanco
            if (f.size() != 3) {
                throw new IllegalStateException("Fila " + (i + 1) + " del CSV: se esperaban 3 columnas y hay " + f.size());
            }
            String fedId = f.get(0).trim();
            String nombre = f.get(1).trim();
            if (fedId.isEmpty() || nombre.isEmpty()) {
                throw new IllegalStateException("Fila " + (i + 1) + " del CSV: fed_id o nombre vacíos");
            }
            if (mapa.containsKey(fedId)) {
                throw new IllegalStateException("fed_id repetido en el CSV: " + fedId);
            }
            if (!nombres.add(nombre.toLowerCase())) {
                throw new IllegalStateException("Nombre repetido en el CSV: " + nombre);
            }
            mapa.put(fedId, new Entrada(fedId, nombre, f.get(2).replace("\r\n", "\n").trim()));
        }
        return mapa;
    }

    private static final int LF = 10, CR = 13, BOM = 0xFEFF, COMILLA = 34, COMA = 44;

    // Descompone el texto en filas de campos (RFC 4180).
    static List<List<String>> filas(Reader origen) throws IOException {
        java.io.PushbackReader reader = new java.io.PushbackReader(origen, 1);
        List<List<String>> filas = new ArrayList<>();
        List<String> fila = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        boolean primero = true;
        int c;
        while ((c = reader.read()) != -1) {
            if (primero) {
                primero = false;
                if (c == BOM) continue; // marca de orden de bytes
            }
            if (entreComillas) {
                if (c == COMILLA) {
                    int sig = reader.read();
                    if (sig == COMILLA) {
                        campo.append((char) COMILLA);
                    } else {
                        entreComillas = false;
                        if (sig != -1) reader.unread(sig);
                    }
                } else {
                    campo.append((char) c);
                }
            } else if (c == COMILLA) {
                entreComillas = true;
            } else if (c == COMA) {
                fila.add(campo.toString());
                campo.setLength(0);
            } else if (c == LF || c == CR) {
                if (c == CR) {
                    int sig = reader.read();
                    if (sig != LF && sig != -1) reader.unread(sig);
                }
                fila.add(campo.toString());
                campo.setLength(0);
                filas.add(fila);
                fila = new ArrayList<>();
            } else {
                campo.append((char) c);
            }
        }
        if (entreComillas) throw new IllegalStateException("El CSV acaba dentro de unas comillas sin cerrar");
        if (campo.length() > 0 || !fila.isEmpty()) {
            fila.add(campo.toString());
            filas.add(fila);
        }
        return filas;
    }
}
