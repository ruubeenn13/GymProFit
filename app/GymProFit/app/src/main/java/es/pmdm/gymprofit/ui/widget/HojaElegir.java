package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// HojaElegir — la hoja inferior de elegir una opción (GP-216, tablero 22): la misma para Tema,
// Idioma, Vaso de agua y Descanso sin pauta.
//
// Las opciones van en dos columnas, cada una con su radio y su nombre; la elegida lleva el borde
// naranja. Tocar una la aplica al momento y cierra la hoja: no hay botón «Listo» (el cambio se
// ve ya, y deshacerlo es volver a abrirla). La del tema lleva una miniatura de cómo queda. Cada
// opción es un radio para TalkBack, con su nombre y su estado.
// ============================================================
public final class HojaElegir {

    /** Una opción: su texto y, solo en la hoja del tema, los colores de su miniatura. */
    public static final class Opcion {
        final String texto;
        @Nullable final int[] miniatura;

        private Opcion(String texto, @Nullable int[] miniatura) {
            this.texto = texto;
            this.miniatura = miniatura;
        }

        /** Una opción de solo texto («250 ml», «Español»). */
        public static Opcion de(@NonNull String texto) {
            return new Opcion(texto, null);
        }

        /** Una opción con miniatura: colores de fondo, de tarjeta y de trazo (recursos). */
        public static Opcion conMiniatura(@NonNull String texto, @ColorRes int fondo, @ColorRes int tarjeta,
                                          @ColorRes int trazo) {
            return new Opcion(texto, new int[]{fondo, tarjeta, trazo});
        }
    }

    /** Se llama con la posición elegida, ya cerrada la hoja. */
    public interface AlElegir { void elegida(int indice); }

    private HojaElegir() { }

