package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.ui.cristal.Cristal;
import es.pmdm.gymprofit.ui.cristal.Hundir;
import es.pmdm.gymprofit.ui.cristal.NivelCristal;

// ============================================================
// BotonCristal — el botón secundario de Liquid Glass (GP-285, lote 1c; DEC-070).
//
// Cristal estático en píldora (Cristal.TARJETA, los tokens de las tarjetas: relleno translúcido
// a 160°, borde de 1 dp y filo de luz arriba), sin desenfoque. En el nivel 3 (Android 11 o
// menos, o texto de alto contraste) pasa a la superficie opaca del cristal vivo
// (Cristal.VIVO_OPACO), donde su texto se lee sin depender de lo de detrás. Se usa con el
// estilo Widget.GymProFit.Boton.Cristal; su texto, gp_cristal_texto, se mide en el punto más
// claro que pueda quedar detrás (ContrasteCristalTest).
//
// No cambia de forma al pulsarlo: se hunde (Hundir). El nivel se mira al crearlo: si cambia
// el alto contraste con la pantalla abierta, se ve al volver a crearla.
// ============================================================
public class BotonCristal extends BotonExpresivo {

    public BotonCristal(@NonNull Context context) {
        this(context, null);
    }

    public BotonCristal(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        boolean opaco = NivelCristal.actual(context) == NivelCristal.OPACO;
        setBackgroundTintList(null);
        setBackground(new Cristal(context, opaco ? Cristal.VIVO_OPACO : Cristal.TARJETA, -1f));
        Hundir.alPulsar(this);
    }

    @Override
    protected boolean cambiaForma() {
        return false;
    }
}
