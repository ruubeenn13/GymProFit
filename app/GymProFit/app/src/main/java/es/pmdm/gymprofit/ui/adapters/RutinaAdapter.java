package es.pmdm.gymprofit.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;

public class RutinaAdapter extends RecyclerView.Adapter<RutinaAdapter.ViewHolder> {

    public interface OnClickListener {
        void onClick(Rutina rutina);
    }

    private List<Rutina> rutinas;
    private List<Rutina> rutinasFiltradas;
    private final OnClickListener clickListener;

    public RutinaAdapter(List<Rutina> rutinas) {
        this(rutinas, null);
    }

    public RutinaAdapter(List<Rutina> rutinas, OnClickListener listener) {
        this.rutinas = rutinas;
        this.rutinasFiltradas = new ArrayList<>(rutinas);
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rutina, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Rutina rutina = rutinasFiltradas.get(position);
        holder.tvNombre.setText(rutina.getNombre());
        holder.tvDescripcion.setText(rutina.getDescripcion());
        holder.chipNivel.setText(rutina.getNivel());
        holder.tvNumEjercicios.setText(rutina.getNumEjercicios() + " ejercicios");
        holder.tvDuracion.setText(rutina.getDuracionMinutos() + " min");
        holder.tvCalorias.setText("~" + rutina.getCaloriasAproximadas() + " kcal");
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> clickListener.onClick(rutina));
        }
    }

    @Override
    public int getItemCount() {
        return rutinasFiltradas.size();
    }

    public void setRutinas(List<Rutina> nuevas) {
        rutinas.clear();
        rutinas.addAll(nuevas);
        rutinasFiltradas.clear();
        rutinasFiltradas.addAll(nuevas);
        notifyDataSetChanged();
    }

    public void filtrarPorNivel(String nivel) {
        rutinasFiltradas.clear();
        if (nivel.equalsIgnoreCase("Todos")) {
            rutinasFiltradas.addAll(rutinas);
        } else {
            for (Rutina r : rutinas) {
                if (r.getNivel().equalsIgnoreCase(nivel)) {
                    rutinasFiltradas.add(r);
                }
            }
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDescripcion, tvNumEjercicios, tvDuracion, tvCalorias;
        Chip chipNivel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreRutina);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionRutina);
            chipNivel = itemView.findViewById(R.id.chipNivel);
            tvNumEjercicios = itemView.findViewById(R.id.tvNumEjercicios);
            tvDuracion = itemView.findViewById(R.id.tvDuracion);
            tvCalorias = itemView.findViewById(R.id.tvCalorias);
        }
    }
}