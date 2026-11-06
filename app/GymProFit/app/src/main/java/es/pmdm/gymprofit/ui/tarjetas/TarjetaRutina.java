package es.pmdm.gymprofit.ui.tarjetas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.sesion.VezRutina;
import es.pmdm.gymprofit.ui.widget.BarrasVecesView;

// ============================================================
// TarjetaRutina — pinta item_rutina_tarjeta, la tarjeta de una rutina de Entrenar (GP-232).
//
// La usan «Rutinas del programa» (TuProgramaVista), con cuándo toca arriba a la derecha, y «Mis
// rutinas» (EntrenarFragment), con ▶ para empezar. Lo de la última vez (kilos, barras, cuándo y
// si batió récord) viene con la lista de activas: ninguna tarjeta pide nada.
// ============================================================
public final class TarjetaRutina {

    /** Lo que se pinta en la tarjeta. */
    public static final class Datos {
        public int id;
        @NonNull public String nombre = "";
        /** Las zonas en frase; "" si no se saben. */
        @NonNull public String zonas = "";
        public int ejercicios;
        public int minutos;
        @NonNull public List<VezRutina> ultimas = java.util.Collections.emptyList();
        /** «Hoy», «Siguiente», «En 2 entrenos», «En curso»; null en las que llevan ▶. */
        @Nullable public String cuando;
        @ColorInt public int colorCuando;
        /** La descripción de ▶ («Empezar Día de brazos»); null si no lleva ▶. */
        @Nullable public String empezarA11y;
    }

    private TarjetaRutina() { }

    /**
     * Infla y pinta la tarjeta en la lista (con 12 dp de hueco con la de arriba).
     *
     * @param empezar   lo que hace ▶; null si no lleva.
     * @return la tarjeta, para ponerle el toque y la pulsación larga.
     */
    @NonNull
    public static View pintar(@NonNull LinearLayout lista, @NonNull Datos d, @NonNull TextosTarjeta textos,
                              @NonNull EntradaTarjeta entrada, @Nullable Runnable empezar) {
        View t = LayoutInflater.from(lista.getContext()).inflate(R.layout.item_rutina_tarjeta, lista, false);
        if (lista.getChildCount() > 0) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) t.getLayoutParams();
            lp.topMargin = lista.getResources().getDimensionPixelSize(R.dimen.espacio_12);
        }
        lista.addView(t);
        ((TextView) t.findViewById(R.id.tvNombre)).setText(d.nombre);

        TextView cuando = t.findViewById(R.id.tvCuando);
        cuando.setVisibility(d.cuando == null ? View.GONE : View.VISIBLE);
        cuando.setText(d.cuando);
        cuando.setTextColor(d.colorCuando);
        ImageButton btn = t.findViewById(R.id.btnEmpezarFila);
        btn.setVisibility(empezar == null ? View.GONE : View.VISIBLE);
        if (empezar != null) {
            btn.setContentDescription(d.empezarA11y);
            btn.setOnClickListener(v -> empezar.run());
            // Con ▶ arriba, el lienzo deja 12 dp arriba y a la derecha en vez de 16.
            int doce = lista.getResources().getDimensionPixelSize(R.dimen.espacio_12);
            int dieciseis = lista.getResources().getDimensionPixelSize(R.dimen.espacio_16);
            t.findViewById(R.id.cuerpoRutina).setPaddingRelative(dieciseis, doce, doce, dieciseis);
        }

        texto(t.findViewById(R.id.tvMusculos), d.zonas);
        String pauta = textos.pauta(d.ejercicios, d.minutos);
        texto(t.findViewById(R.id.tvResumen), pauta);

        TextView kilos = t.findViewById(R.id.tvKilosRutina);
        BarrasVecesView barras = t.findViewById(R.id.barrasRutina);
        TextView ultima = t.findViewById(R.id.tvUltimaRutina);
        if (d.ultimas.isEmpty()) {
            // Sin hacer todavía: en el sitio de los kilos, sin barras.
            barras.setVeces(d.ultimas);
            kilos.setTextAppearance(R.style.TextAppearance_GymProFit_Cuerpo);
            kilos.setTextColor(lista.getContext().getColor(R.color.gp_text_secondary));
            kilos.setText(R.string.tarjeta_sin_hacer);
            ultima.setVisibility(View.GONE);
        } else {
            barras.setVeces(d.ultimas);
            VezRutina u = d.ultimas.get(d.ultimas.size() - 1);
            entrada.pintar("r:" + d.id, kilos, u.getKilos(), R.dimen.text_tarjeta_kg, textos, barras);
            ultima.setVisibility(View.VISIBLE);
            ultima.setText(textos.ultimaVez(d.ultimas));
        }

        StringBuilder a11y = new StringBuilder(d.nombre);
        if (d.cuando != null) a11y.append(", ").append(d.cuando.toLowerCase(textos.idioma()));
        a11y.append(". ");
        if (!d.zonas.isEmpty()) a11y.append(d.zonas).append(". ");
        if (!pauta.isEmpty()) a11y.append(pauta).append(". ");
        a11y.append(textos.ultimaVezA11y(d.ultimas)).append('.');
        String b = textos.barrasA11y(d.ultimas);
        if (b != null) a11y.append(' ').append(b).append('.');
        t.setContentDescription(a11y);
        return t;
    }

    private static void texto(@NonNull TextView tv, @Nullable String s) {
        tv.setText(s);
        tv.setVisibility(s == null || s.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
