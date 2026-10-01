package com.gymprofit.api.service.busqueda;

import com.gymprofit.api.service.productooff.ProductoOffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// ============================================================
// IndiceAlimentos — lo que se busca, en memoria (GP-162, GP-164, DEC-040)
//
// Dos índices, cada uno inmutable y sustituido de golpe:
//   · Catálogo: los alimentos sin dueño y activos (básicos, catálogo de ADMIN y
//     productos ya materializados). Son unos pocos miles: se reconstruye entero, en el
//     momento, la primera vez que se busca después de un cambio.
//   · Productos: productos_off, cerca de 200 000. Se construye al arrancar, en segundo
//     plano, y después de una importación, cuando lleva un rato sin llegar ningún lote.
//     Mientras no está, la búsqueda sigue funcionando sin el grupo de productos.
// Lo de cada usuario no está aquí: se lee de la base en cada búsqueda (ver
// BusquedaAlimentosService), porque es poco y cambia a cada comida apuntada.
//
// Por qué en memoria y no en la base, con las medidas, en DEC-040.
// ============================================================
@Component
public class IndiceAlimentos {

    private static final Logger logger = LoggerFactory.getLogger(IndiceAlimentos.class);

    /** Un alimento del catálogo, con lo que hace falta para buscarlo y ordenarlo. */
    record DocCatalogo(int id, String nombre, String categoria, String fuente, String barcode,
                       List<String> terminosEs, List<String> terminosEn, int posicionHabitual) {
        boolean esProducto() {
            return "OFF".equals(fuente) || (fuente == null && barcode != null);
        }
    }

    /** Índice del catálogo: el texto (ES y EN juntos) y los documentos en el mismo orden. */
    record Catalogo(IndiceTexto texto, List<DocCatalogo> docs, Map<Long, Integer> idPorCodigo) {
    }

    /** Índice de productos_off: por documento, su id, escaneos, primer término y código. */
    record Productos(IndiceTexto texto, int[] ids, int[] escaneos, int[] primerTermino, short[] longitud,
                     long[] codigos, long[] codigosOrdenados) {
        boolean contieneCodigo(long codigo) {
            return codigo >= 0 && Arrays.binarySearch(codigosOrdenados, codigo) >= 0;
        }
    }

