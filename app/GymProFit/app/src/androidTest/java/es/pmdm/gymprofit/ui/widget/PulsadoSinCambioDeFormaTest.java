package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// PulsadoSinCambioDeFormaTest — GP-285, lote 1c (DEC-070): en Liquid Glass ningún botón cambia
// de forma al pulsarlo. El principal de Inicio («Empezar») y el «+» de la barra se hunden a 0,94
// y siguen con su forma: una píldora y un círculo.
//
// Se mira lo que se pinta, no un radio: el botón se dibuja en una imagen en reposo y pulsado, y
// se cuentan los píxeles macizos de su esquina de arriba a la izquierda (un cuadrado del lado
// de medio alto). Si las esquinas pasan a ser más cerradas (el cambio de forma de los botones
// expresivos, DEC-039: de 26 a 12 dp), la esquina se llena y la cuenta sube. Se pulsa con el
// dedo y además con setPressed: dentro de algo que se desplaza Android retrasa el pulsado del
// toque. Con «Quitar animaciones» no se hunde.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class PulsadoSinCambioDeFormaTest {

    // Lo que dura el movimiento del toque, con margen.
    private static final long ESPERA = Movimiento.TOQUE * 3;

    private static Context tema() {
        return new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_GymProFit);
    }

    @Test
    public void el_principal_de_inicio_pulsado_no_cambia_de_forma() {
        View[] boton = new View[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context c = tema();
            FrameLayout raiz = new FrameLayout(c);
            View inicio = LayoutInflater.from(c).inflate(R.layout.fragment_inicio, raiz, false);
            boton[0] = inicio.findViewById(R.id.btnHoyPrincipal);
            ((android.view.ViewGroup) boton[0].getParent()).removeView(boton[0]);
            colocar(boton[0], Math.round(240 * c.getResources().getDisplayMetrics().density));
        });
        comprobar(boton[0], "«Empezar»");
    }

    @Test
    public void el_mas_pulsado_no_cambia_de_forma() {
        View[] boton = new View[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context c = tema();
            BarraNavegacion barra = new BarraNavegacion(c);
            int ancho = Math.round(360 * c.getResources().getDisplayMetrics().density);
            barra.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            barra.layout(0, 0, ancho, barra.getMeasuredHeight());
            boton[0] = barra.findViewById(R.id.btnAcciones);
        });
        comprobar(boton[0], "el «+»");
    }

    // Un botón suelto, medido a ese ancho y a su alto.
    private static void colocar(View v, int ancho) {
        v.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        v.layout(0, 0, ancho, v.getMeasuredHeight());
    }

    private static void comprobar(View boton, String nombre) {
        Context c = boton.getContext();
        int[] reposo = new int[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            reposo[0] = esquina(boton);
            assertTrue(nombre + ": la esquina tiene algo pintado", reposo[0] > 0);
            tocar(boton, MotionEvent.ACTION_DOWN);
            boton.setPressed(true);
        });
        SystemClock.sleep(ESPERA);
        boolean quieto = Movimiento.quieto(c);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            int pulsado = esquina(boton);
            assertEquals(nombre + " pulsado: los píxeles de su esquina (cambia de forma si suben)",
                    reposo[0], pulsado, Math.max(2, reposo[0] / 50));
            float hundido = quieto ? 1f : 0.94f;
            assertEquals(nombre + " pulsado se hunde", hundido, boton.getScaleX(), 0.005f);
            assertEquals(nombre + " pulsado se hunde", hundido, boton.getScaleY(), 0.005f);
            boton.setPressed(false);
            tocar(boton, MotionEvent.ACTION_UP);
        });
        SystemClock.sleep(ESPERA);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            assertEquals(nombre + " al soltar vuelve", 1f, boton.getScaleX(), 0.005f);
            assertEquals(nombre + " al soltar, la misma forma", reposo[0], esquina(boton),
                    Math.max(2, reposo[0] / 50));
        });
    }

    // Los píxeles casi opacos del cuadrado de arriba a la izquierda, de lado medio alto. La sombra
    // que cae en la esquina recortada es suave (35 % como mucho) y no cuenta.
    private static int esquina(View v) {
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(b));
        int lado = v.getHeight() / 2, macizos = 0;
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) {
                if ((b.getPixel(x, y) >>> 24) > 200) macizos++;
            }
        }
        b.recycle();
        return macizos;
    }

    private static void tocar(View v, int accion) {
        long ahora = SystemClock.uptimeMillis();
        MotionEvent e = MotionEvent.obtain(ahora, ahora, accion, v.getWidth() / 2f, v.getHeight() / 2f, 0);
        v.dispatchTouchEvent(e);
        e.recycle();
    }
}
