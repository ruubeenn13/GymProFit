package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// BetaWebTest — la página de la beta lleva al formulario de Brevo y la política
// cuenta ese tratamiento (GP-201).
//
// Las altas de la lista de espera las recoge un formulario alojado de Brevo con doble
// confirmación (DEC-050): si el enlace de /beta cambia sin querer, la gente que llega
// desde TikTok se apunta a ninguna parte. Y la web no carga nada de terceros ni lleva
// JavaScript, así que la beta tampoco. La política tiene que nombrar el tratamiento
// nuevo con su base legal, su plazo y la baja; si alguien la reescribe y se lo deja,
// esto lo dice.
// ============================================================
public class BetaWebTest {

    /** El formulario alojado de Brevo, tal cual lo da su panel. */
    private static final String FORMULARIO_BREVO = "https://c66652ae.sibforms.com/serve/"
            + "MUIFAORtSwe7_YQbPHZ1hfBdigRHNkNWnE7eaZati3CifuqQ3yIKNj2HQV6oOvbjfyUYusgHznzE_WWAA1fhZNqg84lw5KkyrVJxhLaYEyL0XUYchj8wmMdvuu6lNVZIDrylva7nkhY_bEC7VLwLrLIgS7kAabyxan0UmamnG80VnAInVeNAjUYo1BkWePjfwN9ptal3PQSa2H17mA==";

    @Test
    public void elBotonDeLaBetaLlevaAlFormularioDeBrevoEnLaMismaPestana() throws IOException {
        String html = pagina("beta.html");
        Matcher boton = Pattern.compile("<a\\b[^>]*class=\"[^\"]*boton[^\"]*\"[^>]*>").matcher(html);
        assertTrue("/beta no tiene botón", boton.find());
        String etiqueta = boton.group();
        assertTrue("el botón no lleva al formulario de Brevo: " + etiqueta,
                etiqueta.contains("href=\"" + FORMULARIO_BREVO + "\""));
        assertFalse("el botón abre otra pestaña", etiqueta.contains("target="));
    }

    @Test
    public void laBetaNoLlevaJavaScriptNiCargaNadaDeFuera() throws IOException {
        for (String nombre : new String[]{"beta.html", "beta/dentro.html"}) {
            String html = pagina(nombre);
            assertFalse(nombre + " lleva JavaScript", html.contains("<script"));
            // Lo que se pide al cargar: src, srcset y los <link>. Los <a> no cuentan.
            Matcher carga = Pattern.compile("(?:src|srcset)=\"([^\"]*)\"|<link\\b[^>]*href=\"([^\"]*)\"").matcher(html);
            while (carga.find()) {
                String url = carga.group(1) != null ? carga.group(1) : carga.group(2);
                assertTrue(nombre + " carga algo de fuera: " + url, url.startsWith("/"));
            }
        }
    }

    @Test
    public void dentroEnlazaATikTokEInstagram() throws IOException {
        String html = pagina("beta/dentro.html");
        assertTrue(html.contains("href=\"https://www.tiktok.com/@gymprofit\""));
        assertTrue(html.contains("href=\"https://www.instagram.com/gymprofit\""));
    }

    @Test
    public void laPortadaInvitaALaBeta() throws IOException {
        assertTrue(pagina("index.html").contains("href=\"/beta\""));
    }

    @Test
    public void laPoliticaCuentaLaListaDeEspera() throws IOException {
        String html = pagina("privacidad.html");
        String texto = texto(html);
        Matcher seccion = Pattern.compile("Lista de espera de la beta(.*?)Datos que se generan").matcher(texto);
        assertTrue("falta el tratamiento «Lista de espera de la beta»", seccion.find());
        String lista = seccion.group(1);
        for (String debe : new String[]{"correo", "tipo de móvil", "art. 6.1.a", "6 meses", "baja",
                "privacidad@gymprofit.app"}) {
            assertTrue("el tratamiento no dice «" + debe + "»", lista.contains(debe));
        }
        // La fila de Brevo en la tabla de proveedores, no cualquier mención.
        Matcher brevo = Pattern.compile("<tr><td>Brevo</td>(.*?)</tr>", Pattern.DOTALL).matcher(html);
        assertTrue("Brevo no está en la tabla de proveedores", brevo.find());
        assertTrue("Brevo no figura para la lista de espera", brevo.group(1).contains("lista de espera"));
        assertTrue("no dice que Brevo recibe los datos de conexión", brevo.group(1).contains("datos de conexión"));
        assertEquals("la fecha de la política no es la de la lista de espera",
                1, ocurrencias(texto, "Última actualización: 20 de octubre de 2026"));
    }

    private static int ocurrencias(String texto, String buscado) {
        return texto.split(Pattern.quote(buscado), -1).length - 1;
    }

    // El texto de la página sin etiquetas ni saltos de más.
    private static String texto(String html) {
        return html.replaceAll("<[^>]+>", " ").replace("&amp;", "&").replaceAll("\\s+", " ");
    }

    private static String pagina(String nombre) throws IOException {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("web"))) raiz = raiz.getParent();
        assertNotNull("no se encuentra la carpeta web", raiz);
        Path fichero = raiz.resolve("web").resolve(nombre);
        assertTrue("no existe web/" + nombre, Files.exists(fichero));
        return new String(Files.readAllBytes(fichero), StandardCharsets.UTF_8);
    }
}
