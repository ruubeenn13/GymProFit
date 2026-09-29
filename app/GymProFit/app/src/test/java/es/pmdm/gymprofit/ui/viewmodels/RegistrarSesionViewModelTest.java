package es.pmdm.gymprofit.ui.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.RegistroSesionRepositorio;
import es.pmdm.gymprofit.ui.adapters.EjercicioPesoAdapter;
import es.pmdm.gymprofit.ui.viewmodels.RegistrarSesionViewModel.Fase;
import es.pmdm.gymprofit.ui.viewmodels.RegistrarSesionViewModel.ResultadoGuardar;

// ============================================================
// RegistrarSesionViewModelTest — GP-016: lo tecleado en Registrar sesión no se pierde.
//
// El repositorio es un falso que guarda cada llamada y NO responde hasta que el test
// lo pide: así se ve qué pasa con una petición en vuelo, que es justo el caso de girar
// o de que Android mate el proceso mientras se guarda.
//
// «Matar el proceso» se simula como lo hace Android: lo que hay en el SavedStateHandle
// se serializa, se tira el ViewModel y se crea otro con un handle nuevo a partir de esos
// bytes. Si algo del borrador no fuera serializable, el test fallaría ahí.
// ============================================================
public class RegistrarSesionViewModelTest {

    @Rule
    public InstantTaskExecutorRule sincrono = new InstantTaskExecutorRule();

    private static final int RUTINA_PIERNA = 5;
    private static final int RUTINA_TORSO = 8;

    // ── Restaurar ─────────────────────────────────────────────

    @Test
    public void restaurar_devuelve_todo_lo_tecleado_sin_volver_a_pedir_los_ejercicios() throws Exception {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(RUTINA_PIERNA);
        red.responderRutinas();
        red.responderEjercicios(0, ejercicios(RUTINA_PIERNA));

        vm.peso(0, 1, "72,");            // a medio teclear: se conserva el texto tal cual
        vm.repeticiones(0, 2, "7");
        vm.alternarHecha(1, 0);
        vm.setDuracion("45");
        vm.setNotas("Rodilla bien");
        vm.setValoracion(4f);

        Falso redNueva = new Falso();
        RegistrarSesionViewModel restaurado = matarYRestaurar(vm, redNueva);
        restaurado.iniciar(RUTINA_PIERNA);

        assertEquals(Integer.valueOf(RUTINA_PIERNA), restaurado.getRutinaId());
        assertEquals("45", restaurado.getDuracion());
        assertEquals("Rodilla bien", restaurado.getNotas());
        assertEquals(4f, restaurado.getValoracion(), 0f);

        List<EjercicioPesoAdapter.Item> items = restaurado.getEjercicios().getValue();
        assertNotNull(items);
        assertEquals(2, items.size());
        assertEquals("72,", items.get(0).realizadas.get(1).peso);
        assertTrue("escribir un peso la marca hecha", items.get(0).realizadas.get(1).completada);
        assertEquals("7", items.get(0).realizadas.get(2).repeticiones);
        assertTrue(items.get(1).realizadas.get(0).completada);

        assertEquals("no se vuelven a pedir los ejercicios", 0, redNueva.ejercicios.size());
        assertEquals("la lista de rutinas sí se vuelve a pedir", 1, redNueva.delUsuario.size());
    }

    @Test
    public void restaurar_con_los_ejercicios_a_medio_pedir_los_vuelve_a_pedir() throws Exception {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(RUTINA_PIERNA);
        assertEquals(1, red.ejercicios.size());   // se murió antes de que llegaran

        Falso redNueva = new Falso();
        RegistrarSesionViewModel restaurado = matarYRestaurar(vm, redNueva);
        restaurado.iniciar(RUTINA_PIERNA);

        assertEquals(1, redNueva.ejercicios.size());
        assertEquals(Integer.valueOf(RUTINA_PIERNA), redNueva.ejercicios.get(0).rutinaId);
    }

    @Test
    public void la_rutina_de_entrada_solo_vale_la_primera_vez() throws Exception {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(RUTINA_PIERNA);
        vm.elegirRutina(null);   // el usuario la cambia a entrenamiento libre

        RegistrarSesionViewModel restaurado = matarYRestaurar(vm, new Falso());
        restaurado.iniciar(RUTINA_PIERNA);   // el Intent sigue trayendo la de entrada

        assertNull("no pisa lo que eligió el usuario", restaurado.getRutinaId());
    }

