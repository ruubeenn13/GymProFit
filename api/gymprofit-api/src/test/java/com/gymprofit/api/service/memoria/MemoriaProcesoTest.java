package com.gymprofit.api.service.memoria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// MemoriaProcesoTest — la línea de memoria (GP-187)
// Con ficheros de cgroup de mentira, v2 y v1, y sin ninguno.
// ============================================================
class MemoriaProcesoTest {

    private static final long MB = 1024L * 1024L;

    @TempDir
    Path raiz;

    @Test
    @DisplayName("cgroup v2: uso, límite, anónima y caché de memory.stat")
    void v2() throws IOException {
        Files.writeString(raiz.resolve("memory.current"), (402 * MB) + "\n");
        Files.writeString(raiz.resolve("memory.max"), (512 * MB) + "\n");
        Files.writeString(raiz.resolve("memory.stat"), "anon " + (350 * MB) + "\nfile " + (40 * MB) + "\nkernel 123\n");

        MemoriaProceso.Contenedor c = new MemoriaProceso(raiz).contenedor().orElseThrow();
        assertThat(c.version()).isEqualTo("v2");
        assertThat(c.uso()).isEqualTo(402 * MB);
        assertThat(c.limite()).isEqualTo(512 * MB);
        assertThat(c.anonima()).isEqualTo(350 * MB);
        assertThat(c.cache()).isEqualTo(40 * MB);
        assertThat(c.porcentaje()).isEqualTo(79);
        assertThat(new MemoriaProceso(raiz).leer().linea())
                .startsWith("Memoria: contenedor 402 de 512 MB (79 %; anónima 350, caché 40, cgroup v2) · heap ")
                .contains("metaspace").contains("code cache").contains("directa").endsWith("hilos");
    }

    @Test
    @DisplayName("cgroup v2 sin límite («max»)")
    void v2_sin_limite() throws IOException {
        Files.writeString(raiz.resolve("memory.current"), "1048576\n");
        Files.writeString(raiz.resolve("memory.max"), "max\n");
        MemoriaProceso.Contenedor c = new MemoriaProceso(raiz).contenedor().orElseThrow();
        assertThat(c.limite()).isEqualTo(-1);
        assertThat(c.porcentaje()).isEqualTo(-1);
        assertThat(new MemoriaProceso(raiz).leer().linea()).contains("contenedor 1 MB sin límite");
    }

    @Test
    @DisplayName("cgroup v1: usage_in_bytes, limit_in_bytes y total_rss/total_cache")
    void v1() throws IOException {
        Path m = Files.createDirectory(raiz.resolve("memory"));
        Files.writeString(m.resolve("memory.usage_in_bytes"), String.valueOf(300 * MB));
        Files.writeString(m.resolve("memory.limit_in_bytes"), String.valueOf(512 * MB));
        Files.writeString(m.resolve("memory.stat"), "cache 1\nrss 2\ntotal_cache " + (20 * MB) + "\ntotal_rss " + (250 * MB) + "\n");
        MemoriaProceso.Contenedor c = new MemoriaProceso(raiz).contenedor().orElseThrow();
        assertThat(c.version()).isEqualTo("v1");
        assertThat(c.anonima()).isEqualTo(250 * MB);
        assertThat(c.cache()).isEqualTo(20 * MB);
        assertThat(c.porcentaje()).isEqualTo(59);
    }

    @Test
    @DisplayName("cgroup v1 sin límite: el número enorme de v1 no es un límite")
    void v1_sin_limite() throws IOException {
        Path m = Files.createDirectory(raiz.resolve("memory"));
        Files.writeString(m.resolve("memory.usage_in_bytes"), String.valueOf(300 * MB));
        Files.writeString(m.resolve("memory.limit_in_bytes"), "9223372036854771712");
        assertThat(new MemoriaProceso(raiz).contenedor().orElseThrow().limite()).isEqualTo(-1);
    }

    @Test
    @DisplayName("sin cgroup (en local): solo la JVM, y no falla")
    void sin_cgroup() {
        MemoriaProceso.Lectura l = new MemoriaProceso(raiz).leer();
        assertThat(l.contenedor()).isEmpty();
        assertThat(l.linea()).startsWith("Memoria: sin cgroup · heap ");
        assertThat(l.jvm().heapUsado()).isPositive();
        assertThat(l.jvm().metaspace()).isPositive();
        assertThat(l.jvm().hilos()).isPositive();
    }

    @Test
    @DisplayName("un fichero estropeado no tumba nada: sin cgroup")
    void estropeado() throws IOException {
        Files.writeString(raiz.resolve("memory.current"), "no es un número");
        assertThat(new MemoriaProceso(raiz).contenedor()).isEmpty();
    }

    @Test
    @DisplayName("el WARN sale al pasar del 90 %, una vez, y se rearma al bajar del 85 %")
    void aviso() throws IOException {
        Files.writeString(raiz.resolve("memory.max"), String.valueOf(100 * MB));
        VigilanteMemoria v = new VigilanteMemoria(new MemoriaProceso(raiz));
        uso(80);
        assertThat(v.comprobar()).isFalse();
        uso(91);
        assertThat(v.comprobar()).isTrue();
        uso(95);
        assertThat(v.comprobar()).isFalse();
        uso(88);
        assertThat(v.comprobar()).isFalse();
        uso(92);
        assertThat(v.comprobar()).isFalse();
        uso(84);
        assertThat(v.comprobar()).isFalse();
        uso(93);
        assertThat(v.comprobar()).isTrue();
    }

    @Test
    @DisplayName("sin cgroup, el vigilante no avisa ni falla")
    void aviso_sin_cgroup() {
        assertThat(new VigilanteMemoria(new MemoriaProceso(raiz)).comprobar()).isFalse();
    }

    private void uso(int mb) throws IOException {
        Files.writeString(raiz.resolve("memory.current"), String.valueOf(mb * MB));
    }

    @Test
    @DisplayName("el code cache cuenta sin segmentar (C1, como en producción) y segmentado (C2)")
    void code_cache_segmentado_o_no() {
        assertThat(MemoriaProceso.esCodeCache("CodeCache")).isTrue();
        assertThat(MemoriaProceso.esCodeCache("CodeHeap 'non-nmethods'")).isTrue();
        assertThat(MemoriaProceso.esCodeCache("CodeHeap 'profiled nmethods'")).isTrue();
        assertThat(MemoriaProceso.esCodeCache("CodeHeap 'non-profiled nmethods'")).isTrue();
        assertThat(MemoriaProceso.esCodeCache("Metaspace")).isFalse();
        assertThat(MemoriaProceso.esCodeCache("Compressed Class Space")).isFalse();
    }
}
