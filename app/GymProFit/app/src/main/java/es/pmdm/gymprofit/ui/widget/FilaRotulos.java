package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// FilaRotulos — los rótulos de las columnas de una serie («Serie», «Anterior», «kg», «Reps»),
// todos a un mismo tamaño (GP-286).
//
// Cada rótulo es un TextoSinPartir: con letra grande encoge lo justo para que su palabra quepa
// en su columna. Pero las columnas no miden lo mismo, y «Serie», en la más estrecha, encogía
// más que los demás: cuatro rótulos de una misma fila a tres tamaños. Aquí se miden una vez,
// se mira cuál ha quedado más pequeño y los demás bajan a ese (TextoSinPartir.limitarA).
// TextoSinPartir.igualar no vale: da por hecho que sus textos tienen el mismo ancho.
//
// Se decide al medirse, y solo se vuelve a medir a los hijos cuando el tamaño común cambia.
// ============================================================
public class FilaRotulos extends LinearLayout {

    // El tope puesto ahora a los rótulos, en px; 0, ninguno.
    private float tope;

    public FilaRotulos(@NonNull Context context) {
        super(context);
    }

    public FilaRotulos(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int anchoSpec, int altoSpec) {
        super.onMeasure(anchoSpec, altoSpec);
        // Lo que cada uno eligió por su cuenta (su columna manda), tenga o no tope puesto.
        float menor = Float.MAX_VALUE, mayor = 0f;
        for (int i = 0; i < getChildCount(); i++) {
            View hijo = getChildAt(i);
            if (!(hijo instanceof TextoSinPartir) || hijo.getVisibility() == GONE) continue;
            float suyo = ((TextoSinPartir) hijo).tamanoPropio();
            menor = Math.min(menor, suyo);
            mayor = Math.max(mayor, suyo);
        }
        // Si alguno ha encogido, todos al suyo; si no, sin tope. Solo cuando eso cambia se
        // vuelve a medir a los hijos: una fila ya decidida no los toca.
        float nuevo = mayor > menor ? menor : 0f;
        if (nuevo == tope) return;
        poner(nuevo);
        super.onMeasure(anchoSpec, altoSpec);
    }

    private void poner(float nuevo) {
        if (nuevo == tope) return;
        tope = nuevo;
        for (int i = 0; i < getChildCount(); i++) {
            View hijo = getChildAt(i);
            if (hijo instanceof TextoSinPartir) ((TextoSinPartir) hijo).limitarA(nuevo);
        }
    }
}
