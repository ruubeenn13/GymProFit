package es.pmdm.gymprofit.envivo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import es.pmdm.gymprofit.envivo.SesionEnCursoRepositorio.Empezar;
import es.pmdm.gymprofit.envivo.SesionEnCursoRepositorio.Guardar;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.envivo.UltimaVez;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;

// ============================================================
// SesionEnCursoRepositorioTest — la sesión en vivo como fuente única de verdad (GP-012):
// una a la vez, sobrevive en el fichero de su cuenta, no se edita desde el primer
// intento de guardar, el reintento reusa la clave y el de al abrir la app va una sola
// vez y no duplica.
// ============================================================
public class SesionEnCursoRepositorioTest {

    @Rule public InstantTaskExecutorRule instantaneo = new InstantTaskExecutorRule();
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    /** Red falsa: guarda las peticiones y responde cuando el test quiere. */
    static final class RedFalsa implements SesionEnCursoRepositorio.Red {
        final List<Respuesta<List<RutinaEjercicio>>> ejercicios = new ArrayList<>();
        final List<List<Integer>> ultimaVezPedidos = new ArrayList<>();
        final List<Respuesta<List<UltimaVez>>> ultimaVez = new ArrayList<>();
        final List<Map<String, Object>> guardados = new ArrayList<>();
        final List<Respuesta<SesionEntrenamiento>> guardar = new ArrayList<>();

        @Override public void ejerciciosDeRutina(int rutinaId, Respuesta<List<RutinaEjercicio>> r) { ejercicios.add(r); }

        @Override public void ultimaVez(List<Integer> ids, Respuesta<List<UltimaVez>> r) {
            ultimaVezPedidos.add(ids);
            ultimaVez.add(r);
        }

        @Override public void guardarCompleta(Map<String, Object> cuerpo, Respuesta<SesionEntrenamiento> r) {
            guardados.add(cuerpo);
            guardar.add(r);
        }
    }

    private AlmacenSesion almacen;
    private RedFalsa red;
    private long ahora = 1_790_000_000_000L;
    private SesionEnCursoRepositorio repo;

    @Before
    public void preparar() throws Exception {
        almacen = new AlmacenSesion(tmp.newFolder("sesion_en_curso"));
        red = new RedFalsa();
        repo = nuevoProceso();
        repo.usarCuenta(7);
    }

    /** Otro proceso: repositorio nuevo sobre el mismo fichero, como tras matar la app. */
    private SesionEnCursoRepositorio nuevoProceso() {
        return new SesionEnCursoRepositorio(almacen, red, () -> ahora,
                TimeZone.getTimeZone("Europe/Madrid"), new Locale("es", "ES"));
    }

    private static RutinaEjercicio re(int ejercicioId, int series) {
        RutinaEjercicio r = new RutinaEjercicio();
        r.setEjercicioId(ejercicioId);
        r.setSeries(series);
        r.setRepeticiones(10);
        r.setNombreEjercicio("Ejercicio " + ejercicioId);
        return r;
    }

    private void empezarConRutina() {
        assertEquals(Empezar.EMPEZADA, repo.empezar(31, "Pierna B", "Torso y pierna"));
        red.ejercicios.get(0).ok(java.util.Arrays.asList(re(12, 2), re(13, 3)));
    }

    private long serie(int ejercicio, int indice) {
        return repo.actual().ejercicios.get(ejercicio).series.get(indice).id;
    }

    private static SesionEntrenamiento creada(int id) {
        SesionEntrenamiento s = new SesionEntrenamiento();
        s.setId(id);
        return s;
    }

    // ── Empezar ──────────────────────────────────────────────

    @Test
    public void empezar_pone_el_reloj_en_marcha_y_carga_la_rutina() {
        empezarConRutina();

        SesionEnCurso s = repo.actual();
        assertEquals(ahora, s.inicioMs);
        assertEquals(SesionEnCurso.Carga.LISTA, s.carga);
        assertEquals(2, s.ejercicios.size());
        assertEquals(3, s.ejercicios.get(1).series.size());
        assertEquals("pide la última vez de sus ejercicios", 1, red.ultimaVezPedidos.size());
        assertEquals(java.util.Arrays.asList(12, 13), red.ultimaVezPedidos.get(0));
    }

