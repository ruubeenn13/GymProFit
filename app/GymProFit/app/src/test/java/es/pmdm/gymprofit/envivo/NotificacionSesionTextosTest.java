package es.pmdm.gymprofit.envivo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// NotificacionSesionTextosTest — GP-143: durante el descanso, la notificación de la
// sesión no enseña los minutos de sesión.
//
// La notificación del descanso no se repinta cada minuto (lleva la cuenta atrás del
// sistema), así que unos minutos en su texto se quedaban parados en los del principio
// del descanso. Los textos que usa durante el descanso solo tienen sitio para la serie
// y la rutina; el reloj vuelve con la notificación normal al acabar.
// ============================================================
public class NotificacionSesionTextosTest {

    private static final Path RES = Paths.get("src", "main", "res");

    @Test
    public void elTextoDelDescansoSoloLlevaSerieYRutina() throws IOException {
        for (String carpeta : new String[]{"values", "values-en"}) {
            String texto = cadena(carpeta, "envivo_notif_descanso_texto");
            assertNotNull(carpeta + ": falta envivo_notif_descanso_texto", texto);
            assertEquals(carpeta + ": «" + texto + "» lleva algo más que la serie y la rutina",
                    2, huecos(texto));
        }
    }

    @Test
    public void laUltimaSerieNoTieneTextoConMinutos() throws IOException {
        // En la última serie solo queda el nombre de la rutina: no hace falta otro texto.
        for (String carpeta : new String[]{"values", "values-en"}) {
            assertFalse(carpeta + ": sigue envivo_notif_descanso_texto_ultima",
                    cadena(carpeta, "envivo_notif_descanso_texto_ultima") != null);
        }
    }

    private static int huecos(String texto) {
        Matcher m = Pattern.compile("%\\d+\\$[sd]").matcher(texto);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    private static String cadena(String carpeta, String nombre) throws IOException {
        String xml = new String(Files.readAllBytes(RES.resolve(carpeta).resolve("strings.xml")), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("<string name=\"" + nombre + "\"[^>]*>(.*?)</string>").matcher(xml);
        return m.find() ? m.group(1) : null;
    }
}
