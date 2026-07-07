package es.pmdm.gymprofit.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;

// ============================================================
// EjercicioAdapter — adapter de RecyclerView para el catálogo de ejercicios.
// Muestra nombre, descripción, dificultad y calorías de cada Ejercicio.
// El filtrado (texto/grupo/dificultad) se hace en el SERVIDOR vía
// /ejercicios/buscar (paginado); el adapter solo pinta la lista recibida
// y soporta append de páginas para el scroll infinito.
// ============================================================
public class EjercicioAdapter extends RecyclerView.Adapter<EjercicioAdapter.ViewHolder> {

    // Callback invocado al pulsar un ejercicio de la lista.
    public interface OnClickListener {
        void onClick(Ejercicio ejercicio);
    }

    private final List<Ejercicio> ejercicios;
    private final OnClickListener clickListener;

    // Constructor sin listener de click.
    public EjercicioAdapter(List<Ejercicio> ejercicios) {
        this(ejercicios, null);
    }

    // Constructor principal: guarda la lista mostrada (copia defensiva).
    public EjercicioAdapter(List<Ejercicio> ejercicios, OnClickListener clickListener) {
        this.ejercicios = new ArrayList<>(ejercicios);
        this.clickListener = clickListener;
    }

    // Infla el layout de un ítem de ejercicio y crea su ViewHolder.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ejercicio, parent, false);
        return new ViewHolder(view);
    }

    // Rellena las vistas de una fila con los datos del ejercicio.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ejercicio ejercicio = ejercicios.get(position);
        holder.tvNombre.setText(ejercicio.getNombre());
        holder.tvDescripcion.setText(ejercicio.getDescripcion());
        holder.ivIcono.setImageResource(R.drawable.ic_ejercicios);
        holder.chipDificultad.setText(es.pmdm.gymprofit.utils.UIHelper.traducirNivel(holder.itemView.getContext(), ejercicio.getDificultad())); // enum traducido
        holder.chipCalorias.setText(ejercicio.getCalorias() + " kcal");
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> clickListener.onClick(ejercicio));
        }
    }

    @Override
    public int getItemCount() {
        return ejercicios.size();
    }

    // Reemplaza la lista completa (nueva búsqueda / primera página).
    public void setEjercicios(List<Ejercicio> nuevos) {
        ejercicios.clear();
        ejercicios.addAll(nuevos);
        notifyDataSetChanged();
    }

    // Añade una página adicional al final (scroll infinito).
    public void addEjercicios(List<Ejercicio> nuevos) {
        if (nuevos == null || nuevos.isEmpty()) return;
        int desde = ejercicios.size();
        ejercicios.addAll(nuevos);
        notifyItemRangeInserted(desde, nuevos.size());
    }

    // ViewHolder con las referencias a las vistas de cada fila de ejercicio.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcono;
        TextView tvNombre, tvDescripcion;
        Chip chipDificultad, chipCalorias;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcono = itemView.findViewById(R.id.ivIconoEjercicio);
            tvNombre = itemView.findViewById(R.id.tvNombreEjercicio);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionEjercicio);
            chipDificultad = itemView.findViewById(R.id.chipDificultad);
            chipCalorias = itemView.findViewById(R.id.chipCalorias);
        }
    }
}