    @Test
    public void sin_rutina_empieza_vacia() {
        assertEquals(Empezar.EMPEZADA, repo.empezar(null, null, null));
        assertTrue(repo.actual().ejercicios.isEmpty());
        assertTrue(red.ejercicios.isEmpty());
    }

    @Test
    public void una_a_la_vez() {
        empezarConRutina();
        SesionEnCurso enCurso = repo.actual();

        assertEquals(Empezar.YA_HAY_OTRA, repo.empezar(40, "Torso A", null));
        assertEquals(Empezar.YA_HAY_OTRA, repo.empezar(null, null, null));
        assertEquals(Empezar.ES_LA_MISMA, repo.empezar(31, "Pierna B", null));
        assertSame("la que había sigue intacta", enCurso, repo.actual());

        repo.descartar();
        assertEquals(Empezar.EMPEZADA, repo.empezar(40, "Torso A", null));
    }

    // ── El fichero ───────────────────────────────────────────

    @Test
    public void sobrevive_a_que_android_cierre_la_app() {
        empezarConRutina();
        repo.escribirPeso(serie(0, 0), "57,");
        repo.marcar(serie(0, 1));

        SesionEnCursoRepositorio despues = nuevoProceso();
        despues.usarCuenta(7);

        SesionEnCurso s = despues.actual();
        assertNotNull(s);
        assertEquals("el reloj no se para: mismo inicio", ahora, s.inicioMs);
        assertEquals("57,", s.ejercicios.get(0).series.get(0).peso);
        assertTrue(s.ejercicios.get(0).series.get(1).hecha);
    }

    @Test
    public void otra_cuenta_en_el_mismo_movil_no_la_ve() {
        empezarConRutina();

        SesionEnCursoRepositorio otra = nuevoProceso();
        otra.usarCuenta(8);
        assertNull(otra.actual());
        assertEquals(Empezar.EMPEZADA, otra.empezar(null, null, null));

        repo.usarCuenta(8);
        repo.usarCuenta(7);
        assertEquals("Pierna B", repo.actual().rutinaNombre);
    }

    @Test
    public void descartar_la_borra_del_movil() {
        empezarConRutina();
        repo.descartar();

        assertNull(repo.actual());
        assertNull(almacen.leer(7));
    }

    // ── Editar ───────────────────────────────────────────────

    @Test
    public void hasta_veinte_series_por_ejercicio() {
        empezarConRutina();
        long ej = repo.actual().ejercicios.get(0).id;
        for (int i = 2; i < SesionEnCurso.MAX_SERIES; i++) assertTrue(repo.anadirSerie(ej));
        assertFalse(repo.anadirSerie(ej));
        assertEquals(SesionEnCurso.MAX_SERIES, repo.actual().ejercicios.get(0).series.size());
    }

    @Test
    public void anadir_ejercicio_entra_con_tres_series_vacias_y_pregunta_su_ultima_vez() {
        repo.empezar(null, null, null);
        repo.anadirEjercicios(Collections.singletonList(new SesionEnCursoRepositorio.Nuevo(55, "Remo")));

        SesionEnCurso.Ejercicio e = repo.actual().ejercicios.get(0);
        assertEquals(3, e.series.size());
        assertEquals("", e.series.get(0).repeticiones);
        assertFalse(e.deRutina);
        assertEquals(Collections.singletonList(55), red.ultimaVezPedidos.get(0));
    }

    @Test
    public void quitar_serie_y_ejercicio() {
        empezarConRutina();
        assertTrue(repo.quitarSerie(serie(1, 0)));
        assertEquals(2, repo.actual().ejercicios.get(1).series.size());
        assertTrue(repo.quitarEjercicio(repo.actual().ejercicios.get(0).id));
        assertEquals(1, repo.actual().ejercicios.size());
    }

