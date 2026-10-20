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
// BetaWebTest — el formulario de /beta envía a Brevo y la política cuenta ese
// tratamiento (GP-201).
//
// Las altas de la lista de espera las recoge Brevo con doble confirmación, pero el
// formulario es nuestro y se envía por POST normal, sin JavaScript (DEC-050): si la
// action o el nombre de un campo cambian sin querer, la gente que llega desde TikTok
// se apunta a ninguna parte, y Brevo no avisa. La trampa para bots tiene que llegar
// vacía, así que nadie debe poder verla ni rellenarla. La web no carga nada de
// terceros ni lleva JavaScript, así que la beta tampoco. Y la política tiene que
// nombrar el tratamiento con su base legal, su plazo y la baja.
// ============================================================
public class BetaWebTest {

    /** El endpoint del formulario de Brevo, tal cual lo da su panel. */
    private static final String FORMULARIO_BREVO = "https://c66652ae.sibforms.com/serve/"
            + "MUIFAORtSwe7_YQbPHZ1hfBdigRHNkNWnE7eaZati3CifuqQ3yIKNj2HQV6oOvbjfyUYusgHznzE_WWAA1fhZNqg84lw5KkyrVJxhLaYEyL0XUYchj8wmMdvuu6lNVZIDrylva7nkhY_bEC7VLwLrLIgS7kAabyxan0UmamnG80VnAInVeNAjUYo1BkWePjfwN9ptal3PQSa2H17mA==";

    private static final String[] PAGINAS = {"beta.html", "beta/revisa.html", "beta/dentro.html"};

    @Test
    public void elFormularioEnviaABrevoPorPost() throws IOException {
        String form = formulario();
        String apertura = form.substring(0, form.indexOf('>') + 1);
        assertTrue("el formulario no es POST: " + apertura, apertura.contains("method=\"post\""));
        assertTrue("el formulario no envía a Brevo: " + apertura,
                apertura.contains("action=\"" + FORMULARIO_BREVO + "\""));
        assertFalse("el formulario abre otra pestaña", apertura.contains("target="));
    }

    @Test
    public void losCamposSonLosQueEsperaBrevo() throws IOException {
        String form = formulario();
        String correo = campo(form, "EMAIL");
        assertTrue("EMAIL no es type=email: " + correo, correo.contains("type=\"email\""));
        assertTrue("EMAIL no es obligatorio", correo.contains(" required"));
        String casilla = campo(form, "OPT_IN");
        assertTrue("OPT_IN no es una casilla: " + casilla, casilla.contains("type=\"checkbox\""));
        assertTrue("OPT_IN no vale 1", casilla.contains("value=\"1\""));
        assertTrue("OPT_IN no es obligatoria", casilla.contains(" required"));
        assertFalse("OPT_IN viene marcada", casilla.contains("checked"));
        assertTrue(campo(form, "locale").contains("type=\"hidden\" name=\"locale\" value=\"es\""));
        assertTrue(campo(form, "html_type").contains("type=\"hidden\" name=\"html_type\" value=\"simple\""));
        assertFalse("ya no se pregunta el móvil", form.contains("name=\"MOVIL\""));
    }

    @Test
    public void elTextoDelConsentimientoEsElAprobado() throws IOException {
        String texto = texto(formulario());
        assertTrue("falta el texto del consentimiento", texto.contains("Quiero que GymProFit me avise por correo "
                + "cuando abra la beta y cuando se publique la app. Puedo darme de baja cuando quiera."));
        assertTrue("el consentimiento no enlaza la política",
                Pattern.compile("<label class=\"consentimiento\">.*?href=\"/privacidad(#beta)?\".*?</label>", Pattern.DOTALL)
                        .matcher(formulario()).find());
    }

    @Test
    public void laTrampaParaBotsNoLaVeNadie() throws IOException {
        String form = formulario();
        String trampa = campo(form, "email_address_check");
        assertTrue("la trampa no va vacía", trampa.contains("value=\"\""));
        assertTrue("la trampa se alcanza con el tabulador", trampa.contains("tabindex=\"-1\""));
        assertTrue("el navegador puede autorrellenar la trampa", trampa.contains("autocomplete=\"off\""));
        // Su contenedor, oculto también para los lectores de pantalla
        Matcher caja = Pattern.compile("<div class=\"trampa\" aria-hidden=\"true\">\\s*<input[^>]*name=\"email_address_check\"")
                .matcher(form);
        assertTrue("la trampa no está dentro de un contenedor aria-hidden", caja.find());
        assertTrue("la trampa no se oculta en la hoja", pagina("beta.css").contains(".trampa {"));
    }

