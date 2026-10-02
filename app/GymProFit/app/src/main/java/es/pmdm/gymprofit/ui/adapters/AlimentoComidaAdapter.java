package es.pmdm.gymprofit.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.AlimentoComida;

// ============================================================
// AlimentoComidaAdapter — adapter de RecyclerView para los ítems de una comida.
// Muestra cada alimento añadido a una comida concreta (nombre, gramos y
// calorías totales del ítem) y permite eliminarlo/editarlo vía long-click,
// dentro del flujo de registro de comidas del módulo de nutrición.
// ============================================================
/**
 * Adapter para mostrar los alimentos de una comida en un RecyclerView.
 */
public class AlimentoComidaAdapter extends RecyclerView.Adapter<AlimentoComidaAdapter.ViewHolder> {

    /** Callback para long-press sobre un ítem. */
    public interface OnItemLongClickListener {
        void onItemLongClick(AlimentoComida item, View anchorView);
    }

    /** Tocar un alimento (lote 1.6.1): abre la ficha para cambiar su cantidad. */
    public interface OnItemClickListener {
        void onItemClick(AlimentoComida item);
    }

    private final List<AlimentoComida> items;
    private final OnItemLongClickListener longClickListener;
    @androidx.annotation.Nullable private OnItemClickListener clickListener;

    public void setOnItemClickListener(@androidx.annotation.Nullable OnItemClickListener l) {
        this.clickListener = l;
    }

    // Constructor: recibe los ítems de la comida y el listener de long-click.
    public AlimentoComidaAdapter(List<AlimentoComida> items, OnItemLongClickListener listener) {
        this.items = items;
        this.longClickListener = listener;
    }

    // Infla el layout de un ítem alimento-comida y crea su ViewHolder.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alimento_comida, parent, false);
        return new ViewHolder(view);
    }

    // Rellena nombre, gramos y calorías del ítem, y engancha el long-click.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AlimentoComida item = items.get(position);
        h.tvNombreAlimento.setText(item.getNombreAlimento());
        h.tvCantidadGramos.setText(cantidad(h.itemView.getContext(), item));
        h.tvCaloriasItem.setText(h.itemView.getContext().getString(R.string.unidad_kcal, item.getCaloriasTotales()));
        h.itemView.setOnClickListener(v -> { if (clickListener != null) clickListener.onItemClick(item); });
        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onItemLongClick(item, v);
            return true;
        });
    }

    /**
     * La cantidad de la línea: «2 × rebanada (56 g)» si se eligió por raciones (lote
     * 1.6.1), o sus gramos.
     */
    static String cantidad(android.content.Context ctx, AlimentoComida item) {
        if (item.getRacionNombre() == null || item.getRaciones() == null) {
            return ctx.getString(R.string.unidad_g_redondeado, item.getCantidadGramos());
        }
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(
                es.pmdm.gymprofit.utils.FechaUtils.localeDeLaApp(ctx));
        nf.setMaximumFractionDigits(1);
        String gramos = nf.format(Math.round(item.getCantidadGramos()));
        if (item.getRaciones() == 1) return ctx.getString(R.string.cantidad_una_racion, item.getRacionNombre(), gramos);
        return ctx.getString(R.string.cantidad_raciones, nf.format(item.getRaciones()),
                es.pmdm.gymprofit.utils.CantidadFicha.nombreUnidad(item.getRacionNombre()), gramos);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder con las referencias a las vistas de cada ítem de comida.
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreAlimento, tvCantidadGramos, tvCaloriasItem;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreAlimento = itemView.findViewById(R.id.tvNombreAlimento);
            tvCantidadGramos = itemView.findViewById(R.id.tvCantidadGramos);
            tvCaloriasItem   = itemView.findViewById(R.id.tvCaloriasItem);
        }
    }
}
