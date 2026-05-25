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
import es.pmdm.gymprofit.model.rutina.Rutina;

public class AdminRutinaAdapter extends RecyclerView.Adapter<AdminRutinaAdapter.ViewHolder> {

    public interface OnAccionListener {
        void onToggleActiva(Rutina rutina, int position);
        void onEditar(Rutina rutina, int position);
    }

    private final List<Rutina> items;
    private final OnAccionListener listener;

    public AdminRutinaAdapter(List<Rutina> items, OnAccionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_rutina, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Rutina r = items.get(position);
        Context ctx = h.itemView.getContext();

        h.tvNombre.setText(r.getNombre());
        String desc = r.getDescripcion();
        h.tvDescripcion.setText((desc != null && !desc.isEmpty()) ? desc : "—");
        h.chipNivel.setText(r.getNivel());
        h.tvNumEjercicios.setText(r.getNumEjercicios() + " ejerc.");

        if (r.isActiva()) {
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
            popup.getMenu().add(0, 1, 0, ctx.getString(R.string.admin_editar_rutina_titulo));
            popup.getMenu().add(0, 2, 0, r.isActiva()
                    ? ctx.getString(R.string.admin_desactivar)
                    : ctx.getString(R.string.admin_activar));

            popup.setOnMenuItemClickListener(item -> {
                int pos = h.getAdapterPosition();
                if (pos == RecyclerView.NO_ID) return false;
                if (item.getItemId() == 1) {
                    listener.onEditar(items.get(pos), pos);
                } else if (item.getItemId() == 2) {
                    listener.onToggleActiva(items.get(pos), pos);
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void actualizarItem(int position, Rutina rutina) {
        items.set(position, rutina);
        notifyItemChanged(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDescripcion, tvNumEjercicios;
        Chip chipNivel, chipEstado;
        ImageView btnAcciones;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre        = itemView.findViewById(R.id.tvNombreRutina);
            tvDescripcion   = itemView.findViewById(R.id.tvDescripcionRutina);
            chipNivel       = itemView.findViewById(R.id.chipNivelRutina);
            chipEstado      = itemView.findViewById(R.id.chipEstadoRutina);
            tvNumEjercicios = itemView.findViewById(R.id.tvNumEjerciciosRutina);
            btnAcciones     = itemView.findViewById(R.id.btnAccionesRutina);
        }
    }
}
