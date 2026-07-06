package es.pmdm.gymprofit.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.rutina.EjercicioSeleccionado;

// ============================================================
// EjercicioSeleccionadoAdapter — adapter de RecyclerView para ejercicios elegidos en una rutina.
// Muestra cada EjercicioSeleccionado (datos del Ejercicio base + series/repeticiones
// asignadas) al crear o editar una rutina, con botón de eliminar en modo edición
// o chevron de navegación en modo solo lectura, según qué listeners se registren.
// ============================================================
public class EjercicioSeleccionadoAdapter
        extends RecyclerView.Adapter<EjercicioSeleccionadoAdapter.ViewHolder> {

    // Callback invocado al pulsar el botón de eliminar de un ítem.
    public interface OnDeleteListener {
        void onDelete(EjercicioSeleccionado item);
    }

    // Callback invocado al pulsar (click simple) un ítem de la lista.
    public interface OnEjercicioClickListener {
        void onClick(EjercicioSeleccionado item);
    }

    private final List<EjercicioSeleccionado> items;
    private final OnDeleteListener deleteListener;
    private final OnEjercicioClickListener clickListener;

    // Constructor principal: permite combinar borrado y click sobre el ítem.
    public EjercicioSeleccionadoAdapter(List<EjercicioSeleccionado> items,
                                        OnDeleteListener deleteListener,
                                        OnEjercicioClickListener clickListener) {
        this.items = items;
        this.deleteListener = deleteListener;
        this.clickListener = clickListener;
    }

    // Constructor con solo borrado (sin click sobre el ítem).
    public EjercicioSeleccionadoAdapter(List<EjercicioSeleccionado> items,
                                        OnDeleteListener deleteListener) {
        this(items, deleteListener, null);
    }

    // Constructor de solo lectura (sin borrado ni click).
    public EjercicioSeleccionadoAdapter(List<EjercicioSeleccionado> items) {
        this(items, null, null);
    }

    // Infla el layout de un ítem seleccionado y crea su ViewHolder.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ejercicio_seleccionado, parent, false);
        return new ViewHolder(view);
    }

    // Rellena nombre, descripción, dificultad, calorías y series/repeticiones,
    // mostrando u ocultando cada chip según haya o no dato disponible.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        EjercicioSeleccionado item = items.get(position);
        Ejercicio e = item.getEjercicio();

        h.tvNombre.setText(e.getNombre() != null ? e.getNombre() : "");

        String desc = e.getDescripcion();
        if (desc != null && !desc.trim().isEmpty()) {
            h.tvDescripcion.setText(desc);
            h.tvDescripcion.setVisibility(View.VISIBLE);
        } else {
            h.tvDescripcion.setVisibility(View.GONE);
        }

        String dif = e.getDificultad();
        if (dif != null && !dif.trim().isEmpty()) {
            h.chipDificultad.setText(es.pmdm.gymprofit.utils.UIHelper.traducirNivel(h.itemView.getContext(), dif)); // enum traducido
            h.chipDificultad.setVisibility(View.VISIBLE);
        } else {
            h.chipDificultad.setVisibility(View.GONE);
        }

        if (e.getCalorias() > 0) {
            h.chipCalorias.setText(e.getCalorias() + " kcal");
            h.chipCalorias.setVisibility(View.VISIBLE);
        } else {
            h.chipCalorias.setVisibility(View.GONE);
        }

        h.chipSeriesReps.setText(item.getSeries() + " × " + item.getRepeticiones());

        if (deleteListener != null) {
            h.btnEliminar.setVisibility(View.VISIBLE);
            h.ivChevron.setVisibility(View.GONE);
            h.btnEliminar.setOnClickListener(v -> deleteListener.onDelete(item));
        } else {
            h.btnEliminar.setVisibility(View.GONE);
            h.ivChevron.setVisibility(View.VISIBLE);
        }

        if (clickListener != null) {
            h.itemView.setOnClickListener(v -> clickListener.onClick(item));
        } else {
            h.itemView.setOnClickListener(null);
            h.itemView.setClickable(false);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ViewHolder con las referencias a las vistas de cada fila de ejercicio seleccionado.
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDescripcion;
        Chip chipDificultad, chipCalorias, chipSeriesReps;
        ImageView btnEliminar, ivChevron;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre         = itemView.findViewById(R.id.tvNombreSeleccionado);
            tvDescripcion    = itemView.findViewById(R.id.tvDescripcionSeleccionado);
            chipDificultad   = itemView.findViewById(R.id.chipDificultad);
            chipCalorias     = itemView.findViewById(R.id.chipCalorias);
            chipSeriesReps   = itemView.findViewById(R.id.chipSeriesReps);
            btnEliminar      = itemView.findViewById(R.id.btnEliminarSeleccionado);
            ivChevron        = itemView.findViewById(R.id.ivChevron);
        }
    }
}