    @Test
    public void deshacer_devuelve_la_serie_a_su_sitio() {
        empezarConRutina();
        repo.escribirRepeticiones(serie(1, 1), "7");
        long quitada = serie(1, 1);

        SesionEnCursoRepositorio.Quitada q = repo.quitarSerieDeshacible(quitada);
        assertEquals(2, repo.actual().ejercicios.get(1).series.size());
        assertTrue(repo.reponer(q));

        assertEquals(quitada, serie(1, 1));
        assertEquals("7", repo.actual().ejercicios.get(1).series.get(1).repeticiones);
    }

    @Test
    public void el_cronometro_apunta_los_segundos_y_marca() {
        repo.empezar(null, null, null);
        repo.anadirEjercicios(Collections.singletonList(new SesionEnCursoRepositorio.Nuevo(7, "Plancha")));
        repo.actual().ejercicios.get(0).medida = "SEGUNDOS";
        long s = serie(0, 0);

        assertTrue(repo.empezarCronometro(s));
        ahora += 47_500;
        assertTrue(repo.pararYApuntar());

        SesionEnCurso.Serie serie = repo.actual().ejercicios.get(0).series.get(0);
        assertEquals("0:47", serie.segundos);
        assertTrue(serie.hecha);
        assertNull(repo.actual().cronometro);
    }

    @Test
    public void cancelar_el_cronometro_no_apunta() {
        repo.empezar(null, null, null);
        repo.anadirEjercicios(Collections.singletonList(new SesionEnCursoRepositorio.Nuevo(7, "Plancha")));
        long s = serie(0, 0);
        repo.empezarCronometro(s);
        ahora += 30_000;
        repo.cancelarCronometro();

        assertEquals("", repo.actual().ejercicios.get(0).series.get(0).segundos);
        assertFalse(repo.actual().ejercicios.get(0).series.get(0).hecha);
    }

    // ── Guardar ──────────────────────────────────────────────

    @Test
    public void sin_nada_marcado_no_hay_nada_que_guardar() {
        empezarConRutina();
        assertEquals(Guardar.NADA_MARCADO, repo.guardar());
        assertTrue(red.guardados.isEmpty());
        assertTrue("sigue editable", repo.editable());
    }

    @Test
    public void desde_el_primer_intento_ya_no_se_edita() {
        empezarConRutina();
        repo.escribirRepeticiones(serie(0, 0), "10");
        repo.marcar(serie(0, 0));
        ahora += 43 * 60_000L;

        assertEquals(Guardar.ENVIADO, repo.guardar());
        red.guardar.get(0).fallo(-1, null);

        assertFalse(repo.editable());
        repo.escribirPeso(serie(0, 0), "99");
        repo.escribirRepeticiones(serie(0, 0), "1");
        assertNull(repo.marcar(serie(0, 1)));
        assertFalse(repo.anadirSerie(repo.actual().ejercicios.get(0).id));
        assertFalse(repo.quitarSerie(serie(1, 0)));
        assertFalse(repo.quitarEjercicio(repo.actual().ejercicios.get(1).id));
        repo.anadirEjercicios(Collections.singletonList(new SesionEnCursoRepositorio.Nuevo(99, "X")));

        SesionEnCurso s = repo.actual();
        assertEquals("", s.ejercicios.get(0).series.get(0).peso);
        assertEquals("10", s.ejercicios.get(0).series.get(0).repeticiones);
        assertFalse(s.ejercicios.get(0).series.get(1).hecha);
        assertEquals(2, s.ejercicios.size());
        assertEquals(SesionEnCurso.Guardado.FALLO, s.guardado);
        assertNotNull("sigue en el móvil", almacen.leer(7));
    }

