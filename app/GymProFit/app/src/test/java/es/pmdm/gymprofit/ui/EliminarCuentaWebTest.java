package es.pmdm.gymprofit.ui;

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
// EliminarCuentaWebTest — la pantalla de Eliminar cuenta dice lo mismo que
// gymprofit.app/eliminar-cuenta (lote 1.5.2).
//
// Google Play enlaza la página de la web como vía de eliminación, y la app la explica
// otra vez antes de pedir la contraseña: si una dice que se borra algo que la otra no
// nombra, una de las dos miente. Cada línea de «Qué se borra» y la de la copia de los
// datos tienen que estar, letra a letra, en web/eliminar-cuenta.html. Solo en español,
// que es el idioma de la web; el inglés las traduce.
// ============================================================
public class EliminarCuentaWebTest {

    private static final String[] LINEAS = {
            "eliminar_cuenta_borra_1", "eliminar_cuenta_borra_2", "eliminar_cuenta_borra_3",
            "eliminar_cuenta_borra_4", "eliminar_cuenta_borra_5", "eliminar_cuenta_borra_6",
            "eliminar_cuenta_borra_7", "eliminar_cuenta_antes_2",
    };

    @Test
    public void cadaLineaDeLaAppEstaEnLaWeb() throws IOException {
        String web = textoDeLaWeb();
        String xml = new String(Files.readAllBytes(Paths.get("src", "main", "res", "values", "strings.xml")),
                StandardCharsets.UTF_8);
        for (String nombre : LINEAS) {
            Matcher m = Pattern.compile("<string name=\"" + nombre + "\"[^>]*>(.*?)</string>").matcher(xml);
            assertTrue("falta " + nombre, m.find());
            String linea = m.group(1).replace("\\'", "'");
            assertTrue(nombre + ": «" + linea + "» no está en web/eliminar-cuenta.html", web.contains(linea));
        }
    }

    // El texto de la página sin etiquetas ni saltos de más.
    private static String textoDeLaWeb() throws IOException {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("web"))) raiz = raiz.getParent();
        assertNotNull("no se encuentra la carpeta web", raiz);
        String html = new String(Files.readAllBytes(raiz.resolve("web").resolve("eliminar-cuenta.html")),
                StandardCharsets.UTF_8);
        return html.replaceAll("<[^>]+>", "").replace("&amp;", "&").replaceAll("\\s+", " ");
    }
}
