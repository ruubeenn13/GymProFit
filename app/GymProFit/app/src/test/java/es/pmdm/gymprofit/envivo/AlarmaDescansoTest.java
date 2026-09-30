package es.pmdm.gymprofit.envivo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;

// ============================================================
// AlarmaDescansoTest — la alarma del fin del descanso (GP-013), con el AlarmManager
// detrás de una interfaz: exacta con permiso, ventana sin él; puesta al empezar el
// descanso, movida con ±15 s o al empezar otro, y quitada al saltarlo, al terminar la
// sesión, al descartarla o al cambiar de cuenta. Con el permiso recién dado, se rehace
// exacta.
// ============================================================
public class AlarmaDescansoTest {

    @Rule public InstantTaskExecutorRule instantaneo = new InstantTaskExecutorRule();
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    /** AlarmManager falso: apunta cada llamada. */
    static final class AlarmasFalsas implements ProgramadorAlarma.Alarmas {
        boolean permiso = true;
        final List<String> llamadas = new ArrayList<>();

        @Override public boolean puedeExactas() { return permiso; }
        @Override public void exacta(long cuandoMs) { llamadas.add("exacta " + cuandoMs); }
        @Override public void ventana(long cuandoMs, long largoMs) { llamadas.add("ventana " + cuandoMs); }
        @Override public void cancelar() { llamadas.add("cancelar"); }

        String ultima() { return llamadas.isEmpty() ? null : llamadas.get(llamadas.size() - 1); }
    }

    private SesionEnCursoRepositorioTest.RedFalsa red;
    private AlarmasFalsas alarmas;
    private long ahora = 1_790_000_000_000L;
    private SesionEnCursoRepositorio repo;

    @Before
    public void preparar() throws Exception {
        red = new SesionEnCursoRepositorioTest.RedFalsa();
        alarmas = new AlarmasFalsas();
        repo = new SesionEnCursoRepositorio(new AlmacenSesion(tmp.newFolder("s")), red, () -> ahora,
                TimeZone.getTimeZone("Europe/Madrid"), new Locale("es", "ES"));
        repo.setProgramador(new ProgramadorAlarma(alarmas));
        repo.usarCuenta(7);
        repo.empezar(31, "Pierna B", null);
        RutinaEjercicio re = new RutinaEjercicio();
        re.setEjercicioId(12);
        re.setSeries(3);
        re.setRepeticiones(10);
        re.setTiempoDescanso(120);
        red.ejercicios.get(0).ok(Arrays.asList(re));
    }

    private long serie(int i) {
        return repo.actual().ejercicios.get(0).series.get(i).id;
    }

    private void hacer(int i) {
        repo.escribirRepeticiones(serie(i), "10");
        repo.marcar(serie(i));
    }

    @Test
    public void con_permiso_exacta_a_su_hora() {
        hacer(0);

        assertEquals("exacta " + (ahora + 120_000), alarmas.ultima());
    }

    @Test
    public void sin_permiso_una_ventana() {
        alarmas.permiso = false;
        hacer(0);

        assertEquals("ventana " + (ahora + 120_000), alarmas.ultima());
    }

    @Test
    public void mas_quince_la_mueve_y_escribir_no_la_toca() {
        hacer(0);
        repo.ajustarDescanso(15);
        assertEquals("exacta " + (ahora + 135_000), alarmas.ultima());

        int antes = alarmas.llamadas.size();
        repo.escribirPeso(serie(1), "60");
        repo.cerrarFinDescanso();
        assertEquals("nada que cambiar, nada que llamar", antes, alarmas.llamadas.size());
    }

    @Test
    public void empezar_otro_la_sustituye() {
        hacer(0);
        ahora += 40_000;
        hacer(1);

        assertEquals("exacta " + (ahora + 120_000), alarmas.ultima());
    }

    @Test
    public void saltar_la_cancela() {
        hacer(0);
        repo.saltarDescanso();

        assertEquals("cancelar", alarmas.ultima());
    }

    @Test
    public void terminar_la_cancela() {
        hacer(0);
        repo.guardar();

        assertTrue(alarmas.llamadas.contains("cancelar"));
    }

    @Test
    public void descartar_y_cambiar_de_cuenta_la_cancelan() {
        hacer(0);
        repo.descartar();
        assertEquals("cancelar", alarmas.ultima());

        repo.empezar(31, "Pierna B", null);
        red.ejercicios.get(1).ok(java.util.Collections.singletonList(rutinaEjercicio()));
        hacer(0);
        repo.usarCuenta(8);
        assertEquals("cancelar", alarmas.ultima());
    }

    private static RutinaEjercicio rutinaEjercicio() {
        RutinaEjercicio re = new RutinaEjercicio();
        re.setEjercicioId(12);
        re.setSeries(3);
        re.setRepeticiones(10);
        re.setTiempoDescanso(120);
        return re;
    }

    @Test
    public void al_terminar_la_quita() {
        hacer(0);
        ahora += 120_500;
        repo.comprobarDescanso();

        assertEquals("cancelar", alarmas.ultima());
    }

    @Test
    public void con_el_permiso_recien_dado_se_rehace_exacta() {
        alarmas.permiso = false;
        hacer(0);
        assertEquals("ventana " + (ahora + 120_000), alarmas.ultima());

        alarmas.permiso = true;
        repo.revisarPermisoAlarma();

        assertEquals("exacta " + (ahora + 120_000), alarmas.ultima());
        int antes = alarmas.llamadas.size();
        repo.revisarPermisoAlarma();
        assertEquals("sin cambio, no se toca", antes, alarmas.llamadas.size());
    }

    @Test
    public void sin_descanso_revisar_el_permiso_no_pone_nada() {
        repo.revisarPermisoAlarma();

        assertNull(alarmas.ultima());
    }
}
