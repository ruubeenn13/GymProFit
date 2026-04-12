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

    private List<Rutina> rutinas;
    private List<Rutina> rutinasFiltradas;

    public RutinaAdapter(List<Rutina> rutinas) {
        this.rutinas = rutinas;
        this.rutinasFiltradas = new ArrayList<>(rutinas);
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
    }

    @Override
    public int getItemCount() {
        return rutinasFiltradas.size();
    }

    public void addRutina(Rutina rutina) {
        rutinas.add(rutina);
        rutinasFiltradas.add(rutina);
        notifyItemInserted(rutinasFiltradas.size() - 1);
    }

    public void filtrarPorNivel(String nivel) {
        rutinasFiltradas.clear();
        if (nivel.equals("Todos")) {
            rutinasFiltradas.addAll(rutinas);
        } else {
            for (Rutina r : rutinas) {
                if (r.getNivel().equals(nivel)) {
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