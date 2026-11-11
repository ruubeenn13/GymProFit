package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// AdminUsuarioFilaTest — en Usuarios de administración, el correo y el nombre en una línea
// (lote 13, revisor de UI).
//
// Los chips de rol y estado y el ⋮ no ceden sitio, y con letra 1,3 la columna del texto se
// quedaba estrecha: «admin@g / ymprofit / .app», «dani.entren / a». Un correo partido por
// cualquier sitio no se lee; cortado al final con «…» sí, y TalkBack sigue leyéndolo entero.
// ============================================================
public class AdminUsuarioFilaTest {

    @Test
    public void elCorreoYElNombreNoSeParten() throws IOException {
        String xml = new String(Files.readAllBytes(Paths.get("src", "main", "res", "layout",
                "item_admin_usuario_v2.xml")), StandardCharsets.UTF_8);
        for (String id : new String[]{ "tvEmailAdmin", "tvUsernameAdmin" }) {
            Matcher m = Pattern.compile("<TextView\\s+android:id=\"@\\+id/" + id + "\"[^>]*>").matcher(xml);
            assertTrue(id + " no está", m.find());
            String vista = m.group();
            assertTrue(id + ": puede pasar de una línea", vista.contains("android:maxLines=\"1\""));
            assertTrue(id + ": no se corta al final con «…»", vista.contains("android:ellipsize=\"end\""));
            assertTrue(id + ": sin el ancho de la columna no hay dónde cortar", vista.contains("android:layout_width=\"match_parent\""));
        }
    }
}
