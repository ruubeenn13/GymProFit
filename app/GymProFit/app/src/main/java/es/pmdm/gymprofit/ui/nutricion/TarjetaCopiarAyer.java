package es.pmdm.gymprofit.ui.nutricion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.text.NumberFormat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.ComidaReciente;
import es.pmdm.gymprofit.utils.Cantidades;
import es.pmdm.gymprofit.utils.ComidasRecientes;
import es.pmdm.gymprofit.utils.FechaUtils;

// ============================================================
// TarjetaCopiarAyer — «¿Copiar la de ayer?» (tableros 1 y 2b, lote 1.6.4)
// Rellena las dos tarjetas: la del diario (view_copiar_ayer_diario), con «2 alimentos ·
// 203 kcal» y debajo cada alimento con su cantidad y sus kcal; y la de una comida sin
// apuntar (view_copiar_ayer_comida), con sus alimentos y sus kcal en una línea. La ✓ y
// la ✗ las decide quien la pone; plegarla, Movimiento.plegar (momento 18).
// ============================================================
public final class TarjetaCopiarAyer {

    /** Lo que hacen la ✓ (copiar) y la ✗ (no, hoy no). */
    public interface Respuesta {
        void si();

        void no();
    }

    private TarjetaCopiarAyer() {
    }

    /**
     * @param tarjeta  la vista inflada de view_copiar_ayer_diario o view_copiar_ayer_comida.
     * @param ayer     la comida de ayer que se copiaría.
     * @param tipo     la comida de destino (DESAYUNO…CENA), para TalkBack de la ✓.
     * @param r        la ✓ y la ✗.
     */
    public static void pintar(@NonNull View tarjeta, @NonNull ComidaReciente ayer, @NonNull String tipo,
                              @NonNull Respuesta r) {
        Context ctx = tarjeta.getContext();
        NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(ctx));
        LinearLayout lista = tarjeta.findViewById(R.id.listaCopiar);
        boolean enDiario = lista != null;
        int n = ayer.getLineas().size();
        String detalle = enDiario
                ? ctx.getResources().getQuantityString(R.plurals.copiar_ayer_resumen, n, n, nf.format(ayer.getKcal()))
                : CopiarComida.detalle(ctx, ayer);
        ((TextView) tarjeta.findViewById(R.id.tvDetalleCopiar)).setText(detalle);
        String titulo = ctx.getString(R.string.copiar_ayer_titulo);
        tarjeta.findViewById(R.id.textosCopiar).setContentDescription(enDiario
                ? titulo + " " + detalle + ": " + ComidasRecientes.nombres(ayer.getLineas())
                : titulo + " " + detalle);

        View si = tarjeta.findViewById(R.id.btnSiCopiar);
        si.setContentDescription(ctx.getString(CopiarComida.siCopiar(tipo)));
        si.setOnClickListener(v -> r.si());
        tarjeta.findViewById(R.id.btnNoCopiar).setOnClickListener(v -> r.no());

        if (enDiario) {
            lista.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(ctx);
            for (AlimentoComida l : ayer.getLineas()) {
                View fila = inflater.inflate(R.layout.item_linea_copiar, lista, false);
                String cantidad = Cantidades.de(ctx, l);
                String kcal = ctx.getString(R.string.nutricion_kcal_valor, nf.format(l.getCaloriasTotales()));
                ((TextView) fila.findViewById(R.id.tvLineaCopiar)).setText(
                        ctx.getString(R.string.copiar_ayer_linea, l.getNombreAlimento(), cantidad));
                ((TextView) fila.findViewById(R.id.tvKcalLineaCopiar)).setText(kcal);
                fila.setContentDescription(ctx.getString(R.string.copiar_ayer_linea_a11y, l.getNombreAlimento(),
                        cantidad, nf.format(l.getCaloriasTotales())));
                lista.addView(fila);
            }
        }
        tarjeta.setVisibility(View.VISIBLE);
        tarjeta.setAlpha(1f);
        tarjeta.setScaleX(1f);
        tarjeta.setScaleY(1f);
    }
}
