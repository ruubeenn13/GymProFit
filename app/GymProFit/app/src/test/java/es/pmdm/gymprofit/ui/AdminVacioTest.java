package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// ============================================================
// AdminVacioTest — las cuatro listas de administración dicen cuándo no hay nada (lote 13).
//
// Con la lista vacía, Alimentos, Ejercicios, Rutinas y Usuarios salían en blanco bajo los
// chips, y un fallo de carga era solo un toast que se iba. Ahora cada una lleva su VistaEstado
// (DESIGN.md, «Huecos»): vacío con su frase, distinta si hay filtros puestos, y error con
// «Reintentar» en el sitio de la lista.
// ============================================================
public class AdminVacioTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path ACT = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit", "ui", "activities");
    private static final String[][] LISTAS = {
            { "Alimentos", "alimentos" }, { "Ejercicios", "ejercicios" },
            { "Rutinas", "rutinas" }, { "Usuarios", "usuarios" } };

    @Test
    public void cadaListaTieneSuEstado() throws IOException {
        for (String[] l : LISTAS) {
            String layout = leer(RES.resolve("layout/activity_admin_" + l[1] + ".xml"));
            assertTrue(l[0] + ": el layout no incluye view_estado",
                    layout.contains("android:id=\"@+id/estadoLista\"") && layout.contains("layout=\"@layout/view_estado\""));
            String codigo = leer(ACT.resolve("Admin" + l[0] + "Activity.java"));
            assertTrue(l[0] + ": no crea la VistaEstado", codigo.contains("new VistaEstado(findViewById(R.id.estadoLista))"));
            assertTrue(l[0] + ": no dice el vacío", codigo.contains("estado.vacio(hayFiltros()"));
            assertTrue(l[0] + ": el vacío no usa su frase", codigo.contains("R.string.admin_vacio_" + l[1]));
            assertTrue(l[0] + ": un fallo de carga no se queda en la pantalla", codigo.contains("estado.error(VistaEstado.mensaje("));
        }
    }

    @Test
    public void lasFrasesEstanEnLosDosIdiomas() throws IOException {
        for (String tema : new String[]{ "values", "values-en" }) {
            String strings = leer(RES.resolve(tema + "/strings.xml"));
            for (String[] l : LISTAS) {
                assertTrue(tema + ": falta admin_vacio_" + l[1], strings.contains("<string name=\"admin_vacio_" + l[1] + "\">"));
            }
            assertTrue(tema, strings.contains("<string name=\"admin_vacio_filtros\">"));
            assertTrue(tema, strings.contains("<string name=\"admin_lista_error\">"));
        }
    }

    @Test
    public void elFalloDeCargaNoEsSoloUnToast() throws IOException {
        for (String[] l : LISTAS) {
            String codigo = leer(ACT.resolve("Admin" + l[0] + "Activity.java"));
            int ini = codigo.indexOf("private void cargar()");
            int fin = codigo.indexOf("\n    }\n", ini);
            assertTrue(l[0] + ": no hay cargar()", ini >= 0 && fin > ini);
            assertFalse(l[0] + ": cargar() sigue avisando solo con un toast",
                    codigo.substring(ini, fin).contains("UiFeedback.toastError("));
        }
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
