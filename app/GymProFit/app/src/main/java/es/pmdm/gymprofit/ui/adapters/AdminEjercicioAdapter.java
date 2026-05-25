package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;

public class AdminEjercicioAdapter extends RecyclerView.Adapter<AdminEjercicioAdapter.ViewHolder> {

    public interface OnAccionListener {
        void onToggleActivo(Ejercicio ejercicio, int position);
        void onEditar(Ejercicio ejercicio, int position);
    }

    private final List<Ejercicio> items;
    private final OnAccionListener listener;

    public AdminEjercicioAdapter(List<Ejercicio> items, OnAccionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_ejercicio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Ejercicio e = items.get(position);
        Context ctx = h.itemView.getContext();

        h.tvNombre.setText(e.getNombre());
        String desc = e.getDescripcion();
        h.tvDescripcion.setText((desc != null && !desc.isEmpty()) ? desc : "—");

        String grupo = e.getGrupoMuscular();
        h.chipGrupo.setText((grupo != null && !grupo.isEmpty()) ? grupo : "—");

        String dif = e.getDificultad();
        h.chipDificultad.setText((dif != null && !dif.isEmpty()) ? dif : "—");

        if (e.isActivo()) {
            h.chipEstado.setText(ctx.getString(R.string.admin_estado_activo));
            h.chipEstado.setChipBackgroundColorResource(R.color.green_chip);
            h.chipEstado.setTextColor(ContextCompat.getColor(ctx, R.color.green_chip_text));
        } else {
            h.chipEstado.setText(ctx.getString(R.string.admin_estado_inactivo));
            h.chipEstado.setChipBackgroundColorResource(R.color.red_chip);
            h.chipEstado.setTextColor(ContextCompat.getColor(ctx, R.color.red_chip_text));
        }

        h.btnAcciones.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(ctx, v);
            popup.getMenu().add(0, 1, 0, ctx.getString(R.string.admin_editar_ejercicio_titulo));
            popup.getMenu().add(0, 2, 0, e.isActivo()
                    ? ctx.getString(R.string.admin_desactivar)
                    : ctx.getString(R.string.admin_activar));

            popup.setOnMenuItemClickListener(item -> {
                int pos = h.getAdapterPosition();
                if (pos == RecyclerView.NO_ID) return false;
                if (item.getItemId() == 1) {
                    listener.onEditar(items.get(pos), pos);
                } else if (item.getItemId() == 2) {
                    listener.onToggleActivo(items.get(pos), pos);
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void actualizarItem(int position, Ejercicio ejercicio) {
        items.set(position, ejercicio);
        notifyItemChanged(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDescripcion;
        Chip chipGrupo, chipDificultad, chipEstado;
        ImageView btnAcciones;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre      = itemView.findViewById(R.id.tvNombreEjercicio);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionEjercicio);
            chipGrupo     = itemView.findViewById(R.id.chipGrupoEjercicio);
            chipDificultad= itemView.findViewById(R.id.chipDificultadEjercicio);
            chipEstado    = itemView.findViewById(R.id.chipEstadoEjercicio);
            btnAcciones   = itemView.findViewById(R.id.btnAccionesEjercicio);
        }
    }
}
