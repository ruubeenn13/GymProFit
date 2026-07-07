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

// ============================================================
// AlimentoAdapter — adapter de RecyclerView para el catálogo de alimentos.
// Pinta nombre, categoría y calorías por 100g de cada Alimento, gestiona
// el click simple (selección) y el long-click (menú contextual) sobre
// cada fila de la lista de alimentos del sistema de nutrición.
// ============================================================
/**
 * Adapter para mostrar una lista de {@link Alimento} en un RecyclerView.
 */
public class AlimentoAdapter extends RecyclerView.Adapter<AlimentoAdapter.ViewHolder> {

    // Callback invocado al pulsar (click simple) un alimento de la lista.
    public interface OnItemClickListener {
        void onItemClick(Alimento alimento);
    }

    // Callback invocado al mantener pulsado (long-click) un alimento.
    public interface OnItemLongClickListener {
        void onItemLongClick(Alimento alimento, View anchorView);
    }

    private final List<Alimento> lista;
    private final OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    // Constructor: recibe la lista de alimentos y el listener de click.
    public AlimentoAdapter(List<Alimento> lista, OnItemClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    // Permite registrar el listener de long-click desde fuera del adapter.
    public void setOnItemLongClickListener(OnItemLongClickListener l) {
        this.longClickListener = l;
    }

    // Infla el layout de un ítem de alimento y crea su ViewHolder.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alimento, parent, false);
        return new ViewHolder(view);
    }

    // Rellena las vistas de una fila con los datos del alimento y engancha los listeners.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alimento alimento = lista.get(position);
        holder.tvNombreAlimento.setText(alimento.getNombre());
        // Productos de Open Food Facts: se muestra la marca; locales: la categoría
        String subtitulo = alimento.getMarca() != null && !alimento.getMarca().isEmpty()
                ? alimento.getMarca() : alimento.getCategoria();
        holder.tvCategoriaAlimento.setText(subtitulo);
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

    // ViewHolder con las referencias a las vistas de cada fila de alimento.
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
