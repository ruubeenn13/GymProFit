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
// Muestra nombre, descripción, dificultad y calorías de cada Ejercicio, y
// mantiene una lista filtrada independiente de la original para soportar
// búsqueda por texto, filtro por grupo muscular y por dificultad a la vez.
// ============================================================
public class EjercicioAdapter extends RecyclerView.Adapter<EjercicioAdapter.ViewHolder> {

    // Callback invocado al pulsar un ejercicio de la lista.
    public interface OnClickListener {
        void onClick(Ejercicio ejercicio);
    }

    private List<Ejercicio> ejercicios;
    private List<Ejercicio> ejerciciosFiltrados;
    private final OnClickListener clickListener;

    // Constructor sin listener de click.
    public EjercicioAdapter(List<Ejercicio> ejercicios) {
        this(ejercicios, null);
    }

    // Constructor principal: guarda la lista completa y una copia filtrada inicial.
    public EjercicioAdapter(List<Ejercicio> ejercicios, OnClickListener clickListener) {
        this.ejercicios = ejercicios;
        this.ejerciciosFiltrados = new ArrayList<>(ejercicios);
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

    // Rellena las vistas de una fila con los datos del ejercicio filtrado.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ejercicio ejercicio = ejerciciosFiltrados.get(position);
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
        return ejerciciosFiltrados.size();
    }

    // Reemplaza la lista completa de ejercicios y resetea el filtro aplicado.
    public void setEjercicios(List<Ejercicio> nuevos) {
        ejercicios.clear();
        ejercicios.addAll(nuevos);
        ejerciciosFiltrados.clear();
        ejerciciosFiltrados.addAll(nuevos);
        notifyDataSetChanged();
    }

    // Filtra por coincidencia de texto en nombre o descripción.
    public void filtrarPorTexto(String texto) {
        ejerciciosFiltrados.clear();
        if (texto.isEmpty()) {
            ejerciciosFiltrados.addAll(ejercicios);
        } else {
            String textoBajo = texto.toLowerCase();
            for (Ejercicio e : ejercicios) {
                if (e.getNombre().toLowerCase().contains(textoBajo) ||
                        e.getDescripcion().toLowerCase().contains(textoBajo)) {
                    ejerciciosFiltrados.add(e);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Filtra por grupo muscular ("Todos" muestra la lista completa).
    public void filtrarPorGrupo(String grupo) {
        ejerciciosFiltrados.clear();
        if (grupo.equalsIgnoreCase("Todos")) {
            ejerciciosFiltrados.addAll(ejercicios);
        } else {
            for (Ejercicio e : ejercicios) {
                if (e.getGrupoMuscular().equalsIgnoreCase(grupo)) {
                    ejerciciosFiltrados.add(e);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Filtra por nivel de dificultad ("Todos" muestra la lista completa).
    public void filtrarPorDificultad(String dificultad) {
        ejerciciosFiltrados.clear();
        if (dificultad.equalsIgnoreCase("Todos")) {
            ejerciciosFiltrados.addAll(ejercicios);
        } else {
            for (Ejercicio e : ejercicios) {
                if (e.getDificultad().equalsIgnoreCase(dificultad)) {
                    ejerciciosFiltrados.add(e);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Aplica simultáneamente el filtro de texto y el de dificultad.
    public void filtrarCombinado(String texto, String dificultad) {
        ejerciciosFiltrados.clear();
        for (Ejercicio e : ejercicios) {
            boolean coincideTexto = texto.isEmpty()
                    || e.getNombre().toLowerCase().contains(texto.toLowerCase())
                    || e.getDescripcion().toLowerCase().contains(texto.toLowerCase());
            boolean coincideDificultad = dificultad.equalsIgnoreCase("Todos")
                    || e.getDificultad().equalsIgnoreCase(dificultad);
            if (coincideTexto && coincideDificultad) ejerciciosFiltrados.add(e);
        }
        notifyDataSetChanged();
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