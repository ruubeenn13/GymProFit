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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
//
// /beta/entrar es el enlace de los correos de «beta abierta» (GP-202) y es una página
// de tres pasos: unirse al Grupo de Google de probadores, aceptar la prueba en Play e
// instalar (DEC-052). Abrir la beta es cambiar un solo bloque marcado: hasta entonces
// el enlace de Play espera comentado dentro de él, y fuera de ese bloque no puede estar
// en ninguna parte de la web, o la beta queda abierta antes de tiempo. El del grupo,
// solo en /beta/dentro y /beta/entrar.
// ============================================================
public class BetaWebTest {

    /** El endpoint del formulario de Brevo, tal cual lo da su panel. */
    private static final String FORMULARIO_BREVO = "https://c66652ae.sibforms.com/serve/"
            + "MUIFAORtSwe7_YQbPHZ1hfBdigRHNkNWnE7eaZati3CifuqQ3yIKNj2HQV6oOvbjfyUYusgHznzE_WWAA1fhZNqg84lw5KkyrVJxhLaYEyL0XUYchj8wmMdvuu6lNVZIDrylva7nkhY_bEC7VLwLrLIgS7kAabyxan0UmamnG80VnAInVeNAjUYo1BkWePjfwN9ptal3PQSa2H17mA==";

    private static final String[] PAGINAS = {"beta.html", "beta/revisa.html", "beta/dentro.html", "beta/entrar.html"};

    /** El principio de cualquier enlace de unión a una prueba cerrada de Google Play. */
    private static final String PRUEBA_CERRADA = "play.google.com/apps/testing";

    /** El Grupo de Google que hace de lista de probadores (DEC-052). */
    private static final String GRUPO = "groups.google.com/g/gymprofit-beta";

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
        assertTrue("falta el texto del consentimiento", texto.contains(
                "Avísame por correo de la beta y del lanzamiento. Me doy de baja cuando quiera."));
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
    public void diceQueEsSoloAndroidYNombraElIphoneUnaVez() throws IOException {
        String texto = texto(pagina("beta.html"));
        assertTrue("falta la línea de debajo del titular",
                texto.contains("Entreno, dieta y progreso en una sola app. Solo Android."));
        assertEquals("el iPhone se nombra más de una vez", 1, ocurrencias(texto, "iPhone"));
        Matcher iphone = Pattern.compile("[^.]*iPhone[^.]*\\.").matcher(texto);
        assertTrue(iphone.find());
        assertTrue("la línea del iPhone no manda a seguir a @gymprofit", iphone.group().contains("@gymprofit"));
        assertTrue("no dice que la beta se instala desde Google Play", texto.contains("Google Play"));
        assertFalse("se sigue diciendo que el formulario es de Brevo", texto.contains("formulario de Brevo"));
    }

