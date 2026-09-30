package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// CamposDecimalesTest — ningún campo decimal tira la coma (GP-139).
//
// Con inputType="numberDecimal" Android solo deja teclear dígitos y punto, y el
// teclado numérico de un móvil en español manda coma: lo escrito se pierde. Cada
// campo decimal de los layouts tiene que aceptar las dos con android:digits, como
// la sesión en vivo. Recorre los XML de verdad para que un campo nuevo no se escape.
// ============================================================
public class CamposDecimalesTest {

    private static final Pattern ETIQUETA = Pattern.compile("<[A-Za-z][^<>]*?>", Pattern.DOTALL);

    @Test
    public void todo_campo_numberDecimal_acepta_coma_y_punto() throws IOException {
        File dir = new File("src/main/res/layout");
        assertTrue("no se encuentra " + dir.getAbsolutePath(), dir.isDirectory());
        File[] xmls = dir.listFiles((d, n) -> n.endsWith(".xml"));
        List<String> sinComa = new ArrayList<>();
        int decimales = 0;
        for (File f : xmls) {
            String xml = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            Matcher m = ETIQUETA.matcher(xml);
            while (m.find()) {
                String etiqueta = m.group();
                if (!etiqueta.contains("numberDecimal")) continue;
                decimales++;
                Matcher digits = Pattern.compile("android:digits=\"([^\"]*)\"").matcher(etiqueta);
                if (!digits.find() || !digits.group(1).contains(",") || !digits.group(1).contains(".")) {
                    sinComa.add(f.getName());
                }
            }
        }
        assertTrue("no se ha encontrado ningún campo decimal", decimales > 0);
        assertTrue("campos decimales sin coma: " + sinComa, sinComa.isEmpty());
    }
}
