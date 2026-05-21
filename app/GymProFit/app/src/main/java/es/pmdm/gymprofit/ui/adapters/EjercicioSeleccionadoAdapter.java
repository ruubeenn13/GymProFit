package es.pmdm.gymprofit.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.EjercicioSeleccionado;

public class EjercicioSeleccionadoAdapter
        extends RecyclerView.Adapter<EjercicioSeleccionadoAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(EjercicioSeleccionado item);
    }

    private final List<EjercicioSeleccionado> items;
    private final OnDeleteListener deleteListener;

    public EjercicioSeleccionadoAdapter(List<EjercicioSeleccionado> items,
                                        OnDeleteListener deleteListener) {
        this.items = items;
        this.deleteListener = deleteListener;
    }

    public EjercicioSeleccionadoAdapter(List<EjercicioSeleccionado> items) {
        this(items, null);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ejercicio_seleccionado, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        EjercicioSeleccionado item = items.get(position);
        h.tvNombre.setText(item.getEjercicio().getNombre());
        h.tvSeriesReps.setText(item.getSeries() + " × " + item.getRepeticiones());
        if (deleteListener == null) {
            h.btnEliminar.setVisibility(View.GONE);
        } else {
            h.btnEliminar.setVisibility(View.VISIBLE);
            h.btnEliminar.setOnClickListener(v -> deleteListener.onDelete(item));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvSeriesReps;
        ImageView btnEliminar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre     = itemView.findViewById(R.id.tvNombreSeleccionado);
            tvSeriesReps = itemView.findViewById(R.id.tvSeriesReps);
            btnEliminar  = itemView.findViewById(R.id.btnEliminarSeleccionado);
        }
    }
}
