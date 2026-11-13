package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Medidas sin red (revisor, lote 12). Cuando la lista de mediciones fallaba, la pantalla
// llamaba a intentarCrearDesdePerfil: una lectura fallida terminaba en un POST que creaba una
// medición con el peso del perfil (con la API viva y un 500 o un corte, una de más), y sin red
// decía «No hay mediciones registradas» a quien sí las tiene. Ahora un fallo es un fallo: su
// texto y «Reintentar» (VistaEstado), y nada se escribe.
public class MedidasErrorTest {

    private static final Path JAVA = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit");
    private static final Path RES = Paths.get("src", "main", "res");

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    // El cuerpo del onFail de la carga de la lista (el primero del fichero tras getOrdenadas).
    private static String onFailDeLaCarga(String fuente) {
        int carga = fuente.indexOf("medicionApi.getOrdenadas(");
        assertTrue("la carga de la lista sigue ahí", carga >= 0);
        Matcher m = Pattern.compile("public void onFail\\(int code, String message\\) \\{(.*?)\\n            \\}",
                Pattern.DOTALL).matcher(fuente);
        assertTrue(m.find(carga));
        return m.group(1);
    }

    @Test
    public void un_fallo_al_cargar_no_crea_ninguna_medicion_y_ofrece_reintentar() throws IOException {
        String f = leer(JAVA.resolve("ui/fragments/MedidasFragment.java"));
        String fallo = onFailDeLaCarga(f);
        assertFalse("un fallo de lectura no escribe", fallo.contains("intentarCrearDesdePerfil"));
        // GP-064: el error lo pinta mostrarError, que lo pone en view_estado con «Reintentar».
        assertTrue("el fallo se ve con su «Reintentar»", fallo.contains(".error(")
                || (fallo.contains("mostrarError(") && f.matches("(?s).*private void mostrarError\\(.*?estado\\.error\\(.*")));
        assertTrue(leer(RES.resolve("layout/fragment_medidas.xml")).contains("@layout/view_estado"));
    }

    @Test
    public void el_vacio_no_habla_de_un_mas_que_no_existe() throws IOException {
        for (String v : new String[]{"values/strings.xml", "values-en/strings.xml"}) {
            String s = leer(RES.resolve(v));
            Matcher m = Pattern.compile("name=\"medicion_sin_datos_subtitulo\">([^<]*)<").matcher(s);
            assertTrue(v, m.find());
            assertFalse(v, m.group(1).contains("+"));
        }
    }
}
