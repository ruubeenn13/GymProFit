package es.pmdm.gymprofit.envivo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import es.pmdm.gymprofit.envivo.CronometroSerie.Aviso;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;

// ============================================================
// CronometroSerieTest — el cronómetro de las series por tiempo (GP-012): cuenta desde
// que se tocó, una vibración corta al mínimo, una doble al máximo, cada una una vez, y
// a la hora se para solo.
// ============================================================
public class CronometroSerieTest {

    private static SesionEnCurso.Cronometro desde(long inicioMs) {
        SesionEnCurso.Cronometro c = new SesionEnCurso.Cronometro();
        c.inicioMs = inicioMs;
        return c;
    }

    private static long s(int segundos) { return segundos * 1000L; }

    @Test
    public void corta_al_minimo_y_doble_al_maximo_una_vez_cada_una() {
        SesionEnCurso.Cronometro c = desde(0);

        assertEquals(Aviso.NINGUNO, CronometroSerie.comprobar(c, s(29), 30, 60));
        assertEquals(Aviso.VIBRAR_CORTA, CronometroSerie.comprobar(c, s(30), 30, 60));
        assertEquals("no se repite", Aviso.NINGUNO, CronometroSerie.comprobar(c, s(31), 30, 60));
        assertEquals(Aviso.VIBRAR_DOBLE, CronometroSerie.comprobar(c, s(60), 30, 60));
        assertEquals(Aviso.NINGUNO, CronometroSerie.comprobar(c, s(90), 30, 60));
    }

    @Test
    public void sin_rango_solo_la_doble() {
        SesionEnCurso.Cronometro c = desde(0);
        assertEquals(Aviso.NINGUNO, CronometroSerie.comprobar(c, s(44), 45, 45));
        assertEquals(Aviso.VIBRAR_DOBLE, CronometroSerie.comprobar(c, s(45), 45, 45));
    }

    @Test
    public void al_volver_pasado_el_maximo_no_da_la_corta_atrasada() {
        SesionEnCurso.Cronometro c = desde(0);
        assertEquals(Aviso.VIBRAR_DOBLE, CronometroSerie.comprobar(c, s(75), 30, 60));
        assertEquals(Aviso.NINGUNO, CronometroSerie.comprobar(c, s(76), 30, 60));
    }

    @Test
    public void sigue_contando_desde_cuando_empezo() {
        SesionEnCurso.Cronometro c = desde(1_000_000L);
        assertEquals(125, CronometroSerie.segundos(c, 1_000_000L + s(125)));
    }

    @Test
    public void a_la_hora_se_para() {
        SesionEnCurso.Cronometro c = desde(0);
        assertEquals(Aviso.PARAR, CronometroSerie.comprobar(c, s(3600), 30, 60));
    }

    @Test
    public void sin_pauta_no_vibra() {
        SesionEnCurso.Cronometro c = desde(0);
        assertEquals(Aviso.NINGUNO, CronometroSerie.comprobar(c, s(120), null, null));
    }
}
