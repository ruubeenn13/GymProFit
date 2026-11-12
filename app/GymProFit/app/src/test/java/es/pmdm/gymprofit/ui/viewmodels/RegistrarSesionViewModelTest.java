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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import es.pmdm.gymprofit.envivo.LogicaSesion;
import es.pmdm.gymprofit.envivo.SesionEnCursoRepositorio;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.envivo.UltimaVez;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.RegistroSesionRepositorio;
import es.pmdm.gymprofit.ui.viewmodels.RegistrarSesionViewModel.Carga;
import es.pmdm.gymprofit.ui.viewmodels.RegistrarSesionViewModel.Fase;
import es.pmdm.gymprofit.ui.viewmodels.RegistrarSesionViewModel.Porque;
import es.pmdm.gymprofit.ui.viewmodels.RegistrarSesionViewModel.ResultadoGuardar;

// ============================================================
// RegistrarSesionViewModelTest — «Apuntar entreno» no pierde lo marcado (GP-016, GP-241).
//
// El repositorio es un falso que guarda cada llamada y NO responde hasta que el test lo pide:
// así se ve qué pasa con una petición en vuelo, que es justo el caso de girar o de que Android
// mate el proceso mientras se guarda.
//
// «Matar el proceso» se simula como lo hace Android: lo que hay en el SavedStateHandle se
// serializa, se tira el ViewModel y se crea otro con un handle nuevo a partir de esos bytes.
// Si algo del borrador no fuera serializable, el test fallaría ahí.
// ============================================================
public class RegistrarSesionViewModelTest {

    @Rule
    public InstantTaskExecutorRule sincrono = new InstantTaskExecutorRule();

    private static final int RUTINA_PIERNA = 5;
    private static final int RUTINA_TORSO = 8;
    private static final Locale ES = new Locale("es", "ES");

    private static final TimeZone MADRID = TimeZone.getTimeZone("Europe/Madrid");
    // 2026-09-30 19:10:00 en Madrid.
    private static final long AHORA = 1_790_788_200_000L;

    // ── La fecha (GP-012: apuntar un entrenamiento hecho) ────────

    @Test
    public void la_fecha_es_hoy_por_defecto_y_el_inicio_ahora_menos_la_duracion() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = conReloj(red);
        listo(vm, red, RUTINA_PIERNA);

