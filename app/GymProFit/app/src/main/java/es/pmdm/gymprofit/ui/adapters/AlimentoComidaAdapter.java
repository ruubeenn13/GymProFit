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

/**
 * Adapter para mostrar los alimentos de una comida en un RecyclerView.
 */
public class AlimentoComidaAdapter extends RecyclerView.Adapter<AlimentoComidaAdapter.ViewHolder> {

    /** Callback para long-press sobre un ítem. */
    public interface OnItemLongClickListener {
        void onItemLongClick(AlimentoComida item);
    }

    private final List<AlimentoComida> items;
    private final OnItemLongClickListener longClickListener;

    public AlimentoComidaAdapter(List<AlimentoComida> items, OnItemLongClickListener listener) {
        this.items = items;
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alimento_comida, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AlimentoComida item = items.get(position);
        h.tvNombreAlimento.setText(item.getNombreAlimento());
        h.tvCantidadGramos.setText(String.format(Locale.getDefault(), "%.0f g", item.getCantidadGramos()));
        h.tvCaloriasItem.setText(String.format(Locale.getDefault(), "%d kcal", item.getCaloriasTotales()));
        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onItemLongClick(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

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
