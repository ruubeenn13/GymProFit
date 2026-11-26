package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// ============================================================
// WebDist — dónde leen los *WebTest la web (DEC-069; web/PLAN-UN-DIA.md §3).
//
// La web se construye con Astro: lo que se sirve es web/dist, no web/. Las páginas de antes
// viven tal cual en web/public y Astro las copia a dist; las nuevas salen de web/src. Los
// tests miran lo que se va a servir, así que leen dist, y fallan con un mensaje claro si no
// se ha construido: un dist viejo o ausente no puede dar un verde que no toca.
// ============================================================
final class WebDist {

    private WebDist() { }

    /** La raíz del repositorio: la primera carpeta, subiendo, que tiene web/. */
    static Path raiz() {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("web"))) raiz = raiz.getParent();
        assertTrue("no se encuentra la carpeta web/ subiendo desde el directorio de trabajo", raiz != null);
        return raiz;
    }

    /** web/: el código de la web (src, public, la documentación). */
    static Path web() {
        return raiz().resolve("web");
    }

    /** web/dist: lo que se sirve. Falla si no se ha construido. */
    static Path dist() {
        Path dist = web().resolve("dist");
        assertTrue("no existe web/dist: ejecuta «npm ci && npm run build» en web/ antes de los tests",
                Files.isRegularFile(dist.resolve("index.html")));
        return dist;
    }
}
