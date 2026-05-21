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

public class EjercicioAdapter extends RecyclerView.Adapter<EjercicioAdapter.ViewHolder> {

    public interface OnClickListener {
        void onClick(Ejercicio ejercicio);
    }

    private List<Ejercicio> ejercicios;
    private List<Ejercicio> ejerciciosFiltrados;
    private final OnClickListener clickListener;

    public EjercicioAdapter(List<Ejercicio> ejercicios) {
        this(ejercicios, null);
    }

    public EjercicioAdapter(List<Ejercicio> ejercicios, OnClickListener clickListener) {
        this.ejercicios = ejercicios;
        this.ejerciciosFiltrados = new ArrayList<>(ejercicios);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ejercicio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ejercicio ejercicio = ejerciciosFiltrados.get(position);
        holder.tvNombre.setText(ejercicio.getNombre());
        holder.tvDescripcion.setText(ejercicio.getDescripcion());
        holder.ivIcono.setImageResource(R.drawable.ic_ejercicios);
        holder.chipDificultad.setText(ejercicio.getDificultad());
        holder.chipCalorias.setText(ejercicio.getCalorias() + " kcal");
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> clickListener.onClick(ejercicio));
        }
    }

    @Override
    public int getItemCount() {
        return ejerciciosFiltrados.size();
    }

    public void setEjercicios(List<Ejercicio> nuevos) {
        ejercicios.clear();
        ejercicios.addAll(nuevos);
        ejerciciosFiltrados.clear();
        ejerciciosFiltrados.addAll(nuevos);
        notifyDataSetChanged();
    }

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