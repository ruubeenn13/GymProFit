package com.gymprofit.api.integration;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;

// ============================================================
// ServidorSentryFalso — hace de Sentry en los tests de GP-221: recibe lo que manda el SDK
// y lo guarda tal cual llega, para buscar en ello lo que no debería salir.
// Uno por JVM, en un puerto libre de 127.0.0.1.
// ============================================================
final class ServidorSentryFalso {

    private static final List<String> RECIBIDOS = new CopyOnWriteArrayList<>();
    private static final HttpServer SERVIDOR = arrancar();

    private ServidorSentryFalso() {
    }

    /** DSN que apunta a este servidor (clave pública inventada, proyecto 1). */
    static String dsn() {
        return "http://clave0publica0de0prueba@127.0.0.1:" + SERVIDOR.getAddress().getPort() + "/1";
    }

    /** Lo recibido desde el último {@link #vaciar()}. */
    static List<String> recibidos() {
        return List.copyOf(RECIBIDOS);
    }

    static void vaciar() {
        RECIBIDOS.clear();
    }

    private static HttpServer arrancar() {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            s.createContext("/", intercambio -> {
                try (InputStream cuerpo = intercambio.getRequestBody()) {
                    boolean gzip = "gzip".equalsIgnoreCase(
                            intercambio.getRequestHeaders().getFirst("Content-Encoding"));
                    InputStream leido = gzip ? new GZIPInputStream(cuerpo) : cuerpo;
                    RECIBIDOS.add(new String(leido.readAllBytes(), StandardCharsets.UTF_8));
                }
                byte[] respuesta = "{}".getBytes(StandardCharsets.UTF_8);
                intercambio.sendResponseHeaders(200, respuesta.length);
                intercambio.getResponseBody().write(respuesta);
                intercambio.close();
            });
            s.setExecutor(null);
            s.start();
            return s;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