    private final JdbcTemplate jdbc;
    private final JdbcTemplate jdbcStreaming;
    private final List<String> habituales;
    private final long esperaTrasImportacionMs;
    private final ScheduledExecutorService programador = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread hilo = new Thread(r, "indice-productos");
        hilo.setDaemon(true);
        return hilo;
    });

    private volatile Catalogo catalogo;
    private volatile boolean catalogoSucio = true;
    private volatile Productos productos;
    private ScheduledFuture<?> reconstruccionPendiente;
    // Cuántas reconstrucciones de productos se han programado tras un cambio (para los tests).
    private final java.util.concurrent.atomic.AtomicLong programadas = new java.util.concurrent.atomic.AtomicLong();

    public IndiceAlimentos(JdbcTemplate jdbc, DataSource dataSource,
                           @Value("${app.busqueda.reconstruir-productos-tras-ms:30000}") long esperaTrasImportacionMs) {
        this.jdbc = jdbc;
        this.jdbcStreaming = new JdbcTemplate(dataSource);
        // Que el driver no traiga las 200 000 filas de golpe a memoria.
        this.jdbcStreaming.setFetchSize(1000);
        this.habituales = cargarHabituales();
        this.esperaTrasImportacionMs = esperaTrasImportacionMs;
    }

    // --- Catálogo -----------------------------------------------------------

    /** Aviso de que el catálogo de alimentos ha cambiado. */
    public record CatalogoCambiado() {
    }

    // En el momento (para que una búsqueda dentro de la misma transacción lo vea) y otra
    // vez al confirmar (para que no se quede la versión leída antes del commit).
    @EventListener
    void alCambiarCatalogo(CatalogoCambiado evento) {
        catalogoSucio = true;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alConfirmarCambioCatalogo(CatalogoCambiado evento) {
        catalogoSucio = true;
    }

    /** El índice del catálogo, reconstruido si ha cambiado desde la última vez. */
    Catalogo catalogo() {
        Catalogo actual = catalogo;
        if (actual != null && !catalogoSucio) return actual;
        synchronized (this) {
            if (catalogo == null || catalogoSucio) {
                catalogoSucio = false;
                catalogo = construirCatalogo();
            }
            return catalogo;
        }
    }

    private Catalogo construirCatalogo() {
        Map<String, Integer> posicionHabitual = new HashMap<>();
        for (int i = 0; i < habituales.size(); i++) posicionHabitual.put(habituales.get(i), i);

        List<DocCatalogo> docs = new ArrayList<>();
        IndiceTexto.Constructor constructor = new IndiceTexto.Constructor();
        Map<Long, Integer> idPorCodigo = new HashMap<>();
        jdbc.query("""
                SELECT id, nombre, nombre_en, categoria, fuente, codigo_origen, barcode, marca
                FROM alimentos WHERE usuario_id IS NULL AND activo = 1 ORDER BY id""", rs -> {
            String fuente = rs.getString("fuente");
            String origen = fuente + ";" + rs.getString("codigo_origen");
            List<String> es = Normalizador.terminos(rs.getString("nombre"));
            List<String> en = Normalizador.terminos(rs.getString("nombre_en"));
            List<String> todos = new ArrayList<>(es);
            todos.addAll(en);
            todos.addAll(Normalizador.terminos(rs.getString("marca")));
            DocCatalogo doc = new DocCatalogo(rs.getInt("id"), rs.getString("nombre"), rs.getString("categoria"),
                    fuente, rs.getString("barcode"), es, en, posicionHabitual.getOrDefault(origen, -1));
            constructor.anadir(todos);
            docs.add(doc);
            long codigo = codigoNumerico(doc.barcode());
            if (codigo >= 0) idPorCodigo.put(codigo, doc.id());
        });
        return new Catalogo(constructor.construir(), List.copyOf(docs), Map.copyOf(idPorCodigo));
    }

    // --- Productos ----------------------------------------------------------

    /** El índice de productos, o null si todavía no está construido. */
    Productos productos() {
        return productos;
    }

    @EventListener(ApplicationReadyEvent.class)
    void alArrancar() {
        programador.execute(this::reconstruirProductosRegistrando);
    }

    // Llega un evento por lote: se reconstruye cuando lleva un rato sin llegar ninguno.
    @EventListener
    synchronized void alCambiarProductos(ProductoOffService.ProductosCambiados evento) {
        if (reconstruccionPendiente != null) reconstruccionPendiente.cancel(false);
        programadas.incrementAndGet();
        reconstruccionPendiente = programador.schedule(this::reconstruirProductosRegistrando,
                esperaTrasImportacionMs, TimeUnit.MILLISECONDS);
    }

    /** Cuántas reconstrucciones del índice de productos se han programado desde el arranque. */
    long reconstruccionesProgramadas() {
        return programadas.get();
    }

    private void reconstruirProductosRegistrando() {
        try {
            reconstruirProductos();
        } catch (RuntimeException e) {
            // La búsqueda sigue sin el grupo de productos, o con el índice anterior: se
            // registra para verlo y se reintenta con la siguiente importación o arranque.
            logger.error("No se pudo construir el índice de productos", e);
        }
    }

    /**
     * Construye el índice de productos ahora, en el hilo que llama, y lo pone en uso. Lo
     * usan el arranque y la importación (en segundo plano) y los tests.
     */
    public void reconstruirProductos() {
        long inicio = System.nanoTime();
        IndiceTexto.Constructor constructor = new IndiceTexto.Constructor();
        IntLista ids = new IntLista();
        IntLista escaneos = new IntLista();
        List<String> primeros = new ArrayList<>();
        IntLista longitudes = new IntLista();
        List<Long> codigos = new ArrayList<>();
        // En orden de id: la posición en el índice desempata la búsqueda como lo haría el id.
        jdbcStreaming.query("SELECT id, codigo, nombre, marca, escaneos FROM productos_off ORDER BY id", rs -> {
            List<String> nombre = Normalizador.terminos(rs.getString("nombre"));
            List<String> todos = new ArrayList<>(nombre);
            todos.addAll(Normalizador.terminos(rs.getString("marca")));
            constructor.anadir(todos);
            ids.anadir(rs.getInt("id"));
            escaneos.anadir(rs.getInt("escaneos"));
            primeros.add(nombre.isEmpty() ? "" : nombre.get(0));
            longitudes.anadir(Math.min(Short.MAX_VALUE, rs.getString("nombre").length()));
            codigos.add(codigoNumerico(rs.getString("codigo")));
        });
        int n = ids.tamano();
        if (n > (1 << 20)) {
            // La clave de orden de la búsqueda guarda la posición en 20 bits: más de esto
            // desordenaría resultados. Se queda el índice anterior y se avisa (DEC-040).
            throw new IllegalStateException("Más de 1 048 576 productos: el índice de búsqueda no los admite");
        }
        IndiceTexto texto = constructor.construir();
        int[] primerTermino = new int[n];
        short[] longitud = new short[n];
        long[] codigo = new long[n];
        for (int i = 0; i < n; i++) {
            primerTermino[i] = texto.posicion(primeros.get(i));
            longitud[i] = (short) longitudes.get(i);
            codigo[i] = codigos.get(i);
        }
        long[] ordenados = Arrays.stream(codigo).filter(c -> c >= 0).sorted().toArray();
        productos = new Productos(texto, ids.aArray(), escaneos.aArray(), primerTermino, longitud, codigo, ordenados);
        // Render gratis no enseña la memoria: va aquí, en el log del arranque y de cada
        // importación, para saber cuánto margen queda (GP-168).
        java.lang.management.MemoryMXBean memoria = java.lang.management.ManagementFactory.getMemoryMXBean();
        java.lang.management.MemoryUsage heap = memoria.getHeapMemoryUsage();
        logger.info("Índice de productos: {} productos, {} términos distintos, {} ms · heap {} de {} MB, no-heap {} MB",
                n, texto.terminosDistintos(), (System.nanoTime() - inicio) / 1_000_000,
                heap.getUsed() / 1_048_576, heap.getMax() / 1_048_576,
                memoria.getNonHeapMemoryUsage().getUsed() / 1_048_576);
    }

    // --- Utilidades ---------------------------------------------------------

    /** Código de barras como número, para comparar sin cadenas; -1 si no es un número. */
    static long codigoNumerico(String codigo) {
        if (codigo == null || codigo.isEmpty() || codigo.length() > 18) return -1;
        for (int i = 0; i < codigo.length(); i++) {
            if (!Character.isDigit(codigo.charAt(i))) return -1;
        }
        return Long.parseLong(codigo);
    }

    /** Orden de los básicos habituales (fuente;código), para la búsqueda vacía. */
    List<String> habituales() {
        return habituales;
    }

    /** Ids de los habituales que están en el catálogo, en su orden. */
    List<Integer> idsHabituales() {
        DocCatalogo[] porPosicion = new DocCatalogo[habituales.size()];
        for (DocCatalogo doc : catalogo().docs()) {
            if (doc.posicionHabitual() >= 0) porPosicion[doc.posicionHabitual()] = doc;
        }
        List<Integer> ids = new ArrayList<>();
        Set<Integer> vistos = new HashSet<>();
        for (DocCatalogo doc : porPosicion) {
            if (doc != null && vistos.add(doc.id())) ids.add(doc.id());
        }
        return ids;
    }

    private static List<String> cargarHabituales() {
        List<String> lista = new ArrayList<>();
        try (InputStream in = IndiceAlimentos.class.getResourceAsStream("/busqueda/habituales.txt")) {
            if (in == null) throw new IllegalStateException("Falta busqueda/habituales.txt");
            BufferedReader lector = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = linea.strip();
                if (!linea.isEmpty() && !linea.startsWith("#")) lista.add(linea);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return List.copyOf(lista);
    }

    /** Lista de enteros sin cajas: con 200 000 productos, un Integer por número pesa. */
    static final class IntLista {
        private int[] datos = new int[1024];
        private int tamano;

        void anadir(int valor) {
            if (tamano == datos.length) datos = Arrays.copyOf(datos, tamano * 2);
            datos[tamano++] = valor;
        }

        int get(int i) {
            return datos[i];
        }

        int tamano() {
            return tamano;
        }

        int[] aArray() {
            return Arrays.copyOf(datos, tamano);
        }
    }
}