    // ── Cambiar de rutina ─────────────────────────────────────

    @Test
    public void cambiar_de_rutina_a_mano_recarga_y_la_respuesta_vieja_no_pisa_la_nueva() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(RUTINA_PIERNA);
        vm.elegirRutina(RUTINA_TORSO);

        assertEquals(2, red.ejercicios.size());
        red.responderEjercicios(1, ejercicios(RUTINA_TORSO));
        red.responderEjercicios(0, ejercicios(RUTINA_PIERNA));   // llega tarde

        assertEquals(RUTINA_TORSO * 100, vm.getEjercicios().getValue().get(0).ejercicioId);
    }

    @Test
    public void elegir_la_misma_rutina_ya_cargada_no_la_pide_otra_vez() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(RUTINA_PIERNA);
        red.responderEjercicios(0, ejercicios(RUTINA_PIERNA));
        vm.peso(0, 0, "60");

        vm.elegirRutina(RUTINA_PIERNA);

        assertEquals(1, red.ejercicios.size());
        assertEquals("60", vm.getEjercicios().getValue().get(0).realizadas.get(0).peso);
    }

    @Test
    public void si_fallan_los_ejercicios_se_avisa_una_vez_y_se_puede_volver_a_elegir() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(RUTINA_PIERNA);
        red.ejercicios.get(0).respuesta.fallo(-1, "timeout");

        RegistrarSesionViewModel.Evento<RegistrarSesionViewModel.Fallo> evento = vm.getErrorEjercicios().getValue();
        assertNotNull(evento);
        assertEquals(-1, evento.tomar().codigo);
        assertNull("un evento se consume una sola vez", evento.tomar());
        assertTrue(vm.getEjercicios().getValue().isEmpty());

        vm.elegirRutina(RUTINA_PIERNA);
        assertEquals(2, red.ejercicios.size());
    }

    // ── La clave del intento ──────────────────────────────────

    @Test
    public void la_clave_nace_al_guardar_y_el_reintento_la_reusa() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = listoParaGuardar(red);
        assertNull("no hay clave hasta pulsar Guardar", vm.getClave());

        assertEquals(ResultadoGuardar.ENVIADO, vm.guardar());
        String clave = claveDe(red.guardados.get(0).cuerpo);
        assertNotNull(clave);
        red.guardados.get(0).respuesta.fallo(-1, "sin red");

        vm.guardar();
        assertEquals(clave, claveDe(red.guardados.get(1).cuerpo));
    }

    @Test
    public void la_clave_sobrevive_a_matar_el_proceso() throws Exception {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = listoParaGuardar(red);
        vm.guardar();
        String clave = claveDe(red.guardados.get(0).cuerpo);
        red.guardados.get(0).respuesta.fallo(503, "");

        Falso redNueva = new Falso();
        RegistrarSesionViewModel restaurado = matarYRestaurar(vm, redNueva);
        restaurado.iniciar(RUTINA_PIERNA);
        restaurado.guardar();

        assertEquals(clave, claveDe(redNueva.guardados.get(0).cuerpo));
    }

    @Test
    public void tras_guardar_bien_el_siguiente_intento_lleva_otra_clave() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = listoParaGuardar(red);
        vm.guardar();
        String primera = claveDe(red.guardados.get(0).cuerpo);
        red.guardados.get(0).respuesta.ok(sesion(41));

        assertNull(vm.getClave());
        vm.resumenAbierto();
        vm.guardar();
        assertNotEquals(primera, claveDe(red.guardados.get(1).cuerpo));
    }

    // ── Doble envío ───────────────────────────────────────────

    @Test
    public void un_doble_toque_manda_una_sola_peticion() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = listoParaGuardar(red);

        assertEquals(ResultadoGuardar.ENVIADO, vm.guardar());
        assertEquals(ResultadoGuardar.YA_EN_CURSO, vm.guardar());

        assertEquals(1, red.guardados.size());
        assertEquals(Fase.GUARDANDO, vm.getEstadoGuardado().getValue().fase);
    }

    @Test
    public void el_exito_llega_una_vez_y_despues_de_abrir_el_resumen_ya_no_se_repite() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = listoParaGuardar(red);
        vm.guardar();
        // Girar con el guardado en marcha no crea otro ViewModel: la Activity nueva
        // observa el mismo estado, que sigue en GUARDANDO, y no manda nada.
        assertEquals(Fase.GUARDANDO, vm.getEstadoGuardado().getValue().fase);

        SesionEntrenamiento creada = sesion(41);
        red.guardados.get(0).respuesta.ok(creada);
        RegistrarSesionViewModel.EstadoGuardado estado = vm.getEstadoGuardado().getValue();
        assertEquals(Fase.EXITO, estado.fase);
        assertSame(creada, estado.sesion);

        vm.resumenAbierto();
        assertEquals(Fase.TERMINADO, vm.getEstadoGuardado().getValue().fase);
        assertEquals(1, red.guardados.size());
    }

    // ── El fallo lo conserva todo ─────────────────────────────

    @Test
    public void un_fallo_lo_conserva_todo_y_ahora_no_deja_volver_a_guardar() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = listoParaGuardar(red);
        vm.peso(0, 0, "80");
        vm.setNotas("Pesado");
        vm.setValoracion(3f);
        vm.guardar();
        String clave = vm.getClave();

        red.guardados.get(0).respuesta.fallo(500, "boom");

        RegistrarSesionViewModel.EstadoGuardado estado = vm.getEstadoGuardado().getValue();
        assertEquals(Fase.FALLO, estado.fase);
        assertEquals(500, estado.codigo);
        assertFalse(estado.interrumpido);
        assertEquals("80", vm.getEjercicios().getValue().get(0).realizadas.get(0).peso);
        assertEquals("30", vm.getDuracion());
        assertEquals("Pesado", vm.getNotas());
        assertEquals(3f, vm.getValoracion(), 0f);
        assertEquals(Integer.valueOf(RUTINA_PIERNA), vm.getRutinaId());
        assertEquals(clave, vm.getClave());

        vm.descartarFallo();   // «Ahora no»
        assertEquals(Fase.INACTIVO, vm.getEstadoGuardado().getValue().fase);
        assertEquals(clave, vm.getClave());
        assertEquals(ResultadoGuardar.ENVIADO, vm.guardar());
    }

    @Test
    public void una_respuesta_sin_sesion_es_un_fallo_y_no_un_exito() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = listoParaGuardar(red);
        vm.guardar();
        red.guardados.get(0).respuesta.ok(null);

        assertEquals(Fase.FALLO, vm.getEstadoGuardado().getValue().fase);
        assertNotNull(vm.getClave());
    }

    @Test
    public void si_el_proceso_muere_con_el_guardado_en_vuelo_se_ofrece_reintentar_con_la_misma_clave() throws Exception {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = listoParaGuardar(red);
        vm.guardar();
        String clave = vm.getClave();
        // No llega respuesta: Android mata el proceso con la petición en el aire.

        Falso redNueva = new Falso();
        RegistrarSesionViewModel restaurado = matarYRestaurar(vm, redNueva);
        restaurado.iniciar(RUTINA_PIERNA);

        RegistrarSesionViewModel.EstadoGuardado estado = restaurado.getEstadoGuardado().getValue();
        assertEquals("ni guardado ni perdido: se ofrece reintentar", Fase.FALLO, estado.fase);
        assertTrue(estado.interrumpido);
        assertEquals("no se reenvía solo", 0, redNueva.guardados.size());

        restaurado.guardar();
        assertEquals(clave, claveDe(redNueva.guardados.get(0).cuerpo));
    }

    // ── Validación y cuerpo ───────────────────────────────────

    @Test
    public void sin_duracion_valida_no_se_manda_nada_ni_nace_clave() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(null);

        assertEquals(ResultadoGuardar.FALTA_DURACION, vm.guardar());
        vm.setDuracion("0");
        assertEquals(ResultadoGuardar.DURACION_INVALIDA, vm.guardar());

        assertTrue(red.guardados.isEmpty());
        assertNull(vm.getClave());
    }

    @Test
    public void el_cuerpo_lleva_lo_tecleado_y_descarta_las_series_en_blanco() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = listoParaGuardar(red);
        vm.peso(0, 0, "72,5");
        vm.repeticiones(0, 1, "");      // serie 2 en blanco
        vm.setValoracion(5f);

        vm.guardar();
        Map<String, Object> cuerpo = red.guardados.get(0).cuerpo;

        assertEquals(RUTINA_PIERNA, cuerpo.get("rutinaId"));
        assertEquals(30, cuerpo.get("duracionMinutos"));
        assertEquals(5, cuerpo.get("valoracion"));
        assertFalse(cuerpo.containsKey("notas"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ejercicios = (List<Map<String, Object>>) cuerpo.get("ejercicios");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> series = (List<Map<String, Object>>) ejercicios.get(0).get("series");
        assertEquals(new java.math.BigDecimal("72.5"), series.get(0).get("peso"));
        assertEquals(true, series.get(0).get("completada"));
        assertEquals("la serie en blanco no viaja", 2, series.size());
    }

    // ── Apoyo ─────────────────────────────────────────────────

    // Rutina de pierna con los ejercicios ya cargados y una duración puesta.
    private static RegistrarSesionViewModel listoParaGuardar(Falso red) {
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(RUTINA_PIERNA);
        red.responderRutinas();
        red.responderEjercicios(0, ejercicios(RUTINA_PIERNA));
        vm.setDuracion("30");
        return vm;
    }

    /**
     * Lo que hace Android al matar el proceso: el contenido del SavedStateHandle pasa por
     * bytes y vuelve en un handle nuevo, para un ViewModel nuevo.
     */
    @SuppressWarnings("unchecked")
    private static RegistrarSesionViewModel matarYRestaurar(RegistrarSesionViewModel vm, Falso red) throws Exception {
        HashMap<String, Object> guardado = new HashMap<>();
        for (String clave : vm.handle().keys()) guardado.put(clave, vm.handle().get(clave));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(guardado);
        }
        Map<String, Object> leido;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            leido = (Map<String, Object>) in.readObject();
        }
        return new RegistrarSesionViewModel(new SavedStateHandle(leido), red);
    }

    // Dos ejercicios por rutina: 3x10 y 2x12, con ids derivados de la rutina.
    private static List<RutinaEjercicio> ejercicios(int rutinaId) {
        List<RutinaEjercicio> lista = new ArrayList<>();
        lista.add(ejercicio(rutinaId * 100, "Sentadilla", 3, 10));
        lista.add(ejercicio(rutinaId * 100 + 1, "Zancada", 2, 12));
        return lista;
    }

    private static RutinaEjercicio ejercicio(int id, String nombre, int series, int reps) {
        RutinaEjercicio re = new RutinaEjercicio();
        re.setEjercicioId(id);
        re.setNombreEjercicio(nombre);
        re.setSeries(series);
        re.setRepeticiones(reps);
        return re;
    }

    private static SesionEntrenamiento sesion(int id) {
        SesionEntrenamiento s = new SesionEntrenamiento();
        s.setId(id);
        return s;
    }

    private static String claveDe(Map<String, Object> cuerpo) {
        return (String) cuerpo.get("claveIdempotencia");
    }

    /** Repositorio falso: apunta cada llamada y responde solo cuando el test lo pide. */
    static class Falso implements RegistroSesionRepositorio {

        static class Llamada<T> {
            final Integer rutinaId;
            final Map<String, Object> cuerpo;
            final Respuesta<T> respuesta;

            Llamada(Integer rutinaId, Map<String, Object> cuerpo, Respuesta<T> respuesta) {
                this.rutinaId = rutinaId;
                this.cuerpo = cuerpo;
                this.respuesta = respuesta;
            }
        }

        final List<Llamada<List<Rutina>>> delUsuario = new ArrayList<>();
        final List<Llamada<List<RutinaEjercicio>>> ejercicios = new ArrayList<>();
        final List<Llamada<SesionEntrenamiento>> guardados = new ArrayList<>();

        @Override public void rutinasDelUsuario(Respuesta<List<Rutina>> r) { delUsuario.add(new Llamada<>(null, null, r)); }
        @Override public void ejerciciosDeRutina(int id, Respuesta<List<RutinaEjercicio>> r) { ejercicios.add(new Llamada<>(id, null, r)); }
        @Override public void guardarCompleta(Map<String, Object> c, Respuesta<SesionEntrenamiento> r) { guardados.add(new Llamada<>(null, c, r)); }

        void responderRutinas() {
            List<Rutina> lista = new ArrayList<>();
            for (int id : new int[]{RUTINA_PIERNA, RUTINA_TORSO}) {
                Rutina r = new Rutina();
                r.setId(id);
                r.setNombre("Rutina " + id);
                lista.add(r);
            }
            delUsuario.get(delUsuario.size() - 1).respuesta.ok(lista);
        }

        void responderEjercicios(int llamada, List<RutinaEjercicio> lista) {
            ejercicios.get(llamada).respuesta.ok(lista);
        }
    }
}
