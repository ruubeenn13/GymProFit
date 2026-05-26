package es.pmdm.gymprofit.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.alimento.Alimento;

/**
 * Adapter para mostrar una lista de {@link Alimento} en un RecyclerView.
 */
public class AlimentoAdapter extends RecyclerView.Adapter<AlimentoAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Alimento alimento);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Alimento alimento, View anchorView);
    }

    private final List<Alimento> lista;
    private final OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public AlimentoAdapter(List<Alimento> lista, OnItemClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener l) {
        this.longClickListener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alimento, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alimento alimento = lista.get(position);
        holder.tvNombreAlimento.setText(alimento.getNombre());
        holder.tvCategoriaAlimento.setText(alimento.getCategoria());
        holder.tvCaloriasPor100.setText(alimento.getCalorias() + " kcal/100g");
        holder.itemView.setOnClickListener(v -> listener.onItemClick(alimento));
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(alimento, v);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreAlimento;
        TextView tvCategoriaAlimento;
        TextView tvCaloriasPor100;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreAlimento = itemView.findViewById(R.id.tvNombreAlimento);
            tvCategoriaAlimento = itemView.findViewById(R.id.tvCategoriaAlimento);
            tvCaloriasPor100 = itemView.findViewById(R.id.tvCaloriasPor100);
        }
    }
}
