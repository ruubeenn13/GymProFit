package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

// El snackbar de la app (revisor, lote 12). Widget.GymProFit.Snackbar ponía el fondo en
// superficie 2 y dejaba el texto inverso de Material: «Escribe las repeticiones para marcarla»
// quedaba a 1,09:1 en claro y 1,10:1 en oscuro, y así todo snackbar que no fijara su color.
// El tema da a todos el texto del color del texto y la acción en naranja; los avisos
// «invertidos» (Comida, Historial, Crear plato) siguen fijando los suyos.
public class SnackbarTemaTest {

    private static String temas() throws IOException {
        return new String(Files.readAllBytes(Paths.get("src", "main", "res", "values", "themes.xml")),
                StandardCharsets.UTF_8);
    }

    @Test
    public void el_texto_del_snackbar_es_el_del_tema_sobre_superficie_2() throws IOException {
        String t = temas();
        assertTrue(t.contains("<item name=\"snackbarTextViewStyle\">@style/Widget.GymProFit.Snackbar.Texto</item>"));
        assertTrue(Pattern.compile("<style name=\"Widget.GymProFit.Snackbar.Texto\"[^>]*>\\s*"
                + "<item name=\"android:textColor\">\\?attr/colorOnSurface</item>", Pattern.DOTALL).matcher(t).find());
        assertTrue(t.contains("<item name=\"snackbarButtonStyle\">@style/Widget.GymProFit.Snackbar.Accion</item>"));
    }
}
