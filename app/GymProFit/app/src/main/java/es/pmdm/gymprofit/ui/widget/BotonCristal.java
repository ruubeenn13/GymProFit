package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import es.pmdm.gymprofit.R;

import es.pmdm.gymprofit.ui.cristal.Cristal;
import es.pmdm.gymprofit.ui.cristal.Hundir;
import es.pmdm.gymprofit.ui.cristal.NivelCristal;

// ============================================================
// BotonCristal — el botón secundario de Liquid Glass (GP-285, lote 1c; DEC-070).
//
// Cristal estático en píldora (Cristal.TARJETA, los tokens de las tarjetas: relleno translúcido
// a 160°, borde de 1 dp y filo de luz arriba), sin desenfoque. En el nivel 3 (Android 11 o
// menos, o texto de alto contraste) pasa a la superficie opaca del cristal vivo
// (Cristal.VIVO_OPACO), donde su texto se lee sin depender de lo de detrás; salvo encima de un
// cristal vivo (app:cristalSobreVivo, los del panel del descanso), que ya lo es. Se usa con el
// estilo Widget.GymProFit.Boton.Cristal; su texto, gp_cristal_texto, se mide en el punto más
// claro que pueda quedar detrás (ContrasteCristalTest). En claro su borde es tinta al 20 %
// (gp_cristal_boton_borde): con el blanco del cristal, sobre una tarjeta clara no se veía.
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
        android.content.res.TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BotonCristal);
        boolean sobreVivo = a.getBoolean(R.styleable.BotonCristal_cristalSobreVivo, false);
        a.recycle();
        // Encima de un cristal vivo (el panel del descanso) no cambia con el nivel: en el 3 el
        // panel ya es la superficie opaca, y un botón opaco encima no se distinguiría de él.
        boolean opaco = !sobreVivo && NivelCristal.actual(context) == NivelCristal.OPACO;
        setBackgroundTintList(null);
        Cristal fondo = new Cristal(context, opaco ? Cristal.VIVO_OPACO : Cristal.TARJETA, -1f);
        // En claro, un canto que se vea sobre una tarjeta clara (en oscuro, el del cristal).
        fondo.ponerBorde(ContextCompat.getColor(context, R.color.gp_cristal_boton_borde));
        setBackground(fondo);
        Hundir.alPulsar(this);
    }

    @Override
    protected boolean cambiaForma() {
        return false;
    }
}
