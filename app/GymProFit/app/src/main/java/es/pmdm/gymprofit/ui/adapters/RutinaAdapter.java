package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
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

// ============================================================
// RutinaAdapter — adapter de RecyclerView para el listado de rutinas de entrenamiento.
// Muestra nombre, descripción, nivel, nº de ejercicios, duración y calorías
// de cada Rutina, y mantiene una lista filtrada por nivel; el long-click de
// edición/borrado solo se habilita si el usuario es admin o dueño de la rutina.
// ============================================================
public class RutinaAdapter extends RecyclerView.Adapter<RutinaAdapter.ViewHolder> {

    // Callback invocado al pulsar (click simple) una rutina de la lista.
    public interface OnClickListener {
        void onClick(Rutina rutina);
    }

    // Callback invocado al mantener pulsada una rutina editable.
    public interface OnLongClickListener {
        void onLongClick(Rutina rutina, View anchorView);
    }

    private List<Rutina> rutinas;
    private List<Rutina> rutinasFiltradas;
    private final OnClickListener clickListener;
    private OnLongClickListener longClickListener;
    private boolean isAdmin = false;
    private int currentUserId = -1;

    // Constructor sin listener de click.
    public RutinaAdapter(List<Rutina> rutinas) {
        this(rutinas, null);
    }

    // Constructor principal: guarda la lista completa y una copia filtrada inicial.
    public RutinaAdapter(List<Rutina> rutinas, OnClickListener listener) {
        this.rutinas = rutinas;
        this.rutinasFiltradas = new ArrayList<>(rutinas);
        this.clickListener = listener;
    }

    // Registra el listener de long-click (edición/borrado) desde fuera del adapter.
    public void setOnLongClickListener(OnLongClickListener listener) {
        this.longClickListener = listener;
    }

    // Define el contexto de permisos (si es admin y el id del usuario actual)
    // usado para decidir qué rutinas se pueden editar/borrar.
    public void setUserContext(boolean isAdmin, int currentUserId) {
        this.isAdmin = isAdmin;
        this.currentUserId = currentUserId;
    }

    // Infla el layout de un ítem de rutina y crea su ViewHolder.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rutina, parent, false);
        return new ViewHolder(view);
    }

    // Rellena las vistas de una fila con los datos de la rutina y habilita
    // el long-click solo si el usuario tiene permiso para editarla/borrarla.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Rutina rutina = rutinasFiltradas.get(position);
        holder.tvNombre.setText(rutina.getNombre());
        holder.tvDescripcion.setText(rutina.getDescripcion());
        holder.chipNivel.setText(es.pmdm.gymprofit.utils.UIHelper.traducirNivel(holder.itemView.getContext(), rutina.getNivel())); // enum traducido
        holder.tvNumEjercicios.setText(rutina.getNumEjercicios() + " ejercicios");
        holder.tvDuracion.setText(rutina.getDuracionMinutos() + " min");
        holder.tvCalorias.setText("~" + rutina.getCaloriasAproximadas() + " kcal");
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> clickListener.onClick(rutina));
        }
        boolean puedeEditar = longClickListener != null &&
                (isAdmin || (!rutina.isPredefinida() && rutina.getUsuarioId() == currentUserId));
        if (puedeEditar) {
            holder.itemView.setOnLongClickListener(v -> {
                longClickListener.onLongClick(rutina, v);
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return rutinasFiltradas.size();
    }

    // Reemplaza la lista completa de rutinas y resetea el filtro aplicado.
    public void setRutinas(List<Rutina> nuevas) {
        rutinas.clear();
        rutinas.addAll(nuevas);
        rutinasFiltradas.clear();
        rutinasFiltradas.addAll(nuevas);
        notifyDataSetChanged();
    }

    // Filtra por nivel de dificultad ("Todos" muestra la lista completa).
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

    // ViewHolder con las referencias a las vistas de cada fila de rutina.
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