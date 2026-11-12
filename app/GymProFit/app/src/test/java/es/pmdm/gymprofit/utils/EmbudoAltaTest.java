package es.pmdm.gymprofit.utils;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

// ============================================================
// EmbudoAltaTest — GP-019: cada paso del alta se avisa una vez por intento, y solo con
// su nombre.
// ============================================================
public class EmbudoAltaTest {

    private final Set<String> guardados = new HashSet<>();
    private final List<Map<String, String>> enviados = new ArrayList<>();
    private EmbudoAlta.Avisados avisados;

    @Before
    public void preparar() {
        avisados = new EmbudoAlta.Avisados() {
            @Override public Set<String> leer() { return new HashSet<>(guardados); }
            @Override public void guardar(Set<String> pasos) { guardados.clear(); guardados.addAll(pasos); }
        };
    }

    private boolean avisar(EmbudoAlta.Paso p) {
        return EmbudoAlta.avisar(p, avisados, enviados::add);
    }

    private List<String> pasosEnviados() {
        return enviados.stream().map(c -> c.get("paso")).collect(Collectors.toList());
    }

    @Test
    public void el_cuerpo_es_solo_el_nombre_del_paso() {
        Map<String, String> cuerpo = EmbudoAlta.cuerpo(EmbudoAlta.Paso.SOBRE_TI);
        assertEquals(1, cuerpo.size());
        assertEquals("SOBRE_TI", cuerpo.get("paso"));
    }

    @Test
    public void ir_y_volver_no_cuenta_dos_veces() {
        assertTrue(avisar(EmbudoAlta.Paso.BIENVENIDA));
        assertTrue(avisar(EmbudoAlta.Paso.OBJETIVO));
        assertTrue(avisar(EmbudoAlta.Paso.NIVEL));
        assertFalse(avisar(EmbudoAlta.Paso.OBJETIVO));   // atrás
        assertFalse(avisar(EmbudoAlta.Paso.NIVEL));      // y otra vez adelante
        assertEquals(Arrays.asList("BIENVENIDA", "OBJETIVO", "NIVEL"), pasosEnviados());
    }

    @Test
    public void abrir_la_app_otra_vez_a_medias_no_es_otro_intento() {
        avisar(EmbudoAlta.Paso.BIENVENIDA);
        avisar(EmbudoAlta.Paso.OBJETIVO);
        assertFalse(avisar(EmbudoAlta.Paso.BIENVENIDA));
        assertEquals(2, enviados.size());
    }

    @Test
    public void tras_un_alta_terminada_la_bienvenida_empieza_otro_intento() {
        for (EmbudoAlta.Paso p : EmbudoAlta.Paso.values()) assertTrue(avisar(p));
        assertTrue(avisar(EmbudoAlta.Paso.BIENVENIDA));
        assertTrue(avisar(EmbudoAlta.Paso.OBJETIVO));
        assertEquals(EmbudoAlta.Paso.values().length + 2, enviados.size());
        assertEquals(new HashSet<>(Arrays.asList("BIENVENIDA", "OBJETIVO")), guardados);
    }

    @Test
    public void cada_pantalla_del_cuestionario_tiene_su_paso() {
        for (AltaPasos.Paso p : AltaPasos.Paso.values()) {
            assertEquals(p.name(), EmbudoAlta.Paso.de(p).name());
        }
    }

    // La API rechaza con 400 un nombre que no conoce: los de la app tienen que ser los suyos.
    @Test
    public void los_pasos_son_los_de_la_api() throws IOException {
        Path enumApi = Paths.get("..", "..", "..", "api", "gymprofit-api", "src", "main", "java",
                "com", "gymprofit", "api", "enums", "PasoAlta.java");
        String fuente = new String(Files.readAllBytes(enumApi), StandardCharsets.UTF_8);
        String cuerpo = fuente.substring(fuente.indexOf("enum PasoAlta {") + 15, fuente.lastIndexOf('}'));
        List<String> api = new ArrayList<>();
        Matcher m = Pattern.compile("\\b([A-Z_]+)\\b").matcher(cuerpo.replaceAll("//[^\\n]*", ""));
        while (m.find()) api.add(m.group(1));
        List<String> app = Arrays.stream(EmbudoAlta.Paso.values()).map(Enum::name).collect(Collectors.toList());
        assertEquals(api, app);
    }
}