    /**
     * Abre la hoja.
     *
     * @param titulo   el título de la hoja.
     * @param opciones lo que se puede elegir.
     * @param elegida  la posición elegida ahora, o -1 si ninguna.
     * @param nota     la línea de debajo, o 0 si no hay.
     * @param alElegir lo que se hace al tocar una.
     */
    public static void mostrar(@NonNull Context ctx, @StringRes int titulo, @NonNull List<Opcion> opciones,
                               int elegida, @StringRes int nota, @NonNull AlElegir alElegir) {
        BottomSheetDialog hoja = new BottomSheetDialog(ctx);
        View contenido = LayoutInflater.from(ctx).inflate(R.layout.dialog_hoja_elegir, null, false);
        ((TextView) contenido.findViewById(R.id.tvTituloHoja)).setText(titulo);
        TextView tvNota = contenido.findViewById(R.id.tvNotaHoja);
        if (nota == 0) tvNota.setVisibility(View.GONE);
        else tvNota.setText(nota);

        LinearLayout grid = contenido.findViewById(R.id.gridOpciones);
        LayoutInflater inflater = LayoutInflater.from(ctx);
        int separacion = ctx.getResources().getDimensionPixelSize(R.dimen.espacio_12);
        LinearLayout fila = null;
        for (int i = 0; i < opciones.size(); i++) {
            if (i % 2 == 0) {
                fila = new LinearLayout(ctx);
                fila.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) lp.topMargin = separacion;
                grid.addView(fila, lp);
            }
            View op = inflater.inflate(R.layout.item_opcion_hoja, fila, false);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) op.getLayoutParams();
            if (i % 2 == 1) lp.setMarginStart(separacion);
            prepararOpcion(ctx, op, opciones.get(i), i == elegida);
            final int indice = i;
            op.setOnClickListener(v -> {
                Movimiento.vibrar(v, Movimiento.Vibracion.LIGERA);
                hoja.dismiss();
                alElegir.elegida(indice);
            });
            fila.addView(op);
        }
        // Con un número impar, la última fila deja su hueco para que la opción no se estire a todo el ancho.
        if (fila != null && opciones.size() % 2 == 1) {
            View hueco = new View(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 0, 1f);
            lp.setMarginStart(separacion);
            fila.addView(hueco, lp);
        }

        hoja.setContentView(contenido);
        BottomSheetBehavior<?> b = hoja.getBehavior();
        b.setSkipCollapsed(true);
        b.setState(BottomSheetBehavior.STATE_EXPANDED);
        hoja.show();
    }

    private static void prepararOpcion(Context ctx, View op, Opcion o, boolean elegida) {
        ((TextView) op.findViewById(R.id.tvTextoOpcion)).setText(o.texto);
        ImageView radio = op.findViewById(R.id.ivRadioOpcion);
        radio.setImageResource(elegida ? R.drawable.ic_ms_radio_button_checked : R.drawable.ic_ms_radio_button_unchecked);
        radio.setImageTintList(android.content.res.ColorStateList.valueOf(MaterialColors.getColor(op, elegida
                ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOutline)));
        op.setBackgroundResource(elegida ? R.drawable.bg_opcion_hoja_elegida : R.drawable.bg_opcion_hoja);
        op.setContentDescription(o.texto);
        ViewCompat.setAccessibilityDelegate(op, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.RadioButton.class.getName());
                info.setCheckable(true);
                info.setChecked(elegida);
            }
        });
        if (o.miniatura != null) {
            FrameLayout previa = op.findViewById(R.id.vistaPreviaOpcion);
            previa.setVisibility(View.VISIBLE);
            montarMiniatura(ctx, previa, o.miniatura);
        }
    }

    // La miniatura de un tema: un fondo, una cabecera, un bloque naranja y dos tarjetas.
    private static void montarMiniatura(Context ctx, FrameLayout previa, int[] colores) {
        float radioGrande = ctx.getResources().getDimension(R.dimen.radio_miniatura);
        float radioMedio = ctx.getResources().getDimension(R.dimen.espacio_12);
        float radioPequeno = ctx.getResources().getDimension(R.dimen.espacio_8);
        int fondo = ContextCompat.getColor(ctx, colores[0]);
        int tarjeta = ContextCompat.getColor(ctx, colores[1]);
        int trazo = ContextCompat.getColor(ctx, colores[2]);
        int marca = ContextCompat.getColor(ctx, R.color.gp_primary_sobre_hero);
        float d = ctx.getResources().getDisplayMetrics().density;
        int margen = ctx.getResources().getDimensionPixelSize(R.dimen.espacio_12);
        int hueco = (int) (6 * d);

        previa.setBackground(forma(fondo, radioGrande));
        LinearLayout columna = new LinearLayout(ctx);
        columna.setOrientation(LinearLayout.VERTICAL);
        columna.setPadding(margen, margen, margen, margen);

        // La cabecera mide la mitad del ancho: una fila con dos pesos iguales y solo el primero pintado.
        LinearLayout cabecera = new LinearLayout(ctx);
        cabecera.setOrientation(LinearLayout.HORIZONTAL);
        cabecera.setWeightSum(2f);
        View titulo = new View(ctx);
        titulo.setBackground(forma(trazo, radioPequeno));
        cabecera.addView(titulo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        columna.addView(cabecera, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (8 * d)));

        columna.addView(bloque(ctx, marca, radioMedio, (int) (44 * d), hueco));
        columna.addView(bloque(ctx, tarjeta, radioMedio, (int) (30 * d), hueco));
        columna.addView(bloque(ctx, tarjeta, radioPequeno, (int) (18 * d), hueco));
        previa.addView(columna, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // Un bloque de ancho completo con su hueco por encima.
    private static View bloque(Context ctx, int color, float radio, int alto, int huecoArriba) {
        View v = new View(ctx);
        v.setBackground(forma(color, radio));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, alto);
        lp.topMargin = huecoArriba;
        v.setLayoutParams(lp);
        return v;
    }

    private static GradientDrawable forma(int color, float radio) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setColor(color);
        g.setCornerRadius(radio);
        return g;
    }
}
