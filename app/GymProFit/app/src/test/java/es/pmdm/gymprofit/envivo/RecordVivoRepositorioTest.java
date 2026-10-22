package es.pmdm.gymprofit.envivo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.envivo.UltimaVez;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;

// ============================================================
// RecordVivoRepositorioTest — el récord en mitad de la serie dentro del repositorio
// (GP-210 paso 3): se pregunta la mejor marca de quien ya entrenó ese ejercicio, marcar la
// serie que la bate deja el récord para la pantalla (una vez por ejercicio y sesión), y
// todo sobrevive a matar la app sin celebrar dos veces.
// ============================================================
public class RecordVivoRepositorioTest {

    @Rule public InstantTaskExecutorRule instantaneo = new InstantTaskExecutorRule();
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private AlmacenSesion almacen;
    private SesionEnCursoRepositorioTest.RedFalsa red;
    private long ahora = 1_790_000_000_000L;
    private SesionEnCursoRepositorio repo;

    @Before
    public void preparar() throws Exception {
        almacen = new AlmacenSesion(tmp.newFolder("sesion_en_curso"));
        red = new SesionEnCursoRepositorioTest.RedFalsa();
        repo = nuevoProceso();
        repo.usarCuenta(7);
    }

    private SesionEnCursoRepositorio nuevoProceso() {
        SesionEnCursoRepositorio r = new SesionEnCursoRepositorio(almacen, red, () -> ahora,
                TimeZone.getTimeZone("Europe/Madrid"), new Locale("es", "ES"));
        r.setAjustesDescanso(new SesionEnCursoRepositorio.AjustesDescanso() {
            @Override public boolean empezarAlMarcar() { return true; }
            @Override public int sinPautaSegundos() { return 90; }
        });
        return r;
    }

    private static RutinaEjercicio re(int ejercicioId, int series) {
        RutinaEjercicio r = new RutinaEjercicio();
        r.setEjercicioId(ejercicioId);
        r.setSeries(series);
        r.setRepeticiones(8);
        r.setNombreEjercicio("Ejercicio " + ejercicioId);
        return r;
    }

    private static UltimaVez ultima(int ejercicioId) {
        UltimaVez u = new UltimaVez();
        u.ejercicioId = ejercicioId;
        u.fecha = "2026-10-01T18:00:00";
        UltimaVez.Serie s = new UltimaVez.Serie();
        s.numero = 1;
        s.peso = new java.math.BigDecimal("80");
        s.repeticiones = 8;
        u.series = Collections.singletonList(s);
        return u;
    }

    /** Press (12) con historial, sentadilla (13) con historial y curl (14) nuevo. */
    private void empezar() {
        repo.empezar(31, "Cuerpo completo B", null);
        red.ejercicios.get(0).ok(Arrays.asList(re(12, 4), re(13, 3), re(14, 2)));
        red.ultimaVez.get(0).ok(Arrays.asList(ultima(12), ultima(13)));
    }

    private long serie(int ejercicio, int indice) {
        return repo.actual().ejercicios.get(ejercicio).series.get(indice).id;
    }

    private void marcar(int ejercicio, int indice, String kilos, String reps) {
        repo.escribirPeso(serie(ejercicio, indice), kilos);
        repo.escribirRepeticiones(serie(ejercicio, indice), reps);
        assertEquals(LogicaSesion.Marcado.HECHA, repo.marcar(serie(ejercicio, indice)));
    }

    private static Map<Integer, SesionEnCurso.Marca> marcas(Object... pares) {
        Map<Integer, SesionEnCurso.Marca> m = new HashMap<>();
        for (int i = 0; i < pares.length; i += 2) m.put((Integer) pares[i], (SesionEnCurso.Marca) pares[i + 1]);
        return m;
    }

    private static SesionEnCurso.Marca peso(String kilos, int reps) {
        return new SesionEnCurso.Marca("PESO", kilos, reps, 0);
    }

    // ── Preguntar la marca ───────────────────────────────────

    @Test
    public void tras_la_ultima_vez_pide_la_marca_solo_de_quien_ya_entreno_el_ejercicio() {
        empezar();

        assertEquals(1, red.marcasPedidas.size());
        assertEquals(Arrays.asList(12, 13), red.marcasPedidas.get(0));
        assertTrue("el curl es nuevo: ya se sabe que no hay marca", repo.actual().ejercicios.get(2).marcaCargada);
    }

