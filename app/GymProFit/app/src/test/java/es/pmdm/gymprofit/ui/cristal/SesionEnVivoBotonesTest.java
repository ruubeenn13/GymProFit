package es.pmdm.gymprofit.ui.cristal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

// ============================================================
// SesionEnVivoBotonesTest — los botones de la Sesión en vivo y de su descanso siguen la regla
// de DEC-070 (GP-285, lote 2): el principal es el botón de marca, el secundario es de cristal y
// nunca hay dos principales en la misma vista.
//
// Antes del lote el descanso llevaba tres botones con el estilo del principal («−15 s», «+15 s»
// y «Saltar») y «Terminar», arriba, otro. Ahora el único principal del descanso es «Saltar», que
// es el que avanza el entreno (lleva a la serie siguiente); «−15 s» y «+15 s» ajustan la espera
// y son secundarios. «Terminar» es secundario, como en la maqueta. El otro principal de la
// pantalla, «Reintentar», vive en la tarjeta del guardado, que solo sale tras intentar guardar:
// para entonces el descanso ya no existe (SesionEnCursoRepositorio.guardar lo borra).
//
// Lee los layouts tal cual: un principal es un BotonMarca o cualquier botón con el estilo
// relleno de la casa (Widget.GymProFit.BotonPildora, .Expresivo o Boton.Marca).
// ============================================================
public class SesionEnVivoBotonesTest {

    private static final Path LAYOUT = Paths.get("src", "main", "res", "layout");
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final List<String> ESTILOS_PRINCIPAL = Arrays.asList(
            "@style/Widget.GymProFit.BotonPildora",
            "@style/Widget.GymProFit.BotonPildora.Expresivo",
            "@style/Widget.GymProFit.Boton.Marca");

    @Test
    public void el_descanso_tiene_un_solo_principal_y_es_saltar() throws Exception {
        Document descanso = xml("view_descanso.xml");
        assertEquals("principales del descanso", Collections.singletonList("btnDescansoSaltar"),
                principales(descanso.getDocumentElement()));
        Element saltar = porId(descanso, "btnDescansoSaltar");
        assertTrue("«Saltar» es el botón de marca", saltar.getTagName().endsWith(".BotonMarca"));
        assertEquals("@style/Widget.GymProFit.Boton.Marca", saltar.getAttribute("style"));
        for (String id : new String[]{ "btnDescansoMenos", "btnDescansoMas" }) {
            Element boton = porId(descanso, id);
            assertTrue(id + " es secundario: de cristal", boton.getTagName().endsWith(".BotonCristal"));
            assertEquals("@style/Widget.GymProFit.Boton.Cristal", boton.getAttribute("style"));
        }
    }

    @Test
    public void la_pantalla_no_suma_otro_principal_al_del_descanso() throws Exception {
        Document pantalla = xml("activity_sesion_en_vivo.xml");
        // Fuera de la tarjeta del guardado (que no convive con el descanso), ninguno.
        Element guardado = porId(pantalla, "cardGuardado");
        List<String> todos = principales(pantalla.getDocumentElement());
        List<String> delGuardado = principales(guardado);
        List<String> fuera = new ArrayList<>(todos);
        fuera.removeAll(delGuardado);
        assertEquals("principales fuera de la tarjeta del guardado", Collections.emptyList(), fuera);
        assertEquals("principales de la tarjeta del guardado", Collections.singletonList("btnReintentar"),
                delGuardado);
        // La lista: ni las tarjetas de ejercicio ni su final llevan principal.
        for (String item : new String[]{ "item_envivo_ejercicio.xml", "item_envivo_serie.xml",
                "item_envivo_pie.xml", "item_envivo_final.xml" }) {
            assertEquals("principales de " + item, Collections.emptyList(),
                    principales(xml(item).getDocumentElement()));
        }
    }

    private static Document xml(String nombre) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        return f.newDocumentBuilder().parse(LAYOUT.resolve(nombre).toFile());
    }

    // Los ids de los botones principales que cuelgan de ese elemento, en orden.
    private static List<String> principales(Element raiz) {
        List<String> ids = new ArrayList<>();
        recorrer(raiz, ids);
        return ids;
    }

    private static void recorrer(Element e, List<String> ids) {
        if (e.getTagName().endsWith(".BotonMarca") || ESTILOS_PRINCIPAL.contains(e.getAttribute("style"))) {
            ids.add(e.getAttributeNS(ANDROID, "id").replace("@+id/", "").replace("@id/", ""));
        }
        NodeList hijos = e.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node n = hijos.item(i);
            if (n instanceof Element) recorrer((Element) n, ids);
        }
    }

    private static Element porId(Document doc, String id) {
        NodeList todos = doc.getElementsByTagName("*");
        for (int i = 0; i < todos.getLength(); i++) {
            Element e = (Element) todos.item(i);
            String suyo = e.getAttributeNS(ANDROID, "id");
            if (suyo.equals("@+id/" + id) || suyo.equals("@id/" + id)) return e;
        }
        throw new AssertionError("no existe " + id);
    }
}
