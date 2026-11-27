package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.card.MaterialCardView;

import org.junit.Test;
import org.junit.runner.RunWith;

import es.pmdm.gymprofit.R;

// ============================================================
// BotonMarcaSombraTest — GP-285, lote 1c: la sombra naranja del principal de Inicio no se corta.
//
// La sombra cae fuera del botón, sobre todo hacia abajo. El revisor de UI la vio cortada en
// seco justo bajo «Empezar»: el contenido de la tarjeta lleva relleno debajo y recortaba a él.
// Ningún padre del botón, hasta su tarjeta, puede recortar a sus hijos ni a su relleno.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class BotonMarcaSombraTest {

    @Test
    public void ningun_padre_hasta_la_tarjeta_recorta_la_sombra_de_empezar() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context c = new ContextThemeWrapper(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_GymProFit);
            View inicio = LayoutInflater.from(c).inflate(R.layout.fragment_inicio, new FrameLayout(c), false);
            View empezar = inicio.findViewById(R.id.btnHoyPrincipal);
            BotonMarca.quitarRecorte(empezar);
            boolean tarjeta = false;
            for (ViewParent p = empezar.getParent(); p instanceof ViewGroup; p = p.getParent()) {
                if (p instanceof MaterialCardView) {
                    tarjeta = true;
                    break;
                }
                ViewGroup g = (ViewGroup) p;
                String quien = g.getClass().getSimpleName() + " " + (g.getId() != View.NO_ID
                        ? c.getResources().getResourceEntryName(g.getId()) : "(sin id)");
                assertFalse(quien + " recorta a sus hijos", g.getClipChildren());
                assertFalse(quien + " recorta a su relleno", g.getClipToPadding());
            }
            assertTrue("«Empezar» va dentro de su tarjeta", tarjeta);
        });
    }
}
