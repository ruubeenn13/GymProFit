package es.pmdm.gymprofit.ui;

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
// ApostrofosTest — en español no hay apóstrofos (lote 13).
//
// «desde aqu\'í» salía tal cual en lo que lee TalkBack en la fila de la propia cuenta de
// administración: alguien escapó una tilde como si fuera un apóstrofo. En español el único
// \' que vale es el de los patrones de fecha, donde marca el texto literal («d 'de' MMMM»).
// ============================================================
public class ApostrofosTest {

    @Test
    public void enEspanolSoloHayApostrofosEnLasFechas() throws IOException {
        String xml = new String(Files.readAllBytes(Paths.get("src", "main", "res", "values", "strings.xml")),
                StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("<string name=\"(\\w+)\"[^>]*>(.*?)</string>").matcher(xml);
        List<String> malos = new ArrayList<>();
        while (m.find()) {
            String sinFechas = m.group(2).replace("\\'de\\'", "");
            if (sinFechas.contains("\\'") || sinFechas.contains("'")) malos.add(m.group(1) + ": " + m.group(2));
        }
        if (!malos.isEmpty()) fail("Apóstrofos en español fuera de un patrón de fecha:\n" + String.join("\n", malos));
    }
}
