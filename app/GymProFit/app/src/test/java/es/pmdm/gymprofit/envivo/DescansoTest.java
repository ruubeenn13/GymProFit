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
import java.util.Locale;
import java.util.TimeZone;

import es.pmdm.gymprofit.envivo.LogicaDescanso.Estado;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;

// ============================================================
// DescansoTest — el descanso entre series (GP-013) en la sesión en vivo: cuál toca
// (el de la pauta o el de Ajustes), que no hay tras la última serie, que marcar otra
// lo empieza de nuevo, lo que lo quita, ±15 s con sus límites, «Después», y que el fin
// guardado en el fichero sobrevive a matar el proceso sin volver a sonar.
// ============================================================
public class DescansoTest {

    @Rule public InstantTaskExecutorRule instantaneo = new InstantTaskExecutorRule();
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private AlmacenSesion almacen;
    private SesionEnCursoRepositorioTest.RedFalsa red;
    private long ahora = 1_790_000_000_000L;
    private boolean alMarcar = true;
    private int sinPauta = 90;
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
            @Override public boolean empezarAlMarcar() { return alMarcar; }
            @Override public int sinPautaSegundos() { return sinPauta; }
        });
        return r;
    }

    private static RutinaEjercicio re(int ejercicioId, int series, Integer descanso) {
        RutinaEjercicio r = new RutinaEjercicio();
        r.setEjercicioId(ejercicioId);
        r.setSeries(series);
        r.setRepeticiones(10);
        r.setTiempoDescanso(descanso);
        r.setNombreEjercicio(ejercicioId == 12 ? "Hip thrust con barra" : "Sentadilla búlgara");
        return r;
    }

    /** Pierna B: hip thrust 2 × 10 con 2 min, búlgara 2 × 10 con 90 s. */
    private void empezar() {
        repo.empezar(31, "Pierna B", "Torso y pierna");
        red.ejercicios.get(0).ok(Arrays.asList(re(12, 2, 120), re(13, 2, 90)));
    }

    private long serie(int ejercicio, int indice) {
        return repo.actual().ejercicios.get(ejercicio).series.get(indice).id;
    }

    private void hacer(int ejercicio, int indice) {
        repo.escribirRepeticiones(serie(ejercicio, indice), "10");
        assertEquals(LogicaSesion.Marcado.HECHA, repo.marcar(serie(ejercicio, indice)));
    }

    private SesionEnCurso.Descanso descanso() {
        return repo.actual().descanso;
    }

    // ── Cuál toca ────────────────────────────────────────────

    @Test
    public void marcar_empieza_el_de_la_pauta_de_su_ejercicio() {
        empezar();
        hacer(0, 0);

        assertNotNull(descanso());
        assertEquals(ahora + 120_000, descanso().finMs);
        assertEquals(serie(0, 0), descanso().serieId);
    }

    @Test
    public void sin_pauta_el_de_ajustes() {
        sinPauta = 60;
        repo.empezar(null, null, null);
        repo.anadirEjercicios(Collections.singletonList(new SesionEnCursoRepositorio.Nuevo(40, "Curl")));
        hacer(0, 0);

        assertEquals("ejercicio añadido: el de Ajustes", ahora + 60_000, descanso().finMs);
    }

    @Test
    public void de_rutina_sin_descanso_en_la_pauta_el_de_ajustes() {
        repo.empezar(31, "Pierna B", null);
        red.ejercicios.get(0).ok(Collections.singletonList(re(12, 2, null)));
        hacer(0, 0);

        assertEquals(ahora + 90_000, descanso().finMs);
    }

    @Test
    public void con_ajustes_apagado_no_empieza() {
        alMarcar = false;
        empezar();
        hacer(0, 0);

        assertNull(descanso());
    }

    @Test
    public void tras_la_ultima_serie_que_queda_no_hay() {
        empezar();
        hacer(0, 0);
        hacer(0, 1);
        hacer(1, 1);
        assertNotNull("aún queda la 1 de la búlgara", descanso());

        hacer(1, 0);

        assertNull("ya no queda ninguna por hacer", descanso());
    }

    @Test
    public void marcar_otra_lo_empieza_de_nuevo_con_el_suyo() {
        empezar();
        hacer(0, 0);
        ahora += 30_000;
        hacer(1, 0);

        assertEquals(ahora + 90_000, descanso().finMs);
        assertEquals(serie(1, 0), descanso().serieId);
    }

    // ── Lo que lo quita ──────────────────────────────────────

    @Test
    public void desmarcar_la_serie_que_lo_empezo_lo_quita_y_otra_no() {
        empezar();
        hacer(0, 0);
        hacer(0, 1);
        repo.marcar(serie(0, 0));
        assertNotNull("desmarcar otra no lo toca", descanso());

        repo.marcar(serie(0, 1));

        assertNull(descanso());
    }

    @Test
    public void empezar_el_cronometro_lo_quita() {
        empezar();
        hacer(0, 0);
        repo.empezarCronometro(serie(0, 1));

        assertNull(descanso());
    }

    @Test
    public void parar_y_apuntar_lo_empieza() {
        empezar();
        repo.empezarCronometro(serie(0, 0));
        ahora += 40_000;
        assertTrue(repo.pararYApuntar());

        assertEquals(ahora + 120_000, descanso().finMs);
    }

    @Test
    public void terminar_y_descartar_lo_quitan() {
        empezar();
        hacer(0, 0);
        repo.guardar();
        assertNull("guardar", descanso());

        SesionEnCursoRepositorio otro = nuevoProceso();
        otro.usarCuenta(8);
        otro.empezar(31, "Pierna B", null);
        red.ejercicios.get(1).ok(Arrays.asList(re(12, 2, 120)));
        otro.escribirRepeticiones(otro.actual().ejercicios.get(0).series.get(0).id, "10");
        otro.marcar(otro.actual().ejercicios.get(0).series.get(0).id);
        otro.descartar();
        assertNull(almacen.leer(8));
    }

    @Test
    public void saltar_lo_quita_sin_avisar() {
        empezar();
        hacer(0, 0);
        repo.saltarDescanso();

        assertNull(descanso());
        ahora += 200_000;
        assertEquals(Estado.NINGUNO, repo.comprobarDescanso());
        assertNull(repo.finDescanso());
    }

    // ── ±15 s ────────────────────────────────────────────────

    @Test
    public void mas_y_menos_quince_mueven_el_fin() {
        empezar();
        hacer(0, 0);
        long fin = descanso().finMs;

        repo.ajustarDescanso(15);
        assertEquals(fin + 15_000, descanso().finMs);
        repo.ajustarDescanso(-15);
        repo.ajustarDescanso(-15);
        assertEquals(fin - 15_000, descanso().finMs);
    }

    @Test
    public void no_baja_de_cero_ni_pasa_de_diez_minutos() {
        empezar();
        hacer(0, 0);
        for (int i = 0; i < 60; i++) repo.ajustarDescanso(15);
        assertEquals("hasta 10 min", ahora + 600_000, descanso().finMs);
        assertFalse(LogicaDescanso.puedeSumar(descanso(), ahora));

        for (int i = 0; i < 60; i++) repo.ajustarDescanso(-15);
        assertEquals("no queda menos de nada", ahora, descanso().finMs);
    }

    @Test
    public void la_barra_conserva_lo_hecho_al_ajustar() {
        SesionEnCurso s = new SesionEnCurso();
        SesionEnCurso.Descanso d = new SesionEnCurso.Descanso();
        d.finMs = 1000 + 90_000;
        d.duracionMs = 120_000;
        s.descanso = d;
        assertEquals("30 s de 120", 25, LogicaDescanso.progreso(d, 1000));

        LogicaDescanso.ajustar(s, 15, 1000);

        assertEquals(135_000, d.duracionMs);
        assertEquals(22, LogicaDescanso.progreso(d, 1000));
    }

    // ── «Después» ────────────────────────────────────────────

    @Test
    public void despues_es_la_primera_serie_sin_marcar() {
        empezar();
        hacer(0, 0);
        hacer(1, 0);

        LogicaDescanso.Siguiente sig = LogicaDescanso.siguiente(repo.actual());

        assertEquals("Hip thrust con barra", sig.ejercicio.nombre);
        assertEquals(2, sig.numero);
    }

    // ── El fin ───────────────────────────────────────────────

    @Test
    public void al_terminar_avisa_una_vez_y_deja_a_por_la_serie() {
        empezar();
        hacer(0, 0);
        ahora += 119_000;
        assertEquals(Estado.EN_MARCHA, repo.comprobarDescanso());
        assertEquals(1, LogicaDescanso.restanteSegundos(descanso(), ahora));

        ahora += 1_500;
        assertEquals(Estado.TERMINA, repo.comprobarDescanso());
        assertNull(descanso());
        SesionEnCursoRepositorio.FinDescanso fin = repo.finDescanso();
        assertEquals("Hip thrust con barra", fin.ejercicio);
        assertEquals(2, fin.numero);

        assertEquals("una sola vez", Estado.NINGUNO, repo.comprobarDescanso());
        ahora += SesionEnCursoRepositorio.FIN_VISIBLE_MS;
        assertNull("se va sola", repo.finDescanso());
    }

    @Test
    public void el_aviso_se_cierra_con_su_boton() {
        empezar();
        hacer(0, 0);
        ahora += 120_000;
        repo.comprobarDescanso();
        repo.cerrarFinDescanso();

        assertNull(repo.finDescanso());
    }

    @Test
    public void matar_el_proceso_no_para_la_cuenta_atras() {
        empezar();
        hacer(0, 0);
        long fin = descanso().finMs;
        ahora += 50_000;

        SesionEnCursoRepositorio despues = nuevoProceso();
        despues.usarCuenta(7);

        assertEquals(fin, despues.actual().descanso.finMs);
        assertEquals(70, LogicaDescanso.restanteSegundos(despues.actual().descanso, ahora));
        assertEquals(Estado.EN_MARCHA, despues.comprobarDescanso());
    }

    @Test
    public void si_acabo_con_la_app_cerrada_no_vuelve_a_sonar() {
        empezar();
        hacer(0, 0);
        ahora += 10 * 60_000;

        SesionEnCursoRepositorio despues = nuevoProceso();
        despues.usarCuenta(7);

        assertEquals(Estado.TERMINO_ANTES, despues.comprobarDescanso());
        assertNull(despues.actual().descanso);
        assertNull("sin «¡A por la serie!» de hace diez minutos", despues.finDescanso());
        assertNull("y queda quitado en el fichero", almacen.leer(7).descanso);
    }

    @Test
    public void la_alarma_lo_quita_y_dice_que_toca() {
        empezar();
        hacer(0, 0);
        assertNull("antes de su hora no hace nada", repo.terminarPorAlarma());

        ahora += 120_000;
        SesionEnCursoRepositorio.FinDescanso fin = repo.terminarPorAlarma();

        assertEquals("Hip thrust con barra", fin.ejercicio);
        assertEquals(2, fin.numero);
        assertNull(descanso());
        assertNull("al volver no suena otra vez", repo.terminarPorAlarma());
        assertEquals(Estado.NINGUNO, repo.comprobarDescanso());
    }

    // ── Cómo avisar con la app delante ───────────────────────

    @Test
    public void el_aviso_sigue_el_modo_del_movil_y_no_molestar() {
        LogicaDescanso.Aviso normal = LogicaDescanso.aviso(LogicaDescanso.Timbre.NORMAL, false);
        assertTrue(normal.sonar);
        assertTrue(normal.vibrar);

        LogicaDescanso.Aviso vibracion = LogicaDescanso.aviso(LogicaDescanso.Timbre.VIBRACION, false);
        assertFalse(vibracion.sonar);
        assertTrue(vibracion.vibrar);

        LogicaDescanso.Aviso silencio = LogicaDescanso.aviso(LogicaDescanso.Timbre.SILENCIO, false);
        assertFalse(silencio.sonar);
        assertFalse(silencio.vibrar);

        LogicaDescanso.Aviso dnd = LogicaDescanso.aviso(LogicaDescanso.Timbre.NORMAL, true);
        assertFalse("No molestar no molesta", dnd.sonar);
        assertFalse(dnd.vibrar);
    }
}
