package es.pmdm.gymprofit.arranque;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// GArranqueTest — GP-223: lo que el arranque escribe en XML es lo de Movimiento.
//
// La G que entra en el splash del sistema es un AnimatedVectorDrawable: sus tiempos y sus
// curvas viven en res/animator, res/interpolator y el tema, que no pueden pedir un momento
// a Movimiento. Este test los lee como texto y falla si se separan de la clase (DEC-039:
// sin números a mano). También guarda que la G quieta sea el último fotograma de la
// animada, y que el arranque viejo no vuelva.
// ============================================================
public class GArranqueTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path JAVA = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit");

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static List<String> valores(String xml, String atributo) {
        List<String> v = new ArrayList<>();
        Matcher m = Pattern.compile("android:" + atributo + "=\"([^\"]+)\"").matcher(xml);
        while (m.find()) v.add(m.group(1));
        return v;
    }

    private static void todosIguales(String fichero, String atributo, long esperado) throws IOException {
        List<String> v = valores(leer(RES.resolve(fichero)), atributo);
        assertFalse(fichero + " sin " + atributo, v.isEmpty());
        for (String x : v) assertEquals(fichero + " " + atributo, String.valueOf(esperado), x);
    }

    @Test
    public void losTiemposDeLaGSonLosDeMovimiento() throws IOException {
        todosIguales("animator/g_cuerpo_gira.xml", "duration", Movimiento.G_CUERPO);
        todosIguales("animator/g_cuerpo_aparece.xml", "duration", Movimiento.G_CUERPO);
        todosIguales("animator/g_trazo_llega.xml", "duration", Movimiento.G_TRAZO);
        todosIguales("animator/g_trazo_llega.xml", "startOffset", Movimiento.G_TRAZO_RETRASO);
        todosIguales("animator/g_trazo_aparece.xml", "duration", Movimiento.G_TRAZO);
        todosIguales("animator/g_trazo_aparece.xml", "startOffset", Movimiento.G_TRAZO_RETRASO);
        // La pantalla que llega desde el arranque: ENTRA, y la de carga quieta lo mismo debajo.
        todosIguales("anim/arranque_entra.xml", "duration", Movimiento.ENTRA);
        todosIguales("anim/arranque_quieta.xml", "duration", Movimiento.ENTRA);
    }

    @Test
    public void lasCurvasDeLaGSonLasDeMovimiento() throws IOException {
        assertTrue(leer(RES.resolve("animator/g_cuerpo_gira.xml")).contains("@interpolator/estandar"));
        assertTrue(leer(RES.resolve("animator/g_trazo_llega.xml")).contains("@interpolator/rebote"));
        assertTrue(leer(RES.resolve("anim/arranque_entra.xml")).contains("@interpolator/enfatizada"));
        String[][] curvas = {
                {"estandar", "0.2", "0", "0", "1", "ESTANDAR = new PathInterpolator(0.2f, 0f, 0f, 1f)"},
                {"rebote", "0.34", "1.56", "0.64", "1", "REBOTE = new PathInterpolator(0.34f, 1.56f, 0.64f, 1f)"},
                {"enfatizada", "0.05", "0.7", "0.1", "1", "ENFATIZADA = new PathInterpolator(0.05f, 0.7f, 0.1f, 1f)"},
        };
        String movimiento = leer(JAVA.resolve("utils/Movimiento.java"));
        for (String[] c : curvas) {
            String xml = leer(RES.resolve("interpolator/" + c[0] + ".xml"));
            assertEquals(c[0], c[1], valores(xml, "controlX1").get(0));
            assertEquals(c[0], c[2], valores(xml, "controlY1").get(0));
            assertEquals(c[0], c[3], valores(xml, "controlX2").get(0));
            assertEquals(c[0], c[4], valores(xml, "controlY2").get(0));
            assertTrue("Movimiento tiene " + c[0], movimiento.contains(c[5]));
        }
    }

    @Test
    public void elSplashDuraLaEntradaYEnseniaLaGQueToca() throws IOException {
        String base = leer(RES.resolve("values/themes.xml"));
        String v31 = leer(RES.resolve("values-v31/themes.xml"));
        for (String t : new String[]{base, v31}) {
            String estilo = t.substring(t.indexOf("<style name=\"Theme.GymProFit.Arranque\""));
            estilo = estilo.substring(0, estilo.indexOf("</style>"));
            assertTrue(estilo.contains("<item name=\"windowSplashScreenAnimationDuration\">"
                    + Movimiento.G_ENTRADA + "</item>"));
            assertTrue(estilo.contains("<item name=\"postSplashScreenTheme\">@style/Theme.GymProFit</item>"));
            assertTrue("oscuro también en claro, como el icono",
                    estilo.contains("<item name=\"windowSplashScreenBackground\">@color/ic_launcher_fondo</item>"));
        }
        // Android 11 o menos enseña el icono quieto: el último fotograma, no el primero (invisible).
        assertTrue(base.contains("<item name=\"windowSplashScreenAnimatedIcon\">@drawable/g_arranque</item>"));
        assertTrue(v31.contains("<item name=\"windowSplashScreenAnimatedIcon\">@drawable/g_arranque_animada</item>"));
    }

    @Test
    public void laGQuietaEsElUltimoFotogramaDeLaAnimada() throws IOException {
        String quieta = leer(RES.resolve("drawable/g_arranque.xml"));
        String inicial = leer(RES.resolve("drawable/g_arranque_inicial.xml"));
        assertEquals("los mismos trazados", valores(quieta, "pathData"), valores(inicial, "pathData"));
        assertEquals("los mismos colores", valores(quieta, "fillColor"), valores(inicial, "fillColor"));
        assertEquals(List.of("0"), valores(quieta, "rotation"));
        assertEquals(List.of("1", "1"), valores(quieta, "fillAlpha"));
        assertEquals(List.of("0", "0"), valores(inicial, "fillAlpha"));
        assertEquals(List.of("-90"), valores(inicial, "rotation"));

        // 288 dp con la G a 160: el logo de 512 escalado a 0,3125 y centrado.
        assertTrue(quieta.contains("android:width=\"288dp\""));
        assertEquals("0.3125", valores(quieta, "scaleX").get(0));
        assertEquals("64", valores(quieta, "translateX").get(0));

        // El trazo llega desde 18 dp sobre 160, en unidades del logo de 512, con el pivote de la ficha.
        double desde = Movimiento.G_TRAZO_DESDE_DP * 512.0 / Movimiento.G_DP;
        String llega = leer(RES.resolve("animator/g_trazo_llega.xml"));
        assertTrue(llega.contains("android:valueFrom=\"" + fmt(desde) + "\""));
        assertTrue(llega.contains("android:valueFrom=\"" + fmt(-desde) + "\""));
        assertTrue(inicial.contains("android:translateX=\"" + fmt(desde) + "\""));
        assertTrue(inicial.contains("android:translateY=\"" + fmt(-desde) + "\""));
        assertTrue(inicial.contains("android:pivotX=\"" + fmt(Math.round(0.749 * 512 * 100) / 100.0) + "\""));
        assertTrue(inicial.contains("android:pivotY=\"" + fmt(0.2 * 512) + "\""));

        // Cada objetivo de la animada existe en su primer fotograma.
        String animada = leer(RES.resolve("drawable/g_arranque_animada.xml"));
        assertTrue(animada.contains("android:drawable=\"@drawable/g_arranque_inicial\""));
        for (String nombre : valores(animada, "name")) {
            assertTrue("falta " + nombre, inicial.contains("android:name=\"" + nombre + "\""));
        }
    }

    @Test
    public void elArranqueViejoNoVuelve() throws IOException {
        assertFalse(Files.exists(JAVA.resolve("ui/activities/SplashActivity.java")));
        assertFalse(Files.exists(RES.resolve("layout/activity_splash.xml")));
        assertFalse(leer(JAVA.resolve("utils/InsetsUtils.java")).contains("SplashActivity"));
        String manifiesto = leer(Paths.get("src", "main", "AndroidManifest.xml"));
        assertFalse(manifiesto.contains("android:name=\".ui.activities.SplashActivity\""));
        int main = manifiesto.indexOf("android:name=\".ui.activities.MainActivity\"");
        String entrada = manifiesto.substring(main, manifiesto.indexOf("</activity>", main));
        assertTrue(entrada.contains("android.intent.category.LAUNCHER"));
        assertTrue(entrada.contains("@style/Theme.GymProFit.Arranque"));
        // La G de la pantalla de carga es la quieta, la misma que el splash.
        assertTrue(leer(RES.resolve("layout/view_arranque.xml")).contains("android:src=\"@drawable/g_arranque\""));
    }

    @Test
    public void elCorreoDeEntrarSobreviveALaSesionCaducadaPeroNoAlCierreNiAlBorrado() throws IOException {
        String prefs = leer(JAVA.resolve("utils/PreferencesManager.java"));
        String cerrar = cuerpo(prefs, "public void cerrarSesion()");
        assertFalse("cerrarSesion() también es la sesión caducada: no borra el correo",
                cerrar.contains("KEY_ULTIMA_ENTRADA"));
        String borrar = cuerpo(prefs, "public void borrarDatosLocales()");
        assertTrue("borrar la cuenta lo vacía todo", borrar.contains("editor.clear()"));
        assertFalse("y no lo vuelve a poner", borrar.contains("KEY_ULTIMA_ENTRADA"));

        String base = leer(JAVA.resolve("ui/activities/BaseActivity.java"));
        assertTrue("cerrar sesión a propósito lo olvida",
                cuerpo(base, "protected void confirmarCerrarSesion()").contains("olvidarUltimaEntrada()"));
        assertTrue("el 401 a mitad de uso lleva el aviso",
                base.contains("intent.putExtra(LoginActivity.EXTRA_SESION_CERRADA, !cuentaDesactivada)"));
        assertTrue("entrar lo apunta", leer(JAVA.resolve("ui/activities/LoginActivity.java"))
                .contains("prefsManager.saveUltimaEntrada(username)"));
    }

    private static String fmt(double d) {
        String s = String.valueOf(d);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    // El cuerpo de un método, de su llave de apertura a la que la cierra.
    private static String cuerpo(String fuente, String firma) {
        int i = fuente.indexOf(firma);
        assertTrue("no está " + firma, i >= 0);
        int abre = fuente.indexOf('{', i), nivel = 0;
        for (int k = abre; k < fuente.length(); k++) {
            char c = fuente.charAt(k);
            if (c == '{') nivel++;
            if (c == '}' && --nivel == 0) return fuente.substring(abre, k + 1);
        }
        assertNotNull(null);
        return "";
    }
}
