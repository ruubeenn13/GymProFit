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

    private final List<AlimentoComida> items;
    private final OnItemLongClickListener longClickListener;

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
        h.tvCantidadGramos.setText(String.format(Locale.getDefault(), "%.0f g", item.getCantidadGramos()));
        h.tvCaloriasItem.setText(String.format(Locale.getDefault(), "%d kcal", item.getCaloriasTotales()));
        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onItemLongClick(item, v);
            return true;
        });
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
