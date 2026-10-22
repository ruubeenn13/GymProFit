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
import java.util.Locale;
import java.util.TimeZone;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;

// ============================================================
// AccionesDescansoTest — «+15 s» y «Saltar» de la notificación de la sesión (GP-210
// paso 3, momento 27) cambian la sesión de verdad, y el plan de la notificación (cuándo
// lleva los botones y cuándo se pide la promoción a Live Update) sale de la sesión.
// ============================================================
public class AccionesDescansoTest {

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
        r.setRepeticiones(10);
        r.setTiempoDescanso(120);
        r.setNombreEjercicio("Ejercicio " + ejercicioId);
        return r;
    }

    private void empezarYMarcarUna() {
        repo.empezar(31, "Pierna B", null);
        red.ejercicios.get(0).ok(Arrays.asList(re(12, 2), re(13, 2)));
        long id = repo.actual().ejercicios.get(0).series.get(0).id;
        repo.escribirRepeticiones(id, "10");
        assertEquals(LogicaSesion.Marcado.HECHA, repo.marcar(id));
    }

    // ── Las acciones ─────────────────────────────────────────

    @Test
    public void mas_15_alarga_el_descanso_de_la_sesion() {
        empezarYMarcarUna();
        long fin = repo.actual().descanso.finMs;

        assertTrue(AccionDescanso.aplicar(repo, AccionDescanso.ACCION_MAS));

        assertEquals(fin + 15_000, repo.actual().descanso.finMs);
    }

    @Test
    public void saltar_quita_el_descanso_de_la_sesion() {
        empezarYMarcarUna();

        assertTrue(AccionDescanso.aplicar(repo, AccionDescanso.ACCION_SALTAR));

        assertNull(repo.actual().descanso);
    }

    @Test
    public void sin_descanso_las_acciones_no_hacen_nada() {
        repo.empezar(null, null, null);

        assertFalse(AccionDescanso.aplicar(repo, AccionDescanso.ACCION_MAS));
        assertFalse(AccionDescanso.aplicar(repo, AccionDescanso.ACCION_SALTAR));
    }

    @Test
    public void una_accion_desconocida_se_ignora_y_no_toca_el_descanso() {
        empezarYMarcarUna();
        long fin = repo.actual().descanso.finMs;

        assertFalse(AccionDescanso.aplicar(repo, "es.pmdm.gymprofit.OTRA"));
        assertFalse(AccionDescanso.aplicar(repo, null));

        assertEquals(fin, repo.actual().descanso.finMs);
    }

    @Test
    public void mas_15_funciona_con_la_app_muerta() {
        empezarYMarcarUna();
        long fin = repo.actual().descanso.finMs;

        SesionEnCursoRepositorio otro = nuevoProceso();
        otro.usarCuenta(7);
        assertTrue(AccionDescanso.aplicar(otro, AccionDescanso.ACCION_MAS));

        SesionEnCursoRepositorio tercero = nuevoProceso();
        tercero.usarCuenta(7);
        assertEquals("y queda en el fichero", fin + 15_000, tercero.actual().descanso.finMs);
    }

    // ── El plan de la notificación ───────────────────────────

    @Test
    public void durante_el_descanso_lleva_mas_15_y_saltar_y_pide_la_promocion() {
        empezarYMarcarUna();

        PlanNotificacionSesion plan = PlanNotificacionSesion.de(repo.actual(), ahora);

        assertTrue(plan.masQuince);
        assertTrue(plan.saltar);
        assertTrue(plan.promovida);
        assertNotNull(plan.ejercicioDelDescanso);
        assertEquals(1, plan.seriesHechas);
        assertEquals(2, plan.seriesTotales);
    }

    @Test
    public void sin_descanso_no_lleva_botones_pero_si_se_promueve() {
        repo.empezar(null, null, null);

        PlanNotificacionSesion plan = PlanNotificacionSesion.de(repo.actual(), ahora);

        assertFalse(plan.masQuince);
        assertFalse(plan.saltar);
        assertTrue(plan.promovida);
    }

    @Test
    public void en_el_tope_de_diez_minutos_ya_no_ofrece_mas_15() {
        empezarYMarcarUna();
        repo.actual().descanso.finMs = ahora + LogicaDescanso.MAX_SEGUNDOS * 1000L;

        PlanNotificacionSesion plan = PlanNotificacionSesion.de(repo.actual(), ahora);

        assertFalse(plan.masQuince);
        assertTrue("saltar sigue valiendo", plan.saltar);
    }

    @Test
    public void una_sesion_que_no_se_pudo_guardar_no_se_promueve_ni_lleva_botones() {
        empezarYMarcarUna();
        repo.actual().claveIdempotencia = "clave";
        repo.actual().guardado = SesionEnCurso.Guardado.FALLO;

        PlanNotificacionSesion plan = PlanNotificacionSesion.de(repo.actual(), ahora);

        assertFalse(plan.promovida);
        assertFalse(plan.masQuince);
        assertFalse(plan.saltar);
    }
}