        assertEquals("2026-09-30", vm.getFecha());
        vm.guardar();
        assertEquals("19:10 menos 55 minutos", "2026-09-30T18:15:00", red.guardados.get(0).cuerpo.get("fechaInicio"));
    }

    @Test
    public void un_entrenamiento_de_ayer_empieza_ayer_a_la_hora_de_ahora_menos_la_duracion() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = conReloj(red);
        listo(vm, red, RUTINA_PIERNA);

        assertEquals("2026-09-29", vm.ayer());
        assertTrue(vm.setFecha(vm.ayer()));
        vm.guardar();
        assertEquals("2026-09-29T18:15:00", red.guardados.get(0).cuerpo.get("fechaInicio"));
    }

    @Test
    public void nunca_una_fecha_futura() {
        RegistrarSesionViewModel vm = conReloj(new Falso());
        assertFalse(vm.setFecha("2026-10-01"));
        assertEquals("2026-09-30", vm.getFecha());
    }

    @Test
    public void si_la_duracion_cruza_la_medianoche_empieza_el_dia_anterior() {
        // 2026-09-30 00:20 menos 45 minutos.
        assertEquals("2026-09-29T23:35:00",
                RegistrarSesionViewModel.fechaInicio("2026-09-30", 1_790_720_400_000L, 45, MADRID));
    }

    // ── La rutina de entrada: la que toca hoy ─────────────────

    @Test
    public void sin_rutina_de_entrada_se_pone_la_que_toca_hoy_con_su_duracion() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(null);
        assertFalse("hasta tener los datos no se sabe cuál", vm.rutinaDecidida());
        assertEquals(Carga.CARGANDO, vm.getDatos().getValue());

        // Pierna se hizo ayer y Torso hace una semana: toca Torso, la que más lleva sin hacerse.
        red.responderDatos(rutinas(), Arrays.asList(sesion(1, RUTINA_PIERNA, "2026-09-29T18:00:00", 70),
                sesion(2, RUTINA_TORSO, "2026-09-23T18:00:00", 60)), null);

        assertTrue(vm.rutinaDecidida());
        assertEquals(Integer.valueOf(RUTINA_TORSO), vm.getRutinaId());
        assertEquals(Porque.MAS_TIEMPO, vm.porque());
        assertEquals("la de la rutina (52 → 50)", 50, vm.getDuracion());
        assertEquals(Integer.valueOf(RUTINA_TORSO), red.ejercicios.get(0).rutinaId);
    }

    @Test
    public void con_programa_la_que_toca_es_la_del_programa() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(null);
        List<Rutina> lista = rutinas();
        lista.get(0).setProgramaUsuarioId(3);
        lista.get(0).setDuracionMinutos(0);   // las de un programa no traen sus minutos
        ProgramaQueSigue seguido = new ProgramaQueSigue();
        seguido.setMinutos(50);
        seguido.setPosicionHoy(0);
        es.pmdm.gymprofit.model.programa.RutinaConEjercicios hoy = new es.pmdm.gymprofit.model.programa.RutinaConEjercicios();
        hoy.setId(RUTINA_PIERNA);
        hoy.setNombre("Pierna");
        seguido.setRutinas(new ArrayList<>(Arrays.asList(hoy)));
        red.responderDatos(lista, new ArrayList<>(), seguido);

        assertEquals(Integer.valueOf(RUTINA_PIERNA), vm.getRutinaId());
        assertEquals(Porque.PROGRAMA, vm.porque());
        assertEquals("los minutos del programa que sigue", 50, vm.getDuracion());
    }

    @Test
    public void sin_rutinas_es_un_entreno_libre_con_la_duracion_del_ultimo_o_45() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(null);
        red.responderDatos(new ArrayList<>(), Arrays.asList(sesion(1, null, "2026-09-20T10:00:00", 30),
                sesion(2, null, "2026-09-28T10:00:00", 62)), null);

        assertTrue(vm.rutinaDecidida());
        assertNull(vm.getRutinaId());
        assertEquals("la del último entreno (62 → 60)", 60, vm.getDuracion());
        assertEquals(Porque.NINGUNO, vm.porque());

        Falso otra = new Falso();
        RegistrarSesionViewModel nuevo = new RegistrarSesionViewModel(new SavedStateHandle(), otra);
        nuevo.iniciar(null);
        otra.responderDatos(new ArrayList<>(), new ArrayList<>(), null);
        assertEquals(RegistrarSesionViewModel.DURACION_DEFECTO, nuevo.getDuracion());
    }

    @Test
    public void la_rutina_de_entrada_solo_vale_la_primera_vez() throws Exception {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(RUTINA_PIERNA);
        assertEquals("no espera a la que toca hoy", Integer.valueOf(RUTINA_PIERNA), vm.getRutinaId());
        vm.elegirRutina(null);   // el usuario la cambia a entreno libre

        RegistrarSesionViewModel restaurado = matarYRestaurar(vm, new Falso());
        restaurado.iniciar(RUTINA_PIERNA);   // el Intent sigue trayendo la de entrada

        assertNull("no pisa lo que eligió el usuario", restaurado.getRutinaId());
    }

    @Test
    public void si_fallan_los_datos_se_dice_y_reintentar_los_vuelve_a_pedir() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(null);
        red.delUsuario.get(0).respuesta.fallo(-1, "sin red");
        assertEquals(Carga.ERROR, vm.getDatos().getValue());

        vm.reintentar();
        assertEquals(2, red.delUsuario.size());
        assertEquals(Carga.CARGANDO, vm.getDatos().getValue());
    }

    // ── Girar y matar el proceso no pierde lo marcado ─────────

    @Test
    public void restaurar_devuelve_todo_lo_marcado_sin_volver_a_pedir_los_ejercicios() throws Exception {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        listo(vm, red, RUTINA_PIERNA);

        long s1 = serie(vm, 0, 0), s2 = serie(vm, 0, 1);
        vm.escribirPeso(s1, "72,5");
        vm.escribirRepeticiones(s1, "8");
        assertEquals(LogicaSesion.Marcado.HECHA, vm.marcar(s1, ES));
        vm.escribirPeso(s2, "75,");          // a medio teclear: se conserva el texto tal cual
        vm.sumarDuracion(5);
        vm.setNotas("Rodilla bien");
        vm.tocarCara(4);

        Falso redNueva = new Falso();
        RegistrarSesionViewModel restaurado = matarYRestaurar(vm, redNueva);
        restaurado.iniciar(RUTINA_PIERNA);

        assertEquals(Integer.valueOf(RUTINA_PIERNA), restaurado.getRutinaId());
        assertEquals(60, restaurado.getDuracion());
        assertEquals("Rodilla bien", restaurado.getNotas());
        assertEquals(Integer.valueOf(4), restaurado.getValoracion());
        SesionEnCurso.Ejercicio sentadilla = restaurado.getBorrador().ejercicios.get(0);
        assertTrue(sentadilla.series.get(0).hecha);
        assertEquals("72,5", sentadilla.series.get(0).peso);
        assertEquals("75,", sentadilla.series.get(1).peso);
        assertFalse(sentadilla.series.get(1).hecha);
        assertEquals("la última vez sigue ahí", 1, sentadilla.anterior.size());
        assertEquals(1, restaurado.marcadas());

        assertEquals("no se vuelven a pedir los ejercicios", 0, redNueva.ejercicios.size());
        assertEquals("ni su última vez", 0, redNueva.ultimas.size());
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

        assertEquals(RUTINA_TORSO * 100, vm.getBorrador().ejercicios.get(0).ejercicioId);
    }

    @Test
    public void cambiar_de_rutina_quita_lo_marcado_y_cuenta_cuanto_habia() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        listo(vm, red, RUTINA_PIERNA);
        long s = serie(vm, 0, 0);
        vm.escribirRepeticiones(s, "10");
        vm.marcar(s, ES);
        assertEquals("la pantalla pregunta con esto", 1, vm.marcadas());

        vm.elegirRutina(RUTINA_TORSO);
        red.responderEjercicios(1, ejercicios(RUTINA_TORSO));
        assertEquals(0, vm.marcadas());
    }

    @Test
    public void si_fallan_los_ejercicios_se_dice_y_reintentar_los_pide_otra_vez() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(RUTINA_PIERNA);
        red.ejercicios.get(0).respuesta.fallo(500, "boom");
        assertEquals(Carga.ERROR, vm.getCargaEjercicios().getValue());

        vm.reintentar();
        assertEquals(2, red.ejercicios.size());
        assertEquals(Carga.CARGANDO, vm.getCargaEjercicios().getValue());
    }

    // ── Marcar, la duración, las caras y «Todo como la última vez» ─

    @Test
    public void marcar_sin_repeticiones_usa_las_de_la_ultima_vez_y_sin_ella_no_marca() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        listo(vm, red, RUTINA_PIERNA);

        // Sentadilla tiene última vez (100 × 8) en su serie 1; Zancada no.
        long conPista = serie(vm, 0, 0);
        assertEquals(LogicaSesion.Marcado.HECHA, vm.marcar(conPista, ES));
        assertEquals("8", vm.getBorrador().ejercicios.get(0).series.get(0).repeticiones);
        assertEquals("100", vm.getBorrador().ejercicios.get(0).series.get(0).peso);

        assertEquals(LogicaSesion.Marcado.FALTAN_REPS, vm.marcar(serie(vm, 1, 0), ES));
        assertEquals(1, vm.marcadas());
    }

    @Test
    public void la_duracion_va_de_5_en_5_entre_5_y_300() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        listo(vm, red, RUTINA_PIERNA);
        assertEquals(55, vm.getDuracion());
        vm.sumarDuracion(5);
        assertEquals(60, vm.getDuracion());
        for (int i = 0; i < 100; i++) vm.sumarDuracion(-5);
        assertEquals(5, vm.getDuracion());
        for (int i = 0; i < 100; i++) vm.sumarDuracion(5);
        assertEquals(300, vm.getDuracion());
        assertEquals(45, RegistrarSesionViewModel.redondear(44));
        assertEquals(5, RegistrarSesionViewModel.redondear(1));
    }

    @Test
    public void todo_como_la_ultima_vez_marca_las_que_la_tuvieron_con_sus_valores() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        listo(vm, red, RUTINA_PIERNA);
        assertTrue(vm.hayUltimaVez());

        assertEquals("solo la serie 1 de la sentadilla tuvo última vez", 1, vm.comoLaUltimaVez(ES));
        SesionEnCurso.Serie s = vm.getBorrador().ejercicios.get(0).series.get(0);
        assertTrue(s.hecha);
        assertEquals("100", s.peso);
        assertEquals("8", s.repeticiones);
        assertFalse(vm.getBorrador().ejercicios.get(1).series.get(0).hecha);
        assertFalse("ya no queda nada que rellenar: el botón se va", vm.hayUltimaVez());
    }

    @Test
    public void tocar_la_cara_marcada_la_quita_y_sin_cara_no_viaja_la_valoracion() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        listo(vm, red, RUTINA_PIERNA);
        vm.tocarCara(5);
        assertEquals(Integer.valueOf(5), vm.getValoracion());
        vm.tocarCara(5);
        assertNull(vm.getValoracion());

        vm.guardar();
        assertFalse("sin tocar, NULL (GP-077)", red.guardados.get(0).cuerpo.containsKey("valoracion"));
    }

    @Test
    public void hay_datos_con_algo_marcado_la_nota_o_la_cara() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        listo(vm, red, RUTINA_PIERNA);
        assertFalse(vm.hayDatos());
        vm.escribirPeso(serie(vm, 0, 1), "90");
        assertFalse("lo que no se marca no se guarda: salir no lo pierde", vm.hayDatos());
        vm.tocarCara(3);
        assertTrue(vm.hayDatos());
        vm.tocarCara(3);
        vm.marcar(serie(vm, 0, 0), ES);
        assertTrue(vm.hayDatos());
    }

    @Test
    public void el_primero_va_abierto_los_demas_plegados_y_lo_empezado_abierto() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        listo(vm, red, RUTINA_PIERNA);
        SesionEnCurso.Ejercicio primero = vm.getBorrador().ejercicios.get(0);
        SesionEnCurso.Ejercicio segundo = vm.getBorrador().ejercicios.get(1);
        assertTrue(vm.abierto(primero));
        assertFalse(vm.abierto(segundo));

        vm.alternarPliegue(segundo.id);
        assertTrue(vm.abierto(segundo));
        vm.alternarPliegue(primero.id);
        assertFalse(vm.abierto(primero));
        vm.escribirRepeticiones(serie(vm, 0, 0), "8");
        vm.marcar(serie(vm, 0, 0), ES);
        assertTrue("empezado, siempre abierto", vm.abierto(primero));
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
    public void dos_toques_seguidos_en_guardar_dejan_una_sola_peticion() {
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
        assertEquals(Fase.GUARDANDO, vm.getEstadoGuardado().getValue().fase);

        SesionEntrenamiento creada = sesion(41);
        red.guardados.get(0).respuesta.ok(creada);
        RegistrarSesionViewModel.EstadoGuardado estado = vm.getEstadoGuardado().getValue();
        assertEquals(Fase.EXITO, estado.fase);
        assertSame(creada, estado.sesion);
        assertEquals("en éxito, otro toque no manda nada", ResultadoGuardar.YA_EN_CURSO, vm.guardar());

        vm.resumenAbierto();
        assertEquals(Fase.TERMINADO, vm.getEstadoGuardado().getValue().fase);
        assertEquals(1, red.guardados.size());
    }

    // ── El fallo lo conserva todo ─────────────────────────────

    @Test
    public void un_fallo_lo_conserva_todo_y_ahora_no_deja_volver_a_guardar() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = listoParaGuardar(red);
        long s = serie(vm, 0, 0);
        vm.escribirPeso(s, "80");
        vm.escribirRepeticiones(s, "6");
        vm.marcar(s, ES);
        vm.setNotas("Pesado");
        vm.tocarCara(3);
        vm.guardar();
        String clave = vm.getClave();

        red.guardados.get(0).respuesta.fallo(500, "boom");

        RegistrarSesionViewModel.EstadoGuardado estado = vm.getEstadoGuardado().getValue();
        assertEquals(Fase.FALLO, estado.fase);
        assertEquals(500, estado.codigo);
        assertFalse(estado.interrumpido);
        assertEquals("80", vm.getBorrador().ejercicios.get(0).series.get(0).peso);
        assertTrue(vm.getBorrador().ejercicios.get(0).series.get(0).hecha);
        assertEquals("Pesado", vm.getNotas());
        assertEquals(Integer.valueOf(3), vm.getValoracion());
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

    // ── El cuerpo: solo lo marcado ────────────────────────────

    @Test
    public void el_cuerpo_lleva_solo_lo_marcado() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = listoParaGuardar(red);
        long marcada = serie(vm, 0, 0), sinMarcar = serie(vm, 0, 1);
        vm.escribirPeso(marcada, "72,5");
        vm.escribirRepeticiones(marcada, "8");
        vm.marcar(marcada, ES);
        vm.escribirPeso(sinMarcar, "75");
        vm.escribirRepeticiones(sinMarcar, "8");   // escrita pero sin marcar: no viaja
        vm.tocarCara(5);
        vm.setNotas("  Bien  ");

        vm.guardar();
        Map<String, Object> cuerpo = red.guardados.get(0).cuerpo;

        assertEquals(RUTINA_PIERNA, cuerpo.get("rutinaId"));
        assertEquals(55, cuerpo.get("duracionMinutos"));
        assertEquals(5, cuerpo.get("valoracion"));
        assertEquals("Bien", cuerpo.get("notas"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ejercicios = (List<Map<String, Object>>) cuerpo.get("ejercicios");
        assertEquals("solo el ejercicio con algo marcado", 1, ejercicios.size());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> series = (List<Map<String, Object>>) ejercicios.get(0).get("series");
        assertEquals(1, series.size());
        assertEquals(new BigDecimal("72.5"), series.get(0).get("peso"));
        assertEquals(8, series.get(0).get("repeticiones"));
    }

    @Test
    public void sin_nada_marcado_se_guardan_el_dia_y_la_duracion() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = listoParaGuardar(red);
        assertEquals(ResultadoGuardar.ENVIADO, vm.guardar());
        Map<String, Object> cuerpo = red.guardados.get(0).cuerpo;
        assertEquals(55, cuerpo.get("duracionMinutos"));
        assertTrue(((List<?>) cuerpo.get("ejercicios")).isEmpty());
    }

    @Test
    public void una_serie_por_tiempo_marcada_manda_sus_segundos_con_cero_repeticiones() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(RUTINA_PIERNA);
        red.responderDatos(rutinas(), new ArrayList<>(), null);
        RutinaEjercicio plancha = ejercicio(900, "Plancha lateral", 2, 40);
        plancha.setMedida("SEGUNDOS");
        red.responderEjercicios(0, new ArrayList<>(Arrays.asList(plancha)));
        red.responderUltimas(new ArrayList<>());

        long s = serie(vm, 0, 0);
        vm.escribirSegundos(s, "35");
        assertEquals(LogicaSesion.Marcado.HECHA, vm.marcar(s, ES));
        vm.guardar();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ejercicios = (List<Map<String, Object>>) red.guardados.get(0).cuerpo.get("ejercicios");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> series = (List<Map<String, Object>>) ejercicios.get(0).get("series");
        assertEquals(1, series.size());
        assertEquals(35, series.get(0).get("segundos"));
        assertEquals(0, series.get(0).get("repeticiones"));
        assertFalse(series.get(0).containsKey("peso"));
    }

    // ── Entreno libre con ejercicios del catálogo ─────────────

    @Test
    public void un_entreno_libre_guarda_los_ejercicios_anadidos_sin_rutina() throws Exception {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(null);
        red.responderDatos(rutinas(), new ArrayList<>(), null);
        vm.elegirRutina(null);
        assertNull(vm.getRutinaId());
        assertTrue(vm.getBorrador().ejercicios.isEmpty());

        vm.anadirEjercicios(Arrays.asList(new SesionEnCursoRepositorio.Nuevo(31, "Curl de bíceps")));
        assertEquals(1, vm.getBorrador().ejercicios.size());
        assertEquals("con 3 series vacías", 3, vm.getBorrador().ejercicios.get(0).series.size());
        assertTrue("el primero, abierto", vm.abierto(vm.getBorrador().ejercicios.get(0)));
        assertEquals("pide su última vez", Arrays.asList(31), red.ultimas.get(red.ultimas.size() - 1).ids);

        long s = serie(vm, 0, 0);
        vm.escribirPeso(s, "15");
        vm.escribirRepeticiones(s, "10");
        vm.marcar(s, ES);
        RegistrarSesionViewModel restaurado = matarYRestaurar(vm, new Falso());
        restaurado.iniciar(null);
        assertNull("libre sigue libre", restaurado.getRutinaId());
        assertEquals(1, restaurado.marcadas());

        restaurado.guardar();
        Map<String, Object> cuerpo = ((Falso) restauradoRed).guardados.get(0).cuerpo;
        assertFalse(cuerpo.containsKey("rutinaId"));
        assertEquals(1, ((List<?>) cuerpo.get("ejercicios")).size());
    }

    // ── Apoyo ─────────────────────────────────────────────────

    // La red del último matarYRestaurar, para leer lo que mandó el restaurado.
    private static RegistroSesionRepositorio restauradoRed;

    private static RegistrarSesionViewModel conReloj(Falso red) {
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.ahora = () -> AHORA;
        vm.zona = MADRID;
        return vm;
    }

    // Abre la rutina, con los datos, sus ejercicios y la última vez ya llegados.
    private static void listo(RegistrarSesionViewModel vm, Falso red, int rutina) {
        vm.iniciar(rutina);
        red.responderDatos(rutinas(), new ArrayList<>(), null);
        red.responderEjercicios(red.ejercicios.size() - 1, ejercicios(rutina));
        UltimaVez u = new UltimaVez();
        u.ejercicioId = rutina * 100;
        u.fecha = "2026-09-25T18:00:00";
        UltimaVez.Serie s = new UltimaVez.Serie();
        s.numero = 1;
        s.peso = new BigDecimal("100");
        s.repeticiones = 8;
        u.series = new ArrayList<>(Arrays.asList(s));
        red.responderUltimas(new ArrayList<>(Arrays.asList(u)));
    }

    private static RegistrarSesionViewModel listoParaGuardar(Falso red) {
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        listo(vm, red, RUTINA_PIERNA);
        return vm;
    }

    private static long serie(RegistrarSesionViewModel vm, int ejercicio, int serie) {
        return vm.getBorrador().ejercicios.get(ejercicio).series.get(serie).id;
    }

    /**
     * Lo que hace Android al matar el proceso: el contenido del SavedStateHandle pasa por bytes y
     * vuelve en un handle nuevo, para un ViewModel nuevo.
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
        restauradoRed = red;
        return new RegistrarSesionViewModel(new SavedStateHandle(leido), red);
    }

    // Pierna (55 min) y Torso (52 min).
    private static List<Rutina> rutinas() {
        List<Rutina> lista = new ArrayList<>();
        lista.add(rutina(RUTINA_PIERNA, "Pierna", 55));
        lista.add(rutina(RUTINA_TORSO, "Torso", 52));
        return lista;
    }

    private static Rutina rutina(int id, String nombre, int minutos) {
        Rutina r = new Rutina();
        r.setId(id);
        r.setNombre(nombre);
        r.setDuracionMinutos(minutos);
        return r;
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

    private static SesionEntrenamiento sesion(int id, Integer rutina, String inicio, int minutos) {
        SesionEntrenamiento s = sesion(id);
        s.setRutinaId(rutina);
        s.setFechaInicio(inicio);
        s.setDuracionMinutos(minutos);
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
            final List<Integer> ids;
            final Respuesta<T> respuesta;

            Llamada(Integer rutinaId, Map<String, Object> cuerpo, List<Integer> ids, Respuesta<T> respuesta) {
                this.rutinaId = rutinaId;
                this.cuerpo = cuerpo;
                this.ids = ids;
                this.respuesta = respuesta;
            }
        }

        final List<Llamada<List<Rutina>>> delUsuario = new ArrayList<>();
        final List<Llamada<List<SesionEntrenamiento>>> sesiones = new ArrayList<>();
        final List<Llamada<ProgramaQueSigue>> seguidos = new ArrayList<>();
        final List<Llamada<List<RutinaEjercicio>>> ejercicios = new ArrayList<>();
        final List<Llamada<List<UltimaVez>>> ultimas = new ArrayList<>();
        final List<Llamada<Map<Integer, SesionEnCurso.Marca>>> marcas = new ArrayList<>();
        final List<Llamada<SesionEntrenamiento>> guardados = new ArrayList<>();

        @Override public void rutinasDelUsuario(Respuesta<List<Rutina>> r) { delUsuario.add(new Llamada<>(null, null, null, r)); }
        @Override public void ejerciciosDeRutina(int id, Respuesta<List<RutinaEjercicio>> r) { ejercicios.add(new Llamada<>(id, null, null, r)); }
        @Override public void programaSeguido(Respuesta<ProgramaQueSigue> r) { seguidos.add(new Llamada<>(null, null, null, r)); }
        @Override public void sesiones(Respuesta<List<SesionEntrenamiento>> r) { sesiones.add(new Llamada<>(null, null, null, r)); }
        @Override public void ultimaVez(List<Integer> ids, Respuesta<List<UltimaVez>> r) { ultimas.add(new Llamada<>(null, null, ids, r)); }
        @Override public void mejoresMarcas(List<Integer> ids, Respuesta<Map<Integer, SesionEnCurso.Marca>> r) { marcas.add(new Llamada<>(null, null, ids, r)); }
        @Override public void guardarCompleta(Map<String, Object> c, Respuesta<SesionEntrenamiento> r) { guardados.add(new Llamada<>(null, c, null, r)); }

        void responderDatos(List<Rutina> rutinas, List<SesionEntrenamiento> lista, ProgramaQueSigue seguido) {
            delUsuario.get(delUsuario.size() - 1).respuesta.ok(rutinas);
            sesiones.get(sesiones.size() - 1).respuesta.ok(lista);
            if (seguido == null) seguidos.get(seguidos.size() - 1).respuesta.fallo(404, "");
            else seguidos.get(seguidos.size() - 1).respuesta.ok(seguido);
        }

        void responderEjercicios(int llamada, List<RutinaEjercicio> lista) {
            ejercicios.get(llamada).respuesta.ok(lista);
        }

        void responderUltimas(List<UltimaVez> lista) {
            ultimas.get(ultimas.size() - 1).respuesta.ok(lista);
        }
    }

    // ── GP-130 ───────────────────────────────────────────────

    @Test
    public void en_un_entreno_libre_la_plancha_anadida_va_por_tiempo() {
        Falso red = new Falso();
        RegistrarSesionViewModel vm = new RegistrarSesionViewModel(new SavedStateHandle(), red);
        vm.iniciar(null);
        red.responderDatos(rutinas(), new ArrayList<>(), null);
        vm.elegirRutina(null);

        vm.anadirEjercicios(Arrays.asList(new SesionEnCursoRepositorio.Nuevo(7, "Plancha", "SEGUNDOS")));

        SesionEnCurso.Ejercicio plancha = vm.getBorrador().ejercicios.get(0);
        assertEquals("SEGUNDOS", plancha.medidaEjercicio);
        assertTrue(es.pmdm.gymprofit.envivo.LogicaSesion.porTiempo(plancha));
    }
}