    @Test
    public void nadaSeRepite() throws IOException {
        String html = pagina("beta.html");
        assertFalse("vuelve la pastilla de Android", html.contains("pastilla"));
        assertFalse("vuelve la caja de Android", html.contains("class=\"aviso\""));
        assertFalse("vuelve el «¿Te apuntas?» del final", html.contains("¿Te apuntas?"));
        assertEquals("«Apuntarme» sale más de una vez", 1, ocurrencias(texto(html), "Apuntarme"));
        // Ninguna frase dos veces en la página
        Set<String> vistas = new HashSet<>();
        for (String frase : texto(html.replaceAll("(?s)<head>.*?</head>", "")).split("(?<=[.?!:])\\s+")) {
            String f = frase.trim();
            if (f.length() < 12) continue;
            assertTrue("frase repetida: «" + f + "»", vistas.add(f));
        }
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
    public void dentroPideUnirseYaAlGrupo() throws IOException {
        String html = pagina("beta/dentro.html");
        Matcher destacado = Pattern.compile("<div class=\"destacado\">(.*?)</div>", Pattern.DOTALL).matcher(html);
        assertTrue("/beta/dentro no tiene el paso destacado", destacado.find());
        String paso = destacado.group(1);
        assertTrue("el paso no se llama como se aprobó", texto(paso).contains("Únete ya al grupo de probadores"));
        assertTrue("el paso no enlaza al grupo", paso.contains("href=\"https://" + GRUPO + "\""));
        assertTrue("no dice que hace falta una cuenta de Google", texto(paso).contains("cuenta de Google"));
        assertTrue("no dice que así el día de apertura solo hay que aceptar",
                texto(paso).contains("el día de apertura solo tendrás que aceptar la prueba"));
    }

    @Test
    public void entrarSonTresPasosYNoSeIndexa() throws IOException {
        String html = pagina("beta/entrar.html");
        assertTrue("/beta/entrar se indexa", html.contains("<meta name=\"robots\" content=\"noindex\">"));
        // Los pasos de verdad, sin contar el que espera comentado dentro del bloque de apertura
        String visible = sinComentarios(html);
        assertEquals("/beta/entrar no tiene tres pasos", 3, ocurrencias(visible, "<li class=\"paso"));
        String[] pasos = visible.split("<li class=\"paso");
        assertTrue("el primer paso no enlaza al grupo", pasos[1].contains("href=\"https://" + GRUPO + "\""));
        assertTrue("el segundo paso no es aceptar la prueba", texto(pasos[2]).contains("Acepta la prueba en Google Play"));
        assertTrue("el tercer paso no es instalar", texto(pasos[3]).contains("Instala GymProFit"));
        assertTrue("no tiene el botón de vuelta a /beta",
                html.contains("<a class=\"boton hueco\" href=\"/beta\">Volver a la beta</a>"));
    }

    @Test
    public void abrirLaBetaEsCambiarUnSoloBloque() throws IOException {
        String html = pagina("beta/entrar.html");
        assertEquals("el bloque de apertura tiene que estar una vez", 1, ocurrencias(html, "▼ APERTURA DE LA BETA"));
        assertEquals("el bloque de apertura no se cierra una vez", 1, ocurrencias(html, "▲ FIN DE LA APERTURA DE LA BETA"));
        String bloque = html.substring(html.indexOf("▼ APERTURA DE LA BETA"), html.indexOf("▲ FIN DE LA APERTURA DE LA BETA"));
        String fuera = html.replace(bloque, "");
        assertEquals("el enlace de Play tiene que estar una vez, dentro del bloque", 1, ocurrencias(bloque, PRUEBA_CERRADA));
        assertFalse("el enlace de Play está fuera del bloque de apertura", fuera.contains(PRUEBA_CERRADA));
        assertTrue("el enlace no es el de unión de la prueba de GymProFit",
                bloque.contains("href=\"https://" + PRUEBA_CERRADA + "/com.gymprofit.app\""));
        // El bloque empieza dentro del comentario de las instrucciones: se reabre para quitarlo
        String visible = sinComentarios("<!--" + bloque);
        boolean cerrada = visible.contains("Disponible el día de apertura");
        boolean enlaceVisible = visible.contains(PRUEBA_CERRADA);
        // O cerrada, con el paso apagado y el enlace comentado, o abierta, con el enlace y sin el aviso
        assertTrue("el bloque está a medias: cerrada=" + cerrada + ", enlace visible=" + enlaceVisible,
                cerrada != enlaceVisible);
        assertEquals("el bloque tiene que dejar un solo segundo paso", 1, ocurrencias(visible, "<li class=\"paso"));
        if (cerrada) {
            assertTrue("el paso cerrado no está apagado",
                    visible.contains("<span class=\"boton apagado\" aria-disabled=\"true\">"));
        }
    }

    @Test
    public void elEnlaceDePlaySoloEstaEnEntrarYElDelGrupoEnDentroYEntrar() throws IOException {
        Path web = raiz().resolve("web");
        List<Path> ficheros;
        try (Stream<Path> todos = Files.walk(web)) {
            ficheros = todos.filter(Files::isRegularFile)
                    .filter(f -> !f.toString().matches(".*\\.(woff2|webp|png|ico|jpg|svg)$"))
                    .collect(Collectors.toList());
        }
        assertTrue("no hay ficheros de texto en web/", ficheros.size() >= 8);
        for (Path f : ficheros) {
            String nombre = web.relativize(f).toString().replace('\\', '/');
            String contenido = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
            if (!nombre.equals("beta/entrar.html")) {
                assertFalse("el enlace de la prueba cerrada está en " + nombre, contenido.contains(PRUEBA_CERRADA));
            }
            if (!nombre.equals("beta/entrar.html") && !nombre.equals("beta/dentro.html")) {
                assertFalse("el enlace del grupo de probadores está en " + nombre, contenido.contains(GRUPO));
            }
        }
    }

    @Test
    public void entrarYaNoEsUnaRedireccion() {
        // DEC-052 sustituye a DEC-051: /beta/entrar es una página y ninguna regla le gana
        assertFalse("vuelve web/_redirects", Files.exists(raiz().resolve("web").resolve("_redirects")));
    }

    @Test
    public void laPoliticaCuentaElGrupoDeProbadores() throws IOException {
        String texto = texto(pagina("privacidad.html"));
        Matcher grupo = Pattern.compile("El grupo de probadores:([^•]*?)(?:Datos que se generan|$)").matcher(texto);
        assertTrue("la política no cuenta el grupo de probadores", grupo.find());
        String parrafo = grupo.group(1);
        for (String debe : new String[]{"voluntario", "Google", "sus propias condiciones", "solo vemos la dirección de correo"}) {
            assertTrue("lo del grupo no dice «" + debe + "»", parrafo.contains(debe));
        }
    }

    @Test
    public void enEscritorioElFormularioPesaComoElTitular() throws IOException {
        // GP-205: a 1280 y 1440 el formulario quedaba estrecho frente al titular. Su
        // columna no baja de 380 px y el formulario la llena hasta el borde derecho.
        String css = pagina("beta.css");
        int escritorio = css.indexOf("@media (min-width: 900px)");
        assertTrue("beta.css no tiene bloque de escritorio", escritorio >= 0);
        String bloque = css.substring(escritorio);
        Matcher columnas = Pattern.compile("grid-template-columns:\\s*var\\(--ancho-titular\\)\\s+minmax\\((\\d+)px").matcher(bloque);
        assertTrue("la columna del formulario no tiene mínimo en px", columnas.find());
        assertTrue("la columna del formulario baja de 380 px: " + columnas.group(1), Integer.parseInt(columnas.group(1)) >= 380);
        assertTrue("el formulario no llena su columna en escritorio", bloque.contains(".alta { max-width: none;"));
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

    // El HTML sin sus comentarios: lo que de verdad pinta el navegador.
    private static String sinComentarios(String html) {
        return html.replaceAll("(?s)<!--.*?-->", "");
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
        Path fichero = raiz().resolve("web").resolve(nombre);
        assertTrue("no existe web/" + nombre, Files.exists(fichero));
        return new String(Files.readAllBytes(fichero), StandardCharsets.UTF_8);
    }

    // La raíz del repositorio: la primera carpeta hacia arriba que contiene web/.
    private static Path raiz() {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("web"))) raiz = raiz.getParent();
        assertNotNull("no se encuentra la carpeta web", raiz);
        return raiz;
    }
}
