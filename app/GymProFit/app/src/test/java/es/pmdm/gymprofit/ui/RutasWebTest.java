package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

// ============================================================
// RutasWebTest — ninguna URL de gymprofit.app cambia con el build de Astro (DEC-069,
// propuesta; web/PLAN-UN-DIA.md §6).
//
// Pages sirve /privacidad desde privacidad.html. Si una página saliera como
// privacidad/index.html (el formato por defecto de Astro), Pages redirigiría a
// /privacidad/: cambiaría una URL que Google Play tiene declarada (/eliminar-cuenta) y la
// que llevan los correos de la beta (/beta/entrar). Este test lo mira sobre dist, que es
// lo que se sube: cada ruta es un .html y ninguna es una carpeta con index.html.
// ============================================================
public class RutasWebTest {

    /** Las URL públicas (sin la barra inicial) y la prueba de «Un día», que vive aparte. */
    private static final String[] RUTAS = {
            "index", "privacidad", "terminos", "eliminar-cuenta",
            "beta", "beta/revisa", "beta/dentro", "beta/entrar", "un-dia",
    };

    @Test
    public void cadaRutaEsUnFicheroHtml() {
        Path dist = WebDist.dist();
        for (String ruta : RUTAS) {
            assertTrue("falta dist/" + ruta + ".html", Files.isRegularFile(dist.resolve(ruta + ".html")));
            if (!ruta.equals("index")) {
                assertFalse("dist/" + ruta + "/index.html: Pages redirigiría /" + ruta + " a /" + ruta + "/",
                        Files.exists(dist.resolve(ruta).resolve("index.html")));
            }
        }
    }

    /**
     * Las páginas que ya construye Astro (fase 4): salen de src/pages y no queda en public/ un
     * .html con su nombre, que Astro copiaría encima o al lado y la dejaría servir la de antes.
     */
    private static final String[] DE_ASTRO = {
            "privacidad", "terminos", "eliminar-cuenta",
    };

    @Test
    public void lasPaginasSecundariasLasConstruyeAstro() {
        Path web = WebDist.web();
        for (String ruta : DE_ASTRO) {
            assertTrue("falta web/src/pages/" + ruta + ".astro",
                    Files.isRegularFile(web.resolve("src/pages/" + ruta + ".astro")));
            assertFalse("web/public/" + ruta + ".html sigue ahí: se serviría la página de antes",
                    Files.exists(web.resolve("public/" + ruta + ".html")));
            assertTrue("falta dist/" + ruta + ".html", Files.isRegularFile(WebDist.dist().resolve(ruta + ".html")));
        }
    }

    @Test
    public void laPaginaNoEncontradaEstaEnLaRaiz() {
        assertTrue("falta dist/404.html: Pages respondería 200 con la portada",
                Files.isRegularFile(WebDist.dist().resolve("404.html")));
    }
}
