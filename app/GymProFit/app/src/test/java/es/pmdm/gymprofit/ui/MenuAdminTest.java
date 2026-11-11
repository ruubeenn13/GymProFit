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

// ============================================================
// MenuAdminTest — el ⋮ de las cuatro listas de administración, como el resto de la app
// (lote 13, revisor de UI pendiente del lote 12; DESIGN.md «El menú anclado», DEC-019).
//
// Las cuatro abrían el menú sin cabecera (título null), «Desactivar» salía como una opción
// más, sin el rojo ni la raya de lo destructivo, y todos los ⋮ se llamaban «Más acciones».
// Aquí se fija, en cada adaptador: la cabecera es el nombre de la fila, el ⋮ dice de qué
// fila es, «Desactivar» va marcada como destructiva y es la última opción.
// ============================================================
public class MenuAdminTest {

    private static final Path ADAPTERS = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit", "ui", "adapters");
    private static final String[] LISTAS = { "Alimento", "Ejercicio", "Rutina", "Usuario" };

    @Test
    public void elMenuLlevaCabecera() throws IOException {
        for (String lista : LISTAS) {
            String codigo = leer(lista);
            assertTrue(lista + ": no abre el menú anclado", codigo.contains("UIHelper.mostrarMenuAnclado(ctx, v, "));
            assertFalse(lista + ": el menú se abre sin cabecera", codigo.contains("mostrarMenuAnclado(ctx, v, null,"));
        }
    }

    @Test
    public void elBotonDiceDeQueFilaEs() throws IOException {
        for (String lista : LISTAS) {
            assertTrue(lista + ": el ⋮ no dice de qué fila es",
                    leer(lista).contains("h.btnAcciones.setContentDescription(ctx.getString(R.string.crear_opciones_a11y,"));
        }
    }

    @Test
    public void desactivarEsDestructivaYVaLaUltima() throws IOException {
        Pattern accion = Pattern.compile("new UIHelper\\.MenuAction\\((.*?)\\)\\);", Pattern.DOTALL);
        for (String lista : LISTAS) {
            Matcher m = accion.matcher(leer(lista));
            String ultima = null;
            int n = 0;
            while (m.find()) {
                ultima = m.group(1);
                n++;
            }
            assertTrue(lista + ": se esperaban dos opciones", n == 2);
            assertTrue(lista + ": «Desactivar» no es la última opción", ultima.contains("R.string.admin_desactivar"));
            assertTrue(lista + ": «Desactivar» no va marcada como destructiva", Pattern.compile(",\\s*desactiva\\s*,").matcher(ultima).find());
        }
    }

    private static String leer(String lista) throws IOException {
        return new String(Files.readAllBytes(ADAPTERS.resolve("Admin" + lista + "Adapter.java")), StandardCharsets.UTF_8);
    }
}
