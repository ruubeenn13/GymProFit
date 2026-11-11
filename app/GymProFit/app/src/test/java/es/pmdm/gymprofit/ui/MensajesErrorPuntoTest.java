package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// MensajesErrorPuntoTest — los mensajes de error de UiFeedback acaban en punto (lote 13).
//
// UiFeedback.mensaje da la frase de un fallo («Sin conexión. Revisa tu red y vuelve a
// intentarlo») y casi todas las pantallas la ponen al final de la suya: VistaEstado con
// «No se ha podido cargar la lista. », las plantillas «No se ha podido añadir %1$s. %2$s».
// Sin punto, el mensaje entero se quedaba a medias. Ninguna pantalla pone nada después en
// la misma línea, así que el punto va en la frase compartida.
// ============================================================
public class MensajesErrorPuntoTest {

    @Test
    public void cadaMensajeDeErrorAcabaEnPunto() throws IOException {
        for (String carpeta : new String[]{ "values", "values-en" }) {
            String xml = new String(Files.readAllBytes(Paths.get("src", "main", "res", carpeta, "strings.xml")),
                    StandardCharsets.UTF_8);
            List<String> sinPunto = new ArrayList<>();
            int vistos = 0;
            Matcher m = Pattern.compile("<string name=\"(feedback_error_\\w+)\">(.*?)</string>").matcher(xml);
            while (m.find()) {
                vistos++;
                if (!m.group(2).endsWith(".")) sinPunto.add(m.group(1) + ": " + m.group(2));
            }
            Matcher p = Pattern.compile("<plurals name=\"(feedback_error_\\w+)\">(.*?)</plurals>", Pattern.DOTALL).matcher(xml);
            while (p.find()) {
                Matcher item = Pattern.compile("<item quantity=\"(\\w+)\">(.*?)</item>").matcher(p.group(2));
                while (item.find()) {
                    vistos++;
                    if (!item.group(2).endsWith(".")) sinPunto.add(p.group(1) + "/" + item.group(1) + ": " + item.group(2));
                }
            }
            assertTrue(carpeta + ": no se encontraron los mensajes de error", vistos >= 10);
            if (!sinPunto.isEmpty()) fail(carpeta + ", sin punto final:\n" + String.join("\n", sinPunto));
        }
    }
}