    @Test
    public void el_reintento_manda_lo_mismo_con_la_misma_clave() {
        empezarConRutina();
        repo.escribirRepeticiones(serie(0, 0), "10");
        repo.marcar(serie(0, 0));
        ahora += 43 * 60_000L;
        repo.guardar();
        red.guardar.get(0).fallo(500, "x");

        ahora += 10 * 60_000L;
        assertEquals(Guardar.ENVIADO, repo.reintentar());

        assertEquals(2, red.guardados.size());
        assertEquals(red.guardados.get(0), red.guardados.get(1));
        assertEquals("la duración del primer intento", 43, red.guardados.get(1).get("duracionMinutos"));
    }

    @Test
    public void la_duracion_ajustada_manda_sobre_el_reloj() {
        empezarConRutina();
        repo.escribirRepeticiones(serie(0, 0), "10");
        repo.marcar(serie(0, 0));
        ahora += 43 * 60_000L;
        repo.ajustarDuracion(50);
        repo.guardar();

        assertEquals(50, red.guardados.get(0).get("duracionMinutos"));
    }

    @Test
    public void guardada_se_borra_del_movil_y_avisa_una_vez() {
        empezarConRutina();
        repo.escribirRepeticiones(serie(0, 0), "10");
        repo.marcar(serie(0, 0));
        repo.guardar();
        red.guardar.get(0).ok(creada(900));

        assertNull(repo.actual());
        assertNull(almacen.leer(7));
        SesionEnCursoRepositorio.Resultado r = repo.getResultado().getValue().tomar();
        assertTrue(r.ok());
        assertEquals("Pierna B", r.guardada.rutinaNombre);
        assertNull(repo.getResultado().getValue().tomar());
    }

    @Test
    public void el_reintento_al_abrir_va_una_vez_y_no_duplica() {
        empezarConRutina();
        repo.escribirRepeticiones(serie(0, 0), "10");
        repo.marcar(serie(0, 0));
        repo.guardar();
        red.guardar.get(0).fallo(-1, null);

        // Se cierra la app y se abre: un proceso nuevo lee el fichero.
        SesionEnCursoRepositorio abierta = nuevoProceso();
        abierta.usarCuenta(7);
        assertTrue(abierta.reintentarAlAbrir());
        red.guardar.get(1).fallo(-1, null);
        assertFalse("una sola vez por apertura", abierta.reintentarAlAbrir());

        assertEquals(2, red.guardados.size());
        assertEquals("misma clave: la API no crea otra",
                red.guardados.get(0).get("claveIdempotencia"), red.guardados.get(1).get("claveIdempotencia"));
        assertEquals(red.guardados.get(0), red.guardados.get(1));
    }

    @Test
    public void si_el_proceso_murio_guardando_al_volver_es_un_fallo_y_se_reintenta() {
        empezarConRutina();
        repo.escribirRepeticiones(serie(0, 0), "10");
        repo.marcar(serie(0, 0));
        repo.guardar();
        // Sin respuesta: el proceso muere con el envío en el aire.

        SesionEnCursoRepositorio abierta = nuevoProceso();
        abierta.usarCuenta(7);
        assertEquals(SesionEnCurso.Guardado.FALLO, abierta.actual().guardado);
        assertTrue(abierta.reintentarAlAbrir());
        assertEquals(red.guardados.get(0).get("claveIdempotencia"), red.guardados.get(1).get("claveIdempotencia"));
    }

    @Test
    public void sin_intento_previo_no_hay_reintento_al_abrir() {
        empezarConRutina();
        SesionEnCursoRepositorio abierta = nuevoProceso();
        abierta.usarCuenta(7);
        assertFalse(abierta.reintentarAlAbrir());
        assertTrue(red.guardados.isEmpty());
    }

    @Test
    public void una_respuesta_tardia_no_toca_la_sesion_nueva() {
        empezarConRutina();
        repo.escribirRepeticiones(serie(0, 0), "10");
        repo.marcar(serie(0, 0));
        repo.guardar();
        repo.descartar();
        repo.empezar(null, "Libre", null);

        red.guardar.get(0).ok(creada(900));

        assertNotNull(repo.actual());
        assertNotNull(almacen.leer(7));
    }
}