    @Test
    public void la_respuesta_queda_en_la_sesion_y_en_el_fichero() {
        empezar();
        red.marcas.get(0).ok(marcas(12, peso("82.5", 8)));

        assertEquals("82.5", repo.actual().ejercicios.get(0).marca.peso);
        assertNull(repo.actual().ejercicios.get(1).marca);
        assertTrue(repo.actual().ejercicios.get(1).marcaCargada);

        SesionEnCursoRepositorio otro = nuevoProceso();
        otro.usarCuenta(7);
        assertEquals("82.5", otro.actual().ejercicios.get(0).marca.peso);
    }

    @Test
    public void si_falla_la_red_no_se_celebra_nada_y_se_vuelve_a_pedir() {
        empezar();
        red.marcas.get(0).fallo(-1, "sin red");

        assertFalse(repo.actual().ejercicios.get(0).marcaCargada);
        marcar(0, 0, "200", "8");
        assertNull(repo.tomarRecord());

        repo.pedirMarcas();
        assertEquals("una segunda petición", 2, red.marcasPedidas.size());
    }

    // ── Celebrar ─────────────────────────────────────────────

    @Test
    public void marcar_la_serie_que_bate_la_marca_deja_el_record_una_sola_vez() {
        empezar();
        red.marcas.get(0).ok(marcas(12, peso("82.5", 8), 13, peso("100", 5)));

        marcar(0, 0, "85", "7");
        SesionEnCursoRepositorio.RecordBatido r = repo.tomarRecord();

        assertNotNull(r);
        assertEquals(serie(0, 0), r.serieId);
        assertEquals(7, r.nueva.repeticiones);
        assertEquals("82.5", r.anterior.peso);
        assertNull("se consume: la pantalla lo celebra una vez", repo.tomarRecord());
    }

    @Test
    public void la_segunda_serie_mejor_del_mismo_ejercicio_no_deja_otro_record() {
        empezar();
        red.marcas.get(0).ok(marcas(12, peso("82.5", 8)));
        marcar(0, 0, "85", "7");
        assertNotNull(repo.tomarRecord());

        marcar(0, 1, "87.5", "6");

        assertNull(repo.tomarRecord());
    }

    @Test
    public void otro_ejercicio_tiene_su_propio_record() {
        empezar();
        red.marcas.get(0).ok(marcas(12, peso("82.5", 8), 13, peso("100", 5)));
        marcar(0, 0, "85", "7");
        assertNotNull(repo.tomarRecord());

        marcar(1, 0, "102.5", "5");

        assertNotNull(repo.tomarRecord());
    }

    @Test
    public void tras_matar_la_app_el_mismo_ejercicio_no_se_vuelve_a_celebrar() {
        empezar();
        red.marcas.get(0).ok(marcas(12, peso("82.5", 8)));
        marcar(0, 0, "85", "7");
        assertNotNull(repo.tomarRecord());

        SesionEnCursoRepositorio otro = nuevoProceso();
        otro.usarCuenta(7);
        long segunda = otro.actual().ejercicios.get(0).series.get(1).id;
        otro.escribirPeso(segunda, "90");
        otro.escribirRepeticiones(segunda, "5");
        otro.marcar(segunda);

        assertNull(otro.tomarRecord());
    }

    @Test
    public void desmarcar_y_marcar_de_nuevo_la_serie_del_record_no_lo_repite() {
        empezar();
        red.marcas.get(0).ok(marcas(12, peso("82.5", 8)));
        marcar(0, 0, "85", "7");
        assertNotNull(repo.tomarRecord());

        repo.marcar(serie(0, 0));
        repo.marcar(serie(0, 0));

        assertNull(repo.tomarRecord());
    }

    @Test
    public void un_record_no_consumido_se_pierde_al_marcar_otra_serie_sin_record() {
        empezar();
        red.marcas.get(0).ok(marcas(12, peso("82.5", 8)));
        marcar(0, 0, "85", "7");

        marcar(1, 0, "50", "8");

        assertNull("lo que no se celebró en su momento no se celebra después", repo.tomarRecord());
    }
}
