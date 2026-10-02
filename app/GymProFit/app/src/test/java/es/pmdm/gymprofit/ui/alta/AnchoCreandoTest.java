package es.pmdm.gymprofit.ui.alta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// AnchoCreandoTest — GP-163: el botón de «Guarda tu plan» vuelve entero tras un error.
//
// La animación de prueba se mueve a mano, fotograma a fotograma, y se comporta como un
// ValueAnimator: cancelada no pone nada más; acabada pone su valor final. Así se puede
// hacer que la API responda «al instante», a mitad de encoger, y dejar correr lo que
// queda de animación después de la respuesta, que es lo que dejaba el botón en círculo.
// ============================================================
public class AnchoCreandoTest {

    private static final int ENTERO = -1;
    private static final int BOTON = 900;
    private static final int CIRCULO = 156;

    private final List<Animacion> animaciones = new ArrayList<>();
    private int ancho;
    private AnchoCreando creando;

    /** Un ValueAnimator de juguete: avanza cuando el test lo dice. */
    private static final class Animacion implements AnchoCreando.EnMarcha {
        final int desde, hasta;
        final AnchoCreando.Ancho alPaso;
        boolean parada;

        Animacion(int desde, int hasta, AnchoCreando.Ancho alPaso) {
            this.desde = desde;
            this.hasta = hasta;
            this.alPaso = alPaso;
        }

        void fotograma(float t) {
            if (!parada) alPaso.poner(Math.round(desde + (hasta - desde) * t));
        }

        // Lo que queda de los 300 ms, a diez fotogramas.
        void correrHastaElFinal() {
            for (int i = 1; i <= 10; i++) fotograma(i / 10f);
            parada = true;
        }

        @Override
        public void cancelar() {
            parada = true;
        }

        @Override
        public void acabar() {
            fotograma(1f);
            parada = true;
        }
    }

    @Before
    public void preparar() {
        ancho = ENTERO;
        creando = new AnchoCreando(new AnchoCreando.Animador() {
            @NonNull
            @Override
            public AnchoCreando.EnMarcha animar(int desde, int hasta, @NonNull AnchoCreando.Ancho alPaso) {
                Animacion a = new Animacion(desde, hasta, alPaso);
                animaciones.add(a);
                return a;
            }
        }, px -> ancho = px, ENTERO);
    }

    @Test
    public void un_fallo_inmediato_deja_el_boton_entero_aunque_la_animacion_siga() {
        creando.encoger(BOTON, CIRCULO);
        Animacion a = animaciones.get(0);
        a.fotograma(0.1f);

        // «Correo en uso» llega antes de que el botón acabe de encoger.
        creando.volverEntero();
        a.correrHastaElFinal();

        assertEquals("el botón tiene que quedarse entero tras el error", ENTERO, ancho);
    }

    @Test
    public void un_fallo_antes_del_primer_fotograma_tambien_lo_deja_entero() {
        creando.encoger(BOTON, CIRCULO);
        creando.volverEntero();
        animaciones.get(0).correrHastaElFinal();

        assertEquals(ENTERO, ancho);
    }

    @Test
    public void un_fallo_con_la_animacion_ya_acabada_lo_deja_entero() {
        creando.encoger(BOTON, CIRCULO);
        animaciones.get(0).correrHastaElFinal();
        assertEquals(CIRCULO, ancho);

        creando.volverEntero();

        assertEquals(ENTERO, ancho);
    }

    @Test
    public void un_exito_inmediato_lo_deja_en_circulo_y_no_a_medias() {
        creando.encoger(BOTON, CIRCULO);
        Animacion a = animaciones.get(0);
        a.fotograma(0.2f);

        creando.quedarseEnCirculo();
        assertEquals("en círculo al salir bien, no a medio encoger", CIRCULO, ancho);
        a.correrHastaElFinal();

        assertEquals(CIRCULO, ancho);
        assertTrue(a.parada);
    }

    @Test
    public void el_reintento_tras_un_fallo_encoge_otra_vez_y_el_viejo_no_estorba() {
        creando.encoger(BOTON, CIRCULO);
        Animacion primera = animaciones.get(0);
        creando.volverEntero();

        creando.encoger(BOTON, CIRCULO);
        Animacion segunda = animaciones.get(1);
        segunda.fotograma(0.5f);
        primera.correrHastaElFinal();
        creando.volverEntero();
        segunda.correrHastaElFinal();

        assertEquals(ENTERO, ancho);
    }

    @Test
    public void irse_de_la_pantalla_para_la_animacion() {
        creando.encoger(BOTON, CIRCULO);
        Animacion a = animaciones.get(0);
        a.fotograma(0.3f);
        int alIrse = ancho;

        creando.parar();
        a.correrHastaElFinal();

        assertEquals(alIrse, ancho);
    }
}
