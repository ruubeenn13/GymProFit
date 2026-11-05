package es.pmdm.gymprofit.arranque;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.activities.AcercaDeActivity;

// ============================================================
// VistaGBrilloTest — GP-223: el brillo de la G sobrevive al traspaso del splash.
//
// En el traspaso la pantalla de carga cambia de padre (del contenido a la decoración de la
// ventana): la G se suelta de la ventana y vuelve a ella en el mismo paso. En la renovación
// lenta el brillo ya había empezado, y onDetachedFromWindow lo paraba sin que nadie lo
// volviera a pedir. Se monta la G en una ventana de verdad (una Activity cualquiera de la
// app), se le pide el brillo y se la cambia de padre como hace el traspaso.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class VistaGBrilloTest {

    @Test
    public void el_brillo_pedido_sigue_al_cambiar_de_padre() {
        try (ActivityScenario<AcercaDeActivity> esc = ActivityScenario.launch(AcercaDeActivity.class)) {
            esc.onActivity(act -> {
                ViewGroup contenido = act.findViewById(android.R.id.content);
                ViewGroup decor = (ViewGroup) act.getWindow().getDecorView();
                FrameLayout capa = new FrameLayout(act);
                VistaG g = new VistaG(act);
                g.setImageResource(R.drawable.g_arranque);
                capa.addView(g);
                contenido.addView(capa);
                g.empezarBrillo();
                assertTrue("el brillo empieza", g.brillando());

                // Como en el traspaso: fuera del contenido y a la decoración, en el mismo paso.
                contenido.removeView(capa);
                decor.addView(capa);
                assertTrue("tras volver a la ventana, el brillo sigue", g.brillando());
                decor.removeView(capa);
            });
        }
    }

    @Test
    public void el_brillo_parado_no_vuelve_al_cambiar_de_padre() {
        try (ActivityScenario<AcercaDeActivity> esc = ActivityScenario.launch(AcercaDeActivity.class)) {
            esc.onActivity(act -> {
                ViewGroup contenido = act.findViewById(android.R.id.content);
                ViewGroup decor = (ViewGroup) act.getWindow().getDecorView();
                FrameLayout capa = new FrameLayout(act);
                VistaG g = new VistaG(act);
                g.setImageResource(R.drawable.g_arranque);
                capa.addView(g);
                contenido.addView(capa);
                g.empezarBrillo();
                g.pararBrillo();

                contenido.removeView(capa);
                decor.addView(capa);
                assertFalse("pararBrillo lo olvida", g.brillando());
                decor.removeView(capa);
            });
        }
    }
}
