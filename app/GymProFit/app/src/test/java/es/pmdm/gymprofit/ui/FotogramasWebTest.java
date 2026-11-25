package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// ============================================================
// FotogramasWebTest — cómo cuenta el panel ?debug=motion los fotogramas (web/PLAN-UN-DIA.md §4).
//
// Ejecuta web/src/movimiento/fotogramas.js con node (el mismo que construye web/dist en el CI)
// sobre secuencias de eventos inventadas: «s» es un evento de scroll y «c» un
// requestAnimationFrame, en el orden en que los da el navegador (el scroll se despacha antes
// que los rAF del mismo fotograma).
//
// · Solo cuenta mientras hay scroll de verdad: ni la cola de reposo tras parar ni el primer
//   fotograma de un gesto nuevo, que mide la pausa entre gestos y no un tirón.
// · Un tirón entre dos fotogramas con scroll sí cuenta; un hueco de más de 1 s sin ningún
//   rAF es el navegador en pausa, no un tirón, y se cuenta aparte.
// · Dice a cuántos hercios entrega fotogramas la pantalla (60 o 120).
// ============================================================
public class FotogramasWebTest {

    private static final double F60 = 1000.0 / 60;
    private static final double F120 = 1000.0 / 120;

    @Test
    public void laColaDeReposoTrasPararNoCuenta() throws Exception {
        Secuencia s = new Secuencia();
        s.desplazando(31, F60);   // 30 intervalos con scroll
        s.quieto(30, F60);        // 0,5 s sin scroll, el rAF sigue
        JsonObject r = s.resumen();
        assertEquals("solo los 30 intervalos con scroll a los dos lados", 30, r.get("n").getAsInt());
    }

    @Test
    public void laPausaEntreDosGestosNoEsUnFotogramaLento() throws Exception {
        Secuencia s = new Secuencia();
        s.desplazando(21, F60);
        s.hueco(1500);            // el navegador deja de dar rAF (WebView en pausa, otra app encima)
        s.desplazando(21, F60);
        JsonObject r = s.resumen();
        assertEquals("ningún fotograma de más de 20 ms: el hueco es una pausa, no un tirón",
                0.0, r.get("lentos").getAsDouble(), 1e-9);
        assertEquals(40, r.get("n").getAsInt());
        assertEquals("la pausa se cuenta aparte", 1, r.get("pausas").getAsInt());
    }

    @Test
    public void unBloqueoDeMenosDeUnSegundoEnPlenoScrollEsUnTiron() throws Exception {
        Secuencia s = new Secuencia();
        s.desplazando(20, F60);
        s.tiempo += 400 - F60;    // una tarea larga: el compositor sigue, el hilo principal no
        s.desplazando(20, F60);
        JsonObject r = s.resumen();
        assertEquals(39, r.get("n").getAsInt());
        assertEquals(0, r.get("pausas").getAsInt());
        assertTrue(r.get("lentos").getAsDouble() > 0);
    }

    @Test
    public void elPrimerFotogramaTrasPararNoCuentaAunqueElRafSiga() throws Exception {
        Secuencia s = new Secuencia();
        s.desplazando(21, F60);
        s.quieto(5, F60);
        s.tiempo += 60;           // el primer fotograma del gesto nuevo llega tarde (toque)
        s.desplazando(21, F60);
        JsonObject r = s.resumen();
        assertEquals(0.0, r.get("lentos").getAsDouble(), 1e-9);
    }

    @Test
    public void unTironEntreDosFotogramasConScrollSiCuenta() throws Exception {
        Secuencia s = new Secuencia();
        s.desplazando(11, F60);
        s.tiempo += 50 - F60;     // un fotograma de 50 ms en pleno scroll
        s.desplazando(10, F60);
        JsonObject r = s.resumen();
        assertEquals(20, r.get("n").getAsInt());
        assertEquals(100.0 / 20, r.get("lentos").getAsDouble(), 1e-9);
    }

    @Test
    public void conLaPestanaOcultaNoCuenta() throws Exception {
        Secuencia s = new Secuencia();
        s.desplazando(11, F60);
        s.ocultos(10, F60);
        s.desplazando(11, F60);
        assertEquals(20, s.resumen().get("n").getAsInt());
    }

    @Test
    public void diceSiLaPantallaVaA60OA120Hercios() throws Exception {
        Secuencia a60 = new Secuencia();
        a60.desplazando(60, F60);
        assertEquals(60, a60.resumen().get("hz").getAsInt());

        Secuencia a120 = new Secuencia();
        a120.desplazando(120, F120);
        a120.tiempo += 25;        // algún tirón no cambia la frecuencia
        a120.desplazando(10, F120);
        JsonObject r = a120.resumen();
        assertEquals(120, r.get("hz").getAsInt());
        assertTrue("la mediana, a 120 fps", Math.abs(r.get("mediana").getAsDouble() - 120) < 1);
    }

    /** Eventos en el orden del navegador, como [tipo, tiempo, oculto]. */
    private static final class Secuencia {
        double tiempo = 1000;
        private final List<String> eventos = new ArrayList<>();

        void desplazando(int fotogramas, double paso) {
            for (int i = 0; i < fotogramas; i++) {
                eventos.add("['s'," + tiempo + "]");
                eventos.add("['c'," + tiempo + ",0]");
                tiempo += paso;
            }
        }

        void quieto(int fotogramas, double paso) {
            for (int i = 0; i < fotogramas; i++) {
                eventos.add("['c'," + tiempo + ",0]");
                tiempo += paso;
            }
        }

        void ocultos(int fotogramas, double paso) {
            for (int i = 0; i < fotogramas; i++) {
                eventos.add("['c'," + tiempo + ",1]");
                tiempo += paso;
            }
        }

        void hueco(double ms) {
            tiempo += ms;
        }

        JsonObject resumen() throws IOException, InterruptedException {
            Path modulo = WebDist.web().resolve("src/movimiento/fotogramas.js");
            // Sin comillas dobles: en Windows, ProcessBuilder no las escapa bien dentro de -e.
            String codigo = "import {contador} from '" + modulo.toUri() + "';"
                    + "let e='';process.stdin.on('data',d=>e+=d).on('end',()=>{"
                    + "const c=contador();"
                    + "for(const [k,t,o] of eval(e)){k==='s'?c.scroll(t):c.cuadro(t,o===1)}"
                    + "process.stdout.write(JSON.stringify(c.resumen()))})";
            Process p = new ProcessBuilder("node", "--input-type=module", "-e", codigo)
                    .redirectErrorStream(true).start();
            try (OutputStream in = p.getOutputStream()) {
                in.write(("[" + String.join(",", eventos) + "]").getBytes(StandardCharsets.UTF_8));
            }
            String salida = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue("node no terminó", p.waitFor(30, TimeUnit.SECONDS));
            assertEquals("node falló: " + salida, 0, p.exitValue());
            return JsonParser.parseString(salida).getAsJsonObject();
        }
    }
}