    @Test
    public void avisaDeQueLaBetaEsSoloParaAndroid() throws IOException {
        String texto = texto(pagina("beta.html"));
        assertTrue("falta el aviso de Android", texto.contains("La beta es solo para Android"));
        assertTrue(texto.contains("Google Play"));
        assertFalse("se sigue invitando al iPhone", texto.contains("Apúntate igual"));
        assertFalse("se sigue diciendo que el formulario es de Brevo", texto.contains("formulario de Brevo"));
    }

    @Test
    public void laBetaNoLlevaJavaScriptNiCargaNadaDeFuera() throws IOException {
        for (String nombre : PAGINAS) {
            String html = pagina(nombre);
            assertFalse(nombre + " lleva JavaScript", html.contains("<script"));
            assertFalse(nombre + " lleva manejadores de eventos", Pattern.compile("\\son[a-z]+=").matcher(html).find());
            // Lo que se pide al cargar: src, srcset y los <link>. Los <a> y la action no cuentan.
            Matcher carga = Pattern.compile("(?:src|srcset)=\"([^\"]*)\"|<link\\b[^>]*href=\"([^\"]*)\"").matcher(html);
            while (carga.find()) {
                String url = carga.group(1) != null ? carga.group(1) : carga.group(2);
                assertTrue(nombre + " carga algo de fuera: " + url, url.startsWith("/"));
            }
        }
    }

    @Test
    public void revisaPideConfirmarYMirarEnSpam() throws IOException {
        String texto = texto(pagina("beta/revisa.html"));
        assertTrue(texto.contains("Revisa tu correo"));
        assertTrue("no dice que se confirma desde el correo", texto.contains("confirma"));
        assertTrue("no dice que mire en spam", texto.contains("spam"));
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
        for (String debe : new String[]{"correo", "art. 6.1.a", "6 meses", "baja", "privacidad@gymprofit.app"}) {
            assertTrue("el tratamiento no dice «" + debe + "»", lista.contains(debe));
        }
        assertFalse("la política sigue pidiendo el tipo de móvil", texto.contains("tipo de móvil"));
        // La fila de Brevo en la tabla de proveedores, no cualquier mención.
        Matcher brevo = Pattern.compile("<tr><td>Brevo</td>(.*?)</tr>", Pattern.DOTALL).matcher(html);
        assertTrue("Brevo no está en la tabla de proveedores", brevo.find());
        assertTrue("Brevo no figura para la lista de espera", brevo.group(1).contains("lista de espera"));
        assertTrue("no dice que Brevo recibe los datos de conexión", brevo.group(1).contains("datos de conexión"));
        assertEquals("la fecha de la política no es la de la lista de espera",
                1, ocurrencias(texto, "Última actualización: 20 de octubre de 2026"));
    }

    // El único <form> de /beta.
    private static String formulario() throws IOException {
        String html = pagina("beta.html");
        assertEquals("/beta tiene que tener un formulario y solo uno", 1, ocurrencias(html, "<form"));
        Matcher m = Pattern.compile("<form\\b.*?</form>", Pattern.DOTALL).matcher(html);
        assertTrue(m.find());
        return m.group();
    }

    // La etiqueta <input> con ese name.
    private static String campo(String form, String nombre) {
        Matcher m = Pattern.compile("<input\\b[^>]*name=\"" + nombre + "\"[^>]*>").matcher(form);
        assertTrue("falta el campo " + nombre, m.find());
        return m.group();
    }

    private static int ocurrencias(String texto, String buscado) {
        return texto.split(Pattern.quote(buscado), -1).length - 1;
    }

    // El texto de la página sin etiquetas ni saltos de más.
    private static String texto(String html) {
        return html.replaceAll("<[^>]+>", " ").replace("&amp;", "&").replaceAll("\\s+", " ")
                .replaceAll(" ([.,:;])", "$1");
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
