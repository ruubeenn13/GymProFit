package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

// ============================================================
// CamposSerie — los campos de una serie en Registrar (peso × repeticiones, o los
// segundos), en fila si sus rótulos caben y uno debajo de otro si no (GP-133).
//
// En fila, cada campo tiene una parte del ancho. Con la letra grande, o en un idioma
// de palabras largas, «Peso (kg)» no entraba en la suya y Material lo cortaba («Peso
// (…»). Aquí se mide primero en fila; si algún rótulo no entra en el hueco de su campo
// (el ancho del campo sin sus rellenos, que es donde Material lo dibuja), se mide otra
// vez con los campos apilados, cada uno a todo el ancho, y sin el «×», que entre dos
// campos apilados no dice nada. Se decide en cada medida, así que girar el móvil o
// cambiar la letra lo vuelve a decidir.
// ============================================================
public class CamposSerie extends LinearLayout {

    /** Separación entre dos campos apilados: deja sitio al rótulo plegado del de abajo. */
    private static final int SEPARACION_DP = 8;

    private boolean apilados;

    public CamposSerie(@NonNull Context context) {
        super(context);
    }

    public CamposSerie(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CamposSerie(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /** Si los campos van uno debajo de otro porque en fila no cabían sus rótulos. */
    public boolean apilados() {
        return apilados;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        colocar(false);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!cabenLosRotulos()) {
            colocar(true);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /** Pone los hijos en fila (con sus pesos del XML) o apilados. */
    private void colocar(boolean apilar) {
        if (apilar == apilados && getOrientation() == (apilar ? VERTICAL : HORIZONTAL)) return;
        apilados = apilar;
        setOrientation(apilar ? VERTICAL : HORIZONTAL);
        int separacion = Math.round(SEPARACION_DP * getResources().getDisplayMetrics().density);
        boolean primero = true;
        for (int i = 0; i < getChildCount(); i++) {
            View hijo = getChildAt(i);
            LayoutParams lp = (LayoutParams) hijo.getLayoutParams();
            Guardado g = guardado(hijo, lp);
            if (!(hijo instanceof TextInputLayout)) {
                // El «×»: en fila, como lo dejó el adapter; apilados, fuera.
                hijo.setVisibility(apilar ? GONE : g.visibilidad);
                continue;
            }
            if (apilar) {
                lp.width = LayoutParams.MATCH_PARENT;
                lp.weight = 0;
                lp.topMargin = primero || hijo.getVisibility() == GONE ? 0 : separacion;
                if (hijo.getVisibility() != GONE) primero = false;
            } else {
                lp.width = g.ancho;
                lp.weight = g.peso;
                lp.topMargin = g.margenArriba;
            }
            hijo.setLayoutParams(lp);
        }
    }

    // Lo que el XML y el adapter dejaron en cada hijo, para volver a la fila tal cual.
    private static final class Guardado {
        final int ancho;
        final float peso;
        final int margenArriba;
        final int visibilidad;

        Guardado(int ancho, float peso, int margenArriba, int visibilidad) {
            this.ancho = ancho;
            this.peso = peso;
            this.margenArriba = margenArriba;
            this.visibilidad = visibilidad;
        }
    }

    private static Guardado guardado(View hijo, LayoutParams lp) {
        Object t = hijo.getTag(es.pmdm.gymprofit.R.id.campos_serie_guardado);
        if (t instanceof Guardado) return (Guardado) t;
        // La primera vez que se coloca, el hijo está como lo dejaron el XML y el adapter.
        Guardado g = new Guardado(lp.width, lp.weight, lp.topMargin, hijo.getVisibility());
        hijo.setTag(es.pmdm.gymprofit.R.id.campos_serie_guardado, g);
        return g;
    }

    private boolean cabenLosRotulos() {
        for (int i = 0; i < getChildCount(); i++) {
            View hijo = getChildAt(i);
            if (!(hijo instanceof TextInputLayout) || hijo.getVisibility() == GONE) continue;
            TextInputLayout capa = (TextInputLayout) hijo;
            EditText campo = capa.getEditText();
            if (campo == null || capa.getHint() == null) continue;
            float texto = campo.getPaint().measureText(capa.getHint().toString());
            int hueco = campo.getMeasuredWidth() - campo.getCompoundPaddingLeft() - campo.getCompoundPaddingRight();
            if (texto > hueco) return false;
        }
        return true;
    }
}
