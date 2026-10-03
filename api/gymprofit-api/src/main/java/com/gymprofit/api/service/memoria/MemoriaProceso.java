package com.gymprofit.api.service.memoria;

import java.io.IOException;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

// ============================================================
// MemoriaProceso — cuánta memoria gasta la API, como la ve Render (GP-187, lote 1.6.5)
//
// Render mata el contenedor cuando su cgroup pasa del límite (512 MB en el plan
// gratis), sin OutOfMemoryError ni nada en el log, y su panel gratis no enseña la
// memoria. Esto lee lo mismo que mira el que mata:
//   · El cgroup, v2 (memory.current, memory.max) o v1 (memory.usage_in_bytes,
//     memory.limit_in_bytes), y de memory.stat cuánto es memoria anónima (la del
//     proceso: heap, metaspace, pilas…) y cuánto caché de ficheros (que el núcleo
//     puede soltar antes de matar).
//   · De la JVM: heap usado y reservado, metaspace, code cache, memoria directa y hilos.
// Sin cgroup (en local, en Windows) solo la parte de la JVM.
// La raíz de los ficheros se puede cambiar para probarlo con ficheros de mentira.
// ============================================================
public final class MemoriaProceso {

    private static final long MB = 1024L * 1024L;

    /** Lo que se ha leído del cgroup; vacío si no hay. */
    public record Contenedor(long uso, long limite, long anonima, long cache, String version) {

        /** Uso frente al límite, de 0 a 100; -1 si no hay límite. */
        public int porcentaje() {
            return limite > 0 ? (int) Math.round(uso * 100.0 / limite) : -1;
        }
    }

    /** Lo de la JVM, en bytes; los hilos, en número. */
    public record Jvm(long heapUsado, long heapReservado, long heapMaximo, long metaspace,
                      long codeCache, long directa, int hilos) {
    }

    /** Una lectura entera. */
    public record Lectura(Optional<Contenedor> contenedor, Jvm jvm) {

        /** La línea de log: «contenedor 402 de 512 MB (79 %; anónima 350, caché 40) · heap …». */
        public String linea() {
            String c = contenedor.map(x -> x.limite() > 0
                            ? String.format(Locale.ROOT, "contenedor %d de %d MB (%d %%; anónima %d, caché %d, cgroup %s)",
                            x.uso() / MB, x.limite() / MB, x.porcentaje(), x.anonima() / MB, x.cache() / MB, x.version())
                            : String.format(Locale.ROOT, "contenedor %d MB sin límite (anónima %d, caché %d, cgroup %s)",
                            x.uso() / MB, x.anonima() / MB, x.cache() / MB, x.version()))
                    .orElse("sin cgroup");
            return String.format(Locale.ROOT,
                    "Memoria: %s · heap %d usado / %d reservado (máx. %d) · metaspace %d · code cache %d · directa %d MB · %d hilos",
                    c, jvm.heapUsado() / MB, jvm.heapReservado() / MB, jvm.heapMaximo() / MB, jvm.metaspace() / MB,
                    jvm.codeCache() / MB, jvm.directa() / MB, jvm.hilos());
        }
    }

    private final Path raiz;

    /** Lee el cgroup de verdad, en /sys/fs/cgroup. */
    public MemoriaProceso() {
        this(Path.of("/sys/fs/cgroup"));
    }

    /** Lee el cgroup que cuelga de {@code raiz} (para los tests). */
    public MemoriaProceso(Path raiz) {
        this.raiz = raiz;
    }

    /** Lee todo ahora. Nunca lanza: lo que no se pueda leer, no sale. */
    public Lectura leer() {
        return new Lectura(contenedor(), jvm());
    }

    /** El cgroup v2 o v1, o vacío si no hay ninguno. */
    public Optional<Contenedor> contenedor() {
        try {
            Path v2 = raiz.resolve("memory.current");
            if (Files.isReadable(v2)) {
                long uso = numero(v2);
                long limite = limite(raiz.resolve("memory.max"));
                String stat = leerTexto(raiz.resolve("memory.stat"));
                return Optional.of(new Contenedor(uso, limite, campo(stat, "anon"), campo(stat, "file"), "v2"));
            }
            Path v1 = raiz.resolve("memory");
            Path usoV1 = v1.resolve("memory.usage_in_bytes");
            if (Files.isReadable(usoV1)) {
                long uso = numero(usoV1);
                long limite = limite(v1.resolve("memory.limit_in_bytes"));
                String stat = leerTexto(v1.resolve("memory.stat"));
                long anonima = campo(stat, "total_rss") > 0 ? campo(stat, "total_rss") : campo(stat, "rss");
                long cache = campo(stat, "total_cache") > 0 ? campo(stat, "total_cache") : campo(stat, "cache");
                return Optional.of(new Contenedor(uso, limite, anonima, cache, "v1"));
            }
        } catch (IOException | RuntimeException e) {
            // Un fichero que cambia de formato no tumba nada: sin cgroup, y se sigue.
        }
        return Optional.empty();
    }

    /** Lo de la JVM en este momento. */
    public static Jvm jvm() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long metaspace = 0;
        long codeCache = 0;
        for (MemoryPoolMXBean p : ManagementFactory.getMemoryPoolMXBeans()) {
            long usado = p.getUsage() != null ? p.getUsage().getUsed() : 0;
            if ("Metaspace".equals(p.getName())) metaspace = usado;
            else if (p.getName().startsWith("CodeHeap") || "Code Cache".equals(p.getName())) codeCache += usado;
        }
        long directa = 0;
        for (BufferPoolMXBean b : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
            if ("direct".equals(b.getName())) directa = b.getMemoryUsed();
        }
        return new Jvm(heap.getUsed(), heap.getCommitted(), heap.getMax(), metaspace, codeCache, directa,
                ManagementFactory.getThreadMXBean().getThreadCount());
    }

    // «max» o un número muy grande (v1 sin límite) es que no hay límite: -1.
    private static long limite(Path p) throws IOException {
        if (!Files.isReadable(p)) return -1;
        String t = leerTexto(p).trim();
        if (t.isEmpty() || "max".equals(t)) return -1;
        long n = Long.parseLong(t);
        return n >= Long.MAX_VALUE / 2 ? -1 : n;
    }

    private static long numero(Path p) throws IOException {
        return Long.parseLong(leerTexto(p).trim());
    }

    private static String leerTexto(Path p) throws IOException {
        return Files.isReadable(p) ? Files.readString(p) : "";
    }

    // El valor de «clave valor» en memory.stat; 0 si no está.
    static long campo(String stat, String clave) {
        for (String linea : stat.split("\n")) {
            String[] partes = linea.trim().split("\\s+");
            if (partes.length == 2 && partes[0].equals(clave)) return Long.parseLong(partes[1]);
        }
        return 0;
    }
}
